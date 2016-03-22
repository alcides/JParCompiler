package aeminium.jparcompiler.processing;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtMethod;


public class AccessPermissionsProcessor<T> extends AbstractProcessor<CtMethod<T>> {

	PermissionSetVisitor p = new PermissionSetVisitor();
	
	@Override
	public void process(CtMethod<T> meth) {
		FactoryReference.setFactory(meth.getFactory());
		meth.accept(p);
	}

	@Override
	public void processingDone() {
		super.processingDone();
	}

	
}
