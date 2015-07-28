package code;


public class Fib {
	public static void main(String[] args) {
		int f = fib(30);
		System.out.println(f);
	}
	
	public static int fib(int n) {
		if (n <= 2) return 1;
		return fib(n-1) + fib(n-2);
	}
}
