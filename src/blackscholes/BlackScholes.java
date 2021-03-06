package blackscholes;

import aeminium.runtime.futures.codegen.Sequential;

/*************************************************************************
 * Compilation: javac java MyMath.java Execution: java BlackScholes S X r sigma
 * T
 * 
 * Reads in five command line inputs and calculates the option price according
 * to the Black-Scholes formula.
 * 
 * % java BlackScholes 23.75 15.00 0.01 0.35 0.5 8.879159279691955 (actual =
 * 9.10)
 * 
 * % java BlackScholes 30.14 15.0 0.01 0.332 0.25 15.177462481562186 (actual =
 * 14.50)
 * 
 * 
 * Information calculated based on closing data on Monday, June 9th 2003.
 * 
 * Microsoft: share price: 23.75 strike price: 15.00 risk-free interest rate: 1%
 * volatility: 35% (historical estimate) time until expiration: 0.5 years
 * 
 * GE: share price: 30.14 strike price: 15.00 risk-free interest rate 1%
 * volatility: 33.2% (historical estimate) time until expiration 0.25 years
 * 
 * 
 * Reference: http://www.hoadley.net/options/develtoolsvolcalc.htm
 * 
 *************************************************************************/

public class BlackScholes {

	public final static double S = 23.75;
	public final static double X = 15.00;
	public final static double r = 0.01;
	public final static double sigma = 0.35;
	public final static double T = 0.5;

	// Black-Scholes formula
	public static double callPrice(double S, double X, double r, double sigma,
			double T) {
		double d1 = (Math.log(S / X) + (r + sigma * sigma / 2) * T)
				/ (sigma * Math.sqrt(T));
		double d2 = d1 - sigma * Math.sqrt(T);
		return S * Gaussian.Phi(d1) - X * Math.exp(-r * T) * Gaussian.Phi(d2);
	}

	
	public static double estimation1(double S, double X, double r, double sigma, double T) {
		double eps = StdRandom.gaussian();
		double price = S
				* Math.exp(r * T - 0.5 * sigma * sigma * T + sigma * eps
						* Math.sqrt(T));
		return Math.max(price - X, 0);
	}
	
	// estimate by Monte Carlo simulation
	public static double call(double S, double X, double r, double sigma,
			double T, long N) {
		double sum = 0.0;
		for (int i = 0; i < N; i++) {
			double est = estimation1(S, X, r, sigma, T);
			sum += est;
		}
		double mean = sum / N;
		return Math.exp(-r * T) * mean;
	}

	
	public static double estimation2(double S, double X, double r, double sigma, double T) {
		double price = S;
		double dt = T /10000.0;
		for (double t = 0; t <= 10000; t++) {
			price += r * price * dt + sigma * price * Math.sqrt(dt)
					* StdRandom.gaussian();
		}
		return Math.max(price - X, 0);
	}
	
	// estimate by Monte Carlo simulation
	public static double call2(double S, double X, double r, double sigma,
			double T, long N) {
		double sum = 0.0;
		for (int i = 0; i < N; i++) {
			sum += estimation2(S, X, r, sigma, T);
		}
		double mean = sum / N;
		return Math.exp(-r * T) * mean;
	}

	
	public static void run(int N) {
		double cP = callPrice(S, X, r, sigma, T);
		double ca = call(S, X, r, sigma, T, N);
		double c2 = call2(S, X, r, sigma, T, N);
		System.out.println(cP);
		System.out.println(ca);
		System.out.println(c2);
	}
	
	@Sequential
	public static void main(String[] args) {

		int N = 3000;
		if (args.length > 0) N = Integer.parseInt(args[0]);		
		long t = System.nanoTime();
		run(N);
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
	}
}
