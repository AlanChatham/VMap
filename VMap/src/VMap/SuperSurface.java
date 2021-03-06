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

import java.awt.Polygon;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PShader;
import processing.core.PApplet;

public abstract class SuperSurface{
	
	protected PApplet parent;
	protected VMap sm;

	final protected int MODE_RENDER = 0;
	final protected int MODE_CALIBRATE = 1;

	protected int MODE = MODE_RENDER;
	
	protected int activePoint = -1; // Which corner point is selected?

	static protected int GRID_LINE_COLOR;
	static protected int GRID_LINE_SELECTED_COLOR;
	static protected int SELECTED_OUTLINE_OUTER_COLOR;
	static protected int CORNER_MARKER_COLOR;
	static protected int SELECTED_OUTLINE_INNER_COLOR;
	static protected int SELECTED_CORNER_MARKER_COLOR;
	
	// The four corners of the transformed quad (in 2d screen space)
	protected PVector[] cornerPoints;
	
	public final static int QUAD = 0;
	public final static int BEZIER = 1;
	
	public final static int CLOCKWISE = 0;
	public final static int COUNTERCLOCKWISE = 1;
	
	protected PImage texture;
	protected String textureFilename;
	
	protected int type;
	
	protected int surfaceId;
	protected String surfaceName;
	
	protected int ccolor;

	protected Polygon poly = new Polygon();

	protected float currentZ;
	protected boolean shaking;
	protected float shakeStrength;
	protected int shakeSpeed;
	protected float shakeAngle;
	protected int fallOfSpeed;

	protected boolean hidden = false;
	
	protected boolean isSelected;
	protected boolean isLocked;
	protected int selectedCorner;
	
	// Allow each SuperSurface to have it's own shader, just in case
	protected PShader shader;
	
	// Default constructor
	/**
	 * Default constructor
	 */
	public SuperSurface(){
		this.ccolor = 0;
	}
	
	/**
	 * Constructor used to create a new Surface. This should always be used when creating a new surface.
	 * @param fileName is the image texture to use
	 * @param type is either QUAD(0) or BEZIER(1) because Ixagon doesn't know what an abstract class is
	 * @param parent is the processing applet for some reason
	 * @param ks is the VMap this belongs to, again, why?
	 * @param x is the x position
	 * @param y is the y position
	 * @param res is the resolution of the surface
	 * @param id is the id number
	 */
	public SuperSurface(String fileName, int type, PApplet parent, VMap ks, float x, float y, int res, int id){
		this.type = type;
		this.textureFilename = fileName;
		if (this.textureFilename != null){
			this.texture = parent.loadImage(fileName);
		}
		// Set some default values for internal variables
		this.ccolor = 0;
		
/*		switch(type){
		case QUAD:
			quadSurface = new QuadSurface(parent, ks, x, y, res, id);
			break;
			
		case BEZIER:
			if(res%2 != 0) res++;
			bezierSurface = new BezierSurface(parent, ks, x, y, res, id);
			break;
		}
		*/
	}
	/**
	 * Constructor used to create a new Surface. This should always be used when creating a new surface.
	 * @param type is either QUAD(0) or BEZIER(1) because Ixagon doesn't know what an abstract class is
	 * @param parent is the processing applet for some reason
	 * @param ks is the VMap this belongs to, again, why?
	 * @param x is the x position
	 * @param y is the y position
	 * @param res is the resolution of the surface
	 * @param id is the id number
	 */
	public SuperSurface(int type, PApplet parent, VMap ks, float x, float y, int res, int id){
		this(null, type, parent, ks, x, y, res, id);
	}
	
	/**
	 * Gets the surface's current texture
	 * @return Current texture, null if no texture assigned
	 */
	public PImage getTexture(){
		return this.texture;
	}
	
	/**
	 * Sets the surface's texture
	 * @param tex Texture to set
	 */
	public void setTexture(PImage tex){
		this.texture = tex;
	}
	
	/**
	 * Sets the surface's shader
	 * @param tex Texture to set
	 */
	public void setShader(PShader s){
		this.shader = s;
	}
	
	/**
	 * gets the surface's texture
	 * @return PShader set to this texture, often null
	 */
	public PShader getShader(){
		return this.shader;
	}

	/**
	 * Constructor for loading a surface from file
	 * @param type
	 * @param parent
	 * @param ks
	 * @param xml
	 */
/*public abstract SuperSurface(string fileName, PApplet parent, VMap ks, XML xml){
		this.type = type;
		
		switch(type){
		case QUAD:
			quadSurface = new QuadSurface(parent, ks, xml);
			break;
			
		case BEZIER:
			bezierSurface = new BezierSurface(parent, ks, xml);
			break;
		}
	}*/

	/**
	 * Sets the fill color of the surface in calibration mode
	 * @param ccolor
	 */
	public void setColor(int ccolor) {
		this.ccolor = ccolor;
	}
	
	/**
	 * Get the fill color of the surface in calibration mode
	 * @return
	 */
	public int getColor() {
		return ccolor;
	}

	
	/**
	 * The the amount of subdivision currently used
	 * @return
	 */
	abstract public int getRes();
	
	/**
	 * Increase the amount of subdivision
	 */
	public abstract void increaseResolution();
	
	/**
	 * Decrease the amount of subdivision
	 */
	public abstract void decreaseResolution();
	
	
	/**
	 * Set if the surface should be hidden 
	 * @param hidden
	 */
	public void setHide(boolean hidden) {
		this.hidden = hidden;
	}
	
	/**
	 * See if surface is hidden
	 * @return
	 */
	public boolean isHidden() {
		return hidden;
	}
	
	/**
	 * Set Z-displacement for all coordinates of surface
	 * @param currentZ
	 */
	public void setZ(float currentZ){
		this.currentZ = currentZ;
	}
	
	/**
	 * Set parameters for shaking the surface.
	 * @param strength max Z-displacement
	 * @param speed vibration speed
	 * @param fallOfSpeed 1-1000 == how fast strength is diminished
	 */
	public void setShake(int strength, int speed, int fallOfSpeed){
		if(fallOfSpeed < 1) fallOfSpeed = 1;
		if(fallOfSpeed > 1000) fallOfSpeed = 1000;
		shaking = true;
		this.shakeStrength = strength;
		this.shakeSpeed = speed;
		this.fallOfSpeed = 1000-fallOfSpeed;
		shakeAngle = 0;
	}
	
	/**
	 * Tells surface to shake (will only do something if setShake has been called quite recently)
	 */
	public void shake(){
		if(shaking){
			shakeAngle += (float)shakeSpeed/1000;
			shakeStrength *= ((float)this.fallOfSpeed/1000);
			float shakeZ = (float) (Math.sin(shakeAngle)*shakeStrength);
			this.setZ(shakeZ);
			if(shakeStrength < 1){
				shaking = false;
			}
		}
	}
	
	/**
	 * Set surface to calibration mode
	 */
	public void setModeCalibrate() {
		this.MODE = this.MODE_CALIBRATE;
	}

	/**
	 * Set surface to render mode
	 */
	public void setModeRender() {
		this.MODE = this.MODE_RENDER;
	}

	/**
	 * Toggle surface mode
	 */
	public void toggleMode() {
		if (this.MODE == this.MODE_RENDER) {
			this.MODE = this.MODE_CALIBRATE;
		} else {
			this.MODE = this.MODE_RENDER;
		}
	}
	
	/**
	 * Get the index of active corner (or surface)
	 * @return int index of active corner or surface
	 */
	public int getActivePoint() {
		return this.activePoint;
	}
	
	/**
	 * Set index of which corner is active
	 * @param activePoint
	 */
	public void setActivePoint(int activePoint) {
		this.activePoint = activePoint;
	}

	/**
	 * Get a specific corner
	 * @param index
	 * @return PVector indexed corner
	 */
	public PVector getCornerPoint(int index) {
		return this.cornerPoints[index];
	}
	
	/**
	 * Get all corners
	 * @return PVector array of the corner point
	 */
	public PVector[] getCornerPoints() {
		return this.cornerPoints;
	}
	
	/**
	 * Rotate the corners of surface (0=ClockWise, 1=CounterClockWise)
	 * TODO Broken for Bezier Surfaces
	 * @param direction CLOCKWISE or COUNTERCLOCKWISE
	 */
	public abstract void rotateCornerPoints(int direction);
	
	/**
	 * Get the surfaces ID
	 * @return int ID
	 */
	public int getId() {
		return this.surfaceId;
	}
	
	/**
	 * Toggle if surface is locked (a locked surface cannot be moved or manipulated in calibration mode, but other surfaces still snap to it)
	 */
	public void toggleLocked() {
		this.isLocked = !this.isLocked;
	}
	
	/**
	 * See if the surface is locked
	 * @return boolean Is the surface locked?
	 */
	public boolean isLocked(){
		return this.isLocked;
	}
	
	/**
	 * Set if the surface is locked
	 * @param isLocked
	 */
	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}
	
	/**
	 * See if the surface is selected
	 * @return boolean Is the surface selected?
	 */
	public boolean isSelected() {
		return this.isSelected;
	}
	
	/**
	 * Set if the surface is selected
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		this.isSelected = selected;
	}
	
	/**
	 * Get the currently selected corner
	 * @return int index of selected corner
	 */
	public int getSelectedCorner() {
		return this.selectedCorner;
	}

	/**
	 * Set target corner to selected
	 * @param selectedCorner
	 */
	public void setSelectedCorner(int selectedCorner) {
		this.selectedCorner = selectedCorner;
	}
	
	/**
	 * Checks if the coordinates is close to any of the corners, and if not, checks if the coordinates are inside the surface.
	 * Returns the index of the corner (0,1,2,3) or (4) if coordinates was inside the surface 
	 * @param mX
	 * @param mY
	 * @return
	 */
	public int getActiveCornerPointIndex(int mX, int mY) {
		for (int i = 0; i < this.cornerPoints.length; i++) {
			if (PApplet.dist(mX, mY, this.cornerPoints[i].x, this.cornerPoints[i].y) < sm.getSelectionDistance()) {
				setSelectedCorner(i);
				return i;
			}
		}
		if (this.isInside(mX, mY))
			return 2000; 
		return -1;
	}

	/**
	 * Check if coordinates are inside the surface
	 * @param mX X coordinate of the checked point
	 * @param mY Y coordinate of the checked point
	 * @return boolean Whether the coordinates are inside the surface
	 */
	public boolean isInside(float mX, float mY) {
		if (poly.contains(mX, mY))
			return true;
		return false;
	}
	
	/**
	 * Get the surface's polygon
	 * @return Polygon surface's polygon
	 */
	public Polygon getPolygon(){
		return poly;
	}
	
	/**
	 * Manually set coordinates for all corners of the surface
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 */
	public void setCornerPoints(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
		this.cornerPoints[0].x = x0;
		this.cornerPoints[0].y = y0;

		this.cornerPoints[1].x = x1;
		this.cornerPoints[1].y = y1;

		this.cornerPoints[2].x = x2;
		this.cornerPoints[2].y = y2;

		this.cornerPoints[3].x = x3;
		this.cornerPoints[3].y = y3;

		this.updateTransform();
	}
	
	/**
	 * Set target corner point to coordinates
	 * @param pointIndex
	 * @param x
	 * @param y
	 */
	public void setCornerPoint(int pointIndex, float x, float y) {
		this.cornerPoints[pointIndex].x = x;
		this.cornerPoints[pointIndex].y = y;
		this.updateTransform();
	}
	
	protected abstract void updateTransform();
	
	/**
	 * Get the average center point of the surface
	 * @return PVector center point
	 */
	public PVector getCenter() {
		// Find the average position of all the control points, use that as the
		// center point.
		float avgX = 0;
		float avgY = 0;
		for (int c = 0; c < 4; c++) {
			avgX += this.cornerPoints[c].x;
			avgY += this.cornerPoints[c].y;
		}
		avgX /= 4;
		avgY /= 4;

		return new PVector(avgX, avgY);
	}
	
	/**
	 * Translates a point on the screen into a point in the surface. (not implemented in Bezier Surfaces)
	 * TODO: Implement in Bezier surfaces
	 * @param x
	 * @param y
	 * @return
	 */
//	public abstract PVector screenCoordinatesToQuad(float x, float y);
	
	public void render(){
		if(sm != null){
			// Update the shaking
			this.shake();
			// Handle rendering if there is a texture or not
			if (this.texture != null){
				this.render(sm.offScreenBuffer, this.texture);
			}
			else{
				this.render(sm.offScreenBuffer);
			}
		}
	}
	
	/**
	 * Renders the surface in calibration mode
	 * @param g PGraphics buffer to render to
	 */
	public abstract void render(PGraphics g);
	
	/**
	 * Render the surface with texture
	 * @param tex Texture to render to the surface
	 */
	public void render(PImage tex){
		this.texture = tex;
		render();
	}
	
	/**
	 * Render the surface with texture
	 * @param g PGraphics buffer to write to
	 * @param tex Texture to render to the surface
	 */
	public abstract void render(PGraphics g, PImage tex);
	
	/**
	 * Renders the control points of the surface to a PGraphics object, for calibration
	 * @param g PGraphics to draw on
	 */
	public abstract void renderControlPoints(PGraphics g);
	
	/**
	 * See which type this surface is
	 * @return
	 */
	public int getSurfaceType(){
		return type;
	}
	/**
	 * Assign a name to a surface
	 * @param surfaceName
	 */
	public void setSurfaceName(String surfaceName) {
		this.surfaceName = surfaceName;
	}
	/**
	 * Get the name of a surface
	 * @return String surfaceName
	 */
	public String getSurfaceName() {
		if(surfaceName == null) return String.valueOf(this.getId());
		return surfaceName;
	}
}
