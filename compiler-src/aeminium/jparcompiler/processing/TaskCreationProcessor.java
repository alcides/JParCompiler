package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import aeminium.jparcompiler.model.CostEstimation;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;
import aeminium.jparcompiler.processing.granularity.CostModelGranularityControl;
import aeminium.jparcompiler.processing.granularity.GranularityControl;
import aeminium.jparcompiler.processing.granularity.NoGranularityControl;
import aeminium.jparcompiler.processing.granularity.SimpleGranularityControl;
import aeminium.jparcompiler.processing.utils.CodeGenHelper;
import aeminium.jparcompiler.processing.utils.CopyCatFactory;
import aeminium.jparcompiler.processing.utils.ForAnalyzer;
import aeminium.jparcompiler.processing.utils.Safety;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.NoVisit;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtWhile;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.Query;

public class TaskCreationProcessor extends AbstractProcessor<CtElement> {
	private static final String PARALLELIZED_KEY = "parallelized";
	PermissionSetFixer permFixer;
	CostModelFixer costFixer;
	CodeGenHelper codegen;
	int counterTasks;
	HashMap<CtElement, CtVariableReference<?>> tasks = new HashMap<CtElement, CtVariableReference<?>>();
	public static HashMap<CtMethod<?>, CtClass<?>> recAux = new HashMap<CtMethod<?>, CtClass<?>>();

	private static GranularityControl granularityControl;
	static {
		if (System.getenv("PARALLELIZE") == null) {
			granularityControl = new CostModelGranularityControl();
		} else {
			String par = System.getenv("PARALLELIZE");
			if (par.equals("all")) {
				granularityControl = new NoGranularityControl();
			} else {
				granularityControl = new SimpleGranularityControl();
			}
		}
	}

	@Override
	public void init() {
		super.init();
		permFixer = new PermissionSetFixer();
		costFixer = new CostModelFixer();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void process(CtElement element) {
		if (codegen == null) codegen = new CodeGenHelper(element.getFactory());
			
		if (element instanceof CtMethod && ((CtMethod) element).getSimpleName().equals("main")) {
			// Skipping main
		} else {
			if (Safety.isSafe(element))
				return;
		}
		if (element.getParent(CtClass.class).getAnnotation(NoVisit.class) != null)
			return;

		Factory factory = element.getFactory();
		if (element instanceof CtMethod) {
			CtMethod<?> m = (CtMethod<?>) element;
			if (m.getSimpleName().equals("main") && m.hasModifier(ModifierKind.STATIC)
					&& m.hasModifier(ModifierKind.PUBLIC)) {
				// Surround main method with inits and shutdowns.
				CtInvocation<?> c = factory.Code()
						.createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.init();").compile();
				m.getBody().insertBegin(c);
				m.getBody().updateAllParentsBelow();
				setPermissionSet(c, new PermissionSet());
				setCost(c, new CostEstimation());

				CtInvocation<?> c2 = factory.Code()
						.createCodeSnippetStatement("aeminium.runtime.futures.RuntimeManager.shutdown();").compile();
				m.getBody().insertEnd(c2);
				m.getBody().updateAllParentsBelow();
				setPermissionSet(c2, new PermissionSet());
				setCost(c2, new CostEstimation());
			} else {
				if (!recAux.containsKey(m)) { // Not recursive
					// futurifyMethod(element, factory, m); Disabled because
					// granularity control is on call-site.
				}
			}

		}

		if (element instanceof CtConstructorCall) {
			CtConstructorCall<?> call = (CtConstructorCall<?>) element;
			CtTypeReference<?> randomT = factory.Type().createReference("java.util.Random");
			if (call.getType() != null && call.getType().equals(randomT) && call.getExecutable().getDeclaringType().equals(randomT)) {
				CtTypeReference randomTS = factory.Type().createReference(ThreadLocalRandom.class);
				CtExecutableReference ref = randomTS.getDeclaredExecutables().stream()
						.filter(e -> e.getSimpleName().equals("current")).iterator().next();
				CtTypeAccess<?> randomTSAccess = factory.Code().createTypeAccess(randomTS);
				CtExpression current = factory.Code().createInvocation(randomTSAccess, ref,
						new ArrayList<CtExpression<?>>());
				call.replace(current);
			}
		}

		if (element instanceof CtInvocation<?>) {
			
			String qname = null;
			CtInvocation inv = (CtInvocation) element;
			if (inv.getExecutable() != null)
				if (inv.getExecutable().getDeclaringType() != null)
					qname = inv.getExecutable().getDeclaringType().getQualifiedName();
			
			if (  qname == null || !qname.startsWith("aeminium.runtime.futures")) {
				getPermissionSet(element);
				getPermissionSet(element.getParent()); // Double Check
				if (granularityControl.shouldParallelize(element)) {
					processInvocation((CtInvocation<?>) element);
				}	
			}
			
			
		}
		if (element instanceof CtFor) {
			permFixer.scan(element.getParent()); // Hack
			getPermissionSet(element);
			getPermissionSet(element.getParent()); // Double Check
			if (granularityControl.shouldParallelize(element)) {
				processFor((CtFor) element);
			}
		}
		element.getParent().updateAllParentsBelow();
	}

	@SuppressWarnings("unchecked")
	public void futurifyMethod(CtElement element, Factory factory, CtMethod<?> m) {
		// create if parallelize
		Permission perm = new Permission(PermissionType.WRITE, element);
		perm.control = true;
		PermissionSet ps = new PermissionSet();
		ps.add(perm);

		CtIf i = factory.Core().createIf();
		CtExpression<?> inv = factory.Code()
				.createCodeSnippetExpression("aeminium.runtime.futures.RuntimeManager.shouldSeq()").compile();
		setPermissionSet(inv, ps.copy());
		setCost(inv, new CostEstimation());
		i.setCondition((CtExpression<Boolean>) inv);

		CtClass<?> cl = m.getParent(CtClass.class);
		CtExecutableReference<?> ref = factory.Executable()
				.createReference(cl.getMethodsByName(SeqMethodProcessor.SEQ_PREFIX + m.getSimpleName()).get(0));

		ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		for (CtParameter<?> p : m.getParameters()) {
			CtExpression<?> arg = factory.Code().createVariableRead(factory.Method().createParameterReference(p),
					m.hasModifier(ModifierKind.STATIC));
			args.add(arg);
			setCost(arg, new CostEstimation());
			setPermissionSet(arg, ps.copy());
		}

		CtInvocation<?> body = factory.Code().createInvocation(null, ref, args);
		setPermissionSet(body, ps.copy());
		setCost(body, new CostEstimation());

		i.setThenStatement(body);
		setCost(i, new CostEstimation());
		setPermissionSet(i, ps.copy());
		m.getBody().insertBegin(i);
		m.getBody().updateAllParentsBelow();
	}

	private void processFor(CtFor element) {
		// First, check conditions for parallelization.
		ForAnalyzer fa = new ForAnalyzer(element);
		if (!fa.canBeAnalyzed()) {
			return;
		}

		if (!granularityControl.shouldParallelize(element)) {
			return;
		}
		
		int countWrites = fa.vars.count(PermissionType.WRITE);
		if (countWrites <= 1) {
			if (granularityControl.hasGranularityControlExpression(element)) {
				CtIf ifc = createGranularityIf(element, granularityControl.getGranularityControlElement(element, null));
				CtBlock<?> bt = element.getFactory().Core().createBlock();
				bt.addStatement((CtStatement) CopyCatFactory.clone(element));
				ifc.setThenStatement(bt);
				element.replace(ifc);
				CtBlock<?> be = element.getFactory().Core().createBlock();
				be.addStatement(element);
				ifc.setElseStatement(be);
				ifc.getParent().updateAllParentsBelow();
				setPermissionSet(ifc, getPermissionSet(element));
				ifc.getParent().updateAllParentsBelow();
			}
		}
		if (countWrites == 0) {
			generateContinuousFor(element, fa.st, fa.end, fa.type, fa.oldVars);
		} else if (countWrites == 1) {
			generateContinuousForReduce(element, fa.st, fa.end, fa.type, fa.oldVars);
		} else {
			System.out.println("Not generating parallel for because of permissions. " + element.getPosition());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void generateContinuousForReduce(CtFor element, CtExpression<?> st, CtExpression<?> end,
			CtTypeReference<?> iteratorType, PermissionSet oldVars) {
		PermissionSet vars = getPermissionSet(element.getBody());

		CtElement target = null;
		for (Permission p : vars) {
			if (p.type == PermissionType.WRITE) {
				target = p.target;
			}
		}
		CtExpression<?> incrementReducer = null;
		CtOperatorAssignment<?, ?> stToChange = null;
		// Get Updating statement
		CtBlock<?> b = (CtBlock<?>) element.getBody();
		for (CtStatement s : b.getStatements()) {
			for (Permission p : vars) {
				if (p.type == PermissionType.WRITE && p.target == target) {
					if (s instanceof CtOperatorAssignment) {
						CtOperatorAssignment<?, ?> ass = (CtOperatorAssignment<?, ?>) s;	
						if (ass.getKind() == BinaryOperatorKind.PLUS) {
							stToChange = ass;
							if (ass.getType().getSimpleName().equals("int"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression(
										"aeminium.runtime.futures.codegen.ForHelper.intSum").compile();
							if (ass.getType().getSimpleName().equals("long"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression(
										"aeminium.runtime.futures.codegen.ForHelper.longSum").compile();
							if (ass.getType().getSimpleName().equals("float"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression(
										"aeminium.runtime.futures.codegen.ForHelper.floatSum").compile();
							if (ass.getType().getSimpleName().equals("double"))
								incrementReducer = ass.getFactory().Code().createCodeSnippetExpression(
										"aeminium.runtime.futures.codegen.ForHelper.doubleSum").compile();
							setPermissionSet(incrementReducer, new PermissionSet());
							setCost(incrementReducer, new CostEstimation());
						}
					}
				}
			}
		}
		if (incrementReducer == null) {
			System.out.println("Not generating because of missing +=. " + element.getPosition());
			return;
		}

		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);
		CtTypeReference<?> returnType = stToChange.getType();
		CtTypeReference<?> returnTypeBoxed = returnType.box();
		CtTypeReference<?> boxedIterType = iteratorType.box();

		// Backup assign
		CtOperatorAssignment<?, ?> update = (CtOperatorAssignment<?, ?>) CopyCatFactory.clone(stToChange);

		String id = "aeminium_for_tmp_" + counterTasks++;
		String idRet = "aeminium_for_ret_" + counterTasks++;
		CtLocalVariable<?> hollowSetting = factory.Code()
				.createCodeSnippetStatement("aeminium.runtime.futures.HollowFuture<" + returnTypeBoxed + "> " + id
						+ " = aeminium.runtime.futures.codegen.ForHelper.forContinuous" + boxedIterType.getSimpleName()
						+ "Reduce1(0,1, (" + boxedIterType
						+ " i) -> { return null; }, null, aeminium.runtime.Hints.LARGE, 128)")
				.compile();
		CtInvocation<?> forHelper = (CtInvocation<?>) hollowSetting.getDefaultExpression();
		ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		st.addTypeCast(iteratorType);
		end.addTypeCast(iteratorType);
		args.add(st);
		args.add(end);
		CtLambda<?> lambda = (CtLambda<?>) forHelper.getArguments().get(2);
		CtBlock<?> block = (CtBlock<?>) element.getBody();

		// VarName
		String varName = ((CtLocalVariable<?>) element.getForInit().get(0)).getSimpleName();
		lambda.getParameters().get(0).setSimpleName(varName);

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

		// Hints
		int c = element.getBody()
				.getElements((e) -> (e instanceof CtWhile || e instanceof CtFor || e instanceof CtInvocation)).size();
		CtFieldRead hint = getHintForCount(factory, c);
		args.add(hint);
		
		args.add(granularityControl.getGranularityControlUnits(element));
		forHelper.setArguments(args);
		element.replace(hollowSetting);
		setPermissionSet(lambda, new PermissionSet());
		setCost(lambda, new CostEstimation());
		setPermissionSet(hollowSetting, vars);
		setCost(hollowSetting, new CostEstimation());

		// future.get()
		CtInvocation<?> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(hollowSetting.getReference(), false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType((CtTypeReference) returnType);
		read.setExecutable(getExecutableReferenceOfMethodByName(hollowSetting.getType(), "get"));
		update.setAssignment((CtExpression) read);
		setPermissionSet(read, oldVars);
		setPermissionSet(update, oldVars);
		setCost(read, new CostEstimation());
		setCost(update, new CostEstimation());
		insertAfter(hollowSetting, update);
		
		read.putMetadata(PARALLELIZED_KEY, true);
		update.putMetadata(PARALLELIZED_KEY, true);
		varDecl.putMetadata(PARALLELIZED_KEY, true);
		lambda.putMetadata(PARALLELIZED_KEY, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void generateContinuousFor(CtFor element, CtExpression<?> st, CtExpression<?> end,
			CtTypeReference<?> iteratorType, PermissionSet oldBodyVars) {
		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);

		// Required for shadow variables
		counterTasks++;

		CtTypeReference boxedIterType = iteratorType.box();

		PermissionSet vars = getPermissionSet(element);
		PermissionSet oldVars = vars.copy();
		PermissionSet bodyVars = getPermissionSet(element.getBody());
		if (counterTasks == 8)
			bodyVars.printSet();

		// factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.codegen.ForHelper.forContinuousInt(0,1,
		// (Integer i) -> { return null; })").compile();
		CtInvocation hollowSetting = factory.Core().createInvocation();
		CtTypeReference futureType = factory.Type().createReference("aeminium.runtime.futures.codegen.ForHelper");
		CtExecutableReference<?> methodReferenceExpression = futureType.getDeclaredExecutables().stream()
				.filter((e) -> e.getSimpleName().equals("forContinuous" + boxedIterType.getSimpleName()) && e.getParameters().size() == 5 ).iterator()
				.next();

		CtTypeAccess staticReference = factory.Code().createTypeAccess(futureType);
		hollowSetting.setTarget(staticReference);

		hollowSetting.setExecutable(methodReferenceExpression);
		ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		st.addTypeCast(iteratorType);
		end.addTypeCast(iteratorType);
		args.add(st);
		args.add(end);
		CtLambda<?> lambda = factory.Core().createLambda();
		CtParameter par = factory.Core().createParameter();

		// VarName
		String varName = ((CtLocalVariable<?>) element.getForInit().get(0)).getSimpleName();
		par.setSimpleName(varName);
		par.setType(boxedIterType);
		lambda.addParameter(par);

		// Add For Body to Lambda
		CtBlock<?> block = (CtBlock<?>) element.getBody();
		CtReturn<?> ret = factory.Core().createReturn();
		CtLiteral retNull = factory.Core().createLiteral();
		retNull.setValue(null);
		ret.setReturnedExpression(retNull);
		block.addStatement(ret); // adds return null;
		lambda.setBody((CtBlock) block);
		args.add(lambda);

		// Hints
		int c = element.getBody()
				.getElements((e) -> (e instanceof CtWhile || e instanceof CtFor || e instanceof CtInvocation)).size();
		CtFieldRead hint = getHintForCount(factory, c);
		args.add(hint);
		
		args.add(granularityControl.getGranularityControlUnits(element));
		
		
		// Shadow Variables
		ArrayList<CtLocalVariable<?>> lst = new ArrayList<CtLocalVariable<?>>();
		lst.add(((CtLocalVariable<?>) element.getForInit().get(0)));
		List<CtLocalVariable<?>> shadows = createShadowVariables(lambda, bodyVars, factory, lst);
		replaceShadowVariables(lambda, shadows);

		// finalize
		hollowSetting.setArguments(args);
		element.replace(hollowSetting);
		setPermissionSet(hollowSetting, vars);
		for (CtLocalVariable<?> lv : shadows) {
			hollowSetting.insertBefore(lv);
		}
		hollowSetting.getParent().updateAllParentsBelow();

		// Fix permissions on final object
		CtLambda<?> finalLambda = (CtLambda<?>) hollowSetting.getArguments().get(2);
		setPermissionSet(finalLambda, oldBodyVars);
		setPermissionSet(finalLambda.getBody().getLastStatement(), new PermissionSet());
		setPermissionSet(hollowSetting, oldVars);
		permFixer.scan(hollowSetting);
		costFixer.scan(hollowSetting);
		
		finalLambda.putMetadata(PARALLELIZED_KEY, true);
		hollowSetting.putMetadata(PARALLELIZED_KEY, true);
	}

	@SuppressWarnings("rawtypes")
	private CtFieldRead getHintForCount(Factory factory, int c) {
		String tag = "LARGE";
		if (c == 0) tag = "SMALL";
		CtFieldRead hint = (CtFieldRead) factory.Code().createCodeSnippetExpression("aeminium.runtime.Hints." + tag).compile();
		setPermissionSet(hint, new PermissionSet());
		setPermissionSet(hint.getVariable(), new PermissionSet());
		setPermissionSet(hint.getTarget(), new PermissionSet());
		setCost(hint, new CostEstimation());
		return hint;
	}

	private void processInvocation(CtInvocation<?> element) {
		CtElement el = element;
		while (el != null) {
			if (el.getMetadata(PARALLELIZED_KEY) != null) {
				return;
			}
			el = el.getParent();
		}
		futurifyInvocation(element);
		element.putMetadata(PARALLELIZED_KEY, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <E> void futurifyInvocation(CtInvocation<E> element) {
		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);
		
		CostModelFixer cFixer = new CostModelFixer();
		cFixer.scan(element);
		PermissionSet set = getPermissionSet(element);
		PermissionSet parentSet = getPermissionSet(element.getParent());
		
		if (!granularityControl.shouldParallelize(element)) {
			return;
		}

		CtTypeReference<?> t = element.getType();
		CtTypeReference<?> originalType = element.getType();
		if (t.isPrimitive())
			t = t.box();
		boolean needsCast = t.toString().endsWith("[]");
		if (needsCast) {
			t = factory.Type().createReference(Object.class);
		}
		String id = "aeminium_task_" + (counterTasks++);

		// Save block before changing element;
		CtBlock block = element.getParent(CtBlock.class);
		SourcePosition pos = element.getPosition();

		CtMethod<?> meth = (CtMethod<?>) element.getExecutable().getDeclaration();
		CtLocalVariable<?> futureAssign = null;
		CtLambda futureLambda = null;
		CtConstructorCall newFuture = null;

		if (recAux.containsKey(meth)) {
			CtTypeReference futureType = recAux.get(meth).getReference();
			CtTypeReference futureType2 = factory.Type().createReference("aeminium.runtime.futures.FBody");
			CtTypeReference runtimeManager = factory.Type().createReference(RuntimeManager.class);
			CtTypeAccess<?> accRuntimeManager = factory.Code().createTypeAccess(runtimeManager);
			CtExecutableReference createTaskRef = getExecutableReferenceOfMethodByName(runtimeManager, "createTask");

			newFuture = factory.Code().createConstructorCall(futureType);
			newFuture.setArguments(element.getArguments());
			CtInvocation<?> createTask = factory.Code().createInvocation(accRuntimeManager, createTaskRef, newFuture);
			futureAssign = factory.Code().createLocalVariable(futureType2, id, createTask);
			
			setPermissionSet(createTask, new PermissionSet());
			setPermissionSet(newFuture, new PermissionSet());
			setPermissionSet(futureAssign, set.copy());

		} else {
			// Create Assign of Future
			CtTypeReference futureType = factory.Type().createReference("aeminium.runtime.futures.Future");
			futureLambda = factory.Core().createLambda();
			List futureLambdaParams = new ArrayList<CtParameter>();
			CtParameter futureLambdaParam0 = factory.Core().createParameter();
			futureLambdaParam0.setType(factory.Type().createReference("aeminium.runtime.Task"));
			futureLambdaParam0.setSimpleName("aeminium_runtime_tmp");
			futureLambdaParams.add(futureLambdaParam0);
			futureLambda.setParameters(futureLambdaParams);
			// Fill in lambda missing
			setPermissionSet(futureLambda, new PermissionSet());
			newFuture = factory.Code().createConstructorCall(futureType, futureLambda);
			setPermissionSet(newFuture, new PermissionSet());
			
			// If granularity
			CtExpression save = newFuture;
			if (granularityControl.hasGranularityControlExpression(element)) {
				CtConditional conditional = createGranularityConditional(element, granularityControl.getGranularityControlElement(element, element));
				conditional.setThenExpression(factory.Code().createLiteral(null));
				conditional.setElseExpression(newFuture);
				setPermissionSet(conditional, getPermissionSet(element));
				save.updateAllParentsBelow();
				conditional.updateAllParentsBelow();
				save = conditional;
				setPermissionSet(conditional, set.copy());
				setPermissionSet(conditional, new PermissionSet());
			}
			futureAssign = factory.Code().createLocalVariable(futureType, id, save);
			setPermissionSet(futureAssign, set.copy());
			futureAssign.updateAllParentsBelow();
		}
		// Create future.get()

		CtInvocation<E> read = factory.Core().createInvocation();
		CtVariableAccess<?> varRead = factory.Code()
				.createVariableRead(factory.Code().createLocalVariableReference(futureAssign), false);
		read.setTarget(varRead);
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType(element.getType());

		if (futureLambda != null) {
			read.setExecutable(
					(CtExecutableReference<E>) getExecutableReferenceOfMethodByName(futureAssign.getType(), "get"));
		} else {
			read.setExecutable((CtExecutableReference<E>) getExecutableReferenceOfMethodByName(
					factory.Type().createReference("aeminium.runtime.futures.FBody"), "get"));
		}
		if (!isStatement(element))
			read.addTypeCast(originalType);
		read.updateAllParentsBelow();

		CtElement newElement;
		if (isStatement(element)) {
			CtIf ifc = (CtIf) factory.Code().createCodeSnippetStatement("if ( null == null) {} else {}").compile();
			CtBinaryOperator<Boolean> cond = (CtBinaryOperator<Boolean>) ifc.getCondition();
			cond.setLeftHandOperand((CtExpression<?>) CopyCatFactory.basicClone(varRead));
			CtBlock bt = ifc.getThenStatement();
			bt.addStatement((CtStatement) CopyCatFactory.basicClone(element));
			CtBlock be = ifc.getElseStatement();
			be.addStatement(read);
			element.replace(ifc);
			newElement = ifc;
		} else {
			CtConditional conditional = factory.Core().createConditional();
			CtBinaryOperator cond = factory.Core().createBinaryOperator();
			cond.setKind(BinaryOperatorKind.EQ);
			cond.setLeftHandOperand((CtExpression<?>) CopyCatFactory.basicClone(varRead));
			cond.setRightHandOperand(factory.Code().createLiteral(null));
			conditional.setCondition(cond);
			conditional.setThenExpression((CtExpression<?>) CopyCatFactory.basicClone(element));
			conditional.setElseExpression(read);
			element.replace((CtExpression<E>) conditional);
			newElement = conditional;
			setPermissionSet(conditional, new PermissionSet());
			newElement.updateAllParentsBelow();
		}
		setPermissionSet(newElement, set.copy());
		setPermissionSet(read, set.copy());
		setPermissionSet(newElement.getParent(), parentSet);
		setCost(newElement, new CostEstimation());
		//read.getParent(CtBlock.class).updateAllParentsBelow();

		if (futureLambda != null) {
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
				if (needsCast)
					element.addTypeCast(t);
			}

		}

		// Find where to place the Future declaration.
		// Let's start with the current block;
		CtStatement previousStatement = null; // NULL means the beginning.
		List<CtVariableReference<?>> taskDeps = new ArrayList<CtVariableReference<?>>();
		
		if (counterTasks == 61) {
			System.out.println("Trying to insert " +  futureAssign);
			System.out.println("position: " + pos);
		}
		for (CtStatement stmt : block.getStatements()) {
			if (counterTasks == 61) {
				System.out.println("post try: " + stmt.getPosition() + " / " + stmt);
			}
			if (stmt == newElement)
				break;
			if (stmt.getPosition() != null && stmt.getPosition().getLine() >= pos.getLine()) 
				break;

			List<CtVariableAccess> accesses = Query.getElements(element, new Filter<CtVariableAccess>() {
				@Override
				public boolean matches(CtVariableAccess acc) {
					if (acc.getVariable().getDeclaration() instanceof CtLocalVariable) {
						return true;
					}
					return false;
				}
			});
			

			// Hard requirement for local variables to be declared
			if (stmt instanceof CtLocalVariable) {
				for (Permission pi : set)
					if (pi.target == stmt)
						previousStatement = stmt;
				for (CtVariableAccess acc : accesses) {
					if (acc.getVariable().getDeclaration() == stmt) {
						previousStatement = stmt;
					}
				}
				CtLocalVariable v = (CtLocalVariable) stmt;
				if (varRead.getVariable().getDeclaration().equals(v)) {
					previousStatement = stmt;
				}
				for (CtVariableReference<?> ref : taskDeps) {
					if (v.getReference().equals(ref)) {
						previousStatement = stmt;
						continue;
					}
				}
			}
			// Then look for control statements
			PermissionSet stmtSet = getPermissionSet(stmt);

			if (stmtSet.containsControl())
				previousStatement = stmt;
			// Then look for dependencies
			for (Permission pi : set) {
				Permission p2 = stmtSet.getTarget(pi.target);
				if (p2 != null) {
					if (pi.control == true)
						continue;
					if (pi.type == PermissionType.READ && p2.type == PermissionType.READ) {
						continue;
					}
					// This is a dependency.
					CtVariableReference<?> dep = tasks.get(stmt);
					if (dep != null) {
						// Soft dependency
						if (!taskDeps.contains(dep))
							taskDeps.add(dep);
					} else {
						// Hard dependency
						previousStatement = stmt;
					}
				}
			}
		}
		if (counterTasks == 61) {
			System.out.println("Result: " +  previousStatement);
		}
		for (CtVariableReference<?> ref : taskDeps) {
			CtVariableAccess<?> readTask = factory.Code().createVariableRead(ref, false);
			newFuture.addArgument(readTask);
		}

		List<CtLocalVariable<?>> shadows = new ArrayList<CtLocalVariable<?>>();
		if (futureLambda != null) {
			shadows = createShadowVariables(futureLambda, set, factory, null);
			replaceShadowVariables(futureLambda, shadows);
		}

		if (previousStatement == null) {
			block.insertBegin(futureAssign);
			for (CtLocalVariable<?> lv : shadows) {
				block.insertBegin(lv);
			}
			block.updateAllParentsBelow();
		} else {
			previousStatement.updateAllParentsBelow();
			futureAssign.updateAllParentsBelow();
			insertAfter(previousStatement, futureAssign);
			for (CtLocalVariable<?> lv : shadows) {
				insertAfter(previousStatement, lv);
			}
			block = futureAssign.getParent(CtBlock.class);
			permFixer.scan(previousStatement.getParent(CtBlock.class).getParent());
			permFixer.scan(previousStatement.getParent());
		}
		setCost(futureAssign, new CostEstimation());
		tasks.put(read, futureAssign.getReference());
		
		futureAssign.putMetadata(PARALLELIZED_KEY, true);
		read.putMetadata(PARALLELIZED_KEY, true);

		// Fix broken blocks
		block.getParent().updateAllParentsBelow();
		read.getParent().updateAllParentsBelow();
		permFixer.scan(block.getParent());
		//permFixer.scan(read.getParent());
	}

	@SuppressWarnings("rawtypes")
	private CtExecutableReference getExecutableReferenceOfMethodByName(CtTypeReference<?> t, String name) {
		CtExecutableReference<?> refGet = null;
		do {
			if (t.getDeclaredExecutables() != null) {
				for (CtExecutableReference<?> r : t.getDeclaredExecutables()) {
					if (r.getSimpleName().equals(name))
						refGet = r;

				}
			}
			t = t.getSuperclass();
		} while (refGet == null && t != null);
		if (refGet == null) {
			throw new RuntimeException("No method " + name + " in type " + t);
		}
		return refGet;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void replaceShadowVariables(CtLambda futureLambda, List<CtLocalVariable<?>> shadows) {
		// Replace variable accesses by shadows
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
	}

	private boolean isParent(CtElement parent, CtElement child) {
		CtElement p = child.getParent();
		while (p != null) {
			if (p == parent)
				return true;
			p = p.getParent();
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<CtLocalVariable<?>> createShadowVariables(CtLambda futureLambda, PermissionSet set, Factory factory,
			List<CtLocalVariable<?>> exceptions) {
		List<CtLocalVariable<?>> shadows = new ArrayList<CtLocalVariable<?>>();
		List<CtLocalVariable<?>> originals = new ArrayList<CtLocalVariable<?>>();
		Query.getElements(futureLambda, new Filter<CtVariableAccess>() {

			@Override
			public boolean matches(CtVariableAccess acc) {
				if (acc.getVariable() == null)
					return false;
				CtElement decl = null;
				try {
					decl = acc.getVariable().getDeclaration();
				} catch (Exception e) {
					return false;
				}
				if (decl instanceof CtLocalVariable) {
					CtLocalVariable lv = (CtLocalVariable) decl;

					if (isParent(futureLambda, lv))
						return false;

					if (exceptions != null && exceptions.contains(lv))
						return false;

					if (originals.contains(lv)) {
						return false;
					}

					originals.add(lv);
					CtLocalVariable ass = factory.Core().createLocalVariable();
					CtVariableRead<?> r = factory.Core().createVariableRead();
					r.setVariable(lv.getReference());
					ass.setDefaultExpression(r);
					ass.setSimpleName(lv.getSimpleName() + "_aeminium_" + (counterTasks - 1));
					ass.addModifier(ModifierKind.FINAL);
					ass.setType(lv.getType());
					shadows.add(ass);
					setPermissionSet(r, new PermissionSet());
					setPermissionSet(ass, new PermissionSet());
				}

				return false;
			}

		});
		/*
		 * List<CtLocalVariable<?>> shadows = new
		 * ArrayList<CtLocalVariable<?>>(); for (Permission pi : set) { if
		 * (pi.target instanceof CtLocalVariable) { CtLocalVariable lv =
		 * (CtLocalVariable<?>) pi.target; if (exceptions != null &&
		 * exceptions.contains(lv)) continue; if (shadows.contains(lv))
		 * continue; CtLocalVariable ass = factory.Core().createLocalVariable();
		 * CtVariableRead<?> r = factory.Core().createVariableRead();
		 * r.setVariable(lv.getReference()); ass.setDefaultExpression(r);
		 * ass.setSimpleName(lv.getSimpleName() + "_aeminium_" + (counterTasks -
		 * 1)); ass.addModifier(ModifierKind.FINAL); ass.setType(lv.getType());
		 * shadows.add(ass); setPermissionSet(r, new PermissionSet());
		 * setPermissionSet(ass, new PermissionSet()); } }
		 */
		return shadows;
	}

	public <E> boolean shouldFuturify(CtInvocation<E> element) {
		if (Safety.isSafe(element.getExecutable().getDeclaration())) {
			return true;
		}
		if (element.getExecutable().toString().startsWith("java.util.Random")) {
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
	
	@SuppressWarnings("unchecked")
	private CtIf createGranularityIf(CtElement element, CtExpression<?> decision) {
		CtIf i = FactoryReference.getFactory().Core().createIf();
		i.setCondition((CtExpression<Boolean>) decision);
		return i;
	}
	
	@SuppressWarnings("unchecked")
	private CtConditional<?> createGranularityConditional(CtElement element, CtExpression<?> cond) {
		CtConditional<?> i = FactoryReference.getFactory().Core().createConditional();
		i.setCondition((CtExpression<Boolean>) cond);
		return i;
	}

	private boolean isStatement(CtInvocation<?> element) {
		CtElement p = element.getParent();
		if (p instanceof CtStatementList)
			return true;
		if (p instanceof CtBlock)
			return true;
		if (p instanceof CtIf && ((CtIf) p).getCondition() != element)
			return true;
		return false;
	}

	public void copyInto(CtElement from, CtElement to) {
		copyCost(from, to);
		copyPermissionSet(from, to);
	}

	public void setCost(CtElement el, CostEstimation ce) {
		el.putMetadata(CostEstimation.COST_MODEL_KEY, ce);
	}

	public void copyCost(CtElement from, CtElement to) {
		setCost(to, getCost(from));
	}

	public CostEstimation getCost(CtElement e) {
		for (int i = 0; i < 2; i++) {
			CostEstimation vars = (CostEstimation) e.getMetadata(CostEstimation.COST_MODEL_KEY);
			if (vars != null)
				return vars;
			permFixer.scan(e.getParent());
		}
		throw new RuntimeException("Missing cost database for " + e.hashCode() + " / " + e + " / " + e.getClass());
	}

	public PermissionSet getPermissionSet(CtElement element) {
		for (int i = 0; i < 2; i++) {
			PermissionSet vars = (PermissionSet) element.getMetadata(PermissionSet.PERMISSION_MODEL_KEY);
			if (vars != null)
				return vars;
			try {
				permFixer.scan(element.getParent());
			} catch (Exception e) {
				return new PermissionSet(); // TODO: understand this
			}
		}
		throw new RuntimeException(
				"Missing permission database for " + element.hashCode() + " / " + element + " / " + element.getClass());
	}

	protected void setPermissionSet(CtElement e, PermissionSet s) {
		e.putMetadata(PermissionSet.PERMISSION_MODEL_KEY, s);
	}

	protected void copyPermissionSet(CtElement e, CtElement to) {
		setPermissionSet(to, getPermissionSet(e));
	}

	protected boolean hasPermissionSet(CtElement el) {
		return el.getMetadata(PermissionSet.PERMISSION_MODEL_KEY) != null;
	}

}
