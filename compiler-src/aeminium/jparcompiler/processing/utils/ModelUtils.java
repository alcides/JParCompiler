package aeminium.jparcompiler.processing.utils;

import aeminium.jparcompiler.model.CostEstimation;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.visitor.Filter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ModelUtils {

	public static CtExpression<?> model(CtExpression<?> expr) {
		final CtExpression<?> ne = (CtExpression<?>) CopyCatFactory.clone(expr);
		
		ne.getElements(new Filter<CtVariableRead<?>>() {
			@Override
			public boolean matches(CtVariableRead<?> element) {
				replaceLocalVarByDefaults(ne, element);
				return false;
			}
		});
		// TODO: Removed for analyzer
		/*if (expr instanceof CtVariableRead) {
			return replaceLocalVarByDefaults(ne, (CtVariableRead<?>) ne);
		}*/
		return ne;
	}

	private static CtExpression<?> replaceLocalVarByDefaults(CtExpression<?> expr, CtVariableRead<?> element) {
		if (element.getVariable() instanceof CtLocalVariableReference) {
			CtLocalVariableReference lvr = (CtLocalVariableReference) element.getVariable();
			CtLocalVariable lv = lvr.getDeclaration();
			if (lv != null) {
				if (lv.getDefaultExpression() != null) {
					CtExpression n = (CtExpression<?>) CopyCatFactory.clone(lv.getDefaultExpression());
					if (expr == element) {
						return n; // Replace only works inside a subtree
					} else {
						element.replace(n);
					}
				}
			}
		}
		return element;
	}

	public static CostEstimation replaceParameters(CtInvocation<?> invocation, CostEstimation old, CtExecutable<?> original) {

		CostEstimation ne = new CostEstimation();
		ne.add(old.instructions);
		for (String k : old.dependencies.keySet()) {
			ne.add(k, old.dependencies.get(k));
		}

		for (CtExpression<?> index : old.complexDependencies.keySet()) {
			if (index == null || old.complexDependencies.get(index) == null) continue; // BUG: FIXME
			
			// Recursive call
			CostEstimation innerCe = replaceParameters(invocation, old.complexDependencies.get(index), original);
			CtExpression<?> e = (CtExpression<?>) CopyCatFactory.clone(index);
			final CtExpression eInner = e;
			e.getElements(new Filter<CtVariableRead>() {
				@Override
				public boolean matches(CtVariableRead element) {
					replaceParameter(invocation, original, element, eInner);
					return false;
				}
			});

			if (e instanceof CtVariableRead) {
				e = replaceParameter(invocation, original, (CtVariableRead) e, e);
			}
			ne.addComplex(e, innerCe);
		}
		return ne;
	}

	private static CtExpression<?> replaceParameter(CtInvocation<?> invocation, CtExecutable<?> original,
			CtVariableRead element, CtExpression<?> originalCost) {
		if (element.getVariable() instanceof CtParameterReference) {
			CtParameterReference lvr = (CtParameterReference) element.getVariable();
			CtParameter lv = lvr.getDeclaration();
			int i = getParameterIndex(lv, original);
			if (i >= 0) {
				if (originalCost == element)
					return invocation.getArguments().get(i);
				else
					element.replace(invocation.getArguments().get(i));
			}
		}
		return originalCost;
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

	public static CostEstimation wrapScope(CostEstimation old, CtElement context) {
		CostEstimation ne = new CostEstimation();
		ne.add(old.instructions);
		for (String k : old.dependencies.keySet()) {
			ne.add(k, old.dependencies.get(k));
		}

		for (CtExpression<?> index : old.complexDependencies.keySet()) {
			CostEstimation nec = wrapScope(old.complexDependencies.get(index), context);
			CtExpression newIndex = replaceLocalVars(index, context);
			ne.addComplex(newIndex, nec);
		}
		return ne;
	}

	private static CtExpression replaceLocalVars(CtExpression<?> index, CtElement context) {
		if (index instanceof CtBinaryOperator) {
			CtBinaryOperator bin = (CtBinaryOperator) CopyCatFactory.clone(index);
			bin.setRightHandOperand(replaceLocalVars(bin.getRightHandOperand(), context));
			bin.setLeftHandOperand(replaceLocalVars(bin.getLeftHandOperand(), context));
			return bin;
		}
		CtExpression<?> nIndex = (CtExpression<?>) CopyCatFactory.clone(index);
		boolean replaceByLit = nIndex.getElements(new Filter<CtVariableRead>() {
			@Override
			public boolean matches(CtVariableRead element) {
				if (element.getVariable().getDeclaration() instanceof CtLocalVariable) {
					return true;
				}
				return false;
			}
		}).size() > 0;
		if (replaceByLit) 
			return index.getFactory().Code().createLiteral(100);
		else 
			return nIndex;
	}

	public static CostEstimation replaceThisReferences(CostEstimation old, CtExpression<?> target) {
		CostEstimation ne = new CostEstimation();
		ne.add(old.instructions);
		for (String k : old.dependencies.keySet()) {
			ne.add(k, old.dependencies.get(k));
		}

		for (CtExpression<?> index : old.complexDependencies.keySet()) {
			CostEstimation nec = replaceThisReferences(old.complexDependencies.get(index), target);
			CtExpression newIndex = replaceThis(index, target);
			ne.addComplex(newIndex, nec);
		}
		return ne;
	}

	private static CtExpression replaceThis(CtExpression<?> index, CtExpression<?> target) {
		CtExpression<?> nIndex = (CtExpression<?>) CopyCatFactory.clone(index);
		nIndex.getElements(new Filter<CtThisAccess>() {
			@Override
			public boolean matches(CtThisAccess element) {
				CtElement p = element.getParent();
				element.replace(target);
				p.updateAllParentsBelow();
				return false;
			}
		});
		nIndex.getElements(new Filter<CtFieldAccess<?>>() {
			@Override
			public boolean matches(CtFieldAccess<?> element) {
				if (element.getTarget() == null) {
					element.setTarget(target);
					element.updateAllParentsBelow();
				}
				return false;
			}
		});
		nIndex.getElements(new Filter<CtInvocation<?>>() {
			@Override
			public boolean matches(CtInvocation<?> element) {
				if (element.getTarget() == null && !element.getExecutable().isStatic()) {
					element.setTarget(target);
					element.updateAllParentsBelow();
				}
				return false;
			}
		});
		nIndex.updateAllParentsBelow();
		return nIndex;
	}

}
