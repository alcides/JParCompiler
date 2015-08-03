package aeminium.jparcompiler.processing;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;

import spoon.reflect.code.CtAnnotationFieldAccess;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
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
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtCatchVariableReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtUnboundVariableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.Filter;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;

public class PermissionSetVisitor extends CtAbstractVisitor {

	HashMap<CtElement, PermissionSet> database = new HashMap<CtElement, PermissionSet>();
	
	Permission tmp;
	
	public PermissionSetVisitor() {
		super();
	}
	
	public PermissionSet getPermissionSet(CtElement e) {
		return database.get(e);
	}
	
	public void setPermissionSet(CtElement e, PermissionSet s) {
		database.put(e, s);
	}
	
	public PermissionSet merge(CtElement e1, CtElement e2) {
		if (e1 == null && e2 == null) return null; // Check this
		if (e1 == null) return getPermissionSet(e2);
		if (e2 == null) return getPermissionSet(e1);
		return getPermissionSet(e1).merge(getPermissionSet(e2));
	}
	
	
	public void scan(CtElement e) {
		if (e != null) {
			e.accept(this);
			if (!database.containsKey(e)) {
				System.out.println("did not process: " + e + ", " + e.getClass());
			}
		}
	}

	public void scan(CtReference e) {
		if (e != null) {
			e.accept(this);
		}
	}

	public <A extends Annotation> void visitCtAnnotation(
			CtAnnotation<A> annotation) {
		// Do Nothing
	}

	public <A extends Annotation> void visitCtAnnotationType(
			CtAnnotationType<A> annotationType) {
		// Do Nothing
	}

	public void visitCtAnonymousExecutable(CtAnonymousExecutable e) {
		// TODO
	}

	public <T, E extends CtExpression<?>> void visitCtArrayAccess(
			CtArrayAccess<T, E> arrayAccess) {
		scan(arrayAccess.getTarget());
		scan(arrayAccess.getIndexExpression());
		// TODO
	}

	@Override
	public <T> void visitCtArrayRead(CtArrayRead<T> arrayRead) {
		visitCtArrayAccess(arrayRead);
		// TODO
	}

	@Override
	public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayWrite) {
		visitCtArrayAccess(arrayWrite);
		// TODO
	}

	public <T> void visitCtArrayTypeReference(CtArrayTypeReference<T> reference) {
		scan(reference.getComponentType());
		// TODO
	}

	public <T> void visitCtAssert(CtAssert<T> asserted) {
		scan(asserted.getAssertExpression());
		scan(asserted.getExpression());
		// TODO READ
	}

	public <T, A extends T> void visitCtAssignment(
			CtAssignment<T, A> assignement) {
		PermissionSet set = new PermissionSet();
		scan(assignement.getAssigned());
		if (tmp != null) {
			tmp.type = PermissionType.READ;
			set.add(tmp);
		}
		scan(assignement.getAssignment());
		setPermissionSet(assignement, set.merge(getPermissionSet(assignement.getAssignment())));
	}

	public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
		scan(operator.getLeftHandOperand());
		//write(operator.getKind().toString());
		scan(operator.getRightHandOperand());
		setPermissionSet(operator, merge(operator.getLeftHandOperand(), operator.getRightHandOperand()));
	}

	public <R> void visitCtBlock(CtBlock<R> block) {
		PermissionSet set = new PermissionSet();
		for (CtStatement s : block.getStatements()) {
			scan(s);
			set = set.merge(getPermissionSet(s));
		}
		List<CtLocalVariable<?>> es = block.getElements(new Filter<CtLocalVariable<?>>() {

			@Override
			public boolean matches(CtLocalVariable<?> e) {
				return true;
			}
		});
		
		for (CtLocalVariable<?> e : es) {
			set.removeTarget(e);
		}
		
		setPermissionSet(block, set);
	}

	public void visitCtBreak(CtBreak breakStatement) {
		// Do Nothing
	}

	public <E> void visitCtCase(CtCase<E> caseStatement) {
		scan(caseStatement.getCaseExpression());
		// TODO
	}

	public void visitCtCatch(CtCatch catchBlock) {
		// TODO
		scan(catchBlock.getParameter().getType());
	}

	public <T> void visitCtClass(CtClass<T> ctClass) {
		System.out.println("Should not be processing class " + ctClass.getQualifiedName());
	}

	public <T> void visitCtConditional(CtConditional<T> conditional) {
		scan(conditional.getCondition());
		scan(conditional.getThenExpression());
		scan(conditional.getElseExpression());
	}

	public <T> void visitCtConstructor(CtConstructor<T> c) {
		// TODO
		for (CtParameter<?> p : c.getParameters()) {
			scan(p.getType());
		}
	}

	public void visitCtContinue(CtContinue continueStatement) {
		// Do Nothing
	}

	public void visitCtDo(CtDo doLoop) {
		// TODO
		scan(doLoop.getBody());
		scan(doLoop.getLoopingExpression());
	}

	public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
		// Do Nothing
	}

	public <T> void visitCtExecutableReference(
			CtExecutableReference<T> reference) {
		if (reference.getDeclaringType()!=null) { // null in noclasspath
			if (reference.getDeclaringType().getDeclaration() != null) {
				tmp = new Permission(PermissionType.TEMP, reference.getDeclaringType().getDeclaration());
			} else {
				tmp = null;
			}
		} else {
			tmp = new Permission(PermissionType.TEMP, reference.getDeclaration());
		}
	}

	public <T> void visitCtField(CtField<T> f) {
		scan(f.getType());
		// TODO
	}

	public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
		// TODO
		//write(thisAccess.getType().getQualifiedName() + ".this");
	}

	public <T> void visitCtAnnotationFieldAccess(
			CtAnnotationFieldAccess<T> annotationFieldAccess) {
		scan(annotationFieldAccess.getTarget());
	}

	public <T> void visitCtFieldReference(CtFieldReference<T> reference) {
		// TODO
	}

	public void visitCtFor(CtFor forLoop) {
		// TODO
		for (CtStatement s : forLoop.getForInit()) {
			scan(s);
		}
		scan(forLoop.getExpression());
		for (CtStatement s : forLoop.getForUpdate()) {
			scan(s);
		}
		scan(forLoop.getBody());
	}

	public void visitCtForEach(CtForEach foreach) {
		// TODO
		scan(foreach.getVariable());
		scan(foreach.getExpression());
		scan(foreach.getBody());
	}

	public void visitCtIf(CtIf ifElement) {
		scan(ifElement.getCondition());
		scan((CtStatement) ifElement.getThenStatement());
		scan((CtStatement) ifElement.getElseStatement());
		
		PermissionSet set = merge(ifElement.getCondition(), ifElement.getThenStatement());
		if (ifElement.getElseStatement() != null) {
			set = set.merge(getPermissionSet(ifElement.getElseStatement()));
		}
		setPermissionSet(ifElement, set);
		
	}

	public <T> void visitCtInterface(CtInterface<T> intrface) {
		// Do Nothing
	}

	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		PermissionSet parset = new PermissionSet();
		for(int i = 0; i < invocation.getArguments().size();i++){
			CtExpression<?> arg_i = invocation.getArguments().get(i);
			scan(arg_i);
			parset = parset.merge(getPermissionSet(arg_i));
		}
		PermissionSet set = parset.copy();
		
		CtElement e = invocation.getExecutable().getDeclaration();
		if (e == null) {
			Permission t = new Permission(PermissionType.READ, invocation.getTarget());
			set.add(t);
		} else if (e instanceof CtMethod) {
			CtMethod<?> meth = (CtMethod<?>) e; // TODO: Constructor call
			if (meth == invocation.getParent(CtMethod.class)) {
				// Use parset only in recursive calls
			} else {
				if (!database.containsKey(meth)) {
					scan(meth.getBody());
				}
				// TODO: Function call
			}
		} else {
			System.out.println("Constructor TODO" + e.getClass());
		}
		
		scan(invocation.getExecutable());
		if (tmp != null) {
			tmp.type = PermissionType.READ;
			set.add(tmp);
		}
		setPermissionSet(invocation, set);
	
	}

	public <T> void visitCtLiteral(CtLiteral<T> literal) {
		setPermissionSet(literal, new PermissionSet());
	}

	public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
		scan(localVariable.getReference());
		tmp.type = PermissionType.WRITE;
		PermissionSet set = new PermissionSet();
		set.add(tmp);
		
		// Add default expression as well
		scan(localVariable.getDefaultExpression());
		set.merge(getPermissionSet(localVariable.getDefaultExpression()));
		
		setPermissionSet(localVariable, set);
	}

	public <T> void visitCtLocalVariableReference(
			CtLocalVariableReference<T> reference) {
		tmp = new Permission(PermissionType.TEMP, reference.getDeclaration());
	}

	@Override
	public <T> void visitCtCatchVariable(CtCatchVariable<T> catchVariable) {
		// TODO
	}

	@Override
	public <T> void visitCtCatchVariableReference(CtCatchVariableReference<T> reference) {
		scan(reference.getDeclaration());
	}

	public <T> void visitCtMethod(CtMethod<T> m) {
		scan(m.getBody());
		PermissionSet set = getPermissionSet(m.getBody()).copy();
		set.removeReturn();
		setPermissionSet(m, set);
	}

	public <T> void visitCtNewArray(CtNewArray<T> newArray) {
		for (CtExpression<?> e : newArray.getElements()) {
			scan(e);
		}
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
		// TODO
		scan(ctConstructorCall.getExecutable());
	}

	public <T> void visitCtNewClass(CtNewClass<T> newClass) {
		// TODO
		scan(newClass.getExecutable());
		scan(newClass.getAnonymousClass());
	}

	@Override
	public <T> void visitCtLambda(CtLambda<T> lambda) {
		// TODO
		scan(lambda.getBody());
	}

	@Override
	public <T, E extends CtExpression<?>> void visitCtExecutableReferenceExpression(CtExecutableReferenceExpression<T, E> expression) {
		//write(expression.toString());
		// TODO
	}

	public <T> void visitCtCodeSnippetExpression(
			CtCodeSnippetExpression<T> expression) {
		//write(expression.getValue());
		// TODO
	}

	public void visitCtCodeSnippetStatement(CtCodeSnippetStatement statement) {
		//write(statement.getValue());
		/// TODO
	}

	public <T, A extends T> void visitCtOperatorAssignment(
			CtOperatorAssignment<T, A> assignment) {
		// TODO - Important
		scan(assignment.getAssigned());
		scan(assignment.getAssignment());
	}

	public void visitCtPackage(CtPackage ctPackage) {
	}

	public void visitCtPackageReference(CtPackageReference reference) {
		// write(reference.getSimpleName());
		// Do Nothing
	}

	public <T> void visitCtParameter(CtParameter<T> parameter) {
		// Do Nothing
	}

	public <T> void visitCtParameterReference(CtParameterReference<T> reference) {
		tmp = new Permission(PermissionType.TEMP, reference.getDeclaration());
	}

	public <R> void visitCtReturn(CtReturn<R> returnStatement) {
		scan(returnStatement.getReturnedExpression());
		Permission retp = new Permission(PermissionType.WRITE, returnStatement.getParent(CtMethod.class));
		retp.ret = true;
		PermissionSet set = (returnStatement.getReturnedExpression() == null) ? new PermissionSet() : getPermissionSet(returnStatement.getReturnedExpression());
		set.add(retp);
		setPermissionSet(returnStatement, set);
	}

	public <R> void visitCtStatementList(CtStatementList statements) {
		for (CtStatement s : statements.getStatements()) {
			scan(s);
		}
	}

	public <E> void visitCtSwitch(CtSwitch<E> switchStatement) {
		// TODO
		scan(switchStatement.getSelector());
		for (CtCase<?> c : switchStatement.getCases())
			scan(c);
	}

	public void visitCtSynchronized(CtSynchronized synchro) {
		// TODO
		scan(synchro.getExpression());
		scan(synchro.getBlock());
	}

	public void visitCtThrow(CtThrow throwStatement) {
		// TODO
		scan(throwStatement.getThrownExpression());
	}

	public void visitCtTry(CtTry tryBlock) {
		// TODO
		scan(tryBlock.getBody());
		for (CtCatch c : tryBlock.getCatchers()) {
			scan(c);
		}
		scan(tryBlock.getFinalizer());
	}

	@Override
	public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
		// TODO
		for (CtLocalVariable<?> resource : tryWithResource.getResources()) {
			scan(resource);
		}
		scan(tryWithResource.getBody());
		for (CtCatch c : tryWithResource.getCatchers()) {
			scan(c);
		}
		scan(tryWithResource.getFinalizer());
	}

	public void visitCtTypeParameter(CtTypeParameter typeParameter) {
		// Do Nothing
	}

	public void visitCtTypeParameterReference(CtTypeParameterReference ref) {
		// Do Nothing
	}

	public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
		// Do Nothing
	}

	@Override
	public <T> void visitCtTypeAccess(CtTypeAccess<T> typeAccess) {
		scan(typeAccess.getType());
	}

	public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
		scan(operator.getOperand());
		//write(operator.getKind().toString());
		// TODO
	}

	@Override
	public <T> void visitCtVariableAccess(CtVariableAccess<T> variableAccess) {
		scan(variableAccess.getVariable());
	}

	public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
		visitCtVariableAccess(variableRead);
		PermissionSet set = new PermissionSet();
		tmp.type = PermissionType.READ;
		set.add(tmp);
		setPermissionSet(variableRead, set);
	}

	@Override
	public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite) {
		visitCtVariableAccess(variableWrite);
		PermissionSet set = new PermissionSet();
		tmp.type = PermissionType.WRITE;
		set.add(tmp);
		setPermissionSet(variableWrite, set);
	}

	public void visitCtWhile(CtWhile whileLoop) {
		// TODO
		scan(whileLoop.getLoopingExpression());
		scan(whileLoop.getBody());
	}

	public <T> void visitCtUnboundVariableReference(
			CtUnboundVariableReference<T> reference) {
		// Do Nothing
	}

	@Override
	public <T> void visitCtFieldAccess(CtFieldAccess<T> f) {
		scan(f.getVariable());
		// TODO - Important
	}

	@Override
	public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
		visitCtFieldAccess(fieldRead);
		// TODO - Important
	}

	@Override
	public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
		visitCtFieldAccess(fieldWrite);
		// TODO - Important
	}

	@Override
	public <T> void visitCtSuperAccess(CtSuperAccess<T> f) {
		// TODO
		//write(f.getType().getQualifiedName() + ".super");
	}
	

}
