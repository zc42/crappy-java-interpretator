package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Pair;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static zc.dev.interpreter.Pair.P;


public class ParseTreeNodeUtils {

    public static Pair<Integer, Integer> getFirstAndLastCodeLineNumbers(ParseTreeNode node) {
        if (node.getChildren().isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");
        Predicate<ParseTreeNode> predicate = e -> e.getLineNb() != null;
        List<ParseTreeNode> nodes = getAllChildren(node, predicate);
        Integer lineNb = nodes.getFirst().getLineNb();
        Integer lineNb1 = nodes.getLast().getLineNb();
        return P(lineNb, lineNb1);
    }

    public static FirstLastNode getFirstAndLastCodeLineNodes(ParseTreeNode node) {
        if (node.getChildren().isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");
        Predicate<ParseTreeNode> predicate = e -> e.getLineNb() != null;
        List<ParseTreeNode> nodes = getAllChildren(node, predicate);
        if (nodes.isEmpty()) throw new RuntimeException("nodes.isEmpty()");
        ParseTreeNode firstNode = nodes.getFirst();
        ParseTreeNode lastNode = nodes.getLast();
        return FirstLastNode.from(firstNode, lastNode);
    }

    public static List<ParseTreeNode> getAllChildren(ParseTreeNode node, Predicate<ParseTreeNode> predicate) {
        List<ParseTreeNode> nodes = new ArrayList<>();
        Stack<ParseTreeNode> stack = new Stack<>();
        do {
            List<ParseTreeNode> children = node.getChildren();
            children.stream()
                    .filter(predicate)
                    .forEach(nodes::add);
            stack.push(node);
            node = stack.pop();
        } while (!stack.isEmpty());

        return nodes.stream()
                .sorted(Comparator.comparingInt(ParseTreeNode::getLineNb))
                .collect(Collectors.toList());
    }

    public static Optional<ParseTreeNode> getChild(ParseTreeNode node, NodeType nodeType) {
        return node.getChildren().stream()
                .filter(e -> e.getNodeType() == nodeType)
                .findFirst();
    }

    public static boolean hasChildNode(ParseTreeNode node, Predicate<ParseTreeNode> predicate) {
        return node.getChildren().stream().anyMatch(predicate);
    }

    public static Optional<ParseTreeNode> findEntryPoint(ParseTreeNode node) {
        return node.getChildren().stream()
                .filter(e -> e.getNodeType() == NodeType.Class)
                .map(ParseTreeNode::getChildren)
                .flatMap(Collection::stream)
                .filter(e -> e.getNodeType() == NodeType.CodeBlock)
                .map(ParseTreeNode::getChildren)
                .flatMap(Collection::stream)
                .filter(e -> e.getNodeType() == NodeType.FunctionDeclarationStatement)
                .filter(e -> containsToken(e.getTokens(), new Token(TokenType.IDENTIFIER, "main")))
                .findFirst();
    }

    private static boolean containsToken(List<Token> tokens, Token token) {
        return tokens.stream().anyMatch(e -> e.equals(token));
    }
}
