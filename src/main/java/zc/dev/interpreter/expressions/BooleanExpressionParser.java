package zc.dev.interpreter.expressions;

import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.expressions.arithmetic_expressions.NumberExpression;
import zc.dev.interpreter.expressions.boolean_expressions.*;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.*;
import java.util.function.BiFunction;

public class BooleanExpressionParser {

    private static final Map<String, BiFunction<Expression<Boolean>, Expression<Boolean>, Expression<Boolean>>> operationFunctionMap1 = getOperationFunctionMap1();
    private static final Map<String, BiFunction<Expression<Double>, Expression<Double>, Expression<Boolean>>> operationFunctionMap2 = getOperationFunctionMap();

    public static Expression<Boolean> parse(CallStackFrame frame, List<Token> tokens) {

        Stack<Expression<?>> stack = new Stack<>();
        tokens.forEach(token -> process(frame, stack, token));

        if (stack.size() != 1) {
            throw new ArithmeticException("Syntax error: stack.size() != 1\ntokens: " + Token.toString(tokens));
        }
        return (Expression<Boolean>) stack.pop();
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

    private static Map<String, BiFunction<Expression<Boolean>, Expression<Boolean>, Expression<Boolean>>> getOperationFunctionMap1() {
        Map<String, BiFunction<Expression<Boolean>, Expression<Boolean>, Expression<Boolean>>> map = new HashMap<>();
        map.put("&&", AndExpression::new);
        map.put("||", OrExpression::new);
        map.put("==", IsEqualNumericExpression::new);

//        map.put("!", NotExpression::new);

        return map;
    }

    private static Map<String, BiFunction<Expression<Double>, Expression<Double>, Expression<Boolean>>> getOperationFunctionMap() {
        Map<String, BiFunction<Expression<Double>, Expression<Double>, Expression<Boolean>>> map = new HashMap<>();
        map.put(">", IsGreaterExpression::new);
        map.put("<", IsLessExpression::new);
        map.put(">=", IsGreaterOrEqualExpression::new);
        map.put("<=", IsLessOrEqualExpression::new);
        map.put("==", IsEqualExpression::new);
        return map;
    }

    private static Expression<?> getOperationExpression(Stack<Expression<?>> stack, Token token) {
        String value = token.getValue();
        if (Objects.equals(value, "!")) {
            Expression<Boolean> expression = (Expression<Boolean>) stack.pop();
            return new NotExpression(expression);
        }
        Expression<?> right = stack.pop();
        Expression<?> left = stack.pop();
        if (right instanceof NumberExpression && operationFunctionMap2.containsKey(value))
            return operationFunctionMap2.get(value).apply((Expression<Double>) left, (Expression<Double>) right);
        else if (operationFunctionMap1.containsKey(value))
            return operationFunctionMap1.get(value).apply((Expression<Boolean>) left, (Expression<Boolean>) right);
        throw new RuntimeException("Unknown operator: " + value);

    }
}
