package crafter;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
  private final Map<String, Object> fields = new HashMap<>();
  private LoxClass klass;

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  Object get(Token name) {
    if (fields.containsKey(name.lexeme)) return fields.get(name.lexeme);
    throw new RuntimeError(name, "Undefiend property '" + name.lexeme + "'.");
  }

  @Override
  public String toString() {
    return klass.name + "instance";
  }

  public void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }
}
