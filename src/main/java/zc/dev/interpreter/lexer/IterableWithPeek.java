package zc.dev.interpreter.lexer;

import java.util.Optional;

public interface IterableWithPeek<T> {
    boolean hasNext();

    T peek();

    Optional<T> peek(int i);

    T next();
}
