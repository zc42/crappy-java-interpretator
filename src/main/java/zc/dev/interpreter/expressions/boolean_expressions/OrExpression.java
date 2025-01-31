package zc.dev.interpreter.expressions.boolean_expressions;


import zc.dev.interpreter.expressions.Expression;

import java.text.MessageFormat;

import static zc.dev.interpreter.Utils.prnt;

public class OrExpression implements Expression<Boolean> {
    private final Expression<Boolean> leftExpression;
    private final Expression<Boolean> rightExpression;

    public OrExpression(Expression<Boolean> leftExpression, Expression<Boolean> rightExpression) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    @Override
    public Boolean interpret() {
        boolean v1 = leftExpression.interpret();
        boolean v2 = rightExpression.interpret();
        boolean v = v1 || v2;
        prnt(MessageFormat.format("{0} = {1} || {2}", v, v1, v2));
        return v;
    }
}
