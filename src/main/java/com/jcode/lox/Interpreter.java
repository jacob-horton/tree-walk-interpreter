package com.jcode.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.jcode.lox.Expr.Assign;
import com.jcode.lox.Expr.Binary;
import com.jcode.lox.Expr.Call;
import com.jcode.lox.Expr.Get;
import com.jcode.lox.Expr.Grouping;
import com.jcode.lox.Expr.Literal;
import com.jcode.lox.Expr.Logical;
import com.jcode.lox.Expr.Set;
import com.jcode.lox.Expr.Super;
import com.jcode.lox.Expr.Ternary;
import com.jcode.lox.Expr.This;
import com.jcode.lox.Expr.Unary;
import com.jcode.lox.Expr.Variable;
import com.jcode.lox.Stmt.Block;
import com.jcode.lox.Stmt.Break;
import com.jcode.lox.Stmt.Class;
import com.jcode.lox.Stmt.Continue;
import com.jcode.lox.Stmt.Expression;
import com.jcode.lox.Stmt.Function;
import com.jcode.lox.Stmt.If;
import com.jcode.lox.Stmt.Return;
import com.jcode.lox.Stmt.Var;
import com.jcode.lox.Stmt.While;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private Environment environment = globals;
	private Scanner scan;
	private final Map<Expr, Integer> locals = new HashMap<>();

	public Interpreter() {
		globals.define("clock", new LoxCallable() {
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> args) {
				return (double) System.currentTimeMillis() / 1000.0;
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});

		globals.define("str", new LoxCallable() {
			@Override
			public int arity() {
				return 1;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> args) {
				return stringify(args.get(0));
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});

		globals.define("number", new LoxCallable() {
			@Override
			public int arity() {
				return 1;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> args) {
				Object arg = args.get(0);
				if (!(arg instanceof String)) {
					throw new NativeFnError("Can only call 'number()' on strings.");
				}

				try {
					return Double.valueOf((String) arg);
				} catch (NumberFormatException e) {
					throw new NativeFnError("Invalid number input.");
				}
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});

		globals.define("print", new LoxCallable() {
			@Override
			public int arity() {
				return 1;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> args) {
				System.out.print(stringify(args.get(0)));
				return null;
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});

		globals.define("println", new LoxCallable() {
			@Override
			public int arity() {
				return 1;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> args) {
				System.out.println(stringify(args.get(0)));
				return null;
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});

		globals.define("input", new LoxCallable() {
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> args) {
				if (scan.hasNextLine()) {
					return scan.nextLine();
				}

				return null;
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});
	}

	public void interpret(List<Stmt> statements) {
		scan = new Scanner(System.in);

		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		} catch (NativeFnError error) {
			Lox.nativeFnError(error);
		}

		scan.close();
	}

	@Override
	public Object visitTernaryExpr(Ternary expr) {
		Object left = evaluate(expr.left);

		if (isTruthy(left)) {
			return evaluate(expr.middle);
		} else {
			return evaluate(expr.right);
		}
	}

	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case GREATER:
				if (left instanceof Double && right instanceof Double) {
					return (double) left > (double) right;
				}

				if (left instanceof String && right instanceof String) {
					return ((String) left).compareTo((String) right) > 0;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case GREATER_EQUAL:
				if (left instanceof Double && right instanceof Double) {
					return (double) left >= (double) right;
				}

				if (left instanceof String && right instanceof String) {
					return ((String) left).compareTo((String) right) >= 0;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case LESS:
				if (left instanceof Double && right instanceof Double) {
					return (double) left < (double) right;
				}

				if (left instanceof String && right instanceof String) {
					return ((String) left).compareTo((String) right) < 0;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case LESS_EQUAL:
				if (left instanceof Double && right instanceof Double) {
					return (double) left <= (double) right;
				}

				if (left instanceof String && right instanceof String) {
					return ((String) left).compareTo((String) right) <= 0;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case MINUS:
			case MINUS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left - (double) right;
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case PLUS:
			case PLUS_EQUAL:
				if (left instanceof Double && right instanceof Double) {
					return (double) left + (double) right;
				}

				if (left instanceof String && right instanceof String) {
					return (String) left + (String) right;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case SLASH:
			case SLASH_EQUAL:
				checkNumberOperands(expr.operator, left, right);

				if ((double) right == 0) {
					throw new RuntimeError(expr.operator, "Cannot divide by zero.");
				}
				return (double) left / (double) right;
			case STAR:
			case STAR_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left * (double) right;
			default:
				break;
		}

		// Unreachable
		return null;
	}

	@Override
	public Object visitGroupingExpr(Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitLogicalExpr(Logical expr) {
		Object left = evaluate(expr.left);
		if (expr.operator.type == TokenType.OR) {
			if (isTruthy(left))
				return left;
		} else {
			if (!isTruthy(left))
				return left;
		}

		return evaluate(expr.right);
	}

	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double) right;
			case BANG:
				return !isTruthy(right);
			default:
				break;
		}

		// Unreachable
		return null;
	}

	@Override
	public Object visitVariableExpr(Variable expr) {
		return lookUpVariable(expr.name, expr);
	}

	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else {
			return globals.get(name);
		}
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	private boolean isTruthy(Object object) {
		if (object == null)
			return false;
		if (object instanceof Boolean)
			return (boolean) object;
		return true;
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null)
			return false;

		return a.equals(b);
	}

	private String stringify(Object object) {
		if (object == null)
			return "nil";

		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}

			return text;
		}

		return object.toString();
	}

	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	public void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}

	public void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;

		try {
			this.environment = environment;

			for (Stmt statement : statements) {
				execute(statement);
			}
		} finally {
			this.environment = previous;
		}
	}

	@Override
	public Void visitBlockStmt(Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		evaluate(stmt.expression);
		return null;
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
	public Void visitVarStmt(Var stmt) {
		Object value = null;
		if (stmt.initialiser != null)
			value = evaluate(stmt.initialiser);

		environment.define(stmt.name.lexeme, value);
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		try {
			while (isTruthy(evaluate(stmt.condition))) {
				try {
					execute(stmt.body);
				} catch (com.jcode.lox.Continue error) {
				}
			}
		} catch (com.jcode.lox.Break error) {
		}

		return null;
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
	public Void visitBreakStmt(Break stmt) {
		throw new com.jcode.lox.Break();
	}

	@Override
	public Void visitContinueStmt(Continue stmt) {
		throw new com.jcode.lox.Continue();
	}

	@Override
	public Object visitCallExpr(Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> args = new ArrayList<>();
		for (Expr arg : expr.arguments) {
			args.add(evaluate(arg));
		}

		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call funcitons and classes.");
		}

		LoxCallable function = (LoxCallable) callee;
		if (args.size() != function.arity()) {
			throw new RuntimeError(expr.paren,
					"Expected " + function.arity() + " arguments, but got " + args.size() + ".");
		}

		return function.call(this, args);
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme, function);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		Object value = null;
		if (stmt.value != null)
			value = evaluate(stmt.value);

		throw new com.jcode.lox.Return(value);
	}

	@Override
	public Void visitClassStmt(Class stmt) {
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass)) {
				throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
			}
		}

		environment.define(stmt.name.lexeme, null);

		if (stmt.superclass != null) {
			environment = new Environment(environment);
			environment.define("super", superclass);
		}

		Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}

		LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass) superclass, methods);

		if (superclass != null) {
			environment = environment.enclosing;
		}

		environment.assign(stmt.name, klass);
		return null;
	}

	@Override
	public Object visitGetExpr(Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof LoxInstance) {
			return ((LoxInstance) object).get(expr.name);
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitSetExpr(Set expr) {
		Object object = evaluate(expr.object);

		if (!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}

		Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);

		return value;
	}

	@Override
	public Object visitThisExpr(This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Object visitSuperExpr(Super expr) {
		int distance = locals.get(expr);
		LoxClass superclass = (LoxClass) environment.getAt(distance, "super");
		LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
		LoxFunction method = superclass.findMethod(expr.method.lexeme);

		if (method == null) {
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
		}

		return method.bind(object);
	}
}
