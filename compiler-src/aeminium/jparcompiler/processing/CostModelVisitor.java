package aeminium.jparcompiler.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import aeminium.jparcompiler.model.CostEstimation;
import aeminium.jparcompiler.processing.utils.CopyCatFactory;
import aeminium.jparcompiler.processing.utils.ForAnalyzer;
import aeminium.jparcompiler.processing.utils.ModelUtils;
import aeminium.jparcompiler.processing.utils.SizeHelper;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;

public class CostModelVisitor extends CtAbstractVisitor {
	Stack<CtElement> stackCheck = new Stack<CtElement>();
	
	CtLiteral<Integer> loopLiteral = null;

	
	public void scan(CtElement e) {
		if (e == null)
			return;
		if (loopLiteral == null) {
			loopLiteral = e.getFactory().Code().createLiteral(5);
		}
		e.accept(this);
	}

	
	@SuppressWarnings("rawtypes")
	public CtLiteral createLoopLiteral() {
		return (CtLiteral) CopyCatFactory.clone(loopLiteral);
	}
	
	@Override
	public void visitCtAnonymousExecutable(CtAnonymousExecutable e) {
		// TODO
	}
	
	@Override
	public <T> void visitCtArrayRead(CtArrayRead<T> arrayAccess) {
		scan(arrayAccess.getTarget());
		scan(arrayAccess.getIndexExpression());
		CostEstimation ce = new CostEstimation();
		ce.add(get(arrayAccess.getTarget()));
		ce.add(get(arrayAccess.getIndexExpression()));
		ce.add("arrayaccess", 1);
		save(arrayAccess, ce);
	}
	
	@Override
	public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayAccess) {
		scan(arrayAccess.getTarget());
		scan(arrayAccess.getIndexExpression());
		CostEstimation ce = new CostEstimation();
		ce.add(get(arrayAccess.getTarget()));
		ce.add(get(arrayAccess.getIndexExpression()));
		ce.add("arrayaccess", 1);
		save(arrayAccess, ce);
	}
	
	@Override
	public <T> void visitCtAssert(CtAssert<T> asserted) {
		scan(asserted.getAssertExpression());
		scan(asserted.getExpression());
		saveEmpty(asserted);
	}
	
	@Override
	public <T, A extends T> void visitCtAssignment(
			CtAssignment<T, A> assignement) {
		scan(assignement.getAssigned());
		scan(assignement.getAssignment());
		CostEstimation ce = new CostEstimation();
		ce.add(get(assignement.getAssigned()));
		ce.add(get(assignement.getAssignment()));
		save(assignement, ce);
	}
	
	@Override
	public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
		scan(operator.getLeftHandOperand());
		scan(operator.getRightHandOperand());
		CostEstimation ce = new CostEstimation();
		ce.add(get(operator.getLeftHandOperand()));
		ce.add(get(operator.getRightHandOperand()));
		ce.add("op", 1);
		save(operator, ce);
	}
	
	@Override
	public <R> void visitCtBlock(CtBlock<R> block) {
		CostEstimation ce = new CostEstimation();
		for (CtStatement s : block.getStatements()) {
			scan(s);
			ce.add(get(s));
		}
		save(block, ce);
	}
	
	@Override
	public void visitCtBreak(CtBreak breakStatement) {
		saveEmpty(breakStatement);
	}
	public <E> void visitCtCase(CtCase<E> caseStatement) {
		//TODO
		saveEmpty(caseStatement);
	}
	
	public void visitCtCatch(CtCatch catchBlock) {
		// TODO
		saveEmpty(catchBlock);
	}
	
	public <T> void visitCtConditional(CtConditional<T> conditional) {
		scan(conditional.getCondition());
		scan(conditional.getThenExpression());
		scan(conditional.getElseExpression());
		CostEstimation ce = new CostEstimation();
		ce.add(get(conditional.getCondition()));
		ce.add(get(conditional.getCondition()));
		ce.add(get(conditional.getElseExpression()));
		//ce.add("if", 1);
		save(conditional, ce);
	}
	
	public <T> void visitCtConstructor(CtConstructor<T> c) {
		CostEstimation ce = new CostEstimation();
		scan(c.getBody());
		ce.add(get(c.getBody()));
		save(c, ce);
	}
	
	public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
		CostEstimation ce = new CostEstimation();
		for (CtElement e : ctConstructorCall.getArguments()) {
			scan(e);
			ce.add(get(e));
		}
		// TODO: allocate class
		save(ctConstructorCall, ce);
	}
	
	public void visitCtContinue(CtContinue continueStatement) {
		saveEmpty(continueStatement);
	}
	
	public void visitCtDo(CtDo doLoop) {
		CostEstimation ce = new CostEstimation();
		scan(doLoop.getLoopingExpression());
		scan(doLoop.getBody());
		ce.add(get(doLoop.getLoopingExpression()));
		ce.addComplex(createLoopLiteral(), get(doLoop.getBody()));
		save(doLoop, ce);
	}
	
	public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
		// Do Nothing
	}
	
	public <T> void visitCtField(CtField<T> f) {
		// Do Nothing
	}
	
	public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		save(thisAccess, ce);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void visitCtFor(CtFor forLoop) {
		CostEstimation ce = new CostEstimation();
		CostEstimation cbe = new CostEstimation();
		for (CtStatement s : forLoop.getForInit()) {
			scan(s);
			ce.add(get(s));
		}
		scan(forLoop.getExpression());
		cbe.add(get(forLoop.getExpression()));
		for (CtStatement s : forLoop.getForUpdate()) {
			scan(s);
			cbe.add(get(s));
		}
		scan(forLoop.getBody());
		cbe.add(get(forLoop.getBody()));
		CtExpression k = createLoopLiteral();
		ForAnalyzer fa = new ForAnalyzer(forLoop);
		if ( fa.canBeAnalyzed() ) {
			if (fa.st.toString().equals("0")) {
				k = fa.end;
			} else {
				CtBinaryOperator k1 = forLoop.getFactory().Core().createBinaryOperator();
				k1.setKind(BinaryOperatorKind.MINUS);
				k1.setLeftHandOperand(fa.end);
				k1.setRightHandOperand(fa.st);
				k = k1;
			}
		}
		
		ce.addComplex(ModelUtils.model(k), cbe);
		ce = ModelUtils.wrapScope(ce, forLoop);
		save(forLoop, ce);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void visitCtForEach(CtForEach foreach) {
		CostEstimation ce = new CostEstimation();
		CostEstimation cbe = new CostEstimation();
		scan(foreach.getVariable());
		scan(foreach.getExpression());
		scan(foreach.getBody());
		ce.add(get(foreach.getVariable()));
		ce.add(get(foreach.getExpression()));
		cbe.add(get(foreach.getBody()));
		
		CtExpression k = createLoopLiteral();
		if (foreach.getExpression().getType().toString().startsWith("java.util")) {
			CtTypeReference<?> list = foreach.getFactory().Type().createReference("java.util.List");
			CtExecutableReference ref = null;
			for (CtExecutableReference ex : list.getDeclaredExecutables()) {
				if (ex.toString().contains("#size()")) {
					ref = ex;
				}
			}
			CtInvocation k1 = foreach.getFactory().Code().createInvocation(foreach.getExpression(), ref, new ArrayList());
			k = k1;
		}
		ce.addComplex(k, cbe);
		save(foreach, ce);
	}
	
	public void visitCtIf(CtIf ifElement) {
		CostEstimation ce = new CostEstimation();
		scan(ifElement.getCondition());
		scan((CtStatement) ifElement.getThenStatement());
		ce.add(get(ifElement.getCondition()));
		ce.add(get(ifElement.getThenStatement()));
		if (ifElement.getElseStatement() != null) {
			scan((CtStatement) ifElement.getElseStatement());
			ce.add(get(ifElement.getElseStatement()));
		}
		ce.add("if", 1);
		save(ifElement, ce);
	}
	
	public <T> void visitCtInterface(CtInterface<T> intrface) {
		// Do Nothing
	}

	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		CostEstimation ce = new CostEstimation();
		if (invocation.getExecutable() != null) {
			if (invocation.getExecutable().getDeclaration() != null) {
				CtExecutable<T> ex = invocation.getExecutable().getDeclaration();
				int occurrences = Collections.frequency(stackCheck, ex);
				if (occurrences < 2) { 
					// Two times works best for recursive calls
					// Once is done by using:
					// !stackCheck.contains(ex)
					if (ex.getMetadata(CostEstimation.COST_MODEL_KEY) == null) {
						stackCheck.add(ex);
						scan(ex);
						stackCheck.pop();
					}
					ce.add(ModelUtils.replaceParameters(invocation, get(ex), ex));
				} else {
					ce.add("recursion", 1);
				}
			} else {
				ce.add(invocation.getExecutable().toString(), 1);
			}
		}
		if (invocation.getTarget() != null) {			
			scan(invocation.getTarget());
			ce = ModelUtils.replaceThisReferences(ce, invocation.getTarget());
			//ce.add(get(invocation.getTarget()));
		}
		for (CtExpression<?> arg : invocation.getArguments()) {
			scan(arg);
			ce.add(get(arg));
		}
		save(invocation, ce);
	}
	
	public <T> void visitCtLiteral(CtLiteral<T> literal) {
		saveEmpty(literal);
	}
	
	public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		ce.add("memory", SizeHelper.getSizeOf(localVariable.getType()));
		if (localVariable.getDefaultExpression() != null) {
			scan(localVariable.getDefaultExpression());
			ce.add(get(localVariable.getDefaultExpression()));
		}
		save(localVariable, ce);
	}
	
	@Override
	public <T> void visitCtMethod(CtMethod<T> m) {
		scan(m.getBody());
		CostEstimation ce = ModelUtils.wrapScope(copy(get(m.getBody())), m);
		save(m, ce);
	}
	
	public <T> void visitCtNewArray(CtNewArray<T> newArray) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		if (newArray.getDimensionExpressions().size() > 0) {
			int mul = SizeHelper.getSizeOf(newArray.getType());
			
			for (CtExpression<Integer> dim : newArray.getDimensionExpressions()) {
				if (dim instanceof CtLiteral) {
					mul *= (Integer) ((CtLiteral<Integer>) dim).getValue();
				} else {
					CostEstimation nce = new CostEstimation();
					nce.add("memory", mul);
					ce.addComplex(ModelUtils.model(dim), nce);
				}
			}
			ce.add("memory", mul);
		} else {
			ce.add("memory", SizeHelper.getSizeOf(newArray.getType()) * newArray.getElements().size());
		}
		save(newArray, ce);
	}
	
	public <T> void visitCtNewClass(CtNewClass<T> newClass) {
		CostEstimation ce = new CostEstimation();
		for (CtElement e : newClass.getArguments()) {
			scan(e);
			ce.add(get(e));
		}
		scan(newClass.getAnonymousClass());
		ce.add(get(newClass.getAnonymousClass()));
		// TODO: allocate class
		save(newClass, ce);
	}
	
	@Override
	public <T> void visitCtLambda(CtLambda<T> lambda) {
		if (lambda.getBody() != null) {
			scan(lambda.getBody());
			saveCopyInto(lambda.getBody(), lambda);
		} else {
			scan(lambda.getExpression());
			saveCopyInto(lambda.getExpression(), lambda);
		}
	}
	
	public <T, A extends T> void visitCtOperatorAssignment(
			CtOperatorAssignment<T, A> assignment) {
		CostEstimation ce = new CostEstimation();
		scan(assignment.getAssigned());
		scan(assignment.getAssignment());
		ce.add(get(assignment.getAssigned()));
		ce.add(get(assignment.getAssignment()));
		ce.add("op", 1);
		save(assignment, ce);
	}
	
	public void visitCtPackage(CtPackage ctPackage) {
		// Do Nothing
	}

	public <T> void visitCtParameter(CtParameter<T> parameter) {
		saveEmpty(parameter);
	}
	
	public <R> void visitCtReturn(CtReturn<R> returnStatement) {
		if (returnStatement.getReturnedExpression() != null) {
			scan(returnStatement.getReturnedExpression());
			saveCopyInto(returnStatement.getReturnedExpression(), returnStatement);
		} else {
			saveEmpty(returnStatement);
		}
	}
	
	public <R> void visitCtStatementList(CtStatementList statements) {
		CostEstimation ce = new CostEstimation();
		for (CtStatement s : statements.getStatements()) {
			scan(s);
			ce.add(get(s));

		}
		save(statements, ce);
	}
	
	public <E> void visitCtSwitch(CtSwitch<E> switchStatement) {
		// TODO
	}
	
	public void visitCtSynchronized(CtSynchronized synchro) {
		CostEstimation ce = new CostEstimation();
		scan(synchro.getExpression());
		scan(synchro.getBlock());
		ce.add(get(synchro.getExpression()));
		ce.add(get(synchro.getBlock()));
		ce.add("synchronized", 1);
		save(synchro, ce);
	}
	
	@Override
	public void visitCtThrow(CtThrow throwStatement) {
		scan(throwStatement.getThrownExpression());
		saveCopyInto(throwStatement.getThrownExpression(), throwStatement);
	}
	
	@Override
	public void visitCtTry(CtTry tryBlock) {
		CostEstimation ce = new CostEstimation();
		scan(tryBlock.getBody());
		ce.add(get(tryBlock.getBody()));
		for (CtCatch c : tryBlock.getCatchers()) {
			scan(c);
			ce.add(get(c));
		}
		scan(tryBlock.getFinalizer());
		ce.add(get(tryBlock.getFinalizer()));
		save(tryBlock, ce);
	}
	
	@Override
	public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
		CostEstimation ce = new CostEstimation();
		scan(tryWithResource.getBody());
		ce.add(get(tryWithResource.getBody()));
		for (CtCatch c : tryWithResource.getCatchers()) {
			scan(c);
			ce.add(get(c));
		}
		scan(tryWithResource.getFinalizer());
		ce.add(get(tryWithResource.getFinalizer()));
		save(tryWithResource, ce);
	}
	
	@Override
	public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
		CostEstimation ce = new CostEstimation();
		scan(operator.getOperand());
		ce.add(get(operator.getOperand()));
		ce.add("op", 1);
		save(operator, ce);
	}
	
	@Override
	public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		save(variableRead, ce);
	}
	
	@Override
	public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		save(variableWrite, ce);
	}
	
	public void visitCtWhile(CtWhile whileLoop) {
		CostEstimation ce = new CostEstimation();
		scan(whileLoop.getLoopingExpression());
		scan(whileLoop.getBody());
		ce.add(get(whileLoop.getLoopingExpression()));
		ce.addComplex(createLoopLiteral(), get(whileLoop.getBody()));
		save(whileLoop, ce);
	}
	
	public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		save(fieldRead, ce);
	}
	
	public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
		CostEstimation ce = new CostEstimation();
		ce.add("access", 1);
		save(fieldWrite, ce);
	}
	
	
	
	// Helper Methods
	
	public CostEstimation get(CtElement e) {
		if (e == null) return new CostEstimation(); // TODO: FIXME
		return (CostEstimation) e.getMetadata(CostEstimation.COST_MODEL_KEY);
	}
	
	public CostEstimation save(CtElement e, CostEstimation ce) {
		e.putMetadata(CostEstimation.COST_MODEL_KEY, ce);
		return ce;
	}
	
	public CostEstimation merge(CostEstimation e, CostEstimation e2) {
		CostEstimation n = new CostEstimation();
		n.add(e);
		n.add(e2);
		return n;
	}
	
	public CostEstimation copy(CostEstimation e) {
		CostEstimation n = new CostEstimation();
		n.add(e);
		return n;
	}
	
	public void saveCopyInto(CtElement e, CtElement ne) {
		save(ne, copy((CostEstimation) e.getMetadata("cost")));
	}
	
	public void saveCopyWithInto(CtElement e, CtElement e2, CtElement ne) {		
		CostEstimation nc = copy((CostEstimation) e.getMetadata(CostEstimation.COST_MODEL_KEY));
		nc.add((CostEstimation) e2.getMetadata(CostEstimation.COST_MODEL_KEY));
		save(ne, nc);
	}
	
	public void saveEmpty(CtElement e) {
		save(e, new CostEstimation());
	}
	
}
