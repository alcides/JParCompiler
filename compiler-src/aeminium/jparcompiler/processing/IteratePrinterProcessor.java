package aeminium.jparcompiler.processing;
import aeminium.jparcompiler.model.CostEstimation;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

public class IteratePrinterProcessor<T> extends AbstractProcessor<CtMethod<T>>  {
	
	@Override
	public void process(CtMethod<T> m) {
		FactoryReference.setFactory(m.getFactory());
		
		if (m.getSimpleName().equals("iterate")) {
			System.out.println(m.getParent(CtClass.class).getQualifiedName());
			System.out.println(((CostEstimation)m.getMetadata(CostEstimation.COST_MODEL_KEY)).getExpressionNode());
		}
	}

}
