package zc.dev.interpreter.tree_parser.statement.decomposer;

import lombok.Builder;
import lombok.Getter;
import zc.dev.interpreter.tree_parser.NodeType;

@Getter
@Builder(toBuilder = true)
public class NodeTypeAnalysis {
    private final boolean functionCall;
    private final boolean arithmeticExpression;
    private final boolean booleanExpression;
    private final boolean terminal;
    private final NodeType nodeType;
}
