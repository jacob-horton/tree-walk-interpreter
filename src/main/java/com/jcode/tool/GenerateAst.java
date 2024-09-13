package com.jcode.tool;

import java.util.List;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class GenerateAst {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}

		String outputDir = args[0];
		defineAst(outputDir, "Expr", Arrays.asList(
				"Ternary  : Expr left, Token op1, Expr middle, Token op2, Expr right",
				"Assign   : Token name, Expr value",
				"Binary   : Expr left, Token operator, Expr right",
				"Call     : Expr callee, Token paren, List<Expr> arguments",
				"Grouping : Expr expression",
				"Literal  : Object value",
				"Logical  : Expr left, Token operator, Expr right",
				"Variable : Token name",
				"Unary    : Token operator, Expr right"));

		defineAst(outputDir, "Stmt", Arrays.asList(
				"Print      : Expr expression",
				"Break      : ",
				"Continue   : ",
				"Block      : List<Stmt> statements",
				"Expression : Expr expression",
				"Function   : Token name, List<Token> params, List<Stmt> body",
				"Return     : Token keyword, Expr value",
				"If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
				"While      : Expr condition, Stmt body",
				"Var        : Token name, Expr initialiser"));
	}

	private static void defineAst(
			String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");

		writer.println("package com.jcode.lox;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();
		writer.println("abstract class " + baseName + " {");

		defineVisitor(writer, baseName, types);

		// The AST classes
		for (String type : types) {
			writer.println();
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}

		// The base accept() method
		writer.println();
		writer.println("\tabstract <R> R accept(Visitor<R> visitor);");

		writer.println("}");
		writer.close();
	}

	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("\tinterface Visitor<R> {");

		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("\t\tR visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
		}

		writer.println("\t}");
	}

	private static void defineType(
			PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println("\tstatic class " + className + " extends " + baseName + " {");

		// Constructor
		writer.println("\t\t" + className + "(" + fieldList + ") {");

		// Store parameters in fields
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			if (field.trim().length() == 0) {
				continue;
			}

			String name = field.split(" ")[1];
			writer.println("\t\t\tthis." + name + " = " + name + ";");
		}

		writer.println("\t\t}");

		// Visitor pattern
		writer.println();
		writer.println("\t\t@Override");
		writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
		writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
		writer.println("\t\t}");

		// Fields
		writer.println();
		for (String field : fields) {
			if (field.trim().length() == 0) {
				continue;
			}

			writer.println("\t\tfinal " + field + ";");
		}

		writer.println("\t}");
	}
}
