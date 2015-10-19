package nbody;

import java.util.Random;

import aeminium.runtime.futures.codegen.Sequential;

/*
 * Copyright (c) 2011.  Peter Lawrey
 *
 * "THE BEER-WARE LICENSE" (Revision 128)
 * As long as you retain this notice you can do whatever you want with this stuff.
 * If we meet some day, and you think this stuff is worth it, you can buy me a beer in return
 * There is no warranty.
 */

/* The Computer Language Benchmarks Game

 http://shootout.alioth.debian.org/

 contributed by Mark C. Lewis
 modified slightly by Chad Whipkey
 */
// run with: java  -server -XX:+TieredCompilation -XX:+AggressiveOpts nbody 50000000

public class NBody {
	public static final int DEFAULT_ITERATIONS = 1;
	public static final int DEFAULT_SIZE = 10;

	public static final int ADVANCE_THRESHOLD = 1000;
	public static final int APPLY_THRESHOLD = 100;

	static final double PI = 3.141592653589793;
	static final double SOLAR_MASS = 4 * PI * PI;

	public double x;
	public double y;
	public double z;
	public double vx;
	public double vy;
	public double vz;
	public double mass;

	public NBody(Random r) {
		x = r.nextDouble();
		y = r.nextDouble();
		z = r.nextDouble();
		vx = r.nextDouble();
		vy = r.nextDouble();
		vz = r.nextDouble();
		mass = r.nextDouble();

	}
	
	public static void main(String[] args) {
		int n = NBody.DEFAULT_ITERATIONS;
		if (args.length > 0) {
			n = Integer.parseInt(args[0]);
		}
		int size = NBody.DEFAULT_SIZE;
		if (args.length > 1) {
			size = Integer.parseInt(args[1]);
		}

		NBodySystem bodies = new NBodySystem(NBody.generateRandomBodies(size, 1L));
		long t = System.nanoTime();
		double en = bodies.energy();
		System.out.printf("%.9f\n", en);
		for (int i = 0; i < n; ++i) {
			bodies.advance(0.01);
		}
		en = bodies.energy();
		System.out.println("% " + ((double) (System.nanoTime() - t) / (1000 * 1000 * 1000)));
		System.out.printf("%.9f\n", en);
	}
	
	@Sequential
	public static NBody[] generateRandomBodies(int n, long seed) {
		Random random = new Random(seed);
		NBody[] r = new NBody[n];
		for (int i = 0; i < n; i++) {
			r[i] = new NBody(random);
		}
		return r;
	}

	NBody offsetMomentum(double px, double py, double pz) {
		vx = -px / SOLAR_MASS;
		vy = -py / SOLAR_MASS;
		vz = -pz / SOLAR_MASS;
		return this;
	}
	
	public void changeVelocity(double vx, double vy, double vz) {
		this.vx += vx;
		this.vy += vy;
		this.vz += vz;
	}
	
	public static double minus(double a, double b) {
		return a-b;
	}
	
}