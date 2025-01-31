package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.Pair;
import zc.dev.interpreter.Statement;
import zc.dev.interpreter.StatementSplitter;
import zc.dev.interpreter.TextFileReader;
import zc.dev.interpreter.lexer.LexerWithFSA;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static zc.dev.interpreter.Pair.P;

public class CustomCodeParser {

    public static void main(String[] args) {
        String fileName = "TestCode.java";
//        String fileName = "Token.java";
//        String fileName = "CustomParseTree.java";
        String filePath = "/home/zilvinas/git/reversals_repo/reversal_signals/RSVisualizer/src/main/java/zc/dev/stuff/zc.dev.interpreter/test_code/" + fileName;

        TextFileReader reader = TextFileReader.of(filePath);
        String code = String.join("\n", reader.readAll());

        ParseTreeNode root = parseCode(code);
        root.printTree("");
    }

    private final static List<Pair<NodeType, Consumer<ParseTreeContext>>> processors =
            List.of(P(NodeType.CodeBlock, CustomCodeParser::processCodeBlock),
                    P(NodeType.Modifier, CustomCodeParser::processStatementWithModifier),
                    P(NodeType.FunctionDeclarationStatement, CustomCodeParser::processFunctionDeclarationStatement),
                    P(NodeType.RegularStatement, e -> processSemicolonStatement(e, NodeType.RegularStatement)),
                    P(NodeType.ReturnStatement, e -> processSemicolonStatement(e, NodeType.ReturnStatement)),
                    P(NodeType.Package, e -> processSemicolonStatement(e, NodeType.Package)),
                    P(NodeType.Import, e -> processSemicolonStatement(e, NodeType.Import)),
                    P(NodeType.ClassField, e -> processSemicolonStatement(e, NodeType.ClassField)),
                    P(NodeType.Class, CustomCodeParser::processClassStatement),
                    P(NodeType.WhileStatement, e -> processBooleanStatement(e, NodeType.WhileStatement)),
                    P(NodeType.IfElseStatement, CustomCodeParser::processIfElseStatement),
                    P(NodeType.TryCatch, CustomCodeParser::processTryCatchStatement),
                    P(NodeType.Comment, CustomCodeParser::processComment),
                    P(NodeType.Annotation, CustomCodeParser::processAnnotation)
            );

    public static ParseTreeNode parseCode(String code) {
        ParseTreeContext ctx = getParseTreeContext(code);
        decomposeStatements(ctx);
        ParseTreeNode root = ctx.getRootNode();
        LocalVariableMarking.addCodeBlockMarks(root);
        ParseTreeNumerator.addLineNumbers(root);
        ControlFlowForks.addControlFlowForks(root);
        ParseTreeNumerator.addExecutablesToFunctionDeclarationNodes(root);
        return root;
    }

    private static void decomposeStatements(ParseTreeContext ctx) {
        ParseTreeNode root = ctx.getRootNode();
        Stack<ParseTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ParseTreeNode node = stack.pop();
            node.getChildren().forEach(stack::push);
            getDecomposedStatements(node);
        }
    }

    private static void getDecomposedStatements(ParseTreeNode node) {
        List<Statement> statements = StatementSplitter.split(node);
        if (statements.size() == 1) return;
        ParseTreeNode decomposedStatements = new ParseTreeNode(NodeType.DecomposedStatements);
        statements.forEach(e -> decomposedStatements.addChild(new ParseTreeNode(e.getType(), e.getTokens())));
        node.addChild(decomposedStatements);
        if (node.getChildren().size() == 1) return;
        Comparator<ParseTreeNode> comparator = (o1, o2) -> {
            int c1 = o1.getNodeType() == NodeType.DecomposedStatements ? -1 : 1;
            int c2 = o2.getNodeType() == NodeType.DecomposedStatements ? -1 : 1;
            return c1 - c2;
        };
        node.getChildren().sort(comparator);
    }

    private static  ParseTreeContext getParseTreeContext(String code) {
        List<Token> tokens = LexerWithFSA.tokenize(code);
        ParseTreeContext ctx = ParseTreeContext.from(tokens);
        while (ctx.hasNext()) {
            try {

                NodeType nodeType = detectNodeType(ctx);
                Optional<Consumer<ParseTreeContext>> consumer = processors.stream()
                        .filter(e -> e.getKey().equals(nodeType))
                        .findFirst()
                        .map(Pair::getValue);

                consumer.ifPresent(e -> e.accept(ctx));
                consumer.orElseThrow(() -> new ParseTreeException(ctx, "Unknown node type: " + nodeType));

            } catch (Exception e) {
                throw new ParseTreeException(ctx, e);
            }
        }
        return ctx;
    }

    private static NodeType detectNodeType(ParseTreeContext ctx) {
        Token token = ctx.peek();
        if (token.getType() == TokenType.NewLine)
            while (ctx.peek().getType() == TokenType.NewLine) {
                ctx.next();
                token = ctx.peek();
            }

        ParseTreeNode currentNode = ctx.getCurrentNode();
        if (currentNode == null) {
            currentNode = currentNode;
        }
        NodeType currentNodeType = currentNode.getNodeType();
        if (token.getType() == TokenType.BRACE) {
            if (currentNodeType == NodeType.AssigmentStatement) return NodeType.ArrayDeclaration;
            return NodeType.CodeBlock;
        } else if (Objects.equals(token.getValue(), "class")) return NodeType.Class;
        else if (Objects.equals(token.getValue(), "if")) return NodeType.IfElseStatement;
        else if (Objects.equals(token.getValue(), "else")) return NodeType.IfElseStatement;
        else if (Objects.equals(token.getValue(), "for")) return NodeType.ForStatement;
        else if (Objects.equals(token.getValue(), "while")) return NodeType.WhileStatement;
        else if (Objects.equals(token.getValue(), "do")) return NodeType.DoStatement;
        else if (Objects.equals(token.getValue(), "switch")) return NodeType.SwitchStatement;
        else if (Objects.equals(token.getValue(), "case")) return NodeType.Case;
        else if (Objects.equals(token.getValue(), "public")) return NodeType.Modifier;
        else if (Objects.equals(token.getValue(), "private")) return NodeType.Modifier;
        else if (Objects.equals(token.getValue(), "static")) return NodeType.Modifier;
        else if (Objects.equals(token.getValue(), "void")) return NodeType.Void;
        else if (Objects.equals(token.getValue(), "package")) return NodeType.Package;
        else if (Objects.equals(token.getValue(), "import")) return NodeType.Import;
        else if (Objects.equals(token.getValue(), "return")) return NodeType.ReturnStatement;
        else if (Objects.equals(token.getValue(), "try")) return NodeType.TryCatch;
        else if (Objects.equals(token.getValue(), "catch")) return NodeType.TryCatch;
        else if (Objects.equals(token.getValue(), "finally")) return NodeType.TryCatch;
        else if (ctx.containsTokenValuesInCurrentPosition("/", "/")) return NodeType.Comment;
        else if (Objects.equals(token.getValue(), "@")) return NodeType.Annotation;

        boolean unfinishedFunctionDeclaration = currentNodeType ==
                NodeType.FunctionDeclarationStatement
                && Objects.equals(currentNode.getLastToken().getValue(), "(");
        if (unfinishedFunctionDeclaration) return NodeType.FunctionDeclarationStatement;

        boolean unfinishedClassDeclaration = currentNodeType ==
                NodeType.Class
                && Objects.equals(currentNode.getLastToken().getValue(), "class");
        if (unfinishedClassDeclaration) return NodeType.Class;

        boolean unfinishedClassField = currentNodeType ==
                NodeType.ClassField
                && Objects.equals(currentNode.getLastToken().getValue(), "=");
        if (unfinishedClassField) return NodeType.ClassField;

        return NodeType.RegularStatement;
    }


    private static void processSemicolonStatement(ParseTreeContext ctx, NodeType nodeType) {
        List<Token> tokens = new ArrayList<>();
        while (ctx.hasNext()) {
            Token token = ctx.next();
            tokens.add(token);
            if (token.getType() == TokenType.SEMI_COLON) break;
        }

        if (tokens.isEmpty() || !Objects.equals(tokens.get(tokens.size() - 1).getValue(), ";"))
            throw new UnsupportedOperationException("tokens.isEmpty() || !Objects.equals(tokens.get(tokens.size() - 1).value, \";\")");

         List<String> values = tokens.stream().map(Token::getValue).collect(Collectors.toList());
        List<String> testValues = List.of("return", "ctx", ".", "getParseTreeRoot", "(", ")", ";");
        if (values.containsAll(testValues)) {
            values = values;
        }

        ParseTreeNode currentNode = ctx.getCurrentNode();
        if (currentNode.getNodeType() == nodeType) {
            tokens.forEach(currentNode::addToken);
        } else if ((nodeType == NodeType.RegularStatement || nodeType == NodeType.ReturnStatement)
                && isNodeTerminated(currentNode)) {
            currentNode = findOpenCodeBlock(currentNode);
            ParseTreeNode node = new ParseTreeNode(nodeType, tokens);
            currentNode.addChild(node);
            ctx.setCurrentNode(currentNode);
        } else {
            ParseTreeNode node = new ParseTreeNode(nodeType, tokens);
            currentNode.addChild(node);
        }
    }

    private static boolean isNodeTerminated(ParseTreeNode currentNode) {
        return isWithClosedCodeBlock(currentNode)
                || isNodeWithOneStatement(currentNode, NodeType.If)
                || isNodeWithOneStatement(currentNode, NodeType.Else)
                || isNodeWithOneStatement(currentNode, NodeType.ElseIf);
//                || isNodeRegularStatement(currentNode, NodeType.ForStatement)
//                || isNodeRegularStatement(currentNode, NodeType.WhileStatement);
    }

    private static boolean isNodeWithOneStatement(ParseTreeNode currentNode, NodeType nodeType) {
        if (currentNode.getNodeType() != nodeType) return false;

        List<ParseTreeNode> children = currentNode.getChildren();
        if (children.size() != 1) return false;

        NodeType childNodeType = children.get(0).getNodeType();
        return childNodeType == NodeType.RegularStatement || childNodeType == NodeType.ReturnStatement;
    }

    private static ParseTreeNode findOpenCodeBlock(ParseTreeNode node) {
        while (node.getNodeType() != NodeType.Root) {
            if (isCodeBlock(node, true)) return node;
            node = node.getParent();
        }
        return node;
    }

    private static boolean isCodeBlock(ParseTreeNode node, boolean open) {
        if (node.getNodeType() != NodeType.CodeBlock) return false;
        List<String> list = node.getTokens().stream().map(Token::getValue).collect(Collectors.toList());
        return open
                ? list.contains("{") && !list.contains("}")
                : list.contains("{") && list.contains("}");
    }

    private static boolean isWithClosedCodeBlock(ParseTreeNode node) {
        return node.getChildren().stream().anyMatch(e -> isCodeBlock(e, false));
    }

    private static void processIfElseStatement(ParseTreeContext ctx) {
        String tokenValue = ctx.peek().getValue();
        boolean isIf = Objects.equals(tokenValue, "if");
        boolean isElseIf = ctx.containsTokenValuesInCurrentPosition("else", "if");
        boolean isElse = Objects.equals(tokenValue, "else");

        ParseTreeNode node;
        if (isIf) {
            List<Token> tokens = getTokensTerminatedWithParentheses(ctx);
            node = new ParseTreeNode(NodeType.If, tokens);
            ParseTreeNode node0 = new ParseTreeNode(NodeType.IfElseStatement);
            node0.addChild(node);
            ParseTreeNode currentNode = findOpenCodeBlock(ctx.getCurrentNode());
            currentNode.addChild(node0);
            ctx.setCurrentNode(node);
        } else if (isElseIf) {
            List<Token> tokens = getTokensTerminatedWithParentheses(ctx);
            node = new ParseTreeNode(NodeType.ElseIf, tokens);
            ParseTreeNode currentNode = findNode(ctx, NodeType.IfElseStatement);
            currentNode.addChild(node);
            ctx.setCurrentNode(node);
        } else if (isElse) {
            node = new ParseTreeNode(NodeType.Else, ctx.next());
            ParseTreeNode currentNode = findNode(ctx, NodeType.IfElseStatement);
            currentNode.addChild(node);
            ctx.setCurrentNode(node);
        } else throw new ParseTreeException(ctx, "!isIf && !isElseIf && !isElse");
    }

    private static void processAnnotation(ParseTreeContext ctx) {
        List<Token> tokens = new ArrayList<>();
        IntStream.range(0, 2).forEach(i -> tokens.add(ctx.next()));
        getTokensEnclosedWithParentheses(ctx).map(tokens::addAll);

        ParseTreeNode parseTreeNode = new ParseTreeNode(NodeType.Annotation, tokens);
        ctx.saveAnnotation(parseTreeNode);
    }

    private static Optional<List<Token>> getTokensEnclosedWithParentheses(ParseTreeContext ctx) {
        return Objects.equals(ctx.peek().getValue(), "(")
                ? Optional.of(getTokensTerminatedWithParentheses(ctx))
                : Optional.empty();
    }

    private static void processComment(ParseTreeContext ctx) {

        boolean foundTerminal = false;
        List<Token> tokens = new ArrayList<>();

        while (ctx.hasNext()) {
            Token token = ctx.next();
            foundTerminal = token.getType() == TokenType.NewLine;
            if (foundTerminal) break;
            tokens.add(token);
        }

        if (!foundTerminal)
            throw new UnsupportedOperationException("could not find terminal literal for new line");

        ParseTreeNode node = new ParseTreeNode(NodeType.Comment, tokens);
        ctx.getCurrentNode().addChild(node);
    }

    private static void processTryCatchStatement(ParseTreeContext ctx) {
        boolean isTry = Objects.equals(ctx.peek().getValue(), "try");
        if (isTry) processTryStatement(ctx);
        else processCatchFinallyStatements(ctx);
    }

    private static void processCatchFinallyStatements(ParseTreeContext ctx) {
        ParseTreeNode node;
        if (Objects.equals(ctx.peek().getValue(), "catch")) {
            List<Token> tokens = getTokensTerminatedWithParentheses(ctx);
            node = new ParseTreeNode(NodeType.Catch, tokens);
        } else if (Objects.equals(ctx.peek().getValue(), "finally")) {
            node = new ParseTreeNode(NodeType.Catch, ctx.next());
        } else throw new ParseTreeException(ctx, "!isCatch && !isFinally");

        ParseTreeNode currentNode = findNode(ctx, NodeType.TryCatch);
        currentNode.addChild(node);
        ctx.setCurrentNode(node);
    }

    private static ParseTreeNode findNode(ParseTreeContext ctx, NodeType nodeType) {
        ParseTreeNode node = ctx.getCurrentNode();
        while (node.getNodeType() != NodeType.Root) {
            if (node.getNodeType() == nodeType) return node;
            node = node.getParent();
        }
        return node;
    }

    private static void processTryStatement(ParseTreeContext ctx) {
        Token token = ctx.next();
        if (!Objects.equals(token.getValue(), "try"))
            throw new ParseTreeException(ctx, "!Objects.equals(token.value, \"try\")");
        ParseTreeNode node0 = new ParseTreeNode(NodeType.TryCatch);
        ParseTreeNode node = new ParseTreeNode(NodeType.Try, List.of(token));
        ParseTreeNode currentNode = findOpenCodeBlock(ctx.getCurrentNode());
        node0.addChild(node);
        currentNode.addChild(node0);

        ctx.setCurrentNode(node);
    }


    private static void processBooleanStatement(ParseTreeContext ctx, NodeType nodeType) {
        List<Token> tokens = getTokensTerminatedWithParentheses(ctx);
        ParseTreeNode node = new ParseTreeNode(nodeType, tokens);
        ctx.getCurrentNode().addChild(node);
        ctx.setCurrentNode(node);
    }

    private static  List<Token> getTokensTerminatedWithParentheses(ParseTreeContext ctx) {
        int t = 0;
        List<Token> tokens = new ArrayList<>();
        boolean foundTerminal = false;
        while (ctx.hasNext()) {
            Token token = ctx.next();
            tokens.add(token);

            if (Objects.equals(token.getValue(), "(")) t++;
            if (Objects.equals(token.getValue(), ")")) {
                foundTerminal = true;
                t--;
            }

            if (foundTerminal && t == 0) break;
        }

        if (!foundTerminal)
            throw new UnsupportedOperationException("could not find terminal literal:  \")\")");

        return tokens;
    }

    private static void processClassStatement(ParseTreeContext ctx) {
        ParseTreeNode currentNode = ctx.getCurrentNode();
        if (currentNode.getNodeType() != NodeType.Class)
            throw new RuntimeException("ctx.getCurrentNode().getNodeType() != NodeType.Class");

        String terminal = "{";
        List<Token> tokens = new ArrayList<>();
        boolean nextIsOpenBrace = false;
        while (ctx.hasNext()) {
            Token token = ctx.next();
            tokens.add(token);
            nextIsOpenBrace = ctx.peek(0).map(Token::getValue).filter(e -> e.equals(terminal)).isPresent();
            if (nextIsOpenBrace) break;
        }

        if (!nextIsOpenBrace)
            throw new UnsupportedOperationException("could mot find terminal literal:  \"" + terminal + "\")");

        tokens.forEach(currentNode::addToken);
    }

    private static void processStatementWithModifier(ParseTreeContext ctx) {
        //class -> class
        //class function -> identifier + '('
        //class field -> identifier + ';'

        boolean gotClass = false;
        boolean gotOpenBrace = false;
        boolean gotSemicolon = false;
        boolean gotIdentifier = false;
        boolean gotAssignment = false;

        List<Token> tokens = new ArrayList<>();
        while (ctx.hasNext()) {
            Token token = ctx.next();
            gotClass = gotClass || Objects.equals(token.getValue(), "class");
            gotOpenBrace = gotOpenBrace || Objects.equals(token.getValue(), "(");
            gotSemicolon = gotSemicolon || token.getType() == TokenType.SEMI_COLON;
            gotAssignment = gotAssignment || Objects.equals(token.getValue(), "=");
            ;
            gotIdentifier = gotIdentifier || token.getType() == TokenType.IDENTIFIER;
            tokens.add(token);

            if (gotClass) break;
            if (gotIdentifier && gotOpenBrace) break;
            if (gotIdentifier && gotSemicolon) break;
            if (gotIdentifier && gotAssignment) break;
        }

        NodeType nodeType = gotClass
                ? NodeType.Class
                : gotIdentifier && gotOpenBrace
                ? NodeType.FunctionDeclarationStatement
                : gotIdentifier && (gotAssignment || gotSemicolon)
                ? NodeType.ClassField
                : null;

        if (nodeType == null) throw new ParseTreeException(ctx, "could not determine node type");

        ParseTreeNode node = new ParseTreeNode(nodeType, tokens);
        if (!ctx.getAnnotations().isEmpty()) {
            ctx.getAnnotations().forEach(node::addChild);
            ctx.clearAnnotations();
        }

        ParseTreeNode parentNode = findOpenCodeBlock(ctx.getCurrentNode());
        parentNode.addChild(node);
        ctx.setCurrentNode(node);
    }

    private static void processFunctionDeclarationStatement(ParseTreeContext ctx) {
        List<Token> tokens = new ArrayList<>();
        while (ctx.hasNext()) {
            Token token = ctx.next();
            tokens.add(token);
            if (Objects.equals(token.getValue(), ")")) break;
        }

        if (tokens.isEmpty() || !Objects.equals(tokens.get(tokens.size() - 1).getValue(), ")"))
            throw new UnsupportedOperationException("tokens.isEmpty() || !Objects.equals(tokens.get(tokens.size() - 1).value, \")\")");

        tokens.forEach(e -> ctx.getCurrentNode().addToken(e));
    }

    private static final List<NodeType> codeBlockNodeTypes = List.of(
            NodeType.CodeBlock, NodeType.Class, NodeType.Root,
            NodeType.DoStatement,
            NodeType.If, NodeType.ElseIf, NodeType.Else,
            NodeType.WhileStatement, NodeType.SwitchStatement,
            NodeType.FunctionDeclarationStatement,
            NodeType.Try, NodeType.Catch, NodeType.Finally,
            NodeType.ForStatement);

    private static void processCodeBlock(ParseTreeContext ctx) {
        Token token = ctx.peek();
        if (token.getType() != TokenType.BRACE) return;

        ParseTreeNode currentNode = ctx.getCurrentNode();
        NodeType nodeType = currentNode.getNodeType();
        if (nodeType == NodeType.AssigmentStatement) return;

        ctx.next();

        //open
        if (Objects.equals(token.getValue(), "{")) {
            if (!codeBlockNodeTypes.contains(nodeType))
                throw new RuntimeException("codeBlock can't go under: " + nodeType);

            ParseTreeNode node = new ParseTreeNode(NodeType.CodeBlock);
            node.addToken(token);
            currentNode.addChild(node);
            ctx.setCurrentNode(node);
        } else {//close

            currentNode = currentNode.getNodeType() != NodeType.CodeBlock
                    ? findOpenCodeBlock(currentNode)
                    : currentNode;

            currentNode.addToken(token);
            ParseTreeNode parent = currentNode.getParent();
            ctx.setCurrentNode(parent);
        }
    }

    public static ParseTreeNode parseCodeOS(String code) {
        int a;
        a = 1;
        int b = 1;
        int c = a + b;


        // Root node for the parse tree

        ParseTreeNode root = new ParseTreeNode(NodeType.Class);
        root.addToken(new Token(TokenType.IDENTIFIER, "CustomParseTree"));

        // Add the main method declaration
        ParseTreeNode mainMethod = new ParseTreeNode(NodeType.FunctionDeclarationStatement);
        mainMethod.addToken(new Token(TokenType.IDENTIFIER, "main"));
        root.addChild(mainMethod);

        // Add method modifiers
        mainMethod.addChild(new ParseTreeNode(NodeType.Modifier, Token.KeywordToken("public")));
        mainMethod.addChild(new ParseTreeNode(NodeType.Modifier, Token.KeywordToken(" static ")));

        // Add method parameters
        List<Token> params = List.of(
                new Token(TokenType.KEYWORD, "String[]"), //todo: split String and '[' ']'
                new Token(TokenType.IDENTIFIER, "args"));

        ParseTreeNode parameters = new ParseTreeNode(NodeType.Parameters, params);
        mainMethod.addChild(parameters);

        // Add method body
        List<Token> braces = List.of(new Token(TokenType.BRACE, "{"), new Token(TokenType.BRACE, "}"));
        ParseTreeNode methodBody = new ParseTreeNode(NodeType.CodeBlock, braces);
        mainMethod.addChild(methodBody);

        // Add variable declaration inside method body
        List<Token> t = List.of(
                new Token(TokenType.KEYWORD, "int[]"), //todo: split int and '[' ']'
                new Token(TokenType.IDENTIFIER, "arr"));
        ParseTreeNode varDeclaration = new ParseTreeNode(NodeType.VariableDeclaration, t);
        varDeclaration.addChild(new ParseTreeNode(NodeType.AssigmentStatement, new Token(TokenType.NUMBER, "{12, 35, 1, 10, 34, 1}")));
        methodBody.addChild(varDeclaration);

        // Add method call inside method body
        ParseTreeNode methodCall = new ParseTreeNode(NodeType.FunctionCallStatement, new Token(TokenType.IDENTIFIER, "print2largest"));
        methodCall.addChild(new ParseTreeNode(NodeType.Parameters, new Token(TokenType.IDENTIFIER, "print2largest")));
        methodBody.addChild(methodCall);

        return root;
    }

    public static class ParseTreeException extends RuntimeException {

        public ParseTreeException(String message) {
            super(message);
        }

        public ParseTreeException(ParseTreeContext ctx, String message) {
            super(MessageFormat.format(
                    "{0}\ntree:\n{1}",
                    message,
                    ctx.getRootNode().getTreeAsString("")));
        }

        public ParseTreeException(ParseTreeContext ctx, Exception exception) {
            super(MessageFormat.format(
                    "{0}\ntree:\n{1}",
                    exception.getMessage(),
                    ctx.getRootNode().getTreeAsString("")));
        }
    }
}
