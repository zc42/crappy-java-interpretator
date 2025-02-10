package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.tree_parser.statement.decomposer.StatementDecomposer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LocalVariableMarking {

    public static void addCodeBlockMarks(TreeNode root) {
        getCodeBlocksNodes(root).forEach(LocalVariableMarking::_addCodeBlockMarks);
    }

    private static void _addCodeBlockMarks(TreeNode node) {
        if (StatementDecomposer.isPredicateNode(node.getType()))
            addCodeBlockPushPopCommands(node, NodeType.Predicate);
        else if (node.getType() == NodeType.CodeBlock) addCodeBlockPushPopCommands(node);
        else if (node.getType() == NodeType.IfElseStatement) return;
        else if (node.getType() == NodeType.Else) return;
        else throw new RuntimeException("Add code block marks, node type: " + node.getType());
    }

    private static void addCodeBlockPushPopCommands(TreeNode parent, NodeType nodeType) {
        TreeNode child = ParseTreeNodeUtils.getChild(parent, nodeType).orElseThrow(() -> new RuntimeException("node not found, type: " + nodeType));
        child.addAsFirstChild(new TreeNode(NodeType.PUSH_CODE_BLOCK));
        child.addChild(new TreeNode(NodeType.POP_CODE_BLOCK));
    }

    private static void addCodeBlockPushPopCommands(TreeNode node) {
        node.addAsFirstChild(new TreeNode(NodeType.PUSH_CODE_BLOCK));
        node.addChild(new TreeNode(NodeType.POP_CODE_BLOCK));
    }

    private static List<TreeNode> getCodeBlocksNodes(TreeNode root) {
        List<TreeNode> nodes = new ArrayList<>();
        root.getChildren().forEach(e -> collectCodeBlocksNodes(nodes, e));
        List<TreeNode> collect = nodes.stream()
                .filter(LocalVariableMarking::filterCodeBlockNodes)
                .collect(Collectors.toList());
        return collect;
    }

    private static boolean filterCodeBlockNodes(TreeNode node) {
        if (node.getType() != NodeType.CodeBlock) return true;
        TreeNode parent = node.getParent();
        if (parent == null) return false;
        NodeType parentType = parent.getType();
        if (parentType == NodeType.Class) return false;
        if (parentType == NodeType.FunctionDeclarationStatement) return false;
        return true;
//        return !localVariableCodeBlockTypes.contains(parentType);
    }

    private final static Set<NodeType> localVariableCodeBlockTypes = Set.of(
            NodeType.WhileStatement,
            NodeType.If, NodeType.ElseIf, NodeType.Else,
            NodeType.ForStatement,
            NodeType.DoStatement,
            NodeType.CodeBlock
    );

    private static void collectCodeBlocksNodes(List<TreeNode> nodes, TreeNode node) {
        if (localVariableCodeBlockTypes.contains(node.getType())) nodes.add(node);
        node.getChildren().forEach(e -> collectCodeBlocksNodes(nodes, e));
    }
}
