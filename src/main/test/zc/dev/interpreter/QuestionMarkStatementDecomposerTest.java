package zc.dev.interpreter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.tree_parser.TreeNode;
import zc.dev.interpreter.tree_parser.statement.decomposer.QuestionMarkStatementDecomposer;
import zc.dev.interpreter.tree_parser.statement.decomposer.StatementSplitter;

import java.util.List;

class QuestionMarkStatementDecomposerTest {

    @Test
    @DisplayName("int b = y == x ? 1 : y < x ? y == x * 5 ? 2 : 3 : 4")
    void test1() {
        String line = "int b = y == x ? 1 : y < x ? y == x * 5 ? 2 : 3 : 4";

        List<TreeNode> statements = new QuestionMarkStatementDecomposer().decompose(line).orElseThrow();

        assert statements.size() == 4;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = b + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v1 = a ( 1 , 2 )");
        assert Token.toString(statements.get(2).getTokens()).equals("int $v2 = a ( $v0 , $v1 )");
        assert Token.toString(statements.get(3).getTokens()).equals("int c = $v2 + 1");
    }
}
