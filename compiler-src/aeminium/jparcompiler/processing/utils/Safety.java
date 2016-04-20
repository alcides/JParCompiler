package aeminium.jparcompiler.processing.utils;

import java.lang.annotation.Annotation;

import aeminium.jparcompiler.processing.SeqMethodProcessor;
import aeminium.runtime.futures.codegen.Sequential;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

public class Safety {
	public static boolean isSafe(CtElement el) {
		if (el == null) return false;
		CtMethod<?> m;
		if (el instanceof CtMethod) {
			m = (CtMethod<?>) el;
		} else {
			try {
			m = el.getParent(CtMethod.class);
			} catch (Exception e) {
				//System.out.println(el.getParent().getParent().getParent().getParent());
				throw e;
			}
		}
		if (m == null) return true;
		
		for (CtAnnotation<? extends Annotation> an : m.getAnnotations()) {
			if (an.getType().getQualifiedName().equals(Sequential.class.getCanonicalName())) {
				return true;
			}
		}
		CtClass<?> cl = el.getParent(CtClass.class);
		
		for (CtAnnotation<? extends Annotation> an : cl.getAnnotations()) {
			if (an.getType().getQualifiedName().equals(Sequential.class.getCanonicalName())) {
				return true;
			}
		}
		
		if (el instanceof CtConstructorCall) {
			CtConstructorCall<?> e = (CtConstructorCall<?>) el;
			if (e.getType().getQualifiedName().startsWith("aeminium.runtime.futures")) return true;
		}
		
		return m.getSimpleName().startsWith(SeqMethodProcessor.SEQ_PREFIX);
	}
}
