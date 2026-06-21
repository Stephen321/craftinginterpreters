package main.java.com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/*
Grammar

expression     → equality
equality       → comparison (("== | "!= comparison )*
comparison     → term ((">" | ">=" | "<" | "<=") term )*
term           → factor (("+" | "-") factor )*
factor         → unary (("*" | "/") unary )*
unary          → ("-" | "!") unary | primary
primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
 */

public class GenerateAst {
    static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generate_aset [output_directory]");
            System.exit(64);
        }
        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
                "Literal    : Object value",
                "Grouping   : Expr expression",
                "Unary      : Token operator, Expr expression",
                "Binary     : Expr left, Token operator, Expr right"
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
                    "(" + className + " " + className.toLowerCase() + ");");
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
