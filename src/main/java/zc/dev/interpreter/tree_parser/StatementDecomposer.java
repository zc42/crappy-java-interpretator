package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Statement;
import zc.dev.interpreter.StatementSplitter;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class StatementDecomposer {

    public static void decomposeStatements(ParseTreeNode root) {
        StatementSplitter splitter = new StatementSplitter();
        Stack<ParseTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ParseTreeNode node = stack.pop();
            node.getChildren().forEach(stack::push);
            getDecomposedStatements(node, splitter);
        }
    }

    private static void getDecomposedStatements(ParseTreeNode node, StatementSplitter splitter) {
        List<Statement> statements = splitter.split(node);
        NodeType nodeType = node.getNodeType();
        boolean predicateNode = isPredicateNode(nodeType);
        if (!predicateNode && statements.size() == 1) return;
        NodeType newNodeType = predicateNode ? NodeType.Predicate : NodeType.DecomposedStatements;
        ParseTreeNode decomposedStatements = new ParseTreeNode(newNodeType);
        statements.forEach(e -> decomposedStatements.addChild(new ParseTreeNode(e.getType(), e.getTokens())));
        node.addChild(decomposedStatements);
        if (node.getChildren().size() == 1) return;
        Comparator<ParseTreeNode> comparator = (o1, o2) -> {
            int c1 = o1.getNodeType() == newNodeType ? -1 : 1;
            int c2 = o2.getNodeType() == newNodeType ? -1 : 1;
            return c1 - c2;
        };
        node.getChildren().sort(comparator);
    }

    public static boolean isPredicateNode(NodeType nodeType) {
        List<NodeType> predicateNodeTypes = List.of(NodeType.WhileStatement, NodeType.ForStatement, NodeType.If, NodeType.ElseIf);
        return predicateNodeTypes.contains(nodeType);
    }
}
