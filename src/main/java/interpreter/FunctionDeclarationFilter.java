package interpreter;

import interpreter.call_stack.CallStackFrame;
import interpreter.lexer.Token;
import interpreter.lexer.TokenType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
class FunctionDeclarationFilter {
    private final Token name;
    private final List<Token> argumentTypes;

    public static FunctionDeclarationFilter from(List<Token> tokens) {
        return from(null, tokens);
    }

    public static FunctionDeclarationFilter from(CallStackFrame frame, List<Token> tokens) {
        Token name = getName(tokens);
        List<Token> argumentTypes = getArgumentTypes(frame, tokens);
        return new FunctionDeclarationFilter(name, argumentTypes);
    }

    private static List<Token> getArgumentTypes(CallStackFrame frame, List<Token> tokens) {
        int start = tokens.indexOf(new Token(TokenType.PARENTHESES, "("));
        if (start == -1) throw new RuntimeException("could not find argument types: indexes.size() != 2");
        return tokens.stream()
                .skip(start)
                .map(FunctionDeclarationFilter::numberToInt)
                .map(FunctionDeclarationFilter::textToString)
                .map(e -> frame == null ? e : getVariableType(frame, e))
                .filter(e -> e.getType() == TokenType.TYPE)
                .collect(Collectors.toList());
    }

    private static Token getVariableType(CallStackFrame frame, Token token) {
        return token.getType() == TokenType.IDENTIFIER
                ? new Token(TokenType.TYPE, frame.getVariableType(token.getValue()))
                : token;
    }

    private static Token textToString(Token token) {
        return token.getType() == TokenType.STRING
                ? new Token(TokenType.TYPE, "String")
                : token;
    }

    private static Token numberToInt(Token token) {
        return token.getType() == TokenType.NUMBER
                ? new Token(TokenType.TYPE, "int")
                : token;
    }

    private static Token getName(List<Token> tokens) {
        Predicate<Integer> predicate = e -> e + 1 < tokens.size()
                && tokens.get(e).getType() == TokenType.IDENTIFIER
                && tokens.get(e + 1).getValue().equals("(");

        return IntStream.range(0, tokens.size()).boxed()
                .filter(predicate)
                .findFirst()
                .map(tokens::get)
                .orElseThrow(() -> new RuntimeException("could not find name token"));
    }
}
