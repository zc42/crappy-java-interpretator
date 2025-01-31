package zc.dev.interpreter.expressions.boolean_expressions;


import zc.dev.interpreter.expressions.Expression;

import java.text.MessageFormat;

import static zc.dev.interpreter.Utils.prnt;

public class NotExpression implements Expression<Boolean> {
    private final Expression<Boolean> leftExpression;

    public NotExpression(Expression<Boolean> leftExpression) {
        this.leftExpression = leftExpression;
    }

    @Override
    public Boolean interpret() {
        boolean v1 = leftExpression.interpret();
        boolean v = !v1;
        prnt(MessageFormat.format("{0} = ! {1}", v, v1));
        return v;
    }
}
