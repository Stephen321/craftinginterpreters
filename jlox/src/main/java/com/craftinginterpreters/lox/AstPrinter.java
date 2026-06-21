package main.java.com.craftinginterpreters.lox;


class AstPrinter implements Expr.Visitor<String> {

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
       System.out.print(new AstPrinter().print(expression));
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal literal) {
        return literal.value.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping grouping) {
        return parenthesize("group", grouping.expression);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary unary) {
        return parenthesize(unary.operator.lexume, unary.expression);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary binary) {
        return parenthesize(binary.operator.lexume, binary.left, binary.right);
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
}
