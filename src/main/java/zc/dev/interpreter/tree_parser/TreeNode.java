package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.StatementActions;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import lombok.Getter;
import lombok.Setter;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static zc.dev.interpreter.Utils.prnt;

public class TreeNode {
    @Getter
    @Setter
    private NodeType type;
    @Getter
    private final List<Token> tokens;
    @Getter
    private final List<TreeNode> children;
    @Getter
    private TreeNode parent;

    @Getter
    @Setter
    private Integer lineNb;
    @Getter
    @Setter
    private List<TreeNode> executableNodes;

    @Getter
    @Setter
    private StatementActions statementActions;
    private final List<NodeType> nodeTypePath = new ArrayList<>();

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
        child.nodeTypePath.addAll(getNodeTypePath());
    }

    private List<NodeType> getNodeTypePath() {
        List<NodeType> nodeTypes = new ArrayList<>(nodeTypePath);
        nodeTypes.add(type);
        return nodeTypes;
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

    public TreeNode getRoot() {
        TreeNode node = this;
        while (node.getParent() != null) {
            node = node.getParent();
        }
        return node;
    }

    public void printNode() {
        String message = MessageFormat.format("{0}: {1}", type, Token.toString(tokens));
        prnt(message);
    }

    public Optional<TreeNode> getChildNode(List<NodeType> nodeTypePath) {
        Stack<TreeNode> stack = new Stack<>();
        stack.addAll(children);
        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            if (endsWith(node.getNodeTypePath(), nodeTypePath)) return Optional.of(node);
            stack.addAll(node.getChildren());
        }
        return Optional.empty();
    }

    private boolean endsWith(List<NodeType> nodeTypes0, List<NodeType> nodeTypes1) {
        int size0 = nodeTypes0.size();
        int size1 = nodeTypes1.size();
        if (size0 < size1) return false;
        int delta = size0 - size1;
        return IntStream.range(delta, size1).boxed()
                .filter(i -> nodeTypes0.get(i) != nodeTypes1.get(i - delta))
                .findFirst()
                .isEmpty();
    }
}
