package zc.dev.interpreter.expressions;

import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.expressions.arithmetic_expressions.NumberExpression;
import zc.dev.interpreter.expressions.boolean_expressions.*;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BooleanExpressionParser {

    private static final Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Boolean>>> operationFunctionMap1 = getOperationFunctionMap1();
    private static final Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Boolean>>> operationFunctionMap2 = getOperationFunctionMap();

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

    private static Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Boolean>>> getOperationFunctionMap1() {
        Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Boolean>>> map = new HashMap<>();
        map.put("&&", getBooleanExpression((b1, b2) -> b1 && b2));
        map.put("||", getBooleanExpression((b1, b2) -> b1 || b2));
        map.put("==", getBooleanExpression((b1, b2) -> b1 == b2));
        map.put("!=", getBooleanExpression((b1, b2) -> b1 != b2));
        return map;
    }

    private static Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Boolean>>> getOperationFunctionMap() {
        Map<String, BiFunction<Expression<?>, Expression<?>, Expression<Boolean>>> map = new HashMap<>();
        map.put(">", getBooleanNumericExpression((d1, d2) -> d1 > d2));
        map.put("<", getBooleanNumericExpression((d1, d2) -> d1 < d2));
        map.put(">=", getBooleanNumericExpression((d1, d2) -> d1 >= d2));
        map.put("<=", getBooleanNumericExpression((d1, d2) -> d1 <= d2));
        map.put("==", getBooleanNumericExpression(Double::equals));
        map.put("!=", getBooleanNumericExpression((d1, d2) -> !d1.equals(d2)));
        return map;
    }

    private static BiFunction<Expression<?>, Expression<?>, Expression<Boolean>> getBooleanExpression
            (BiFunction<Boolean, Boolean, Boolean> function) {
        return (left, right) -> new BooleanExpression(left, right, function);
    }

    private static BiFunction<Expression<?>, Expression<?>, Expression<Boolean>> getBooleanNumericExpression
            (BiFunction<Double, Double, Boolean> function) {
        return (left, right) -> new BooleanNumericExpression(left, right, function);
    }

    private static Expression<?> getOperationExpression(Stack<Expression<?>> stack, Token token) {
        String value = token.getValue();
        if (Objects.equals(value, "!")) return new NotExpression(stack.pop());

        Expression<?> right = stack.pop();
        Expression<?> left = stack.pop();
        if (operationFunctionMap2.containsKey(value)) return operationFunctionMap2.get(value).apply(left, right);
        else if (operationFunctionMap1.containsKey(value)) return operationFunctionMap1.get(value).apply(left, right);
        throw new RuntimeException("Unknown operator: " + value);
    }
}
