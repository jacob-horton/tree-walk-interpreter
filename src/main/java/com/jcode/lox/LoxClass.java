package com.jcode.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
	public String name;
	final LoxClass superclass;
	private final Map<String, LoxFunction> methods;

	public LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
		this.name = name;
		this.superclass = superclass;
		this.methods = methods;
	}

	@Override
	public int arity() {
		LoxFunction initialiser = findMethod("init");
		if (initialiser == null)
			return 0;

		return initialiser.arity();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> args) {
		LoxInstance instance = new LoxInstance(this);
		LoxFunction initialiser = findMethod("init");
		if (initialiser != null) {
			initialiser.bind(instance).call(interpreter, args);
		}

		return instance;
	}

	@Override
	public String toString() {
		return name;
	}

	public LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}

		if (superclass != null) {
			return superclass.findMethod(name);
		}

		return null;
	}
}
