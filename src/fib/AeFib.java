package fib;

import java.util.Arrays;

import aeminium.runtime.Body;
import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.Sequential;


@Sequential
public class AeFib {
	
	public static <T> FBody<T> createTask(FBody<T> b, Task... ts) {
		Task t = RuntimeManager.rt.createNonBlockingTask(b, Runtime.NO_HINTS);
		b.setTask(t);
		RuntimeManager.rt.schedule(t, Runtime.NO_PARENT, Arrays.asList(ts));
		return b;
	}
	
	public static <T> FBody<T> createTask(FBody<T> b) {
		Task t = RuntimeManager.rt.createNonBlockingTask(b, Runtime.NO_HINTS);
		b.setTask(t);
		RuntimeManager.rt.schedule(t, Runtime.NO_PARENT, Runtime.NO_DEPS);
		return b;
	}
	
	public static long parFib(long n) {
		return createTask(new parFibBody(n)).get();
	}
	
	public static abstract class FBody<T> implements Body{
		Task t;
		T ret;
		
		public T get() {
			t.getResult();
			return ret;
		}
		
		public void setTask(Task t) {
			this.t = t;
		}
		
		public void setResult(T res) {
			ret = res;
		}
 	}
	
	public static long par_parFib(long n) {
		if (n <= 2) return 1;
		FBody<Long> t1 = createTask(new parFibBody(n-1));
		FBody<Long> t2 = createTask(new parFibBody(n-2));
		return t1.get() + t2.get();
	}
	
	
	public static long seq_parFib(long n) {
		if (n <= 2) return 1;
		else return (seq_parFib(n - 1) + seq_parFib(n - 2));
	}
	
	
	public static class parFibBody extends FBody<Long> {
		long a;
		public parFibBody(long a) {
			this.a = a;
		}
		
		@Override
		public void execute(Runtime rt, Task current) throws Exception {
			this.setResult((rt.parallelize(current)) ? par_parFib(a) : seq_parFib(a));
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
