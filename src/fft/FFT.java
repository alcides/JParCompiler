package fft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import aeminium.runtime.futures.codegen.Sequential;

public class FFT {
	
	public static int DEFAULT_SIZE = 1024*32;
	
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
	
	public static void main(String[] args) {
		int size = FFT.DEFAULT_SIZE;
		if (args.length > 0) {
			size = Integer.parseInt(args[0]);
		}

		List<Complex> input = Arrays.asList(FFT.createRandomComplexArray(size, new Random(1L)));

		List<Complex> result = sequentialFFT(input);
		System.out.println(result.get(0));
	}

	/* Linear Version */
	public static List<Complex> sequentialFFT(List<Complex> x) {
		int N = x.size();

		// base case
		if (N == 1) {
			ArrayList<Complex> l = new ArrayList<Complex>();
			l.add(x.get(0));
			return l;
		}

		// radix 2 Cooley-Tukey FFT
		if (N % 2 != 0) {
			throw new RuntimeException("N is not a power of 2");
		}

		// fft of even terms
		ArrayList<Complex> even = new ArrayList<Complex>();
		for (int k = 0; k < N / 2; k++) {
			even.add(x.get(2*k));
		}
		
		// fft of even terms
		ArrayList<Complex> odd = new ArrayList<Complex>();
		for (int k = 0; k < N / 2; k++) {
			odd.add(x.get(2*k+1));
		}
		
		List<Complex> q = sequentialFFT(even);
		List<Complex> r = sequentialFFT(odd);

		// combine
		Complex[] y = new Complex[N];
		for (int k = 0; k < N / 2; k++) {
			double kth = -2 * k * Math.PI / N;
			Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
			y[k] = q.get(k).plus(wk.times(r.get(k)));
			y[k + N / 2] = q.get(k).minus(wk.times(r.get(k)));
		}
		return Arrays.asList(y);
	}
}
