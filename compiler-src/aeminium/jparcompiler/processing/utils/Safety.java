package aeminium.jparcompiler.processing.utils;

import java.lang.annotation.Annotation;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import aeminium.jparcompiler.processing.SeqMethodProcessor;
import aeminium.runtime.futures.codegen.Sequential;

public class Safety {
	public static boolean isSafe(CtElement el) {
		if (el == null) return false;
		CtMethod<?> m;
		if (el instanceof CtMethod) {
			m = (CtMethod<?>) el;
		} else {
			m = el.getParent(CtMethod.class);
		}
		if (m == null) return true;
		
		for (CtAnnotation<? extends Annotation> an : m.getAnnotations()) {
			if (an.getSignature().equals("@" + Sequential.class.getCanonicalName() )) {
				return true;
			}
		}
		CtClass<?> cl = el.getParent(CtClass.class);
		
		for (CtAnnotation<? extends Annotation> an : cl.getAnnotations()) {
			if (an.getSignature().equals("@" + Sequential.class.getCanonicalName() )) {
				return true;
			}
		}
		
		return m.getSimpleName().startsWith(SeqMethodProcessor.SEQ_PREFIX);
	}
}
