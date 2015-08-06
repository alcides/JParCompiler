package aeminium.jparcompiler.processing;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import spoon.reflect.code.CtAnnotationFieldAccess;
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
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.cu.SourcePosition;
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
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtCatchVariableReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtUnboundVariableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.Filter;
import aeminium.jparcompiler.model.Permission;
import aeminium.jparcompiler.model.PermissionSet;
import aeminium.jparcompiler.model.PermissionType;

public class PermissionSetVisitor extends CtAbstractVisitor {

	HashMap<CtElement, PermissionSet> database = new HashMap<CtElement, PermissionSet>();
	HashMap<SourcePosition, PermissionSet> databasePos = new HashMap<SourcePosition, PermissionSet>(); // backup
	Stack<CtElement> stackCheck = new Stack<CtElement>();

	static CtElement out;
	
	public PermissionSetVisitor() {
		super();
	}
	
	public PermissionSet getPermissionSet(CtElement e) {
		if (e == null) {
			throw new RuntimeException("Null pointer: " + e);
		}
		return database.get(e);
	}
	
	public void setPermissionSet(CtElement e, PermissionSet s) {
		database.put(e, s);
		databasePos.put(e.getPosition(), s);
	}
	
	public PermissionSet merge(CtElement e1, CtElement e2) {
		if (e1 == null && e2 == null) return null; // Check this
		if (e1 == null) return getPermissionSet(e2);
		if (e2 == null) return getPermissionSet(e1);
		return getPermissionSet(e1).merge(getPermissionSet(e2));
	}
	
	
	public void scan(CtElement e) {
		if (out == null) {
			Factory f = e.getFactory();
			out = f.Core().createAssert(); // Hackish, I know.
		}
		
		if (e != null) {
			e.accept(this);
			if (getPermissionSet(e) == null) {
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

	@Override
	public <T> void visitCtArrayRead(CtArrayRead<T> arrayAccess) {
		scan(arrayAccess.getTarget());
		scan(arrayAccess.getIndexExpression());
		setPermissionSet(arrayAccess, merge(arrayAccess.getTarget(), arrayAccess.getIndexExpression()));
	}

	@Override
	public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayAccess) {
		scan(arrayAccess.getTarget());
		scan(arrayAccess.getIndexExpression());
		
		PermissionSet set = getPermissionSet(arrayAccess.getTarget());
		set = set.merge(getPermissionSet(arrayAccess.getIndexExpression()));
		setPermissionSet(arrayAccess, set);
	}

	public <T> void visitCtAssert(CtAssert<T> asserted) {
		scan(asserted.getAssertExpression());
		scan(asserted.getExpression());
		setPermissionSet(asserted, merge(asserted.getExpression(), asserted.getAssertExpression()));
	}

	public <T, A extends T> void visitCtAssignment(
			CtAssignment<T, A> assignement) {
		scan(assignement.getAssigned());
		scan(assignement.getAssignment());
		PermissionSet set = getPermissionSet(assignement.getAssigned());
		setPermissionSet(assignement, set.merge(getPermissionSet(assignement.getAssignment())));
	}

	public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
		scan(operator.getLeftHandOperand());
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
		set.removeIf((p) -> p.control == true && p.target == block);
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
		scan(doLoop.getLoopingExpression());
		scan(doLoop.getBody());
		setPermissionSet(doLoop, merge(doLoop.getBody(), doLoop.getLoopingExpression()));
	}

	public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
		// Do Nothing
	}

	public <T> void visitCtField(CtField<T> f) {
		// Do Nothing
	}

	public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
		PermissionSet set = new PermissionSet();
		Permission p = new Permission(PermissionType.READ, thisAccess.getType().getDeclaration());
		p.instance = true;
		set.add(p);
		setPermissionSet(thisAccess, set);
	}

	public <T> void visitCtAnnotationFieldAccess(
			CtAnnotationFieldAccess<T> annotationFieldAccess) {
		// Do Nothing
	}

	public void visitCtFor(CtFor forLoop) {
		for (CtStatement s : forLoop.getForInit()) {
			scan(s);
		}
		scan(forLoop.getExpression());
		for (CtStatement s : forLoop.getForUpdate()) {
			scan(s);
		}
		scan(forLoop.getBody());
		
		PermissionSet set = merge(forLoop.getExpression(), forLoop.getBody());		
		for (CtStatement s : forLoop.getForInit()) {
			set = set.merge(getPermissionSet(s));
		}
		for (CtStatement s : forLoop.getForUpdate()) {
			set = set.merge(getPermissionSet(s));
		}
		
		setPermissionSet(forLoop, set);
		
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
		set.add(createControlPermission(ifElement.getParent(CtBlock.class)));
		setPermissionSet(ifElement, set);
	}

	public <T> void visitCtInterface(CtInterface<T> intrface) {
		// Do Nothing
	}

	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		PermissionSet parset = new PermissionSet();
		
		if (invocation.getTarget() != null) {
			scan(invocation.getTarget());
			PermissionSet tset = getPermissionSet(invocation.getTarget());
			// CheckForDefaults
			tset = checkForDefaults(tset, invocation.getExecutable());
			if (tset != null) {
				parset = parset.merge(tset);
			}
		}
		
		for(int i = 0; i < invocation.getArguments().size();i++){
			CtExpression<?> arg_i = invocation.getArguments().get(i);
			scan(arg_i);
			parset = parset.merge(getPermissionSet(arg_i));
		}
		PermissionSet set = parset.copy();
		
		scan(invocation.getExecutable());
		CtElement target = getDeclarationOf(invocation.getExecutable());
		if (target != null) {
			Permission acess = new Permission(PermissionType.READ, target);
			set.add(acess);
			CtMethod<?> meth = (CtMethod<?>) target;
			if (meth == invocation.getParent(CtMethod.class) || stackCheck.contains(meth)) {
				// Use parset only in recursive calls
			} else {
				if (getPermissionSet(meth) == null) {
					stackCheck.add(meth);
					scan(meth);
					stackCheck.pop();
				}
				if (getPermissionSet(meth.getBody()) == null) {
					stackCheck.add(meth);
					scan(meth.getBody());
					stackCheck.pop();
				}
				PermissionSet declareSet = getPermissionSet(meth.getBody());
				declareSet.removeReturn();
				for (Permission p : declareSet) {
					if (p.target instanceof CtParameter) {
						/*
						int index = 0;
						for (CtParameter<?> par : meth.getParameters()) {
							if (par == p.target) break;
							index++;
						}
						Permission p2 = new Permission(p.type, invocation.getArguments().get(index));
						set.add(p2);
						*/
					} else {
						set.add(p);
					}
				}
			}
		}
		setPermissionSet(invocation, set);
	}

	public <T> void visitCtLiteral(CtLiteral<T> literal) {
		setPermissionSet(literal, new PermissionSet());
	}

	public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
		Permission p = new Permission(PermissionType.WRITE, localVariable);
		PermissionSet set = new PermissionSet();
		set.add(p);
		
		if (localVariable.getAssignment() != null) {
			scan(localVariable.getAssignment());
			set = set.merge(getPermissionSet(localVariable.getAssignment()));
		}
		setPermissionSet(localVariable, set);
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
		PermissionSet set = new PermissionSet();
		for (CtExpression<?> e : newArray.getElements()) {
			scan(e);
			set = set.merge(getPermissionSet(e));
		}
		setPermissionSet(newArray, set);
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
		scan(ctConstructorCall.getArguments());
		PermissionSet parset = new PermissionSet();
		for(int i = 0; i < ctConstructorCall.getArguments().size();i++){
			CtExpression<?> arg_i = ctConstructorCall.getArguments().get(i);
			scan(arg_i);
			parset = parset.merge(getPermissionSet(arg_i));
		}
		setPermissionSet(ctConstructorCall, parset);
	}

	public <T> void visitCtNewClass(CtNewClass<T> newClass) {
		scan(newClass.getArguments());
		PermissionSet parset = new PermissionSet();
		for(int i = 0; i < newClass.getArguments().size();i++){
			CtExpression<?> arg_i = newClass.getArguments().get(i);
			scan(arg_i);
			parset = parset.merge(getPermissionSet(arg_i));
		}
		PermissionSet set = parset.copy();
		if (newClass.getAnonymousClass() != null) {
			scan(newClass.getAnonymousClass());
			set = set.merge(getPermissionSet(newClass.getAnonymousClass()));
		}
		setPermissionSet(newClass, set);
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
		scan(assignment.getAssigned());
		scan(assignment.getAssignment());
		PermissionSet set = merge(assignment.getAssigned(), assignment.getAssignment());
		setPermissionSet(assignment, set);
	}

	public void visitCtPackage(CtPackage ctPackage) {
	}

	public <T> void visitCtParameter(CtParameter<T> parameter) {
		// Do Nothing
	}
	
	public <R> void visitCtReturn(CtReturn<R> returnStatement) {
		scan(returnStatement.getReturnedExpression());
		Permission retp = createControlPermission(returnStatement.getParent(CtBlock.class));
		PermissionSet set = (returnStatement.getReturnedExpression() == null) ? new PermissionSet() : getPermissionSet(returnStatement.getReturnedExpression());
		set.add(retp);
		setPermissionSet(returnStatement, set);
	}

	public <R> void visitCtStatementList(CtStatementList statements) {
		PermissionSet set = new PermissionSet();
		for (CtStatement s : statements.getStatements()) {
			scan(s);
			set = set.merge(getPermissionSet(s));

		}
		setPermissionSet(statements, set);		
	}

	public <E> void visitCtSwitch(CtSwitch<E> switchStatement) {
		// TODO
		scan(switchStatement.getSelector());
		for (CtCase<?> c : switchStatement.getCases())
			scan(c);
	}
	
	public <T>void visitCtTypeAccess(spoon.reflect.code.CtTypeAccess<T> typeAccess) {
		PermissionSet set = new PermissionSet();
		if (typeAccess.getType().getDeclaration() != null) {
			Permission p = new Permission(PermissionType.READ, typeAccess.getType().getDeclaration());
			set.add(p);
		}
		setPermissionSet(typeAccess, set);
	}

	public void visitCtSynchronized(CtSynchronized synchro) {
		scan(synchro.getExpression());
		scan(synchro.getBlock());
		Permission retp = createControlPermission(synchro.getParent(CtBlock.class));
		PermissionSet set = getPermissionSet(synchro.getExpression());
		set.add(retp);
		set = set.merge(getPermissionSet(synchro.getBlock()));
		setPermissionSet(synchro, set);
	}

	public void visitCtThrow(CtThrow throwStatement) {
		scan(throwStatement.getThrownExpression());
		Permission retp = createControlPermission(throwStatement.getParent(CtBlock.class));
		PermissionSet set = getPermissionSet(throwStatement.getThrownExpression());
		set.add(retp);
		setPermissionSet(throwStatement, set);
	}

	public void visitCtTry(CtTry tryBlock) {
		PermissionSet set = new PermissionSet();
		scan(tryBlock.getBody());
		set = set.merge(getPermissionSet(tryBlock.getBody()));
		for (CtCatch c : tryBlock.getCatchers()) {
			scan(c);
			set = set.merge(getPermissionSet(c));
		}
		scan(tryBlock.getFinalizer());
		setPermissionSet(tryBlock, set);
	}

	@Override
	public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
		PermissionSet set = new PermissionSet();
		// TODO: Resource
		set.add(createControlPermission(tryWithResource.getParent(CtBlock.class)));
		for (CtLocalVariable<?> resource : tryWithResource.getResources()) {
			scan(resource);
		}
		scan(tryWithResource.getBody());
		set = set.merge(getPermissionSet(tryWithResource.getBody()));
		for (CtCatch c : tryWithResource.getCatchers()) {
			scan(c);
			set = set.merge(getPermissionSet(c));
		}
		scan(tryWithResource.getFinalizer());
		set = set.merge(getPermissionSet(tryWithResource.getFinalizer()));
		setPermissionSet(tryWithResource, set);
	}

	public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
		scan(operator.getOperand());
		setPermissionSet(operator, getPermissionSet(operator.getOperand()));
	}

	public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
		CtElement target = variableRead.getVariable().getDeclaration();
		Permission p = new Permission(PermissionType.READ, target);
		PermissionSet set = new PermissionSet();
		set.add(p);
		setPermissionSet(variableRead, set);
	}

	@Override
	public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite) {
		CtElement target = variableWrite.getVariable().getDeclaration();
		Permission p = new Permission(PermissionType.WRITE, target);
		PermissionSet set = new PermissionSet();
		set.add(p);
		setPermissionSet(variableWrite, set);
	}

	public void visitCtWhile(CtWhile whileLoop) {
		scan(whileLoop.getLoopingExpression());
		scan(whileLoop.getBody());
		setPermissionSet(whileLoop, merge(whileLoop.getBody(), whileLoop.getLoopingExpression()));
	}

	public <T> void visitCtUnboundVariableReference(
			CtUnboundVariableReference<T> reference) {
		throw new RuntimeException("Found unbound reference " + reference);
	}

	@Override
	public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
		scan(fieldRead.getTarget());
		PermissionSet set = new PermissionSet();
		if (fieldRead.getVariable().toString().startsWith("java.lang.Math")) {
			setPermissionSet(fieldRead, set);
			return;
		}
		if (fieldRead.getSignature().endsWith("#length")) {
			set.add(new Permission(PermissionType.READ, fieldRead.getTarget()));
		} else {
			if (fieldRead.getVariable().getDeclaration() != null) {
				set.add(new Permission(PermissionType.READ, fieldRead.getVariable().getDeclaration()));
			} else {
				set.add(new Permission(PermissionType.WRITE, out));
			}
		}
		setPermissionSet(fieldRead, set);
	}

	@Override
	public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
		
		PermissionSet set = new PermissionSet();
		scan(fieldWrite.getTarget());
		if (fieldWrite.getVariable().getDeclaration() != null) {
			Permission p = new Permission(PermissionType.WRITE, fieldWrite.getVariable().getDeclaration());
			set.add(p);
		}
		setPermissionSet(fieldWrite, set);
	}

	@Override
	public <T> void visitCtSuperAccess(CtSuperAccess<T> f) {
		throw new RuntimeException("Found super access " + f);
	}
	
	private Permission createControlPermission(CtElement ifElement) {
		Permission p = new Permission(PermissionType.WRITE, ifElement);
		p.control = true;
		return p;
	}
	
	private CtElement getDeclarationOf(CtExecutableReference<?> r) {
		if (r.getDeclaration() != null) return r.getDeclaration();
		if (r.getDeclaringType() != null && r.getDeclaringType().getDeclaration() != null) return r.getDeclaringType().getDeclaration();
		return null;
	}
	private PermissionSet checkForDefaults (PermissionSet set, CtExecutableReference<?> ref) {
		if (ref == null) return set;
		if (ref.toString().startsWith("java.util.ArrayList") && ref.toString().endsWith(".add")) {
			set = set.copy();
			for (Permission p : set) {
				if (p.type == PermissionType.READ) p.type = PermissionType.READWRITE;
			}
		}
		return set;
	}
}
