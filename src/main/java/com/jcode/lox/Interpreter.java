package com.jcode.lox;

import com.jcode.lox.Expr.Binary;
import com.jcode.lox.Expr.Grouping;
import com.jcode.lox.Expr.Literal;
import com.jcode.lox.Expr.Ternary;
import com.jcode.lox.Expr.Unary;

class Interpreter implements Expr.Visitor<Object> {
	public void interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
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
}
