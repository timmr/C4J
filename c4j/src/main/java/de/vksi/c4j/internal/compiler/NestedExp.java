package de.vksi.c4j.internal.compiler;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.expr.Expr;
import de.vksi.c4j.internal.compiler.StandaloneExp.CodeStandaloneExp;

public abstract class NestedExp extends Exp {
	public static final NestedExp THIS = new CodeNestedExp("this");
	public static final NestedExp NULL = new CodeNestedExp("null");
	public static final NestedExp RETURN_VALUE = new CodeNestedExp("$_");
	public static final NestedExp EXCEPTION_VALUE = new CodeNestedExp("$e");
	public static final NestedExp CALLING_OBJECT = new CodeNestedExp("$0");
	public static final NestedExp PROCEED = new CodeNestedExp("$proceed($$)");
	public static final NestedExp ALL_ARGS = new CodeNestedExp("$$");

	public static NestedExp arg(int num) {
		return new CodeNestedExp("$" + num);
	}

	public static NestedExp field(String name) {
		return new CodeNestedExp(name);
	}

	public static NestedExp field(CtField field) {
		return new CodeNestedExp(field.getName());
	}

	public static NestedExp var(String name) {
		return new CodeNestedExp(name);
	}

	public static List<NestedExp> getArgsList(CtBehavior behavior, int startIndex) throws NotFoundException {
		List<NestedExp> args = new ArrayList<NestedExp>();
		for (int i = 0; i < behavior.getParameterTypes().length; i++) {
			args.add(NestedExp.arg(i + startIndex));
		}
		return args;
	}

	/**
	 * Not supported by Javassist yet.
	 */
	public static NestedExp field(String name, CtClass parentClass) {
		return new CodeNestedExp(parentClass.getName().replace('$', '.') + ".this." + name);
	}

	public static NestedExp method(String name, NestedExp... params) {
		CodeNestedExp exp = new CodeNestedExp(name + getCodeForParams(params));
		return exp;
	}

	public StandaloneExp toStandalone() {
		return CodeStandaloneExp.fromNested(getCode());
	}

	protected static String getCodeForParams(NestedExp... params) {
		return "(" + getCodeForValues(params) + ")";
	}

	protected static String getCodeForValues(NestedExp... values) {
		boolean firstValue = true;
		StringBuilder valueCode = new StringBuilder();
		for (Exp value : values) {
			if (!firstValue) {
				valueCode.append(", ");
			}
			firstValue = false;
			valueCode.append(value.getCode());
		}
		return valueCode.toString();
	}

	public NestedExp appendCall(String method, NestedExp... params) {
		return new CodeNestedExp(getCode() + "." + method + getCodeForParams(params));
	}

	public NestedExp appendCall(String method, List<NestedExp> callParams) {
		return appendCall(method, callParams.toArray(new NestedExp[0]));
	}

	public NestedExp appendAccess(String field) {
		return new CodeNestedExp(getCode() + "." + field);
	}

	@Override
	public void insertAfter(CtBehavior behavior) throws CannotCompileException {
		toStandalone().insertAfter(behavior);
	}

	@Override
	public void insertBefore(CtBehavior behavior) throws CannotCompileException {
		toStandalone().insertBefore(behavior);
	}

	@Override
	public void insertCatch(CtClass exceptionType, CtBehavior behavior) throws CannotCompileException {
		toStandalone().insertCatch(exceptionType, behavior);
	}

	@Override
	public void insertFinally(CtBehavior behavior) throws CannotCompileException {
		toStandalone().insertFinally(behavior);
	}

	@Override
	public void replace(Expr expression) throws CannotCompileException {
		toStandalone().replace(expression);
	}

	protected static class CodeNestedExp extends NestedExp {
		private String code;

		public CodeNestedExp(String code) {
			this.code = code;
		}

		@Override
		protected String getCode() {
			return code;
		}
	}

}
