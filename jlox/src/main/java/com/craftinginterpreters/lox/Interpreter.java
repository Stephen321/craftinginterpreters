package main.java.com.craftinginterpreters.lox;

import static main.java.com.craftinginterpreters.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object> {
    
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        }
        catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal literal) {
        return literal.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping grouping) {
        return evaluate(grouping.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary unary) {
        Object right = evaluate(unary.expression);
        return switch (unary.operator.type) {
            case MINUS -> {
                checkNumberOperand(unary.operator, right);
                yield -(double) right;
            }
            case BANG -> !isTruthy(right);
            // unreachable
            default -> null;
        };
    }


    @Override
    public Object visitBinaryExpr(Expr.Binary binary) {
        // TODO: evalutes both even if we know the checking of operands would fail
        Object left = evaluate(binary.left);
        Object right = evaluate(binary.right);

        if (binary.operator.type == COMMA) {
            return right;
        }

        if (binary.operator.type == PLUS) {
            if (left instanceof Double && right instanceof Double) {
                return (double)left + (double)right;
            }
            if (left instanceof String && right instanceof String) {
               return (String)left + (String)right;
            }
            throw new RuntimeError(binary.operator, "Operands must both be numbers or strings");
        }

        checkNumberOperands(binary.operator, left, right);
        return switch (binary.operator.type) {
            case MINUS ->  (double)left - (double)right;
            case SLASH -> (double)left / (double)right;
            case STAR -> (double)left * (double)right;
            // never reached
            default -> null;
        };
    }


    @Override
    public Object visitConditionalExpr(Expr.Conditional conditional) {
        Object condition = evaluate(conditional.condition);
        return (isTruthy(condition)) ? evaluate(conditional.then) : evaluate(conditional.otherwise);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void checkNumberOperand(Token token, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(token, "Operand must be a number");
    }

    private void checkNumberOperands(Token token, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(token, "Operands must be a numbers");
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean)object;
        }
        return true;
    }
    
    private String stringify(Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return value.toString();
    }
}
