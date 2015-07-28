package code;


public class Fib {
	public static void main(String[] args) {
		int f = fib(10);
		System.out.println(f);
	}
	
	public static int fib(int n) {
		if (n <= 2) return 1;
		int a = fib(n-1);
		int b = fib(n-2);
		return a + b;
	}
}
