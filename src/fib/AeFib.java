package fib;

import java.util.Arrays;

import aeminium.runtime.Body;
import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.Sequential;


@Sequential
public class AeFib {
	
	public abstract static class FBody<T> implements Body {
		@Override
		public void execute(Runtime rt, Task current) {
			current.setResult(evaluate(current));
		}
		public abstract T evaluate(Task t);
	}
	
	public static Task createTask(Body b, Task... ts) {
		Task t = RuntimeManager.rt.createNonBlockingTask(b, Runtime.NO_HINTS);
		RuntimeManager.rt.schedule(t, Runtime.NO_PARENT, Arrays.asList(ts));
		return t;
	}
	
	public static Task createTask(Body b) {
		Task t = RuntimeManager.rt.createNonBlockingTask(b, Runtime.NO_HINTS);
		RuntimeManager.rt.schedule(t, Runtime.NO_PARENT, Runtime.NO_DEPS);
		return t;
	}
	
	public static long seqFib(long n) {
		if (n <= 2) return 1;
		else return (seqFib(n - 1) + seqFib(n - 2));
	}
	
	public static long parFib(long n) {
		if (RuntimeManager.shouldSeq()) {
			return seqFib(n);
		}
		if (n <= 2) return 1;
		Task t1 = createTask(new parFibBody(n-1));
		Task t2 = createTask(new parFibBody(n-1));
		long v1 = (long) t1.getResult();
		long v2 = (long) t2.getResult();
		return v1 + v2;
	}
	
	
	public static class parFibBody implements Body {

		long a;
		public parFibBody(long a) {
			this.a = a;
		}
		
		@Override
		public void execute(Runtime rt, Task current) throws Exception {
			current.setResult(parFib(a));
		}
	}
	

	public static void main(String[] args) {

		int fib = 20;
		if (args.length > 0) {
			fib = Integer.parseInt(args[0]);
		}

		RuntimeManager.init();
		long t = System.nanoTime();
		long v = parFib(fib);
		java.lang.System.out.println(("% " + (((double)(((System.nanoTime()) - t))) / ((1000 * 1000) * 1000))));
		System.out.println("R: " + v);
		RuntimeManager.shutdown();
		
		

	}
}
