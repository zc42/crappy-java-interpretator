package zc.dev.interpreter.tree_parser.statement.decomposer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
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

        treeNode = transformToIfElseTree(treeNode);
        fixNodeTokens(treeNode);

        TreeNode assignmentNode = nodes.getFirst();
        addAssignment(assignmentNode, treeNode);
        assignmentNode.printTree();
        treeNode.printTree();

        List<TreeNode> result = assignmentNode.getTokens().size() > 1
                ? List.of(assignmentNode, treeNode)
                : List.of(treeNode);

        return Optional.of(result);
    }

    private void addAssignment(TreeNode assigmentNode, TreeNode ifElseNode) {
        Token identifier = assigmentNode.getTokens().getLast();
        if (identifier.getType() != TokenType.IDENTIFIER)
            throw new RuntimeException("identifier is not identifier: " + identifier);
        addAssignment(ifElseNode, identifier);
    }

    private void addAssignment(TreeNode node, Token identifier) {
        node.getChildren().forEach(child -> addAssignment(child, identifier));

        boolean b1 = node.getType() == NodeType.RegularStatement;
        boolean b2 = node.getParent() != null && node.getParent().getType() == NodeType.CodeBlock;
        boolean b3 = b2 && node.getParent().getParent() == null || b2 && node.getParent().getParent().getType() != NodeType.Predicate;

        boolean addAssignment = b1 && b2 && b3;
        if (!addAssignment) return;

        List<Token> tokens = node.getTokens();
        List<Token> nodeTokens = new ArrayList<>(tokens);
        tokens.clear();
        tokens.add(identifier);
        tokens.add(new Token(TokenType.ASSIGNMENT, "="));
        tokens.addAll(nodeTokens);
    }

    private void fixNodeTokens(TreeNode node) {
        node.getChildren().forEach(this::fixNodeTokens);
        NodeType nodeType = node.getType();
        if (nodeType == NodeType.Else || nodeType == NodeType.IfElseStatement) node.getTokens().clear();
        else if (nodeType == NodeType.If) {
            node.getTokens().clear();
            node.getTokens().addAll(getPredicateStatement(node));
        }
    }

    private Collection<Token> getPredicateStatement(TreeNode node) {
        TreeNode predicateNode = node.getChildren().stream().filter(e -> e.getType() == NodeType.Predicate).findFirst().orElseThrow();
        TreeNode codeBlockNode = predicateNode.getChildren().stream().filter(e -> e.getType() == NodeType.CodeBlock).findFirst().orElseThrow();
        return codeBlockNode.getChildren().getFirst().getTokens();
    }

    private TreeNode transformToIfElseTree(TreeNode node) {

        List<TreeNode> children = node.getChildren().isEmpty()
                ? List.of()
                : node.getChildren().stream().map(this::transformToIfElseTree).toList();

        NodeType nodeType = node.getType();
        TreeNode newNode = new TreeNode(nodeType, node.getTokens());
        children.forEach(newNode::addChild);

        List<Token> predicateTokens = node.getParent() == null ? newNode.getTokens() : node.getParent().getTokens();
        NodeType parentNodeType = node.getParent() == null ? NodeType.UNKNOWN : node.getParent().getType();
        if (newNode.getChildren().isEmpty()) {
            if (nodeType == NodeType.If) {
                addPredicateNode(newNode, predicateTokens);
                addStatementNode(newNode, false);
                if (parentNodeType != NodeType.IfElseStatement) addIfElse(newNode);
            } else if (nodeType == NodeType.Else) {
                addStatementNode(newNode, false);
            } else throw new RuntimeException("node type error: " + newNode);
        } else {
            if (nodeType == NodeType.If) {// || nodeType == NodeType.IfElseStatement) {
                addPredicateNode(newNode, predicateTokens);
                addStatementNode(newNode, true);
                addIfElse(newNode);
                adjustChildIfElseStatement(newNode);
            } else if (nodeType == NodeType.Else) {
                moveElseUnderIfElseStatement(newNode);
            } else if (nodeType == NodeType.IfElseStatement) return newNode;
            else throw new RuntimeException("node type error: " + newNode);
        }
        node.printNode();
        newNode.printTree();
        prnt("---");
        return newNode;
    }

    private void adjustChildIfElseStatement(TreeNode node) {
        List<NodeType> pathToElse = List.of(NodeType.If, NodeType.CodeBlock, NodeType.Else);
        List<NodeType> pathToIfElse = List.of(NodeType.If, NodeType.CodeBlock, NodeType.IfElseStatement);
        Consumer<TreeNode> addElseAsChild = e -> node.getChildNode(pathToElse).ifPresent(child -> {
            child.getParent().getChildren().remove(child);
            e.addChild(child);
        });
        node.getChildNode(pathToIfElse).ifPresent(addElseAsChild);
    }

    private static void addIfElse(TreeNode node) {
        TreeNode ifStatement = new TreeNode(NodeType.If);
        node.getChildren().forEach(ifStatement::addChild);
        node.getChildren().removeAll(ifStatement.getChildren());
        node.addChild(ifStatement);
        node.setType(NodeType.IfElseStatement);
    }

    private void moveElseUnderIfElseStatement(TreeNode node) {
        TreeNode ifElseStatement = node.getChildren().stream().filter(e -> e.getType() == NodeType.IfElseStatement).findFirst().orElseThrow();
        TreeNode elseStatement = node.getChildren().stream().filter(e -> e.getType() == NodeType.Else).findFirst().orElseThrow();
        node.getChildren().remove(elseStatement);
        ifElseStatement.addChild(elseStatement);
    }

    private static void addStatementNode(TreeNode node, boolean hasChildren) {
        TreeNode codeBlock = new TreeNode(NodeType.CodeBlock);
        if (hasChildren) {
            node.getChildren().stream()
                    .filter(e -> e.getType() != NodeType.Predicate)
                    .forEach(codeBlock::addChild);
        } else {
            TreeNode statement = new TreeNode(NodeType.RegularStatement, node.getTokens());
            codeBlock.addChild(statement);
        }
        node.getChildren().removeAll(codeBlock.getChildren());
        node.addChild(codeBlock);
//        node.getTokens().clear();
    }

    private static void addPredicateNode(TreeNode node, List<Token> parentNodeTokens) {
        TreeNode predicateNode = new TreeNode(NodeType.Predicate);
        TreeNode codeBlock1 = new TreeNode(NodeType.CodeBlock);
        TreeNode statement1 = new TreeNode(NodeType.RegularStatement, parentNodeTokens);
        predicateNode.addChild(codeBlock1);
        codeBlock1.addChild(statement1);
        node.addChild(predicateNode);
    }

    private List<TreeNode> splitAssignmentAndWhatsLeft(List<Token> tokens) {
        int index = getAssignmentIndex(tokens);
        if (index == -1) throw new RuntimeException("Assignment not found");
        List<Token> tokens1 = tokens.subList(0, index);
        List<Token> tokens2 = tokens.subList(index + 1, tokens.size());
        return List.of(
                new TreeNode(NodeType.RegularStatement, tokens1),
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
        AtomicReference<TreeNode> nodeReference = new AtomicReference<>(root);
        IntStream.range(1, nodes.size()).boxed().forEach(e -> constructTree(nodeReference, nodes, e));
        return nodeReference.get().getRoot();
    }

    @Getter
    @ToString
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    public static class TreeNodePredicate {
        private final TreeNode node;
        private final boolean childNode;
        private final boolean childOfPrevParent;

        public static TreeNodePredicate from(List<TreeNode> nodes, int index) {
            TreeNode node02 = index - 2 >= 0 ? nodes.get(index - 2) : null;
            TreeNode node01 = nodes.get(index - 1);

            boolean ___if = node02 != null && (node02.getType() == NodeType.If || node02.getType() == NodeType.IfElseStatement);
            boolean isLastRoot = node01.getType() == NodeType.IfElseStatement;
            boolean __if = isLastRoot || node01.getType() == NodeType.If;

            TreeNode node = nodes.get(index);
            boolean _if = node.getType() == NodeType.If;

            boolean b1 = !___if && __if && _if;
            boolean b2 = ___if && __if && !_if;
            boolean b3 = ___if && !__if && !_if;
            boolean b4 = !__if && _if;

            boolean isChildNode = isLastRoot || b1 || b4;
            boolean isChildOfPrevParent = b2 || b3;

            return TreeNodePredicate.of(node, isChildNode, isChildOfPrevParent);
        }
    }

    private void constructTree(AtomicReference<TreeNode> parentNodeReference, List<TreeNode> nodes, int index) {
        TreeNodePredicate predicate = TreeNodePredicate.from(nodes, index);
        TreeNode parent = parentNodeReference.get();
        TreeNode node = predicate.node;

        if (predicate.isChildNode()) {
            parent.addChild(node);
            parentNodeReference.set(node);
        } else if (predicate.isChildOfPrevParent()) {
            parent = getParentWithOneChild(parent);
            parent.addChild(node);
            parentNodeReference.set(node);
        } else {
            throw new RuntimeException("not implemented: " + predicate);
        }
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
