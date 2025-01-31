package interpreter;

import lombok.*;

import java.io.Serializable;

@Getter
@ToString
@EqualsAndHashCode
@Builder(access = AccessLevel.PUBLIC)
//@RequiredArgsConstructor(staticName = "P", access = AccessLevel.PUBLIC)
public class Pair<T1, T2> implements Serializable {

    private final T1 key;
    private final T2 value;

    public static <T1, T2> Pair<T1, T2> P(T1 key, T2 value) {
        return new Pair<>(key, value);
    }
}

