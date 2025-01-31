package interpreter.expressions;

import interpreter.call_stack.CallStackFrame;
import interpreter.expressions.arithmetic_expressions.NumberExpression;
import interpreter.expressions.boolean_expressions.*;
import interpreter.lexer.Token;
import interpreter.lexer.TokenType;

import java.util.*;
import java.util.function.BiFunction;

// Parser to convert input string into an expression tree
public class BooleanExpressionParser {

    private static final Map<String, BiFunction<Expression<Boolean>, Expression<Boolean>, Expression<Boolean>>> operationFunctionMap1 = getOperationFunctionMap1();
    private static final Map<String, BiFunction<Expression<Double>, Expression<Double>, Expression<Boolean>>> operationFunctionMap2 = getOperationFunctionMap();

    public static Expression<?> parse(CallStackFrame frame, List<Token> tokens) {

//        long operationCount = tokens.stream().filter(e -> e.getType() == TokenType.ARITHMETIC_OPERATOR).count();
//        long numberCount = tokens.stream().filter(e -> e.getType() == TokenType.NUMBER || e.getType() == TokenType.IDENTIFIER).count();
//        if (numberCount != operationCount + 1)
//            throw new RuntimeException("Number of arithmetic operators and digits not match, should be numberCount == operationCount + 1");
//        if (numberCount + operationCount != tokens.size())
//            throw new RuntimeException("Number of arithmetic operators and digits not match total count of tokens");

        Stack<Expression<?>> stack = new Stack<>();
        tokens.forEach(token -> process(frame, stack, token));

        if (stack.size() != 1) {
            throw new ArithmeticException("Syntax error: stack.size() != 1\ntokens: " + Token.toString(tokens));
        }
        return stack.pop();
    }

    private static void process(CallStackFrame frame, Stack<Expression<?>> stack, Token token) {
        if (token.getType() == TokenType.BOOLEAN_OPERATOR) {
            Expression<?> expression = getOperationExpression(stack, token);
            stack.push(expression);
        } else if (token.getType() == TokenType.NUMBER) {
            double v = Double.parseDouble(token.getValue());
            Expression<Double> expression = new NumberExpression(v);
            stack.push(expression);
        } else if (token.getType() == TokenType.IDENTIFIER) {
            Object o = frame.getVariableValue(token.getValue());
            double v = o.getClass() == String.class
                    ? Double.parseDouble(o.toString())
                    : (double) o;
            Expression<Double> expression = new NumberExpression(v);
            stack.push(expression);
        }
    }

    private static  Map<String, BiFunction<Expression<Boolean>, Expression<Boolean>, Expression<Boolean>>> getOperationFunctionMap1() {
        Map<String, BiFunction<Expression<Boolean>, Expression<Boolean>, Expression<Boolean>>> map = new HashMap<>();
        map.put("&&", AndExpression::new);
        map.put("||", OrExpression::new);
        map.put("==", IsEqualExpression::new);

//        map.put("!", NotExpression::new);

        return map;
    }

    private static  Map<String, BiFunction<Expression<Double>, Expression<Double>, Expression<Boolean>>> getOperationFunctionMap() {
        Map<String, BiFunction<Expression<Double>, Expression<Double>, Expression<Boolean>>> map = new HashMap<>();
        map.put(">", IsGreaterExpression::new);
        map.put("<", IsLessExpression::new);
        map.put(">=", IsGreaterOrEqualExpression::new);
        map.put("<=", IsLessOrEqualExpression::new);
        return map;
    }

    private static Expression<?> getOperationExpression(Stack<Expression<?>> stack, Token token) {
        String value = token.getValue();
        if (operationFunctionMap1.containsKey(value)) {
            Expression<Boolean> right = (Expression<Boolean>) stack.pop();
            Expression<Boolean> left = (Expression<Boolean>) stack.pop();
            return operationFunctionMap1.get(value).apply(left, right);
        } else if (operationFunctionMap2.containsKey(value)) {
            Expression<Double> right = (Expression<Double>) stack.pop();
            Expression<Double> left = (Expression<Double>) stack.pop();
            return operationFunctionMap2.get(value).apply(left, right);
        } else if (Objects.equals(value, "!")) {
            Expression<Boolean> expression = (Expression<Boolean>) stack.pop();
            return new NotExpression(expression);
        }

        throw new RuntimeException("Unknown operator: " + value);

    }
}
