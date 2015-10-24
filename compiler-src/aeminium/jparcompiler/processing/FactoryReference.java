package aeminium.jparcompiler.processing;

import spoon.reflect.factory.Factory;

public class FactoryReference {
	public static Factory f;
	public static void setFactory(Factory f) {
		FactoryReference.f = f;
	}
	
	public static Factory getFactory() {
		if (f != null) {
			return f;
		} else {
			System.out.println("Factory is not defined!!!!");
			return null;
		}
	}
}
