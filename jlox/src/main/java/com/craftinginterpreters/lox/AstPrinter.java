package main.java.com.craftinginterpreters.lox;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AstPrinter implements Expr.Visitor<String>,
                            Stmt.Visitor<String> {

    static void main() {
       Expr expression = new Expr.Binary(
               new Expr.Unary(
                       new Token(TokenType.MINUS, "-", null, 1),
                       new Expr.Literal(123)
               ),
               new Token(TokenType.STAR, "*", null, 1),
               new Expr.Grouping(
                       new Expr.Literal(45.67)
               )
       );
       Stmt stmt = new Stmt.Print(expression);
       System.out.print(new AstPrinter().print(stmt));
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    String print(Stmt stmt) {
        return print(Arrays.asList(stmt));
    }

    String print(List<Stmt> statements) {
        StringBuilder builder = new StringBuilder();
        for (Stmt stmt : statements) {
            builder.append("\n").append(stmt.accept(this));
        }
        return builder.toString();
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexume, expr.expression);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexume, expr.left, expr.right);
    }

    @Override
    public String visitConditionalExpr(Expr.Conditional conditional) {
        return parenthesize("?", conditional.condition, conditional.then, conditional.otherwise);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexume, expr.left, expr.right);
    }

    @Override
    public String visitIdentifierExpr(Expr.Identifier expr) {
        return "<" + expr.name.lexume + ">";
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("= " + expr.name.lexume, expr.value);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        List<Expr> exprs = new ArrayList<>(expr.arguments);
        exprs.addFirst(expr.callee);
        return parenthesize("= call", expr.arguments.toArray(new Expr[0]));
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    // statements
    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return bracketize("var " + stmt.name.lexume + " = ", stmt.initializer);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return bracketize("expression ", stmt.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return bracketize("print ", stmt.expression);
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("<{ block");
        for (Stmt statement : stmt.statements) {
            builder.append("\n" + statement.accept(this));
        }
        builder.append("\n end block}>");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        String res = "<if (" + parenthesize("cond", stmt.condition) + ") "+
                "<then " +  stmt.then.accept(this) + " ;>";
        if (stmt.otherwise != null) {

            res += "<else " +  stmt.otherwise.accept(this) + " ;>";
        }
        return res;
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "<while (" + parenthesize("cond", stmt.condition) + ")\n" +
                stmt.body.accept(this);
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder("<fun " + stmt.name.lexume + " (");
        for (Token param : stmt.params) {
            builder.append(param.lexume + ", ");
        }
        builder.append(") {");
        builder.append(new Stmt.Block(stmt.body).accept(this));
        builder.append(">");
        return builder.toString();
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return parenthesize("return ", stmt.value);
    }

    private String bracketize(String name, Expr expr) {
        if (expr == null) {
            return "<null>;";
        }
        Object value = expr.accept(this);
        if (value != null && expr instanceof Expr.Literal && ((Expr.Literal)expr).value instanceof String) {
            value = "\"" + value + "\"";
        }
        return "<" + name + value + " ;>";
    }
}
