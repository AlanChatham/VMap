/**
 * Part of the VMap library: https://github.com/AlanChatham/VMap
 * 
 * Portions to update to Processing 2 copyright (c) 2014 - Laboratory LLC
 * Copyright (c) 2011-12 Ixagon AB 
 *
 * This source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is available on the World
 * Wide Web at <http://www.gnu.org/copyleft/gpl.html>. You can also
 * obtain it by writing to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package VMap;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import processing.event.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;
import processing.opengl.PJOGL;
import processing.core.PMatrix;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.XML;
import processing.opengl.PGL;
import processing.opengl.PShader;
import processing.core.PStyle;
// Add this in after updating to a new processing.core library
//import processing.core.PSurface;

//OpenGL imports
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3ES3;

public class VMap extends PImage implements PConstants{
		
	public final String VERSION = "2";

	private PApplet parent;
	private ArrayList<SuperSurface> surfaces;
	private ArrayList<SuperSurface> selectedSurfaces;
	
	//Off-screen PGraphics buffer, in case anyone needs access to that
	public PGraphics offScreenBuffer;
	
	//Default PGraphics buffer - this will get 
	// drawn by default 
	public PGraphics defaultDrawBuffer;
	
	private boolean allowUserInput;
	

	final static public int MODE_RENDER = 0;
	final static public int MODE_CALIBRATE = 1;
	public int MODE = MODE_CALIBRATE;
	private int snapDistance = 30;
	private int selectionDistance = 15; 
	private int selectionMouseColor;
	
	final static public int CMD = 157;
	private int numAddedSurfaces = 0;

	private boolean snap = true;

	private PVector prevMouse = new PVector();
	private boolean keybindingsEnabled = true;
	private boolean ctrlDown;
	private boolean altDown;
	private boolean grouping;
	
	private PImage backgroundTexture;
	private boolean usingBackground = false;
	
	private Rectangle selectionTool;
	private PVector startPos;
	private boolean isDragging;
	private boolean disableSelectionTool;
	
	private int[] ccolor;
	private int width;
	private int height;
	
	private PFont idFont;

	private boolean debug = true;
	
	private int TEXT_LEFT_MARGIN = 20;
	private int TEXT_BOTTOM_MARGIN = 20;
	
	// Should we use the defaultDrawBuffer, 
	//  which lets us do vmap.rect() and stuff,
	//  and automatically apply it to mapped areas with no texture?
	private boolean useDefault = true;

	private boolean shaking;
	private int shakeStrength;
	private int shakeSpeed;
	private float shakeAngle;
	private float shakeZ;
	
	// OpenGL stuff
	public PImage gridTexture;
	
	
	public PShader testShader;
	public PShader projectiveShader;
	public PShader bilinearShader;
	public PShader currentMainShader;
	
	// Shader attribute locations
	private int basicPosLoc;
	private int basicColorLoc;
	private int basicTexLoc;
	
	private int projectivePosLoc;
	private int projectiveColorLoc;
	private int projectiveTexLoc;
	
	private int bilinearPosLoc;
	private int bilinearColorLoc;
	private int bilinearTexLoc;
	
	
	private int VAOAddress;
	private int VBOAddress;
	private int gridTexAddress;
	
	private ArrayList<Float> quadVertices;
	
	PJOGL pgl;
	GL3ES3 gl;
	
	public String DefaultSaveLocation = "mapping.xml";
	
	
	/**
	 * Create instance of VMap
	 * @param parent Parent applet, usually the processing sketch using it
	 * @param width Width of the sketch window
	 * @param height Height of the sketch window
	 */

	public VMap(PApplet parent, int width, int height) {
		super(width, height);
		this.parent = parent;
		//this.enableMouseEvents();
		this.parent.registerMethod("keyEvent", this);
		this.parent.registerMethod("mouseEvent", this);
		this.width = width;
		this.height = height;
		this.ccolor = new int[0];
		this.idFont = parent.createFont("Verdana", 80);
		this.setSelectionMouseColor(0xFFCCCCCC);
		surfaces = new ArrayList<SuperSurface>();
		selectedSurfaces = new ArrayList<SuperSurface>();
		allowUserInput = true;

		// check the renderer type
		// issue a warning if its PGraphics2D
		PGraphics pg = parent.g;
		if (!(pg instanceof PGraphics3D)) {
			PApplet.println("VMap --> The VMap library will not work with PGraphics2D as the renderer because it relies on texture mapping. Set your renderer to P3D");
		}

		this.offScreenBuffer = parent.createGraphics(width, height, P3D);
		this.defaultDrawBuffer = parent.createGraphics(width, height, P3D);
		
		
		// OpenGL setup stuff //
		// use newer, nicer OpenGL features, mainly for EBOs
		//PJOGL.profile = 4;
		
		quadVertices = new ArrayList<Float>();		
		// Load in our shaders
		this.testShader = parent.loadShader("testShader.frag", "testShader.vert");
		this.projectiveShader = parent.loadShader("projectiveShader.frag", "projectiveShader.vert");
		this.bilinearShader = parent.loadShader("bilinearInterpolation.frag", "bilinearInterpolation.vert");
		
		this.currentMainShader = this.bilinearShader;
		
		PApplet.println("Loaded shaders");
		
		this.gridTexture = parent.loadImage("grid.png");
		
		//Set up our OpenGL contexts
		//this.beginDraw();
		
		
		this.pgl = (PJOGL) parent.beginPGL(); 
		PApplet.print("Loaded pgl: ");
		PApplet.println(pgl);
		
		gl = pgl.gl.getGL3ES3();
		PApplet.print("Loaded GL3ES3 context: ");
		PApplet.println(gl);
		
		
		
		
		parent.ellipse(200,200,200,200);

		setupOpenGLAddresses();
		
		this.findVertexAttributeLocations();
		

		PApplet.println("finished setup");
		//this.endDraw();
		
	}
	
	/**
	 * Sets up vertex attribute locations
	 *  by getting them from the shaders.
	 *  Only have to do this once at the start
	 */
	private void findVertexAttributeLocations(){
		// Get Vertex Attribute Location for position
		this.testShader.bind();
	    basicPosLoc = gl.glGetAttribLocation(testShader.glProgram, "position");
	    basicColorLoc = gl.glGetAttribLocation(testShader.glProgram, "color");
	    basicTexLoc = gl.glGetAttribLocation(testShader.glProgram, "texLoc");
	    this.testShader.unbind();
	    
	    this.projectiveShader.bind();
	    projectivePosLoc = gl.glGetAttribLocation(projectiveShader.glProgram, "position");
	    projectiveColorLoc = gl.glGetAttribLocation(projectiveShader.glProgram, "color");
	    projectiveTexLoc = gl.glGetAttribLocation(projectiveShader.glProgram, "texLoc");
	    this.projectiveShader.unbind();
	    
	    this.bilinearShader.bind();
	    bilinearPosLoc = gl.glGetAttribLocation(bilinearShader.glProgram, "position");
	    bilinearColorLoc = gl.glGetAttribLocation(bilinearShader.glProgram, "color");
	    bilinearTexLoc = gl.glGetAttribLocation(bilinearShader.glProgram, "texLoc");    
	    this.bilinearShader.unbind();
	
	}
	
	/**
	 * Set up addresses for OpenGL buffers
	 *  Sets up a VAO, VBO, and a texture buffer
	 */
	private void setupOpenGLAddresses(){
		// Let's set up some Vertex Attribute Objects
				IntBuffer VAONames = IntBuffer.allocate(1);
				gl.glGenVertexArrays(1, VAONames);
				VAOAddress = VAONames.get(0);
	
				// Get a buffer address from the GPU
				IntBuffer VBONames = IntBuffer.allocate(1); 
				gl.glGenBuffers(1, VBONames);
				VBOAddress = VBONames.get(0);
				
				// Get a texture address
				IntBuffer TexNames = IntBuffer.allocate(1);
				gl.glGenTextures(1, TexNames);
				gridTexAddress = TexNames.get(0);
	}
	
	/**
	 * Sets up geometry in OpenGL
	 *  Kind of a disaster right now, since
	 *  it involves pulling mystery data out of thin air
	 *  from a global set of coordinates in setOpenGLVerticies
	 *  which should probably be refactored.
	 */
	private void setupOpenGLGeometry(){
		
		// Bind our VAO
		gl.glBindVertexArray(VAOAddress);

		// Now, create a VBO, bind it, add data to it,
		//  and keep it bound for us to use in this function
		setOpenGLVertices();		
		
		
		// Now tell the shader how our data in our VBO is structured,
		//  using Vertex Attribute pointers, like you do
		gl.glVertexAttribPointer(basicPosLoc, 3, GL.GL_FLOAT,
				                 false, 10 * Float.BYTES, 0);
		gl.glEnableVertexAttribArray(basicPosLoc);  
		
		// color
		gl.glVertexAttribPointer(basicColorLoc, 4, GL.GL_FLOAT,
		                         false, 10 * Float.BYTES, 3 * Float.BYTES);
		gl.glEnableVertexAttribArray(basicColorLoc);
		  
		  // texture position
		gl.glVertexAttribPointer(basicTexLoc, 3, GL.GL_FLOAT,
		                         false, 10 * Float.BYTES, 7 * Float.BYTES);
		gl.glEnableVertexAttribArray(basicTexLoc);
		
		
		//Unbind our VAO, just for s's and g's
		//gl.glBindVertexArray(0);
		// Do the same for our VBO
		//gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	}
	
	/**
	 * This helper function is suuuuuper important
	 *  It takes in an array of exactly 4 PVector vertices, and
	 *  calculates helper values that our projective and bilinear
	 *  interpolation shaders need. Where the magic happens.
	 * @param vertices Array of 4 PVectors, taking the 4 corner points of the 
	 *                  quad in screen pixel coordinates
	 * @param projective Are we using the projective shader? If so, we need
	 *                    to know, since we do extra calculations on our UV points
	 */
	private void addQuadPointsToVertexList(PVector[] vertices, boolean projective){
		if (vertices.length != 4){
			PApplet.println("Warning! One of your superSurfaces has less than 4 vertices");
		}
		// For more sensible internal calculation
		PVector topLeft = convertPixelToOpenGLCoords(vertices[0]);
		PVector topRight = convertPixelToOpenGLCoords(vertices[1]);
		PVector bottomRight = convertPixelToOpenGLCoords(vertices[2]);
		PVector bottomLeft = convertPixelToOpenGLCoords(vertices[3]);
		
		// Figure out q values for projective projection
		// Figure out the diagonal intersection point
		PVector di = new PVector (0,0);
		float m1 = (topRight.y - bottomLeft.y) / (topRight.x - bottomLeft.x);
		float m2 = (bottomRight.y - topLeft.y) / (bottomRight.x - topLeft.x);
		//  println(m1);
		//  println(m2);
		// y = mx + b, y - mx = b
		float b1 = topRight.y - (m1 * topRight.x);  
		float b2 = topLeft.y - (m2 * topLeft.x);

		//  println(b1);
		//  println(b2);
		// woo more algebra
		// y1 = m1x1 + b1,        y1 = y2 and x1 = x2
		// y2 = m2x2 + b2,  m1x + b1 = m2x2 + b2
        //                 m1x - m2x = b2 - b1
		//                   m1 - m2 = (b2 - b1) / x
		//     (m1 - m2) / (b2 - b1) = 1/x
		//     (b2 - b1) / (m1 - m2) = x
		di.x = (b2 - b1) / (m1 - m2);
		di.y = di.x * m1 + b1;

		// distances between the points and the intersection point
		float dtr = di.dist(topRight);
		float dbr = di.dist(bottomRight);
		float dbl = di.dist(bottomLeft);
		float dtl = di.dist(topLeft);

		// uvq values for the points
		float q = 1;
		if (projective == true) {q = (dtr + dbl) / dbl;}  
		PVector topRightUVQ = new PVector(1.0f * q, 0.0f * q, q); 

		if (projective == true) {q = (dbr + dtl) / dtl;}
		PVector bottomRightUVQ = new PVector(1.0f * q, 1.0f * q, q);

		if (projective == true) {q = (dtr + dbl) / dtr;}
		PVector bottomLeftUVQ = new PVector(0.0f * q, 1.0f * q, q); 

		if (projective == true) {q = (dbr + dtl) / dbr;}
		PVector topLeftUVQ = new PVector(0.0f * q, 0.0f * q, q); 
		
		// Set the actual quad vertices
		PVectorToQuadVertices(bottomLeft, bottomLeftUVQ); // bottom left
		PVectorToQuadVertices(bottomRight, bottomRightUVQ); // bottom right
		PVectorToQuadVertices(topLeft, topLeftUVQ); // top left
		PVectorToQuadVertices(bottomRight, bottomRightUVQ); // bottom right
		PVectorToQuadVertices(topRight, topRightUVQ); // top right
		PVectorToQuadVertices(topLeft, topLeftUVQ); // top left
//		
//		PVectorToQuadVertices(convertPixelToOpenGLCoords(vertices[3]), 0.0f, 0.0f); // bottom left
//		PVectorToQuadVertices(convertPixelToOpenGLCoords(vertices[2]), 1.0f, 0.0f); // bottom right
//		PVectorToQuadVertices(convertPixelToOpenGLCoords(vertices[0]), 0.0f, 1.0f); // top left
//		PVectorToQuadVertices(convertPixelToOpenGLCoords(vertices[2]), 1.0f, 0.0f); // bottom right
//		PVectorToQuadVertices(convertPixelToOpenGLCoords(vertices[1]), 1.0f, 1.0f); // top right
//		PVectorToQuadVertices(convertPixelToOpenGLCoords(vertices[0]), 0.0f, 1.0f); // top left
	}
	
	
//	private void PVectorToQuadVertices(PVector vector, float U, float V){
//		PVectorToQuadVertices(vector, U, V, 1.0f);
//	}
	
	/**
	 * Helper function that adds a default color (1.0f, 1.0f, 1.0f)
	 *  and UV texture mapping data to a coordinate PVector,
	 *  getting it ready to send to the shader
	 *  to our 
	 * @param positions PVector containing X,Y (and Z, but that gets set to 0) points
	 * @param texCoords PVector containing U,V texture mapping data for the point
	 */
	private void PVectorToQuadVertices(PVector positions, PVector texCoords){
		this.PVectorToQuadVertices(positions, texCoords.x, texCoords.y, texCoords.z);
	}
	
	/**
	 * Helper function that adds a default color (1.0f, 1.0f, 1.0f)
	 *  and UV texture mapping data to a coordinate PVector,
	 *  getting it ready to send to the shader
	 *  to our 
	 * @param positions PVector containing X,Y (and Z, but that gets set to 0) points
	 * @param U U texture sampling point for this vertex
	 * @param V V texture sampling point for this vertex
	 * @param Q Special mapping value, used in our projective mapping shader
	 */
	private void PVectorToQuadVertices(PVector vector, float U, float V, float Q){
		float[] vertex = {vector.x, vector.y, 0.0f, //position
						  1.0f, 1.0f, 1.0f, 1.0f,   //color
						  U, V, Q};				//UVs
		
		for (int i = 0; i < vertex.length; i++){
			quadVertices.add(vertex[i]);
		}
		
	}
	
	/*
	 * This function passes quad points to our bilinear shader
	 *  It uses some magic data stored in our global quadVerticies
	 *  array, which is dumb.
	 */
	private void setBilinearPoints(){
		// Sanity check to make sure we have points to send
		if (quadVertices.size() != 60){
			PApplet.println("Warning: tried to set bilinear shader points without the right number of vertices");
			PApplet.println("Expected 6, had " + quadVertices.size());
			return;
		}
		// Also, make sure we've bound the bilinear shader
		if (this.bilinearShader == null) {
			PApplet.println("Warning: bilinearShader was null when trying to use setBilinearPoints");
			return;
		}
		if (this.bilinearShader.bound() == false){
		  PApplet.println("Warning: bilinear shader needs to be bound in order for setBilinearPoints to work");
		  return;
		}
		
		// Now actually get points!
		int p0Location = gl.glGetUniformLocation(bilinearShader.glProgram, "p0");
		int p1Location = gl.glGetUniformLocation(bilinearShader.glProgram, "p1");
		int p2Location = gl.glGetUniformLocation(bilinearShader.glProgram, "p2");
		int p3Location = gl.glGetUniformLocation(bilinearShader.glProgram, "p3");
		
		// Since we have our nice points, let's send them to the shader
		gl.glUniform2f(p0Location, quadVertices.get(20), quadVertices.get(21)); //Top left//40Top right, set in addQuadPointsToVertex
		gl.glUniform2f(p1Location, quadVertices.get(40), quadVertices.get(41)); //Top right //10Bottom right, set in addQuadPointsToVertex
		gl.glUniform2f(p2Location, quadVertices.get(0), quadVertices.get(1)); //Bottom left //20Top left, set in addQuadPointsToVertex
		gl.glUniform2f(p3Location, quadVertices.get(10), quadVertices.get(11)); //Bottom right//0Bottom left, set in addQuadPointsToVertex
		
		
	}
	
	/**
	 * This loads in a texture to the graphics card for OpenGL to use
	 * @param texture Texture we want to use for the next OpenGL draw operations
	 */
	private void setupGridTexture(PImage texture){
		// If we're in calibration mode, 
		//  set the texture to our grid texture
		if (MODE == MODE_CALIBRATE){
		    texture = gridTexture;
		}
		// If there's no texture, draw VMap's defaultDrawBuffer
					//  i.e., basic mode
		else if (texture == null){
				texture = this.defaultDrawBuffer;
		}
		
		texture.loadPixels();
		IntBuffer buf = allocateDirectIntBuffer(texture.width * texture.height);
		buf.put(texture.pixels);
		buf.rewind();
		
		// Bind the texture
		gl.glBindTexture(GL.GL_TEXTURE_2D, gridTexAddress);
		// Now generate the texture on the graphics card
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_BGRA,
		                  texture.width, texture.height, 0, GL.GL_BGRA, 
		                  GL.GL_UNSIGNED_BYTE, buf);
		gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
		
		
	}
	
	/**
	 * Helper function that creates a buffer of size n. Mostly exists to make our code prettier
	 * @param n Size of buffer
	 * @return Returns a FloatBuffer of size n
	 */
	private FloatBuffer allocateDirectFloatBuffer(int n) {
		  return ByteBuffer.allocateDirect(n * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		}
	
	/**
	 * Helper function that creates a buffer of size n. Mostly exists to make our code prettier
	 * @param n Size of buffer
	 * @return Returns an IBuffer of size n
	 */
	private IntBuffer allocateDirectIntBuffer(int n) {
		  return ByteBuffer.allocateDirect(n * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
		}
	
	/**
	 * Convert a point on the screen to OpenGL -1.0f to 1.0f coordinates
	 * @param point Point to convert
	 * @return Returns a PVector containing coordinates translated into OpenGL scale
	 */
	private PVector convertPixelToOpenGLCoords(PVector point){
		float convertedX = point.x - (parent.width/2);
		convertedX = convertedX / (parent.width/2);
		
		// Processing has opposite Y coordinates, so we need to invert
		float convertedY = -point.y + (parent.height/2);
		convertedY = convertedY / (parent.height/2);

		PVector convertedVector = new PVector(convertedX, convertedY);
		return convertedVector;
	}
	
	/**
	 * Takes some magic data (from our global quadVertices array)
	 *  and sticks it into the OpenGL vertex buffer object (VBO)
	 *  that we are currently using. Needs refactoring to at least
	 *  pass our data like civilized people, probably the VBO address too
	 */
	private void setOpenGLVertices(){

		// create a buffer for our vertices, and then add them in
		FloatBuffer verticesBuffer = allocateDirectFloatBuffer(this.quadVertices.size());
		verticesBuffer.rewind();
		for (int i = 0; i < this.quadVertices.size(); i++){
			verticesBuffer.put(quadVertices.get(i));
		}
		verticesBuffer.rewind();
		
		// debugging printing
//		PApplet.println(verticesBuffer);
//		for (int j = 0; j < quadVertices.size()/9; j++){
//			for (int i = 0; i < 9; i++){
//				PApplet.print(" " + verticesBuffer.get());
//			}
//			PApplet.println(" ");
//		}
//		verticesBuffer.rewind();
		
		
		// Put the vertices in it!
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOAddress);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, Float.BYTES * quadVertices.size(),
                verticesBuffer, GL.GL_STATIC_DRAW);
		
	}
	
	/**
	 * Render method used when calibrating. Shouldn't be used for final rendering.
	 * @param glos PGrapics buffer for offscreen rendering
	 */
	public void render(PGraphics glos) {
		glos.beginDraw();
		glos.clear();
		glos.endDraw();
		if (MODE == MODE_CALIBRATE) {
			parent.cursor();
			glos.beginDraw();
			
			if(this.isUsingBackground()){
				glos.image(backgroundTexture, 0, 0, width, height);
			}
			
			glos.fill(0,40);
			glos.noStroke();
			glos.rect(-2,-2,width+4,height+4);
			glos.stroke(255, 255, 255, 40);
			glos.strokeWeight(1);
			float gridRes = 32.0f;
			
			float step = width/gridRes;

			for (float i = 1; i < width; i += step) {
				glos.line(i, 0, i, parent.height);
			}
			
			step = height/gridRes;
			
			for (float i = 1; i < width; i += step) {
				glos.line(0, i, parent.width, i);
			}
			
			glos.stroke(255);
			glos.strokeWeight(2);
			glos.line(1,1,width-1,1);
			glos.line(width-1,1,width-1, height-1);
			glos.line(1,height-1,width-1,height-1);
			glos.line(1,1,1,height-1);
			
		
			
			if (selectionTool != null && !disableSelectionTool) {
				glos.stroke(255,100);
				glos.strokeWeight(1);
				glos.fill(100, 100, 255, 50);
				glos.rect(selectionTool.x, selectionTool.y, selectionTool.width, selectionTool.height);
				glos.noStroke();
			}
			
			glos.endDraw();

			for (int i = 0; i < surfaces.size(); i++) {
				surfaces.get(i).render(glos);
			}
			
			//Draw circles for SelectionDistance or SnapDistance (snap if CMD is down)
			glos.beginDraw();
			if(!ctrlDown){
				glos.ellipseMode(PApplet.CENTER);
				glos.fill(this.getSelectionMouseColor(),100);
				glos.noStroke();
				glos.ellipse(parent.mouseX, parent.mouseY, this.getSelectionDistance()*2, this.getSelectionDistance()*2);
			}else{
				glos.ellipseMode(PApplet.CENTER);
				glos.fill(255,0,0,100);
				glos.noStroke();
				glos.ellipse(parent.mouseX, parent.mouseY, this.getSnapDistance()*2, this.getSnapDistance()*2);
			}
			
			// Set up some instruction text
			glos.text("New quad: a", 10, glos.width - 50);		
			
			glos.endDraw();
			
			
			
		} else {
			parent.noCursor();
		}
	}
	
	/**
	 * Render method used when calibrating. Shouldn't be used for final rendering.
	 */
	public void render() {
		offScreenBuffer.beginDraw();
		// parent.clear();
		offScreenBuffer.clear();
		offScreenBuffer.endDraw();
		
		if (MODE == MODE_CALIBRATE) {
			parent.cursor();
			offScreenBuffer.beginDraw();
			
			if(this.isUsingBackground()){
				offScreenBuffer.image(backgroundTexture, 0, 0, width, height);
			}
			//Draw a frame around the buffer
			offScreenBuffer.fill(0,40);
			offScreenBuffer.noStroke();
			offScreenBuffer.rect(-2,-2,width+4,height+4);
			offScreenBuffer.stroke(255, 255, 255, 40);
			offScreenBuffer.strokeWeight(1);
			float gridRes = 32.0f;
			
			float step = width/gridRes;

			for (float i = 1; i < width; i += step) {
				offScreenBuffer.line(i, 0, i, parent.height);
			}
			
			step = height/gridRes;
			
			for (float i = 1; i < width; i += step) {
				offScreenBuffer.line(0, i, parent.width, i);
			}
			
			offScreenBuffer.stroke(255);
			offScreenBuffer.strokeWeight(2);
			offScreenBuffer.line(1,1,width-1,1);
			offScreenBuffer.line(width-1,1,width-1, height-1);
			offScreenBuffer.line(1,height-1,width-1,height-1);
			offScreenBuffer.line(1,1,1,height-1);
			
			if (selectionTool != null && !disableSelectionTool) {
				offScreenBuffer.stroke(255,100);
				offScreenBuffer.strokeWeight(1);
				offScreenBuffer.fill(100, 100, 255, 50);
				offScreenBuffer.rect(selectionTool.x, selectionTool.y, selectionTool.width, selectionTool.height);
				offScreenBuffer.noStroke();
			}
			
			offScreenBuffer.endDraw();

			// in preparation for rendering, we need to reset our vertex list
			
			for (int i = 0; i < surfaces.size(); i++) {
				surfaces.get(i).render(offScreenBuffer);
//				
//				PVector[] surfaceVertices = surfaces.get(i).cornerPoints;
//				addQuadPointsToVertexList(surfaceVertices);			
			
				
			}
			
			//Draw circles for SelectionDistance or SnapDistance (snap if CMD is down)
			offScreenBuffer.beginDraw();
			if(!ctrlDown){
				offScreenBuffer.ellipseMode(PApplet.CENTER);
				offScreenBuffer.fill(this.getSelectionMouseColor(),100);
				offScreenBuffer.noStroke();
				offScreenBuffer.ellipse(parent.mouseX, parent.mouseY, this.getSelectionDistance()*2, this.getSelectionDistance()*2);
			}else{
				offScreenBuffer.ellipseMode(PApplet.CENTER);
				offScreenBuffer.fill(255,0,0,100);
				offScreenBuffer.noStroke();
				offScreenBuffer.ellipse(parent.mouseX, parent.mouseY, this.getSnapDistance()*2, this.getSnapDistance()*2);
			}
			offScreenBuffer.endDraw();
			
			
			
			
		}
		// Render mode!
		else {
			
			parent.noCursor();
			for (SuperSurface ss : this.surfaces){
				// Add the default draw buffer on blank surfaces
				if (useDefault){
					if (ss.texture == null){
					    ss.setTexture(defaultDrawBuffer);
					}
				}
//				ss.render();
			}
		}

		// Now that we've drawn all our surfaces, let's show them with OpenGL!
		drawOpenGLGeometry();
		
		// Now copy all those pixels that are offscreen to our own buffer
		//  This could probably be taken out by a big refactor, but I don't understand
		//  the PGraphics side well enough, and it's easier to understand new VMap(width, height)
		//  than createGraphics()...
		
		
		this.copy(offScreenBuffer, 0, 0, width, height, 0, 0, width, height);

	}
	
	/**
	 * Use OpenGL to draw some stuff. Textured quads, in particular.
	 *  This function actually draws all of our surfaces
	 */
	private void drawOpenGLGeometry(){
		// OpenGL rendering of our stuff
		//  draws the actual surfaces
		//  as OpenGL triangle-quads
		
		//Draw our test triangle
		offScreenBuffer.beginDraw();
		offScreenBuffer.beginPGL();
		
		
		// Boo, we have to do a full draw routine for each
		//  surface. Lame. Which means maybe we want
		//  to move all the GL code to render() inside QuadSurface later?
		for (int i = 0; i < surfaces.size(); i++) {
			// First, get all the points of the surface
			PVector[] surfaceVertices = surfaces.get(i).cornerPoints;
			
			// Pick our shader
			if (this.currentMainShader == null){
				this.currentMainShader = this.testShader;
			}
			
			if (this.currentMainShader == this.projectiveShader){
				addQuadPointsToVertexList(surfaceVertices, true);
			}
			else {
				addQuadPointsToVertexList(surfaceVertices, false);
			}
		    
			this.currentMainShader.bind();
			
			setupOpenGLGeometry();
			
			if (this.currentMainShader == this.bilinearShader){
				this.setBilinearPoints();
			}
			
			setupGridTexture(surfaces.get(i).texture);
			int numVertices = this.quadVertices.size() / 10;
			gl.glDrawArrays(GL.GL_TRIANGLES, 0, numVertices);
			
			currentMainShader.unbind();
			
			this.quadVertices.clear();
		}
	
		
		offScreenBuffer.endPGL();
		offScreenBuffer.endDraw();
		
	}
	
	
	/**
	 * Shake all surfaces with max Z-displacement strength, vibration-speed speed, and shake decline fallOfSpeed. (min 0, max 1000 (1000 = un-ending shaking))
	 * @param strength Strength of z-displacement
	 * @param speed Vibration speed
	 * @param fallOffSpeed Falloff speed
	 */
	public void setShakeAll(int strength, int speed, int fallOffSpeed){
		for(SuperSurface ss : surfaces){
			ss.setShake(strength, speed, fallOffSpeed);
		}
	}
	
	/**
	 * Update shaking for all surfaces
	 */
	public void shake(){
		for(SuperSurface ss : surfaces){
			ss.shake();
		}
	}
	
	/**
	 * Get font for drawing text
	 * @return Returns font for drawing text
	 */
	public PFont getIdFont(){
		return idFont;
	}
	
	/**
	 * Unregisters Mouse Event listener for the VMap
	 */
//	public void disableMouseEvents(){
//		this.parent.unregisterMethod(mouseEvent, this);
//	}
	
	/**
	 * Registers Mouse Event listener for the VMap
	 */
//	public void enableMouseEvents(){
//		this.parent.registerMethod(mouseEvent, this);
//	}
	
	/**
	 * Get current max distance for an object to be selected
	 * @return Returns integer max distance
	 */
	public int getSelectionDistance(){
		return selectionDistance;
	}
	
	/**
	 * Set the max distance for an object to be selected
	 * @param selectionDistance Max distance for an object to be selected
	 */
	public void setSelectionDistance(int selectionDistance){
		this.selectionDistance = selectionDistance;
	}
	
	/**
	 * Sets the color of the mouse selection
	 * @param selectionMouseColor Color to set it to
	 */
	public void setSelectionMouseColor(int selectionMouseColor) {
		this.selectionMouseColor = selectionMouseColor;
	}
	
	/**
	 * Gets the color of the mouse selection
	 * @return Returns an int containing the selection color
	 */
	public int getSelectionMouseColor() {
		return selectionMouseColor;
	}

	/**
	 * Returns the array of colors used in calibration mode for coloring the surfaces.
	 * @return Returns array of colors used in calibration mode for coloring the surfaces.
	 */
	public int[] getColor() {
		return ccolor;
	}
	
	/**
	 * Set the array of colors used in calibration mode for coloring the surfaces.
	 * @param calibrationColors Color array to use for calibration
	 */
	public void setColor(int[] calibrationColors) {
		this.ccolor = calibrationColors;
	}
	
	/**
	 * Returns the rectangle used for selecting surfaces
	 * @return Returns rectangle used for selecting surfaces
	 */
	public Rectangle getSelectionTool() {
		return selectionTool;	
	}
	
	/**
	 * Optionally set a background image in calibration mode. 
	 * @param img Image to set as background
	 */
	public void setBackground(PImage img){
		this.backgroundTexture = img;
		this.setUsingBackground(true);
	}
	
	/**
	 * Boolean used to know if the background image should be rendered in calibration mode.
	 * @return Boolean, if you're rendering the background image in calibration mode or not
	 */
	public boolean isUsingBackground(){
		return usingBackground;
	}
	
	/**
	 * Set if background image should rendered in calibration mode
	 * @param val boolean, should you render background image in calibration mode
	 */
	public void setUsingBackground(boolean val){
		usingBackground = val;
	}

	/**
	 * Creates a Quad surface with perspective transform. Res is the amount of subdivisioning. Returns the surface after it has been created.
	 * @param res amount of subdivisioning
	 * @return Returns the QUADSurface you created
	 */
	public QuadSurface createQuadSurface(int res) {
		QuadSurface s = new QuadSurface(parent, this, parent.mouseX, parent.mouseY, res, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
		return s;
	}
	
	/**
	 * Creates a blank QuadSurface, adds it to the VMap's internal registry
	 * @param x X position
	 * @param y Y position
	 */
	public void addQuadSurface(int x, int y){
		addQuadSurface(null, x, y);
	}
	
	/**
	 * Creates a QuadSurface, adds it to the VMap's internal registry
	 * @param imageFile File to use
	 * @param x X position
	 * @param y Y position
	 */
	public void addQuadSurface(String imageFile, int x, int y){
		QuadSurface s = new QuadSurface(imageFile, parent, this, x, y, 3, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
	}
	
	/**
	 * Adds a SuperSurface to the internal surfaces array
	 * @param ss SuperSurface to add
	 */
	public void addSurface(SuperSurface ss){
		ss.setModeCalibrate();
		surfaces.add(ss);
		numAddedSurfaces++;
	}
	
	/**
	 * Creates a Quad surface at X/Y with perspective transform. Res is the amount of subdivisioning. Returns the surface after it has been created.
	 * @param res amout of subdivisioning for the surface
	 * @param x x coordinate of origin of new surface
	 * @param y y coordinate of origin of new surface
	 * @return Returns the SuperSurface you created
	 */
	public QuadSurface createQuadSurface(int res, int x, int y) {
		QuadSurface s = new QuadSurface(parent, this, x, y, res, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
		return s;
	}
	
	/**
	 * Creates a Bezier surface with perspective transform. Res is the amount of subdivisioning. Returns the surface after it has been created.
	 * @param res Amount of subdivisioning
	 * @return Returns the BezierSurface you created
	 */
	public BezierSurface createBezierSurface(int res) {
		BezierSurface s = new BezierSurface(parent, this, parent.mouseX, parent.mouseY, res, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
		return s;
	}
	
	/**
	 * Creates a Bezier surface at X/Y with perspective transform. Res is the amount of subdivisioning. Returns the surface after it has been created.
	 * @param res amount of subdivisioning of the surface
	 * @param x x coordinate of the surface's origin
	 * @param y y coordinate of the surface's origin
	 * @return Returns the BezierSurface you created
	 */
	public BezierSurface createBezierSurface(int res, int x, int y) {
		BezierSurface s = new BezierSurface(parent, this, x, y, res, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
		return s;
	}
	
	/**
	 * Adds a Bezier surface at X/Y with perspective transform. Res is the amount of subdivisioning. Returns the surface after it has been created.
	 * @param imageFilename filename for image texture to use
	 * @param x x coordinate of the surface's origin
	 * @param y y coordinate of the surface's origin
	 * @return Returns the BezierSurface you created
	 */
	public void addBezierSurface(String imageFilename, int x, int y) {
		BezierSurface s = new BezierSurface(imageFilename, parent, this, x, y, 3, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
	}
	
	/**
	 * Creates a blank BezierSurface, adds it to the Vmap's registry
	 * @param x X position
	 * @param y Y position
	 */
	public void addBezierSurface(int x, int y){
		addBezierSurface(null, x, y);
	}
	
	/**
	 * Get previous mouse position
	 * @return Pvector, with previous mouseX and mouseY
	 */
	public PVector getPrevMouse() {
		return prevMouse;
	}
	
	/**
	 * Set previous mouse position
	 * @param x Previous mouseX
	 * @param y Previous mouseY
	 */
	public void setPrevMouse(float x, float y) {
		prevMouse = new PVector(x, y);
	}
	
	/**
	 * Set the selection tool
	 * @param r Rectangle defining the boundaries of the selection tool
	 */
	public void setSelectionTool(Rectangle r) {
		selectionTool = r;
	}
	
	/**
	 * Set the selection tool
	 * @param x Selection rectangle origin x
	 * @param y Selection rectangle origin y
	 * @param width Selection rectangle width
	 * @param height Selection rectangle height
	 */
	public void setSelectionTool(int x, int y, int width, int height) {
		selectionTool = new Rectangle(x, y, width, height);
	}
	
	/**
	 * Is the selection tool disabled?
	 * @return Boolean, is the selection tool disabled?
	 */
	public boolean getDisableSelectionTool() {
		return disableSelectionTool;
	}
	
	/**
	 * Enable/disable selection tool
	 * @param disableSelectionTool true/false
	 */
	public void setDisableSelectionTool(boolean disableSelectionTool) {
		this.disableSelectionTool = disableSelectionTool;
	}
	
	/**
	 * Is CTRL pressed? 
	 * @return Boolean, is CTRL pressed?
	 */
	public boolean isCtrlDown() {
		return ctrlDown;
	}
	
	/**
	 * Is ALT pressed?
	 * @return Boolean, is ALT pressed?
	 */
	public boolean isAltDown(){
		return altDown;
	}
	
	/**
	 * @return Boolean, are you dragging something or not
	 */
	public boolean isDragging() {
		return isDragging;
	}
	
	/**
	 * @param isDragging Are you dragging something in the current VMap?
	 */
	public void setIsDragging(boolean isDragging) {
		this.isDragging = isDragging;
	}
	
	/**
	 * Add a surface to selected surfaces
	 * @param cps SuperSurface to add
	 */
	public void addSelectedSurface(SuperSurface cps) {
		selectedSurfaces.add(cps);
	}

	/**
	 * Get the selected surfaces
	 * @return Returns an ArrayList of all surfaces currently selected
	 */
	public ArrayList<SuperSurface> getSelectedSurfaces() {
		return selectedSurfaces;
	}
	
	/**
	 * Clears the arraylist of selected surfaces.
	 */
	public void clearSelectedSurfaces() {
		selectedSurfaces.clear();
	}
	
	/**
	 * Get all surfaces
	 * @return Returns an ArrayList of all the surfaces that belong to this mapper
	 */
	public ArrayList<SuperSurface> getSurfaces() {
		return surfaces;
	}
	
	/**
	 * Remove all surfaces
	 */
	public void clearSurfaces(){
		selectedSurfaces.clear();
		surfaces.clear();
	}

	/**
	 * Get surface by Id.
	 * @param id
	 * @return
	 */
	public SuperSurface getSurfaceById(int id) {
		SuperSurface cps = null;
		for (int i = 0; i < surfaces.size(); i++) {
			if (surfaces.get(i).getId() == id) {
				return surfaces.get(i);
			}
		}
		return cps;
	}
	
	/**
	 * Select the surface. Deselects all previously selected surfaces.
	 * @param cps New SuperSurface to select
	 */
	public void setSelectedSurface(SuperSurface cps) {
		for (SuperSurface ss : selectedSurfaces) {
			ss.setSelected(false);
		}
		selectedSurfaces.clear();
		cps.setSelected(true);
		selectedSurfaces.add(cps);
	}

	/**
	 * Check if coordinates is inside any of the surfaces.
	 * @param mX
	 * @param mY
	 * @return
	 */
	public boolean findActiveSurface(float mX, float mY) {
		for (int i = 0; i < surfaces.size(); i++) {
			SuperSurface surface = surfaces.get(i);

			if (surface.isInside(mX, mY)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check which mode is enabled (render or calibrate)
	 * @return
	 */
	public int getMode() {
		return this.MODE;
	}
	
	/**
	 * Set mode to calibrate
	 */
	public void setModeCalibrate() {
		this.MODE = VMap.MODE_CALIBRATE;
		for (SuperSurface s : surfaces) {
			s.setModeCalibrate();
		}
	}
	
	/**
	 * Set mode to render
	 */
	public void setModeRender() {
		this.MODE = VMap.MODE_RENDER;
		for (SuperSurface s : surfaces) {
			s.setModeRender();
		}
	}
	
	/**
	 * Toggle the mode
	 */
	public void toggleCalibration() {
		if (MODE == MODE_RENDER)
			MODE = MODE_CALIBRATE;
		else
			MODE = MODE_RENDER;

		for (SuperSurface s : surfaces) {
			if (MODE == MODE_CALIBRATE)
				s.setModeCalibrate();
			else
				s.setModeRender();
		}
	}

	/**
	 * Toggle if corner snapping is used
	 */
	public void toggleSnap() {
		snap = !snap;
	}
	
	/**
	 * See if snap mode is used
	 * @return snap
	 */
	public boolean getSnap() {
		return snap;
	}

	/**
	 * Manually set corner snap mode
	 * @param snap
	 */
	public void setSnap(boolean snap) {
		this.snap = snap;
	}

	/**
	 * See the snap distance
	 * @return snapDistance
	 */
	public int getSnapDistance() {
		return snapDistance;
	}

	/**
	 * Set corner snap distance
	 * @param snapDistance
	 */
	public void setSnapDistance(int snapDistance) {
		this.snapDistance = snapDistance;
	}

	/**
	 * See if debug mode is on.
	 * @return
	 */
	public boolean getDebug() {
		return debug;
	}

	/**
	 * Manually set debug mode. (debug mode will print more to console)
	 * @param debug
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String version() {
		return VERSION;
	}

	/**
	 * Puts all projection mapping data in the XML
	 * @param root
	 */
	public void saveXML(XML root) {
		root.setName("ProjectionMap");
		// create XML elements for each surface containing the resolution
		// and control point data
		for (SuperSurface s : surfaces) {
			XML surf = new XML("surface");
			surf.setName("surface");
			surf.setString("filename", s.textureFilename);
			surf.setInt("type", s.getSurfaceType());
			surf.setInt("id", s.getId());
			surf.setString("name", s.getSurfaceName());
			surf.setInt("res", s.getRes());
			if (s.isLocked())
				surf.setInt("lock", 1);
			else
				surf.setInt("lock", 0);
			
			for (int i = 0; i < s.getCornerPoints().length; i++) {
				XML cp = new XML("cornerpoint");
				cp.setName("cornerpoint");
				cp.setInt("i", i);
				cp.setFloat("x", s.getCornerPoint(i).x);
				cp.setFloat("y", s.getCornerPoint(i).y);
				surf.addChild(cp);

			}
			
			if(s instanceof BezierSurface){
				surf.setInt("horizontalForce", ((BezierSurface)s).getHorizontalForce());
				surf.setInt("verticalForce", ((BezierSurface)s).getVerticalForce());

				for(int i = 0; i < 8; i++){
					XML bp = new XML("bezierpoint");
					bp.setName("bezierpoint");
					bp.setInt("i", i);
					bp.setFloat("x", ((BezierSurface)s).getBezierPoint(i).x);
					bp.setFloat("y", ((BezierSurface)s).getBezierPoint(i).y);
					surf.addChild(bp);
				}
			}
			root.addChild(surf);
		}
	}

	/**
	 * Save all projection mapping data to file
	 * @param filename
	 */
	public void saveXML(String filename) {
		if (this.MODE == VMap.MODE_CALIBRATE){
			XML root = new XML("root");
			this.saveXML(root);
			try {
				parent.saveXML(root, "data/" + filename);
			} catch (Exception e) {
				PApplet.println((Object)e.getStackTrace());
			}
		}
	}

	/**
	 * Load projection map from file
	 * @param filename
	 */
	public void loadXML(String filename) {
		if (this.MODE == VMap.MODE_CALIBRATE) {
			File f = new File(parent.dataPath(filename));
			if (f.exists()) {
				// Clear our current state of everything
				this.setGrouping(false);
				selectedSurfaces.clear();
				surfaces.clear();
				// Load in XML data
				XML root = parent.loadXML(filename);
				
				// Not sure why I have to copy this in, I think it's because
				//  loadXML is too slow...
				String XMLCopy = root.toString();
				XML cleanRoot = parent.parseXML(XMLCopy);
				// Parse the data, reconstructing all the surfaces from the data.
				for (int i = 0; i < cleanRoot.getChildCount(); i++) {
					SuperSurface s = null;
					
					XML surface = cleanRoot.getChild(i);
					if (surface.getInt("type")  ==  SuperSurface.QUAD){
						s = new QuadSurface(parent, this, surface);
					}
					else if (surface.getInt("type") == SuperSurface.BEZIER){
						s = new BezierSurface(parent, this, surface);
					}
					
					if (s != null){
						s.setModeCalibrate();
						surfaces.add(s);
						if (s.getId() > numAddedSurfaces)
							numAddedSurfaces = s.getId() + 1;
					}
				}
				if(this.getDebug()) PApplet.println("Projection layout loaded from " + filename + ". " + surfaces.size() + " surfaces were loaded!");
			} else {
				if(this.getDebug()) PApplet.println("ERROR loading XML! No projection layout exists!");
			}
		}

	}
	
	/**
	 * Move a point of a surface
	 * @param ss SuperSurface point is owned by
	 * @param x X position to move point to
	 * @param y Y position to move point to
	 */
	public void movePoint(SuperSurface ss, int x, int y){
		int index = ss.getSelectedCorner();
		ss.setCornerPoint(index, ss.getCornerPoint(index).x + x, ss.getCornerPoint(index).y + y);
		if (ss instanceof BezierSurface){
			index = index*2;
			((BezierSurface) ss).setBezierPoint(index, ((BezierSurface) ss).getBezierPoint(index).x + x, ((BezierSurface) ss).getBezierPoint(index).y + y);
			index = index+1;
			((BezierSurface) ss).setBezierPoint(index, ((BezierSurface) ss).getBezierPoint(index).x + x, ((BezierSurface) ss).getBezierPoint(index).y + y);
		}
	}
	
	/**
	 * Check if any user event is allowed
	 * @return
	 */
	public boolean isAllowUserInput() {
		return allowUserInput;
	}
	
	/**
	 * Set if any user event is allowed
	 * @param allowUserInput
	 */
	public void setAllowUserInput(boolean allowUserInput) {
		this.allowUserInput = allowUserInput;
	}
	
	/**
	 * Places the surface last in the surfaces array, i.e. on top.
	 * @param index
	 */
	public void bringSurfaceToFront(int index){
		SuperSurface s = surfaces.get(index);
		surfaces.remove(index);
		surfaces.add(s);
	}
	
	/**
	 * Check if multiple surfaces are being manipulated
	 * @return Boolean, are multiple surfaces being manipulated
	 */
	public boolean isGrouping(){
		return grouping;
	}
	
	/**
	 * Set if multiple surfaces are being manipulated
	 * @param grouping Boolean, can you select multiple surfaces?
	 */
	public void setGrouping(boolean grouping){
		this.grouping = grouping;
	}
	
	/**
	 * Handles Mouse Wheel input
	 * @param delta
	 */
	private void mouseWheelAction(int delta){
		if(allowUserInput && this.MODE == VMap.MODE_CALIBRATE){
			if(delta < 0){
				if(ctrlDown){
					if(this.getSnapDistance() < 60){
						this.setSnapDistance(this.getSnapDistance()+2);
					}
				}else{
					if(this.getSelectionDistance() < 60)
						this.setSelectionDistance(this.getSelectionDistance()+2);
				}
			}
			if(delta > 0){
				if(ctrlDown){
					if(this.getSnapDistance() > 6){
						this.setSnapDistance(this.getSnapDistance()-2);
					}
				}else{
					if(this.getSelectionDistance() > 16)
						this.setSelectionDistance(this.getSelectionDistance()-2);
				}
			}
		}
	}
	
	/**
	 * MouseEvent method. Forwards the MouseEvent to ksMouseEvent if user input is allowed
	 * @param e
	 */
	public void mouseEvent(MouseEvent e) {
		if (allowUserInput) {
			ksMouseEvent(e);
		}
	}
	
	/**
	 * Returns whether or not keybindings are enabled
	 * @return Returns true or false if keyboard shortcuts are enabled or disabled
	 */
	public boolean getKeybindingsEnabled(){
		return this.keybindingsEnabled;
	}
	/**
	 * Sets keyboard shortcuts to on or off (true/false)
	 * @param value true for enabled, false for disabled
	 */
	public void setKeybindingsEnabled(boolean value){
		this.keybindingsEnabled = value;
	}
	
	/**
	 * Turns on keyboard shortcuts. Keyboard shortcuts default to being enabled.
	 */
	public void enableKeybindings(){
		this.setKeybindingsEnabled(true);
	}
	
	/**
	 * Disables keyboard shortcuts. Keyboard shortcuts default to being enabled.
	 *  Use this if the keybindings are interfering with the program you're writing,
	 *  most likely to happen if you're program might accept a 'c' as an input,
	 *  since that's the one keybinding that's active oustide of configuration mode
	 */
	public void disableKeybindings(){
		this.setKeybindingsEnabled(false);
	}
	
	
	
	/**
	 * KeyEvent method
	 * @param k KeyEvent thrown from Processing
	 */
	public void keyEvent(KeyEvent k) {
		
		//If key bindings are disabled, just return immediately
		if (this.keybindingsEnabled == false){
			return;
		}
		
		// Make sure that, assuming keybindings work,
		//  you can always toggle calibration mode with 'c'
		if (k.getKey() == 'c' && k.getAction() == KeyEvent.PRESS){
			this.toggleCalibration();;
		}
		// Ignore everything else we're in render mode (instead of calibration mode)
		if (MODE == MODE_RENDER){
			return; // ignore everything unless we're in calibration mode
		}

		switch (k.getAction()) {
		case KeyEvent.RELEASE:

			switch (k.getKeyCode()) {

			case KeyEvent.META:
			case PConstants.CONTROL:
			case KeyEvent.CTRL:
				ctrlDown = false;
				break;

			case KeyEvent.ALT:
			case PConstants.ALT:
				altDown = false;
				break;
			}
			break;

		case KeyEvent.PRESS:
			// Coded keys (as opposed to ASCII letters
			switch (k.getKeyCode()) {
				case '1':
					if (selectedSurfaces.size() == 1)
						selectedSurfaces.get(0).setSelectedCorner(0);
					break;
	
				case '2':
					if (selectedSurfaces.size() == 1)
						selectedSurfaces.get(0).setSelectedCorner(1);
					break;
	
				case '3':
					if (selectedSurfaces.size() == 1)
						selectedSurfaces.get(0).setSelectedCorner(2);
					break;
	
				case '4':
					if (selectedSurfaces.size() == 1)
						selectedSurfaces.get(0).setSelectedCorner(3);
					break;
	
				case PConstants.UP:
					for (SuperSurface ss : selectedSurfaces) {
						movePoint(ss, 0,-1);
					}
					break;
	
				case PConstants.DOWN:
					for (SuperSurface ss : selectedSurfaces) {
						movePoint(ss, 0, 1);
					}
					break;
	
				case PConstants.LEFT:
					for (SuperSurface ss : selectedSurfaces) {
						movePoint(ss, -1, 0);
					}
					break;
	
				case PConstants.RIGHT:
					for (SuperSurface ss : selectedSurfaces) {
						movePoint(ss, 1, 0);
					}
					break;
					
				case PConstants.DELETE:
				case PConstants.BACKSPACE:
					removeSelectedSurfaces();
					break;
				
				case PConstants.CONTROL:
				case CMD:
					ctrlDown = true;
					grouping = true;
					break;
	
				case PConstants.ALT:
					altDown = true;
					break;
			}
			
			switch (k.getKey()){
				
				case 'o':
					for (SuperSurface ss : selectedSurfaces) {
						ss.increaseResolution();
					}
					break;
	
				case 'p':
					for (SuperSurface ss : selectedSurfaces) {
						ss.decreaseResolution();
					}
					break;
				/*	
				case 'u':
					for (SuperSurface ss : selectedSurfaces) {
						ss.increaseHorizontalForce();
					}
					break;
	
				case 'i':
					for (SuperSurface ss : selectedSurfaces) {
						ss.decreaseHorizontalForce();
					}
					break;
				
				case 'j':
					for (SuperSurface ss : selectedSurfaces) {
						ss.increaseVerticalForce();
					}
					break;
	
				case 'k':
					for (SuperSurface ss : selectedSurfaces) {
						ss.decreaseVerticalForce();
					}
					break;
	*/
				case 't':
					for (SuperSurface ss : selectedSurfaces) {
						ss.toggleLocked();
					}
					break;
				
				case 'a':
					this.addQuadSurface(this.parent.mouseX, this.parent.mouseY);
					break;
					
				case 'z':
					this.addBezierSurface(this.parent.mouseX, this.parent.mouseY);
					break;
					
				case 's':
					this.saveXML(this.DefaultSaveLocation);
					break;	
					
				case 'l':
					this.loadXML(this.DefaultSaveLocation);
					break;
					
				case 'w':
					for(SuperSurface ss : this.selectedSurfaces){
					    ss.rotateCornerPoints(SuperSurface.CLOCKWISE);
					}
					break;
					
				case 'q':
					for(SuperSurface ss : this.selectedSurfaces){
					    ss.rotateCornerPoints(SuperSurface.COUNTERCLOCKWISE);
					}
					break;
					  
			
			}
		}
	}

	/**
	 * MouseEvent method.
	 * @param e
	 */
	public void ksMouseEvent(MouseEvent e) {
		if (this.MODE == VMap.MODE_RENDER)
			return;

		int mX = e.getX();
		int mY = e.getY();

		switch (e.getAction()) {
		case MouseEvent.WHEEL:
			this.mouseWheelAction(e.getCount());
			break;
		case MouseEvent.PRESS:
			if (this.MODE == VMap.MODE_CALIBRATE) {
				startPos = new PVector(mX, mY);
				for (int i = surfaces.size() - 1; i >= 0; i--) {
					SuperSurface cps = surfaces.get(i);

					cps.setActivePoint(cps.getActiveCornerPointIndex(mX, mY));
					
					if(cps instanceof BezierSurface){
						((BezierSurface)cps).setSelectedBezierControl(((BezierSurface)cps).getActiveBezierPointIndex(mX, mY));
					}
					
					if (cps.getActivePoint() >= 0 || 
					   (cps instanceof BezierSurface && ((BezierSurface)cps).getSelectedBezierControl() >= 0)) {
						if(grouping && !ctrlDown){
							if(!cps.isSelected()){
								for (SuperSurface ss : selectedSurfaces) {
									ss.setSelected(false);
								}
								grouping = false;
								selectedSurfaces.clear();
							}
						}
						
						disableSelectionTool = true;
						if (ctrlDown && grouping) {
							boolean actionTaken = false;
							if(cps.isSelected()){
								cps.setSelected(false);
								for(int j = selectedSurfaces.size() - 1; j >= 0; j-- ){
									if(cps.getId() == selectedSurfaces.get(j).getId()) selectedSurfaces.remove(j);
								}
								actionTaken = true;
							}
							if(!cps.isSelected() && !actionTaken){
								cps.setSelected(true);
								selectedSurfaces.add(cps);
								removeDuplicates(selectedSurfaces);
							}
						} else {
							if (grouping == false) {
								for (SuperSurface ss : selectedSurfaces) {
									ss.setSelected(false);
								}
								selectedSurfaces.clear();
								cps.setSelected(true);
								selectedSurfaces.add(cps);
							}
						}
						
						// no need to loop through all surfaces unless multiple
						// surfaces has been selected
						if (!grouping)
							break;
					}
				}
				if (grouping) {
					int moveClick = 0;
					for (SuperSurface ss : selectedSurfaces) {
						if (ss.getActivePoint() == 2000)
							moveClick++;
					}
				//	PApplet.println(moveClick);
					if (moveClick > 0) {
						for (SuperSurface ss : selectedSurfaces) {
							ss.setActivePoint(2000);
					//		PApplet.println(ss.getActivePoint());
						}
					}
				}
				
			}

			break;

		case MouseEvent.DRAG:
			if (this.MODE == VMap.MODE_CALIBRATE) {

				float deltaX = mX - prevMouse.x;
				float deltaY = mY - prevMouse.y;

				// Right mouse button drags very slowly.
				if (e.getButton() == PConstants.RIGHT) {
					deltaX *= 0.1;
					deltaY *= 0.1;
				}
				
				boolean[] movingPolys = new boolean[surfaces.size()];
				int iteration = 0;
				for (SuperSurface ss : surfaces) {
					
					movingPolys[iteration] = false;
					// Don't allow editing of surface if it's locked!
					if (!ss.isLocked()) {
						if(ss instanceof BezierSurface && ((BezierSurface)ss).getSelectedBezierControl() != -1){
							((BezierSurface)ss).setBezierPoint(((BezierSurface)ss).getSelectedBezierControl(), ((BezierSurface)ss).getBezierPoint(((BezierSurface)ss).getSelectedBezierControl()).x + deltaX, ((BezierSurface)ss).getBezierPoint(((BezierSurface)ss).getSelectedBezierControl()).y + deltaY);
						}else if (ss.getActivePoint() != -1) {
							// special case.
							// index 2000 is the center point so move all four
							// corners.
							if (ss.getActivePoint() == 2000) {
								// If multiple surfaces are selected, ALT need
								// to be pressed in order to move them.
								if ((grouping && altDown) || selectedSurfaces.size() == 1) {
									for (int i = 0; i < 4; i++) {
										ss.setCornerPoint(i, ss.getCornerPoint(i).x + deltaX, ss.getCornerPoint(i).y + deltaY);
										if (ss instanceof BezierSurface){
											((BezierSurface)ss).setBezierPoint(i, ((BezierSurface)ss).getBezierPoint(i).x + deltaX, ((BezierSurface)ss).getBezierPoint(i).y + deltaY);
											((BezierSurface)ss).setBezierPoint(i+4, ((BezierSurface)ss).getBezierPoint(i+4).x + deltaX, ((BezierSurface)ss).getBezierPoint(i+4).y + deltaY);
										}
									}	
									movingPolys[iteration] = true;
								}
							} else {
								movePoint(ss, (int)deltaX, (int)deltaY);
								// Move a corner point.
/*								int index =  ss.getActivePoint();
								ss.setCornerPoint(index, ss.getCornerPoint(ss.getActivePoint()).x + deltaX, ss.getCornerPoint(ss.getActivePoint()).y + deltaY);
								index = index*2;
								ss.setBezierPoint(index, ss.getBezierPoint(index).x + deltaX, ss.getBezierPoint(index).y + deltaY);
								index = index+1;
								ss.setBezierPoint(index, ss.getBezierPoint(index).x + deltaX, ss.getBezierPoint(index).y + deltaY);
								movingPolys[iteration] = true;
*/							}
						}
						
					}
					iteration++;
				}

				for (int i = 0; i < movingPolys.length; i++) {
					if (movingPolys[i]) {
						disableSelectionTool = true;
						break;
					}
				}

				if (altDown)
					disableSelectionTool = true;

				if (!disableSelectionTool) {
					selectionTool = new Rectangle((int) startPos.x, (int) startPos.y, (int) (mX - startPos.x), (int) (mY - startPos.y));
					
					PVector sToolPos = new PVector(selectionTool.x, selectionTool.y);

					if (selectionTool.x < selectionTool.x - selectionTool.width) {
						sToolPos.set(sToolPos.x + selectionTool.width, sToolPos.y, 0);
					}
					if (selectionTool.y < selectionTool.y - selectionTool.height) {
						sToolPos.set(sToolPos.x, sToolPos.y + selectionTool.height, 0);
					}

					for (SuperSurface cps : surfaces) {
						java.awt.Polygon p = cps.getPolygon();

						if (p.intersects(sToolPos.x, sToolPos.y, Math.abs(selectionTool.width), Math.abs(selectionTool.height))) {
							cps.setSelected(true);
							selectedSurfaces.add(cps);
							removeDuplicates(selectedSurfaces);
							grouping = true;
						} else {
							if (!ctrlDown) {
								cps.setSelected(false);
								selectedSurfaces.remove(cps);
							}
						}
					}
				}
				isDragging = true;
			}

			break;

		case MouseEvent.RELEASE:
			if (this.MODE == VMap.MODE_CALIBRATE) {
				if (snap) {
					for (SuperSurface ss : selectedSurfaces) {
						if (ss.getActivePoint() != 2000 && ss.getActivePoint() != -1) {
							int closestIndex = -1;
							int cornerIndex = -1;
							float closestDist = this.getSnapDistance()+1;
							for (int j = 0; j < surfaces.size(); j++) {
								if(surfaces.get(j).getId() != ss.getId()){
									for (int i = 0; i < surfaces.get(j).getCornerPoints().length; i++) {
										float dist = PApplet.dist(ss.getCornerPoint(ss.getActivePoint()).x, ss.getCornerPoint(ss.getActivePoint()).y, surfaces.get(j).getCornerPoint(i).x,
												surfaces.get(j).getCornerPoint(i).y);
										if (dist < this.getSnapDistance()) {
											if(dist < closestDist){ 
												closestDist = dist;
												closestIndex = j;
												cornerIndex = i;
											}
										}
									}
								}
							}
							if(closestDist > -1 && closestDist < this.getSnapDistance()){
								ss.setCornerPoint(ss.getActivePoint(), surfaces.get(closestIndex).getCornerPoint(cornerIndex).x, surfaces.get(closestIndex).getCornerPoint(cornerIndex).y);
							}
						}
					}
					int selection = 0;
					for (SuperSurface cps : surfaces) {
						cps.setActivePoint(-1);
						if (cps.getActiveCornerPointIndex(mX, mY) != -1)
							selection++;
					}
					
					if (isDragging)
						selection++;

					if (selection == 0) {
						for (SuperSurface ss : selectedSurfaces) {
							ss.setSelected(false);
						}
						grouping = false;
						selectedSurfaces.clear();
					}
				}
				
			}
			startPos = new PVector(0, 0);
			selectionTool = null;
			disableSelectionTool = false;
			isDragging = false;
			break;

		}
		prevMouse = new PVector(mX, mY, 0);
	}

	
	/**
	 * Delete the selected surfaces 
	 */
	public void removeSelectedSurfaces(){
		for (SuperSurface ss : selectedSurfaces) {
			for (int i = surfaces.size() - 1; i >= 0; i--) {
				if (ss.getId() == surfaces.get(i).getId()) {
					if (ss.isLocked()) return;
					if (this.getDebug())
						PApplet.println("Keystone --> DELETED SURFACE with ID: #" + ss.getId());
					surfaces.remove(i);
				}
			}
		}
		this.setGrouping(false);
		selectedSurfaces.clear();
		if (surfaces.size() == 0)
			numAddedSurfaces = 0;
	}

	public static <T> void removeDuplicates(ArrayList<T> list) {
		int size = list.size();
		int out = 0;
		{
			final Set<T> encountered = new HashSet<T>();
			for (int in = 0; in < size; in++) {
				final T t = list.get(in);
				final boolean first = encountered.add(t);
				if (first) {
					list.set(out++, t);
				}
			}
		}
		while (out < size) {
			list.remove(--size);
		}
	}

	// This section has a partial list of
	//  the PGraphics functions that get passed
	//  through to the defaultDrawBuffer
	// This allows you to do the simple mode of VMap,
	//  where you just do stuff like
	//   vmap.rect()
	
	public void ambient(float gray){ defaultDrawBuffer.ambient(gray);}
	public void ambient(float v1, float v2, float v3){ defaultDrawBuffer.ambient(v1,v2,v3);}
	public void ambient(int rgb){defaultDrawBuffer.ambient(rgb);}
	public void ambientLight(float v1, float v2, float v3){defaultDrawBuffer.ambientLight(v1, v2, v3);}
	public void ambientLight(float v1, float v2, float v3, float x, float y, float z) {defaultDrawBuffer.ambientLight(v1, v2, v3, x, y, z);}
	public void applyMatrix(float n00, float n01, float n02, float n10, float n11, float n12) { defaultDrawBuffer.applyMatrix(n00, n01, n02, n10, n11, n12);}
	public void applyMatrix(float n00, float n01, float n02, float n03, float n10, float n11, float n12, float n13, float n20, float n21, float n22, float n23, float n30, float n31, float n32, float n33) {defaultDrawBuffer.applyMatrix(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);}
	public void applyMatrix(PMatrix source){defaultDrawBuffer.applyMatrix(source);}
	public void applyMatrix(PMatrix2D source){defaultDrawBuffer.applyMatrix(source);}
	public void applyMatrix(PMatrix3D source){defaultDrawBuffer.applyMatrix(source);}
	public void arc(float a, float b, float c, float d, float start, float stop){defaultDrawBuffer.arc(a, b, c, d, start, stop);}
	public void arc(float a, float b, float c, float d, float start, float stop, int mode) {defaultDrawBuffer.arc(a, b, c, d, start, stop, mode);}
	public void background(float gray){defaultDrawBuffer.background(gray);}
	public void background(float gray, float alpha) {defaultDrawBuffer.background(gray, alpha);}
	public void background(float v1, float v2, float v3){defaultDrawBuffer.background(v1, v2, v3);}
	public void background(float v1, float v2, float v3, float alpha) {defaultDrawBuffer.background(v1, v2, v3, alpha);}
	public void background(int rgb){defaultDrawBuffer.background(rgb);}
	public void background(int rgb, float alpha) {defaultDrawBuffer.background(rgb, alpha);}
	public void background(PImage image){defaultDrawBuffer.background(image);}
	public void beginCamera(){defaultDrawBuffer.beginCamera();}
	public void beginContour() {defaultDrawBuffer.beginContour();}
	public void beginDraw(){defaultDrawBuffer.beginDraw();}
	public PGL beginPGL() {return defaultDrawBuffer.beginPGL();}
	public void beginRaw(PGraphics rawGraphics){defaultDrawBuffer.beginRaw(rawGraphics);}
	public void beginShape(){defaultDrawBuffer.beginShape();}
	public void bezier(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {defaultDrawBuffer.bezier(x1, y1, x2, y2, x3, y3, x4, y4);}
	public void bezier(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4){defaultDrawBuffer.bezier(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);}
	public void bezierDetail(int detail){defaultDrawBuffer.bezierDetail(detail);}
	public float bezierPoint(float a, float b, float c, float d, float t) {return defaultDrawBuffer.bezierPoint(a, b, c, d, t);}
	public float bezierTangent(float a, float b, float c, float d, float t){return defaultDrawBuffer.bezierTangent(a, b, c, d, t);}
	public void bezierVertex(float x2, float y2, float x3, float y3, float x4, float y4) {defaultDrawBuffer.bezierVertex(x2, y2, x3, y3, x4, y4);}
	public void bezierVertex(float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4){defaultDrawBuffer.bezierVertex(x2, y2, z2, x3, y3, z3, x4, y4, z4);}
	public void blendMode(int mode){defaultDrawBuffer.blendMode(mode);}
	public float blue(int rgb){return defaultDrawBuffer.blue(rgb);}
	public void box(float size){defaultDrawBuffer.box(size);}
	public void box(float w, float h, float d) {defaultDrawBuffer.box(w, h, d);}
	public float brightness(int rgb){return defaultDrawBuffer.brightness(rgb);}
	public void camera(){defaultDrawBuffer.camera();}
	public void camera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {defaultDrawBuffer.camera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);}
	public void clear() {defaultDrawBuffer.clear();}
	public void clip(float a, float b, float c, float d){defaultDrawBuffer.clip(a, b, c, d);}
	public int color(float gray) {return defaultDrawBuffer.color(gray);}
	public int color(float gray, float alpha) {return defaultDrawBuffer.color(gray, alpha);}
	public int color(float v1, float v2, float v3) {return defaultDrawBuffer.color(v1, v2, v3);}
	public int color(float v1, float v2, float v3, float a) {return defaultDrawBuffer.color(v1, v2, v3, a);}
	public int color(int c) {return defaultDrawBuffer.color(c);}
	public int color(int c, float alpha) {return defaultDrawBuffer.color(c, alpha);}
	public int color(int c, int alpha) {return defaultDrawBuffer.color(c, alpha);}
	public int color(int v1, int v2, int v3) {return defaultDrawBuffer.color(v1, v2, v3);}
	public int color(int v1, int v2, int v3, int a) {return defaultDrawBuffer.color(v1, v2, v3, a);}
	public void colorMode(int mode){defaultDrawBuffer.colorMode(mode);}
	public void colorMode(int mode, float max) {defaultDrawBuffer.colorMode(mode, max);}
	public void colorMode(int mode, float max1, float max2, float max3) {defaultDrawBuffer.colorMode(mode, max1, max2, max3);}
	public void colorMode(int mode, float max1, float max2, float max3, float maxA) {defaultDrawBuffer.colorMode(mode, max1, max2, max3, maxA);}
	public PShape createShape() {return defaultDrawBuffer.createShape();}
	public PShape createShape(int type) {return defaultDrawBuffer.createShape(type);}
	public PShape createShape(int kind, float... p) {return defaultDrawBuffer.createShape(kind, p);}
//	public PSurface createSurface() {return defaultDrawBuffer.createSuface();}
	public void	curve(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {defaultDrawBuffer.curve(x1, y1, x2, y2, x3, y3, x4, y4);}
	public void	curve(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {defaultDrawBuffer.curve(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);}
	public void	curveDetail(int detail) {defaultDrawBuffer.curveDetail(detail);}
	public float	curvePoint(float a, float b, float c, float d, float t) {return defaultDrawBuffer.curvePoint(a, b, c, d, t);}
	public float	curveTangent(float a, float b, float c, float d, float t) {return defaultDrawBuffer.curveTangent(a, b, c, d, t);}
	public void	curveTightness(float tightness) {defaultDrawBuffer.curveTightness(tightness);}
	public void	curveVertex(float x, float y) {defaultDrawBuffer.curveVertex(x, y);}
	public void	curveVertex(float x, float y, float z)  {defaultDrawBuffer.curveVertex(x, y, z);}
	public void	directionalLight(float v1, float v2, float v3, float nx, float ny, float nz) {defaultDrawBuffer.directionalLight(v1, v2, v3, nx, ny, nz);}
	public boolean	displayable() {return defaultDrawBuffer.displayable();}
	public void	dispose() {defaultDrawBuffer.dispose();}
	public void	edge(boolean edge) {defaultDrawBuffer.edge(edge);}
	public void	ellipse(float a, float b, float c, float d) {defaultDrawBuffer.ellipse(a, b, c, d);}
	public void	ellipseMode(int mode) {defaultDrawBuffer.ellipseMode(mode);}
	public void	emissive(float gray) {defaultDrawBuffer.emissive(gray);}
	public void	emissive(float v1, float v2, float v3) {defaultDrawBuffer.emissive(v1, v2, v3);} 
	public void	emissive(int rgb) {defaultDrawBuffer.emissive(rgb);}
	public void	endCamera() {defaultDrawBuffer.endCamera();}
	public void	endContour()  {defaultDrawBuffer.endContour();}
	public void	endDraw() {defaultDrawBuffer.endDraw();}
	public void	endPGL()  {defaultDrawBuffer.endPGL();}
	public void	endRaw()  {defaultDrawBuffer.endRaw();}
	public void	endShape()  {defaultDrawBuffer.endShape();}
	public void	endShape(int mode) {defaultDrawBuffer.endShape(mode);}
	public void	fill(float gray)  {defaultDrawBuffer.fill(gray);}
	public void	fill(float gray, float alpha)  {defaultDrawBuffer.fill(gray, alpha);}
	public void	fill(float v1, float v2, float v3)  {defaultDrawBuffer.fill(v1, v2, v3);}
	public void	fill(float v1, float v2, float v3, float alpha)  {defaultDrawBuffer.fill(v1, v2, v3, alpha);}
	public void	fill(int rgb) {defaultDrawBuffer.fill(rgb);}
	public void	fill(int rgb, float alpha)  {defaultDrawBuffer.fill(rgb, alpha);}
	public void	filter(PShader shader)  {defaultDrawBuffer.filter(shader);}
	public void	filter(int kind){defaultDrawBuffer.filter(kind);}
	public void	filter(int kind, float param) { defaultDrawBuffer.filter(kind, param);}
	public void	flush()  {defaultDrawBuffer.flush();}
	public void	frustum(float left, float right, float bottom, float top, float near, float far) {defaultDrawBuffer.frustum(left, right, bottom, top, near, far);}
	public Object	getCache(PImage image) {return defaultDrawBuffer.getCache(image);}
	public PMatrix	getMatrix() {return defaultDrawBuffer.getMatrix();}
	public PMatrix2D	getMatrix(PMatrix2D target) {return defaultDrawBuffer.getMatrix(target);}
	public PMatrix3D	getMatrix(PMatrix3D target) {return defaultDrawBuffer.getMatrix(target);}
	public PGraphics	getRaw() {return defaultDrawBuffer.getRaw();}
	public PStyle	getStyle() {return defaultDrawBuffer.getStyle();}
	public PStyle	getStyle(PStyle s) {return defaultDrawBuffer.getStyle(s);}
	public float	green(int rgb) {return defaultDrawBuffer.green(rgb);}
	public boolean	haveRaw() {return defaultDrawBuffer.haveRaw();}
	public void	hint(int which) {defaultDrawBuffer.hint(which);}
	public float	hue(int rgb) {return defaultDrawBuffer.hue(rgb);}
	public void	image(PImage img, float a, float b) {defaultDrawBuffer.image(img, a, b);}
	public void	image(PImage img, float a, float b, float c, float d)  {defaultDrawBuffer.image(img, a, b, c, d);}
	public void	image(PImage img, float a, float b, float c, float d, int u1, int v1, int u2, int v2) {defaultDrawBuffer.image(img, a, b, c, d, u1, v1, u2, v2);}
	public void	imageMode(int mode) {defaultDrawBuffer.imageMode(mode);}
	public boolean	is2D() {return defaultDrawBuffer.is2D();}
	public boolean	is3D() {return defaultDrawBuffer.is3D();}
	public boolean	isGL() {return defaultDrawBuffer.isGL();}
	public int	lerpColor(int c1, int c2, float amt) {return defaultDrawBuffer.lerpColor(c1, c2, amt);}
//	static int	lerpColor(int c1, int c2, float amt, int mode) {return defaultDrawBuffer.lerpColor
	public void	lightFalloff(float constant, float linear, float quadratic) {defaultDrawBuffer.lightFalloff(constant, linear, quadratic);}
	public void	lights() {defaultDrawBuffer.lights();}
	public void	lightSpecular(float v1, float v2, float v3) {defaultDrawBuffer.lightSpecular(v1, v2, v3);}
	public void	line(float x1, float y1, float x2, float y2) {defaultDrawBuffer.line(x1, y1, x2, y2);}
	public void	line(float x1, float y1, float z1, float x2, float y2, float z2) {defaultDrawBuffer.line(x1, y1, z1, x2, y2, z2);}
	public PShader	loadShader(String fragFilename) {return defaultDrawBuffer.loadShader(fragFilename);}
	public PShader	loadShader(String fragFilename, String vertFilename)  {return defaultDrawBuffer.loadShader(fragFilename, vertFilename);}
	public PShape	loadShape(String filename)  {return defaultDrawBuffer.loadShape(filename);}
	public PShape	loadShape(String filename, String options)  {return defaultDrawBuffer.loadShape(filename, options);}
	public float	modelX(float x, float y, float z) {return defaultDrawBuffer.modelX(x, y, z);}
	public float	modelY(float x, float y, float z) {return defaultDrawBuffer.modelY(x, y, z);}
	public float	modelZ(float x, float y, float z) {return defaultDrawBuffer.modelZ(x, y, z);}
	public void	noClip() {defaultDrawBuffer.noClip();}
	public void	noFill() {defaultDrawBuffer.noFill();}
	public void	noLights() {defaultDrawBuffer.noLights();}
	public void	normal(float nx, float ny, float nz) {defaultDrawBuffer.normal(nx, ny, nz);}
	public void	noSmooth()  {defaultDrawBuffer.noSmooth();}
	public void	noStroke() {defaultDrawBuffer.noStroke();}
	public void	noTexture() {defaultDrawBuffer.noTexture();}
	public void	noTint() {defaultDrawBuffer.noTint();}
	public void	ortho() {defaultDrawBuffer.ortho();}
	public void	ortho(float left, float right, float bottom, float top)  {defaultDrawBuffer.ortho(left, right, bottom, top);}
	public void	ortho(float left, float right, float bottom, float top, float near, float far) {defaultDrawBuffer.ortho(left, right, bottom, top, near, far);}
	public void	perspective() {defaultDrawBuffer.perspective();}
	public void	perspective(float fovy, float aspect, float zNear, float zFar)  {defaultDrawBuffer.perspective(fovy, aspect, zNear, zFar);}
	public void	point(float x, float y) {defaultDrawBuffer.point(x, y);}
	public void	point(float x, float y, float z)  {defaultDrawBuffer.point(x, y, z);}
	public void	pointLight(float v1, float v2, float v3, float x, float y, float z) {defaultDrawBuffer.pointLight(v1, v2, v3, x, y, z);}
	public void	popMatrix() {defaultDrawBuffer.popMatrix();}
	public void	popStyle() {defaultDrawBuffer.popStyle();}
	public void	printCamera() {defaultDrawBuffer.printCamera();}
	public void	printMatrix() {defaultDrawBuffer.printMatrix();}
	public void	printProjection() {defaultDrawBuffer.printProjection();}
	public void	pushMatrix() {defaultDrawBuffer.pushMatrix();}
	public void	pushStyle() {defaultDrawBuffer.pushStyle();}
	public void	quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {defaultDrawBuffer.quad(x1, y1, x2, y2, x3, y3, x4, y4);}
	public void	quadraticVertex(float cx, float cy, float x3, float y3)  {defaultDrawBuffer.quadraticVertex(cx, cy, x3, y3);}
	public void	quadraticVertex(float cx, float cy, float cz, float x3, float y3, float z3) {defaultDrawBuffer.quadraticVertex(cx, cy, cz, x3, y3, z3);}
	public void	rect(float a, float b, float c, float d) {defaultDrawBuffer.rect(a, b, c, d);}
	public void	rect(float a, float b, float c, float d, float r)  {defaultDrawBuffer.rect(a, b, c, d, r);}
	public void	rect(float a, float b, float c, float d, float tl, float tr, float br, float bl)  {defaultDrawBuffer.rect(a, b, c, d, tl, tr, br, bl);}
	public void	rectMode(int mode) {defaultDrawBuffer.rectMode(mode);}
	public float	red(int rgb) {return defaultDrawBuffer.red(rgb);}
	public void	removeCache(PImage image) {defaultDrawBuffer.removeCache(image);}
	public void	resetMatrix() {defaultDrawBuffer.resetMatrix();}
	public void	resetShader() {defaultDrawBuffer.resetShader();}
	public void	resetShader(int kind)  {defaultDrawBuffer.resetShader(kind);}
	public void	rotate(float angle) {defaultDrawBuffer.rotate(angle);}
	public void	rotate(float angle, float x, float y, float z) {defaultDrawBuffer.rotate(angle, x, y, z);}
	public void	rotateX(float angle) {defaultDrawBuffer.rotateX(angle);}
	public void	rotateY(float angle) {defaultDrawBuffer.rotateY(angle);}
	public void	rotateZ(float angle) {defaultDrawBuffer.rotateZ(angle);}
	public float	saturation(int rgb) {return defaultDrawBuffer.saturation(rgb);}
	public boolean	save(String filename) {return defaultDrawBuffer.save(filename);}
	public void	scale(float s) {defaultDrawBuffer.scale(s);}
	public void	scale(float x, float y) {defaultDrawBuffer.scale(x, y);}
	public void	scale(float x, float y, float z)  {defaultDrawBuffer.scale(x, y, z);}
	public float	screenX(float x, float y) {return defaultDrawBuffer.screenX(x, y);}
	public float	screenX(float x, float y, float z) {return defaultDrawBuffer.screenX(x, y, z);}
	public float	screenY(float x, float y) {return defaultDrawBuffer.screenY(x, y);}
	public float	screenY(float x, float y, float z) {return defaultDrawBuffer.screenY(x, y, z);}
	public float	screenZ(float x, float y, float z) {return defaultDrawBuffer.screenZ(x, y, z);}
	public void	setCache(PImage image, Object storage) {defaultDrawBuffer.setCache(image, storage);}
	public void	setMatrix(PMatrix source) {defaultDrawBuffer.setMatrix(source);}
	public void	setMatrix(PMatrix2D source) {defaultDrawBuffer.setMatrix(source);}
	public void	setMatrix(PMatrix3D source) {defaultDrawBuffer.setMatrix(source);}
	public void	setParent(PApplet parent)  {defaultDrawBuffer.setParent(parent);}
	public void	setPath(String path)  {defaultDrawBuffer.setPath(path);}
	public void	setPrimary(boolean primary) {defaultDrawBuffer.setPrimary(primary);}
	public void	setSize(int w, int h) {defaultDrawBuffer.setSize(w, h);}
	public void	shader(PShader shader) {defaultDrawBuffer.shader(shader);}
	public void	shader(PShader shader, int kind)  {defaultDrawBuffer.shader(shader, kind);}
	public void	shape(PShape shape)  {defaultDrawBuffer.shape(shape);}
	public void	shape(PShape shape, float x, float y) {defaultDrawBuffer.shape(shape, x, y);}
	public void	shape(PShape shape, float a, float b, float c, float d)  {defaultDrawBuffer.shape(shape, a, b, c, d);}
	public void	shapeMode(int mode) {defaultDrawBuffer.shapeMode(mode);}
	public void	shearX(float angle) {defaultDrawBuffer.shearX(angle);}
	public void	shearY(float angle) {defaultDrawBuffer.shearY(angle);}
	public void	shininess(float shine) {defaultDrawBuffer.shininess(shine);}
//	static void	showDepthWarning(String method) {defaultDrawBuffer
//	static void	showDepthWarningXYZ(String method) {defaultDrawBuffe
//	static void	showException(String msg) {defaultDrawBuffe
//	static void	showMethodWarning(String method) {defaultDrawBuffe
//	static void	showMissingWarning(String method) {defaultDrawBuffe
//	static void	showVariationWarning(Stringstatic void	showWarning(String msg, Object... args) str) {defaultDrawBuffe
//	static void	showWarning(String msg) {defaultDrawBuffe
	public void	smooth() {defaultDrawBuffer.smooth();}
	public void	smooth(int quality) {defaultDrawBuffer.smooth(quality);}
	public void	specular(float gray) {defaultDrawBuffer.specular(gray);}
	public void	specular(float v1, float v2, float v3) {defaultDrawBuffer.specular(v1, v2, v3);} 
	public void	specular(int rgb) {defaultDrawBuffer.specular(rgb);}
	public void	sphere(float r) {defaultDrawBuffer.sphere(r);}
	public void	sphereDetail(int res) {defaultDrawBuffer.sphereDetail(res);}
	public void	sphereDetail(int ures, int vres) {defaultDrawBuffer.sphereDetail(ures, vres);}
	public void	spotLight(float v1, float v2, float v3, float x, float y, float z, float nx, float ny, float nz, float angle, float concentration) {defaultDrawBuffer.spotLight(v1, v2, v3, x, y, z, nx, ny, nz, angle, concentration);}
	public void	stroke(float gray)  {defaultDrawBuffer.stroke(gray);}
	public void	stroke(float gray, float alpha)  {defaultDrawBuffer.stroke(gray, alpha);}
	public void	stroke(float v1, float v2, float v3)  {defaultDrawBuffer.stroke(v1, v2, v3);}
	public void	stroke(float v1, float v2, float v3, float alpha)  {defaultDrawBuffer.stroke(v1, v2, v3, alpha);}
	public void	stroke(int rgb) {defaultDrawBuffer.stroke(rgb);}
	public void	stroke(int rgb, float alpha) {defaultDrawBuffer.stroke(rgb, alpha);}
	public void	strokeCap(int cap) {defaultDrawBuffer.strokeCap(cap);}
	public void	strokeJoin(int join) {defaultDrawBuffer.strokeJoin(join);}
	public void	strokeWeight(float weight) {defaultDrawBuffer.strokeWeight(weight);}
	public void	style(PStyle s) {defaultDrawBuffer.style(s);}
	public void	text(char[] chars, int start, int stop, float x, float y) {defaultDrawBuffer.text(chars, start, stop, x, y);}
	public void	text(char[] chars, int start, int stop, float x, float y, float z) {defaultDrawBuffer.text(chars, start, stop, x, y, z);}
	public void	text(char c, float x, float y) {defaultDrawBuffer.text(c, x, y);}
	public void	text(char c, float x, float y, float z)  {defaultDrawBuffer.text(c, x, y, z);}
	public void	text(float num, float x, float y) {defaultDrawBuffer.text(num, x, y);}
	public void	text(float num, float x, float y, float z)  {defaultDrawBuffer.text(num, x, y, z);}
	public void	text(int num, float x, float y)  {defaultDrawBuffer.text(num, x, y);}
	public void	text(int num, float x, float y, float z)  {defaultDrawBuffer.text(num, x, y, z);}
	public void	text(String str, float x, float y) {defaultDrawBuffer.text(str, x, y);}
	public void	text(String str, float x, float y, float z) {defaultDrawBuffer.text(str, x, y, z);}
	public void	text(String str, float x1, float y1, float x2, float y2) {defaultDrawBuffer.text(str, x1, y1, x2, y2);}
	public void	textAlign(int alignX)  {defaultDrawBuffer.textAlign(alignX);}
	public void	textAlign(int alignX, int alignY) {defaultDrawBuffer.textAlign(alignX, alignY);}
	public float	textAscent() {return defaultDrawBuffer.textAscent();}
	public float	textDescent() {return defaultDrawBuffer.textDescent();}
	public void	textFont(PFont which) {defaultDrawBuffer.textFont(which);}
	public void	textFont(PFont which, float size) {defaultDrawBuffer.textFont(which, size);}
	public void	textLeading(float leading) {defaultDrawBuffer.textLeading(leading);}
	public void	textMode(int mode) {defaultDrawBuffer.textMode(mode);}
	public void	textSize(float size) {defaultDrawBuffer.textSize(size);}
	public void	texture(PImage image) {defaultDrawBuffer.texture(image);}
	public void	textureMode(int mode) {defaultDrawBuffer.textureMode(mode);}
	public void	textureWrap(int wrap) {defaultDrawBuffer.textureWrap(wrap);}
	public float	textWidth(char c) {return defaultDrawBuffer.textWidth(c);}
	public float	textWidth(char[] chars, int start, int length) {return defaultDrawBuffer.textWidth(chars, start, length);}
	public float	textWidth(String str) {return defaultDrawBuffer.textWidth(str);}
	public void	tint(float gray)  {defaultDrawBuffer.tint(gray);}
	public void	tint(float gray, float alpha) {defaultDrawBuffer.tint(gray, alpha);}
	public void	tint(float v1, float v2, float v3)  {defaultDrawBuffer.tint(v1, v2, v3);}
	public void	tint(float v1, float v2, float v3, float alpha) {defaultDrawBuffer.tint(v1, v2, v3, alpha);}
	public void	tint(int rgb) {defaultDrawBuffer.tint(rgb);}
	public void	tint(int rgb, float alpha)  {defaultDrawBuffer.tint(rgb, alpha);}
	public void	translate(float x, float y) {defaultDrawBuffer.translate(x, y);}
	public void	translate(float x, float y, float z)  {defaultDrawBuffer.translate(x, y, z);}
	public void	triangle(float x1, float y1, float x2, float y2, float x3, float y3) {defaultDrawBuffer.triangle(x1, y1, x2, y2, x3, y3);}
	public void	vertex(float[] v) {defaultDrawBuffer.vertex(v);}
	public void	vertex(float x, float y) {defaultDrawBuffer.vertex(x, y);}
	public void	vertex(float x, float y, float z) {defaultDrawBuffer.vertex(x, y, z);}
	public void	vertex(float x, float y, float u, float v) {defaultDrawBuffer.vertex(x, y, u, v);}
	public void	vertex(float x, float y, float z, float u, float v) {defaultDrawBuffer.vertex(x, y, z, u, v);}
}


