package aeminium.jparcompiler.processing.utils;

import aeminium.jparcompiler.model.PermissionSet;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

public class ForAnalyzer {
	
	CtFor element;
	
	boolean valid = false;
	
	public CtExpression<?> st;
	public CtExpression<?> end = null;
	public CtTypeReference<?> type;
	public PermissionSet oldVars;
	public PermissionSet vars;
	
	
	public ForAnalyzer(CtFor forElement) {
		this.element = forElement;
	}
	
	public boolean canBeAnalyzed() {
		check();
		return valid;
	}
	
	public void check() {
		if (element.getForInit().size() != 1)
			return;
		if (element.getForUpdate().size() != 1)
			return;
		
		// Get Variable
		CtStatement def = element.getForInit().get(0);
		if (!(def instanceof CtLocalVariable)) {
			return;
		}
		CtLocalVariable<?> v = (CtLocalVariable<?>) def;
		st = v.getAssignment();
		type = v.getType();

		// Check for Increment
		boolean postinc = false;
		CtStatement inc = (CtStatement) element.getForUpdate().get(0);
		if (inc instanceof CtUnaryOperator) {
			CtUnaryOperator<?> incu = (CtUnaryOperator<?>) inc;
			if (incu.getKind() == UnaryOperatorKind.POSTINC) {
				postinc = true;
			}
		}
		if (!postinc)
			return;

		// Get ceiling
		CtExpression<Boolean> cond = element.getExpression();
		if (cond instanceof CtBinaryOperator) {
			CtBinaryOperator<Boolean> comp = (CtBinaryOperator<Boolean>) cond;
			if (comp.getKind() != BinaryOperatorKind.LT)
				return;
			CtExpression<?> left = comp.getLeftHandOperand();
			CtExpression<?> right = comp.getRightHandOperand();
			if (left instanceof CtVariableRead) {
				CtVariableRead<?> read = (CtVariableRead<?>) left;
				if (read.getVariable().getDeclaration() == v) {
					end = right;
				}
			}
			if (right instanceof CtVariableRead) {
				CtVariableRead<?> read = (CtVariableRead<?>) right;
				if (read.getVariable().getDeclaration() == v) {
					end = left;
				}
			}
		}
		if (end == null)
			return;
		// Now we know its postinc and we have the bottom and ceiling.

		oldVars = (PermissionSet) element.getBody().getMetadata(PermissionSet.PERMISSION_MODEL_KEY);
		if (oldVars != null) {
			// We have to remove the indexed writes and reads that are parallel
			element.getElements((e) -> {
				if (e instanceof CtArrayAccess) {
					CtElement el = e;
					while (el != element) {
						if (el.getMetadataKeys().contains(PermissionSet.PERMISSION_MODEL_KEY)) {
							boolean deleted = ((PermissionSet) el.getMetadata(PermissionSet.PERMISSION_MODEL_KEY)).removeIf(
									(p) -> p.index != null && p.index == v);
							if (!deleted) {
								break;
							}
						} else {
							//System.out.println("Fail in " + el + ", " + el.getPosition());
						}
						el = el.getParent();
					}
				}
				return false;
			});
			// Next, we evaluate for write permissions inside the cycle.
			vars = (PermissionSet) element.getBody().getMetadata(PermissionSet.PERMISSION_MODEL_KEY);
			//System.out.println("Permissions for  " + element.getPosition());
			//vars.printSet();
		}
		valid = true;
	}
	
	
}
