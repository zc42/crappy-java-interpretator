package zc.dev.interpreter;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zc.dev.interpreter.lexer.LexerWithFSA;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.ParseTreeNode;
import zc.dev.interpreter.tree_parser.StatementDecomposer;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Predicate;
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

    public List<Statement> split(String line) {
        List<Token> tokens = LexerWithFSA.tokenize(line);
        Token.prntTokens(tokens);
        List<Statement> statements = split(tokens);
        statements.forEach(e -> prnt(MessageFormat.format("{0} {1}", e.getType(), Token.toString(e.getTokens()))));
        return statements;
    }

    public List<Statement> split(ParseTreeNode node) {
        List<Token> tokens = node.getTokens();
        StatementActions actions = StatementActions.main(node);
        boolean nodeWithPredicate = StatementDecomposer.isPredicateNode(node.getNodeType());
        if (!nodeWithPredicate && !actions.isNeedToSplit()) {
            Statement statement = Statement.of(node.getNodeType(), tokens);
            return List.of(statement);
        }

        if (nodeWithPredicate) {
            tokens = Token.remove(tokens, "if", "else", "for", "while");
            Token.removeAt(tokens, "(", 0);
            Token.removeAt(tokens, ")", tokens.size() - 1);
        }
        return split(tokens);
    }

    private List<Statement> split(List<Token> tokens) {
        tokens = Token.removeSemicolon(tokens);

        Stack<Statement> stack = new Stack<>();
        List<Statement> result = new ArrayList<>();
        List<Token> finalTokens = tokens;
        IntStream.range(0, tokens.size()).boxed()
                .map(i -> getStatement(stack, finalTokens, i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(result::add);

        while (!stack.isEmpty()) {
            Statement popped = stack.pop();
            StatementActions actions = StatementActions.from(popped.getTokens());
            popped.setType(getNodeType(actions));
            result.add(popped);
        }

        return result.stream()
                .map(this::splitFunctionArguments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor(staticName = "from")
    public static class TokenPredicate {
        private final int index;
        private final Predicate<Token> predicate;
    }

    @RequiredArgsConstructor(staticName = "from")
    public static class TokenTester {
        private final List<Token> tokens;
        private final int index;

        public boolean testToken(TokenPredicate... tokenPredicates) {
            Predicate<TokenPredicate> predicate = this::testToken;
            return Arrays.stream(tokenPredicates)
                    .filter(predicate.negate())
                    .findFirst()
                    .isEmpty();
        }

        private boolean testToken(TokenPredicate tokenPredicate) {
            int i = index + tokenPredicate.getIndex();
            if (i < 0 || i >= tokens.size()) return false;
            Token token = tokens.get(i);
            return tokenPredicate.getPredicate().test(token);
        }
    }

    private Optional<Statement> getStatement(Stack<Statement> stack, List<Token> tokens, int index) {

        AA.BB bb = AA.getData(stack, tokens, index);
        Token token = tokens.get(index);
        if (stack.isEmpty()) {
            Statement statement = Statement.of(bb.nodeType, List.of(token));
            stack.push(statement);
            return Optional.empty();
        }

        Statement statement = stack.peek();

        if (bb.isFunctionCall) {
            statement = Statement.from(NodeType.FunctionCallStatement);
            statement.addToken(token);
            stack.push(statement);
        } else if (bb.isArithmeticExpression || bb.isBooleanExpression) {
            NodeType newStatementType = bb.nodeType;
            if (statement.getType() == NodeType.UNKNOWN || statement.getType() == newStatementType) {
                statement.addToken(token);
                statement.setType(newStatementType);
            } else {
                statement = Statement.from(newStatementType);
                statement.addToken(token);
                stack.push(statement);
            }
        } else if (bb.isTerminal) {// && !isLastToken) {
            statement.addToken(token);
            Statement popped = stack.pop();
            if (stack.isEmpty()) return Optional.of(popped);
            addSystemVariableAssignment(popped);
            Token variableToken = popped.getToken(1);
            stack.peek().addToken(variableToken);
            return Optional.of(popped);
        } else statement.addToken(token);

        return Optional.empty();
    }

    static public class AA {

        @Getter
        @Builder(toBuilder = true)
        static public class BB {
            private final boolean isFunctionCall;
            private final boolean isArithmeticExpression;
            private final boolean isBooleanExpression;
            private final boolean isTerminal;
            private final NodeType nodeType;
        }

        public static BB getData(Stack<Statement> stack, List<Token> tokens, int index) {
            TokenTester tokenTester = TokenTester.from(tokens, index);
            boolean isFunctionCall = isIsFunctionCall(tokenTester);
            boolean isArithmeticExpression = isIsArithmeticExpression(tokenTester);
            boolean isBooleanExpression = isIsBooleanExpression(tokenTester);
            NodeType nodeType = index == 0
                    ? getNodeTypeForFirstToken(tokenTester, tokens)
                    : getNodeType(isFunctionCall, isArithmeticExpression, isBooleanExpression);
            boolean isTerminal = AA.isTerminal(stack, tokens, index, tokenTester, nodeType);

            return BB.builder()
                    .isFunctionCall(isFunctionCall)
                    .isArithmeticExpression(isArithmeticExpression)
                    .isBooleanExpression(isBooleanExpression)
                    .isTerminal(isTerminal)
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

        private static boolean isTerminal(Stack<Statement> stack, List<Token> tokens, int index, TokenTester tokenTester, NodeType nodeType) {
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

        private static boolean isIsDifferentNodeType(Stack<Statement> stack, NodeType nodeType) {
            Statement statement = stack.isEmpty() ? null : stack.peek();
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

        private static boolean isIsArithmeticOrBooleanOperationAndNextIsComma(Stack<Statement> stack, TokenTester tokenTester) {
            Statement statement = stack.isEmpty() ? null : stack.peek();
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

    private static boolean isIsBooleanExpression(TokenTester tokenTester) {
        TokenPredicate booleanPredicate2 = TokenPredicate.from(0, e -> e.getType() == TokenType.BOOLEAN_OPERATOR);
        return tokenTester.testToken(booleanPredicate2);
    }

    private static boolean isIsArithmeticExpression(TokenTester tokenTester) {
        TokenPredicate arithmeticPredicate1 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticPredicate2 = TokenPredicate.from(0, e -> e.getValue().equals("("));
        TokenPredicate arithmeticPredicate3 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticPredicate4 = TokenPredicate.from(1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        return tokenTester.testToken(arithmeticPredicate1, arithmeticPredicate2) || tokenTester.testToken(arithmeticPredicate3, arithmeticPredicate4);
    }

    private static boolean isIsFunctionCall(TokenTester tokenTester) {
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

    private List<Statement> splitFunctionArguments(Statement statement) {
        if (statement.getType() != NodeType.FunctionCallStatement) return List.of(statement);

        List<Token> tokens = statement.getTokens();
        Stack<Statement> stack = new Stack<>();
        List<Statement> result = new ArrayList<>();

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

            if (stack.isEmpty()) stack.push(Statement.from(NodeType.FunctionCallStatement));
            Statement newStatement = stack.peek();

            if (isArithmeticOperation1 || isArithmeticOperation2) {
                newStatement = Statement.from(NodeType.ArithmeticExpressionStatement);
                newStatement.addToken(token);
                stack.push(newStatement);
            } else if (isTerminal && newStatement.getType() == NodeType.ArithmeticExpressionStatement) {
                Statement popped = stack.pop();
//                if (!stack.isEmpty()) {
                addSystemVariableAssignment(popped);
                result.add(popped);
                Token variableToken = popped.getToken(1);
                stack.peek().addToken(variableToken);
                stack.peek().addToken(token);
//                }
            } else newStatement.addToken(token);

        });
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }

        return result;
    }


    private void addSystemVariableAssignment(Statement statement) {
        //todo: get type token from function or from expression or from stackFrame
        List<Token> result = new ArrayList<>();
        result.add(new Token(TokenType.TYPE, "int")); // todo: fix ..

        result.add(new Token(TokenType.IDENTIFIER, "$v" + systemVariableNb++));
        result.add(new Token(TokenType.ASSIGNMENT, "="));
        result.addAll(statement.getTokens());
        statement.getTokens().clear();
        statement.getTokens().addAll(result);
    }
}
