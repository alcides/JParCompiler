package aeminium.jparcompiler.processing.granularity;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

public class SimpleGranularityControl implements GranularityControl {

	@Override
	public boolean shouldParallelize(CtElement element) {
		/*
		if (element instanceof CtInvocation) {
			CtInvocation<?> el = (CtInvocation) element;
			if (el.getExecutable().getDeclaringType().getQualifiedName().startsWith("java.lang.Math")) return false;
		}
		*/
		return true;
	}

}
