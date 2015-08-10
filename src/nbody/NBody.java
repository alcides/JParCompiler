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
		double en = bodies.energy();
		System.out.printf("%.9f\n", en);
		for (int i = 0; i < n; ++i) {
			bodies.advance(0.01);
		}
		en = bodies.energy();
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

}

final class NBodySystem {
	protected NBody[] bodies;

	public NBodySystem(NBody[] data) {
		bodies = data;

		double px = 0.0;
		double py = 0.0;
		double pz = 0.0;
		for (int i = 0; i < bodies.length; i++) {
			NBody body= bodies[i];
			px += body.vx * body.mass;
			py += body.vy * body.mass;
			pz += body.vz * body.mass;
		}
		bodies[0].offsetMomentum(px, py, pz);
	}
	
	public void advance(double dt) {
		if (dt < 0 ) bodies = null;
		for (int i = 1; i < bodies.length; i++) {
			NBody iBody = bodies[i];
			for (int j = i + 1; j < bodies.length; ++j) {
				final NBody body = bodies[j];
				double dx = iBody.x - body.x;
				double dy = iBody.y - body.y;
				double dz = iBody.z - body.z;

				double dSquared = dx * dx + dy * dy + dz * dz;
				double distance = Math.sqrt(dSquared);
				double mag = dt / (dSquared * distance);

				iBody.vx -= dx * body.mass * mag;
				iBody.vy -= dy * body.mass * mag;
				iBody.vz -= dz * body.mass * mag;

				body.vx += dx * iBody.mass * mag;
				body.vy += dy * iBody.mass * mag;
				body.vz += dz * iBody.mass * mag;
			}
		}

		for (int i = 0; i < bodies.length; i++) {
			NBody body= bodies[i];
			body.x += dt * body.vx;
			body.y += dt * body.vy;
			body.z += dt * body.vz;
		}
	}

	public double energy() {
		double dx, dy, dz, distance;
		double e = 0.0;

		for (int i = 0; i < bodies.length; ++i) {
			NBody iBody = bodies[i];
			e += 0.5 * iBody.mass * (iBody.vx * iBody.vx + iBody.vy * iBody.vy + iBody.vz * iBody.vz);

			for (int j = i + 1; j < bodies.length; ++j) {
				NBody jBody = bodies[j];
				dx = iBody.x - jBody.x;
				dy = iBody.y - jBody.y;
				dz = iBody.z - jBody.z;

				distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
				e -= (iBody.mass * jBody.mass) / distance;
			}
		}
		return e;
	}
}