package zc.dev.interpreter.expressions.arithmetic_expressions;

import zc.dev.interpreter.expressions.Expression;

import java.util.function.BiFunction;

public class ArithmeticExpression implements Expression<Double> {
    private final Expression<Double> leftExpression;
    private final Expression<Double> rightExpression;
    private final BiFunction<Double, Double, Double> function;

    public ArithmeticExpression(Expression<?> leftExpression,
                                Expression<?> rightExpression,
                                BiFunction<Double, Double, Double> function) {

        if (leftExpression instanceof NumberExpression) this.leftExpression = (NumberExpression) leftExpression;
        else if (leftExpression instanceof ArithmeticExpression) this.leftExpression = (ArithmeticExpression) leftExpression;
        else throw new RuntimeException("leftExpression type is not supported: " + leftExpression.getClass());
        if (rightExpression instanceof NumberExpression) this.rightExpression = (NumberExpression) rightExpression;
        else if (rightExpression instanceof ArithmeticExpression) this.rightExpression = (ArithmeticExpression) rightExpression;
        else throw new RuntimeException("rightExpression type is not supported: " + rightExpression.getClass());
        this.function = function;
    }

    @Override
    public Double interpret() {
        double v1 = leftExpression.interpret();
        double v2 = rightExpression.interpret();
        return function.apply(v1, v2);
    }
}
