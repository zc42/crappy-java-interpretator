package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.StatementActions;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParseTreeNumerator {

    public static void main(TreeNode root) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        int lineNb = 0;
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            lineNb = addLineNb(node, lineNb);
            queue.addAll(node.getChildren());
        }
//            root.printTree();
    }

    private static int addLineNb(TreeNode node, int lineNb) {
        node.setLineNb(lineNb);
        return lineNb + 1;
    }

    public static void addLineNumbers(TreeNode root) {
        NodeType nodeType = NodeType.FunctionDeclarationStatement;
        TreeNodeFlattener.makeFlat(root, nodeType).forEach(ParseTreeNumerator::addLineNumberToExecutableStatements);
    }

    public static void addExecutablesToFunctionDeclarationNodes(TreeNode root) {
        NodeType nodeType = NodeType.FunctionDeclarationStatement;
        List<TreeNode> nodes = TreeNodeFlattener.makeFlat(root, nodeType);
        nodes.forEach(ParseTreeNumerator::addExecutableStatements);
        nodes.forEach(ParseTreeNumerator::addStatementActions);
    }

    private static void addExecutableStatements(TreeNode node) {
        List<TreeNode> executableNodes = getNodesWithLineNb(node);
        node.setExecutableNodes(executableNodes);
    }

    private static void addStatementActions(TreeNode node) {
        List<TreeNode> nodes = node.getExecutableNodes();
        nodes.forEach(e -> e.setStatementActions(StatementActions.main(e)));
    }

    private static void addLineNumberToExecutableStatements(TreeNode node) {
        List<TreeNode> executableNodes = getExecutableNodes(node);
        IntStream.range(0, executableNodes.size()).forEach(x -> executableNodes.get(x).setLineNb(x * 10));
    }

    private static List<TreeNode> getExecutableNodes(TreeNode root) {
        return TreeNodeFlattener.makeFlat(root).stream()
                .filter(ParseTreeNumerator::isNodeExecutable)
                .collect(Collectors.toList());
    }

    private static List<TreeNode> getNodesWithLineNb(TreeNode root) {
        return TreeNodeFlattener.makeFlat(root).stream()
                .filter(e -> e.getLineNb() != null)
                .collect(Collectors.toList());
    }

    private static boolean isNodeExecutable(TreeNode node) {
        NodeType nodeType = node.getType();
        boolean hasDecomposedNodeChild = ParseTreeNodeUtils.getChild(node, NodeType.DecomposedStatements).isPresent();
        if (nodeType == NodeType.RegularStatement && !hasDecomposedNodeChild) return true;
        if (nodeType == NodeType.ReturnStatement && !hasDecomposedNodeChild) return true;
        if (nodeType == NodeType.PUSH_CODE_BLOCK) return true;
        if (nodeType == NodeType.POP_CODE_BLOCK) return true;
//        if (nodeType == NodeType.GOTO && !hasDecomposedNodeChild) return true;

        TreeNode parent = node.getParent();
        if (parent == null) return false;
        NodeType parentNodeType = parent.getType();
        if (parentNodeType == NodeType.DecomposedStatements) return true;
        if (parentNodeType == NodeType.Predicate) return true;

        return false;
    }

}
