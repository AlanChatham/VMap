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
import processing.core.PImage;
import processing.core.PVector;
import processing.data.XML;

public class VMap extends PImage implements PConstants{
		
	public final String VERSION = "2";

	private PApplet parent;
	private ArrayList<SuperSurface> surfaces;
	private ArrayList<SuperSurface> selectedSurfaces;
	
	//Off-screen PGraphics buffer, in case anyone needs access to that
	public PGraphics offScreenBuffer;
	
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

	private boolean shaking;
	private int shakeStrength;
	private int shakeSpeed;
	private float shakeAngle;
	private float shakeZ;
	
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
///// TODO: Implement mouse wheel with proper form		
//		parent.addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
//		    public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
//		      mouseWheelAction(evt.getWheelRotation());
//		  }}); 
	}
	
	/**
	 * Render method used when calibrating. Shouldn't be used for final rendering.
	 * @param glos PGrapics buffer for offscreen rendering
	 */
	public void render(PGraphics glos) {
		glos.beginDraw();
		glos.clear();
//		glos.clear(50); // Former value for Processing 1.5.1 code
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

			for (int i = 0; i < surfaces.size(); i++) {
				surfaces.get(i).render(offScreenBuffer);
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
				ss.render();
			}
		}
		// Now copy all those pixels that are offscreen to our own buffer
		//  This could probably be taken out by a big refactor, but I don't understand
		//  the PGraphics side well enough, and it's easier to understand new VMap(width, height)
		//  than createGraphics()...
		
		this.copy(offScreenBuffer, 0, 0, width, height, 0, 0, width, height);
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
	
	public void setSelectionMouseColor(int selectionMouseColor) {
		this.selectionMouseColor = selectionMouseColor;
	}

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
	
	public void addQuadSurface(String imageFile, int x, int y){
		QuadSurface s = new QuadSurface(imageFile, parent, this, x, y, 3, numAddedSurfaces);
		if (ccolor.length > 0)
			s.setColor(ccolor[numAddedSurfaces % ccolor.length]);
		s.setModeCalibrate();
		surfaces.add(s);
		numAddedSurfaces++;
	}
	
	public void addSurface(SuperSurface ss){
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
	 * KeyEvent method
	 * @param k KeyEvent thrown from Processing
	 */
	public void keyEvent(KeyEvent k) {
		if (MODE == MODE_RENDER)
			return; // ignore everything unless we're in calibration mode

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
				/*
			case KeyEvent.VK_O:
				for (SuperSurface ss : selectedSurfaces) {
					ss.increaseResolution();
				}
				break;

			case KeyEvent.VK_P:
				for (SuperSurface ss : selectedSurfaces) {
					ss.decreaseResolution();
				}
				break;
				
			case KeyEvent.VK_U:
				for (SuperSurface ss : selectedSurfaces) {
					ss.increaseHorizontalForce();
				}
				break;

			case KeyEvent.VK_I:
				for (SuperSurface ss : selectedSurfaces) {
					ss.decreaseHorizontalForce();
				}
				break;
			
			case KeyEvent.VK_J:
				for (SuperSurface ss : selectedSurfaces) {
					ss.increaseVerticalForce();
				}
				break;

			case KeyEvent.VK_K:
				for (SuperSurface ss : selectedSurfaces) {
					ss.decreaseVerticalForce();
				}
				break;

			case KeyEvent.VK_T:
				for (SuperSurface ss : selectedSurfaces) {
					ss.toggleLocked();
				}
				break;

			case KeyEvent.VK_BACK_SPACE:
				removeSelectedSurfaces();
				break;
			*/
			case PConstants.CONTROL:
			case CMD:
				ctrlDown = true;
				grouping = true;
				break;

			case PConstants.ALT:
				altDown = true;
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
}
