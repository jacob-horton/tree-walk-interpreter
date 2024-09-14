package com.jcode.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration;
	private final Environment closure;

	private boolean isInitialiser;

	public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitialiser) {
		this.isInitialiser = isInitialiser;
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
			if (isInitialiser)
				return closure.getAt(0, "this");
			return returnValue.value;
		}

		if (isInitialiser)
			return closure.getAt(0, "this");
		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}

	LoxFunction bind(LoxInstance instance) {
		Environment env = new Environment(closure);
		env.define("this", instance);
		return new LoxFunction(declaration, env, isInitialiser);
	}
}
