package com.jcode.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
	private static class ParseError extends RuntimeException {
	}

	private final List<Token> tokens;
	private int current = 0;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	public List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while (!isAtEnd()) {
			statements.add(declaration());
		}

		return statements;
	}

	private Stmt declaration() {
		try {
			if (match(TokenType.CLASS))
				return classDeclaration();
			if (match(TokenType.FUN))
				return function("function");
			if (match(TokenType.VAR))
				return varDeclaration();

			return statement();
		} catch (ParseError error) {
			synchronise();
			return null;
		}
	}

	private Stmt classDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect class name.");

		Expr.Variable superclass = null;
		if (match(TokenType.LESS)) {
			consume(TokenType.IDENTIFIER, "Expect superclass name.");
			superclass = new Expr.Variable(previous());
		}

		consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

		List<Stmt.Function> methods = new ArrayList<>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			methods.add(function("method"));
		}

		consume(TokenType.RIGHT_BRACE, "Expect '}' after method definitions.");
		return new Stmt.Class(name, superclass, methods);
	}

	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

		Expr initialiser = null;
		if (match(TokenType.EQUAL)) {
			initialiser = expression();
		}

		consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initialiser);
	}

	private Stmt statement() {
		if (match(TokenType.IF))
			return ifStatement();
		if (match(TokenType.FOR))
			return forStatement();
		if (match(TokenType.WHILE))
			return whileStatement();
		if (match(TokenType.RETURN))
			return returnStatement();
		if (match(TokenType.BREAK))
			return breakStatement();
		if (match(TokenType.CONTINUE))
			return continueStatement();
		if (match(TokenType.LEFT_BRACE))
			return new Stmt.Block(block());

		return expressionStatement();
	}

	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(TokenType.SEMICOLON)) {
			value = expression();
		}

		consume(TokenType.SEMICOLON, "Expect ';' after return value");
		return new Stmt.Return(keyword, value);
	}

	private Stmt breakStatement() {
		Stmt stmt = new Stmt.Break(previous());
		consume(TokenType.SEMICOLON, "Expect ';' after break");
		return stmt;
	}

	private Stmt continueStatement() {
		Stmt stmt = new Stmt.Continue(previous());
		consume(TokenType.SEMICOLON, "Expect ';' after continue");
		return stmt;
	}

	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after while.");

		Stmt initialiser;
		if (match(TokenType.SEMICOLON)) {
			initialiser = null;
		} else if (match(TokenType.VAR)) {
			initialiser = varDeclaration();
		} else {
			initialiser = expressionStatement();
		}

		Expr condition = null;
		if (!check(TokenType.SEMICOLON)) {
			condition = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after loop condition");

		Expr increment = null;
		if (!check(TokenType.RIGHT_PAREN)) {
			increment = expression();
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' for clauses.");
		Stmt body = statement();

		if (increment != null) {
			body = new Stmt.Block(Arrays.asList(
					body,
					new Stmt.Expression(increment)));
		}

		if (condition == null)
			condition = new Expr.Literal(true);
		body = new Stmt.While(condition, body);

		if (initialiser != null) {
			body = new Stmt.Block(Arrays.asList(initialiser, body));
		}

		return body;
	}

	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after while.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.");
		Stmt body = statement();

		return new Stmt.While(condition, body);
	}

	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after if.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(TokenType.ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}

	private Stmt expressionStatement() {
		Expr expr = expression();

		consume(TokenType.SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expr);
	}

	private Stmt.Function function(String kind) {
		Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
		consume(TokenType.LEFT_PAREN, "Expect ')' after " + kind + " name.");
		List<Token> params = new ArrayList<>();
		if (!check(TokenType.RIGHT_PAREN)) {
			do {
				if (params.size() >= 255) {
					error(peek(), "Can't have more than 255 parameters.");
				}

				params.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
			} while (match(TokenType.COMMA));
		}

		consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters");
		consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, params, body);
	}

	private Expr expression() {
		return assignment();
	}

	private Expr assignment() {
		Expr expr = or();

		// Ternary
		if (match(TokenType.QUESTION)) {
			Token op1 = previous();
			Expr middle = expression();
			Token op2 = consume(TokenType.COLON, "Expect ':' after expression.");
			Expr right = expression();

			return new Expr.Ternary(expr, op1, middle, op2, right);
		}

		// Desugar assignment
		if (match(TokenType.EQUAL) ||
				match(TokenType.PLUS_EQUAL) ||
				match(TokenType.MINUS_EQUAL) ||
				match(TokenType.SLASH_EQUAL) ||
				match(TokenType.STAR_EQUAL)) {
			Token op = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				if (op.type != TokenType.EQUAL) {
					value = new Expr.Binary(expr, op, value);
				}

				return new Expr.Assign(name, value);
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr;
				return new Expr.Set(get.object, get.name, value);
			}

			error(op, "Invalid assignment target.");
			return expr;
		}

		// Normal assignment
		if (match(TokenType.EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);
			}

			error(equals, "Invalid assignment target.");
		}

		return expr;
	}

	private Expr or() {
		Expr expr = and();

		while (match(TokenType.OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private Expr and() {
		Expr expr = equality();

		while (match(TokenType.AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();

		while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr comparison() {
		Expr expr = term();

		while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();

		while (match(TokenType.MINUS, TokenType.PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr factor() {
		Expr expr = unary();

		while (match(TokenType.STAR, TokenType.SLASH)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr unary() {
		if (match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return call();
	}

	private Expr call() {
		Expr expr = primary();

		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(TokenType.DOT)) {
				Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
	}

	private Expr finishCall(Expr callee) {
		List<Expr> args = new ArrayList<>();
		if (!check(TokenType.RIGHT_PAREN)) {
			do {
				if (args.size() >= 255) {
					error(peek(), "Can't have more than 255 arguments.");
				}
				args.add(expression());
			} while (match(TokenType.COMMA));
		}

		Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
		return new Expr.Call(callee, paren, args);
	}

	private Expr primary() {
		if (match(TokenType.FALSE))
			return new Expr.Literal(false);
		if (match(TokenType.TRUE))
			return new Expr.Literal(true);
		if (match(TokenType.NIL))
			return new Expr.Literal(null);
		if (match(TokenType.THIS))
			return new Expr.This(previous());
		if (match(TokenType.SUPER)) {
			Token keyword = previous();
			consume(TokenType.DOT, "Expect '.' after 'super'.");
			Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
			return new Expr.Super(keyword, method);
		}

		if (match(TokenType.IDENTIFIER))
			return new Expr.Variable(previous());

		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal);
		}

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		throw error(peek(), "Expect expression.");
	}

	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();

		throw error(peek(), message);
	}

	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	private void synchronise() {
		advance();
		while (!isAtEnd()) {
			if (previous().type == TokenType.SEMICOLON)
				return;

			switch (peek().type) {
				case CLASS:
				case FOR:
				case FUN:
				case IF:
				case RETURN:
				case VAR:
				case WHILE:
					return;
				default:
					break;
			}

			advance();
		}
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}

		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd())
			return false;
		return peek().type == type;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token advance() {
		if (!isAtEnd())
			current++;
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}

	private Token previous() {
		return tokens.get(current - 1);
	}
}
