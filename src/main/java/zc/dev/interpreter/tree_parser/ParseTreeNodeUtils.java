package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Pair;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.*;
import java.util.function.Predicate;

import static zc.dev.interpreter.Pair.P;


public class ParseTreeNodeUtils {

    public static Pair<Integer, Integer> getFirstAndLastCodeLineNumbers(TreeNode node) {
        if (node.getChildren().isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");
        Predicate<TreeNode> predicate = e -> e.getLineNb() != null;
        List<TreeNode> nodes = getAllChildren(node, predicate);
        Integer lineNb = nodes.getFirst().getLineNb();
        Integer lineNb1 = nodes.getLast().getLineNb();
        return P(lineNb, lineNb1);
    }

    public static FirstLastNode getFirstAndLastCodeLineNodes(TreeNode node) {
        if (node.getChildren().isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");
        Predicate<TreeNode> predicate = e -> e.getLineNb() != null;
        List<TreeNode> nodes = getAllChildren(node, predicate);
        if (nodes.isEmpty()) throw new RuntimeException("nodes.isEmpty()");
        TreeNode firstNode = nodes.getFirst();
        TreeNode lastNode = nodes.getLast();
        return FirstLastNode.from(firstNode, lastNode);
    }

    public static List<TreeNode> getAllChildren(TreeNode root, Predicate<TreeNode> predicate) {
        List<TreeNode> nodes = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) accumNodes(nodes, stack, predicate);
        return nodes;
    }

    private static void accumNodes(List<TreeNode> nodes, Stack<TreeNode> stack, Predicate<TreeNode> predicate) {
        TreeNode node = stack.pop();
        if (predicate.test(node)) nodes.add(node);
        List<TreeNode> children = new ArrayList<>(node.getChildren());
        Collections.reverse(children);
        children.forEach(stack::push);
    }

    public static Optional<TreeNode> getChild(TreeNode node, NodeType nodeType) {
        return node.getChildren().stream()
                .filter(e -> e.getType() == nodeType)
                .findFirst();
    }

    public static boolean hasChildNode(TreeNode node, Predicate<TreeNode> predicate) {
        return node.getChildren().stream().anyMatch(predicate);
    }

    public static Optional<TreeNode> findEntryPoint(TreeNode node) {
        return node.getChildren().stream()
                .filter(e -> e.getType() == NodeType.Class)
                .map(TreeNode::getChildren)
                .flatMap(Collection::stream)
                .filter(e -> e.getType() == NodeType.CodeBlock)
                .map(TreeNode::getChildren)
                .flatMap(Collection::stream)
                .filter(e -> e.getType() == NodeType.FunctionDeclarationStatement)
                .filter(e -> containsToken(e.getTokens(), new Token(TokenType.IDENTIFIER, "main")))
                .findFirst();
    }

    private static boolean containsToken(List<Token> tokens, Token token) {
        return tokens.stream().anyMatch(e -> e.equals(token));
    }
}
