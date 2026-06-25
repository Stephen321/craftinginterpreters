package main.java.com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/* TODO
- Once we have functions, we could simplify the language by tearing out the old print syntax and replacing it with a native function. But that would mean that examples early in the book wouldn’t run on the interpreter from later chapters and vice versa. So, for the book, I’ll leave it alone.
 */

/*
Grammar

// statements
program        → declaration* "EOF" ;
declaration    → varDecl | statement ;
statement      → exprStmt | printStmt | blockStmt | ifStmt | whileStmt | forStmt;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
blockStmt      → "{" declaration* "}" ;
ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
whileStmt      →  "while" "(" expression ")" statement ;
forStmt        →  "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression? ")" statement ;



// expressions
expression     → comma ;
comma          → assignment ( "," assignment )* ;
assignment     → IDENTIFIER "=" assignment | conditional ;
conditional    → logic_or ( "?" comma ":" conditional )? ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ("==" | "!=" comparison )* ;
comparison     → term ( (">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "+" | "-" ) factor )* ;
factor         → unary ( ( "*" | "/" ) unary )* ;
unary          → ( "-" | "!") unary | call ;
call           → primary "(" arguments? ")" ;
primary        → NUMBER | STRING | "true" | "false" | "nil" |
                 "(" expression ")" | IDENTIFIER
// error productions
                "," comma |
                ( "==", "!=" ) equality |
                ( ">" | ">=" | "<" | "<=" ) comparison |
                "+" term |
                ( "*" | "/" ) factor ;

arguments      → expression ("," expression )* ;

// NOTE: assignment in C++ is in the same group as the Tenary
 */

public class GenerateAst {
    static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generate_aset [output_directory]");
            System.exit(64);
        }
        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
                "Literal     : Object value",
                "Grouping    : Expr expression",
                "Unary       : Token operator, Expr expression",
                "Binary      : Expr left, Token operator, Expr right",
                "Conditional : Expr condition, Expr then, Expr otherwise",
                "Logical     : Expr left, Token operator, Expr right",
                "Identifier  : Token name",
                "Assign      : Token name, Expr value",
                "Call        : Expr callee, Token paren, List<Expr> arguments"
                ));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Var         : Token name, Expr initializer",
                "Expression  : Expr expression",
                "Print       : Expr expression",
                "Block       : List<Stmt> statements",
                "If          : Expr condition, Stmt then, Stmt otherwise",
                "While       : Expr condition, Stmt body"

        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types)  throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, Charset.defaultCharset());

        writer.println("package main.java.com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);
        writer.println();
        writer.println("\tabstract<R> R accept(Visitor<R> visitor);");
        writer.println();

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("\tinterface Visitor<R> {");

        for (String type : types) {
            String className = type.split(":")[0].trim();
            writer.println("\t\tR visit" +  className + baseName +
                    "(" + className + " " + baseName.toLowerCase() + ");");
        }

        writer.println("\t}");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldsStr) {
        writer.println("\tstatic class " + className + " extends " + baseName + "{");

        String[] fields = fieldsStr.split(",");
        for (String field : fields) {
            field = field.trim();
            writer.println("\t\tfinal " + field + ";");
        }
        writer.println();
        writer.println("\t\t" + className + "(" + fieldsStr + ") {");
        for (String field : fields) {
            field = field.trim();
            String name = field.split(" ")[1].trim();
            writer.println("\t\t\tthis." + name + " = " + name + ";");
        }
        writer.println("\t\t}");
        writer.println();

        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
        writer.println("\t\t}");

        writer.println("\t}");
        writer.println();
    }
}
