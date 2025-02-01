package zc.dev.interpreter.expressions.boolean_expressions;


import zc.dev.interpreter.expressions.Expression;

import java.text.MessageFormat;

import static zc.dev.interpreter.Utils.prnt;

public class NotExpression implements Expression<Boolean> {
    private final Expression<Boolean> expression;

    public NotExpression(Expression<?> expression) {
        switch (expression) {
            case NotExpression notExpression -> this.expression = notExpression;
            case BooleanExpression booleanExpression -> this.expression = booleanExpression;
            case BooleanNumericExpression booleanNumericExpression -> this.expression = booleanNumericExpression;
            case null, default ->
                    throw new RuntimeException("expression type is not supported: " + expression.getClass());
        }
    }

    @Override
    public Boolean interpret() {
        boolean v1 = expression.interpret();
        boolean v = !v1;
        prnt(MessageFormat.format("{0} = ! {1}", v, v1));
        return v;
    }
}
