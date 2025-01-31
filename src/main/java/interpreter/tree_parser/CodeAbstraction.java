package interpreter.tree_parser;

import java.util.ArrayList;
import java.util.List;

public class CodeAbstraction {

    interface CodeElement {
    }

    static class CodeBlock implements CodeElement {
        public void add(Statement statement) {

        }
    }

    static abstract class Statement implements CodeElement {
        
    }

    static class RegularStatement extends Statement {
        private final List<Class<? extends CodeElement>> classes = List.of(
                FunctionCallStatement.class,
                VariableDeclaration.class,
                VariableIdentifier.class,
                AssigmentStatement.class,
                ExpressionStatement.class,
                FunctionCallStatement.class);

        private final List<CodeElement> elements = new ArrayList<>();

        public void add(CodeElement codeElement) {
            if (!classes.contains(codeElement.getClass())) throw new RuntimeException("Unknown class " + codeElement);
            elements.add(codeElement);
        }

        private void checkElements() {
            //1||(3||4)
            //functionCall || (op variableDeclaration + variableAssignment + expression || functionCall)
            if (elements.isEmpty()) throw new RuntimeException("Empty statement");
            Class<? extends CodeElement> class0 = elements.get(0).getClass();
            if (elements.size() == 1 && class0 == FunctionCallStatement.class) return;
            if (elements.size() == 3) {
                //identifier, assignment, expression || functionCall
                //declaration, assignment, expression || functionCall

                boolean isValid =
                        (elements.get(0).getClass() == VariableIdentifier.class
                                || elements.get(0).getClass() == VariableDeclaration.class)
                                && elements.get(1).getClass() == AssigmentStatement.class
                                && (elements.get(2).getClass() == ExpressionStatement.class
                                || elements.get(2).getClass() == FunctionCallStatement.class);

                if (isValid) return;

                throw new RuntimeException("Unsupported statement, supported:\n" +
                        "(identifier || declaration) && assignment && (expression || functionCall)");

            }
            throw new RuntimeException("Unsupported statement, supported:\n" +
                    "(identifier || declaration) && assignment && (expression || functionCall)");
        }

    }

    static class IfStatement extends Statement {
    }

    static class WhileStatement extends Statement {
    }

    static class ForStatement extends Statement {
    }

    static class DoStatement extends Statement {
    }

    static class ReturnStatement extends Statement {
    }

    static class BreakStatement extends Statement {
    }

    static class ContinueStatement extends Statement {
    }

    static class SwitchStatement extends Statement {
    }

    static class AssigmentStatement implements CodeElement {
    }

    static class ExpressionStatement implements CodeElement {
    }

    static class FunctionStatement extends Statement {
    }

    static class FunctionCallStatement extends Statement {
    }

    static class VariableDeclaration implements CodeElement {
    }

    static class VariableIdentifier implements CodeElement {
    }

    public static void main(String[] args) {
        CodeBlock codeBlock = new CodeBlock();

        //a=1;
        //a=1+1;
        //int a=1+1;
        //int a=aa();
        //int a=aa(b);
        //int a=aa(1)+1;

        //functionCall || (op variableDeclaration + variableAssignment + expression || functionCall)


        RegularStatement statement1 = new RegularStatement();
        statement1.add(new FunctionCallStatement());

        RegularStatement statement2 = new RegularStatement();
        statement2.add(new VariableDeclaration());
        statement2.add(new AssigmentStatement());
        statement2.add(new FunctionCallStatement());

        RegularStatement statement3 = new RegularStatement();
        statement3.add(new VariableDeclaration());
        statement3.add(new AssigmentStatement());
        statement3.add(new ExpressionStatement());

        RegularStatement statement4 = new RegularStatement();
        statement4.add(new VariableIdentifier());
        statement4.add(new AssigmentStatement());
        statement4.add(new ExpressionStatement());

        statement1.checkElements();
        statement2.checkElements();
        statement3.checkElements();
        statement4.checkElements();

        codeBlock.add(statement1);
        codeBlock.add(statement2);
        codeBlock.add(statement3);
    }
}
