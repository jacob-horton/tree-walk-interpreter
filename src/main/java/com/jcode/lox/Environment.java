package com.jcode.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
	public final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	public Environment() {
		enclosing = null;
	}

	public Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}

		if (enclosing != null)
			return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	void define(String name, Object value) {
		values.put(name, value);
	}

	Object assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			return values.put(name.lexeme, value);
		}

		if (enclosing != null)
			return enclosing.assign(name, value);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
}
