package aeminium.jparcompiler.launchers;

import aeminium.jparcompiler.processing.AccessPermissionsProcessor;
import aeminium.jparcompiler.processing.CostEstimatorProcessor;
import aeminium.jparcompiler.processing.SeqMethodProcessor;
import aeminium.jparcompiler.processing.TaskCreationProcessor;
import spoon.Launcher;

public class JParLauncher {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception {
		Launcher lr = new Launcher();
		lr.addProcessor(new AccessPermissionsProcessor());
		lr.addProcessor(new CostEstimatorProcessor());
		lr.addProcessor(new SeqMethodProcessor());
		lr.addProcessor(new TaskCreationProcessor());
		lr.run(args);
	}
	
}
