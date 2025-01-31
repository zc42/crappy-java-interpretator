package zc.dev.interpreter.expressions.arithmetic_expressions;

import zc.dev.interpreter.expressions.Expression;

// Terminal expression for numbers
public class NumberExpression implements Expression<Double> {
    private final double number;

    public NumberExpression(double number) {
        this.number = number;
    }

    @Override
    public Double interpret() {
        return number;
    }
}
