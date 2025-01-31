package interpreter.example;

import java.util.ArrayList;
import java.util.List;

// Define the node of the parse tree
class ParseTreeNode {
    String type; // Type of the node (e.g., "MethodDeclaration", "VariableDeclaration")
    String value; // Value of the node (e.g., method name, variable name)
    List<ParseTreeNode> children; // Child nodes

    public ParseTreeNode(String type, String value) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(ParseTreeNode child) {
        children.add(child);
    }

    // Utility function to print the tree
    public void printTree(String prefix) {
        System.out.println(prefix + type + ": " + value);
        for (ParseTreeNode child : children) {
            child.printTree(prefix + "    ");
        }
    }
}

// Main class
public class CustomParseTree {

    public static void main(String[] args) {
        // Example code to parse
//        String code = """
//                public static void main(String[] args) {
//                    int[] arr = {12, 35, 1, 10, 34, 1};
//                    print2largest(arr);
//                }
//                """;

        // Assuming tokenizer already implemented, parse the code into a parse tree
        ParseTreeNode root = parseCode("code");

        // Print the parse tree
        root.printTree("");
    }

    public static ParseTreeNode parseCode(String code) {
        // Root node for the parse tree
        ParseTreeNode root = new ParseTreeNode("ClassDeclaration", "CustomParseTree");

        // Add the main method declaration
        ParseTreeNode mainMethod = new ParseTreeNode("MethodDeclaration", "main");
        root.addChild(mainMethod);

        // Add method modifiers
        mainMethod.addChild(new ParseTreeNode("Modifier", "public"));
        mainMethod.addChild(new ParseTreeNode("Modifier", "static"));

        // Add method parameters
        ParseTreeNode parameters = new ParseTreeNode("Parameters", "String[] args");
        mainMethod.addChild(parameters);

        // Add method body
        ParseTreeNode methodBody = new ParseTreeNode("Block", "{...}");
        mainMethod.addChild(methodBody);

        // Add variable declaration inside method body
        ParseTreeNode varDeclaration = new ParseTreeNode("VariableDeclaration", "int[] arr");
        varDeclaration.addChild(new ParseTreeNode("ArrayInitializer", "{12, 35, 1, 10, 34, 1}"));
        methodBody.addChild(varDeclaration);

        // Add method call inside method body
        ParseTreeNode methodCall = new ParseTreeNode("MethodCall", "print2largest");
        methodCall.addChild(new ParseTreeNode("Arguments", "arr"));
        methodBody.addChild(methodCall);

        return root;
    }
}
