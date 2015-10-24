package aeminium.jparcompiler.model;

import java.util.HashMap;

import aeminium.jparcompiler.processing.CostEstimatorProcessor;
import aeminium.jparcompiler.processing.FactoryReference;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;

public class CostEstimation {
	public int instructions = 0;
	public HashMap<String, Integer> dependencies = new HashMap<>();
	public HashMap<CtExpression<?>, CostEstimation> complexDependencies = new HashMap<>();
	public boolean isExpressionComplex = false;
	public long simpleCost = 0;
	
	
	public void add(int i) {
		instructions += i;
	}
	
	public void add(String el, int i) {
		if (dependencies.containsKey(el)) {
			int v = dependencies.get(el);
			dependencies.put(el, v+i);
		} else {
			dependencies.put(el, i);
		}
	}
	
	public void add(CostEstimation other) {
		this.instructions += other.instructions;
		for (String k : other.dependencies.keySet()) {
			this.add(k, other.dependencies.get(k));
		}
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(instructions);
		for (String k : dependencies.keySet()) {
			b.append("+" + k + "*" + dependencies.get(k));
		}
		for (CtElement k : complexDependencies.keySet()) {
			b.append("+" + k.toString() + "*(" + complexDependencies.get(k) + ")");
		}
		return b.toString();
	}
	
	@SuppressWarnings("rawtypes")
	public void addComplex(CtExpression e, CostEstimation inside) {
		complexDependencies.put(e, inside);
	}
	
	@SuppressWarnings("rawtypes")
	public CtExpression getExpressionNode() {
		if (CostEstimatorProcessor.basicCosts.isEmpty()) {
			throw new RuntimeException("Basic Costs not calculated yet.");
		}
		HashMap<String, Long> map = CostEstimatorProcessor.basicCosts; 
		simpleCost = instructions;
		for (String k : dependencies.keySet()) {
			if (map.containsKey(k)) {
				simpleCost += map.get(k) * dependencies.get(k);
			} else {
				//System.out.println("unknown: " + k);
			}
		}
		CtExpression ret = null;
		for (CtExpression k : complexDependencies.keySet()) {
			CtExpression val = complexDependencies.get(k).getExpressionNode();
			try {
				long multiplier = Long.parseLong(k.toString());
				long multiplied = Long.parseLong(val.toString());
				simpleCost += multiplier * multiplied;
			} catch (NumberFormatException e) {
				isExpressionComplex = true;
				CtExpression row = this.merge(k, val, BinaryOperatorKind.MUL);
				if (ret == null) ret = row;
				else ret = this.merge(ret, row, BinaryOperatorKind.PLUS);
				
			}
		}
		CtExpression other = FactoryReference.getFactory().Code().createLiteral(simpleCost);
		if (ret == null) ret = other;
		else ret = this.merge(ret, other, BinaryOperatorKind.PLUS);
		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	public CtExpression merge(CtExpression a, CtExpression b, BinaryOperatorKind k) {
		CtBinaryOperator<?> bin = a.getFactory().Core().createBinaryOperator();
		bin.setKind(k);
		bin.setLeftHandOperand(a);
		bin.setRightHandOperand(b);
		return bin;
	}
}
