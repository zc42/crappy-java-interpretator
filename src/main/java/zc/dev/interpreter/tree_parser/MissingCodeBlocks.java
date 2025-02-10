package zc.dev.interpreter.tree_parser;

import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class MissingCodeBlocks {

    public static void addMissingCodeBlocks(TreeNode root) {

        Predicate<TreeNode> predicate = e ->
                e.getType() == NodeType.If
                        || e.getType() == NodeType.Else
                        || e.getType() == NodeType.ElseIf
                        || e.getType() == NodeType.WhileStatement
                        || e.getType() == NodeType.ForStatement;

        List<TreeNode> children = ParseTreeNodeUtils.getAllChildren(root, predicate);
        children.forEach(MissingCodeBlocks::_addMissingCodeBlocks);
    }

    private static void _addMissingCodeBlocks(TreeNode node) {
        Optional<TreeNode> option = ParseTreeNodeUtils.getChild(node, NodeType.CodeBlock);
        if(option.isPresent()) return;
        if(node.getChildren().isEmpty()) throw new RuntimeException("Missing children");
        List<TreeNode> children = new ArrayList<>(node.getChildren());
        node.getChildren().clear();
        Token token1 = new Token(TokenType.BRACE, "{");
        Token token2 = new Token(TokenType.BRACE, "}");
        TreeNode codeBlock = new TreeNode(NodeType.CodeBlock, List.of(token1, token2));
        children.forEach(codeBlock::addChild);
        node.addChild(codeBlock);
    }
}
