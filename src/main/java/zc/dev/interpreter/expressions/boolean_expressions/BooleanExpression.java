package zc.dev.interpreter.expressions.boolean_expressions;

import zc.dev.interpreter.expressions.Expression;

import java.util.function.BiFunction;

public class BooleanExpression implements Expression<Boolean> {
    private final Expression<Boolean> leftExpression;
    private final Expression<Boolean> rightExpression;
    private final BiFunction<Boolean, Boolean, Boolean> function;

    public BooleanExpression(Expression<?> leftExpression,
                             Expression<?> rightExpression,
                             BiFunction<Boolean, Boolean, Boolean> function) {

        switch (leftExpression) {
            case NotExpression expression -> this.leftExpression = expression;
            case BooleanExpression booleanExpression -> this.leftExpression = booleanExpression;
            case BooleanNumericExpression booleanNumericExpression -> this.leftExpression = booleanNumericExpression;
            case null, default ->
                    throw new RuntimeException("leftExpression type is not supported: " + leftExpression.getClass());
        }

        switch (rightExpression) {
            case NotExpression notExpression -> this.rightExpression = notExpression;
            case BooleanExpression booleanExpression -> this.rightExpression = booleanExpression;
            case BooleanNumericExpression booleanNumericExpression -> this.rightExpression = booleanNumericExpression;
            case null, default ->
                    throw new RuntimeException("rightExpression type is not supported: " + rightExpression.getClass());
        }

        this.function = function;
    }

    @Override
    public Boolean interpret() {
        boolean v1 = leftExpression.interpret();
        boolean v2 = rightExpression.interpret();
        return function.apply(v1, v2);
    }
}
