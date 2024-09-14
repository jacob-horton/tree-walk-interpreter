package com.jcode.lox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Environment {
	public final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();
	private final Set<String> unassigned = new HashSet<>();

	public Environment() {
		enclosing = null;
	}

	public Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	Object get(Token name) {
		if (unassigned.contains(name.lexeme)) {
			throw new RuntimeError(name, "Unassigned variable '" + name.lexeme + "'.");
		}

		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}

		if (enclosing != null)
			return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	void define(String name, Object value) {
		values.put(name, value);

		if (value == null)
			unassigned.add(name);
	}

	Object assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			unassigned.remove(name.lexeme);
			return values.put(name.lexeme, value);
		}

		if (enclosing != null)
			return enclosing.assign(name, value);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}

	Object assignAt(int distance, Token name, Object value) {
		return ancestor(distance).values.put(name.lexeme, value);
	}

	Environment ancestor(int distance) {
		Environment environment = this;
		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}

		return environment;
	}
}
