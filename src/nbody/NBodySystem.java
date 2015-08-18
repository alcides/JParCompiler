package nbody;

public class NBodySystem {
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