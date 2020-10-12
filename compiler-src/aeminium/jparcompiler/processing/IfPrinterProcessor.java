package aeminium.jparcompiler.processing;

import aeminium.jparcompiler.model.CostEstimation;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtIf;
import spoon.reflect.declaration.CtClass;

public class IfPrinterProcessor extends AbstractProcessor<CtIf>  {
	
	
	@Override
	public void process(CtIf iff) {
		FactoryReference.setFactory(iff.getFactory());
		if (iff.getCondition().toString().contains(".parallelize(")) {
			System.out.println("---------------------------------");
			System.out.println(iff.getParent(CtClass.class).getQualifiedName());
			System.out.println(iff.getCondition());
			if (iff.getThenStatement() != null) {
				System.out.println("Then cost:");
				System.out.println(((CostEstimation) iff.getThenStatement().getMetadata(CostEstimation.COST_MODEL_KEY)).getExpressionNode());
			}
			if (iff.getElseStatement() != null) {
				System.out.println("Else cost:");
				System.out.println(((CostEstimation) iff.getElseStatement().getMetadata(CostEstimation.COST_MODEL_KEY)).getExpressionNode());
			}
		}
	}

}
