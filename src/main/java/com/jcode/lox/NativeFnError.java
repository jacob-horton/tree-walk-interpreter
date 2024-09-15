package com.jcode.lox;

class NativeFnError extends RuntimeException {
	NativeFnError(String message) {
		super(message);
	}
}
