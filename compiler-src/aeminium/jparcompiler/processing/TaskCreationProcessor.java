package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AbstractFilter;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;
import aeminium.jparcompiler.processing.utils.Safety;


public class TaskCreationProcessor extends AbstractProcessor<CtElement> {
	
	HashMap<CtElement, PermissionSet> database;
	int counter;
	
	@Override
	public void init() {
		super.init();
		database = AccessPermissionsProcessor.database;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void process(CtElement element) {
		if (Safety.isSafe(element)) return;
		
		Factory factory = element.getFactory();
		if (element instanceof CtMethod) {
			CtMethod<?> m = (CtMethod<?>) element;
			if (m.getSimpleName().equals("main") && m.hasModifier(ModifierKind.STATIC) && m.hasModifier(ModifierKind.PUBLIC)) {
				// Surround main method with inits and shutdowns.
				CtInvocation<?> c = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.init();").compile();
				m.getBody().insertBegin(c);
				
				CtInvocation<?> c2 = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.shutdown();").compile();
				m.getBody().insertEnd(c2);
			} else {
				// create if parallelize
				CtIf i = factory.Core().createIf();
				CtExpression<?> inv = factory.Code().createCodeSnippetExpression("aeminium.runtime.futures.RuntimeManager.shouldSeq()").compile();
				i.setCondition((CtExpression<Boolean>) inv);
				
				CtClass<?> cl = m.getParent(CtClass.class);
				CtExecutableReference<?> ref = factory.Executable().createReference(cl.getMethodsByName(SeqMethodProcessor.SEQ_PREFIX + m.getSimpleName()).get(0));
				
				ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
				for (CtParameter<?> p : m.getParameters()) {
					args.add(factory.Code().createVariableRead(factory.Method().createParameterReference(p), m.hasModifier(ModifierKind.STATIC)));
				}
				
				CtInvocation<?> body = factory.Code().createInvocation(null, ref, args);
				i.setThenStatement(body);
				
				m.getBody().insertBegin(i);
			}
			
		}
		if (element instanceof CtInvocation<?>) {
			processInvocation((CtInvocation<?>) element);
		}
	}

	private void processInvocation(CtInvocation<?> element) {
		if (element.getPosition().getFile().toString().endsWith("FuturifyTemplate.java")) {
			return;
		}
		if (element.getExecutable().getDeclaringType().getQualifiedName().startsWith("java.lang.Math")) {
			return;
		}
		futurify(element);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <E> void futurify(CtInvocation<E> element) {
		Factory factory = element.getFactory();
		
		CtTypeReference<?> t = element.getType();
		String id = "aeminium_task_" + (counter++);
		
		if (element.getExecutable() != null) {
			if (Safety.isSafe(element.getExecutable().getDeclaration())) {
				return;
			}
		}
		
		// Lambda
		element.getFactory().getEnvironment().setComplianceLevel(8);
		if (t.isPrimitive()) t = t.box();
		CtLocalVariable<?> c = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.Future<" + t.getQualifiedName() + "> " + id + " = new aeminium.runtime.futures.Future<" + t.getQualifiedName() + ">( (aeminium_runtime_tmp) -> null);").compile();
		
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
		
		// Let's find lastest local dependency

		PermissionSet set = database.get(element);
		CtBlock block = element.getParent(CtBlock.class);
		CtStatement top = block.getStatements().get(0);
		int topLine = -1;
		
		// First iterate through local variables
		for (Permission p : set) {
			if (p.target instanceof CtLocalVariable) {
				int line = p.target.getPosition().getLine();
				if  (line > topLine) {
					topLine = line;
					top = (CtLocalVariable) p.target;
				}
			}
		}
		// Then look for control statements
		int line = -1;
		for (CtStatement s : block.getStatements()) {
			line = (s.getPosition() != null) ? s.getPosition().getLine() : line+1;
			if (line < topLine) continue;
			if (line >= element.getPosition().getLine()) break;
			if (database.get(s) != null && database.get(s).containsControl()) {
				topLine = line;
				top = s;
			}
		}
		if (topLine == -1) {
			block.insertBegin(c);
		} else {
			insertAfter(top, c);
			top.getParent().updateAllParentsBelow();
		}
		
		// Create future.get()
		CtInvocation<E> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(element.getFactory().Code().createLocalVariableReference(c), false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType(element.getType());
		read.setExecutable((CtExecutableReference<E>) c.getType().getSuperclass().getDeclaredExecutables().toArray()[0]);
		
		element.replace((CtExpression<E>) read);
		

		
	}
	
	public static void insertAfter(CtElement a1, CtStatement a2) {
		CtStatementList e = a1.getParent(CtStatementList.class);
		if (e == null) {
			System.out.println("Bode");
		}
		List<CtStatement> stmt = new ArrayList<CtStatement>();
		for (CtStatement s : e.getStatements()) {
			stmt.add(s);
			if (s == a1) {
				stmt.add(a2);
			}
		}
		e.setStatements(stmt);
	}
	

}
