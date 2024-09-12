package com.jcode.lox;

import java.util.List;

import com.jcode.lox.Expr.Assign;
import com.jcode.lox.Expr.Binary;
import com.jcode.lox.Expr.Grouping;
import com.jcode.lox.Expr.Literal;
import com.jcode.lox.Expr.Ternary;
import com.jcode.lox.Expr.Unary;
import com.jcode.lox.Expr.Variable;
import com.jcode.lox.Stmt.Block;
import com.jcode.lox.Stmt.Expression;
import com.jcode.lox.Stmt.Print;
import com.jcode.lox.Stmt.Var;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	private Environment environment = new Environment();

	public void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
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
				checkNumberOperands(expr.operator, left, right);
				return (double) left - (double) right;
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL:
				return isEqual(left, right);
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

				if ((double) right == 0) {
					throw new RuntimeError(expr.operator, "Cannot divide by zero.");
				}
				return (double) left / (double) right;
			case STAR:
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
		return environment.get(expr.name);
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

	private void executeBlock(List<Stmt> statements, Environment environment) {
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
	public Void visitPrintStmt(Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
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
	public Object visitAssignExpr(Assign expr) {
		Object value = evaluate(expr.value);
		environment.assign(expr.name, value);
		return value;
	}
}
