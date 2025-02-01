package zc.dev.interpreter.expressions.boolean_expressions;

import zc.dev.interpreter.expressions.Expression;
import zc.dev.interpreter.expressions.arithmetic_expressions.NumberExpression;

import java.util.function.BiFunction;

public class BooleanNumericExpression implements Expression<Boolean> {
    private final Expression<Double> leftExpression;
    private final Expression<Double> rightExpression;
    private final BiFunction<Double, Double, Boolean> function;

    public BooleanNumericExpression(Expression<?> leftExpression,
                                    Expression<?> rightExpression,
                                    BiFunction<Double, Double, Boolean> function) {

        if (leftExpression instanceof NumberExpression) this.leftExpression = (NumberExpression) leftExpression;
        else throw new RuntimeException("leftExpression type is not supported: " + leftExpression.getClass());
        if (rightExpression instanceof NumberExpression) this.rightExpression = (NumberExpression) rightExpression;
        else throw new RuntimeException("rightExpression type is not supported: " + rightExpression.getClass());
        this.function = function;
    }

    @Override
    public Boolean interpret() {
        double v1 = leftExpression.interpret();
        double v2 = rightExpression.interpret();
        return function.apply(v1, v2);
    }
}
