package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.StatementActions;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import lombok.Getter;
import lombok.Setter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static zc.dev.interpreter.Utils.prnt;


@Getter
public class TreeNode {
    @Setter
    private NodeType type;
    private final List<Token> tokens;
    private final List<TreeNode> children;
    private TreeNode parent;

    @Setter
    private Integer lineNb;
    @Setter
    private List<TreeNode> executableNodes;

    @Setter
    private StatementActions statementActions;

    public TreeNode(NodeType type) {
        this.type = type;
        this.tokens = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public TreeNode(NodeType type, Token token) {
        this.type = type;
        this.tokens = new ArrayList<>();
        this.tokens.add(token);
        this.children = new ArrayList<>();
    }

    public TreeNode(NodeType type, List<Token> tokens) {
        this.type = type;
        this.tokens = tokens.stream().filter(e -> e.getType() != TokenType.NewLine).collect(Collectors.toList());
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
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
        String mes = MessageFormat.format("{3}{0} {1}: {2}\n", prefix, type, tokensAsString, getLineNb() == null ? "" : getLineNb());
//        System.out.println(mes);
        StringBuilder builder = new StringBuilder();
        builder.append(mes);
        Consumer<TreeNode> treeNodeConsumer = e -> builder.append(e.getTreeAsString(prefix + "    "));
        children.forEach(treeNodeConsumer);
        return builder.toString();
    }

    public void addToken(Token token) {
        if (token.getType() == TokenType.NewLine) return;
        tokens.add(token);
    }

    public Token getLastToken() {
        if (tokens.isEmpty()) return null;
        return tokens.getLast();
    }

    public void printTree() {
        printTree("");
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} | {1}", type, Token.toString(tokens));
    }

    public void addAsFirstChild(TreeNode node) {
        List<TreeNode> _children = new ArrayList<>(children);
        children.clear();
        children.add(node);
        children.addAll(_children);
    }

    public Token getToken(int index) {
        return tokens.get(index);
    }
}
