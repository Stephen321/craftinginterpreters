package main.java.com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static main.java.com.craftinginterpreters.lox.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 0;


    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("if",     IF);
        keywords.put("else",   ELSE);
        keywords.put("and",    AND);
        keywords.put("or",     OR);
        keywords.put("false",  FALSE);
        keywords.put("true",   TRUE);
        keywords.put("nil",    NIL);
        keywords.put("for",    FOR);
        keywords.put("while",  WHILE);
        keywords.put("class",  CLASS);
        keywords.put("super",  SUPER);
        keywords.put("fun",    FUN);
        keywords.put("return", RETURN);
        keywords.put("this",   THIS);
        keywords.put("var",    VAR);
        keywords.put("print",  PRINT);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        tokens.clear();

        while (!isAtEnd()) {
            // set start to beginning of next lexume
            start = current;
            scanToken();
        }

        addToken(EOF, "", null);

        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case ';': addToken(SEMICOLON); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                    // don't consume \n as need it in next loop to increment line
                }
                else {
                    addToken(SLASH);
                }
                break;
            case '\n':
                line++;
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                }
                else if (isAlpha(c)) {
                    identifier();
                }
                else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }


    private void string() {
       while (peek() != '"' && !isAtEnd()) {
           if (peek() == '\n') {
               line++;
           }
          advance();
       }
       if (isAtEnd()){
           Lox.error(line, "Unterminated string");
           return;
       }

       // the closing "
       advance();

       // without surrounding "
       String literal = source.substring(start + 1, current - 1);
       addToken(STRING, literal);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peek(1))) {
            // consume the .
            advance();
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String lexume = getLexume();
        TokenType tokenType = keywords.get(lexume);
        if (tokenType == null) {
            tokenType = IDENTIFIER;
        }
        addToken(tokenType, lexume);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return  c >= 'a' && c <= 'z' ||
                c >= 'A' && c <= 'Z' ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (peek() != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int distance) {
        if (distance < 0) {
            distance = 0;
        }
        int target = current + distance;
        if (target >= source.length()) {
            return '\0';
        }
        return source.charAt(target);
    }

    private String getLexume() {
       return source.substring(start, current);
    }

    private void addToken(TokenType tokenType) {
        addToken(tokenType, null);
    }

    private void addToken(TokenType tokenType, Object literal) {
        String lexume = getLexume();
        addToken(tokenType, lexume, literal);
    }

    private void addToken(TokenType tokenType, String lexume) {
        addToken(tokenType, lexume, null);
    }

    private void addToken(TokenType tokenType, String lexume, Object literal) {

        Token token = new Token(tokenType, lexume, literal, line);
        tokens.add(token);
    }

    String getSource() {
        return source;
    }

    boolean isAtEnd() {
        return current >= source.length();
    }


}
