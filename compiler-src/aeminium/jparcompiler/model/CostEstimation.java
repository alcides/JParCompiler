package aeminium.jparcompiler.model;

import java.util.HashMap;

public class CostEstimation {
	public int instructions = 0;
	public HashMap<String, Integer> dependencies = new HashMap<>();
	public HashMap<String, CostEstimation> complexDependencies = new HashMap<>();
	public int cost = 0;
	
	
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
		for (String k : dependencies.keySet()) {
			if (map.containsKey(k)) {
				cost += map.get(k) * dependencies.get(k);
			} else {
				//System.out.println("unknown: " + k);
			}
		}
		for (String k : complexDependencies.keySet()) {
			try {
				long mult = Long.parseLong(k);
				long val = complexDependencies.get(k).apply(map);
				cost += mult * val;
				if (val == -1) {
					cost = -1;
					break;
				}
			} catch (NumberFormatException e) {
				cost = -1;
				break;
			}
		}
		return cost;
	}
}
