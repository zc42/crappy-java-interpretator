package zc.dev.interpreter.tree_parser.statement.decomposer;

import zc.dev.interpreter.StatementActions;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.TreeNode;

import java.util.List;
import java.util.Stack;

public class NodeTypeAnalyzer {

    public static NodeTypeAnalysis analyze(Stack<TreeNode> stack, List<Token> tokens, int index) {
        TokenTester tokenTester = TokenTester.from(tokens, index);
        boolean isFunctionCall = StatementSplitter.isIsFunctionCall(tokenTester);
        boolean isArithmeticExpression = StatementSplitter.isIsArithmeticExpression(tokenTester);
        boolean isBooleanExpression = StatementSplitter.isIsBooleanExpression(tokenTester);
        NodeType nodeType = index == 0
                ? getNodeTypeForFirstToken(tokenTester, tokens)
                : StatementSplitter.getNodeType(isFunctionCall, isArithmeticExpression, isBooleanExpression);
        boolean isTerminal = NodeTypeAnalyzer.isTerminal(stack, tokens, index, tokenTester, nodeType);

        return NodeTypeAnalysis.builder()
                .functionCall(isFunctionCall)
                .arithmeticExpression(isArithmeticExpression)
                .booleanExpression(isBooleanExpression)
                .terminal(isTerminal)
                .nodeType(nodeType)
                .build();
    }

    private static NodeType getNodeTypeForFirstToken(TokenTester tokenTester, List<Token> tokens) {

        TokenPredicate isIdentifier = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER);
        TokenPredicate isOpenParentheses = TokenPredicate.from(1, e -> e.getValue().equals("("));
        boolean beginsWithFunctionCall = tokenTester.testToken(isIdentifier, isOpenParentheses);

        StatementActions actions = StatementActions.from(tokens);
        if (beginsWithFunctionCall) return NodeType.FunctionCallStatement;
        else if (actions.isBooleanOperation()) return NodeType.BooleanExpressionStatement;
        else if (actions.isArithmeticOperation()) return NodeType.ArithmeticExpressionStatement;
        else if (actions.isCallFunction()) return NodeType.FunctionCallStatement;
        throw new RuntimeException("getNodeTypeForFirstToken unsuported actions combination: " + actions);
    }

    private static boolean isTerminal(Stack<TreeNode> stack, List<Token> tokens, int index, TokenTester tokenTester, NodeType nodeType) {
        boolean isArithmeticTerminal1 = isIsArithmeticTerminal1(tokenTester);
        boolean isArithmeticTerminal2 = isIsArithmeticTerminal2(tokens, index, tokenTester);
        boolean isClosingParentheses = isIsClosingParentheses(tokenTester);
        boolean isArithmeticOrBooleanOperationAndNextIsComma = isIsArithmeticOrBooleanOperationAndNextIsComma(stack, tokenTester);
        boolean isDifferentNodeType = isIsDifferentNodeType(stack, nodeType);

        return isClosingParentheses
                || isArithmeticOrBooleanOperationAndNextIsComma
                || isDifferentNodeType
                || isArithmeticTerminal1
                || isArithmeticTerminal2;
    }

    private static boolean isIsDifferentNodeType(Stack<TreeNode> stack, NodeType nodeType) {
        TreeNode statement = stack.isEmpty() ? null : stack.peek();
        return stack.size() > 1
                && statement != null
                && statement.getType() != nodeType
                && NodeType.UNKNOWN != nodeType;
    }

    private static boolean isIsArithmeticTerminal2(List<Token> tokens, int index, TokenTester tokenTester) {
        TokenPredicate arithmeticTermPredicate11 = TokenPredicate.from(-2, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticTermPredicate12 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticTermPredicate13 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        return tokenTester.testToken(arithmeticTermPredicate11, arithmeticTermPredicate12, arithmeticTermPredicate13)
                && index == tokens.size() - 1;
    }

    private static boolean isIsArithmeticTerminal1(TokenTester tokenTester) {
        TokenPredicate arithmeticTermPredicate1 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticTermPredicate2 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticTermPredicate3 = TokenPredicate.from(1, e -> e.getType() != TokenType.ARITHMETIC_OPERATOR);
        return tokenTester.testToken(arithmeticTermPredicate1, arithmeticTermPredicate2, arithmeticTermPredicate3);
    }

    private static boolean isIsArithmeticOrBooleanOperationAndNextIsComma(Stack<TreeNode> stack, TokenTester tokenTester) {
        TreeNode statement = stack.isEmpty() ? null : stack.peek();
        TokenPredicate nextCommaPredicate = TokenPredicate.from(1, e -> e.getType() == TokenType.COMMA);
        return tokenTester.testToken(nextCommaPredicate)
                && statement != null
                && (statement.getType() == NodeType.ArithmeticExpressionStatement
                || statement.getType() == NodeType.BooleanExpressionStatement);
    }

    private static boolean isIsClosingParentheses(TokenTester tokenTester) {
        TokenPredicate terminalPredicate = TokenPredicate.from(0, e -> e.getValue().equals(")"));
        return tokenTester.testToken(terminalPredicate);
    }
}
