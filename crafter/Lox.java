package crafter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  private static final Interpreter interpreter = new Interpreter();
  static boolean hadError = false;
  private static boolean hadRuntimeError;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (; ; ) {
      System.out.print("> ");
      String line = reader.readLine();
      // Ctrl + D to break;
      if (line == null) break;
      run(line);
      hadError = false;
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] btyes = Files.readAllBytes(Paths.get(path));
    run(new String(btyes, Charset.defaultCharset()));
    // Indicate an error in the exit code.
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    List<Stmt> statments = parser.parse();
    // Expr expression = parser.parse();

    if (hadError) return;
    // interpreter.interpret(expression);
    interpreter.interpret(statments);
    // System.out.println(new AstPrinter().print(expression));
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  public static void runtimeError(RuntimeError err) {
    System.err.println(err.getMessage() + "\n[Line " + err.token.line + "]");
    hadRuntimeError = true;
  }
}
