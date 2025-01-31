package interpreter;


import interpreter.lexer.Token;
import interpreter.lexer.TokenType;

import java.util.*;

public class InfixToPostfixConverter {
    private final static Map<String, Integer> precedence = getPrecedences();

    public static List<Token> convert(List<Token> infix) {
        List<Token> postfix = new ArrayList<>();
        Stack<Token> stack = new Stack<>();


        infix.forEach(c -> process(c, postfix, stack));

        while (!stack.isEmpty()) {
            postfix.add(stack.pop());
        }

        return postfix;
    }

    private static void process(Token c, List<Token> postfix, Stack<Token> stack) {
        if (c.getType() == TokenType.NUMBER || c.getType() == TokenType.IDENTIFIER) {
            postfix.add(c);
        } else if (Objects.equals(c.getValue(), "(")) {
            stack.push(c);
        } else if (Objects.equals(c.getValue(), ")")) {
            while (!stack.isEmpty() && !Objects.equals(stack.peek().getValue(), "(")) {
                postfix.add(stack.pop());
            }
            stack.pop(); // Remove '('
        } else if (precedence.containsKey(c.getValue())) {
            while (isPrecedencePriorityHigher(stack, c)) {
                postfix.add(stack.pop());
            }
            stack.push(c);
        }
    }

    private static boolean isPrecedencePriorityHigher(Stack<Token> stack, Token token) {
        if (stack.isEmpty()) return false;
        String key = stack.peek().getValue();
        return InfixToPostfixConverter.precedence.containsKey(key)
                && InfixToPostfixConverter.precedence.get(key) >= InfixToPostfixConverter.precedence.get(token.getValue());
    }

    private static Map<String, Integer> getPrecedences() {
        Map<String, Integer> map = new HashMap<>();
        map.put("+", 1);
        map.put("-", 1);
        map.put("*", 2);
        map.put("/", 2);
        map.put("!", 3);
        map.put("&&", 2);
        map.put("<", 2);
        map.put(">", 2);
        map.put("||", 1);
        return map;
    }

}
