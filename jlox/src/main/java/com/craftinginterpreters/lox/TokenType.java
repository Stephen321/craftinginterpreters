package main.java.com.craftinginterpreters.lox;

public enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, SEMICOLON, MINUS, PLUS, SLASH, STAR,
    QUESTION, COLON,

    // One or more character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // TODO: why is TRUE/FALSE not a literal?
    // Keywords
    IF, ELSE, AND, OR, FALSE, TRUE, NIL,
    FOR, WHILE,
    CLASS, SUPER, FUN, RETURN, THIS,
    VAR,
    PRINT,
    EOF
}
