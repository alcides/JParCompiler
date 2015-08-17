package fib;


public class Fib2 {
    public static void aeminium_seq_main(java.lang.String[] args) {
        int size = 47;
        if ((args.length) > 0) {
            size = java.lang.Integer.parseInt(args[0]);
        } 
        long t = java.lang.System.nanoTime();
        int f = fib.Fib.fib(size);
        java.lang.System.out.println(("% " + (((double)(((java.lang.System.nanoTime()) - t))) / ((1000 * 1000) * 1000))));
        java.lang.System.out.println(f);
    }

    public static void main(java.lang.String[] args) {
        aeminium.runtime.futures.RuntimeManager.init();
        int size = 47;
        if ((args.length) > 0) {
            size = java.lang.Integer.parseInt(args[0]);
        } 
        aeminium.runtime.futures.FBody aeminium_task_14 = aeminium.runtime.futures.RuntimeManager.createTask(new fib.Fib2.Future_fib(size));
        long t = java.lang.System.nanoTime();
        int f = ((int)(aeminium_task_14.get()));
        java.lang.System.out.println(("% " + (((double)(((java.lang.System.nanoTime()) - t))) / ((1000 * 1000) * 1000))));
        java.lang.System.out.println(f);
        aeminium.runtime.futures.RuntimeManager.shutdown();
    }

    public static int aeminium_seq_fib(int n) {
        if (n <= 2)
            return 1;
        
        return (fib.Fib.fib((n - 1))) + (fib.Fib.fib((n - 2)));
    }

    public static int fib(int n) {
        if (n <= 2)
            return 1;
        
        aeminium.runtime.futures.FBody aeminium_task_13 = aeminium.runtime.futures.RuntimeManager.createTask(new fib.Fib2.Future_fib((n - 2)));
        aeminium.runtime.futures.FBody aeminium_task_12 = aeminium.runtime.futures.RuntimeManager.createTask(new fib.Fib2.Future_fib((n - 1)));
        return ((int)(aeminium_task_12.get())) + ((int)(aeminium_task_13.get()));
    }

    @aeminium.runtime.futures.codegen.NoVisit
    static class Future_fib extends aeminium.runtime.futures.FBody<java.lang.Integer> {
        public Future_fib(int n) {
            n_ae = n;
        }

        int n_ae;

        public void execute(aeminium.runtime.Runtime aeRuntime, aeminium.runtime.Task aeTask) throws java.lang.Exception {
        	ret = (aeRuntime.parallelize(aeTask) ? fib(n_ae) : aeminium_seq_fib(n_ae));
        }
    }
}
