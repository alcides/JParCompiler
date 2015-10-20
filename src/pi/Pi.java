package pi;

import java.util.Random;

import aeminium.runtime.futures.codegen.Sequential;

public class Pi {
	
	public static final int DEFAULT_DART_SIZE = 100000000;
	
	@Sequential
	public static void main(String[] args) {
		
		int dartsc = DEFAULT_DART_SIZE;
		if (args.length > 0) dartsc = Integer.parseInt(args[0]);
		
		long t = System.nanoTime();
		double d = pi(dartsc);
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		System.out.println("PI = " + d);
	}
	
	public static double pi(int dartsc) {
		Random random = new Random();
		long score = 0;
		for (long n = 0; n < dartsc; n++) {
			/* generate random numbers for x and y coordinates */
			double x_coord = randomPosition(random);
			double y_coord = randomPosition(random);

			/* if dart lands in circle, increment score */
			int inc = 0;
			if (inside(x_coord, y_coord)) inc = 1;
			score += inc;
		}
		double d = 4.0 * (double) score / (double) dartsc;
		return d;
	}
	
	public static boolean inside(double x_coord, double y_coord) {
		return (x_coord * x_coord + y_coord * y_coord) <= 1.0;
	}
	
	public static double randomPosition(Random random) {
		double r = random.nextDouble();
		return (2.0 * r) - 1.0;
	}
	
}
