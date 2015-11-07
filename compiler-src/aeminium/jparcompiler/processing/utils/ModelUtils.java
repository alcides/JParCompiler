package aeminium.jparcompiler.processing.utils;

import aeminium.jparcompiler.model.CostEstimation;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.visitor.Filter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ModelUtils {

	public static CtExpression<?> model(CtExpression<?> expr) {
		CtExpression<?> ne = (CtExpression<?>) CopyCatFactory.clone(expr);

		ne.getElements(new Filter<CtVariableRead<?>>() {
			@Override
			public boolean matches(CtVariableRead<?> element) {
				
				if (element.getVariable() instanceof CtLocalVariableReference) {
					CtLocalVariableReference lvr = (CtLocalVariableReference) element.getVariable();
 					CtLocalVariable lv = lvr.getDeclaration();
 					if (lv != null) {
						if (lv.getDefaultExpression() != null) {
							CtExpression n = (CtExpression<?>) CopyCatFactory.clone(lv.getDefaultExpression()); 
							element.replace(n);	
						}
 					}
				}
				return false;
			}
			
		});
		return ne;
	}

	public static CostEstimation replaceParameters(CtInvocation<?> invocation, CostEstimation old, CtExecutable<?> original) {
		
		CostEstimation ne = new CostEstimation();
		ne.add(old.instructions);
		for (String k : old.dependencies.keySet()) {
			ne.add(k, old.dependencies.get(k));
		}
		
		for (CtExpression<?> index : old.complexDependencies.keySet()) {
			// Recursive call
			CostEstimation innerCe = replaceParameters(invocation, old.complexDependencies.get(index), original);
			CtExpression<?> e = (CtExpression<?>) CopyCatFactory.clone(index);
			
			e.getElements(new Filter<CtVariableRead>() {

				@Override
				public boolean matches(CtVariableRead element) {
					if (element.getVariable() instanceof CtParameterReference) {
						CtParameterReference lvr = (CtParameterReference) element.getVariable();
	 					CtParameter lv = lvr.getDeclaration();
	 					int i = getParameterIndex(lv, original);
	 					if (i >= 0) {
	 						element.replace(invocation.getArguments().get(i));
	 					}
					}
					return false;
				}
				
			});
			
			ne.addComplex(e, innerCe);
		}
		return ne;
	}

	protected static int getParameterIndex(CtParameter lv, CtExecutable<?> declaration) {
		int i = 0;
		for ( CtParameter p : declaration.getParameters()) {
			if (p.getSimpleName().equals(lv.getSimpleName()) && lv.getType().equals(p.getType())) {
				return i;
			}
			i += 1;
		}
		return -1;
	}

}
