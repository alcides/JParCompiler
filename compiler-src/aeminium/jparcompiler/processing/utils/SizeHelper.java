package aeminium.jparcompiler.processing.utils;

import spoon.reflect.reference.CtTypeReference;

public class SizeHelper {

	//@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int getSizeOf(CtTypeReference<?> type) {
		
		/*
		if(type instanceof CtArrayTypeReference)  {
			CtArrayTypeReference<?> aType = (CtArrayTypeReference<?>) type;
			MemoryModel mm1 = new MemoryModel(type.getFactory());
			modelSizeOf(aType.getComponentType(), null, mm1, recLevel+1);
			if (par == null) {
				mm.add(400); // TODO: average value;
			} else {
				CtBinaryOperator<?> mul = type.getFactory().Core().createBinaryOperator();
				mul.setKind(BinaryOperatorKind.PLUS);
				mul.setLeftHandOperand(mm1.getExpression());
				CtVariableRead parRead = mm.factory.Core().createVariableRead();
				parRead.setVariable(par.getReference());
				
				Factory f = new FactoryImpl(new DefaultCoreFactory(),new StandardEnvironment());
				
				CtInvocation<?> exp = f.Code().createCodeSnippetStatement("System.out.println(new int[1].length); ").compile();
				CtFieldRead frt = (CtFieldRead) exp.getArguments().get(0);
				CtFieldRead fr = mm.factory.Core().createFieldRead();
				fr.setTarget(parRead);
				fr.setVariable(frt.getVariable());
				mul.setRightHandOperand(fr);
				mm.addExpression(mul);
			}
			return;
		}
		*/
		
		if (type.isPrimitive()) {
			if (type.getQualifiedName().equals("boolean")) return 1;
			if (type.getQualifiedName().equals("byte")) return 1;
			if (type.getQualifiedName().equals("short")) return 2;
			if (type.getQualifiedName().equals("char")) return 2;
			if (type.getQualifiedName().equals("int")) return 4;
			if (type.getQualifiedName().equals("float")) return 4;
			if (type.getQualifiedName().equals("double")) return 8;
			if (type.getQualifiedName().equals("long")) return 8;
			return 8;
		}
		if (type.getQualifiedName().equals("String")) {
			return 16;
		}
		return 8;
		/*
		if (type.getDeclaration() != null) {
			for (CtFieldReference<?> f : type.getDeclaration().getDeclaredFields()) {
				modelSizeOf(f.getType(), null, mm, recLevel+1);
			}	
			return;
		}
		
		if (type.toString().startsWith("java.util.List")) {
			if (par != null) {
				CtTypeReference aType = type.getActualTypeArguments().get(0);
				
				MemoryModel mm1 = new MemoryModel(type.getFactory());
				modelSizeOf(aType, null, mm1, recLevel+1);
				
				
				CtBinaryOperator<?> mul = type.getFactory().Core().createBinaryOperator();
				mul.setKind(BinaryOperatorKind.PLUS);
				mul.setLeftHandOperand(mm1.getExpression());
				CtVariableRead parRead = mm.factory.Core().createVariableRead();
				parRead.setVariable(par.getReference());
				
				Factory f = new FactoryImpl(new DefaultCoreFactory(),new StandardEnvironment());
				
				CtInvocation<?> exp = f.Code().createCodeSnippetStatement("System.out.println(new java.util.ArrayList().size()); ").compile();
				CtInvocation invt = (CtInvocation) exp.getArguments().get(0);
				CtInvocation inv = mm.factory.Code().createInvocation(parRead, invt.getExecutable(), new ArrayList());
				mul.setRightHandOperand(inv);
				mm.addExpression(mul);
			} else {
				System.out.println("This is a field." + type);
			}
		}
		System.out.println("Type:" + type + " not found");
		*/
	}
}
