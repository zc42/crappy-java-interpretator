package zc.dev.interpreter.tree_parser.statement.decomposer;

import lombok.RequiredArgsConstructor;
import zc.dev.interpreter.lexer.Token;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor(staticName = "from")
public class TokenTester {
    private final List<Token> tokens;
    private final int index;

    public boolean testToken(TokenPredicate... tokenPredicates) {
        Predicate<TokenPredicate> predicate = this::testToken;
        return Arrays.stream(tokenPredicates)
                .filter(predicate.negate())
                .findFirst()
                .isEmpty();
    }

    private boolean testToken(TokenPredicate tokenPredicate) {
        int i = index + tokenPredicate.getIndex();
        if (i < 0 || i >= tokens.size()) return false;
        Token token = tokens.get(i);
        return tokenPredicate.getPredicate().test(token);
    }
}
