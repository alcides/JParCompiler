package aeminium.jparcompiler.processing.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;

public class CopyCatFactory extends CtAbstractVisitor {
	
	public static CtElement basicClone(CtElement e) {
		return e.getFactory().Core().clone(e);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static CtElement clone(CtElement el) {
		if (el == null) return null;
		
		CtElement ccat = basicClone(el);
		
		if (el instanceof CtAnonymousExecutable) {
			CtAnonymousExecutable cat = (CtAnonymousExecutable) ccat;
			cat.setBody( (CtBlock<Void>) clone(cat.getBody()) );
		}
		else if (el instanceof CtArrayAccess) {
			CtArrayAccess cat = (CtArrayAccess) ccat;
			cat.setIndexExpression((CtExpression) clone(cat.getIndexExpression()));
			cat.setTarget((CtExpression) clone(cat.getTarget()));
		}
		else if (el instanceof CtAssert) {
			CtAssert cat = (CtAssert) ccat;
			cat.setAssertExpression((CtExpression) clone(cat.getAssertExpression()));
			cat.setExpression((CtExpression) clone(cat.getExpression()));
		}
		else if (el instanceof CtAssignment) {
			CtAssignment cat = (CtAssignment) ccat;
			cat.setAssigned((CtExpression) clone(cat.getAssigned()));
			cat.setAssignment((CtExpression) clone(cat.getAssignment()));
		}
		else if (el instanceof CtBinaryOperator) {
			CtBinaryOperator cat = (CtBinaryOperator) ccat;
			cat.setLeftHandOperand((CtExpression) clone(cat.getLeftHandOperand()));
			cat.setRightHandOperand((CtExpression) clone(cat.getRightHandOperand()));
		}
		else if (el instanceof CtBlock) {
			CtBlock cat = (CtBlock) ccat;
			List<CtStatement> list = new ArrayList<CtStatement>();
			for (CtStatement ct : cat.getStatements()) {
				list.add((CtStatement) clone(ct));
			}
			cat.setStatements(list);
		}
		else if (el instanceof CtCase) {
			CtCase cat = (CtCase) ccat;
			cat.setCaseExpression((CtExpression) clone(cat.getCaseExpression()));
			List<CtStatement> list = new ArrayList<CtStatement>();
			for (CtStatement ct : cat.getStatements()) {
				list.add((CtStatement) clone(ct));
			}
			cat.setStatements(list);
		}
		else if (el instanceof CtCatch) {
			CtCatch cat = (CtCatch) ccat;
			cat.setBody((CtBlock<?>) clone(cat.getBody()));
		}
		else if (el instanceof CtClass) {
			CtClass cat = (CtClass) ccat;
			Set<Object> list = new HashSet<Object>();
			for (Object ct : cat.getConstructors()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setConstructors(list);
			list = new HashSet<Object>();
			for (Object ct : cat.getMethods()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setMethods(list);
			list = new HashSet<Object>();
			for (Object ct : cat.getFields()) {
				list.add((Object) clone((CtElement) ct));
				cat.removeField((CtField) ct);
			}
			for (Object ct: list) {
				cat.addField((CtField) ct);
			}
			// Probably missing sth
		}
		else if (el instanceof CtConditional) {
			CtConditional cat = (CtConditional) ccat;
			cat.setCondition((CtExpression) clone(cat.getCondition()));
			cat.setThenExpression((CtExpression) clone(cat.getThenExpression()));
			cat.setElseExpression((CtExpression) clone(cat.getElseExpression()));
		}
		else if (el instanceof CtConstructor) {
			CtConstructor cat = (CtConstructor) ccat;
			cat.setBody((CtBlock) clone(cat.getBody()));
			
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getParameters()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setParameters(list);
		}
		else if (el instanceof CtDo) {
			CtDo cat = (CtDo) ccat;
			cat.setBody((CtStatement) clone(cat.getBody()));
			cat.setLoopingExpression((CtExpression<Boolean>) clone(cat.getLoopingExpression()));
		}
		else if (el instanceof CtEnum) {
			// TODO
		}
		else if (el instanceof CtExecutableReference) {
			CtExecutableReference cat = (CtExecutableReference) ccat;
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getParameters()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setParameters(list);
		}
		else if (el instanceof CtField) {
			CtField cat = (CtField) ccat;
			cat.setAssignment((CtExpression) clone(cat.getAssignment()));
			cat.setDefaultExpression((CtExpression) clone(cat.getDefaultExpression()));
		}
		else if (el instanceof CtFor) {
			CtFor cat = (CtFor) ccat;
			cat.setBody((CtStatement) clone(cat.getBody()));
			cat.setExpression((CtExpression<Boolean>) clone(cat.getExpression()));
			List<CtStatement> list = new ArrayList<CtStatement>();
			for (CtStatement ct : cat.getForInit()) {
				list.add((CtStatement) clone(ct));
			}
			cat.setForInit(list);
			list = new ArrayList<CtStatement>();
			for (CtStatement ct : cat.getForUpdate()) {
				list.add((CtStatement) clone(ct));
			}
			cat.setForUpdate(list);
		}
		else if (el instanceof CtForEach) {
			CtForEach cat = (CtForEach) ccat;
			cat.setBody((CtStatement) clone(cat.getBody()));
			cat.setExpression((CtExpression<?>) clone(cat.getExpression()));
			cat.setVariable((CtLocalVariable<?>) clone(cat.getVariable()));
		}
		else if (el instanceof CtIf) {
			CtIf cat = (CtIf) ccat;
			cat.setCondition((CtExpression<Boolean>) clone(cat.getCondition()));
			cat.setThenStatement((CtStatement) clone(cat.getThenStatement()));
			cat.setElseStatement((CtStatement) clone(cat.getElseStatement()));
		}
		else if (el instanceof CtInterface) {
			CtInterface cat = (CtInterface) ccat;
			Set<Object> list = new HashSet<Object>();
			for (Object ct : cat.getMethods()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setMethods(list);
			list = new HashSet<Object>();
			for (Object ct : cat.getFields()) {
				list.add((Object) clone((CtElement) ct));
				cat.removeField((CtField) ct);
			}
			for (Object ct: list) {
				cat.addField((CtField) ct);
			}
			// Probably missing sth
		}
		else if (el instanceof CtInvocation) {
			CtInvocation cat = (CtInvocation) ccat;
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getArguments()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setArguments(list);
			cat.setTarget((CtExpression) clone(cat.getTarget()));
		}
		else if (el instanceof CtLiteral) {
			//CtLiteral cat = (CtLiteral) ccat;
			// value?
		}
		else if (el instanceof CtLocalVariable) {
			CtLocalVariable cat = (CtLocalVariable) ccat;
			cat.setAssignment((CtExpression) clone(cat.getAssignment()));
			cat.setDefaultExpression((CtExpression) clone(cat.getDefaultExpression()));
		}
		else if (el instanceof CtCatchVariable) {
			CtCatchVariable cat = (CtCatchVariable) ccat;
			cat.setDefaultExpression((CtExpression) clone(cat.getDefaultExpression()));
		}
		else if (el instanceof CtMethod) {
			CtMethod cat = (CtMethod) ccat;
			cat.setBody((CtBlock) clone(cat.getBody()));
			List<CtTypeReference<?>> list = new ArrayList<CtTypeReference<?>>();
			for (Object ct : cat.getFormalTypeParameters()) {
				list.add((CtTypeReference<?>) clone((CtElement) ct));
			}
			cat.setFormalTypeParameters(list);
		}
		else if (el instanceof CtNewArray) {
			CtNewArray cat = (CtNewArray) ccat;
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getDimensionExpressions()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setDimensionExpressions(list);
			list = new ArrayList<Object>();
			for (Object ct : cat.getElements()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setElements(list);
		}
		else if (el instanceof CtLambda) {
			CtLambda cat = (CtLambda) ccat;
			if (cat.getBody() != null) cat.setBody((CtBlock) clone(cat.getBody()));
			if (cat.getExpression() != null) cat.setExpression((CtExpression) clone(cat.getExpression()));
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getParameters()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setParameters(list);
		}
		else if (el instanceof CtConstructorCall) {
			CtConstructorCall cat = (CtConstructorCall) ccat;
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getArguments()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setArguments(list);
			if (cat.getType() == null) {
				throw new RuntimeException("null constructor: " + el);
			}
		}
		else if (el instanceof CtOperatorAssignment) {
			CtOperatorAssignment cat = (CtOperatorAssignment) ccat;
			cat.setAssigned((CtExpression) clone(cat.getAssigned()));
			cat.setAssignment((CtExpression) clone(cat.getAssignment()));
		}
		else if (el instanceof CtReturn) {
			CtReturn cat = (CtReturn) ccat;
			cat.setReturnedExpression((CtExpression) clone(cat.getReturnedExpression()));
		}
		else if (el instanceof CtStatementList) {
			CtStatementList cat = (CtStatementList) ccat;
			List<CtStatement> list = new ArrayList<CtStatement>();
			for (CtStatement ct : cat.getStatements()) {
				list.add((CtStatement) clone(ct));
			}
			cat.setStatements(list);
		}
		else if (el instanceof CtSwitch) {
			CtSwitch cat = (CtSwitch) ccat;
			cat.setSelector((CtExpression) clone(cat.getSelector()));
			List<Object> list = new ArrayList<Object>();
			for (Object ct : cat.getCases()) {
				list.add((Object) clone((CtElement) ct));
			}
			cat.setCases(list);
		}
		else if (el instanceof CtSynchronized) {
			CtSynchronized cat = (CtSynchronized) ccat;
			cat.setExpression((CtExpression<?>) clone(cat.getExpression()));
			cat.setBlock((CtBlock<?>) clone(cat.getBlock()));
		}
		else if (el instanceof CtThrow) {
			CtThrow cat = (CtThrow) ccat;
			cat.setThrownExpression((CtExpression<? extends Throwable>) clone(cat.getThrownExpression()));
		}
		else if (el instanceof CtTry) {
			CtTry cat = (CtTry) ccat;
			cat.setBody((CtBlock<?>) clone(cat.getBody()));
			if (cat.getFinalizer() != null)
				cat.setFinalizer((CtBlock<?>) clone(cat.getFinalizer()));
			List<CtCatch> list = new ArrayList<CtCatch>();
			for (Object ct : cat.getCatchers()) {
				list.add((CtCatch) clone((CtElement) ct));
			}
			cat.setCatchers(list);
		}
		else if (el instanceof CtTryWithResource) {
			CtTryWithResource cat = (CtTryWithResource) ccat;
			cat.setBody((CtBlock<?>) clone(cat.getBody()));
			cat.setFinalizer((CtBlock<?>) clone(cat.getFinalizer()));
			List<CtCatch> list = new ArrayList<CtCatch>();
			for (Object ct : cat.getCatchers()) {
				list.add((CtCatch) clone((CtElement) ct));
			}
			cat.setCatchers(list);
			List<CtLocalVariable<?>> list2 = new ArrayList<CtLocalVariable<?>>();
			for (Object ct : cat.getResources()) {
				list2.add((CtLocalVariable) clone((CtElement) ct));
			}
			cat.setResources(list2);
		}
		else if (el instanceof CtUnaryOperator) {
			CtUnaryOperator cat = (CtUnaryOperator) ccat;
			cat.setOperand((CtExpression<?>) clone(cat.getOperand()));
		}
		else if (el instanceof CtWhile) {
			CtWhile cat = (CtWhile) ccat;
			cat.setBody((CtStatement) clone(cat.getBody()));
			cat.setLoopingExpression((CtExpression<Boolean>) clone(cat.getLoopingExpression()));
		}
		return ccat;
	}
}
