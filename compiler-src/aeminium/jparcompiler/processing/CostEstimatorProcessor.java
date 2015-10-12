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
	public static HashMap<String, Long> basicCosts = new HashMap<>();
	public static CostModelVisitor visitor = new CostModelVisitor();
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void process(CtMethod<T> meth) {
		try {  
		    FileInputStream fis = new FileInputStream("benchmark.data");
		    ObjectInputStream ois = new ObjectInputStream(fis);
			CostEstimatorProcessor.basicCosts = (HashMap<String,Long>) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		meth.accept(visitor);
	}

	@Override
	public void processingDone() {
		super.processingDone();
		database = visitor.database;
	}
}
