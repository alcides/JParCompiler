package aeminium.jparcompiler.processing.granularity;

import spoon.reflect.declaration.CtElement;

public interface GranularityControl {
	public boolean shouldParallelize(CtElement element);
}