package interpreter;

import interpreter.lexer.Token;
import interpreter.lexer.TokenType;
import interpreter.tree_parser.NodeType;
import interpreter.tree_parser.ParseTreeNode;
import interpreter.tree_parser.ParseTreeNodeUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Getter
@Setter
@ToString
public class StatementActions {
    private int functionCallCount;
    private String variableName;
    private boolean returnStatement;
    private boolean callFunction;
    private boolean arithmeticOperation;
    private boolean booleanOperation;
    private boolean createVariable;
    private boolean assignValue;
    //---------
    private boolean pushControlBlock;
    private boolean popControlBlock;


    public boolean isNeedToSplit() {
        return functionCallCount > 1
                || (arithmeticOperation && callFunction)
                || (arithmeticOperation && booleanOperation)
                || (booleanOperation && callFunction);
    }

    public static StatementActions from(List<Token> tokens) {
        StatementActions statementActions = new StatementActions();
        int size = tokens.size();
        IntStream.range(0, size).forEach(e -> accumStatementActions(statementActions, tokens, e));
        return statementActions;
    }

    public static StatementActions main(ParseTreeNode node) {
        StatementActions statementActions = new StatementActions();
        List<Token> tokens = node.getTokens();
        int size = tokens.size();
        IntStream.range(0, size).forEach(e -> accumStatementActions(statementActions, tokens, e));
        statementActions.pushControlBlock = ParseTreeNodeUtils.hasChildNode(node, e -> e.getNodeType() == NodeType.PUSH_CODE_BLOCK);
        statementActions.popControlBlock = ParseTreeNodeUtils.hasChildNode(node, e -> e.getNodeType() == NodeType.POP_CODE_BLOCK);
        return statementActions;
    }

    private static void accumStatementActions(StatementActions statementActions, List<Token> tokens, int index) {
        int size = tokens.size();
        Token token = tokens.get(index);
        Token nextToken = index + 1 < size ? tokens.get(index + 1) : null;

        boolean isArithmeticOperation = token.getType() == TokenType.ARITHMETIC_OPERATOR;
        boolean isBooleanOperation = token.getType() == TokenType.BOOLEAN_OPERATOR;
        boolean isAssignment = nextToken != null && token.getType() == TokenType.IDENTIFIER && nextToken.getType() == TokenType.ASSIGNMENT;
        boolean isFunctionCall = nextToken != null && token.getType() == TokenType.IDENTIFIER && Objects.equals(nextToken.getValue(), "(");
        boolean isVariableCreation = nextToken != null && token.getType() == TokenType.TYPE && nextToken.getType() == TokenType.IDENTIFIER;
        boolean isReturnStatement = token.getValue().equals("return");
        statementActions.functionCallCount = isFunctionCall ? statementActions.functionCallCount + 1 : statementActions.functionCallCount;

        if (isFunctionCall) statementActions.callFunction = true;
        else if (isBooleanOperation) statementActions.booleanOperation = true;
        else if (isArithmeticOperation) statementActions.arithmeticOperation = true;
        else if (isReturnStatement) statementActions.returnStatement = true;
        else if (isAssignment) {
            statementActions.assignValue = true;
            statementActions.variableName = token.getValue();
        } else if (isVariableCreation) {
            statementActions.createVariable = true;
            statementActions.variableName = nextToken.getValue();
        }
    }

    public boolean isAllDone() {
        return !returnStatement
                && !callFunction
                && !arithmeticOperation
                && !booleanOperation
                && !createVariable
                && !assignValue

                && !pushControlBlock
                && !popControlBlock;
    }

    public StatementActions getCopy() {
        StatementActions statementActions = new StatementActions();
        statementActions.functionCallCount = functionCallCount;
        statementActions.variableName = variableName;
        statementActions.returnStatement = returnStatement;
        statementActions.callFunction = callFunction;
        statementActions.arithmeticOperation = arithmeticOperation;
        statementActions.booleanOperation = booleanOperation;
        statementActions.createVariable = createVariable;
        statementActions.assignValue = assignValue;
        statementActions.pushControlBlock = pushControlBlock;
        statementActions.popControlBlock = popControlBlock;
        return statementActions;
    }
}
