package rendering;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

import physics.ParticleSystem;
import rendering.Containers.ParticleShader;
import rendering.Containers.ShaderHelper;
import egl.math.Matrix4;
import egl.math.Vector2;
import egl.math.Vector3;

public class Renderer {
	public static ParticleSystem system = new ParticleSystem(.1f, false);

	public static double time = 0;
	
	// CONSTANTS
	public static float xrot = 0.3f;
	public static float yrot = 0.3f;
	public static float scale = (float) 1 / 50;
	public static float trans = -0f;
	public static float transback = -5;

	public static final Vector3 lightPosition = new Vector3(10, 10, 10);

	public void initGl() throws LWJGLException {
		int width = 640;
		int height = 640;

		// set up window and display
		Display.setDisplayMode(new DisplayMode(width, height));
		Display.setVSyncEnabled(true);
		Display.setTitle("Fluid Simulation");

		Display.create(new PixelFormat(), new ContextAttribs(3, 2).withForwardCompatible(true).withProfileCore(true));

		glViewport(0, 0, width, height);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

	}

	public void run() {
		ArrayList<Vector3> points = system.getPositions();

		ParticleShader particleShader = new ParticleShader();

		particleShader.initProgram("src/rendering/Shaders/particleDepth.vert", "src/rendering/Shaders/particleDepth.frag");
		
		particleShader.position = glGetAttribLocation(particleShader.program, "vertexPos");
		
		particleShader.mView = glGetUniformLocation(particleShader.program, "mViewProj");
		particleShader.screenSize = glGetUniformLocation(particleShader.program, "screenSize");
		
		glBindFragDataLocation(particleShader.program, 0, "depth");
		
		glEnable(GL_DEPTH_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		constructVertexArrayObject(points);
		
		while (Display.isCloseRequested() == false) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
 
			//Enable point size on Mac
			glEnable(0x8642);

			// Create Matrices
			Matrix4 M = Matrix4.createTranslation((float) 0, (float) 0, transback);
			Matrix4 R = Matrix4.createRotationY(yrot);
			Matrix4 R2 = Matrix4.createRotationX(xrot);
			Matrix4 V = Matrix4.createPerspective((float) 1, (float) 1, (float) 4, (float) 1);
			
			Matrix4 mViewProj = M.clone().mulBefore(R2).mulBefore(R).mulBefore(V);

			RenderUtility.addMatrix(shader, mViewProj, "mViewProj");
			RenderUtility.addVector2(shader, new Vector2(Display.getWidth(), Display.getHeight()), "screenSize");
			RenderUtility.addVector3(shader, lightPosition, "lightPos");			

			// draw VAO
			glDrawArrays(GL_POINTS, 0, points.size());

			// check for errors
			if (glGetError() != GL_NO_ERROR) {
				throw new RuntimeException("OpenGL error: " + GLU.gluErrorString(glGetError()));
			}

			// swap buffers and sync frame rate to 60 fps
			Display.update();
			Display.sync(60);

			system.update();
			points = new ArrayList<Vector3>(system.getPositions());
			constructVertexArrayObject(points);
		}
		
		Display.destroy();
	}

	/**
	 * Create Vertex Array Object necessary to pass data to the shader
	 */
	private void constructVertexArrayObject(ArrayList<Vector3> points) {
		Matrix4 S = Matrix4.createScale((float)1 / 40);
		Matrix4 T = Matrix4.createTranslation((float)-0.5, (float)-0.3, (float)-0.5);
		// Create the array for the vectors of positions
		float[] buffer = new float[points.size() * 3];
		for (int i = 0; i < points.size(); i++) {
			S.mulDir(points.get(i));
			T.mulPos(points.get(i));
			buffer[3 * i] = (points.get(i).x);
			buffer[3 * i + 1] = (points.get(i).y);
			buffer[3 * i + 2] = (points.get(i).z);
		}

		FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(buffer.length);

		positionBuffer.flip();
 
		// convert color array to buffer		
		// create vertex buffer object (VBO) for vertices
		/*int positionBufferHandle = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferHandle);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positionBuffer, GL15.GL_STATIC_DRAW);
 
		// create VBO for color values
		int colorBufferHandle = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorBufferHandle);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorBuffer, GL15.GL_STATIC_DRAW);
 
		// unbind VBO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
 
		// create vertex array object (VAO)
		int vaoHandle = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoHandle);
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
 
		// assign vertex VBO to slot 0 of VAO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferHandle);
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
 
		// assign vertex VBO to slot 1 of VAO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorBufferHandle);
		GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
 
		// unbind VBO
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);*/
	}

	public static void main(String[] args) throws LWJGLException {
		Renderer r = new Renderer();
		r.initGl();
		r.run();
	}
}
