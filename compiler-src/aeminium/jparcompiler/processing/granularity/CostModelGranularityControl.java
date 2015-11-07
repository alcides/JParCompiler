package aeminium.jparcompiler.processing.granularity;

import aeminium.jparcompiler.model.CostEstimation;
import aeminium.jparcompiler.processing.CostEstimatorProcessor;
import aeminium.jparcompiler.processing.utils.ModelUtils;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;

public class CostModelGranularityControl implements GranularityControl {
	
	public boolean shouldParallelize(CtElement element) {
		CostEstimation ce = CostEstimatorProcessor.database.get(element);
		if (ce == null) {
			CostEstimatorProcessor.visitor.scan(element);
			ce = CostEstimatorProcessor.visitor.get(element);
		}
		ce.getExpressionNode();
		if (ce.isExpressionComplex) {
			return true;
		} else {
			long estimation = ce.simpleCost;
			long overhead = CostEstimatorProcessor.basicCosts.get("parallel");
			return estimation > overhead;
		}
	}
	
	public boolean hasGranularityControlExpression(CtElement e) {
		CostEstimation ce = CostEstimatorProcessor.database.get(e);
		return ce != null && ce.isExpressionComplex;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public CtExpression<?> getGranularityControlElement(CtElement e, CtExpression<?> context) {
		CostEstimation ce = CostEstimatorProcessor.database.get(e);
		CtBinaryOperator<Boolean> inf = e.getFactory().Core().createBinaryOperator();
		CtExpression exp;
		if (context instanceof CtInvocation) {
			exp = (CtExpression) ModelUtils.replaceThisReferences(ce, (CtExpression<?>) context).getExpressionNode();
		} else {
			if (context != null)
				System.out.println("REF" + context.getClass());
			exp = ce.getExpressionNode();
		}
		inf.setKind(BinaryOperatorKind.LT);
		inf.setLeftHandOperand(exp);
		long threshold = CostEstimatorProcessor.basicCosts.get("parallel");
		CtLiteral lit = e.getFactory().Code().createLiteral(threshold);
		inf.setRightHandOperand(lit);
		inf.updateAllParentsBelow();
		return inf;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public CtExpression<?> getGranularityControlUnits(CtFor e) {
		shouldParallelize(e);
		CostEstimation ce = CostEstimatorProcessor.database.get(e);
		CtBinaryOperator<Boolean> inf = e.getFactory().Core().createBinaryOperator();
		inf.setKind(BinaryOperatorKind.DIV);
		inf.setLeftHandOperand(ce.getExpressionNode());
		long threshold = CostEstimatorProcessor.basicCosts.get("parallel");
		CtLiteral lit = e.getFactory().Code().createLiteral(threshold);
		inf.setRightHandOperand(lit);
		inf.addTypeCast(e.getFactory().Type().createReference(int.class));
		return inf;
	}
}
