package com.jcode.lox;

import com.jcode.lox.Expr.Binary;
import com.jcode.lox.Expr.Grouping;
import com.jcode.lox.Expr.Literal;
import com.jcode.lox.Expr.Ternary;
import com.jcode.lox.Expr.Unary;

public class AstPrinter implements Expr.Visitor<String> {
	String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesise(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesise("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null)
			return "nil";
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesise(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitTernaryExpr(Ternary expr) {
		return parenthesise(expr.op1.lexeme + expr.op2.lexeme, expr.left, expr.middle, expr.right);
	}

	private String parenthesise(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}

		builder.append(")");

		return builder.toString();
	}
}
