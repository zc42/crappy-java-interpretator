package zc.dev.interpreter.tree_parser.statement.decomposer;

import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.TreeNode;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class StatementDecomposer {

    public static void decomposeStatements(TreeNode root) {
        StatementSplitter splitter = new StatementSplitter();
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            node.getChildren().forEach(stack::push);
            getDecomposedStatements(node, splitter);
        }
    }

    private static void getDecomposedStatements(TreeNode node, StatementSplitter splitter) {
        List<TreeNode> statements = splitter.split(node);
        NodeType nodeType = node.getType();
        boolean predicateNode = isPredicateNode(nodeType);
        if (!predicateNode && statements.size() == 1) return;
        NodeType newNodeType = predicateNode ? NodeType.Predicate : NodeType.DecomposedStatements;
        TreeNode decomposedStatements = new TreeNode(newNodeType);
        statements.forEach(e -> decomposedStatements.addChild(new TreeNode(e.getType(), e.getTokens())));
        node.addChild(decomposedStatements);
        if (node.getChildren().size() == 1) return;
        Comparator<TreeNode> comparator = (o1, o2) -> {
            int c1 = o1.getType() == newNodeType ? -1 : 1;
            int c2 = o2.getType() == newNodeType ? -1 : 1;
            return c1 - c2;
        };
        node.getChildren().sort(comparator);
    }

    public static boolean isPredicateNode(NodeType nodeType) {
        List<NodeType> predicateNodeTypes = List.of(NodeType.WhileStatement, NodeType.ForStatement, NodeType.If, NodeType.ElseIf);
        return predicateNodeTypes.contains(nodeType);
    }
}
