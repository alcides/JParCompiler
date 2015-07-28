package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.HashMap;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AbstractFilter;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;


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
		if (element instanceof CtMethod) {
			CtMethod<?> m = (CtMethod<?>) element;
			if (m.getSimpleName().equals("main") && m.hasModifier(ModifierKind.STATIC) && m.hasModifier(ModifierKind.PUBLIC)) {
				
				// Surround main method with inits and shutdowns.
				Factory factory = m.getFactory();
				CtInvocation<?> c = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.init();").compile();
				m.getBody().insertBegin(c);
				
				CtInvocation<?> c2 = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.shutdown();").compile();
				m.getBody().insertEnd(c2);
				
			}
			
		}
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <E> void futurify(CtInvocation<E> element) {
		Factory factory = element.getFactory();
		
		CtTypeReference<?> t = element.getType();
		System.out.println(element);
		String id = "aeminium_task_" + (counter++);
		
		element.getFactory().getEnvironment().setComplianceLevel(8);

		if (t.isPrimitive()) t = t.box();
		
		CtLocalVariable<?> c = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.Future<" + t.getQualifiedName() + "> " + id + " = new aeminium.runtime.futures.Future<" + t.getQualifiedName() + ">( (t) -> null);").compile();
		
		// Replace null by current element
		CtLiteral<E> lit = (CtLiteral<E>) c.getElements(new AbstractFilter<CtLiteral<?>>(CtLiteral.class) {
			@Override
			public boolean matches(CtLiteral<?> element) {
				return element.getValue() == null;
			}
		}).get(0);
		CtInvocation<E> invoc = factory.Core().createInvocation();
		invoc.setTarget(element.getTarget());
		invoc.setExecutable(element.getExecutable());
		invoc.setType(element.getType());
		invoc.setArguments(element.getArguments());
		lit.replace(invoc);
		if (t.getSimpleName().equals("Void")) {
			CtReturn<?> nullret = factory.Core().createReturn();
			nullret.setReturnedExpression(factory.Code().createLiteral(null));
			
			CtLambda<?> lambda = ((CtLambda<?>) invoc.getParent());
			CtBlock<?> statlist = factory.Core().createBlock();
			statlist.addStatement((CtInvocation<?>) lambda.getExpression());
			statlist.addStatement(nullret);
			
			CtLambda<?> lambda2 = factory.Core().createLambda();
			lambda2.setBody((CtBlock) statlist);
			lambda2.setParameters(lambda.getParameters());
			lambda.replace((CtExpression) lambda2);
			
		}
		
		
		CtStatementList statlist = factory.Core().createStatementList();
		statlist.addStatement(c);
		
		CtStatement st = element.getParent(CtStatement.class);
		if (st.getParent() instanceof CtExecutable) {
			st = element;
		}
		st.insertBefore(statlist);
		
		
		// Create future.get()
		CtInvocation<E> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(element.getFactory().Code().createLocalVariableReference(c), false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType(element.getType());

		// Hard coded
		read.setExecutable((CtExecutableReference<E>) c.getType().getSuperclass().getDeclaredExecutables().toArray()[0]);
		
		element.replace((CtExpression<E>) read);
		

		
	}

}
