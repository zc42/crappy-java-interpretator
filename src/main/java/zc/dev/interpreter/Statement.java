package zc.dev.interpreter;

import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.tree_parser.NodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(staticName = "from")
public class Statement {
    @Setter
    private NodeType type;
    private final List<Token> tokens = new ArrayList<>();

    public static Statement of(NodeType type, List<Token> tokens) {
        Statement statement = Statement.from(type);
        statement.tokens.addAll(tokens);
        return statement;
    }

    public void addToken(Token token) {
        tokens.add(token);
    }

    public Token getToken(int index) {
        return tokens.get(index);
    }
}
