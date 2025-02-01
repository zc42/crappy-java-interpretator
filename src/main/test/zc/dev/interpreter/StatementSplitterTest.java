package zc.dev.interpreter;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zc.dev.interpreter.lexer.Token;

class StatementSplitterTest {

    @Test
    @DisplayName("int c = a ( b + 1 , a ( 1 , 2 ) ) + 1")
    public void test0() {
        String line = "int c = a ( b + 1 , a ( 1 , 2 ) ) + 1";

        List<Statement> statements = StatementSplitter.split(line);

        assert statements.size() == 4;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v1 = b + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v2 = a ( 1 , 2 )");
        assert Token.toString(statements.get(2).getTokens()).equals("int $v3 = a ( $v1 , $v2 )");
        assert Token.toString(statements.get(3).getTokens()).equals("int c = $v3 + 1");
    }

    @Test
    @DisplayName("b = 1 + a(a + 1, a(1, 2))")
    public void test1() {
        String line = "b = 1 + a(a + 1, a(1, 2))";

        List<Statement> statements = StatementSplitter.split(line);

        assert statements.size() == 4;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v4 = a + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v5 = a ( 1 , 2 )");
        assert Token.toString(statements.get(2).getTokens()).equals("int $v6 = a ( $v4 , $v5 )");
        assert Token.toString(statements.get(3).getTokens()).equals("b = 1 + $v6");
    }

    @Test
    @DisplayName("b > 0 || b + 1 > 0 || a(1)")
    public void test2() {
        String line = "b > 0 || b + 1 > 0 || a(1)";

        List<Statement> statements = StatementSplitter.split(line);

        assert statements.size() == 3;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v7 = b + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v8 = a ( 1 )");
        assert Token.toString(statements.get(2).getTokens()).equals("b > 0 || $v7 > 0 || $v8");
    }



    @Test
    @DisplayName("a % 2 == 0")
    public void modWithBooleanExpression() {
        String line = "boolean c = a % 2 == 0";

        List<Statement> statements = StatementSplitter.split(line);

        assert statements.size() == 2;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = a % 2");
        assert Token.toString(statements.get(1).getTokens()).equals("boolean c = $v0 == 0");
    }

    @Test
    @DisplayName("0 == a % 2")
    public void booleanExpressionWithMod() {
        String line = "boolean c = 0 == a % 2";

        List<Statement> statements = StatementSplitter.split(line);

        assert statements.size() == 2;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = a % 2");
        assert Token.toString(statements.get(1).getTokens()).equals("boolean c = 0 == $v0");
    }
}
