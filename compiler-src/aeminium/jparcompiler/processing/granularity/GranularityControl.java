package aeminium.jparcompiler.processing.granularity;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.declaration.CtElement;

public interface GranularityControl {
	public boolean shouldParallelize(CtElement element);
	public boolean hasGranularityControlExpression(CtElement e);
	public CtExpression<?> getGranularityControlElement(CtElement e, CtExpression<?> context);
	public CtExpression<?> getGranularityControlUnits(CtFor element);
}
