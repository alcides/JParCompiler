package aeminium.jparcompiler.model;

import java.util.HashMap;

public class CostEstimation {
	public int instructions = 0;
	public HashMap<String, Integer> dependencies = new HashMap<>();
	public HashMap<String, CostEstimation> complexDependencies = new HashMap<>();
	public int expressionCost = 0;
	public String expressionString;
	public boolean isExpressionComplex = false;
	
	
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
		for (String k : complexDependencies.keySet()) {
			b.append("+" + k + "*(" + complexDependencies.get(k) + ")");
		}
		return b.toString();
	}
	
	public void addComplex(String n, CostEstimation inside) {
		complexDependencies.put(n, inside);
	}

	public long apply(HashMap<String, Long> map) {
		StringBuilder sb = new StringBuilder();
		boolean hasPrevious = false;
		for (String k : dependencies.keySet()) {
			if (map.containsKey(k)) {
				expressionCost += map.get(k) * dependencies.get(k);
			} else {
				//System.out.println("unknown: " + k);
			}
		}
		for (String k : complexDependencies.keySet()) {
			boolean rowNumerical = true;
			long val = complexDependencies.get(k).apply(map);
			long mult = 1;
			if (val == -1) rowNumerical = false;			
			try {
				mult = Long.parseLong(k);
			} catch (NumberFormatException e) {
				rowNumerical = false;
			}
			if (rowNumerical) {
				expressionCost += mult * val;
			} else {
				isExpressionComplex = true;
				if (hasPrevious) {
					sb.append("+");
				}
				sb.append("(" + k + "*" + complexDependencies.get(k).expressionString + ")");
				hasPrevious = true;
			}
		}
		if (hasPrevious) {
			sb.append("+");
		}
		sb.append(expressionCost);
		expressionString = sb.toString();
		if (isExpressionComplex) {
			return -1;
		}
		return expressionCost;
	}
}
