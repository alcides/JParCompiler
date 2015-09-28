package aeminium.jparcompiler.processing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;

import aeminium.jparcompiler.model.CostEstimation;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

public class CostEstimatorProcessor<T> extends AbstractProcessor<CtMethod<T>>  {
	public static HashMap<CtElement, CostEstimation> database;
	CostModelVisitor p = new CostModelVisitor();
	HashMap<String, Long> basicCosts = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	@Override
	public void process(CtMethod<T> meth) {
		try {  
		    FileInputStream fis = new FileInputStream("benchmark.data");
		    ObjectInputStream ois = new ObjectInputStream(fis);
			basicCosts = (HashMap<String,Long>) ois.readObject();

		     ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		meth.accept(p);
	}

	@Override
	public void processingDone() {
		super.processingDone();
		database = p.database;
		for (CtElement e : database.keySet()) {
			CostEstimation ce = database.get(e);
			System.out.println("Cost: " + ce.apply(basicCosts));
		}
	}
}
