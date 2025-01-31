package interpreter.expressions.boolean_expressions;


import interpreter.expressions.Expression;

import java.text.MessageFormat;

import static interpreter.Utils.prnt;

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
