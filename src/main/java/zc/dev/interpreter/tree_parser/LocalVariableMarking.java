package zc.dev.interpreter.tree_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LocalVariableMarking {

    public static void addCodeBlockMarks(ParseTreeNode root) {
        getCodeBlocksNodes(root).forEach(LocalVariableMarking::_addCodeBlockMarks);
    }

    private static void _addCodeBlockMarks(ParseTreeNode node) {
        if (node.getNodeType() == NodeType.WhileStatement)
            addCodeBlockPushPopCommands(node, NodeType.Predicate);
        else if (node.getNodeType() == NodeType.CodeBlock) addCodeBlockPushPopCommands(node);
        else if (node.getNodeType() == NodeType.IfElseStatement) return;
        else throw new RuntimeException("Add code block marks, node type: " + node.getNodeType());
    }

    private static void addCodeBlockPushPopCommands(ParseTreeNode node, NodeType nodeType) {
        ParseTreeNode parentNode = ParseTreeNodeUtils.getChild(node, nodeType).orElseThrow(() -> new RuntimeException("node not found, type: " + nodeType));
        parentNode.addAsFirstChild(new ParseTreeNode(NodeType.PUSH_CODE_BLOCK));
        parentNode.addChild(new ParseTreeNode(NodeType.POP_CODE_BLOCK));
    }

    private static void addCodeBlockPushPopCommands(ParseTreeNode node) {
        node.addAsFirstChild(new ParseTreeNode(NodeType.PUSH_CODE_BLOCK));
        node.addChild(new ParseTreeNode(NodeType.POP_CODE_BLOCK));
    }

    private static List<ParseTreeNode> getCodeBlocksNodes(ParseTreeNode root) {
        List<ParseTreeNode> nodes = new ArrayList<>();
        root.getChildren().forEach(e -> collectCodeBlocksNodes(nodes, e));
        return nodes.stream()
                .filter(LocalVariableMarking::filterCodeBlockNodes)
                .collect(Collectors.toList());
    }

    private static boolean filterCodeBlockNodes(ParseTreeNode node) {
        if (node.getNodeType() != NodeType.CodeBlock) return true;
        ParseTreeNode parent = node.getParent();
        if (parent == null) return false;
        NodeType parentType = parent.getNodeType();
        if (parentType == NodeType.Class) return false;
        if (parentType == NodeType.FunctionDeclarationStatement) return false;
        return true;
//        return !localVariableCodeBlockTypes.contains(parentType);
    }

    private final static Set<NodeType> localVariableCodeBlockTypes = Set.of(
            NodeType.WhileStatement,
            NodeType.IfElseStatement,
            NodeType.ForStatement,
            NodeType.DoStatement,
            NodeType.CodeBlock
    );

    private static void collectCodeBlocksNodes(List<ParseTreeNode> nodes, ParseTreeNode node) {
        if (localVariableCodeBlockTypes.contains(node.getNodeType())) nodes.add(node);
        node.getChildren().forEach(e -> collectCodeBlocksNodes(nodes, e));
    }
}
