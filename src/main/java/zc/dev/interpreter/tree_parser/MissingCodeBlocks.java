package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class MissingCodeBlocks {

    public static void addMissingCodeBlocks(ParseTreeNode root) {

        Predicate<ParseTreeNode> predicate = e ->
                e.getNodeType() == NodeType.If
                        || e.getNodeType() == NodeType.Else
                        || e.getNodeType() == NodeType.ElseIf
                        || e.getNodeType() == NodeType.WhileStatement
                        || e.getNodeType() == NodeType.ForStatement;

        List<ParseTreeNode> children = ParseTreeNodeUtils.getAllChildren(root, predicate);
        children.forEach(MissingCodeBlocks::_addMissingCodeBlocks);
    }

    private static void _addMissingCodeBlocks(ParseTreeNode node) {
        Optional<ParseTreeNode> option = ParseTreeNodeUtils.getChild(node, NodeType.CodeBlock);
        if(option.isPresent()) return;
        if(node.getChildren().isEmpty()) throw new RuntimeException("Missing children");
        List<ParseTreeNode> children = new ArrayList<>(node.getChildren());
        node.getChildren().clear();
        Token token1 = new Token(TokenType.BRACE, "{");
        Token token2 = new Token(TokenType.BRACE, "}");
        ParseTreeNode codeBlock = new ParseTreeNode(NodeType.CodeBlock, List.of(token1, token2));
        children.forEach(codeBlock::addChild);
        node.addChild(codeBlock);
    }
}
