package interpreter.expressions.boolean_expressions;

import interpreter.expressions.Expression;

import java.text.MessageFormat;

import static interpreter.Utils.prnt;

public class AndExpression implements Expression<Boolean> {
    private final Expression<Boolean> leftExpression;
    private final Expression<Boolean> rightExpression;

    public AndExpression(Expression<Boolean> leftExpression, Expression<Boolean> rightExpression) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    @Override
    public Boolean interpret() {
        Boolean v1 = leftExpression.interpret();
        Boolean v2 = rightExpression.interpret();
        boolean v = v1 && v2;
        prnt(MessageFormat.format("{0} = {1} && {2}", v, v1, v2));
        return v;
    }
}
