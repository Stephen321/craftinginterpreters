package main.java.com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
        // TODO: could implement syntax error if you try redine something already defined
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexume)) {
            values.put(name.lexume, value);
            return;
        }

        if (enclosing != null) {
            enclosing.get(name);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexume + "'");
    }

    Object get(Token name) {
        if (values.containsKey(name.lexume)) {
            return values.get(name.lexume);
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        // NOTE: we treat access of undefined as Runtime errors, not detected at compile time (could be
        // done during parsing though would need the environment outside of the Interpreter?
        throw new RuntimeError(name, "Undefined variable '" + name.lexume + "'");
    }

}
