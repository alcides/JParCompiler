package fib;


public class Fib {
	public static void main(String[] args) {
		
		int size = 47;
		if (args.length > 0) {
			size = Integer.parseInt(args[0]);
		}
		
		long t = System.nanoTime();
		int f = fib(size);
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		System.out.println(f);
	}
	
	public static int fib(int n) {
		if (n <= 2) return 1;
		return fib(n-1) + fib(n-2);
	}
}
