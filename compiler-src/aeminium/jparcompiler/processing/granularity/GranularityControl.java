package aeminium.jparcompiler.processing.granularity;

import spoon.reflect.declaration.CtElement;

public interface GranularityControl {
	public boolean shouldParallelize(CtElement element);
	public boolean hasGranularityControlExpression(CtElement e);
	public String getGranularityControlString(CtElement e);
}
