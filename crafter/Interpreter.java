package crafter;

import crafter.Expr.Assign;
import crafter.Expr.Binary;
import crafter.Expr.Call;
import crafter.Expr.Get;
import crafter.Expr.Grouping;
import crafter.Expr.Logical;
import crafter.Expr.Set;
import crafter.Expr.Unary;
import crafter.Expr.Variable;
import crafter.Stmt.Block;
import crafter.Stmt.Class;
import crafter.Stmt.Expression;
import crafter.Stmt.Function;
import crafter.Stmt.If;
import crafter.Stmt.Print;
import crafter.Stmt.Var;
import crafter.Stmt.While;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  private final Map<Expr, Integer> locals = new HashMap<>();
  final Environment globals = new Environment();
  private Environment environment = globals;

  Interpreter() {
    globals.define(
        "clock",
        new LoxCallable() {
          @Override
          public int arity() {
            return 0;
          }

          @Override
          public Object call(Interpreter interpreter, List<Object> arguments) {
            return (double) System.currentTimeMillis() / 1000.0;
          }

          @Override
          public String toString() {
            return "<native fn>";
          }
        });
  }

  void interpret(List<Stmt> statments) {
    try {
      for (Stmt statment : statments) {
        execute(statment);
      }
    } catch (RuntimeError err) {
      Lox.runtimeError(err);
    }
  }

  private String stringify(Object object) {
    if (object == null) return "nil";
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return object.toString();
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }
        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL:
        return isEqual(left, right);
    }
    return null;
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null) return false;
    return left.equals(right);
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
    }

    return null;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  private Object evaluate(Expr expression) {
    return expression.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    return lookupVariable(expr.name, expr);
  }

  private Object lookupVariable(Token name, Variable expr) {
    Integer distance = locals.get(expr);

    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
    return value;
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statments, new Environment(environment));
    return null;
  }

  public void executeBlock(List<Stmt> statments, Environment environment) {
    Environment prev = this.environment;
    try {
      this.environment = environment;
      for (Stmt statment : statments) {
        execute(statment);
      }
    } finally {
      this.environment = prev;
    }
  }

  @Override
  public Void visitIfStmt(If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitLogicalExpr(Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(expr.right);
  }

  @Override
  public Void visitWhileStmt(While stmt) {
    while (isTruthy(evaluate(stmt.condition))) execute(stmt.body);
    return null;
  }

  @Override
  public Object visitCallExpr(Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable))
      throw new RuntimeError(expr.paren, "can only call functions and classes");

    LoxCallable function = (LoxCallable) callee;

    if (arguments.size() != function.arity())
      throw new RuntimeError(
          expr.paren,
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");

    return function.call(this, arguments);
  }

  @Override
  public Void visitFunctionStmt(Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitClassStmt(Class stmt) {
    environment.define(stmt.name.lexeme, null);
    LoxClass klass = new LoxClass(stmt.name.lexeme);
    environment.assign(stmt.name, klass);

    return null;
  }

  @Override
  public Object visitGetExpr(Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) return ((LoxInstance) object).get(expr.name);

    throw new RuntimeError(expr.name, "only instances have properties.");
  }

  @Override
  public Object visitSetExpr(Set expr) {
    Object object = evaluate(expr.object);
    if (!(object instanceof LoxInstance))
      throw new RuntimeError(expr.name, "only instances have fields");

    Object value = evaluate(expr.value);
    ((LoxInstance) object).set(expr.name, value);

    return value;
  }
}
