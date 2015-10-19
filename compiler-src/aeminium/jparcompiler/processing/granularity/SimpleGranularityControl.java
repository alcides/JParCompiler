package aeminium.jparcompiler.processing.granularity;

import spoon.reflect.declaration.CtElement;

public class SimpleGranularityControl implements GranularityControl {

	@Override
	public boolean shouldParallelize(CtElement element) {
		return true;
	}
	
	public boolean hasGranularityControlExpression(CtElement e) {
		return false;
	}

}
