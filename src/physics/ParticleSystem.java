package physics;
import java.util.ArrayList;

public class ParticleSystem {
	private ArrayList<Particle> particles;
	private CellGrid cube;

	private static final Vector3 GRAVITY = new Vector3(0f, -9.8f, 0f);
	private static final float deltaT = 0.1f;
	private static final float H = 1f;
	private static final float KPOLY = (float) (315f / (64f * Math.PI * Math.pow(H, 9)));
	//We may want to damp the spikey density
	private static final float SPIKY = (float) (45f / (Math.PI * Math.pow(H, 6)));
	private static final float REST_DENSITY = 1f;

	public ParticleSystem() {
		for (int i = 0; i < 10; i++) {
			for (int j = 10; j < 20; j++) {
				for (int k = 0; k < 10; k++) {
					particles.add(new Particle(new Vector3(i, j, k), 1));
				}
			}
		}

		//create cell cube
		CellGrid cube = new CellGrid(15, 15, 15); //should be whatever the size of our box is
	}

	public void update() {
		applyGravity();
		for (Particle p : particles) {
			//update velocity vi = vi + delta T * fext
			p.setVelocity(p.getVelocity() + p.getForce().mul(deltaT));

			//predict position x* = xi + delta T * vi
			p.setNewPos(p + p.getVelocity().mul(deltaT));
		}

		//get neighbors
		cube.updateCells(particles);
		for (Particle p : particles) {
			p.setNeighbors(p.getCell().getParticles());
		}

		//while sovler < iterations (they say that 2-4 is enough in the paper)
		for (int i = 0; i < 4; i++) {
			for (Particle p : particles) {
				//calculate c (density constraint)
				float density = 0;
				ArrayList<Particle> neighbors = p.getNeighbors();
				for (Particle n : neighbors) {
					density += WPoly6(p.getNewPos(), n.getNewPos());
				}
				p.setDensity(density);
				p.setPConstraint((density / REST_DENSITY) - 1);
			}

			//calculate gradient constraint
			for (Particle p : particles) {
				ArrayList<Particle> neighbors = p.getNeighbors();
				for (Particle n : neighbors) {
					Vector3 gradient = gradientConstraint(p, n);
				}
			}

			//calculate lambda

			for (Particle p : particles) {
				//update position - delta Pi - requires lambda
				Vector3 deltaP = new Vector3 (0f, 0f, 0f);
				ArrayList<Particle> neighbors = p.getNeighbors();
				for (Particle n : neighbors) {
					float lambdaSum = p.getLambda() + n.getLambda();
					deltaP = (deltaP.add(WSpiky(p.getNewPos(), n.getNewPos()))).mul(lambdaSum);
				}
				p.setDeltaP(deltaP.div(REST_DENSITY));
				//collision detection including with box
			}

			for (Particle p : particles) {
				//update x*i = x*i + delta Pi
				p.setNewPos(p.getNewPos().add(p.getDeltaP()));
			}
		}

		for (Particle p : particles) {
			//set new velocity vi = (1/delta T) * (x*i - xi)
			p.setVelocity(p.getNewPos().sub(p.getOldPos()).div(deltaT));

			//apply vorticity confinement
			curl(p);
			applyVorticity(p);
			
			p.setVelocity(p.getVelocity() + p.getForce().mul(deltaT));
			p.setNewPos(p + p.getVelocity().mul(deltaT));

			//apply XSPH viscosity

			//update position xi = x*i
			p.setOldPos(p.getNewPos());
		}
	}

	private void applyGravity() {
		for (Particle p : particles) {
			p.getForce().add(GRAVITY);
		}
	}

	//Poly6 Kernel
	private float WPoly6(Vector3 pi, Vector3 pj) {
		float rSquared = (float) pi.sub(pj).magnitude();
		if (rSquared > H) return 0;
		rSquared = Math.pow(rSquared, 2);
		return (float) (KPOLY * Math.pow((H - rSquared), 3));
	}
	
	//Spiky Kernel
	private Vector3 WSpiky (Vector3 pi, Vector3 pj) {
		Vector3 radius = pi.sub(pj);
		float coeff = (float) Math.pow(H - radius.magnitude(), 2);
		coeff *= SPIKY;
		coeff /= radius.magnitude();
		return radius.mul(coeff);
	}

	//TODO
	private Vector3 gradientConstraint(Particle p, Particle neighbor) {
		//first case k == i
		if (p.equals(neighbor)) {
			Vector3 sum = new Vector3(0f, 0f, 0f);
			ArrayList<Particle> neighbors = p.getNeighbors();
			for (Particle n : neighbors) {
				if (!n.equals(p)) {
					//Vector3D gradient = 
					//sum.add(gradient);
				}
			}

			sum = sum.div(REST_DENSITY);
			return sum;
		} else { //second case k == j

		}
		
		//Suppress errors
		return null;
	}

	private void lambda(Particle p) {

	}

	private void curl(Particle p) {
		Vector3 w = new Vector3(0, 0, 0);
		Vector3 velocityDiff;
		Vector3 gradient;
		ArrayList<Particle> neighbors = p.getNeighbors();
		for (Particle n : neighbors) {
			velocityDiff = n.getVelocity().sub(p.getVelocity());
			gradient = WSpiky(p, n);
			w.add(velocityDiff.cross(gradient));
		}

		p.setCurl(w);
	}

	private void applyVorticity(Particle p) {
		Vector3 N;
		Vector3 w = p.getCurl();
		Vector3 r;
		Vector3 gradient = new Vector3(0, 0, 0);
		Vector3 vorticity;
		ArrayList<Particle> neighbors = p.getNeighbors();
		for (Particle n : neighbors) {
			d = n.sub(p);
			Vector3 mw = n.getCurl().sub();
			float magnitudeW = mw.magnitude();
			gradient.x += magnitudeW / d.x;
			gradient.y += magnitudeW / d.y;
			gradient.z += magnitudeW / d.z;
		}

		N = gradient.div(gradient.magnitude());
		vorticity = epsilon * (N.cross(w));
		p.getForce().add(vorticity);
	}
}
