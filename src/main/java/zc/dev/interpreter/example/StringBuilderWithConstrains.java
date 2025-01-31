package zc.dev.interpreter.example;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class StringBuilderWithConstrains {
    private final StringBuilder stringBuilder = new StringBuilder();
    private final List<Character> constrains;

    public static StringBuilderWithConstrains from(Character... constrains) {
        List<Character> constrainsList = Arrays.stream(constrains).collect(Collectors.toList());
        return new StringBuilderWithConstrains(constrainsList);
    }

    public StringBuilderWithConstrains append(char c) {
        if (constrains.contains(c)) {
            String message = MessageFormat.format("c == {1}, postfix: {0}{1}", stringBuilder, c);
            throw new RuntimeException(message);
        }
        stringBuilder.append(c);
        return this;
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
