package integrate;

import aeminium.runtime.futures.codegen.Sequential;

public class Integrate {
	
	static double errorTolerance = 1.0e-11; //13;
	static double start = -2101.0;
	static double end = 1700.0;

	// the function to integrate
	static double computeFunction(double x) {
		return (x * x + 1.0) * x;
	}
	
	@Sequential
	public static void main(String[] args) throws Exception {

		if (args.length > 0) {
			double exp = Double.parseDouble(args[0]);
			Integrate.errorTolerance = Math.pow(10, -exp);
		}
		
		if (args.length > 1) {
			Integrate.end = Double.parseDouble(args[1]);
		}
		double fs = Integrate.computeFunction(Integrate.start);
		double fe = Integrate.computeFunction(Integrate.end);
		long t = System.nanoTime();
		double a = recEval(Integrate.start, Integrate.end, fs, fe, 0);
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		System.out.println("Integral: " + a);
	}

	static final double recEval(double l, double r, double fl, double fr, double a) {
		double h = (r - l) * 0.5;
		double c = l + h;
		double fc = computeFunction(c);
		double al = area(fl, fc, h);
		double ar = area(fr, fc, h);
		double alr = al + ar;
		if (Math.abs(alr - a) <= Integrate.errorTolerance) {
			return alr;
		}
		return recEval(c, r, fc, fr, ar) + recEval(l, c, fl, fc, al);
	}

	private static double area(double fl, double fc, double h) {
		double hh = h * 0.5;
		return (fl + fc) * hh;
	}
}
