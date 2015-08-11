package integrate;

import aeminium.runtime.futures.codegen.Sequential;


public class Integrate {
	
	static double errorTolerance = 1.0e-3; //13;
	static int threshold = 100;
	static double start = -2101.0;
	static double end = 1036.0;

	// the function to integrate
	@Sequential
	static double computeFunction(double x) {
		return (x * x + 1.0) * x;
	}
	
	public static void main(String[] args) throws Exception {

		if (args.length > 0) {
			double exp = Double.parseDouble(args[0]);
			Integrate.errorTolerance = Math.pow(10, -exp);
		}

		double fs = Integrate.computeFunction(Integrate.start);
		double fe = Integrate.computeFunction(Integrate.end);
		double a = recEval(Integrate.start, Integrate.end, fs, fe, 0);
		System.out.println("Integral: " + a);
	}

	static final double recEval(double l, double r, double fl, double fr, double a) {
		double h = (r - l) * 0.5;
		double c = l + h;
		double fc = (c * c + 1.0) * c;
		double hh = h * 0.5;
		double al = (fl + fc) * hh;
		double ar = (fr + fc) * hh;
		double alr = al + ar;
		if (Math.abs(alr - a) <= Integrate.errorTolerance) {
			return alr;
		}
		return recEval(c, r, fc, fr, ar) + recEval(l, c, fl, fc, al);
	}
}
