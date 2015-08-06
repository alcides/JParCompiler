package aeminium.jparcompiler.processing;

import java.util.HashMap;

import aeminium.jparcompiler.model.PermissionSet;
import spoon.reflect.declaration.CtElement;

public class PermissionSetFixer extends PermissionSetVisitor {
	
	PermissionSetFixer(HashMap<CtElement, PermissionSet> db) {
		super();
		this.database = db;
	}
	
	public void setPermissionSet(CtElement e, PermissionSet s) {
		if (e != null) {
			if (!database.containsKey(e)) {
				database.put(e, s);
			}
		}
	}

}
