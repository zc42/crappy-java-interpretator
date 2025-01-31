package zc.dev.interpreter.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

// Class to convert infix expressions to postfix expressions
public class InfixToPostfixConverter {

    public static String convert(String infix) {
        StringBuilderWithConstrains postfix = StringBuilderWithConstrains.from('(', ')');
        Stack<Character> stack = new Stack<>();

        Map<Character, Integer> precedence = new HashMap<>();
        precedence.put('+', 1);
        precedence.put('-', 1);
        precedence.put('*', 2);
        precedence.put('/', 2);

        for (char c : infix.toCharArray()) {
            if (Character.isDigit(c)) {
                postfix.append(c).append(' ');
            } else if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    postfix.append(stack.pop()).append(' ');
                }
                stack.pop(); // Remove '('
            } else if (precedence.containsKey(c)) {
                while (isPrecedencePriorityHigher(stack, precedence, c)) {
                    postfix.append(stack.pop()).append(' ');
                }
                stack.push(c);
            }
        }

        while (!stack.isEmpty()) {
            postfix.append(stack.pop()).append(' ');
        }

        return postfix.toString().trim();
    }

    private static boolean isPrecedencePriorityHigher(Stack<Character> stack, Map<Character, Integer> precedence, char c) {
        if (stack.isEmpty()) return false;
        Character key = stack.peek();
        return precedence.containsKey(key)
                && precedence.get(key) >= precedence.get(c);
    }
}
