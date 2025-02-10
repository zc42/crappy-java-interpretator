package zc.dev.interpreter.tree_parser;

import lombok.RequiredArgsConstructor;
import zc.dev.interpreter.Pair;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class ControlFlowForks {

    public static void addControlFlowForks(TreeNode root) {
        List<TreeNode> forkNodes = getControlFlowForkNodes(root);
        forkNodes.forEach(ControlFlowForks::addGotoStatements);
    }

    private static void addGotoStatements(TreeNode node) {
        if (node.getType() == NodeType.WhileStatement) addGotoStatementsToWhile(node);
        else if (node.getType() == NodeType.IfElseStatement) addGotoStatementsToIfElse(node);
        else throw new UnsupportedOperationException("Add goto statements, node type: " + node.getType());
    }

    private static void addGotoStatementsToIfElse(TreeNode parent) {

        List<TreeNode> ifElseNodes = parent.getChildren().stream()
                .filter(e -> e.getType() == NodeType.If
                        || e.getType() == NodeType.ElseIf
                        || e.getType() == NodeType.Else)
                .toList();

        List<PredicateAndCodeBlockNodes> list = ifElseNodes.stream().map(ControlFlowForks::getPredicateAndCodeBlockNodes).toList();
        PredicateAndCodeBlockNodes lastPredicateAndCodeBlockNodes = list.getLast();
        int gotoLineNumber = lastPredicateAndCodeBlockNodes.codeBlockNode.lastLineNumber + 10;
        IntStream.range(0, list.size()).boxed().forEach(i -> addGoToForElseIf(list, i, gotoLineNumber));
    }

    private static PredicateAndCodeBlockNodes getPredicateAndCodeBlockNodes(TreeNode node) {
        TreeNode predicateNode = ParseTreeNodeUtils.getChild(node, NodeType.Predicate).orElse(null);
        TreeNode codeNode = ParseTreeNodeUtils.getChild(node, NodeType.CodeBlock).orElseThrow(() -> new RuntimeException("Predicate node not found"));
        Pair<Integer, Integer> predicateNodeN = predicateNode == null ? null : ParseTreeNodeUtils.getFirstAndLastCodeLineNumbers(predicateNode);
        Pair<Integer, Integer> codeNodeN = ParseTreeNodeUtils.getFirstAndLastCodeLineNumbers(codeNode);
        NodeWithLastLineNumber a = predicateNode == null ? null : NodeWithLastLineNumber.from(predicateNode, predicateNodeN.getValue());
        NodeWithLastLineNumber b = NodeWithLastLineNumber.from(codeNode, codeNodeN.getValue());
        return PredicateAndCodeBlockNodes.from(a, b);
    }

    private static void addGoToForElseIf(List<PredicateAndCodeBlockNodes> list, int i, int gotoLineNumber) {
        PredicateAndCodeBlockNodes predicateAndCodeBlockNodes = list.get(i);
        NodeWithLastLineNumber predicateNode = predicateAndCodeBlockNodes.predicateNode;
        NodeWithLastLineNumber codeBlockNode = predicateAndCodeBlockNodes.codeBlockNode;

        //no predicator node means it is else, no need for goto statements
        if (predicateNode == null) return;

        int gotoIfFalse = codeBlockNode.lastLineNumber + 10;
        int gotoIfFalseCodeLineNumber = predicateNode.lastLineNumber + 1;
        addGoToCodeLine(predicateNode.node, TokenType.GOTO_IF_FALSE, gotoIfFalse, gotoIfFalseCodeLineNumber);

        //if one if no need goto else if or else
        if (list.size() > 1) {
            int gotoCodeLineNumber = codeBlockNode.lastLineNumber + 1;
            addGoToCodeLine(codeBlockNode.node, TokenType.GOTO, gotoLineNumber, gotoCodeLineNumber);
        }
    }

    @RequiredArgsConstructor(staticName = "from")
    static public class PredicateAndCodeBlockNodes {
        private final NodeWithLastLineNumber predicateNode;
        private final NodeWithLastLineNumber codeBlockNode;
    }

    @RequiredArgsConstructor(staticName = "from")
    static public class NodeWithLastLineNumber {
        private final TreeNode node;
        private final int lastLineNumber;
    }

    private static void addGotoStatementsToWhile(TreeNode node) {
        TreeNode predicateNode = ParseTreeNodeUtils.getChild(node, NodeType.Predicate).orElseThrow(() -> new RuntimeException("Predicate node not found"));
        TreeNode codeNode = ParseTreeNodeUtils.getChild(node, NodeType.CodeBlock).orElseThrow(() -> new RuntimeException("Predicate node not found"));
        Pair<Integer, Integer> predicateNodeN = ParseTreeNodeUtils.getFirstAndLastCodeLineNumbers(predicateNode);
        Pair<Integer, Integer> codeNodeN = ParseTreeNodeUtils.getFirstAndLastCodeLineNumbers(codeNode);

        int gotoIfFalseLineNumber = codeNodeN.getValue() + 10;
        int gotoIfFalseCodeLineNumber = predicateNodeN.getValue() + 1;
        int gotoLineNumber = predicateNodeN.getKey();
        int gotoCodeLineNumber = codeNodeN.getValue() + 1;

        addGoToCodeLine(predicateNode, TokenType.GOTO_IF_FALSE, gotoIfFalseLineNumber, gotoIfFalseCodeLineNumber);
        addGoToCodeLine(codeNode, TokenType.GOTO, gotoLineNumber, gotoCodeLineNumber);
    }

    private static void addGoToCodeLine(TreeNode node, TokenType tokenType, int lineNumber, int codeLineNumber) {
        List<TreeNode> children = node.getChildren();
        if (children.isEmpty()) throw new RuntimeException("node.getChildren().isEmpty()");

        Token gotoToken = new Token(tokenType, tokenType.name());
        Token lineNumberToken = new Token(TokenType.NUMBER, String.valueOf(lineNumber));
        TreeNode gotoNode = new TreeNode(NodeType.GOTO, List.of(gotoToken, lineNumberToken));
        gotoNode.setLineNb(codeLineNumber);
        children.add(gotoNode);
    }


    private static List<TreeNode> getControlFlowForkNodes(TreeNode root) {
        List<TreeNode> forkNodes = new ArrayList<>();
        root.getChildren().forEach(e -> getControlFlowForkNodes(forkNodes, e));
        return forkNodes;
    }

    private static void getControlFlowForkNodes(List<TreeNode> forkNodes, TreeNode node) {
        Set<NodeType> forkNodeTypes = Set.of(
                NodeType.WhileStatement,
                NodeType.IfElseStatement
        );
        if (forkNodeTypes.contains(node.getType())) forkNodes.add(node);
        node.getChildren().forEach(e -> getControlFlowForkNodes(forkNodes, e));
    }
}
