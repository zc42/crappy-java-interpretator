package interpreter.tree_parser;

import interpreter.lexer.IterableWithPeek;
import interpreter.lexer.Token;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ParseTreeContext implements IterableWithPeek<Token> {
    private final List<Token> tokens;
    @Getter
    private int i = 0;
    @Getter
    private final ParseTreeNode rootNode = new ParseTreeNode(NodeType.Root);
    @Getter
    @Setter
    private ParseTreeNode currentNode;
    @Getter
    private final List<ParseTreeNode> annotations = new ArrayList<>();

    public static ParseTreeContext from(List<Token> tokens) {
        ParseTreeContext ctx = new ParseTreeContext(tokens);
        ctx.currentNode = ctx.rootNode;
        return ctx;
    }

    @Override
    public boolean hasNext() {
        return i < tokens.size();
    }

    @Override
    public Token peek() {
        return tokens.get(i);
    }

    @Override
    public Optional<Token> peek(int n) {
        int index = i + n;
        return index < 0 || index >= tokens.size()
                ? Optional.empty()
                : Optional.of(tokens.get(index));
    }

    @Override
    public Token next() {
        Token token = tokens.get(i);
        i++;
        return token;
    }

    public boolean containsTokenValuesInCurrentPosition(String... tokenValues) {
         String v1 = String.join(" ", tokenValues);
         String v2 = IntStream.range(i, i + tokenValues.length).boxed()
                .filter(e -> e < tokens.size())
                .map(tokens::get)
                .map(Token::getValue)
                .collect(Collectors.joining(" "));
        return v1.equals(v2);
    }

    public void saveAnnotation(ParseTreeNode node) {
        this.annotations.add(node);
    }

    public void clearAnnotations() {
        this.annotations.clear();
    }
}
