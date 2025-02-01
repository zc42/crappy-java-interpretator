package zc.dev.interpreter;

import zc.dev.interpreter.lexer.LexerWithFSA;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.ParseTreeNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zc.dev.interpreter.tree_parser.StatementDecomposer;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static zc.dev.interpreter.Utils.prnt;

public class StatementSplitter {

    public static void main(String[] args) {
        List<String> lines = List.of(
//                "b = 1 + a(a + 1, a(1, 2));",
//                "b > 0 || b + 1 > 0 || a(1)",
//                "int c = a ( b + 1 , a ( 1 , 2 ) ) + 1"
                "boolean c = a%2==0",
                "boolean c = 0==a%2"
        );

        lines.forEach(StatementSplitter::split);
    }

    public static List<Statement> split(String line) {
        List<Token> tokens = LexerWithFSA.tokenize(line);
        Token.prntTokens(tokens);
        List<Statement> statements = split(tokens);
        statements.forEach(e -> prnt(MessageFormat.format("{0} {1}", e.getType(), Token.toString(e.getTokens()))));
        return statements;
    }

    public static List<Statement> split(ParseTreeNode node) {
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

    private static List<Statement> split(List<Token> tokens) {
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
                .map(StatementSplitter::splitFunctionArguments)
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

    private static Optional<Statement> getStatement(Stack<Statement> stack, List<Token> tokens, int index) {

//        Token.prntTokens(tokens);

        Token token = tokens.get(index);
        if (Objects.equals(token.getValue(), "==")) {
            token = token;
        }
//        Token prevToken = index - 1 >= 0 ? tokens.get(index - 1) : null;
//        Token nextToken = index + 1 < tokens.size() ? tokens.get(index + 1) : null;

        TokenPredicate functionPredicate1 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER);
        TokenPredicate functionPredicate2 = TokenPredicate.from(1, e -> e.getValue().equals("("));

        TokenPredicate arithmeticPredicate1 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticPredicate2 = TokenPredicate.from(0, e -> e.getValue().equals("("));

        TokenPredicate arithmeticPredicate3 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticPredicate4 = TokenPredicate.from(1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);

        TokenPredicate arithmeticTermPredicate1 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticTermPredicate2 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticTermPredicate3 = TokenPredicate.from(1, e -> e.getType() != TokenType.ARITHMETIC_OPERATOR);

        TokenPredicate arithmeticTermPredicate11 = TokenPredicate.from(-2, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate arithmeticTermPredicate12 = TokenPredicate.from(-1, e -> e.getType() == TokenType.ARITHMETIC_OPERATOR);
        TokenPredicate arithmeticTermPredicate13 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);

//        TokenPredicate booleanPredicate1 = TokenPredicate.from(0, e -> e.getType() == TokenType.IDENTIFIER || e.getType() == TokenType.NUMBER);
        TokenPredicate booleanPredicate2 = TokenPredicate.from(0, e -> e.getType() == TokenType.BOOLEAN_OPERATOR);

        TokenPredicate terminalPredicate = TokenPredicate.from(0, e -> e.getValue().equals(")"));
        TokenPredicate nextCommaPredicate = TokenPredicate.from(1, e -> e.getType() == TokenType.COMMA);

        TokenTester tokenTester = TokenTester.from(tokens, index);

        Statement statement = stack.isEmpty() ? null : stack.peek();
        boolean isFunctionCall = tokenTester.testToken(functionPredicate1, functionPredicate2);
        boolean isArithmeticExpression = tokenTester.testToken(arithmeticPredicate1, arithmeticPredicate2)
                || tokenTester.testToken(arithmeticPredicate3, arithmeticPredicate4);
        boolean isBooleanExpression = tokenTester.testToken(booleanPredicate2);
        boolean isTerminal = tokenTester.testToken(terminalPredicate)

                || (tokenTester.testToken(nextCommaPredicate)
                && statement != null
                && (statement.getType() == NodeType.ArithmeticExpressionStatement
                || statement.getType() == NodeType.BooleanExpressionStatement))

                || (stack.size() > 1
                && statement != null
                && statement.getType() != getNodeType(isFunctionCall, isArithmeticExpression, isBooleanExpression)
                && NodeType.UNKNOWN != getNodeType(isFunctionCall, isArithmeticExpression, isBooleanExpression))

                || tokenTester.testToken(arithmeticTermPredicate1, arithmeticTermPredicate2, arithmeticTermPredicate3)
                || tokenTester.testToken(arithmeticTermPredicate11, arithmeticTermPredicate12, arithmeticTermPredicate13)
                && index == tokens.size() - 1;


        if (stack.isEmpty()) {
            NodeType statementType = getNodeType(isFunctionCall, isArithmeticExpression, isBooleanExpression);
            stack.push(Statement.from(statementType));
        }
        statement = stack.peek();

        if (isFunctionCall) {
            statement = Statement.from(NodeType.FunctionCallStatement);
            statement.addToken(token);
            stack.push(statement);
        } else if (isArithmeticExpression || isBooleanExpression) {
            NodeType newStatementType = getNodeType(isFunctionCall, isArithmeticExpression, isBooleanExpression);
            if (statement.getType() == NodeType.UNKNOWN || statement.getType() == newStatementType) {
                statement.addToken(token);
                statement.setType(newStatementType);
            } else {
                statement = Statement.from(newStatementType);
                statement.addToken(token);
                stack.push(statement);
            }
        } else if (isTerminal) {// && !isLastToken) {
            statement.addToken(token);
            Statement popped = stack.pop();
            if (stack.isEmpty()) return Optional.empty();
            addSystemVariableAssignment(popped);
            Token variableToken = popped.getToken(1);
            stack.peek().addToken(variableToken);
            return Optional.of(popped);
        } else statement.addToken(token);

        return Optional.empty();
    }

    private static NodeType getNodeType(StatementActions actions) {
        return actions.isCallFunction()
                ? NodeType.FunctionCallStatement
                : actions.isArithmeticOperation()
                ? NodeType.ArithmeticExpressionStatement
                : actions.isBooleanOperation()
                ? NodeType.BooleanExpressionStatement
                : NodeType.UNKNOWN;
    }

    private static NodeType getNodeType(boolean isFunctionCall, boolean isArithmeticExpression, boolean isBooleanExpression) {
        return isFunctionCall
                ? NodeType.FunctionCallStatement
                : isArithmeticExpression
                ? NodeType.ArithmeticExpressionStatement
                : isBooleanExpression
                ? NodeType.BooleanExpressionStatement
                : NodeType.UNKNOWN;
    }

    private static List<Statement> splitFunctionArguments(Statement statement) {
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

    private static int systemVariableNb;

    private static void addSystemVariableAssignment(Statement statement) {
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
