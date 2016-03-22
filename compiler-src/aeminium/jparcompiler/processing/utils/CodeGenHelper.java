package aeminium.jparcompiler.processing.utils;

import spoon.reflect.code.CtFieldRead;
import spoon.reflect.factory.Factory;

public class CodeGenHelper {
	
	Factory factory;
	public CodeGenHelper(Factory f) {
		this.factory = f;
	}
	
	@SuppressWarnings("rawtypes")
	public CtFieldRead generateHints(int code) {
		String tag = "LARGE";
		if (code == 0) tag = "SMALL";
		CtFieldRead hint = (CtFieldRead) factory.Code().createCodeSnippetExpression("aeminium.runtime.Hints." + tag).compile();
		return hint;
	}
}
