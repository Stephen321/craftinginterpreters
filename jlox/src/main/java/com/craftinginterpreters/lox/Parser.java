package main.java.com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static main.java.com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
//        catch (ParseError error) {
//            return null;
//        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDecl();
            }
            return statement();
        }
        catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(LEFT_BRACE)) {
            return blockStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(FOR)) {
            return forStatement();
        }
        return expressionStatement();
    }

    private Stmt varDecl() {
        Token name = consume(IDENTIFIER, "Expect variable name");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Missing ; at end of var statement");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        Expr expression = expression();
        consume(SEMICOLON, "Expected ';' after expression");
        return new Stmt.Expression(expression);
    }

    private Stmt printStatement() {
        Expr expression = expression();
        consume(SEMICOLON, "Expected ';' after value");
        return new Stmt.Print(expression);
    }

    private Stmt blockStatement() {
        return new Stmt.Block(block());
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition");
        Stmt then = statement();
        Stmt otherwise = null;
        if (match(ELSE)) {
           otherwise = statement();
        }
        return new Stmt.If(condition, then, otherwise);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'while' condition");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        }
        else if (match(VAR)) {
            initializer = varDecl();
        }
        else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after 'for' condition");


        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after 'for' clauses");

        Stmt body = statement();

        // desugar to while loop

        // add increment to end of body
        if (increment != null)  {
            body = new Stmt.Block(
                    Arrays.asList(
                        body,
                        new Stmt.Expression(increment)
                    )
            );
        }

        // default to true if none was provided
        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        // create while loop
        body = new Stmt.While(condition, body);

        // add initializer beforehand
        if (initializer != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            initializer,
                            body
                    )
            );
        }

        return body;
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        Expr expr = assignment();

        while (match(COMMA)) {
            Token operator = previous();
            Expr right = assignment();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr assignment() {
        Expr expr = conditional();

        if (match(EQUAL)) {
            Token equals = previous();

            if (expr instanceof Expr.Identifier identifier) {
                Token name = identifier.name;
                Expr value = assignment();
                return new Expr.Assign(name, value);
            }

            // NOTE: reports, doesn't throw as not in a panic state where we need to sync
            error(equals, "Invalid assignment target");
        }

        return expr;
    }

    private Expr conditional() {
        Expr expr = logic_or();
        if (match(QUESTION)) {
            Expr then = comma();
            consume(COLON, "Expect ':' after expression");
            Expr otherwise = conditional();
            return new Expr.Conditional(expr, then, otherwise);
        }
        return expr;
    }

    private Expr logic_or() {
        Expr expr = logic_and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = logic_and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr logic_and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(STAR, SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(MINUS, BANG)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        else {
            return call();
        }
    }

    private Expr call() {
        Expr expr = primary();

        // TODO: to be explained why while(True) instead of while(match(LEFT_PAREN))
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            }
            else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Identifier(previous());
        }

        // error productions/
        // TODO: not sure if the return null is correct
        if (match(COMMA)) {
            ParseError err = error(previous(), "Missing left operand");
            comma();
            throw err;
        }
        if (match(EQUAL_EQUAL, BANG_EQUAL)) {
            ParseError err = error(previous(), "Missing left operand");
            equality();
            throw err;
        }
        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            ParseError err = error(previous(), "Missing left operand");
            comparison();
            throw err;
        }
        if (match(PLUS)) {
            ParseError err = error(previous(), "Missing left operand");
            term();
            throw err;
        }
        if (match(STAR, SLASH)) {
            ParseError err = error(previous(), "Missing left operand");
            factor();
            throw err;
        }

        throw error(peek(), "Expect expression");
    }

    private Token consume(TokenType type, String message) throws ParseError {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        return tokens.get(current++);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

}
