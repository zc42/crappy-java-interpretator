package zc.dev.interpreter.lexer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

@Getter
@RequiredArgsConstructor()
public class TokenizerContext implements IterableWithPeek<Character> {
    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private final int length;
    private int i = 0;

    public static TokenizerContext from(String input) {
        return new TokenizerContext(input, input.length());
    }

    public boolean hasNext() {
        return i < length;
    }

    public Character peek() {
        return input.charAt(i);
    }

    public Optional<Character> peek(int n) {
        int index = i + n;
        return index < 0 || index >= input.length()
                ? Optional.empty()
                : Optional.of(input.charAt(index));
    }

    public Character next() {
        char c = peek();
        i++;
        return c;
    }

    public void addToken(Token token) {
        tokens.add(token);
    }

    public boolean containsFromCurrentPosition(String value) {
        IntPredicate predicate = e -> input.length() > e && value.charAt(e) == input.charAt(e);
        return IntStream.range(0, value.length())
                .filter(predicate.negate())
                .findFirst()
                .isEmpty();
    }
}

