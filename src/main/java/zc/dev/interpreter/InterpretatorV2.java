package zc.dev.interpreter;

import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.expressions.ArithmeticExpressionParser;
import zc.dev.interpreter.expressions.BooleanExpressionParser;
import zc.dev.interpreter.expressions.Expression;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.CustomCodeParser;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.TreeNode;
import zc.dev.interpreter.tree_parser.ParseTreeNodeUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static zc.dev.interpreter.Utils.getCode;
import static zc.dev.interpreter.Utils.prnt;


public class InterpretatorV2 {

    public static void main(String[] args) {
        String code = getCode();
        TreeNode rootNode = CustomCodeParser.parseCode(code);
        if (rootNode == null) throw new RuntimeException("rootNode == null");

        rootNode.printTree();
        TreeNode entryPointNode = ParseTreeNodeUtils.findEntryPoint(rootNode).orElseThrow(() -> new RuntimeException("No entry point found"));
        entryPointNode.printTree();

        InterpreterContext ctx = InterpreterContext.from(rootNode);
        ctx.createCallStackFrame(entryPointNode, args);
        Stack<CallStackFrame> callStack = ctx.getCallStack();
        while (!callStack.isEmpty()) executeCode(ctx, 5);
    }

    private static void executeCode(InterpreterContext ctx, int nbOfCycleToRun) {
        Stack<CallStackFrame> callStack = ctx.getCallStack();
        for (int i = 0; i < nbOfCycleToRun; i++) {
            CallStackFrame frame = callStack.peek();
            Optional<TreeNode> nodeOptional = frame.getCurrentExecutableNode();
            if (nodeOptional.isPresent()) nodeOptional.ifPresent(e -> executeDecomposedStatement(ctx, e));
            else popCallStackFrame(callStack);
            if (callStack.isEmpty()) break;
        }
    }

    private static void popCallStackFrame(Stack<CallStackFrame> callStack) {
        CallStackFrame frame;
        CallStackFrame lastFrame = callStack.pop();
        if (callStack.isEmpty()) return;
        frame = callStack.peek();
        lastFrame.getTempValue().ifPresent(frame::setTempValue);
    }

    private static void executeDecomposedStatement(InterpreterContext ctx, TreeNode node) {
        CallStackFrame frame = ctx.getCurrentCallStackFrame();

        List<Token> tokens = node.getTokens();
        StatementActions statementActions = frame.getCurrentExecutableNodeActions();
        prnt(statementActions);

        if (node.getType() == NodeType.GOTO) executeGoToStatement(frame, node);
        else if (statementActions.isPushControlBlock()) frame.pushControlBlock();
        else if (statementActions.isCallFunction()) executeCallFunctionStatement(ctx, tokens);
        else if (statementActions.isArithmeticOperation()) executeArithmeticOperationStatement(tokens, frame);
        else if (statementActions.isBooleanOperation()) executeBooleanOperationStatement(tokens, frame);
        else if (statementActions.isCreateVariable()) executeCreateVariableStatement(frame);
        else if (statementActions.isAssignValue()) executeAssignVariableStatement(frame);
        else if (statementActions.isReturnStatement()) executeReturnStatement(ctx, tokens);
        else if (statementActions.isPopControlBlock()) frame.popControlBlock();

        prnt("----------");
    }

    private static void executeGoToStatement(CallStackFrame frame, TreeNode node) {
        prnt("goto statement " + node);

        TokenType tokenType = node.getTokens().get(0).getType();
        String value = node.getTokens().get(1).getValue();
        int lineNb = Integer.parseInt(value);

        if (tokenType == TokenType.GOTO) {
            prnt(MessageFormat.format("goto -> {0}", lineNb));
            frame.goTo(lineNb);
        } else if (tokenType == TokenType.GOTO_IF_FALSE) {
            boolean isFalse = frame.getTempValue()
                    .filter(e -> e instanceof Boolean)
                    .filter(e -> !((Boolean) e))
                    .isPresent();

            if (isFalse) {
                prnt(MessageFormat.format("goto -> {0}", lineNb));
                frame.goTo(lineNb);
            }
        }
        prnt("done goto statement " + node);
    }

    private static void executeReturnStatement(InterpreterContext ctx, List<Token> tokens) {
        Stack<CallStackFrame> callStack = ctx.getCallStack();
        CallStackFrame frame = callStack.peek();
        prnt("function call ended, return value: " + frame.getTempValue());

        TreeNode node = frame.getCurrentExecutableNode().orElseThrow();
        StatementActions nodeStatementActions = node.getStatementActions();
        boolean isTempValueAssigned = nodeStatementActions.isCallFunction()
                || nodeStatementActions.isArithmeticOperation()
                || nodeStatementActions.isBooleanOperation();

        if (!isTempValueAssigned) {
            Token variable = tokens.stream()
                    .filter(e -> e.getType() == TokenType.IDENTIFIER)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No identifier name was found:\n" + Token.toString(tokens)));
            Object v = frame.getVariableValue(variable.getValue());
            frame.addTempValue(v);
        }

        frame.getCurrentExecutableNodeActions().setReturnStatement(false);
        CallStackFrame lastFrame = callStack.pop();
        if (callStack.isEmpty()) return;

        frame = callStack.peek();
        lastFrame.getTempValue().ifPresent(frame::setTempValue);

        prnt(".. function call ended, return value: " + frame.getTempValue());
    }

    private static void executeAssignVariableStatement(CallStackFrame frame) {
        prnt("assignValue..");
        String name = frame.getCurrentExecutableNodeActions().getVariableName();
        frame.assignVariable(name);
        prnt("assign variable.." + name);
    }

    private static void executeCreateVariableStatement(CallStackFrame frame) {
        prnt("createVariable..");
        String name = frame.getCurrentExecutableNodeActions().getVariableName();
        frame.createVariable(name);
        prnt("variable created.." + name);
    }

    private static void executeBooleanOperationStatement(List<Token> tokens, CallStackFrame frame) {
        prnt("execute Boolean Operation..");
        List<Token> expressionTokens = removeAssignmentPart(tokens);

        List<Token> postfix = InfixToPostfixConverter.convert(expressionTokens);
        Token.prntTokens(expressionTokens);
        Token.prntTokens(postfix);

        Expression<Boolean> parsedExpression = BooleanExpressionParser.parse(frame, postfix);
        frame.addTempValue(parsedExpression.interpret());
        frame.getCurrentExecutableNodeActions().setBooleanOperation(false);
        prnt("result of expression: " + frame.getTempValue());
    }


    private static void executeArithmeticOperationStatement(List<Token> tokens, CallStackFrame frame) {
        prnt("executeArithmeticOperation..");
        Token.prntTokens(tokens);
        List<Token> expressionTokens = removeAssignmentPart(tokens);
//            expressionTokens = replaceVariablesWithValues(ctx, expressionTokens);
        List<Token> postfix = InfixToPostfixConverter.convert(expressionTokens);
        Token.prntTokens(expressionTokens);
        Token.prntTokens(postfix);

        Expression<Double> parsedExpression = ArithmeticExpressionParser.parse(frame, postfix);
        frame.addTempValue(parsedExpression.interpret());
        frame.getCurrentExecutableNodeActions().setArithmeticOperation(false);
        prnt("result of expression: " + frame.getTempValue());
    }

    private static void executeCallFunctionStatement(InterpreterContext ctx, List<Token> tokens) {
        prnt("calling function: " + Token.toString(tokens));
        CallStackFrame frame = ctx.getCallStack().peek();
        //=====for debug========
        Optional<TreeNode> option = findFunctionNode(ctx, tokens);
        if (option.isEmpty()) {
            findFunctionNode(ctx, tokens);
        }
        //=============

        TreeNode functionNode = findFunctionNode(ctx, tokens)
                .orElseThrow(() -> new RuntimeException("No function declaration was found:\n" + Token.toString(tokens)));

        Object[] argumentValues = getArguments(frame, tokens);
        if (functionNode.getType() == NodeType.SystemFunction) {
            callSystemFunction(frame, functionNode, argumentValues).ifPresent(frame::setTempValue);
        } else {
            ctx.createCallStackFrame(functionNode, argumentValues);
//            CallStackFrame functionFrame = callFunction(ctx, functionNode, argumentValues);
//            functionFrame.getTempValue().ifPresent(frame::setTempValue);
        }
        frame.getCurrentExecutableNodeActions().setCallFunction(false);
        prnt("function call done");
    }

    private static Object[] getArguments(CallStackFrame frame, List<Token> tokens) {
        int start = tokens.indexOf(new Token(TokenType.PARENTHESES, "("));
        Predicate<Token> predicate = e -> e.getType() == TokenType.IDENTIFIER
                || e.getType() == TokenType.NUMBER
                || e.getType() == TokenType.STRING
                || e.getType() == TokenType.Boolean;

        return tokens.stream()
                .skip(start)
                .filter(predicate)
                .map(e -> getValue(frame, e))
                .toArray();
    }

    private static Object getValue(CallStackFrame frame, Token token) {
        return token.getType() == TokenType.IDENTIFIER
                ? frame.getVariableValue(token.getValue())
                : token.getValue();
    }

    private static List<Token> removeAssignmentPart(List<Token> tokens) {
        int i = tokens.indexOf(new Token(TokenType.ASSIGNMENT, "="));
        if (i == -1) return tokens;
        return tokens.stream()
                .skip(i + 1)
                .collect(Collectors.toList());
    }

    private static Optional<TreeNode> findFunctionNode(InterpreterContext ctx, List<Token> functionCallTokens) {
        CallStackFrame frame = ctx.getCurrentCallStackFrame();
        Optional<TreeNode> systemFunctionOption = findSystemFunction(frame, functionCallTokens);
        if (systemFunctionOption.isPresent()) return systemFunctionOption;

        TreeNode node = findParentClassNode(ctx);
        return node.getChildren().stream()
                .filter(e -> e.getType() == NodeType.CodeBlock)
                .map(TreeNode::getChildren)
                .flatMap(Collection::stream)
                .filter(e -> e.getType() == NodeType.FunctionDeclarationStatement)
                .filter(e -> isSameFunction(frame, e.getTokens(), functionCallTokens)) //todo: change arg names to arg types
                .findFirst();
    }

    private static Optional<TreeNode> findSystemFunction(CallStackFrame frame, List<Token> functionCallTokens) {
        FunctionDeclarationFilter filter = FunctionDeclarationFilter.from(frame, functionCallTokens);
        String name = filter.getName().getValue();
        List<Token> argumentTypes = filter.getArgumentTypes();
        if (name.equals("prnt") && argumentTypes.size() == 1) {
            TreeNode node = new TreeNode(NodeType.SystemFunction, functionCallTokens);
            return Optional.of(node);
        }
        return Optional.empty();
    }

    private static Optional<Object> callSystemFunction(CallStackFrame frame, TreeNode functionNode, Object[] argumentValues) {
        FunctionDeclarationFilter filter = FunctionDeclarationFilter.from(frame, functionNode.getTokens());
        String name = filter.getName().getValue();
        List<Token> argumentTypes = filter.getArgumentTypes();
        if (name.equals("prnt") && argumentTypes.size() == 1) {
            prnt(argumentValues[0]);
            return Optional.empty();
        }
        String message = MessageFormat.format("system function nod found: {0}, {1}", name, Token.toString(argumentTypes));
        throw new RuntimeException(message);
    }

    private static TreeNode findParentClassNode(InterpreterContext ctx) {
        TreeNode node = ctx.getCurrentCallStackFrame().getNode();
        List<Token> tokens = node.getTokens();
        while (node.getType() != NodeType.Root) {
            node = node.getParent();
            if (node.getType() == NodeType.Class) return node;
        }
        throw new RuntimeException("could not find parent class node.\n" + Token.toString(tokens));
    }

    private static boolean isSameFunction(CallStackFrame frame, List<Token> functionDeclarationTokens, List<Token> functionCallTokens) {
        FunctionDeclarationFilter filter1 = FunctionDeclarationFilter.from(functionDeclarationTokens);
        FunctionDeclarationFilter filter2 = FunctionDeclarationFilter.from(frame, functionCallTokens);
        return filter1.equals(filter2);
    }
}
