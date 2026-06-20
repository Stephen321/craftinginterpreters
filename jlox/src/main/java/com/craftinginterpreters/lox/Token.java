package main.java.com.craftinginterpreters.lox;

class Token {
    final TokenType type;
    final String lexume;
    final Object literal;
    final int line;

    Token(TokenType type, String lexume, Object literal, int line) {
        this.type = type;
        this.lexume = lexume;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + " " + lexume + " " + literal;
    }
}
