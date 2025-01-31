package interpreter.tree_parser;

public enum NodeType {
    CodeBlock,
    Statement,
    RegularStatement,

    WhileStatement,
    ForStatement,
    DoStatement,
    ReturnStatement,
    BreakStatement,
    ContinueStatement,
    SwitchStatement,
    AssigmentStatement,
    ArithmeticExpressionStatement,
    FunctionDeclarationStatement,
    FunctionCallStatement,
    VariableDeclaration,
    Class,
    Modifier,
    Parameters,
    Root,
    ArrayDeclaration,
    Case,
    Void,
    ClassField,
    Package,
    Import,
    TryCatch,
    Catch,
    Finally,
    Try,
    IfElseStatement,
    If,
    ElseIf,
    Else,
    Comment, Annotation, SystemFunction, DecomposedStatements, BooleanExpressionStatement, UNKNOWN, GOTO, PUSH_CODE_BLOCK, POP_CODE_BLOCK, VariableIdentifier
}


