package main.java.com.craftinginterpreters.lox;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


class Lox {

    private static boolean hadError = false;

    static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: Lox [script]");
        }
        else if (args.length == 1) {
            runFile(args[0]);
        }
        else {
            runPrompt();
        }
    }

    public static void error(int line, String message){
        report(line, "", message);
    }

    public static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    public static void runFile(String filePath) throws IOException {
        String source = Files.readString(Path.of(filePath));
        run(source);
    }

    public static void runPrompt() throws IOException {
        InputStreamReader stream = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(stream);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}

