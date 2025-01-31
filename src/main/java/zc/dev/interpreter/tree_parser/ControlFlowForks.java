package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Pair;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ControlFlowForks {

    public static void addControlFlowForks(ParseTreeNode root) {
        List<ParseTreeNode> forkNodes = getControlFlowForkNodes(root);
        forkNodes.forEach(ControlFlowForks::addGotoStatements);
    }

    private static void addGotoStatements(ParseTreeNode node) {
        if (node.getNodeType() == NodeType.WhileStatement) addGotoStatementsToWhile(node);
        else if (node.getNodeType() == NodeType.IfElseStatement) return;
        else throw new UnsupportedOperationException("Add goto statements, node type: " + node.getNodeType());
    }

    private static void addGotoStatementsToWhile(ParseTreeNode node) {
        ParseTreeNode predicateNode = ParseTreeNodeUtils.getChild(node, NodeType.Predicate).orElseThrow(() -> new RuntimeException("Predicate node not found"));
        ParseTreeNode codeNode = ParseTreeNodeUtils.getChild(node, NodeType.CodeBlock).orElseThrow(() -> new RuntimeException("Predicate node not found"));
        Pair<Integer, Integer> predicateNodeN = ParseTreeNodeUtils.getFirstAndLastCodeLineNumbers(predicateNode);
        Pair<Integer, Integer> codeNodeN = ParseTreeNodeUtils.getFirstAndLastCodeLineNumbers(codeNode);

        int gotoIfFalseLineNumber = codeNodeN.getValue() + 10;
        int gotIfFalseCodeLineNumber = predicateNodeN.getValue() + 1;
        int gotoLineNumber = predicateNodeN.getKey();
        int gotoCodeLineNumber = codeNodeN.getValue() + 1;

        addGoToCodeLine(predicateNode, TokenType.GOTO_IF_FALSE, gotoIfFalseLineNumber, gotIfFalseCodeLineNumber);
        addGoToCodeLine(codeNode, TokenType.GOTO, gotoLineNumber, gotoCodeLineNumber);
    }

    private static void addGoToCodeLine(ParseTreeNode node, TokenType tokenType, int lineNumber, int codeLineNumber) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");

        Token gotoToken = new Token(tokenType, tokenType.name());
        Token lineNumberToken = new Token(TokenType.NUMBER, String.valueOf(lineNumber));
        ParseTreeNode gotoNode = new ParseTreeNode(NodeType.GOTO, List.of(gotoToken, lineNumberToken));
        gotoNode.setLineNb(codeLineNumber);
        children.add(gotoNode);
    }


    private static List<ParseTreeNode> getControlFlowForkNodes(ParseTreeNode root) {
        List<ParseTreeNode> forkNodes = new ArrayList<>();
        root.getChildren().forEach(e -> getControlFlowForkNodes(forkNodes, e));
        return forkNodes;
    }

    private final static Set<NodeType> forkNodeTypes = Set.of(
            NodeType.WhileStatement,
            NodeType.IfElseStatement
    );

    private static void getControlFlowForkNodes(List<ParseTreeNode> forkNodes, ParseTreeNode node) {
        if (forkNodeTypes.contains(node.getNodeType())) forkNodes.add(node);
        node.getChildren().forEach(e -> getControlFlowForkNodes(forkNodes, e));
    }
}
