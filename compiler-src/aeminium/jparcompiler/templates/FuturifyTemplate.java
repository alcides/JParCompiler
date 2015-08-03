package aeminium.jparcompiler.templates;

import spoon.template.Local;
import spoon.template.Parameter;
import aeminium.runtime.futures.Future;
import aeminium.runtime.futures.codegen.Sequential;

@Sequential
public class FuturifyTemplate extends spoon.template.BlockTemplate {
	
	@Parameter()
	Class<?> _Type_;
	
	@Parameter()
	String _code_;

	@Local
	public FuturifyTemplate(Class<?> t, String c) {
		_Type_ = t;
		_code_ = c;
	}

	@Override
	public void block() throws Throwable {
		Future<_Type_> _code_ = new Future<_Type_>((t) -> null);
		_Type_ a = _code_.get();
	}

}

interface _Type_ {
}