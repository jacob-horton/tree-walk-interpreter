package com.jcode.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration;
	private final Environment closure;

	public LoxFunction(Stmt.Function declaration, Environment closure) {
		this.closure = closure;
		this.declaration = declaration;
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> args) {
		Environment env = new Environment(closure);
		for (int i = 0; i < declaration.params.size(); i++) {
			env.define(declaration.params.get(i).lexeme, args.get(i));
		}

		try {
			interpreter.executeBlock(declaration.body, env);
		} catch (Return returnValue) {
			return returnValue.value;
		}

		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
