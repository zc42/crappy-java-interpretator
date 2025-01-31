package interpreter.lexer;

import java.util.List;
import java.util.function.Predicate;

public class LexerWithFSA {

    public static List<Token> tokenize(String input) {
        TokenizerContext ctx = TokenizerContext.from(input);
        while (ctx.hasNext()) {
            char c = ctx.peek();

            if ("&".indexOf(c) != -1) {
                c = c;
            }


            if ("\n".indexOf(c) != -1) addToken(ctx, TokenType.NewLine);
            else if (Character.isWhitespace(c)) ctx.next();
            else if ("&|".indexOf(c) != -1 || Character.isLetter(c)) processIdentifierOrKeywordState(ctx);
            else if (Character.isDigit(c)) processNumberState(ctx);
            else if ("+-*/".indexOf(c) != -1) addToken(ctx, TokenType.ARITHMETIC_OPERATOR);
            else if ("!<>".indexOf(c) != -1) addToken(ctx, TokenType.BOOLEAN_OPERATOR);
            else if ("=".indexOf(c) != -1) addToken(ctx, TokenType.ASSIGNMENT);
            else if ("\"".indexOf(c) != -1) processStringState(ctx);
            else if ("()".indexOf(c) != -1) addToken(ctx, TokenType.PARENTHESES);
            else if ("{}".indexOf(c) != -1) addToken(ctx, TokenType.BRACE);
            else if (";".indexOf(c) != -1) addToken(ctx, TokenType.SEMI_COLON);
            else if ("[]".indexOf(c) != -1) addToken(ctx, TokenType.SQUARE_BRACKET);
            else if (",".indexOf(c) != -1) addToken(ctx, TokenType.COMMA);
            else if (".".indexOf(c) != -1) addToken(ctx, TokenType.DOT);
            else if ("@".indexOf(c) != -1) addToken(ctx, TokenType.Ampersand);

            else addToken(ctx, TokenType.UNKNOWN);
        }
        return ctx.getTokens();
    }

    private static void addToken(TokenizerContext ctx, TokenType tokenType) {
        char c = ctx.next();
        Token token = new Token(tokenType, Character.toString(c));
        ctx.addToken(token);
    }

    private static void processNumberState(TokenizerContext ctx) {
        StringBuilder buffer = new StringBuilder();

        Predicate<TokenizerContext> isDigit = e -> e.hasNext()
                && Character.isDigit(e.peek());

        while (isDigit.test(ctx)) {
            char c = ctx.next();
            buffer.append(c);
        }

        Token token = new Token(TokenType.NUMBER, buffer.toString());
        ctx.addToken(token);
    }

    private static final List<String> booleanOperators = List.of("&&", "||");

    private static final List<String> keywords = List.of(
            "for", "while", "if", "do", "return", "new", "case", "break", "continue",
            "try", "catch", "finally");

    private static final List<String> modifiers = List.of(
            "public", "private", "static", "final");

    private static final List<String> types = List.of(
            "int", "float", "double", "boolean", "char", "void", "byte", "short",
            "String", "Integer", "Long", "Float", "Double", "Byte", "Short",
            "Boolean", "Character", "void");

    private static void processStringState(TokenizerContext ctx) {
        StringBuilder buffer = new StringBuilder();
        if (ctx.peek() != '"') throw new RuntimeException("ctx.peek() != '\"'");
        ctx.next();

        Predicate<TokenizerContext> isQuotationMark = e -> e.peek() == '"'
                && e.peek(-1).orElse('x') != '\\';

        while (ctx.hasNext() && !isQuotationMark.test(ctx)) {
            char c = ctx.next();
            buffer.append(c);
        }

        if (!ctx.hasNext())
            throw new RuntimeException("processStringState(..) - enclosing quotation mark not found, ctx.hasNext() == false");
        ctx.next();

        String word = buffer.toString();
        Token token = new Token(TokenType.STRING, word);

        ctx.addToken(token);
    }

    private static void processIdentifierOrKeywordState(TokenizerContext ctx) {
        StringBuilder buffer = new StringBuilder();

        Predicate<TokenizerContext> isLetterOrDash = e -> e.hasNext()
                && (Character.isLetterOrDigit(e.peek())
                || "&|".indexOf(e.peek()) != -1
                || e.peek() == '_');

        while (isLetterOrDash.test(ctx)) {
            Character c = ctx.next();
            buffer.append(c);
        }

        String word = buffer.toString();
        Token token = keywords.contains(word)
                ? new Token(TokenType.KEYWORD, word)
                : modifiers.contains(word)
                ? new Token(TokenType.MODIFIER, word)
                : types.contains(word)
                ? new Token(TokenType.TYPE, word)
                : booleanOperators.contains(word)
                ? new Token(TokenType.BOOLEAN_OPERATOR, word)
                : new Token(TokenType.IDENTIFIER, word);

        ctx.addToken(token);
    }

    public static void main(String[] args) {
        String code = "" +
                "{" +
                "   String msg = \"42 + 1 \\\"kazkas cia idomaus gal\\\"\";" +
                "   int y = 42 + 1;" +
                "   int x = 42 + y;" +
                "}";
        List<Token> tokens = tokenize(code);
        tokens.forEach(System.out::println);
    }
}
