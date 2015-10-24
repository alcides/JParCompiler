package aeminium.jparcompiler.processing.granularity;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.declaration.CtElement;

public class NoGranularityControl implements GranularityControl {

	@Override
	public boolean shouldParallelize(CtElement element) {
		return true;
	}
	
	public boolean hasGranularityControlExpression(CtElement e) {
		return false;
	}
	
	public CtExpression<?> getGranularityControlElement(CtElement e) {
		return e.getFactory().Code().createCodeSnippetExpression("false").compile();
	}
	
	@Override
	public CtExpression<?> getGranularityControlUnits(CtFor element) {
		return element.getFactory().Code().createLiteral(128);
	}

}
