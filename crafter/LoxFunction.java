package crafter;

import java.util.List;

public class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer;

  LoxFunction(Stmt.Function declaration, Environment closure, Boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.declaration = declaration;
    this.closure = closure;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment env = new Environment(closure);
    env.define("this", instance);
    return new LoxFunction(declaration, env, isInitializer);
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

  @Override
  public int arity() {
    return this.declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(this.closure);

    for (int i = 0; i < declaration.params.size(); i++)
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      if (isInitializer) return closure.getAt(0, "this");
      return returnValue.value;
    }
    if (isInitializer) return closure.getAt(0, "this");
    return null;
  }
}
