package zc.dev.interpreter.lexer;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static zc.dev.interpreter.Utils.prnt;


@Getter
@EqualsAndHashCode
public class Token {
    private final TokenType type;
    private final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public static Token KeywordToken(String value) {
        return new Token(TokenType.KEYWORD, value);
    }

    @Override
    public String toString() {
        return "Token{" + "type=" + type + ", value='" + value + '\'' + '}';
    }

    public static void prntTokens(List<Token> tokens) {
        String codeLine = toString(tokens);
        prnt(codeLine);
    }

    public static  String toString(List<Token> tokens) {
        return tokens.stream()
                .filter(e -> e.getType() != TokenType.SEMI_COLON)
//                .map(e -> MessageFormat.format("{1}({0})", e.getType(), e.getValue()))
                .map(e -> MessageFormat.format("{0}", e.getValue()))
                .collect(Collectors.joining(" "));
    }

    public static List<Token> removeSemicolon(List<Token> tokens) {
        return tokens.stream()
                .filter(e -> e.getType() != TokenType.SEMI_COLON)
                .collect(Collectors.toList());
    }

    public static List<Token> remove(List<Token> tokens, String... values) {
         List<String> list = Arrays.stream(values).collect(Collectors.toList());
        return tokens.stream()
                .filter(e -> !list.contains(e.getValue()))
                .collect(Collectors.toList());
    }

    public static void removeAt(List<Token> tokens, String value, int index) {
        if (index < 0 || index >= tokens.size()) throw new IndexOutOfBoundsException(index);
        boolean b = tokens.get(index).getValue().equals(value);
        if (b) tokens.remove(index);
    }
}
