package aeminium.jparcompiler.processing;

import aeminium.jparcompiler.processing.utils.CopyCatFactory;
import aeminium.jparcompiler.processing.utils.Safety;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

public class SeqMethodProcessor extends AbstractProcessor<CtMethod<?>> {
	
	public static String SEQ_PREFIX = "aeminium_seq_";

	@Override
	public void process(CtMethod<?> m) {
		if (!Safety.isSafe(m)) {
			CtMethod<?> seq = (CtMethod<?>) CopyCatFactory.clone(m);
			seq.setSimpleName(SEQ_PREFIX + seq.getSimpleName());
			CtClass<?> cl = (CtClass<?>) m.getParent();
			cl.addMethod(seq);
			
		}
	}

}
