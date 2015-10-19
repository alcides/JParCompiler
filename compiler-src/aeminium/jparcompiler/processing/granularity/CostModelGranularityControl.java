package aeminium.jparcompiler.processing.granularity;

import aeminium.jparcompiler.model.CostEstimation;
import aeminium.jparcompiler.processing.CostEstimatorProcessor;
import spoon.reflect.declaration.CtElement;

public class CostModelGranularityControl implements GranularityControl {
	
	public boolean shouldParallelize(CtElement element) {
		CostEstimation ce = CostEstimatorProcessor.database.get(element);
		if (ce == null) {
			CostEstimatorProcessor.visitor.scan(element);
			ce = CostEstimatorProcessor.visitor.get(element);
		}
		ce.apply(CostEstimatorProcessor.basicCosts);
		if (ce.isExpressionComplex) {
			return true;
		} else {
			long estimation = ce.expressionCost;
			long overhead = CostEstimatorProcessor.basicCosts.get("parallel");
			return estimation > overhead;
		}
	}
	
	public boolean hasGranularityControlExpression(CtElement e) {
		CostEstimation ce = CostEstimatorProcessor.database.get(e);
		return ce != null && ce.isExpressionComplex;
	}
	
}
