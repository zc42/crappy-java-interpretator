package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.StatementActions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParseTreeNumerator {

    public static void main(ParseTreeNode root) {
        Queue<ParseTreeNode> queue = new LinkedList<>();
        queue.add(root);
        int lineNb = 0;
        while (!queue.isEmpty()) {
            ParseTreeNode node = queue.poll();
            lineNb = addLineNb(node, lineNb);
            queue.addAll(node.getChildren());
        }
//            root.printTree();
    }

    private static int addLineNb(ParseTreeNode node, int lineNb) {
        node.setLineNb(lineNb);
        return lineNb + 1;
    }

    public static void addLineNumbers(ParseTreeNode root) {
        List<ParseTreeNode> nodes = new ArrayList<>();
        collectParseTreeNode(nodes, root, NodeType.FunctionDeclarationStatement);
        nodes.forEach(ParseTreeNumerator::addLineNumberToExecutableStatements);
    }

    public static void addExecutablesToFunctionDeclarationNodes(ParseTreeNode root) {
        List<ParseTreeNode> nodes = new ArrayList<>();
        collectParseTreeNode(nodes, root, NodeType.FunctionDeclarationStatement);
        nodes.forEach(ParseTreeNumerator::addExecutableStatements);
        nodes.forEach(ParseTreeNumerator::addStatementActions);
    }

    private static void addExecutableStatements(ParseTreeNode node) {
        List<ParseTreeNode> executableNodes = getNodesWithLineNb(node);
        node.setExecutableNodes(executableNodes);
    }

    private static void addStatementActions(ParseTreeNode node) {
        List<ParseTreeNode> nodes = node.getExecutableNodes();
        nodes.forEach(e -> e.setStatementActions(StatementActions.main(e)));
    }

    private static void addLineNumberToExecutableStatements(ParseTreeNode node) {
        List<ParseTreeNode> executableNodes = getExecutableNodes(node);
        IntStream.range(0, executableNodes.size()).forEach(x -> executableNodes.get(x).setLineNb(x * 10));
    }

    private static List<ParseTreeNode> getExecutableNodes(ParseTreeNode root) {
        List<ParseTreeNode> nodes = new ArrayList<>();
        collectParseTreeNode(nodes, root);
        return nodes.stream()
                .filter(ParseTreeNumerator::isNodeExecutable)
                .collect(Collectors.toList());
    }

    private static List<ParseTreeNode> getNodesWithLineNb(ParseTreeNode root) {
        List<ParseTreeNode> nodes = new ArrayList<>();
        collectParseTreeNode(nodes, root);
        return nodes.stream()
                .filter(e -> e.getLineNb() != null)
                .collect(Collectors.toList());
    }

    private static boolean isNodeExecutable(ParseTreeNode node) {
        NodeType nodeType = node.getNodeType();
        boolean hasDecomposedNodeChild = ParseTreeNodeUtils.getChild(node, NodeType.DecomposedStatements).isPresent();
        if (nodeType == NodeType.RegularStatement && !hasDecomposedNodeChild) return true;
        if (nodeType == NodeType.ReturnStatement && !hasDecomposedNodeChild) return true;
        if (nodeType == NodeType.PUSH_CODE_BLOCK) return true;
        if (nodeType == NodeType.POP_CODE_BLOCK) return true;
//        if (nodeType == NodeType.GOTO && !hasDecomposedNodeChild) return true;

        ParseTreeNode parent = node.getParent();
        if (parent == null) return false;
        NodeType parentNodeType = parent.getNodeType();
        if (parentNodeType == NodeType.DecomposedStatements) return true;

        return false;
    }

    private static void collectParseTreeNode(List<ParseTreeNode> identity, ParseTreeNode node) {
        identity.add(node);
        Consumer<ParseTreeNode> treeNodeConsumer = e -> collectParseTreeNode(identity, e);
        node.getChildren().forEach(treeNodeConsumer);
    }

    private static void collectParseTreeNode(List<ParseTreeNode> identity, ParseTreeNode node, NodeType nodeType) {
        if (node.getNodeType() == nodeType) identity.add(node);
        Consumer<ParseTreeNode> treeNodeConsumer = e -> collectParseTreeNode(identity, e, nodeType);
        node.getChildren().forEach(treeNodeConsumer);
    }
}
