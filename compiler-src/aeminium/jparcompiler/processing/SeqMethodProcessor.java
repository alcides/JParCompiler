package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import aeminium.jparcompiler.model.CostEstimation;
import aeminium.jparcompiler.processing.utils.CopyCatFactory;
import aeminium.jparcompiler.processing.utils.Safety;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.NoVisit;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.Filter;

public class SeqMethodProcessor extends AbstractProcessor<CtMethod<?>> {
	
	public static String SEQ_PREFIX = "aeminium_seq_";
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void process(CtMethod<?> m) {
		if (m.getParent(CtClass.class).getAnnotation(NoVisit.class) != null ) return;
		if (!Safety.isSafe(m)) {
			CtMethod<?> seq = (CtMethod<?>) CopyCatFactory.clone(m);
			seq.setSimpleName(SEQ_PREFIX + seq.getSimpleName());
			CtClass<?> cl = (CtClass<?>) m.getParent();
			cl.addMethod(seq);
			seq.getElements((CtInvocation i) -> {
				if (i.getExecutable().getDeclaration() != null && i.getExecutable().getDeclaration().equals(m)) {
					i.setExecutable(seq.getReference());
				}
				return false; 
			});
			
			if (isRecursive(m)) { 
				futurifyRecursiveMethod(m.getFactory(), m);
				return;
			} 
		}
	}

	private boolean isRecursive(CtMethod<?> m) {
		return m.getElements((e) -> {
			if (e instanceof CtInvocation) {
				CtInvocation<?> inv = (CtInvocation<?>) e;
				if (inv.getExecutable().getDeclaration() == m) 
					return true;
			}
			return false;
		}).size() > 0;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void futurifyRecursiveMethod(Factory factory,
			CtMethod<?> m) {
		CtClass container = (CtClass<?>) m.getParent();
		CtClass cl = factory.Class().create(container, "Future_" + m.getSimpleName());
		if (m.hasModifier(ModifierKind.STATIC)) {
			cl.addModifier(ModifierKind.STATIC);
		}
		CtTypeReference<?> retType = m.getType();
		if (retType.isPrimitive()) retType = retType.box();
		CtTypeReference<?> fbodyType = factory.Type().createTypeParameterReference("aeminium.runtime.futures.FBody<" + retType + ">");
		cl.setSuperclass(fbodyType);
		
		CtConstructor<?> cons = factory.Constructor().create(cl, new TreeSet<ModifierKind>(), m.getParameters(), m.getThrownTypes());
		cons.addModifier(ModifierKind.PUBLIC);
		CtBlock block = factory.Core().createBlock();
		
		List<CtExpression<?>> args = new ArrayList<CtExpression<?>>();
		HashMap<CtParameter, CtField> mapping = new HashMap<>();
		for (CtParameter p : m.getParameters()) {
			CtField<?> f = factory.Core().createField();
			f.setSimpleName(p.getSimpleName() + "_ae");
			f.setType(p.getType());
			cl.addField(f);
			mapping.put(p, f);
			
			CtAssignment ass = factory.Core().createAssignment();
			CtFieldAccess fass = factory.Core().createFieldWrite();
			fass.setVariable(f.getReference());
			ass.setAssigned(fass);
			CtVariableRead pass = factory.Core().createVariableRead();
			pass.setVariable(p.getReference());
			ass.setAssignment(pass);
			block.addStatement(ass);
			
			CtFieldAccess arg = factory.Core().createFieldRead();
			arg.setVariable(f.getReference());
			args.add(arg);
		}
		
		CostEstimation est = (CostEstimation) m.getMetadata(CostEstimation.COST_MODEL_KEY);
		CtExpression memoryModel = visitMemoryModel(m, est, 5000);
		
		// Execute
		List<CtParameter<?>> exePars = new ArrayList<CtParameter<?>>();
		Set<CtTypeReference<? extends Throwable>> exeThrows = new TreeSet<CtTypeReference<? extends Throwable>>();
		exeThrows.add(factory.Type().createReference("java.lang.Exception"));
		CtMethod exe = factory.Method().create(cl, new TreeSet<ModifierKind>(), factory.Type().VOID_PRIMITIVE, "execute", exePars, exeThrows);
		exe.addModifier(ModifierKind.PUBLIC);
		CtParameter<?> aeRuntime = factory.Executable().createParameter(exe, factory.Type().createReference("aeminium.runtime.Runtime"), "aeRuntime");
		CtParameter<?> aeTask = factory.Executable().createParameter(exe, factory.Type().createReference("aeminium.runtime.Task"), "aeTask");
		
		List<CtExpression<?>> targs = new ArrayList<CtExpression<?>>();
		CtVariableAccess taskRead = factory.Core().createVariableRead();
		taskRead.setVariable(aeTask.getReference());
		targs.add(taskRead);
		CtVariableAccess rtRead = factory.Core().createVariableRead();
		rtRead.setVariable(aeRuntime.getReference());
		CtTypeReference rtReference = factory.Type().createReference("aeminium.runtime.Runtime");
		CtExecutableReference parallelizeRef = rtReference.getDeclaredExecutables().stream().filter(e -> e.getSimpleName().equals("parallelize")).iterator().next();
		CtExpression parallelize = factory.Code().createInvocation(rtRead, parallelizeRef, targs);
		
		CtInvocation parVersion = factory.Code().createInvocation(null, m.getReference(), args);
		
		CtMethod seqMethod = (CtMethod) container.getMethodsByName("aeminium_seq_" + m.getSimpleName()).get(0);
		CtInvocation seqVersion = factory.Code().createInvocation(null, seqMethod.getReference(), args);
		
		if (System.getenv("MEMORYMODEL") != null) {
			// Replace parameters by fields;
			
			memoryModel = (CtExpression) CopyCatFactory.clone(memoryModel);
			memoryModel.getElements(new Filter<CtVariableRead>() {

				@Override
				public boolean matches(CtVariableRead element) {
					CtElement parent = element.getParent();
					CtElement possiblePar = element.getVariable().getDeclaration();
					if (possiblePar instanceof CtParameter) {
						if (mapping.containsKey(possiblePar)) {
							CtFieldRead fr = factory.Core().createFieldRead();
							fr.setVariable(mapping.get(possiblePar).getReference());
							element.replace(fr);
							parent.updateAllParentsBelow();
						}
					}
					return false;
				}
				
			});
			
			CtBinaryOperator<Boolean> cmp = factory.Core().createBinaryOperator();
			cmp.setKind(BinaryOperatorKind.LT);
			cmp.setLeftHandOperand(memoryModel); // TODO: reduce memoryModel
			cmp.setRightHandOperand(factory.Code().createCodeSnippetExpression("java.lang.Runtime.getRuntime().freeMemory()").compile());
			
			CtBinaryOperator<Boolean> and = factory.Core().createBinaryOperator();
			and.setKind(BinaryOperatorKind.AND);
			and.setRightHandOperand(parallelize);
			and.setLeftHandOperand(cmp);
			parallelize = and;
			parallelize.updateAllParentsBelow();
		}
		
		CtBlock exeBlock = factory.Core().createBlock();
		exe.setBody(exeBlock);
		cl.addMethod(exe);
		cons.setBody(block);
		cl.addConstructor(cons);
		container.addNestedType(cl);
		
		CtFieldRead<?> rtAccess = factory.Core().createFieldRead();
		CtTypeReference runtimeManager = factory.Type().createReference(
				RuntimeManager.class);
		CtTypeAccess<?> accRuntimeManager = factory.Core()
				.createTypeAccess();
		accRuntimeManager.setType(runtimeManager);
		rtAccess.setTarget(accRuntimeManager);
		CtVariableReference fieldRef = runtimeManager.getDeclaredFields().stream().filter(e -> e.getSimpleName().equals("currentTask")).iterator().next();
		rtAccess.setVariable(fieldRef);
		
		/* RuntimeManager.currentTask.set(aeTask);
		CtVariableAccess taskRead2 = (CtVariableAccess) CopyCatFactory.clone(taskRead);
		CtTypeReference tlocal = factory.Type().createReference(ThreadLocal.class);
		CtExecutableReference setExecutable = tlocal.getDeclaredExecutables().stream().filter(e -> e.getSimpleName().equals("set")).iterator().next();
		CtInvocation<?> setTask = factory.Code().createInvocation(rtAccess, setExecutable, taskRead2)
		exeBlock.addStatement(setTask);
		*/
		if (retType.equals(factory.Type().VOID)) {
			CtIf iif = factory.Core().createIf();
			iif.setCondition(parallelize);
			iif.setThenStatement(parVersion);
			iif.setElseStatement(seqVersion);
			exeBlock.addStatement(iif);
		} else {
			CtConditional cond = factory.Core().createConditional();
			cond.setCondition(parallelize);
			cond.setThenExpression(parVersion);
			cond.setElseExpression(seqVersion);
			CtExecutableReference setResultRef = factory.Executable().createReference(fbodyType, factory.Type().VOID_PRIMITIVE, "setResult", factory.Type().OBJECT);
			CtInvocation setResult = factory.Code().createInvocation(null, setResultRef, cond);
			exeBlock.addStatement(setResult);
		}
		CtTypeReference<?> an = factory.Type().createReference("aeminium.runtime.futures.codegen.NoVisit");
		factory.Annotation().annotate(cl, (CtTypeReference) an);
		TaskCreationProcessor.recAux.put(m, cl);
	}

	private CtExpression<?> visitMemoryModel(CtMethod<?> e, CostEstimation est, int overhead) {
		return est.getMemory(e.getFactory(), overhead);
	}

}
