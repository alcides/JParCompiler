package aeminium.jparcompiler.processing;

import java.util.HashMap;

import spoon.processing.AbstractProcessor;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import aeminium.jparcompiler.model.PermissionSet;


public class AccessPermissionsProcessor<T> extends AbstractProcessor<CtMethod<T>> {

	public static HashMap<CtElement, PermissionSet> database;
	public static HashMap<SourcePosition, PermissionSet> databasePos;
	PermissionSetVisitor p = new PermissionSetVisitor();
	
	@Override
	public void process(CtMethod<T> meth) {
		FactoryReference.setFactory(meth.getFactory());
		meth.accept(p);
	}

	@Override
	public void processingDone() {
		super.processingDone();
		database = p.database;
		databasePos = p.databasePos;
		for (PermissionSet s : database.values()) {
			s.clean();
		}
	}

	
}
