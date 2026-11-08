package main.java.com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexume)) {
            return fields.get(name.lexume);
        }

        LoxFunction method = klass.findMethod(name.lexume);
        if (method != null) {
            return method;
        }

        throw new RuntimeError(name, "Undefined property'" + name.lexume + "'");
    }

    public void set(Token name, Object value) {
       fields.put(name.lexume, value);
    }
}
