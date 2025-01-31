package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Statement;
import zc.dev.interpreter.StatementSplitter;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class StatementDecomposer {

    public static void decomposeStatements(ParseTreeNode root) {
        Stack<ParseTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ParseTreeNode node = stack.pop();
            node.getChildren().forEach(stack::push);
            getDecomposedStatements(node);
        }
    }

    private static void getDecomposedStatements(ParseTreeNode node) {
        List<Statement> statements = StatementSplitter.split(node);
        if (statements.size() == 1) return;
        List<NodeType> predicateNodeTypes = List.of(NodeType.WhileStatement, NodeType.IfElseStatement, NodeType.ForStatement);
        NodeType nodeType = predicateNodeTypes.contains(node.getNodeType())
                ? NodeType.Predicate
                : NodeType.DecomposedStatements;
        ParseTreeNode decomposedStatements = new ParseTreeNode(nodeType);
        statements.forEach(e -> decomposedStatements.addChild(new ParseTreeNode(e.getType(), e.getTokens())));
        node.addChild(decomposedStatements);
        if (node.getChildren().size() == 1) return;
        Comparator<ParseTreeNode> comparator = (o1, o2) -> {
            int c1 = o1.getNodeType() == nodeType ? -1 : 1;
            int c2 = o2.getNodeType() == nodeType ? -1 : 1;
            return c1 - c2;
        };
        node.getChildren().sort(comparator);
    }
}
