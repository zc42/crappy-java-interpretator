package zc.dev.interpreter.tree_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TreeNodeFlattener {

    public static List<TreeNode> makeFlat(TreeNode node) {
        List<TreeNode> identity = new ArrayList<>();
        collectNodes(identity, node);
        return identity;
    }

    public static List<TreeNode> makeFlat(TreeNode node, NodeType nodeType) {
        List<TreeNode> identity = new ArrayList<>();
        collectNodes(identity, node, nodeType);
        return identity;
    }

    private static void collectNodes(List<TreeNode> identity, TreeNode node) {
        identity.add(node);
        Consumer<TreeNode> treeNodeConsumer = e -> collectNodes(identity, e);
        node.getChildren().forEach(treeNodeConsumer);
    }

    private static void collectNodes(List<TreeNode> identity, TreeNode node, NodeType nodeType) {
        if (node.getType() == nodeType) identity.add(node);
        Consumer<TreeNode> treeNodeConsumer = e -> collectNodes(identity, e, nodeType);
        node.getChildren().forEach(treeNodeConsumer);
    }
}
