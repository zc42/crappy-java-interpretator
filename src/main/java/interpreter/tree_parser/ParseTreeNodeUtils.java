package interpreter.tree_parser;

import interpreter.Pair;
import interpreter.lexer.Token;
import interpreter.lexer.TokenType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static interpreter.Pair.P;


public class ParseTreeNodeUtils {

    public static Pair<Integer, Integer> getFirstAndLastCodeLineNumbers(ParseTreeNode node) {
        if (node.getChildren().isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");
        List<ParseTreeNode> nodes = getCodeLineNodes(node);
        return P(
                nodes.get(0).getLineNb(),
                nodes.get(nodes.size() - 1).getLineNb()
        );
    }

    public static Pair<ParseTreeNode, ParseTreeNode> getFirstAndLastCodeLineNodes(ParseTreeNode node) {
        if (node.getChildren().isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");
        List<ParseTreeNode> nodes = getCodeLineNodes(node);
        return P(
                nodes.get(0),
                nodes.get(nodes.size() - 1)
        );
    }

    private static List<ParseTreeNode> getCodeLineNodes(ParseTreeNode node) {
        return node.getChildren().stream()
                .map(ParseTreeNodeUtils::_getCodeLineNodes)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(ParseTreeNode::getLineNb))
                .collect(Collectors.toList());
    }

    private static List<ParseTreeNode> _getCodeLineNodes(ParseTreeNode node) {
        if (node.getLineNb() != null) return List.of(node);
        return node.getChildren().stream()
                .filter(e -> e.getLineNb() != null)
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
