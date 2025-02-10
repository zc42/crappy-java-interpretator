package zc.dev.interpreter.tree_parser.statement.decomposer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zc.dev.interpreter.lexer.Token;

import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor(staticName = "from")
public class TokenPredicate {
    private final int index;
    private final Predicate<Token> predicate;
}
