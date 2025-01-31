package interpreter;

import interpreter.call_stack.CallStackFrame;
import interpreter.tree_parser.ParseTreeNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Stack;

@Getter
@RequiredArgsConstructor(staticName = "from")
public class InterpreterContext {
    private final ParseTreeNode rootNode;
    private final Stack<CallStackFrame> callStack = new Stack<>();

    public CallStackFrame getCurrentCallStackFrame() {
        return callStack.peek();
    }

    public void createCallStackFrame(ParseTreeNode node, Object[] args) {
        CallStackFrame frame = new CallStackFrame(node, args);
        callStack.push(frame);
    }
}
