package interpreter.tree_parser;

import interpreter.StatementActions;
import interpreter.lexer.Token;
import interpreter.lexer.TokenType;
import lombok.Getter;
import lombok.Setter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static interpreter.Utils.prnt;


@Getter
public class ParseTreeNode {
    private final NodeType nodeType;
    private final List<Token> tokens;
    private final List<ParseTreeNode> children;
    private ParseTreeNode parent;

    @Setter
    private Integer lineNb;
    @Setter
    private List<ParseTreeNode> executableNodes;

    @Setter
    private StatementActions statementActions;

    public ParseTreeNode(NodeType nodeType) {
        this.nodeType = nodeType;
        this.tokens = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public ParseTreeNode(NodeType nodeType, Token token) {
        this.nodeType = nodeType;
        this.tokens = new ArrayList<>();
        this.tokens.add(token);
        this.children = new ArrayList<>();
    }

    public ParseTreeNode(NodeType nodeType, List<Token> tokens) {
        this.nodeType = nodeType;
        this.tokens = tokens.stream().filter(e -> e.getType() != TokenType.NewLine).collect(Collectors.toList());
        this.children = new ArrayList<>();
    }

    public void addChild(ParseTreeNode child) {
        children.add(child);
        child.parent = this;
    }

    public void printTree(String prefix) {
        prnt(getTreeAsString(prefix));
    }

    private final static Function<Token, String> tokenValueMapper = e -> e.getType() == TokenType.STRING
            ? MessageFormat.format("\"{0}\"", e.getValue())
            : e.getValue();

    public String getTreeAsString(String prefix) {
        String tokensAsString = tokens.stream().map(tokenValueMapper).collect(Collectors.joining(" "));
//        boolean last = ParseTreeNavigator.isLast(this);
        String mes = MessageFormat.format("{3}{0} {1}: {2}\n", prefix, nodeType, tokensAsString, getLineNb() == null ? "" : getLineNb());
//        System.out.println(mes);
        StringBuilder builder = new StringBuilder();
        builder.append(mes);
        Consumer<ParseTreeNode> treeNodeConsumer = e -> builder.append(e.getTreeAsString(prefix + "    "));
        children.forEach(treeNodeConsumer);
        return builder.toString();
    }

    public void addToken(Token token) {
        if (token.getType() == TokenType.NewLine) return;
        tokens.add(token);
    }

    public Token getLastToken() {
        if (tokens.isEmpty()) return null;
        return tokens.get(tokens.size() - 1);
    }

    public void printTree() {
        printTree("");
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} | {1}", nodeType, Token.toString(tokens));
    }
}
