package aeminium.jparcompiler.processing;

import java.util.HashMap;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import aeminium.jparcompiler.model.PermissionSet;


public class AccessPermissionsProcessor<T> extends AbstractProcessor<CtMethod<T>> {

	public static HashMap<CtElement, PermissionSet> database;
	PermissionSetVisitor p = new PermissionSetVisitor();
	
	@Override
	public void process(CtMethod<T> meth) {
		if (meth.getParent(CtClass.class).getSimpleName().equals("FuturifyTemplate")) return;
		
		meth.accept(p);
		System.out.println("Permissions of " +meth.getSimpleName());
		p.getPermissionSet(meth).printSet();
	}

	@Override
	public void processingDone() {
		super.processingDone();
		database = p.database;
		for (PermissionSet s : database.values()) {
			s.clean();
		}
	}

	
}
