package aeminium.jparcompiler.launchers;

import aeminium.jparcompiler.processing.CostEstimatorProcessor;
import aeminium.jparcompiler.processing.IfPrinterProcessor;
import aeminium.jparcompiler.processing.IteratePrinterProcessor;
import spoon.Launcher;

public class GranularityDeciderLauncher {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception {
		Launcher lr = new Launcher();
		lr.addProcessor(new CostEstimatorProcessor());
		lr.addProcessor(new IfPrinterProcessor());
		lr.addProcessor(new IteratePrinterProcessor());
		lr.run(args);
	}

}