package com.jcode.lox;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitPrintStmt(Print stmt);
		R visitBlockStmt(Block stmt);
		R visitExpressionStmt(Expression stmt);
		R visitVarStmt(Var stmt);
	}

	static class Print extends Stmt {
		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

		final Expr expression;
	}

	static class Block extends Stmt {
		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}

		final List<Stmt> statements;
	}

	static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		final Expr expression;
	}

	static class Var extends Stmt {
		Var(Token name, Expr initialiser) {
			this.name = name;
			this.initialiser = initialiser;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

		final Token name;
		final Expr initialiser;
	}

	abstract <R> R accept(Visitor<R> visitor);
}
