package aeminium.jparcompiler.processing;

import aeminium.jparcompiler.model.PermissionSet;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtElement;

public class PermissionSetFixer extends PermissionSetVisitor {

	PermissionSetFixer() {
		super();
	}
	
	public <T> void visitCtCodeSnippetExpression(
			CtCodeSnippetExpression<T> expression) {
		if (expression.getMetadata(PermissionSet.PERMISSION_MODEL_KEY) != null) return;
		setPermissionSet(expression, new PermissionSet());
		System.out.println("Faking permission set for " + expression);
	}

	public void visitCtCodeSnippetStatement(CtCodeSnippetStatement statement) {
		if (statement.getMetadata(PermissionSet.PERMISSION_MODEL_KEY) != null) return;
		setPermissionSet(statement, new PermissionSet());
		System.out.println("Faking permission set for " + statement);
	}

	public void setPermissionSet(CtElement e, PermissionSet s) {
		if (e != null) {
			if (e.getMetadata(PermissionSet.PERMISSION_MODEL_KEY) == null) {
				e.putMetadata(PermissionSet.PERMISSION_MODEL_KEY, s);
			}
		}
	}

}
