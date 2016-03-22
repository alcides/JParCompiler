package aeminium.jparcompiler.processing;

import aeminium.jparcompiler.model.CostEstimation;
import spoon.reflect.declaration.CtElement;

public class CostModelFixer extends CostModelVisitor {
	
	public CostModelFixer() {
		super();
	}
	
	public CostEstimation save(CtElement e, CostEstimation ce) {
		if (e != null) {
			if (e.getMetadata(CostEstimation.COST_MODEL_KEY) == null) {
				e.putMetadata(CostEstimation.COST_MODEL_KEY, ce);
			}
		}
		return ce;
	}
}
