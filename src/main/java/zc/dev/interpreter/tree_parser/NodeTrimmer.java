package zc.dev.interpreter.tree_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class NodeTrimmer {
    public static void removeDecomposedNodes(TreeNode node) {
        Predicate<TreeNode> predicate = NodeTrimmer::containsChildWithDecomposedStatements;
        List<TreeNode> children = ParseTreeNodeUtils.getAllChildren(node, predicate);
        children.forEach(NodeTrimmer::replaceDecomposedNodes);
    }

    private static void replaceDecomposedNodes(TreeNode node) {
        TreeNode dNode = ParseTreeNodeUtils.getChild(node, NodeType.DecomposedStatements).orElseThrow(() -> new RuntimeException("no child found"));
        TreeNode parent = node.getParent();
        List<TreeNode> children = new ArrayList<>(parent.getChildren());
        int i = children.indexOf(node);
        if (i == -1) throw new RuntimeException("no child found");
        if (children.size() == 1) {
            parent.getChildren().clear();
            parent.getChildren().addAll(dNode.getChildren());
        } else {
            List<TreeNode> l1 = children.subList(0, i + 1);
            List<TreeNode> l2 = i + 1 >= children.size() ? List.of() : new ArrayList<>(children.subList(i + 1, children.size()));
            l1.remove(i);
            l1.addAll(dNode.getChildren());
            l1.addAll(l2);
            parent.getChildren().clear();
            parent.getChildren().addAll(l1);
        }
    }

    private static boolean containsChildWithDecomposedStatements(TreeNode e) {
        return e.getChildren().stream()
                .anyMatch(x -> x.getType() == NodeType.DecomposedStatements);
    }
}
