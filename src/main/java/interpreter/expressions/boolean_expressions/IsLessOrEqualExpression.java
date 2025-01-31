package interpreter.expressions.boolean_expressions;

import interpreter.expressions.Expression;

import java.text.MessageFormat;

import static interpreter.Utils.prnt;

public class IsLessOrEqualExpression implements Expression<Boolean> {
    private final Expression<Double> leftExpression;
    private final Expression<Double> rightExpression;

    public IsLessOrEqualExpression(Expression<Double> leftExpression, Expression<Double> rightExpression) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    @Override
    public Boolean interpret() {
        Double v1 = leftExpression.interpret();
        Double v2 = rightExpression.interpret();
        boolean v = v1 <= v2;
        prnt(MessageFormat.format("{0} = {1} <= {2}", v, v1, v2));
        return v;
    }
}
