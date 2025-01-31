package zc.dev.interpreter.example;

import java.util.Stack;

// Expression interface
interface DoubleExpression {
    double interpret();
}

// Terminal expression for numbers
class NumberExpression implements DoubleExpression {
    private final double number;

    public NumberExpression(double number) {
        this.number = number;
    }

    @Override
    public double interpret() {
        return number;
    }
}

// Non-terminal expressions for arithmetic operations
class AdditionExpression implements DoubleExpression {
    private final DoubleExpression left;
    private final DoubleExpression right;

    public AdditionExpression(DoubleExpression left, DoubleExpression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public double interpret() {
        return left.interpret() + right.interpret();
    }
}

class SubtractionExpression implements DoubleExpression {
    private final DoubleExpression left;
    private final DoubleExpression right;

    public SubtractionExpression(DoubleExpression left, DoubleExpression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public double interpret() {
        return left.interpret() - right.interpret();
    }
}

class MultiplicationExpression implements DoubleExpression {
    private final DoubleExpression left;
    private final DoubleExpression right;

    public MultiplicationExpression(DoubleExpression left, DoubleExpression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public double interpret() {
        return left.interpret() * right.interpret();
    }
}

class DivisionExpression implements DoubleExpression {
    private final DoubleExpression left;
    private final DoubleExpression right;

    public DivisionExpression(DoubleExpression left, DoubleExpression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public double interpret() {
        return left.interpret() / right.interpret();
    }
}

// Parser to convert input string into an expression tree
class ExpressionParser {
    public static DoubleExpression parse(String input) {
        Stack<DoubleExpression> stack = new Stack<>();
        String[] tokens = input.split(" ");

        for (String token : tokens) {
            switch (token) {
                case "+": {
                    DoubleExpression right = stack.pop();
                    DoubleExpression left = stack.pop();
                    stack.push(new AdditionExpression(left, right));
                    break;
                }
                case "-": {
                    DoubleExpression right = stack.pop();
                    DoubleExpression left = stack.pop();
                    stack.push(new SubtractionExpression(left, right));
                    break;
                }
                case "*": {
                    DoubleExpression right = stack.pop();
                    DoubleExpression left = stack.pop();
                    stack.push(new MultiplicationExpression(left, right));
                    break;
                }
                case "/": {
                    DoubleExpression right = stack.pop();
                    DoubleExpression left = stack.pop();
                    stack.push(new DivisionExpression(left, right));
                    break;
                }
                default: {
                    stack.push(new NumberExpression(Double.parseDouble(token)));
                }
            }
        }
        return stack.pop();
    }
}


public class InterpreterPatternExample {
    public static void main(String[] args) {
        // Infix expressions
        String infixExpression1 = "(2 + 3) * 5";
        String infixExpression2 = "((2 + 3) * 3) * 5";
        String infixExpression3 = "((2 + 3 * 3) * 5 - 1 / 5)";

        // Convert infix to postfix
        String postfixExpression1 = InfixToPostfixConverter.convert(infixExpression1);
        String postfixExpression2 = InfixToPostfixConverter.convert(infixExpression2);
        String postfixExpression3 = InfixToPostfixConverter.convert(infixExpression3);

        System.out.println("Postfix of expression 1: " + postfixExpression1);
        System.out.println("Postfix of expression 2: " + postfixExpression2);
        System.out.println("Postfix of expression 3: " + postfixExpression3);

        // Evaluate postfix expressions
        DoubleExpression parsedExpression1 = ExpressionParser.parse(postfixExpression1);
        System.out.println("Result of expression 1: " + parsedExpression1.interpret());

        DoubleExpression parsedExpression2 = ExpressionParser.parse(postfixExpression2);
        System.out.println("Result of expression 2: " + parsedExpression2.interpret());

        DoubleExpression parsedExpression3 = ExpressionParser.parse(postfixExpression3);
        System.out.println("Result of expression 3: " + parsedExpression3.interpret());
    }
}

//public class InterpreterPatternExample {
//    public static void main(String[] args) {
//        // Input expressions in postfix notation (Reverse Polish Notation)
//        String expression1 = "2 3 + 5 *"; // (2 + 3) * 5
//        String expression2 = "2 3 3 * + 5 *"; // (2 + 3 * 3) * 5
//        String expression3 = "2 3 3 * + 5 * 1 5 / -"; // (2 + 3 * 3) * 5 - 1 / 5
//
//        DoubleExpression parsedExpression1 = ExpressionParser.parse(expression1);
//        System.out.println("Result of expression 1: " + parsedExpression1.interpret());
//
//        DoubleExpression parsedExpression2 = ExpressionParser.parse(expression2);
//        System.out.println("Result of expression 2: " + parsedExpression2.interpret());
//
//        DoubleExpression parsedExpression3 = ExpressionParser.parse(expression3);
//        System.out.println("Result of expression 3: " + parsedExpression3.interpret());
//    }
//}
