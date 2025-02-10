package zc.dev.interpreter;

import zc.dev.interpreter.call_stack.CallStackFrame;
import zc.dev.interpreter.tree_parser.TreeNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Stack;

@Getter
@RequiredArgsConstructor(staticName = "from")
public class InterpreterContext {
    private final TreeNode rootNode;
    private final Stack<CallStackFrame> callStack = new Stack<>();

    public CallStackFrame getCurrentCallStackFrame() {
        return callStack.peek();
    }

    public void createCallStackFrame(TreeNode node, Object[] args) {
        CallStackFrame frame = new CallStackFrame(node, args);
        callStack.push(frame);
    }
}
