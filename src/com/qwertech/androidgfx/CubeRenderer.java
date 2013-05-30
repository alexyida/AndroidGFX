package com.qwertech.androidgfx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

public class CubeRenderer implements GLSurfaceView.Renderer {
    /** Used for debug logs. */
	private static final String TAG = "CubeRenderer";
	
	/** References to other main objects. */
	private final MainActivity mainActivity;
	
	/** Store our model data in a float buffer. */
    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeColors;
    private final FloatBuffer mCubeNormals;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;
    
    /**
    * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
    * it positions things relative to our eye.
    */
    private float[] mViewMatrix = new float[16];
    
    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];
    
    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];
    
    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];
    
    /** 
	 * Stores a copy of the model matrix specifically for the light position.
	 */
    private float[] mLightModelMatrix = new float[16];
    
    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;
    
    /** This will be used to pass in the modelview matrix. */
    private int mMVMatrixHandle;
    
    /** This will be used to pass in the light position. */
    private int mLightPosHandle;
    
    /** This will be used to pass in model position information. */
    private int mPositionHandle;
    
    /** This will be used to pass in model color information. */
    private int mColorHandle;
    
    /** This will be used to pass in model normal information. */
    private int mNormalHandle;
    
    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;
    
    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;
    
    /** Size of the normal data in elements. */
    private final int mNormalDataSize = 3;
    
    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
	 *  we multiply this by our transformation matrices. */
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

	/** Used to hold the current position of the light in world space (after transformation via model matrix). */
    private final float[] mLightPosInWorldSpace = new float[4];
    
    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
    private final float[] mLightPosInEyeSpace = new float[4];
    
    /** This is a handle to our per-vertex cube shading program. */
    private int mPerVertexProgramHandle;
    
    /** This is a handle to our per-fragment cube shading program. */
    private int mPerFragmentProgramHandle;
    
    /** This is a handle to our light point program. */
    private int mPointProgramHandle;
    
    private boolean mPerFragment = false;
    
    /**
     * Initialize the model data.
     */
    public CubeRenderer(final MainActivity mainActivity)
    {
    	this.mainActivity = mainActivity;
    	
    	// X, Y, Z
    	final float[] cubePositionData = {
    			// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
    	        // if the points are counter-clockwise we are looking at the "front". If not we are looking at
    	        // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
    	        // usually represent the backside of an object and aren't visible anyways.
    			
				// Front face
				-1.0f, 1.0f, 1.0f,				
				-1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f, 
				-1.0f, -1.0f, 1.0f, 				
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				
				// Right face
				1.0f, 1.0f, 1.0f,				
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,				
				1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f,
				
				// Back face
				1.0f, 1.0f, -1.0f,				
				1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,				
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				
				// Left face
				-1.0f, 1.0f, -1.0f,				
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f, 
				-1.0f, -1.0f, -1.0f,				
				-1.0f, -1.0f, 1.0f, 
				-1.0f, 1.0f, 1.0f, 
				
				// Top face
				-1.0f, 1.0f, -1.0f,				
				-1.0f, 1.0f, 1.0f, 
				1.0f, 1.0f, -1.0f, 
				-1.0f, 1.0f, 1.0f, 				
				1.0f, 1.0f, 1.0f, 
				1.0f, 1.0f, -1.0f,
				
				// Bottom face
				1.0f, -1.0f, -1.0f,				
				1.0f, -1.0f, 1.0f, 
				-1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, 				
				-1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f
    	};
    	
    	// R, G, B, A
    	final float[] cubeColorData = {
    			// Front face (red)
				1.0f, 0.0f, 0.0f, 1.0f,				
				1.0f, 0.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 0.0f, 1.0f,				
				1.0f, 0.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 0.0f, 1.0f,
				
				// Right face (green)
				0.0f, 1.0f, 0.0f, 1.0f,				
				0.0f, 1.0f, 0.0f, 1.0f,
				0.0f, 1.0f, 0.0f, 1.0f,
				0.0f, 1.0f, 0.0f, 1.0f,				
				0.0f, 1.0f, 0.0f, 1.0f,
				0.0f, 1.0f, 0.0f, 1.0f,
				
				// Back face (blue)
				0.0f, 0.0f, 1.0f, 1.0f,				
				0.0f, 0.0f, 1.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 1.0f,				
				0.0f, 0.0f, 1.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 1.0f,
				
				// Left face (yellow)
				1.0f, 1.0f, 0.0f, 1.0f,				
				1.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 0.0f, 1.0f,				
				1.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 0.0f, 1.0f,
				
				// Top face (cyan)
				0.0f, 1.0f, 1.0f, 1.0f,				
				0.0f, 1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f, 1.0f,				
				0.0f, 1.0f, 1.0f, 1.0f,
				0.0f, 1.0f, 1.0f, 1.0f,
				
				// Bottom face (magenta)
				1.0f, 0.0f, 1.0f, 1.0f,				
				1.0f, 0.0f, 1.0f, 1.0f,
				1.0f, 0.0f, 1.0f, 1.0f,
				1.0f, 0.0f, 1.0f, 1.0f,				
				1.0f, 0.0f, 1.0f, 1.0f,
				1.0f, 0.0f, 1.0f, 1.0f
    	};
    	
    	// X, Y, Z
    	// The normal is used in light calculations and is a vector which points
    	// orthogonal to the plane of the surface. For a cube model, the normals
    	// should be orthogonal to the points of each face.
    	final float[] cubeNormalData = {
    			// Front face
				0.0f, 0.0f, 1.0f,				
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,				
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				
				// Right face 
				1.0f, 0.0f, 0.0f,				
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,				
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				
				// Back face 
				0.0f, 0.0f, -1.0f,				
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,				
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				
				// Left face 
				-1.0f, 0.0f, 0.0f,				
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,				
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				
				// Top face 
				0.0f, 1.0f, 0.0f,			
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,				
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				
				// Bottom face 
				0.0f, -1.0f, 0.0f,			
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,				
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f
    	};
    	
    	// Initialize the buffers.
    	mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();    	
    	mCubePositions.put(cubePositionData).position(0);
    	
    	mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();    	
    	mCubeColors.put(cubeColorData).position(0);
    	
    	mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();    	
    	mCubeNormals.put(cubeNormalData).position(0);
    }
    
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
    	// Set the background clear color to black.
    	GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    	
    	// Use culling to remove back faces.
    	GLES20.glEnable(GLES20.GL_CULL_FACE);
    	
    	// Enable depth testing
    	GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    	
    	// Position the eye in front of the origin.
    	final float eyeX = 0.0f;
    	final float eyeY = 0.0f;
    	final float eyeZ = -0.5f;
    	
    	// We are looking toward the distance
    	final float lookX = 0.0f;
    	final float lookY = 0.0f;
    	final float lookZ = -5.0f;
    	
    	// Set our up vector. This is where our head would be pointing were we holding the camera.
    	final float upX = 0.0f;
    	final float upY = 1.0f;
    	final float upZ = 0.0f;
    	
    	// Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
    	Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    	
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(this.mainActivity, R.raw.vertex_shader);;
        
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(this.mainActivity, R.raw.fragment_shader);
        
        // Load in the vertex shader.
        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);       
        // Load in the fragment shader shader.
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {"a_Position", "a_Color", "a_Normal"});
    
        final String perFragmentVertexShader = RawResourceReader.readTextFileFromRawResource(this.mainActivity, R.raw.per_fragment_vertex_shader);;
        
        final String perFragmentFragmentShader = RawResourceReader.readTextFileFromRawResource(this.mainActivity, R.raw.per_fragment_fragment_shader);
        
        // Load in the vertex shader.
        final int perFragmentVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, perFragmentVertexShader);       
        // Load in the fragment shader shader.
        final int perFragmentFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, perFragmentFragmentShader);
        
        mPerFragmentProgramHandle = createAndLinkProgram(perFragmentVertexShaderHandle, perFragmentFragmentShaderHandle, new String[] {"a_Position", "a_Color", "a_Normal"});
        
        // Define a simple shader program for our point.
        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(this.mainActivity, R.raw.point_vertex_shader);
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(this.mainActivity, R.raw.point_fragment_shader);
        
        final int pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        
        mPointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, new String[] {"a_Position"});
    }
    
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
    	// Set the OpenGL viewport to the same size as the surface.
    	GLES20.glViewport(0, 0, width, height);
    	
    	// Create a new perspective projection matrix. The height will stay the same
    	// while the width will vary as per aspect ratio.
    	final float ratio = (float) width / height;
    	final float left = -ratio;
    	final float right = ratio;
    	final float bottom = -1.0f;
    	final float top = 1.0f;
    	final float near = 1.0f;
    	final float far = 10.0f;
    	
    	Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }
    
    @Override
    public void onDrawFrame(GL10 glUnused) {
    	GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
    	
    	// Do a complete rotation every 10 seconds.
    	long time = SystemClock.uptimeMillis() % 10000L;
    	float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
    	
    	// Tell OpenGL to use this program when rendering.
    	// Set our lighting program.
    	
    	int programHandle;
    	if (mPerFragment)
    		programHandle = mPerFragmentProgramHandle;
    	else
    		programHandle = mPerVertexProgramHandle;
    		
    	GLES20.glUseProgram(programHandle);
    	
        // Set our per-vertex lighting program. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(programHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal");
    	
    	// Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);      
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);
               
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);  
    	
        // Draw some cubes.        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);        
        drawCube();
                        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);        
        drawCube();
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);        
        drawCube();
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -4.0f, -7.0f);
        drawCube();
        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);        
        drawCube();
        
        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();
    }
    
    /**
	 * Draws a cube.
	 */
    private void drawCube() {
    	// Pass in the position information
    	mCubePositions.position(0);
    	GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, mCubePositions);
    	
    	GLES20.glEnableVertexAttribArray(mPositionHandle);
    	
    	// Pass in the color information
    	mCubeColors.position(0);
    	GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false, 0, mCubeColors);

    	GLES20.glEnableVertexAttribArray(mColorHandle);
    	
    	// Pass in the normal information
    	mCubeNormals.position(0);
    	GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 0, mCubeNormals);

    	GLES20.glEnableVertexAttribArray(mNormalHandle);
    	
    	// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
    	Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
    	
    	// Pass in the modelview matrix.
    	GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);
    	
    	// This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
    	Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    	
    	// Pass in the combined matrix.
    	GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    	
    	// Pass in the light position in eye space. 
    	GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
    	
    	// Draw the cube.
    	GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }
    
    /**
	 * Draws a point representing the position of the light.
	 */
    private void drawLight() {
    	final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
    	final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");
    	
    	// Pass in the position.
    	GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);
    	
    	// Since we are not using a buffer object, disable vertex arrays for this attribute.
    	GLES20.glDisableVertexAttribArray(pointPositionHandle);
    	
    	// Pass in the transformation matrix.
    	Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
    	Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    	GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    	
    	// Draw the point.
    	GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }
    
    /** 
	 * Helper function to compile a shader.
	 * 
	 * @param shaderType The shader type.
	 * @param shaderSource The shader source code.
	 * @return An OpenGL handle to the shader.
	 */
    private int compileShader(final int shaderType, final String shaderCode) {
        // Load in the shader.
        int shaderHandle = GLES20.glCreateShader(shaderType);
        
        if (shaderHandle != 0) {
        	// Pass in the shader source.
        	GLES20.glShaderSource(shaderHandle, shaderCode);
        	
        	// Compile the shader.
        	GLES20.glCompileShader(shaderHandle);
        	
        	// Get the compilation status.
        	final int[] compileStatus = new int[1];
        	GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        	
        	// If the compilation failed, delete the shader.
        	if (compileStatus[0] == 0) {
        		Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
        		GLES20.glDeleteShader(shaderHandle);
        		shaderHandle = 0;
        	}
        }
        
        if (shaderHandle == 0)
        	throw new RuntimeException("Error creating shader.");
        
        return shaderHandle;
    }
    
    /**
	 * Helper function to compile and link a program.
	 * 
	 * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String attributes[]) {
        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();
        
        if (programHandle != 0) {
        	// Bind the vertex shader to the program.
        	GLES20.glAttachShader(programHandle, vertexShaderHandle);
        	
        	// Bind the fragment shader to the program.
        	GLES20.glAttachShader(programHandle, fragmentShaderHandle);
        	
        	// Bind attributes
        	if (attributes != null)
        		for (int i = 0; i < attributes.length; i++)
        			GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
        	
        	// Link the two shaders together into a program.
        	GLES20.glLinkProgram(programHandle);
        	
        	// Get the link status.
        	final int[] linkStatus = new int[1];
        	GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        	
        	// If the link failed, delete the program.
        	if (linkStatus[0] == 0) {
        		Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
        		GLES20.glDeleteProgram(programHandle);
        		programHandle = 0;
        	}
        }
        
        if (programHandle == 0)
        	throw new RuntimeException("Error creating program.");
        
        return programHandle;
    }
    
    public void switchMode() {
    	mPerFragment = !mPerFragment;
    }
}
