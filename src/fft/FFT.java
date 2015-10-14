package fft;

import java.util.Random;

import aeminium.runtime.futures.codegen.Sequential;

public class FFT {
	
	public static int DEFAULT_SIZE = 2*1024*1024;
	
	@Sequential
	public static Complex[] createRandomComplexArray(int n) {
		Complex[] x = new Complex[n];
		for (int i = 0; i < n; i++) {
			x[i] = new Complex(i, 0);
			x[i] = new Complex(-2 * Math.random() + 1, 0);
		}
		return x;
	}
	
	@Sequential
	public static Complex[] createRandomComplexArray(int n, Random r) {
		Complex[] x = new Complex[n];
		for (int i = 0; i < n; i++) {
			x[i] = new Complex(i, 0);
			x[i] = new Complex(-2 * r.nextDouble() + 1, 0);
		}
		return x;
	}
	
	
	@Sequential
	public static void main(String[] args) {
		int size = FFT.DEFAULT_SIZE;
		if (args.length > 0) {
			size = Integer.parseInt(args[0]);
		}

		Complex[] input = FFT.createRandomComplexArray(size, new Random());
		long t = System.nanoTime();
		Complex[] result = sequentialFFT(input);
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		System.out.println(result[0]);
	}

	/* Linear Version */
	public static Complex[] sequentialFFT(Complex[] x) {
		int N = x.length;

		// base case
		if (N == 1) return new Complex[] { x[0] };

		// radix 2 Cooley-Tukey FFT
		if (N % 2 != 0) {
			throw new RuntimeException("N is not a power of 2");
		}

		// fft of even terms
		Complex[] even = new Complex[N / 2];
		for (int k1 = 0; k1 < N / 2; k1++) {
			even[k1] = x[2 * k1];
		}

		// fft of odd terms
		Complex[] odd = new Complex[N / 2];
		for (int k = 0; k < N / 2; k++) {
			odd[k] = x[2 * k + 1];
		}
		Complex[] q = sequentialFFT(even);
		Complex[] r = sequentialFFT(odd);

		// combine
		Complex[] y = new Complex[N];
		for (int k = 0; k < N / 2; k++) {
			double kth = -2 * k * Math.PI / N;
			Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
			y[k] = q[k].plus(wk.times(r[k]));
			y[k + N / 2] = q[k].minus(wk.times(r[k]));
		}
		return y;
	}
}
