package aeminium.jparcompiler.processing;

import java.util.HashMap;

import aeminium.jparcompiler.model.CostEstimation;
import spoon.reflect.declaration.CtElement;

public class CostModelFixer extends CostModelVisitor {
	
	CostModelFixer(HashMap<CtElement, CostEstimation> db) {
		super();
		this.database = db;
	}
	
	public CostEstimation save(CtElement e, CostEstimation ce) {
		if (e != null) {
			if (!database.containsKey(e)) {
				return database.put(e, ce);
			}
		}
		return database.get(e);
	}
}
