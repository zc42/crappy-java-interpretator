package interpreter.expressions.arithmetic_expressions;

import interpreter.expressions.Expression;

public class MultiplicationExpression implements Expression<Double> {
    private final Expression<Double> left;
    private final Expression<Double> right;

    public MultiplicationExpression(Expression<Double> left, Expression<Double> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Double interpret() {
        return left.interpret() * right.interpret();
    }
}
