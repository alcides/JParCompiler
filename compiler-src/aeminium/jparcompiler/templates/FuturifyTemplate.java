package aeminium.jparcompiler.templates;

import spoon.template.StatementTemplate;
import spoon.template.Local;
import spoon.template.Parameter;
import aeminium.runtime.futures.Future;

public class FuturifyTemplate extends StatementTemplate {
	
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
	@SuppressWarnings("unused")
	public void statement() throws Throwable {
		Future<_Type_> _code_ = new Future<_Type_>((t) -> null);
		_Type_ a = _code_.get();
	}
}

interface _Type_ {
}