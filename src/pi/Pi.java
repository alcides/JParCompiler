package pi;

import java.util.Random;

public class Pi {
	
	public static final int DEFAULT_DART_SIZE = 100000000;
	
	public static void main(String[] args) {
		
		int dartsc = DEFAULT_DART_SIZE;
		if (args.length > 0) dartsc = Integer.parseInt(args[0]);
		
		Random random = new Random(1L);
		long t = System.nanoTime();
		long score = 0;
		for (long n = 0; n < dartsc; n++) {
			/* generate random numbers for x and y coordinates */
			double r = random.nextDouble();
			double x_coord = (2.0 * r) - 1.0;
			r = random.nextDouble();
			double y_coord = (2.0 * r) - 1.0;

			/* if dart lands in circle, increment score */
			int inc = 0;
			if ((x_coord * x_coord + y_coord * y_coord) <= 1.0) inc = 1;
			score += inc;
		}
		double d = 4.0 * (double) score / (double) dartsc;
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		System.out.println("PI = " + d);
	}
}
