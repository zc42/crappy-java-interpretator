package zc.dev.interpreter;

import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.expressions.ArithmeticExpressionParser;
import zc.dev.interpreter.expressions.BooleanExpressionParser;
import zc.dev.interpreter.expressions.Expression;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.CustomCodeParser;
import zc.dev.interpreter.tree_parser.NodeType;
import zc.dev.interpreter.tree_parser.ParseTreeNode;
import zc.dev.interpreter.tree_parser.ParseTreeNodeUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static zc.dev.interpreter.Utils.prnt;


public class InterpretatorV2 {

    public static void main(String[] args) {
        String code = getCode();
        ParseTreeNode rootNode = CustomCodeParser.parseCode(code);
        if (rootNode == null) throw new RuntimeException("rootNode == null");

        rootNode.printTree();
        ParseTreeNode entryPointNode = ParseTreeNodeUtils.findEntryPoint(rootNode).orElseThrow(() -> new RuntimeException("No entry point found"));
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
            Optional<ParseTreeNode> nodeOptional = frame.getCurrentExecutableNode();
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

    private static void executeDecomposedStatement(InterpreterContext ctx, ParseTreeNode node) {
        CallStackFrame frame = ctx.getCurrentCallStackFrame();

        List<Token> tokens = node.getTokens();
        StatementActions statementActions = frame.getCurrentExecutableNodeActions();
        prnt(statementActions);

        if (node.getNodeType() == NodeType.GOTO) {
            executeGoToStatement(frame, node);
            return;
        }

        if (statementActions.isPushControlBlock()) frame.pushControlBlock();

        if (statementActions.isCallFunction()) {
            statementCallFunction(ctx, tokens);
            return;
        } else if (statementActions.isArithmeticOperation()) statementExecuteArithmeticOperation(tokens, frame);
        else if (statementActions.isBooleanOperation()) statementExecuteBooleanOperation(tokens, frame);

        if (statementActions.isCreateVariable()) statementCreateVariable(frame);
        if (statementActions.isAssignValue()) statementAssignVariable(frame);
        if (statementActions.isReturnStatement()) statementReturn(ctx, tokens);
        if (statementActions.isPopControlBlock()) frame.popControlBlock();
        prnt("----------");
    }

    private static void executeGoToStatement(CallStackFrame frame, ParseTreeNode node) {
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

    private static void statementReturn(InterpreterContext ctx, List<Token> tokens) {
        Stack<CallStackFrame> callStack = ctx.getCallStack();
        CallStackFrame frame = callStack.peek();
        prnt("function call ended, return value: " + frame.getTempValue());

        ParseTreeNode node = frame.getCurrentExecutableNode().orElseThrow();
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

    private static void statementAssignVariable(CallStackFrame frame) {
        prnt("assignValue..");
        String name = frame.getCurrentExecutableNodeActions().getVariableName();
        frame.assignVariable(name);
        prnt("assign variable.." + name);
    }

    private static void statementCreateVariable(CallStackFrame frame) {
        prnt("createVariable..");
        String name = frame.getCurrentExecutableNodeActions().getVariableName();
        frame.createVariable(name);
        prnt("variable created.." + name);
    }

    private static void statementExecuteBooleanOperation(List<Token> tokens, CallStackFrame frame) {
        prnt("execute Boolean Operation..");
        List<Token> expressionTokens = removeAssignmentPart(tokens);

        List<Token> postfix = InfixToPostfixConverter.convert(expressionTokens);
        Token.prntTokens(expressionTokens);
        Token.prntTokens(postfix);

        Expression<Boolean> parsedExpression = (Expression<Boolean>) BooleanExpressionParser.parse(frame, postfix);
        frame.addTempValue(parsedExpression.interpret());
        frame.getCurrentExecutableNodeActions().setBooleanOperation(false);
        prnt("result of expression: " + frame.getTempValue());
    }


    private static void statementExecuteArithmeticOperation(List<Token> tokens, CallStackFrame frame) {
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

    private static void statementCallFunction(InterpreterContext ctx, List<Token> tokens) {
        prnt("calling function: " + Token.toString(tokens));
        CallStackFrame frame = ctx.getCallStack().peek();
        //=====for debug========
        Optional<ParseTreeNode> option = findFunctionNode(ctx, tokens);
        if (option.isEmpty()) {
            findFunctionNode(ctx, tokens);
        }
        //=============

        ParseTreeNode functionNode = findFunctionNode(ctx, tokens)
                .orElseThrow(() -> new RuntimeException("No function declaration was found:\n" + Token.toString(tokens)));

        Object[] argumentValues = getArguments(frame, tokens);
        if (functionNode.getNodeType() == NodeType.SystemFunction) {
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

    private static Optional<ParseTreeNode> findFunctionNode(InterpreterContext ctx, List<Token> functionCallTokens) {
        CallStackFrame frame = ctx.getCurrentCallStackFrame();
        Optional<ParseTreeNode> systemFunctionOption = findSystemFunction(frame, functionCallTokens);
        if (systemFunctionOption.isPresent()) return systemFunctionOption;

        ParseTreeNode node = findParentClassNode(ctx);
        return node.getChildren().stream()
                .filter(e -> e.getNodeType() == NodeType.CodeBlock)
                .map(ParseTreeNode::getChildren)
                .flatMap(Collection::stream)
                .filter(e -> e.getNodeType() == NodeType.FunctionDeclarationStatement)
                .filter(e -> isSameFunction(frame, e.getTokens(), functionCallTokens)) //todo: change arg names to arg types
                .findFirst();
    }

    private static Optional<ParseTreeNode> findSystemFunction(CallStackFrame frame, List<Token> functionCallTokens) {
        FunctionDeclarationFilter filter = FunctionDeclarationFilter.from(frame, functionCallTokens);
        String name = filter.getName().getValue();
        List<Token> argumentTypes = filter.getArgumentTypes();
        if (name.equals("prnt") && argumentTypes.size() == 1) {
            ParseTreeNode node = new ParseTreeNode(NodeType.SystemFunction, functionCallTokens);
            return Optional.of(node);
        }
        return Optional.empty();
    }

    private static Optional<Object> callSystemFunction(CallStackFrame frame, ParseTreeNode functionNode, Object[] argumentValues) {
        FunctionDeclarationFilter filter = FunctionDeclarationFilter.from(frame, functionNode.getTokens());
        String name = filter.getName().getValue();
        List<Token> argumentTypes = filter.getArgumentTypes();
        if (name.equals("prnt") && argumentTypes.size() == 1) {
            if (argumentValues.length == 0) {
                argumentValues = argumentValues;
            }
            prnt(argumentValues[0]);
            return Optional.empty();
        }
        String message = MessageFormat.format("system function nod found: {0}, {1}", name, Token.toString(argumentTypes));
        throw new RuntimeException(message);
    }

    private static ParseTreeNode findParentClassNode(InterpreterContext ctx) {
        ParseTreeNode node = ctx.getCurrentCallStackFrame().getNode();
        List<Token> tokens = node.getTokens();
        while (node != null && node.getNodeType() != NodeType.Root) {
            node = node.getParent();
            if (node.getNodeType() == NodeType.Class) return node;
        }
        throw new RuntimeException("could not find parent class node.\n" + Token.toString(tokens));
    }

    private static boolean isSameFunction(CallStackFrame frame, List<Token> functionDeclarationTokens, List<Token> functionCallTokens) {
        FunctionDeclarationFilter filter1 = FunctionDeclarationFilter.from(functionDeclarationTokens);
        FunctionDeclarationFilter filter2 = FunctionDeclarationFilter.from(frame, functionCallTokens);
        return filter1.equals(filter2);
    }

    private static  String getCode() {
        String fileName = "TestCode.java";
        String filePath = "/home/zilvinas/git/crappy-java-interpretator/src/main/java/zc/dev/interpreter/test_code/" + fileName;
        TextFileReader reader = TextFileReader.of(filePath);
        return String.join("\n", reader.readAll());
    }
}
