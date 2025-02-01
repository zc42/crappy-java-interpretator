package zc.dev.interpreter.expressions.arithmetic_expressions;

import zc.dev.interpreter.expressions.Expression;

public class ModExpression implements Expression<Double> {
    private final Expression<Double> left;
    private final Expression<Double> right;

    public ModExpression(Expression<Double> left, Expression<Double> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Double interpret() {
        return left.interpret() % right.interpret();
    }
}
