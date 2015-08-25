package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
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
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.Query;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;
import aeminium.jparcompiler.processing.utils.CopyCatFactory;
import aeminium.jparcompiler.processing.utils.Safety;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.NoVisit;

public class TaskCreationProcessor extends AbstractProcessor<CtElement> {

	HashMap<CtElement, PermissionSet> database;
	HashMap<CtElement, CtVariableReference<?>> tasks = new HashMap<CtElement, CtVariableReference<?>>();
	public static HashMap<CtMethod<?>, CtClass<?>> recAux = new HashMap<CtMethod<?>, CtClass<?>>();
	PermissionSetFixer fixer;
	int counterTasks;

	@Override
	public void init() {
		super.init();
		database = AccessPermissionsProcessor.database;
		fixer = new PermissionSetFixer(database);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void process(CtElement element) {
		if (Safety.isSafe(element))
			return;
		if (element.getParent(CtClass.class).getAnnotation(NoVisit.class) != null)
			return;

		Factory factory = element.getFactory();
		if (element instanceof CtMethod) {
			CtMethod<?> m = (CtMethod<?>) element;
			if (m.getSimpleName().equals("main")
					&& m.hasModifier(ModifierKind.STATIC)
					&& m.hasModifier(ModifierKind.PUBLIC)) {
				// Surround main method with inits and shutdowns.
				CtInvocation<?> c = factory
						.Code()
						.createCodeSnippetStatement(
								"aeminium.runtime.futures.RuntimeManager.init();")
						.compile();
				m.getBody().insertBegin(c);
				m.getBody().updateAllParentsBelow();
				setPermissionSet(c, new PermissionSet());

				CtInvocation<?> c2 = factory
						.Code()
						.createCodeSnippetStatement(
								"aeminium.runtime.futures.RuntimeManager.shutdown();")
						.compile();
				m.getBody().insertEnd(c2);
				m.getBody().updateAllParentsBelow();
				setPermissionSet(c2, new PermissionSet());
			} else {
				if (!recAux.containsKey(m)) { // Not recursive
					futurifyMethod(element, factory, m);
				}
			}

		}
		
		if (element instanceof CtConstructorCall) {
			CtConstructorCall<?> call = (CtConstructorCall<?>) element;
			CtTypeReference<?> randomT = factory.Type().createReference("java.util.Random");
			if (call.getType().equals(randomT) && call.getExecutable().getDeclaringType().equals(randomT)) {
				CtTypeReference randomTS = factory.Type().createReference(ThreadLocalRandom.class);
				CtExecutableReference ref = randomTS.getDeclaredExecutables().stream().filter(e -> e.getSimpleName().equals("current")).iterator().next();
				CtTypeAccess<?> randomTSAccess = factory.Core()
						.createTypeAccess();
				randomTSAccess.setType(randomTS);
				CtExpression current = factory.Code().createInvocation(randomTSAccess, ref, new ArrayList<CtExpression<?>>());
				call.replace(current);
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
			processFor((CtFor) element);
		}
	}

	@SuppressWarnings("unchecked")
	private void futurifyMethod(CtElement element, Factory factory,
			CtMethod<?> m) {
		// create if parallelize
		Permission perm = new Permission(PermissionType.WRITE, element);
		perm.control = true;
		PermissionSet ps = new PermissionSet();
		ps.add(perm);

		CtIf i = factory.Core().createIf();
		CtExpression<?> inv = factory
				.Code()
				.createCodeSnippetExpression(
						"aeminium.runtime.futures.RuntimeManager.shouldSeq()")
				.compile();
		setPermissionSet(inv, ps.copy());
		i.setCondition((CtExpression<Boolean>) inv);

		CtClass<?> cl = m.getParent(CtClass.class);
		CtExecutableReference<?> ref = factory.Executable().createReference(
				cl.getMethodsByName(
						SeqMethodProcessor.SEQ_PREFIX + m.getSimpleName()).get(
						0));

		ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		for (CtParameter<?> p : m.getParameters()) {
			CtExpression<?> arg = factory.Code().createVariableRead(
					factory.Method().createParameterReference(p),
					m.hasModifier(ModifierKind.STATIC));
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

	private void processFor(CtFor element) {
		// First, check conditions for parallelization.
		if (element.getForInit().size() != 1)
			return;
		if (element.getForUpdate().size() != 1)
			return;

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
		if (!postinc)
			return;

		// Get ceiling
		CtExpression<Boolean> cond = element.getExpression();
		if (cond instanceof CtBinaryOperator) {
			CtBinaryOperator<Boolean> comp = (CtBinaryOperator<Boolean>) cond;
			if (comp.getKind() != BinaryOperatorKind.LT)
				return;
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
		if (end == null)
			return;
		// Now we know its postinc and we have the bottom and ceiling.

		PermissionSet oldVars = getPermissionSet(element.getBody());

		// We have to remove the indexed writes and reads that are parallel
		element.getElements((e) -> {
			if (e instanceof CtArrayAccess) {
				CtElement el = e;
				while (el != element) {
					boolean deleted = getPermissionSet(el).removeIf(
							(p) -> p.index != null && p.index == v);
					if (!deleted) {
						break;
					}
					el = el.getParent();
				}
			}
			return false;
		});
		// Next, we evaluate for write permissions inside the cycle.
		PermissionSet vars = getPermissionSet(element.getBody());
		int countWrites = vars.count(PermissionType.WRITE);
		if (countWrites == 0) {
			generateContinuousFor(element, st, end, type, oldVars);
		} else if (countWrites == 1) {
			generateContinuousForReduce(element, st, end, type, oldVars);
		} else {
			System.out
					.println("Not generating parallel for because of permissions. "
							+ element.getPosition());
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void generateContinuousForReduce(CtFor element, CtExpression<?> st,
			CtExpression<?> end, CtTypeReference<?> iteratorType,
			PermissionSet oldVars) {
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
								incrementReducer = ass
										.getFactory()
										.Code()
										.createCodeSnippetExpression(
												"aeminium.runtime.futures.codegen.ForHelper.intSum")
										.compile();
							if (ass.getType().getSimpleName().equals("long"))
								incrementReducer = ass
										.getFactory()
										.Code()
										.createCodeSnippetExpression(
												"aeminium.runtime.futures.codegen.ForHelper.longSum")
										.compile();
							if (ass.getType().getSimpleName().equals("float"))
								incrementReducer = ass
										.getFactory()
										.Code()
										.createCodeSnippetExpression(
												"aeminium.runtime.futures.codegen.ForHelper.floatSum")
										.compile();
							if (ass.getType().getSimpleName().equals("double"))
								incrementReducer = ass
										.getFactory()
										.Code()
										.createCodeSnippetExpression(
												"aeminium.runtime.futures.codegen.ForHelper.doubleSum")
										.compile();
							setPermissionSet(incrementReducer,
									new PermissionSet());
						}
					}
				}
			}
		}
		if (incrementReducer == null) {
			System.out.println("Not generating because of missing +=. "
					+ element.getPosition());
			return;
		}

		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);
		CtTypeReference<?> returnType = stToChange.getType();
		CtTypeReference<?> returnTypeBoxed = returnType.box();
		CtTypeReference<?> boxedIterType = iteratorType.box();

		// Backup assign
		CtOperatorAssignment<?, ?> update = (CtOperatorAssignment<?, ?>) CopyCatFactory
				.clone(stToChange);

		String id = "aeminium_for_tmp_" + counterTasks++;
		String idRet = "aeminium_for_ret_" + counterTasks++;
		CtLocalVariable<?> hollowSetting = factory
				.Code()
				.createCodeSnippetStatement(
						"aeminium.runtime.futures.HollowFuture<"
								+ returnTypeBoxed
								+ "> "
								+ id
								+ " = aeminium.runtime.futures.codegen.ForHelper.forContinuous"
								+ boxedIterType.getSimpleName()
								+ "Reduce1(0,1, ("
								+ boxedIterType
								+ " i) -> { return null; }, null, aeminium.runtime.Hints.LARGE)")
				.compile();
		CtInvocation<?> forHelper = (CtInvocation<?>) hollowSetting
				.getDefaultExpression();
		ArrayList<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		st.addTypeCast(iteratorType);
		end.addTypeCast(iteratorType);
		args.add(st);
		args.add(end);
		CtLambda<?> lambda = (CtLambda<?>) forHelper.getArguments().get(2);
		CtBlock<?> block = (CtBlock<?>) element.getBody();

		// VarName
		String varName = ((CtLocalVariable<?>) element.getForInit().get(0))
				.getSimpleName();
		lambda.getParameters().get(0).setSimpleName(varName);

		CtLocalVariable<?> varDecl = factory.Core().createLocalVariable();
		varDecl.setSimpleName(idRet);
		varDecl.setType((CtTypeReference) returnType);
		varDecl.setAssignment((CtExpression) stToChange.getAssignment());
		stToChange.replace(varDecl);

		CtReturn<?> ret = factory.Core().createReturn();
		CtVariableRead varRead = (CtVariableRead) factory.Code()
				.createVariableRead(varDecl.getReference(), false);
		ret.setReturnedExpression(varRead);
		block.addStatement(ret);

		lambda.setBody((CtBlock) block);
		args.add(lambda);
		args.add(incrementReducer);

		// Hints
		int c = element
				.getBody()
				.getElements(
						(e) -> (e instanceof CtWhile || e instanceof CtFor || e instanceof CtInvocation))
				.size();
		CtTypeReference hintType = factory.Type().createReference(
				"aeminium.runtime.Hints");
		CtTypeAccess hintTypeAccess = factory.Core().createTypeAccess();
		hintTypeAccess.setType(hintType);
		CtFieldRead hint = factory.Core().createFieldRead();
		CtVariableReference hintRef = factory.Core().createFieldReference();
		hintRef.setType(hintType);
		if (c == 0) {
			hintRef.setSimpleName("SMALL");
		} else {
			hintRef.setSimpleName("LARGE");
		}
		hint.setTarget(hintTypeAccess);
		hint.setVariable(hintRef);
		setPermissionSet(hintTypeAccess, new PermissionSet());
		setPermissionSet(hint, new PermissionSet());
		args.add(hint);

		forHelper.setArguments(args);
		element.replace(hollowSetting);
		setPermissionSet(lambda, new PermissionSet());
		setPermissionSet(hollowSetting, vars);

		// future.get()
		CtInvocation<?> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(
				hollowSetting.getReference(), false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType((CtTypeReference) returnType);
		read.setExecutable(getExecutableReferenceOfMethodByName(
				hollowSetting.getType(), "get"));
		update.setAssignment((CtExpression) read);
		setPermissionSet(read, oldVars);
		setPermissionSet(update, oldVars);
		insertAfter(hollowSetting, update);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void generateContinuousFor(CtFor element, CtExpression<?> st,
			CtExpression<?> end, CtTypeReference<?> iteratorType,
			PermissionSet oldBodyVars) {
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

		// factory.Code().createCodeSnippetStatement("aeminium.runtime.futures.codegen.ForHelper.forContinuousInt(0,1, (Integer i) -> { return null; })").compile();
		CtInvocation hollowSetting = factory.Core().createInvocation();
		CtTypeReference futureType = factory.Type().createReference(
				"aeminium.runtime.futures.codegen.ForHelper");
		CtExecutableReference<?> methodReferenceExpression = futureType
				.getDeclaredExecutables()
				.stream()
				.filter((e) -> e.getSimpleName().equals(
						"forContinuous" + boxedIterType.getSimpleName()))
				.iterator().next();

		CtTypeAccess staticReference = factory.Core().createTypeAccess();
		staticReference.setType(futureType);
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
		CtLocalVariable<?> init = (CtLocalVariable<?>) element.getForInit().get(0);
		String varName = init.getSimpleName();
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
		
		lambda.getElements((CtVariableRead v) -> {			
			if (v.getVariable() instanceof CtLocalVariableReference) {
				if (v.getVariable().getDeclaration() == init) {
					CtVariableRead n = factory.Core().createVariableRead();
					n.setVariable(par.getReference());
					v.replace(n);
					setPermissionSet(n, getPermissionSet(v));
				}
			}
			return true;
		});
		
		args.add(lambda);

		// Hints
		int c = element
				.getBody()
				.getElements(
						(e) -> (e instanceof CtWhile || e instanceof CtFor || e instanceof CtInvocation))
				.size();
		CtTypeReference hintType = factory.Type().createReference(
				"aeminium.runtime.Hints");
		CtTypeAccess hintTypeAccess = factory.Core().createTypeAccess();
		hintTypeAccess.setType(hintType);
		CtFieldRead hint = factory.Core().createFieldRead();
		CtVariableReference hintRef = factory.Core().createFieldReference();
		hintRef.setType(hintType);
		if (c == 0) {
			hintRef.setSimpleName("SMALL");
		} else {
			hintRef.setSimpleName("LARGE");
		}
		hint.setTarget(hintTypeAccess);
		hint.setVariable(hintRef);
		setPermissionSet(hintTypeAccess, new PermissionSet());
		setPermissionSet(hint, new PermissionSet());
		args.add(hint);

		// Shadow Variables
		ArrayList<CtVariable<?>> lst = new ArrayList<CtVariable<?>>();
		lst.add(((CtLocalVariable<?>) element.getForInit().get(0)));
		List<CtLocalVariable<?>> shadows = createShadowVariables(element.getBody(), bodyVars,
				factory, lst);
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
		CtLambda<?> finalLambda = (CtLambda<?>) hollowSetting.getArguments()
				.get(2);
		setPermissionSet(finalLambda, oldBodyVars);
		setPermissionSet(finalLambda.getBody().getLastStatement(),
				new PermissionSet());
		setPermissionSet(hollowSetting, oldVars);
		fixer.scan(hollowSetting);
	}

	private void processInvocation(CtInvocation<?> element) {
		if (element.getExecutable().getDeclaringType().getQualifiedName()
				.startsWith("java.lang.Math")) {
			return;
		}
		futurifyInvocation(element);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <E> void futurifyInvocation(CtInvocation<E> element) {
		PermissionSet set = getPermissionSet(element);
		Factory factory = element.getFactory();
		factory.getEnvironment().setComplianceLevel(8);

		if (shouldFuturify(element))
			return;

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

		CtMethod<?> meth = (CtMethod<?>) element.getExecutable()
				.getDeclaration();
		CtLocalVariable<?> futureAssign = null;
		CtLambda futureLambda = null;
		CtConstructorCall newFuture = null;

		if (recAux.containsKey(meth)) {
			CtTypeReference futureType = recAux.get(meth).getReference();
			CtTypeReference futureType2 = factory.Type().createReference(
					"aeminium.runtime.futures.FBody");
			CtTypeReference runtimeManager = factory.Type().createReference(
					RuntimeManager.class);
			CtTypeAccess<?> accRuntimeManager = factory.Core()
					.createTypeAccess();
			accRuntimeManager.setType(runtimeManager);
			CtExecutableReference createTaskRef = getExecutableReferenceOfMethodByName(
					runtimeManager, "createTask");

			newFuture = factory.Core().createConstructorCall();
			newFuture.setType(futureType);
			newFuture.setArguments(element.getArguments());
			CtInvocation<?> createTask = factory.Code().createInvocation(
					accRuntimeManager, createTaskRef, newFuture);
			futureAssign = factory.Code().createLocalVariable(futureType2, id,
					createTask);
			setPermissionSet(createTask, new PermissionSet());
			setPermissionSet(newFuture, new PermissionSet());
			setPermissionSet(futureAssign, set.copy());

		} else {
			// Create Assign of Future
			CtTypeReference futureType = factory.Type().createReference(
					"aeminium.runtime.futures.Future");
			futureLambda = factory.Core().createLambda();
			List futureLambdaParams = new ArrayList<CtParameter>();
			CtParameter futureLambdaParam0 = factory.Core().createParameter();
			futureLambdaParam0.setType(factory.Type().createReference(
					"aeminium.runtime.Task"));
			futureLambdaParam0.setSimpleName("aeminium_runtime_tmp");
			futureLambdaParams.add(futureLambdaParam0);
			futureLambda.setParameters(futureLambdaParams);
			// Fill in lambda missing
			setPermissionSet(futureLambda, new PermissionSet());
			newFuture = factory.Core().createConstructorCall();
			setPermissionSet(newFuture, new PermissionSet());
			newFuture.setType(futureType);
			newFuture.addArgument(futureLambda);
			futureAssign = factory.Code().createLocalVariable(futureType, id,
					newFuture);
			setPermissionSet(futureAssign, set.copy());
		}
		// Create future.get()
		CtInvocation<E> read = factory.Core().createInvocation();
		read.setTarget(factory.Code().createVariableRead(
				factory.Code().createLocalVariableReference(futureAssign),
				false));
		read.setArguments(new ArrayList<CtExpression<?>>());
		read.setType(element.getType());

		if (futureLambda != null) {
			read.setExecutable((CtExecutableReference<E>) getExecutableReferenceOfMethodByName(
					futureAssign.getType(), "get"));
		} else {
			read.setExecutable((CtExecutableReference<E>) getExecutableReferenceOfMethodByName(
					factory.Type().createReference(
							"aeminium.runtime.futures.FBody"), "get"));
		}
		if (!(element.getParent() instanceof CtBlock))
			read.addTypeCast(originalType);
		setPermissionSet(read, set.copy());
		element.replace((CtExpression<E>) read);
		read.getParent(CtBlock.class).updateAllParentsBelow();

		if (futureLambda != null) {
			// Filling in Lambda after using element in replace.

			if (t.getSimpleName().equals("Void")) {
				CtReturn<?> nullret = factory.Core().createReturn();
				nullret.setReturnedExpression(factory.Code()
						.createLiteral(null));
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

		for (CtStatement stmt : block.getStatements()) {
			if (stmt == read)
				break;
			if (stmt.getPosition() != null
					&& stmt.getPosition().getLine() >= pos.getLine())
				break;

			// Hard requirement for local variables to be declared
			if (stmt instanceof CtLocalVariable) {
				for (Permission pi : set)
					if (pi.target == stmt)
						previousStatement = stmt;
				if (findVariableAccessInElement(read, (CtLocalVariable) stmt) > 0) {
					previousStatement = stmt;
				}
				if (futureLambda != null) {
					if (findVariableAccessInElement(futureLambda, (CtLocalVariable) stmt) > 0) {
						previousStatement = stmt;
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
					if (pi.type == PermissionType.READ
							&& p2.type == PermissionType.READ) {
						continue;
					}
					// This is a dependency.
					CtVariableReference<?> dep = tasks.get(stmt);
					if (dep != null) {
						// Task dependency
						taskDeps.add(dep);
					} else {
						// Soft dependency
						previousStatement = stmt;
					}
				}
			}
		}
		for (CtVariableReference<?> ref : taskDeps) {
			CtVariableAccess<?> readTask = factory.Code().createVariableRead(
					ref, false);
			newFuture.addArgument(readTask);
		}

		List<CtLocalVariable<?>> shadows = new ArrayList<CtLocalVariable<?>>();
		if (futureLambda != null) {
			shadows = createShadowVariables(element, set, factory, null);
			replaceShadowVariables(futureLambda, shadows);
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
			fixer.scan(previousStatement.getParent());
		}
		tasks.put(read, futureAssign.getReference());

		// Fix broken blocks
		fixer.scan(block.getParent());
		fixer.scan(read.getParent(CtBlock.class).getParent());
	}

	@SuppressWarnings("rawtypes")
	private CtExecutableReference getExecutableReferenceOfMethodByName(
			CtTypeReference<?> t, String name) {
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
	private void replaceShadowVariables(CtLambda futureLambda,
			List<CtLocalVariable<?>> shadows) {
		// Replace variable accesses by shadows
		if (shadows.size() > 0) {
			futureLambda.getElements((CtVariableAccess va) -> {
				for (CtLocalVariable lv : shadows) {
					String[] parts = lv.getSimpleName().split("_");
					if (parts[0].equals(va.getVariable().getSimpleName())) {
						va.setVariable(lv.getReference());
					}
				}
				return true;
			});
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private CtLocalVariable createShadowCopy(CtVariable lv, CtElement el, Factory factory, List<CtLocalVariable<?>> shadows, List<CtVariable<?>> exceptions) {
		if (exceptions != null) {
			for (CtVariable<?> ex : exceptions) {
				if (compareRefs(ex.getReference(), lv.getReference())) return null;
			}
		}
		for (CtLocalVariable<?> sh : shadows) {
			if (compareRefs(sh.getReference(), lv.getReference())) return null;
		}
		if (lv.getReference() instanceof CtFieldReference) return null;
		
		
		// Check if access does not exists in lambda
		if (findVariableAccessInElement(el, lv) == 0) {
			return null;
		}
		
		if (el.getElements((CtParameter p) -> (p.getSimpleName().equals(lv.getReference().toString()))).size() > 0) return null;
		
		CtLocalVariable ass = factory.Core().createLocalVariable();
		CtVariableRead<?> r = factory.Core().createVariableRead();
		r.setVariable(lv.getReference());
		ass.setDefaultExpression(r);
		ass.setSimpleName(lv.getSimpleName() + "_aeminium_"
				+ (counterTasks - 1));
		ass.addModifier(ModifierKind.FINAL);
		ass.setType(lv.getType());
		setPermissionSet(r, new PermissionSet());
		setPermissionSet(ass, new PermissionSet());
		return ass;
	}

	@SuppressWarnings({ "rawtypes" })
	private List<CtLocalVariable<?>> createShadowVariables(CtElement el, PermissionSet set,
			Factory factory, List<CtVariable<?>> exs) {
		
		if (exs == null) exs = new ArrayList<CtVariable<?>>();
		final List<CtVariable<?>> exceptions = exs;
		List<CtLocalVariable<?>> shadows = new ArrayList<CtLocalVariable<?>>();
		for (Permission pi : set) {
			if (pi.index != null) {
				CtLocalVariable c = createShadowCopy(pi.index, el, factory, shadows, exceptions);
				if (c != null) {
					exceptions.add(pi.index);
					shadows.add(c);
				}
			}
			if (pi.target instanceof CtVariable) {
				CtLocalVariable c = createShadowCopy((CtVariable<?>) pi.target, el, factory, shadows, exceptions);
				if (c != null) {
					exceptions.add((CtVariable<?>) pi.target);
					shadows.add(c);
				}
			}
			if (pi.target instanceof CtArrayRead) {
				CtArrayRead r = (CtArrayRead) pi.target;
				if (r.getIndexExpression() instanceof CtVariableRead) {
					CtVariableRead vr = (CtVariableRead) r.getIndexExpression();
					if (vr.getVariable() instanceof CtVariableReference){
						CtVariableReference ref = (CtVariableReference) vr.getVariable();
						if (ref.getDeclaration() != null) {
							CtLocalVariable c = createShadowCopy(ref.getDeclaration(), el, factory, shadows, exceptions);
							if (c != null) {
								exceptions.add(ref.getDeclaration());
								shadows.add(c);
							}
						}
					}
				}
			}
		}
		
		return shadows;
	}

	private int findVariableAccessInElement(CtElement el,
			CtVariable<?> variableRef) {
		return el.getElements( e -> {
			if (e instanceof CtVariableAccess) {
				CtVariableAccess<?> ass = (CtVariableAccess<?>) e;
				return compareRefs(ass.getVariable(), variableRef.getReference());
			} else {
				return false;
			}
		}).size();
	}
	
	@SuppressWarnings("rawtypes")
	private boolean compareRefs(CtVariableReference a, CtVariableReference b) {
		if (a == null || b == null) return false;
		try {
			return a.getDeclaration() == b.getDeclaration();
		} catch (Exception ex) {
			return false;
		}
	}

	private <E> boolean shouldFuturify(CtInvocation<E> element) {
		if (Safety.isSafe(element.getExecutable().getDeclaration())) {
			return true;
		}
		if (element.getExecutable().toString().startsWith("java.util.Random")) {
			return true;
		}
		if (element.getExecutable().toString()
				.equals("java.io.PrintStream.println")) {
			return true;
		}
		if (element.getExecutable().toString()
				.equals("java.util.Arrays.asList")) {
			return true;
		}
		if (element.getExecutable().toString().startsWith("java.util.List")) {
			return true;
		}
		if (element.getExecutable().toString()
				.startsWith("java.util.ArrayList")) {
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
			throw new RuntimeException("Cannot insert after this element: "
					+ a1);
		}
		if (getPermissionSet(a1) == null) {
			throw new RuntimeException("Statement " + a1
					+ " does not have a PermissionSet");
		}
		if (getPermissionSet(a2) == null) {
			throw new RuntimeException("Statement " + a2
					+ " does not have a PermissionSet");
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
			if (vars != null)
				return vars;
			fixer.scan(element.getParent());
		}
		throw new RuntimeException("Missing database for " + element.hashCode()
				+ " / " + element + " / " + element.getClass());
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
