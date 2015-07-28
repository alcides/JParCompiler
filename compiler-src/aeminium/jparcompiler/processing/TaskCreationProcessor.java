package aeminium.jparcompiler.processing;

import java.util.HashMap;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;
import aeminium.jparcompiler.templates.FuturifyTemplate;


public class TaskCreationProcessor extends AbstractProcessor<CtElement> {
	
	HashMap<CtElement, PermissionSet> database;
	int counter;
	
	@Override
	public void init() {
		super.init();
		database = AccessPermissionsProcessor.database;
	}

	@Override
	public void process(CtElement element) {
		if (element instanceof CtInvocation<?>) {
			processInvocation((CtInvocation<?>) element);
		}
	}

	private void processInvocation(CtInvocation<?> element) {
		if (database.containsKey(element)) {
			PermissionSet s = database.get(element);
			boolean hasWrites = false;
			for (Permission p : s) {
				if (p.type == PermissionType.WRITE || p.type == PermissionType.READWRITE) {
					hasWrites = true;
				}
			}
			if(!hasWrites) {
				futurify(element);
			}
		}
	}

	private <E> void futurify(CtInvocation<E> element) {
		CtTypeReference<?> t = element.getType();
		System.out.println(element);
		String id = "aeminium_task_" + (counter++);

		if (t.isPrimitive()) t = t.box();
		FuturifyTemplate template = new FuturifyTemplate(t.getActualClass(), id);
		CtStatement nst = template.apply(null);
	
		CtStatement st = element.getParent(CtStatement.class);
	}

}
