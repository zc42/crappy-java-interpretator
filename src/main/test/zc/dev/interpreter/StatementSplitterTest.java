package zc.dev.interpreter;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zc.dev.interpreter.lexer.Token;

class StatementSplitterTest {

    @Test
    @DisplayName("int c = a ( b + 1 , a ( 1 , 2 ) ) + 1")
    void functionsCallWithArithmeticOperation() {
        String line = "int c = a ( b + 1 , a ( 1 , 2 ) ) + 1";

        List<Statement> statements = new StatementSplitterV1().split(line);

        assert statements.size() == 4;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = b + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v1 = a ( 1 , 2 )");
        assert Token.toString(statements.get(2).getTokens()).equals("int $v2 = a ( $v0 , $v1 )");
        assert Token.toString(statements.get(3).getTokens()).equals("int c = $v2 + 1");
    }

    @Test
    @DisplayName("b = 1 + a(a + 1, a(1, 2))")
    void arithmeticOperationWithFunctionCalls() {
        String line = "b = 1 + a(a + 1, a(1, 2))";

        List<Statement> statements = new StatementSplitterV1().split(line);

        assert statements.size() == 4;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = a + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v1 = a ( 1 , 2 )");
        assert Token.toString(statements.get(2).getTokens()).equals("int $v2 = a ( $v0 , $v1 )");
        assert Token.toString(statements.get(3).getTokens()).equals("b = 1 + $v2");
    }

    @Test
    @DisplayName("b > 0 || b + 1 > 0 || a(1)")
    void booleanAndArithmeticOperationsWithsFunctionCall() {
        String line = "b > 0 || b + 1 > 0 || a(1)";

        List<Statement> statements = new StatementSplitterV1().split(line);

        assert statements.size() == 3;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = b + 1");
        assert Token.toString(statements.get(1).getTokens()).equals("int $v1 = a ( 1 )");
        assert Token.toString(statements.get(2).getTokens()).equals("b > 0 || $v0 > 0 || $v1");
    }

    @Test
    @DisplayName("0 == a % 2")
    void booleanExpressionWithMod() {
        String line = "boolean c = 0 == a % 2";

        List<Statement> statements = new StatementSplitterV1().split(line);

        assert statements.size() == 2;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = a % 2");
        assert Token.toString(statements.get(1).getTokens()).equals("boolean c = 0 == $v0");
    }

    @Test
    @DisplayName("a % 2 == 0")
    void modWithBooleanExpression() {
        String line = "boolean c = a % 2 == 0";

        List<Statement> statements = new StatementSplitterV1().split(line);

        assert statements.size() == 2;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = a % 2");
        assert Token.toString(statements.get(1).getTokens()).equals("boolean c = $v0 == 0");
    }

    @Test
    @DisplayName("prnt(a % 2)")
    void functionCallWithMod() {
        String line = "prnt(a % 2)";

        List<Statement> statements = new StatementSplitterV1().split(line);

        assert statements.size() == 2;
        assert Token.toString(statements.get(0).getTokens()).equals("int $v0 = a % 2");
        assert Token.toString(statements.get(1).getTokens()).equals("prnt ( $v0 )");
    }
}
