package zc.dev.interpreter.expressions;

import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.expressions.arithmetic_expressions.*;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;

// Parser to convert input string into an expression tree
public class ArithmeticExpressionParser {

    private static final Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Double>>> OPERATION_FUNCTION_MAP = getOperationFunctionMap();

    public static Expression<Double> parse(CallStackFrame frame, List<Token> tokens) {

        long operationCount = tokens.stream().filter(e -> e.getType() == TokenType.ARITHMETIC_OPERATOR).count();
        long numberCount = tokens.stream().filter(e -> e.getType() == TokenType.NUMBER || e.getType() == TokenType.IDENTIFIER).count();
        if (numberCount != operationCount + 1)
            throw new RuntimeException("Number of arithmetic operators and digits not match, should be numberCount == operationCount + 1");
        if (numberCount + operationCount != tokens.size())
            throw new RuntimeException("Number of arithmetic operators and digits not match total count of tokens");

        Stack<Expression<Double>> stack = new Stack<>();
        tokens.forEach(token -> process(frame, stack, token));

        if (stack.size() != 1) {
            throw new ArithmeticException("Syntax error: stack.size() != 1\ntokens: " + Token.toString(tokens));
        }
        return stack.pop();
    }

    private static void process(CallStackFrame frame, Stack<Expression<Double>> stack, Token token) {
        if (token.getType() == TokenType.ARITHMETIC_OPERATOR) {
            Expression<Double> expression = getArithmeticOperationExpression(stack, token);
            stack.push(expression);
        } else if (token.getType() == TokenType.NUMBER) {
            double v = Double.parseDouble(token.getValue());
            NumberExpression expression = new NumberExpression(v);
            stack.push(expression);
        } else if (token.getType() == TokenType.IDENTIFIER) {
            Object o = frame.getVariableValue(token.getValue());
            double v = o.getClass() == String.class
                    ? Double.parseDouble(o.toString())
                    : (double) o;
            NumberExpression expression = new NumberExpression(v);
            stack.push(expression);
        }
    }

    private static Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Double>>> getOperationFunctionMap() {
        Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Double>>> map = new HashMap<>();
        map.put("+", (e1, e2) -> new ArithmeticExpression(e1, e2, Double::sum));
        map.put("-", (e1, e2) -> new ArithmeticExpression(e1, e2, (d1, d2) -> d1 - d2));
        map.put("*", (e1, e2) -> new ArithmeticExpression(e1, e2, (d1, d2) -> d1 * d2));
        map.put("/", (e1, e2) -> new ArithmeticExpression(e1, e2, (d1, d2) -> d1 / d2));
        map.put("%", (e1, e2) -> new ArithmeticExpression(e1, e2, (d1, d2) -> d1 % d2));
        return map;
    }

    private static Expression<Double> getArithmeticOperationExpression(Stack<Expression<Double>> stack, Token token) {
        Expression<?> right = stack.pop();
        Expression<?> left = stack.pop();
        String value = token.getValue();
        if (!ArithmeticExpressionParser.OPERATION_FUNCTION_MAP.containsKey(value))
            throw new RuntimeException("Unknown operator: " + value);
        return ArithmeticExpressionParser.OPERATION_FUNCTION_MAP.get(value).apply(left, right);
    }
}
