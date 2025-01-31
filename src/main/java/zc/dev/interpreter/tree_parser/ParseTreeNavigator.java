package zc.dev.interpreter.tree_parser;


import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.lexer.Token;

import java.util.List;
import java.util.Optional;


public class ParseTreeNavigator {

    public static Optional<ParseTreeNode> getNext(CallStackFrame frame, ParseTreeNode node) {
        List<ParseTreeNode> children = node.getParent().getChildren();
        int i = children.indexOf(node);

        boolean last = isLast(node);
        if (!last) return getNextNode(node);
        boolean isPartOfDecomposedStatements = isPartOfDecomposedStatements(node);
        if (isPartOfDecomposedStatements) {
            node = node.getParent().getParent();
        }
        boolean isWhileStatement = node.getNodeType() == NodeType.WhileStatement;
        //todo: check frame.tempValue
//        if (isWhileStatement) {
//            boolean enterCodeBlock = frame.getTempValue()
//                    .filter(e -> e instanceof Boolean)
//                    .map(e -> (Boolean) e)
//                    .filter(Boolean::booleanValue)
//                    .isPresent();
//            if (enterCodeBlock) return getFirstExecutableChildInCodeBlock(frame, node);
//            else return getNextExecutableCodeLine(frame, node);
//        }


//        node.getNodeType() =
//
//                prnt(i);
        Token.prntTokens(node.getTokens());
        return Optional.empty();
    }

    private static Optional<ParseTreeNode> getFirstExecutableChildInCodeBlock(CallStackFrame frame, ParseTreeNode node) {

        ParseTreeNode node1 = node.getChildren().get(0);
        boolean b = node1.getNodeType() == NodeType.CodeBlock;
        if (!b) return Optional.of(node1);

        return null;
    }

    public static boolean isLast(ParseTreeNode node) {
        ParseTreeNode parent = node.getParent();
        if (parent == null) return false;
        List<ParseTreeNode> children = parent.getChildren();
        return children.indexOf(node) == children.size() - 1;
    }

    public static Optional<ParseTreeNode> getNextNode(ParseTreeNode node) {
        ParseTreeNode parent = node.getParent();
        if (parent == null) return Optional.empty();
        List<ParseTreeNode> children = parent.getChildren();
        int index = children.indexOf(node);
        return Optional.of(children.get(index + 1));
    }

    public static boolean isPartOfDecomposedStatements(ParseTreeNode node) {
        ParseTreeNode parent = node.getParent();
        return parent != null && parent.getNodeType() == NodeType.DecomposedStatements;
    }
}
