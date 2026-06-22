package main.java.com.craftinginterpreters.lox;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static main.java.com.craftinginterpreters.lox.TokenType.EOF;


class Lox {

    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: Lox [script]");
            System.exit(64);
        }
        else if (args.length == 1) {
            runFile(args[0]);
        }
        else {
            runPrompt();
        }
    }

    static void error(int line, String message){
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == EOF) {
            report(token.line, " at end", message);
        }
        else {
            report(token.line, " at '" + token.lexume + "'", message);
        }
    }

    static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;

    }

    static void runFile(String filePath) throws IOException {
        String source = Files.readString(Path.of(filePath));
        run(source);
    }

    static void runPrompt() throws IOException {
        InputStreamReader stream = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(stream);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        System.out.println("Tokens:");
        for (Token token : tokens) {
            System.out.print("{" + token + "} ");
        }
        System.out.println();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a sync error
        if (hadError) {
            return;
        }

        System.out.print("AST:");
        System.out.println(new AstPrinter().print(statements) + "\n");

        System.out.println("Run Result:");
        interpreter.interpret(statements);

    }

}

