package com.jcode.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private final Interpreter interpreter;
	private final Stack<HashMap<String, Boolean>> scopes = new Stack<>();

	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;
	private boolean inLoop = false;

	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	private enum ClassType {
		NONE, CLASS, SUBCLASS,
	}

	private enum FunctionType {
		NONE, FUNCTION, METHOD, INITIALISER,
	}

	@Override
	public Void visitBreakStmt(Break stmt) {
		if (!inLoop) {
			Lox.error(stmt.keyword, "Can't break outside of a loop.");
		}

		return null;
	}

	@Override
	public Void visitContinueStmt(Continue stmt) {
		if (!inLoop) {
			Lox.error(stmt.keyword, "Can't continue outside of a loop.");
		}

		return null;
	}

	@Override
	public Void visitBlockStmt(Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return from top-level code.");
		}

		if (stmt.value != null) {
			if (currentFunction == FunctionType.INITIALISER) {
				Lox.error(stmt.keyword, "Can't return a value from an initialiser");
			}
			resolve(stmt.value);
		}

		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null)
			resolve(stmt.elseBranch);
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		resolve(stmt.condition);

		inLoop = true;
		resolve(stmt.body);
		inLoop = false;

		return null;
	}

	@Override
	public Void visitVarStmt(Var stmt) {
		declare(stmt.name);
		if (stmt.initialiser != null) {
			resolve(stmt.initialiser);
		}

		define(stmt.name);
		return null;
	}

	@Override
	public Void visitTernaryExpr(Ternary expr) {
		resolve(expr.left);
		resolve(expr.middle);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitAssignExpr(Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		resolve(expr.callee);

		for (Expr arg : expr.arguments) {
			resolve(arg);
		}

		return null;
	}

	@Override
	public Void visitGroupingExpr(Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitVariableExpr(Variable expr) {
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Can't read local variable in its own initialiser.");
		}

		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Unary expr) {
		resolve(expr.right);
		return null;
	}

	public void resolve(List<Stmt> statements) {
		for (Stmt stmt : statements) {
			resolve(stmt);
		}
	}

	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}

	private void resolve(Expr expr) {
		expr.accept(this);
	}

	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}

	private void endScope() {
		scopes.pop();
	}

	private void declare(Token name) {
		if (scopes.isEmpty())
			return;

		Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexeme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}

		scope.put(name.lexeme, false);
	}

	private void define(Token name) {
		if (scopes.isEmpty())
			return;

		scopes.peek().put(name.lexeme, true);
	}

	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}

	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;

		beginScope();
		for (Token param : function.params) {
			declare(param);
			define(param);
		}

		resolve(function.body);
		endScope();

		currentFunction = enclosingFunction;
	}

	@Override
	public Void visitClassStmt(Class stmt) {
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;

		declare(stmt.name);
		define(stmt.name);
		if (stmt.superclass != null) {
			if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
				Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
			}

			currentClass = ClassType.SUBCLASS;
			resolve(stmt.superclass);

			beginScope();
			scopes.peek().put("super", true);
		}

		beginScope();
		scopes.peek().put("this", true);

		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALISER;
			}

			resolveFunction(method, declaration);
		}

		endScope();

		if (stmt.superclass != null)
			endScope();

		currentClass = enclosingClass;
		return null;
	}

	@Override
	public Void visitGetExpr(Get expr) {
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitSetExpr(Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitThisExpr(This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
			return null;
		}

		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitSuperExpr(Super expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'super' outside of a class");
		} else if (currentClass != ClassType.SUBCLASS) {
			Lox.error(expr.keyword, "Can't use 'super' in class with no superclass");
		}

		resolveLocal(expr, expr.keyword);
		return null;
	}
}
