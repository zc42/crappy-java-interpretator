package interpreter.expressions.arithmetic_expressions;

import interpreter.expressions.Expression;

// Non-terminal expressions for arithmetic operations
public class AdditionExpression implements Expression<Double> {
    private final Expression<Double> left;
    private final Expression<Double> right;

    public AdditionExpression(Expression<Double> left, Expression<Double> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Double interpret() {
        return left.interpret() + right.interpret();
    }
}
