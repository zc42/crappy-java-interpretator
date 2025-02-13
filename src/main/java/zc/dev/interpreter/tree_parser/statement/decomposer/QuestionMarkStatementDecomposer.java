package zc.dev.interpreter.tree_parser.statement.decomposer;

import zc.dev.interpreter.lexer.LexerWithFSA;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.TreeNode;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static zc.dev.interpreter.Utils.prnt;

public class QuestionMarkStatementDecomposer {
    private int systemVariableNb;

    public static void main(String[] args) {
        List<String> lines = List.of(
//                "b = 1 + a(a + 1, a(1, 2));",
//                "b > 0 || b + 1 > 0 || a(1)",
//                "int c = a ( b + 1 , a ( 1 , 2 ) ) + 1"
                "boolean c = a%2==0",
                "boolean c = 0==a%2"
        );

        QuestionMarkStatementDecomposer splitter = new QuestionMarkStatementDecomposer();
        lines.forEach(splitter::decompose);
    }

    public Optional<List<TreeNode>> decompose(String line) {
        List<Token> tokens = LexerWithFSA.tokenize(line);
        Token.prntTokens(tokens);
        Optional<List<TreeNode>> nodesOption = decompose(tokens);
        nodesOption.ifPresent(QuestionMarkStatementDecomposer::prntInfo);
        return nodesOption;
    }

    private static void prntInfo(List<TreeNode> nodes) {
        Consumer<TreeNode> nodeConsumer = e -> prnt(MessageFormat.format("{0} {1}", e.getType(), Token.toString(e.getTokens())));
        nodes.forEach(nodeConsumer);
    }

    private Optional<List<TreeNode>> decompose(List<Token> tokens) {
        Set<Token> tokenSet = new HashSet<>(tokens);
        boolean hasQuestionMark = tokenSet.contains(new Token(TokenType.QUETION_MARK, "?"));
        boolean hasColon = tokenSet.contains(new Token(TokenType.COLON, ":"));
        boolean startsWithAssignment = startsWithAssignment(tokens);
        boolean b = startsWithAssignment && hasQuestionMark && hasColon;
        if (!b) return Optional.empty();

        List<TreeNode> nodes = splitAssignmentAndWhatsLeft(tokens);
        List<TreeNode> statementNodes = splitToStatements(nodes.get(1).getTokens());
        TreeNode treeNode = createPlainTree(statementNodes);
        treeNode.printTree();

        throw new RuntimeException("not done yet");
    }

    private List<TreeNode> splitAssignmentAndWhatsLeft(List<Token> tokens) {
        int index = getAssignmentIndex(tokens);
        if (index == -1) throw new RuntimeException("Assignment not found");
        List<Token> tokens1 = tokens.subList(0, index);
        List<Token> tokens2 = tokens.subList(index + 1, tokens.size());
        return List.of(
                new TreeNode(NodeType.UNKNOWN, tokens1),
                new TreeNode(NodeType.UNKNOWN, tokens2));
    }

    private static int getAssignmentIndex(List<Token> tokens) {
        TokenTester tokenTester = TokenTester.from(tokens, 0);
        TokenPredicate tp0 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER);
        TokenPredicate tp1 = TokenPredicate.from(1, e -> e.getType() == TokenType.ASSIGNMENT);
        if (tokenTester.testToken(tp0, tp1)) return 1;

        TokenPredicate tp_0 = TokenPredicate.from(0, e -> e.getType() == TokenType.TYPE);
        TokenPredicate tp_1 = TokenPredicate.from(1, e -> e.getType() == TokenType.IDENTIFIER);
        TokenPredicate tp_2 = TokenPredicate.from(2, e -> e.getType() == TokenType.ASSIGNMENT);

        return tokenTester.testToken(tp_0, tp_1, tp_2) ? 2 : -1;
    }

    private List<TreeNode> splitToStatements(List<Token> tokens) {
        List<TreeNode> nodes = new ArrayList<>();
        tokens.forEach(token -> accumStatements(nodes, token));
        return nodes;
    }

    private void accumStatements(List<TreeNode> nodes, Token token) {
        boolean isQuestionMark = token.getType() == TokenType.QUETION_MARK;
        boolean isColon = token.getType() == TokenType.COLON;
        boolean isSeparator = isQuestionMark || isColon;

        if (!nodes.isEmpty() && isSeparator) {
            NodeType nodeType = isQuestionMark ? NodeType.If : NodeType.Else;
            TreeNode node = new TreeNode(nodeType);
            nodes.add(node);
        } else {
            TreeNode node;
            if (nodes.isEmpty()) {
                node = new TreeNode(NodeType.IfElseStatement);
                nodes.add(node);
            } else {
                node = nodes.getLast();
            }
            node.addToken(token);
        }
    }

    private TreeNode createPlainTree(List<TreeNode> nodes) {
        TreeNode root = nodes.getFirst();
        AtomicReference<TreeNode> reference = new AtomicReference<>(root);
        IntStream.range(1, nodes.size()).boxed().forEach(e -> {

            root.printTree();

            TreeNode node02 = e - 2 >= 0 ? nodes.get(e - 2) : null;
            TreeNode node01 = nodes.get(e - 1);

            boolean ___if = node02 != null && (node02.getType() == NodeType.If || node02.getType() == NodeType.IfElseStatement);
            boolean isLastRoot = node01.getType() == NodeType.IfElseStatement;
            boolean __if = isLastRoot || node01.getType() == NodeType.If;

            TreeNode node = nodes.get(e);
            boolean _if = node.getType() == NodeType.If;

            TreeNode parent = reference.get();

            boolean b1 = !___if && __if && _if;
            boolean b2 = ___if && __if && !_if;
            boolean b3 = ___if && !__if && !_if;
            boolean b4 = !__if && _if;

            boolean isChildNode = isLastRoot || b1 || b4;
            boolean belongsToPrevParent = b2 || b3;

            if (isChildNode) {
                parent.addChild(node);
                reference.set(node);
            } else if (belongsToPrevParent) {
                parent = getParentWithOneChild(parent);
                parent.addChild(node);
                reference.set(node);
            } else {
                throw new RuntimeException("not implemented: " + node);
            }
        });

        TreeNode last = reference.get();
        while (last.getParent() != null) {
            last = last.getParent();
        }

        return last;
    }

    private TreeNode getParentWithOneChild(TreeNode node) {
        while (node.getChildren().size() != 1) {
            node = node.getParent();
        }
        return node;
    }

    private static boolean startsWithAssignment(List<Token> tokens) {
        return getAssignmentIndex(tokens) != -1;
    }

}
