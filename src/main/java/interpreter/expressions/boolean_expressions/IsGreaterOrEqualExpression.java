package interpreter.expressions.boolean_expressions;

import interpreter.expressions.Expression;

import java.text.MessageFormat;

import static interpreter.Utils.prnt;

public class IsGreaterOrEqualExpression implements Expression<Boolean> {
    private final Expression<Double> leftExpression;
    private final Expression<Double> rightExpression;

    public IsGreaterOrEqualExpression(Expression<Double> leftExpression, Expression<Double> rightExpression) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    @Override
    public Boolean interpret() {
        double v1 = leftExpression.interpret();
        double v2 = rightExpression.interpret();
        boolean v = v1 >= v2;
        prnt(MessageFormat.format("{0} = {1} >= {2}", v, v1, v2));
        return v;
    }
}
