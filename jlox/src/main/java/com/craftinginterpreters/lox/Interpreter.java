package main.java.com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static main.java.com.craftinginterpreters.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        }
        catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // expressions
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.expression);
        return switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            case BANG -> !isTruthy(right);
            // unreachable
            default -> null;
        };
    }


    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // TODO: evalutes both even if we know the checking of operands would fail
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        if (expr.operator.type == COMMA) {
            return right;
        }

        if (expr.operator.type == PLUS) {
            if (left instanceof Double && right instanceof Double) {
                return (double)left + (double)right;
            }
            if (left instanceof String && right instanceof String) {
               return left + (String)right;
            }
            throw new RuntimeError(expr.operator, "Operands must both be numbers or strings");
        }

        checkNumberOperands(expr.operator, left, right);
        return switch (expr.operator.type) {
            case MINUS ->  (double)left - (double)right;
            case SLASH -> (double)left / (double)right;
            case STAR -> (double)left * (double)right;
            case GREATER -> (double)left > (double)right;
            case GREATER_EQUAL -> (double)left >= (double)right;
            case LESS -> (double)left < (double)right;
            case LESS_EQUAL -> (double)left <= (double)right;
            // never reached
            default -> null;
        };
    }


    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        Object condition = evaluate(expr.condition);
        return (isTruthy(condition)) ? evaluate(expr.then) : evaluate(expr.otherwise);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == OR) {
            if (isTruthy(left)) {
                return left;
            }
        }
        else {
            if (!isTruthy(left)) {
                return left;
            }
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitIdentifierExpr(Expr.Identifier expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }

        if (arguments.size() != function.arity()) {
            throw  new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size());
        }

        return function.call(this, arguments);
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

    // statements
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        Object condition = evaluate(stmt.condition);
        if (isTruthy(condition)) {
            execute(stmt.then);
        }
        else if (stmt.otherwise != null) {
            execute(stmt.otherwise);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexume, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexume, value);
        return null;
    }


    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        }
        finally {
            this.environment = previous;
        }
    }
}
