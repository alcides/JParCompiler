package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;
import aeminium.jparcompiler.processing.utils.CopyCatFactory;
import aeminium.jparcompiler.processing.utils.Safety;


public class TaskCreationProcessor extends AbstractProcessor<CtElement> {
	
	HashMap<CtElement, PermissionSet> database;
	PermissionSetFixer fixer;
	int counter;
	
	@Override
	public void init() {
		super.init();
		database = AccessPermissionsProcessor.database;
		fixer = new PermissionSetFixer(database);
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
				m.getBody().updateAllParentsBelow();
				setPermissionSet(c, new PermissionSet());
				
				CtInvocation<?> c2 = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.shutdown();").compile();
				m.getBody().insertEnd(c2);
				m.getBody().updateAllParentsBelow();
				setPermissionSet(c2, new PermissionSet());
			} else {
				// create if parallelize
				
				Permission perm = new Permission(PermissionType.WRITE, element);
				perm.control = true;
				PermissionSet ps = new PermissionSet();
				ps.add(perm);
				
				CtIf i = factory.Core().createIf();
				CtExpression<?> inv = factory.Code().createCodeSnippetExpression("aeminium.runtime.futures.RuntimeManager.shouldSeq()").compile();
				setPermissionSet(inv, ps.copy());
				i.setCondition((CtExpression<Boolean>) inv);
				
				CtClass<?> cl = m.getParent(CtClass.class);
				CtExecutableReference<?> ref = factory.Executable().createReference(cl.getMethodsByName(SeqMethodProcessor.SEQ_PREFIX + m.getSimpleName()).get(0));
				
				ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
				for (CtParameter<?> p : m.getParameters()) {
					CtExpression<?> arg = factory.Code().createVariableRead(factory.Method().createParameterReference(p), m.hasModifier(ModifierKind.STATIC));
					args.add(arg);
					setPermissionSet(arg, ps.copy());
				}
				
				CtInvocation<?> body = factory.Code().createInvocation(null, ref, args);
				setPermissionSet(body, ps.copy());
				i.setThenStatement(body);
				
				setPermissionSet(i, ps.copy());
				m.getBody().insertBegin(i);
				m.getBody().updateAllParentsBelow();
			}
			
		}
		if (element instanceof CtInvocation<?>) {
			getPermissionSet(element);
			getPermissionSet(element.getParent()); // Double Check
			processInvocation((CtInvocation<?>) element);
		}
		if (element instanceof CtFor) {
			fixer.scan(element.getParent()); // Hack
			getPermissionSet(element);
			getPermissionSet(element.getParent()); // Double Check
			//processFor((CtFor) element);
		}
	}

	private void processFor(CtFor element) {
		// First, check conditions for parallelization.
		if (element.getForInit().size() != 1) return;
		if (element.getForUpdate().size() != 1) return;
		
		// Get Variable
		CtLocalVariable<?> v = (CtLocalVariable<?>) element.getForInit().get(0);
		CtExpression<?> st = v.getAssignment();
		CtExpression<?> end = null;
		CtTypeReference<?> type = v.getType();
		
		// Check for Increment
		boolean postinc = false;
		CtStatement inc = (CtStatement) element.getForUpdate().get(0);
		if (inc instanceof CtUnaryOperator) {
			CtUnaryOperator<?> incu = (CtUnaryOperator<?>) inc;
			if (incu.getKind() == UnaryOperatorKind.POSTINC) {
				postinc = true;
			}
		}
		if (!postinc) return;
		
		// Get ceiling
		CtExpression<Boolean> cond = element.getExpression();
		if (cond instanceof CtBinaryOperator) {
			CtBinaryOperator<Boolean> comp = (CtBinaryOperator<Boolean>) cond;
			if (comp.getKind() != BinaryOperatorKind.LT) return;
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
		if (end == null) return;
		// Now we know its postinc and we have the bottom and ceiling. 
		// Next, we evaluate for write permissions inside the cycle.
		PermissionSet vars = getPermissionSet(element.getBody());
		int countWrites = vars.count(PermissionType.WRITE);
		if (countWrites == 0) {
			generateContinuousFor(element, st, end);
		} else if (countWrites == 1) {
			generateContinuousForReduce(element, st, end, type);
		} else {
			System.out.println("Not generating parallel for because of permissions");
			vars.printSet();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void generateContinuousForReduce(CtFor element, CtExpression<?> st,
			CtExpression<?> end, CtTypeReference<?> iteratorType) {
		PermissionSet vars = getPermissionSet(element.getBody());
		
		CtElement target = null;
		for (Permission p : vars) {
			if (p.type == PermissionType.READWRITE || p.type == PermissionType.WRITE) {
				target = p.target;
			}
		}
		CtExpression<?> incrementReducer = null;
		CtOperatorAssignment<?,?> stToChange = null;
		// Get Updating statement
		CtBlock<?> b = (CtBlock<?>) element.getBody();
		for (CtStatement s : b.getStatements()) {
			for (Permission p : vars) {
				if ((p.type == PermissionType.READWRITE || p.type == PermissionType.WRITE) && p.target == target) {					
					if (s instanceof CtOperatorAssignment) {
						CtOperatorAssignment<?,?> ass = (CtOperatorAssignment<?,?>) s;
						if (ass.getKind() == BinaryOperatorKind.PLUS) {
							stToChange = ass;
							if (ass.getType().getSimpleName().equals("int"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression("aeminium.runtime.futures.codegen.ForHelper.intSum").compile();
							if (ass.getType().getSimpleName().equals("long"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression("aeminium.runtime.futures.codegen.ForHelper.longSum").compile();
							if (ass.getType().getSimpleName().equals("float"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression("aeminium.runtime.futures.codegen.ForHelper.floatSum").compile();
							if (ass.getType().getSimpleName().equals("double"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression("aeminium.runtime.futures.codegen.ForHelper.doubleSum").compile();
						}
					}
				}
			}
		}
		if (incrementReducer == null) {
			System.out.println("Not generating because of missing +=");
			return;
		}
		
		
		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);
		CtTypeReference<?> returnType = stToChange.getType();
		CtTypeReference<?> returnTypeBoxed = returnType.box();
		
		// Backup assign
		CtOperatorAssignment<?, ?> update = (CtOperatorAssignment<?, ?>) CopyCatFactory.clone(stToChange);
		
		String id = "aeminium_for_tmp_" + counter++;
		String idRet = "aeminium_for_ret_" + counter++;
		CtLocalVariable<?> hollowSetting = factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.HollowFuture<" + returnTypeBoxed + "> " + id + " = aeminium.runtime.futures.codegen.ForHelper.forContinuousIntReduce1(0,1, (Integer i) -> { return null; }, null)").compile();
		CtInvocation<?> forHelper = (CtInvocation<?>) hollowSetting.getDefaultExpression();
		ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		st.addTypeCast(iteratorType);
		end.addTypeCast(iteratorType);
		args.add(st);
		args.add(end);
		CtLambda<?> lambda = (CtLambda<?>) forHelper.getArguments().get(2);
		CtBlock<?> block = (CtBlock<?>) element.getBody();
		
		CtLocalVariable<?> varDecl = factory.Core().createLocalVariable();
		varDecl.setSimpleName(idRet);
		varDecl.setType((CtTypeReference) returnType);
		varDecl.setAssignment((CtExpression) stToChange.getAssignment());
		stToChange.replace(varDecl);
		
		CtReturn<?> ret = factory.Core().createReturn();
		CtVariableRead varRead = (CtVariableRead) factory.Code().createVariableRead(varDecl.getReference(), false);
		ret.setReturnedExpression(varRead);
		block.addStatement(ret);
		
		lambda.setBody((CtBlock) block);
		args.add(lambda);
		args.add(incrementReducer);
		forHelper.setArguments(args);
		element.replace(hollowSetting);
		setPermissionSet(hollowSetting, vars);
		
		// future.get()
		CtInvocation<?> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(hollowSetting.getReference(), false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType((CtTypeReference) returnType);
		read.setExecutable((CtExecutableReference) hollowSetting.getType().getDeclaredExecutables().toArray()[0]);
		update.setAssignment((CtExpression) read);
		setPermissionSet(read, vars);
		setPermissionSet(update, vars);
		insertAfter(hollowSetting, update);
	}

	private void generateContinuousFor(CtFor element, CtExpression<?> st,
			CtExpression<?> end) {
		System.out.println("Generating Continuous For");
	}

	private void processInvocation(CtInvocation<?> element) {
		if (element.getExecutable().getDeclaringType().getQualifiedName().startsWith("java.lang.Math")) {
			return;
		}
		futurifyInvocation(element);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <E> void futurifyInvocation(CtInvocation<E> element) {
		PermissionSet set = getPermissionSet(element);
		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);
		
		if (shouldFuturify(element)) return;
		
		CtTypeReference<?> t = element.getType();
		if (t.isPrimitive()) t = t.box();
		if (t.toString().endsWith("[]")) {
			System.out.println("Do not know how to handle this type " + t);
			return;
		}
		
		String id = "aeminium_task_" + (counter++);
		
		if (id.equals("aeminium_task_31")) {
			System.out.println("Element: " + element + ", " + element.getClass());
			set.printSet();
		}
		
		// Save block before changing element;
		CtBlock block = element.getParent(CtBlock.class);
		SourcePosition pos = element.getPosition();
		
		// Create Assign of Future
		CtTypeReference futureType = factory.Type().createReference("aeminium.runtime.futures.Future");
		CtLambda futureLambda = factory.Core().createLambda();
		List futureLambdaParams = new ArrayList<CtParameter>();
		CtParameter futureLambdaParam0 = factory.Core().createParameter();
		futureLambdaParam0.setType(factory.Type().createReference("aeminium.runtime.Task"));
		futureLambdaParam0.setSimpleName("aeminium_runtime_tmp");
		futureLambdaParams.add(futureLambdaParam0);
		futureLambda.setParameters(futureLambdaParams);
		// Fill in lambda missing
		setPermissionSet(futureLambda, new PermissionSet());
		CtConstructorCall newFuture = factory.Core().createConstructorCall();
		setPermissionSet(newFuture, new PermissionSet());
		newFuture.setType(futureType);
		newFuture.addArgument(futureLambda);
		CtLocalVariable<?> futureAssign = factory.Code().createLocalVariable(futureType, id, newFuture);
		setPermissionSet(futureAssign, set.copy());
		
		// Create future.get()
		CtInvocation<E> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(factory.Code().createLocalVariableReference(futureAssign), false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType(element.getType());
		read.setExecutable((CtExecutableReference<E>) futureAssign.getType().getSuperclass().getDeclaredExecutables().toArray()[0]);
		if (!(element.getParent() instanceof CtBlock)) read.addTypeCast(t);
		setPermissionSet(read, set.copy());
		element.replace((CtExpression<E>) read);
		read.getParent(CtBlock.class).updateAllParentsBelow();
		
		// Filling in Lambda after using element in replace.
		
		if (t.getSimpleName().equals("Void")) {
			CtReturn<?> nullret = factory.Core().createReturn();
			nullret.setReturnedExpression(factory.Code().createLiteral(null));
			setPermissionSet(nullret, new PermissionSet());
			
			CtBlock<?> futureLambdaBlock = factory.Core().createBlock();
			futureLambdaBlock.addStatement(element);
			futureLambdaBlock.addStatement(nullret);
			setPermissionSet(futureLambdaBlock, new PermissionSet());
			futureLambda.setBody(futureLambdaBlock);
		} else {
			futureLambda.setExpression(element);
		}
		
		// Find where to place the Future declaration.
		// Let's start with the current block;
		CtStatement previousStatement = null; // NULL means the beginning.
		for (CtStatement s : block.getStatements()) {
			if (s.getPosition() != null && s.getPosition().getLine() >= pos.getLine()) break;
			if (s instanceof CtLocalVariable){
				for (Permission pi : set) if (pi.target == s) previousStatement = s;
			}
			// Then look for control statements
			if (getPermissionSet(s).containsControl()) previousStatement = s;
		}
		// TODO: Find dependencies
		
		List<CtLocalVariable<?>> shadows = new ArrayList<CtLocalVariable<?>>();
		for (Permission pi : set ) {
			if (pi.target instanceof CtLocalVariable) {
				CtLocalVariable lv = (CtLocalVariable<?>) pi.target;
				CtLocalVariable ass = factory.Core().createLocalVariable();
				CtVariableRead<?> r = factory.Core().createVariableRead();
				r.setVariable(lv.getReference());
				ass.setDefaultExpression(r);
				ass.setSimpleName(lv.getSimpleName() + "_aeminium_shadow_v" + (counter++));
				ass.addModifier(ModifierKind.FINAL);
				ass.setType(lv.getType());
				shadows.add(ass);
				setPermissionSet(r, new PermissionSet());
				setPermissionSet(ass, new PermissionSet());
			}
		}
		if (shadows.size() > 0) {
			Query.getElements(futureLambda, (e) -> {
				if (e instanceof CtVariableAccess) {
					CtVariableAccess<?> va = (CtVariableAccess<?>) e;
					for (CtLocalVariable lv : shadows) {
						String[] parts = lv.getSimpleName().split("_");
						if (parts[0].equals(va.getVariable().getSimpleName())) {
							va.setVariable(lv.getReference());
						}
					}
				}
				return true;
			});
		}

		if (previousStatement == null) {
			block.insertBegin(futureAssign);
			for (CtLocalVariable<?> lv : shadows) {
				block.insertBegin(lv);
			}
			block.updateAllParentsBelow();
		} else {
			fixer.scan(previousStatement.getParent(CtBlock.class).getParent());
			insertAfter(previousStatement, futureAssign);
			for (CtLocalVariable<?> lv : shadows) {
				insertAfter(previousStatement, lv);
			}
			block = futureAssign.getParent(CtBlock.class);
		}
		
		// Fix broken blocks
		fixer.scan(block.getParent());
		fixer.scan(read.getParent(CtBlock.class).getParent());
	}

	private <E> boolean shouldFuturify(CtInvocation<E> element) {
		if (Safety.isSafe(element.getExecutable().getDeclaration())) {
			return true;
		}
		if (element.getExecutable().toString().equals("java.io.PrintStream.println")) {
			return true;
		}
		if (element.getExecutable().toString().equals("java.util.Arrays.asList")) {
			return true;
		}
		if (element.getExecutable().toString().startsWith("java.util.List")) {
			return true;
		}
		if (element.getExecutable().toString().startsWith("java.util.ArrayList")) {
			return true;
		}
		if (element.getExecutable().toString().startsWith("java.lang")) {
			return true;
		}
		return false;
	}
	
	protected void fixBlock(CtBlock<?> brokenBlock) {
		if (brokenBlock != null) {
			if (!hasPermissionSet(brokenBlock)) {
				PermissionSet blockSet = new PermissionSet();
				for (CtStatement s : brokenBlock.getStatements()) {
					blockSet = blockSet.merge(getPermissionSet(s));
				}
				setPermissionSet(brokenBlock, blockSet);
			}
		}
	}
	
	public void insertAfter(CtElement a1, CtStatement a2) {
		CtStatementList e = a1.getParent(CtStatementList.class);
		if (e == null) {
			throw new RuntimeException("Cannot insert after this element: " + a1);
		}
		if (getPermissionSet(a1) == null) {
			throw new RuntimeException("Statement " + a1 + " does not have a PermissionSet");
		}
		if (getPermissionSet(a2) == null) {
			throw new RuntimeException("Statement " + a2 + " does not have a PermissionSet");
		}
		PermissionSet backup = getPermissionSet(e);
		
		List<CtStatement> stmt = new ArrayList<CtStatement>();
		for (CtStatement s : e.getStatements()) {
			stmt.add(s);
			int match = Query.getElements(s, (CtElement el) -> el == a1).size();
			if (match == 1) {
				stmt.add(a2);
				a2.setParent(e);
			}
		}
		e.setStatements(stmt);
		e.updateAllParentsBelow();
		setPermissionSet(e, backup);
	}
	
	protected PermissionSet getPermissionSet(CtElement element) {
		for (int i = 0; i < 2; i++) {
			PermissionSet vars = database.get(element);
			if (vars != null) return vars;
			fixer.scan(element.getParent());
		}
		throw new RuntimeException("Missing database for " + element.hashCode() + " / " + element + " / " + element.getClass());
	}
	
	protected void setPermissionSet(CtElement e, PermissionSet s) {
		database.put(e, s);
	}
	
	protected void copyPermissionSet(CtElement e, CtElement to) {
		setPermissionSet(to, getPermissionSet(e));
	}
	
	protected boolean hasPermissionSet(CtElement el) {
		return database.containsKey(el);
	}

}
