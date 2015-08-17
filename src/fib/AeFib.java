package fib;

import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.futures.FBody;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.Sequential;


@Sequential
public class AeFib {
	
	
	public static class Future_parFib extends FBody<Long> {
		long a;
		public Future_parFib(long a) {
			this.a = a;
		}
		
		@Override
		public void execute(Runtime rt, Task current) throws Exception {
			this.setResult((rt.parallelize(current)) ? fib(a) : seq_parFib(a));
		}
	}
	
	public static long fib(long n) {
		if (n <= 2) return 1;
		FBody<Long> t1 = RuntimeManager.createTask(new Future_parFib(n-1));
		FBody<Long> t2 = RuntimeManager.createTask(new Future_parFib(n-2));
		return t1.get() + t2.get();
	}
	
	
	public static long seq_parFib(long n) {
		if (n <= 2) return 1;
		else return (seq_parFib(n - 1) + seq_parFib(n - 2));
	}
	

	public static void main(String[] args) {

		int fib = 20;
		if (args.length > 0) {
			fib = Integer.parseInt(args[0]);
		}

		RuntimeManager.init();
		long t = System.nanoTime();
		long v = fib(fib);
		java.lang.System.out.println(("% " + (((double)(((System.nanoTime()) - t))) / ((1000 * 1000) * 1000))));
		System.out.println("R: " + v);
		RuntimeManager.shutdown();
		
		

	}
}
