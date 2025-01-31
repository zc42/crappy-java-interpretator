package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LocalVariableMarking {

    public static void addCodeBlockMarks(ParseTreeNode root) {
        List<ParseTreeNode> forkNodes = getCodeBlocksNodes(root);
        forkNodes.forEach(LocalVariableMarking::addGotoStatements);
    }

    private static void addGotoStatements(ParseTreeNode node) {
        if (node.getNodeType() == NodeType.WhileStatement) {
            addCodeBlockPushPopCommands(node, NodeType.DecomposedStatements);
            addCodeBlockPushPopCommands(node, NodeType.CodeBlock);
            return;
        }
        if(node.getNodeType() == NodeType.IfElseStatement) return;
        throw new RuntimeException("Add goto statements, node type: " + node.getNodeType());
    }

    private static void addCodeBlockPushPopCommands(ParseTreeNode node, NodeType nodeType) {
        ParseTreeNode predicateNode = ParseTreeNodeUtils.getChild(node, nodeType).orElseThrow(() -> new RuntimeException("node not found, type: " + nodeType));
        FirstLastNode nodes = ParseTreeNodeUtils.getFirstAndLastCodeLineNodes(predicateNode);
        nodes.getFirstNode().addChild(new ParseTreeNode(NodeType.PUSH_CODE_BLOCK));
        nodes.getLastNode().addChild(new ParseTreeNode(NodeType.POP_CODE_BLOCK));
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
        return !localVariableCodeBlockTypes.contains(parentType);
    }

    private final static Set<NodeType> localVariableCodeBlockTypes = Set.of(
            NodeType.WhileStatement,
            NodeType.IfElseStatement,
            NodeType.ForStatement,
            NodeType.DoStatement,
            NodeType.CodeBlock
    );

    private static void collectCodeBlocksNodes(List<ParseTreeNode> forkNodes, ParseTreeNode node) {
        if (localVariableCodeBlockTypes.contains(node.getNodeType())) forkNodes.add(node);
        node.getChildren().forEach(e -> collectCodeBlocksNodes(forkNodes, e));
    }
}
