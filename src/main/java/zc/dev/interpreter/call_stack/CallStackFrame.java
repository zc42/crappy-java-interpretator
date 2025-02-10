package zc.dev.interpreter.call_stack;

import zc.dev.interpreter.StatementActions;
import zc.dev.interpreter.lexer.Token;
import zc.dev.interpreter.lexer.TokenType;
import zc.dev.interpreter.tree_parser.TreeNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static zc.dev.interpreter.Utils.prnt;

public class CallStackFrame {
    @Getter
    private final TreeNode node;
    private ObjectValue tempValue;

    private final Map<String, Optional<Object>> localVariables = new HashMap<>();

    private final Stack<Integer> codeBlockMarks = new Stack<>();
    private final Map<Integer, List<String>> codeBlockVariableNames = new HashMap<>();
    private final Map<Integer, TreeNode> executablesMap = new HashMap<>();
    private List<Integer> codeLinesNumbers;
    private TreeNode currentExecutableNode;
    @Getter
    private StatementActions currentExecutableNodeActions;

    public CallStackFrame(TreeNode node, Object[] args) {
        this.node = node;
        initExecutablesMap(node.getExecutableNodes());
        addArgsToVariables(node, args);
    }

    private void initExecutablesMap(List<TreeNode> executableNodes) {
        if (executableNodes == null || executableNodes.isEmpty())
            throw new RuntimeException("No executable nodes found");
        Optional<TreeNode> nullLineNumberNode = executableNodes.stream().filter(e -> e.getLineNb() == null).findFirst();
        if (nullLineNumberNode.isPresent())
            throw new RuntimeException("Found node without line number: " + Token.toString(nullLineNumberNode.get().getTokens()));

        executableNodes.forEach(e -> executablesMap.put(e.getLineNb(), e));
        codeLinesNumbers = executablesMap.values().stream().map(TreeNode::getLineNb).sorted().collect(Collectors.toList());
        Integer key = codeLinesNumbers.getFirst();
        currentExecutableNode = executablesMap.get(key);
        currentExecutableNodeActions = currentExecutableNode.getStatementActions().getCopy();
    }

    private void addArgsToVariables(TreeNode node, Object[] args) {
        List<Token> tokens = node.getTokens();
        int start = tokens.indexOf(new Token(TokenType.PARENTHESES, "("));
        List<Token> argNames = tokens.stream()
                .skip(start)
                .filter(e -> e.getType() == TokenType.IDENTIFIER)
                .toList();

        IntStream.range(0, argNames.size()).forEach(i -> {
            String name = argNames.get(i).getValue();
            Object value = i < args.length ? args[i] : null;
            localVariables.put(name, Optional.ofNullable(value));
        });
    }

    public void createVariable(String name) {
        if (localVariables.containsKey(name))
            throw new IllegalArgumentException("Variable " + name + " already exists");
        localVariables.put(name, Optional.empty());
        currentExecutableNodeActions.setCreateVariable(false);

        if (codeBlockMarks.isEmpty()) return;
        Integer key = codeBlockMarks.peek();
        codeBlockVariableNames.putIfAbsent(key, new ArrayList<>());
        codeBlockVariableNames.get(key).add(name);
    }

    public void addTempValue(Object value) {
        tempValue = ObjectValue.from(value);
    }

    public void assignVariable(String name) {
        if (!localVariables.containsKey(name)) throw new RuntimeException("variable " + name + " not found");
        if (tempValue == null) throw new RuntimeException("tempValue is empty");
        localVariables.put(name, Optional.of(tempValue.value));
        tempValue = null;
        currentExecutableNodeActions.setAssignValue(false);
    }

    public Object getVariableValue(String name) {
        if (!localVariables.containsKey(name)) throw new RuntimeException("variable " + name + " not found");
        return localVariables.get(name)
                .orElseThrow(() -> new RuntimeException("variable " + name + " not assigned"));
    }

    public Optional<Object> getTempValue() {
        return ObjectValue.asOptional(tempValue);
    }

    public void setTempValue(Object value) {
        prnt(MessageFormat.format("setting tempValue: {0}", value));
        tempValue = ObjectValue.from(value);
    }

    public String getVariableType(String name) {
        if (!localVariables.containsKey(name)) throw new RuntimeException("variable " + name + " not found");
        return "int";
    }

    public void pushControlBlock() {
        int key = codeBlockMarks.isEmpty() ? 0 : codeBlockMarks.peek() + 1;
        codeBlockMarks.push(key);
        prnt("push key=" + key);
        currentExecutableNodeActions.setPushControlBlock(false);
    }

    public void popControlBlock() {
        if (codeBlockMarks.isEmpty()) throw new RuntimeException("codeBlockMarks is empty");
        int key = codeBlockMarks.pop();
        currentExecutableNodeActions.setPopControlBlock(false);
        prnt("pop key=" + key);
        if (codeBlockVariableNames.containsKey(key)) {
//            throw new RuntimeException("!codeBlockVariableNames.containsKey(codeBlockMarkNode)");
            codeBlockVariableNames.get(key).forEach(localVariables::remove);
            codeBlockVariableNames.remove(key);
        }
    }

    public Optional<TreeNode> getCurrentExecutableNode() {
        if (currentExecutableNode == null) return Optional.empty();
        if (!currentExecutableNodeActions.isAllDone()) return Optional.of(currentExecutableNode);

        Optional<Integer> nextLineNbOption = getNextLineNb(currentExecutableNode.getLineNb());
        if (nextLineNbOption.isEmpty()) return Optional.empty();
        int nextLineNb = nextLineNbOption.get();
        if (!executablesMap.containsKey(nextLineNb)) return Optional.empty();
        currentExecutableNode = executablesMap.get(nextLineNb);
        currentExecutableNodeActions = currentExecutableNode.getStatementActions().getCopy();
        return Optional.of(currentExecutableNode);
    }

    private Optional<Integer> getNextLineNb(int lineNb) {
        int i = codeLinesNumbers.indexOf(lineNb) + 1;
        if (i >= codeLinesNumbers.size()) return Optional.empty();
        return Optional.of(codeLinesNumbers.get(i));
    }

    public void goTo(int lineNb) {
        if (executablesMap.containsKey(lineNb)) {
            currentExecutableNode = executablesMap.get(lineNb);
            currentExecutableNodeActions = currentExecutableNode.getStatementActions().getCopy();
        } else {
            currentExecutableNode = null;
            currentExecutableNodeActions = null;
            prnt("code line not found, line " + lineNb);
        }
    }

    @RequiredArgsConstructor(staticName = "from")
    static class ObjectValue {
        private final Object value;

        public static Optional<Object> asOptional(ObjectValue tempValue) {
            return tempValue == null ? Optional.empty() : Optional.of(tempValue.value);
        }

    }
}

