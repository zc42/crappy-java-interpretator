package zc.dev.interpreter.tree_parser.statement.decomposer;

import zc.dev.interpreter.StatementActions;
import zc.dev.interpreter.lexer.LexerWithFSA;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.TreeNode;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static zc.dev.interpreter.Utils.prnt;

public class StatementSplitter {
    private int systemVariableNb;

    public static void main(String[] args) {
        List<String> lines = List.of(
//                "b = 1 + a(a + 1, a(1, 2));",
//                "b > 0 || b + 1 > 0 || a(1)",
//                "int c = a ( b + 1 , a ( 1 , 2 ) ) + 1"
                "boolean c = a%2==0",
                "boolean c = 0==a%2"
        );

        StatementSplitter splitter = new StatementSplitter();
        lines.forEach(splitter::split);
    }

    public List<TreeNode> split(String line) {
        List<Token> tokens = LexerWithFSA.tokenize(line);
        Token.prntTokens(tokens);
        List<TreeNode> TreeNodes = split(tokens);
        TreeNodes.forEach(e -> prnt(MessageFormat.format("{0} {1}", e.getType(), Token.toString(e.getTokens()))));
        return TreeNodes;
    }

    public List<TreeNode> split(TreeNode node) {
        List<Token> tokens = node.getTokens();
        StatementActions actions = StatementActions.main(node);
        boolean nodeWithPredicate = StatementDecomposer.isPredicateNode(node.getType());
        if (!nodeWithPredicate && !actions.isNeedToSplit()) {
            TreeNode TreeNode = new TreeNode(node.getType(), tokens);
            return List.of(TreeNode);
        }

        if (nodeWithPredicate) {
            tokens = Token.remove(tokens, "if", "else", "for", "while");
            Token.removeAt(tokens, "(", 0);
            Token.removeAt(tokens, ")", tokens.size() - 1);
        }
        return split(tokens);
    }

    private List<TreeNode> split(List<Token> tokens) {
        tokens = Token.removeSemicolon(tokens);

        Stack<TreeNode> stack = new Stack<>();
        List<TreeNode> result = new ArrayList<>();
        List<Token> finalTokens = tokens;
        IntStream.range(0, tokens.size()).boxed()
                .map(i -> getTreeNode(stack, finalTokens, i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(result::add);

        while (!stack.isEmpty()) {
            TreeNode popped = stack.pop();
            StatementActions actions = StatementActions.from(popped.getTokens());
            popped.setType(getNodeType(actions));
            result.add(popped);
        }

        return result.stream()
                .map(this::splitFunctionArguments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Optional<TreeNode> getTreeNode(Stack<TreeNode> stack, List<Token> tokens, int index) {

        NodeTypeAnalysis nodeTypeAnalysis = NodeTypeAnalyzer.analyze(stack, tokens, index);
        Token token = tokens.get(index);
        if (stack.isEmpty()) {
            TreeNode TreeNode = new TreeNode(nodeTypeAnalysis.getNodeType(), List.of(token));
            stack.push(TreeNode);
            return Optional.empty();
        }

        TreeNode node = stack.peek();

        if (nodeTypeAnalysis.isFunctionCall()) {
            node = new TreeNode(NodeType.FunctionCallStatement);
            node.addToken(token);
            stack.push(node);
        } else if (nodeTypeAnalysis.isArithmeticExpression() || nodeTypeAnalysis.isBooleanExpression()) {
            NodeType newTreeNodeType = nodeTypeAnalysis.getNodeType();
            if (node.getType() == NodeType.UNKNOWN || node.getType() == newTreeNodeType) {
                node.addToken(token);
                node.setType(newTreeNodeType);
            } else {
                node = new TreeNode(newTreeNodeType);
                node.addToken(token);
                stack.push(node);
            }
        } else if (nodeTypeAnalysis.isTerminal()) {// && !isLastToken) {
            node.addToken(token);
            TreeNode popped = stack.pop();
            if (stack.isEmpty()) return Optional.of(popped);
            addSystemVariableAssignment(popped);
            Token variableToken = popped.getToken(1);
            stack.peek().addToken(variableToken);
            return Optional.of(popped);
        } else node.addToken(token);

        return Optional.empty();
    }

    static boolean isIsBooleanExpression(TokenTester tokenTester) {
        TokenPredicate booleanPredicate2 = TokenPredicate.from(0, e -> e.getType() == TokenType.BOOLEAN_OPERATOR);
        return tokenTester.testToken(booleanPredicate2);
    }

    static boolean isIsArithmeticExpression(TokenTester tokenTester) {
        TokenPredicate arithmeticPredicate1 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticPredicate2 = TokenPredicate.from(0, e -> e.getValue().equals("("));
        TokenPredicate arithmeticPredicate3 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticPredicate4 = TokenPredicate.from(1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        return tokenTester.testToken(arithmeticPredicate1, arithmeticPredicate2) || tokenTester.testToken(arithmeticPredicate3, arithmeticPredicate4);
    }

    static boolean isIsFunctionCall(TokenTester tokenTester) {
        TokenPredicate functionPredicate1 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER);
        TokenPredicate functionPredicate2 = TokenPredicate.from(1, e -> e.getValue().equals("("));
        return tokenTester.testToken(functionPredicate1, functionPredicate2);
    }

    private NodeType getNodeType(StatementActions actions) {
        return actions.isCallFunction()
                ? NodeType.FunctionCallStatement
                : actions.isArithmeticOperation()
                ? NodeType.ArithmeticExpressionStatement
                : actions.isBooleanOperation()
                ? NodeType.BooleanExpressionStatement
                : NodeType.UNKNOWN;
    }

    public static NodeType getNodeType(boolean isFunctionCall, boolean isArithmeticExpression, boolean isBooleanExpression) {
        return isFunctionCall
                ? NodeType.FunctionCallStatement
                : isArithmeticExpression
                ? NodeType.ArithmeticExpressionStatement
                : isBooleanExpression
                ? NodeType.BooleanExpressionStatement
                : NodeType.UNKNOWN;
    }

    private List<TreeNode> splitFunctionArguments(TreeNode TreeNode) {
        if (TreeNode.getType() != NodeType.FunctionCallStatement) return List.of(TreeNode);

        List<Token> tokens = TreeNode.getTokens();
        Stack<TreeNode> stack = new Stack<>();
        List<TreeNode> result = new ArrayList<>();

        IntStream.range(0, tokens.size()).forEach(i -> {
            Token token = tokens.get(i);
            Token prev2Token = i - 2 >= 0 ? tokens.get(i - 2) : null;
            Token prevToken = i - 1 >= 0 ? tokens.get(i - 1) : null;
            Token nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

            boolean isArithmeticOperation1 = prev2Token != null
                    && prev2Token.getType() == TokenType.IDENTIFIER
                    && prevToken.getValue().equals("(")
                    && (token.getType() == TokenType.IDENTIFIER
                    || token.getType() == TokenType.NUMBER)
                    && nextToken != null
                    && nextToken.getType() == TokenType.ARITHMETIC_OPERATOR;

            boolean isArithmeticOperation2 = prevToken != null
                    && prevToken.getType() == TokenType.COMMA
                    && (token.getType() == TokenType.IDENTIFIER
                    || token.getType() == TokenType.NUMBER)
                    && nextToken != null
                    && nextToken.getType() == TokenType.ARITHMETIC_OPERATOR;

            boolean isTerminal = token.getValue().equals(")") || token.getType() == TokenType.COMMA;

            if (stack.isEmpty()) stack.push(new TreeNode(NodeType.FunctionCallStatement));
            TreeNode newTreeNode = stack.peek();

            if (isArithmeticOperation1 || isArithmeticOperation2) {
                newTreeNode = new TreeNode(NodeType.ArithmeticExpressionStatement);
                newTreeNode.addToken(token);
                stack.push(newTreeNode);
            } else if (isTerminal && newTreeNode.getType() == NodeType.ArithmeticExpressionStatement) {
                TreeNode popped = stack.pop();
//                if (!stack.isEmpty()) {
                addSystemVariableAssignment(popped);
                result.add(popped);
                Token variableToken = popped.getToken(1);
                stack.peek().addToken(variableToken);
                stack.peek().addToken(token);
//                }
            } else newTreeNode.addToken(token);

        });
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }

        return result;
    }


    private void addSystemVariableAssignment(TreeNode TreeNode) {
        //todo: get type token from function or from expression or from stackFrame
        List<Token> result = new ArrayList<>();
        result.add(new Token(TokenType.TYPE, "int")); // todo: fix ..

        result.add(new Token(TokenType.IDENTIFIER, "$v" + systemVariableNb++));
        result.add(new Token(TokenType.ASSIGNMENT, "="));
        result.addAll(TreeNode.getTokens());
        TreeNode.getTokens().clear();
        TreeNode.getTokens().addAll(result);
    }
}
