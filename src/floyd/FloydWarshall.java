package floyd;

import aeminium.runtime.futures.codegen.Sequential;

public class FloydWarshall {
	
	public static void floydwarshall2(double[][] path, int k) {
		
	}

	public static void floydwarshall(double[][] path) {
		for (int k = 0; k < path.length; k++) {
			for (int i = 0; i < path.length; i++) {
				double[] row = path[i];
				for (int j = 0; j < path.length; j++) {
					row[j] = Math.min(path[i][j], path[i][k] + path[k][j]);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		int N = 150;
		if (args.length > 0) {
			N = Integer.parseInt(args[0]);
		}
		double[][] path = generateRandomPath(N);
		//print(path);
		//System.out.println(".");
		long t = System.nanoTime();
		floydwarshall(path);
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		//print(path);
		
	}

	@Sequential
	public static void print(double[][] path) {
		for (int i=0; i<path.length; i++) {
			for (int j=0; j<path.length; j++) {
				System.out.print(path[i][j] + ", ");
			}
			System.out.println();
		}
	}

	private static double[][] generateRandomPath(int a) {
		double[][] m = new double[a][a];
		for (int i=0; i<a; i++) {
			for (int j=0; j<a; j++) {
				m[i][j] = Math.random() * 10000;
			}
		}
		return m;
	}

}
