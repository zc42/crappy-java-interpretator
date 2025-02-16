package zc.dev.interpreter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.tree_parser.TreeNode;
import zc.dev.interpreter.tree_parser.TreeNodeFlattener;
import zc.dev.interpreter.tree_parser.statement.decomposer.QuestionMarkStatementDecomposer;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static zc.dev.interpreter.Utils.prnt;

class QuestionMarkStatementDecomposerTest {

    @Test
    @DisplayName("int b = y == x ? 1 : y < x ? y == x * 5 ? 2 : 3 : 4")
    void test1() {
        String line = "int b = y == x ? 1 : y < x ? y == x * 5 ? 2 : 3 : 4";

        List<TreeNode> nodes = new QuestionMarkStatementDecomposer().decompose(line).orElseThrow();
        nodes = nodes.stream().map(TreeNodeFlattener::makeFlat).flatMap(Collection::stream).toList();

        nodes.forEach(this::prntInfo);

        assert nodes.size() == 27;
        assert Token.toString(nodes.get(0).getTokens()).equals("int $v0 = b + 1");
        assert Token.toString(nodes.get(1).getTokens()).equals("int $v1 = a ( 1 , 2 )");
        assert Token.toString(nodes.get(2).getTokens()).equals("int $v2 = a ( $v0 , $v1 )");
        assert Token.toString(nodes.get(3).getTokens()).equals("int c = $v2 + 1");
    }

    private void prntInfo(TreeNode node) {
        String tokens = node.getTokens().stream().map(Token::getValue).collect(Collectors.joining(" "));
        String message = MessageFormat.format("{0}: {1}", node.getType(), tokens);
        prnt(message);
    }


}
