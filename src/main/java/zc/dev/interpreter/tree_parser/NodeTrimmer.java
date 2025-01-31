package zc.dev.interpreter.tree_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class NodeTrimmer {
    public static void removeDecomposedNodes(ParseTreeNode node) {
        Predicate<ParseTreeNode> predicate = NodeTrimmer::containsChildWithDecomposedStatements;
        List<ParseTreeNode> children = ParseTreeNodeUtils.getAllChildren(node, predicate);
        children.forEach(NodeTrimmer::replaceDecomposedNodes);
    }

    private static void replaceDecomposedNodes(ParseTreeNode node) {
        ParseTreeNode dNode = ParseTreeNodeUtils.getChild(node, NodeType.DecomposedStatements).orElseThrow(() -> new RuntimeException("no child found"));
        ParseTreeNode parent = node.getParent();
        List<ParseTreeNode> children = new ArrayList<>(parent.getChildren());
        int i = children.indexOf(node);
        if (i == -1) throw new RuntimeException("no child found");
        if (children.size() == 1) {
            parent.getChildren().clear();
            parent.getChildren().addAll(dNode.getChildren());
        } else {
            List<ParseTreeNode> l1 = children.subList(0, i + 1);
            List<ParseTreeNode> l2 = i + 1 >= children.size() ? List.of() : new ArrayList<>(children.subList(i + 1, children.size()));
            l1.remove(i);
            l1.addAll(dNode.getChildren());
            l1.addAll(l2);
            parent.getChildren().clear();
            parent.getChildren().addAll(l1);
        }
    }

    private static boolean containsChildWithDecomposedStatements(ParseTreeNode e) {
        return e.getChildren().stream()
                .anyMatch(x -> x.getNodeType() == NodeType.DecomposedStatements);
    }
}
