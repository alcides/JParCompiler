package fib;

import aeminium.runtime.Body;
import aeminium.runtime.Hints;
import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.futures.RuntimeManager;
import aeminium.runtime.futures.codegen.Sequential;


@Sequential
public class AeFib {

	public static class FibBody implements Body {
		public volatile long value;

		public FibBody(long n) {
			this.value = n;
		}

		public long seqFib(long n) {
			if (n <= 2) return 1;
			else return (seqFib(n - 1) + seqFib(n - 2));
		}

		@Override
		public void execute(Runtime rt, Task current) {
			if (!rt.parallelize(current)) {
				value = seqFib(value);
			} else {
				if (value <= 2) {
					value = 1;
					return;
				}

				FibBody b1 = new FibBody(value - 1);
				Task t1 = rt.createNonBlockingTask(b1, Hints.RECURSION);
				rt.schedule(t1, Runtime.NO_PARENT, Runtime.NO_DEPS);

				FibBody b2 = new FibBody(value - 2);
				Task t2 = rt.createNonBlockingTask(b2, Hints.RECURSION);
				rt.schedule(t2, Runtime.NO_PARENT, Runtime.NO_DEPS);

				t1.getResult();
				t2.getResult();
				value = b1.value + b2.value;
			}
		}
	}

	public static void main(String[] args) {

		int fib = Integer.parseInt(args[0]);

		RuntimeManager.init();
		long t = System.nanoTime();
		FibBody body = new AeFib.FibBody(fib);
		Task t1 = RuntimeManager.rt.createNonBlockingTask(body, Runtime.NO_HINTS);
		RuntimeManager.rt.schedule(t1, Runtime.NO_PARENT, Runtime.NO_DEPS);
		t1.getResult();
		java.lang.System.out.println(("% " + (((double)(((System.nanoTime()) - t))) / ((1000 * 1000) * 1000))));
		System.out.println("R: " + body.value);
		RuntimeManager.shutdown();
		
		

	}
}
