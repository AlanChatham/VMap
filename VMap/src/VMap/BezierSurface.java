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

import processing.core.PApplet;
import processing.data.XML;
import processing.core.PImage;
import processing.core.PGraphics;
import processing.core.PVector;

//Parts derived from MappingTools library

public class BezierSurface extends SuperSurface{

	// Contains all coordinates
	private PVector[][] vertexPoints;
	
	private BezierPoint3D[] cornerPoints;

	// Coordinates of the bezier vectors
	private PVector[] bezierPoints;
	
	// Displacement forces

	private int horizontalForce = 0;
	private int verticalForce = 0;

	private int GRID_RESOLUTION;
	private int DEFAULT_SIZE = 100;

	private int selectedBezierControl;
	
	/**
	 * Constructor for creating a new surface at X,Y with RES subdivision.
	 * @param parent
	 * @param ks
	 * @param x
	 * @param y
	 * @param res
	 * @param id
	 */
	BezierSurface(String fileName, PApplet parent, VMap ks, float x, float y, int res, int id) {
		this(parent, ks, x, y, res, id);
		this.textureFilename = fileName;
		if (this.textureFilename != null){
			this.texture = parent.loadImage(fileName);
		}
	}
	
	/**
	 * Constructor for creating a new surface at X,Y with RES subdivision.
	 * @param parent
	 * @param ks
	 * @param x
	 * @param y
	 * @param res
	 * @param id
	 */
	BezierSurface(PApplet parent, VMap ks, float x, float y, int res, int id) {
		init(parent, ks, res, id, null);
		this.cornerPoints[0].x = (float) (x - (this.DEFAULT_SIZE * 0.5));
		this.cornerPoints[0].y = (float) (y - (this.DEFAULT_SIZE * 0.5));

		this.cornerPoints[1].x = (float) (x + (this.DEFAULT_SIZE * 0.5));
		this.cornerPoints[1].y = (float) (y - (this.DEFAULT_SIZE * 0.5));

		this.cornerPoints[2].x = (float) (x + (this.DEFAULT_SIZE * 0.5));
		this.cornerPoints[2].y = (float) (y + (this.DEFAULT_SIZE * 0.5));

		this.cornerPoints[3].x = (float) (x - (this.DEFAULT_SIZE * 0.5));
		this.cornerPoints[3].y = (float) (y + (this.DEFAULT_SIZE * 0.5));
		
		//bezier points init
		this.cornerPoints[0].controlPoint0.x = (float) (this.cornerPoints[0].x + (this.DEFAULT_SIZE * 0.0));
		this.cornerPoints[0].controlPoint0.y = (float) (this.cornerPoints[0].y + (this.DEFAULT_SIZE * 0.3));

		this.cornerPoints[0].controlPoint1.x = (float) (this.cornerPoints[0].x + (this.DEFAULT_SIZE * 0.3));
		this.cornerPoints[0].controlPoint1.y = (float) (this.cornerPoints[0].y + (this.DEFAULT_SIZE * 0.0));

		this.cornerPoints[1].controlPoint0.x = (float) (this.cornerPoints[1].x - (this.DEFAULT_SIZE * 0.3));
		this.cornerPoints[1].controlPoint0.y = (float) (this.cornerPoints[1].y + (this.DEFAULT_SIZE * 0.0));

		this.cornerPoints[1].controlPoint1.x = (float) (this.cornerPoints[1].x - (this.DEFAULT_SIZE * 0.0));
		this.cornerPoints[1].controlPoint1.y = (float) (this.cornerPoints[1].y + (this.DEFAULT_SIZE * 0.3));
		
		this.cornerPoints[2].controlPoint0.x = (float) (this.cornerPoints[2].x - (this.DEFAULT_SIZE * 0.0));
		this.cornerPoints[2].controlPoint0.y = (float) (this.cornerPoints[2].y - (this.DEFAULT_SIZE * 0.3));

		this.cornerPoints[2].controlPoint1.x = (float) (this.cornerPoints[2].x - (this.DEFAULT_SIZE * 0.3));
		this.cornerPoints[2].controlPoint1.y = (float) (this.cornerPoints[2].y - (this.DEFAULT_SIZE * 0.0));

		this.cornerPoints[3].controlPoint0.x = (float) (this.cornerPoints[3].x + (this.DEFAULT_SIZE * 0.3));
		this.cornerPoints[3].controlPoint0.y = (float) (this.cornerPoints[3].y + (this.DEFAULT_SIZE * 0.0));

		this.cornerPoints[3].controlPoint1.x = (float) (this.cornerPoints[3].x - (this.DEFAULT_SIZE * 0.0));
		this.cornerPoints[3].controlPoint1.y = (float) (this.cornerPoints[3].y - (this.DEFAULT_SIZE * 0.3));

		this.updateTransform();
	}

	/**
	 * Constructor used when loading a surface from file
	 * @param parent
	 * @param ks
	 * @param xml
	 */
	BezierSurface(PApplet parent, VMap ks, XML xml) {
		if (xml.getString("filename") != null){
			init(xml.getString("filename"), parent, ks, xml.getInt("res"), xml.getInt("id"), xml.getString("name"));
		}
		else{
			init(parent, ks, (xml.getInt("res")), xml.getInt("id"), xml.getString("name"));
		}
		if (xml.getInt("lock") == 1)
			this.toggleLocked();

		// reload the Corners
		for (int i = 0; i < xml.getChildCount(); i++) {
			XML point = xml.getChild(i);
			if(point.getName().equals("cornerpoint"))
				setCornerPoint(point.getInt("i"), point.getFloat("x"), point.getFloat("y"));
			if(point.getName().equals("bezierpoint"))
				this.setBezierPoint(point.getInt("i"), point.getFloat("x"), point.getFloat("y"));
		}
		
		horizontalForce = xml.getInt("horizontalForce");
		verticalForce = xml.getInt("verticalForce");

		this.updateTransform();
	}
	
	/**
	 * Convenience method used by the constructors
	 * @param filename Image filename to use
	 * @param parent Parent applet
	 * @param ks Vmap object containing this surface
	 * @param res resolution
	 * @param id ID of this BezierSurface
	 * @param name Name
	 */
	private void init(String filename, PApplet parent, VMap ks, int res, int id, String name){
		init(parent, ks, res, id, name);
		this.textureFilename = filename;
		if (this.textureFilename != null){
			this.texture = parent.loadImage(filename);
		}
	}

	/**
	 * Convenience method used by the constructors.
	 * @param parent Parent applet
	 * @param ks Vmap object containing this surface
	 * @param res resolution
	 * @param id ID of this BezierSurface
	 */
	private void init(PApplet parent, VMap ks, int res, int id, String name) {
		this.parent = parent;
		this.sm = ks;
		this.surfaceName = name;
		this.surfaceId = id;
		this.GRID_RESOLUTION = res;
		this.horizontalForce = 0;
		this.verticalForce = 0;
		this.selectedBezierControl = -1;
		this.type = SuperSurface.BEZIER;

		this.cornerPoints = new BezierPoint3D[4];
		this.bezierPoints = new PVector[8];
		this.vertexPoints = new PVector[this.GRID_RESOLUTION+1][this.GRID_RESOLUTION+1];

		for (int i = 0; i < this.cornerPoints.length; i++) {
			this.cornerPoints[i] = new BezierPoint3D();
		}

		for (int i = 0; i < this.cornerPoints.length; i++) {
			this.bezierPoints[i * 2] = cornerPoints[i].controlPoint0;
			this.bezierPoints[i * 2 + 1] = cornerPoints[i].controlPoint1;
		}
		
		// Updating the superclass's cornerPoints array to point to the same stuff
		super.cornerPoints = this.cornerPoints;
		
		GRID_LINE_COLOR = parent.color(128, 128, 128);
		GRID_LINE_SELECTED_COLOR = parent.color(160, 160, 160);
		SELECTED_OUTLINE_OUTER_COLOR = parent.color(255, 255, 255, 128);
		SELECTED_OUTLINE_INNER_COLOR = parent.color(255, 255, 255);
		CORNER_MARKER_COLOR = parent.color(255, 255, 255);
		SELECTED_CORNER_MARKER_COLOR = parent.color(255, 0, 0);
		
		this.updateTransform();
	}
	
	

	/**
	 * Get the amount of subdivision used in the surface
	 * @return GRID_RESOLUTION Grid Resolution
	 */
	public int getRes() {
		// The actual resolution is the number of tiles, not the number of mesh
		// points
		return GRID_RESOLUTION;
	}

	/**
	 * Increase the subdivision
	 */
	public void increaseResolution() {
		this.GRID_RESOLUTION += 2;
		this.vertexPoints = new PVector[this.GRID_RESOLUTION+1][this.GRID_RESOLUTION+1];
		this.updateTransform();
	}

	/**
	 * Decrease the subdivision
	 */
	public void decreaseResolution() {
		if ((this.GRID_RESOLUTION - 1) > 2) {
			this.GRID_RESOLUTION -= 2;
			this.vertexPoints = new PVector[this.GRID_RESOLUTION+1][this.GRID_RESOLUTION+1];
			this.updateTransform();
		}
	}
	
	/**
	 * Increase the amount of horizontal displacement force used for spherical mapping for bezier surfaces. (using orthographic projection)
	 */
	public void increaseHorizontalForce(){
		this.horizontalForce += 2;
		this.updateTransform();
	}
	
	/**
	 * Decrease the amount of horizontal displacement force used for spherical mapping for bezier surfaces. (using orthographic projection)
	 */
	public void decreaseHorizontalForce(){
		this.horizontalForce -= 2;
		this.updateTransform();
	}
	
	/**
	 * Increase the amount of vertical displacement force used for spherical mapping for bezier surfaces. (using orthographic projection)
	 */
	public void increaseVerticalForce(){
		this.verticalForce += 2;
		this.updateTransform();
	
	}
	
	/**
	 * Decrease the amount of vertical displacement force used for spherical mapping for bezier surfaces. (using orthographic projection)
	 */
	public void decreaseVerticalForce(){
		this.verticalForce -= 2;
		this.updateTransform();
	}
	
	/**
	 * Get the amount of horizontal displacement force used for spherical mapping for bezier surfaces.
	 * @return horizontalForce
	 */
	public int getHorizontalForce(){
		return horizontalForce;
	}
	
	/**
	 * Get the amount of vertical displacement force used for spherical mapping for bezier surfaces.
	 * @return verticalForce
	 */
	public int getVerticalForce(){
		return verticalForce;
	}
	
	/**
	 * Set target bezier control point to coordinates
	 * @param pointIndex index of control point
	 * @param x X position of control point
	 * @param y Y position of control point
	 */
	public void setBezierPoint(int pointIndex, float x, float y) {
		this.bezierPoints[pointIndex].x = x;
		this.bezierPoints[pointIndex].y = y;
		this.updateTransform();
	}
	
	/**
	 * Get all bezier points
	 * @return PVector array of all bezier points
	 */
	public PVector[] getBezierPoints() {
		return this.bezierPoints;
	}

	/**
	 * Get the target bezier point
	 * @param index Index of bezeier
	 * @return PVector of bezier point
	 */
	public PVector getBezierPoint(int index) {
		return this.bezierPoints[index];
	}

	/**
	 * Set target bezier control to selected
	 * @param selectedBezierControl Index of Bezier control point to select
	 */
	public void setSelectedBezierControl(int selectedBezierControl) {
		this.selectedBezierControl = selectedBezierControl;
	}

	/**
	 * Get the currently selected bezier control
	 * @return currently selected control
	 */
	public int getSelectedBezierControl() {
		return selectedBezierControl;
	}
	
	/**
	 * Returns index 0-7 if coordinates are on a bezier control
	 * @param mX mouseX
	 * @param mY mouseY
	 * @return index of first nearby bezier point
	 */
	public int getActiveBezierPointIndex(int mX, int mY){
		for(int i = 0; i < this.bezierPoints.length; i++){
			if(PApplet.dist(mX, mY, this.bezierPoints[i].x, this.bezierPoints[i].y) < sm.getSelectionDistance()){
				this.setSelectedBezierControl(i);
				return i;
			}
		}
		return -1;
	}

	/**
	 * Recalculates all coordinates of the surface.
	 * Must be called whenever any change has been done to the surface.
	 */
	public void updateTransform(){
		
		for (int i = 0; i <= GRID_RESOLUTION; i++) {
			for (int j = 0; j <= GRID_RESOLUTION; j++) {
				
		        float start_x = parent.bezierPoint(cornerPoints[0].x, cornerPoints[0].controlPoint0.x, cornerPoints[3].controlPoint1.x, cornerPoints[3].x, (float)j/GRID_RESOLUTION);
		        //float end_x = parent.bezierPoint(cornerPoints[1].x, bezierPoints[3].x, bezierPoints[4].x, cornerPoints[2].x, (float)j/GRID_RESOLUTION);
		        float end_x = parent.bezierPoint(cornerPoints[1].x, cornerPoints[1].controlPoint1.x, cornerPoints[2].controlPoint0.x, cornerPoints[2].x, (float)j/GRID_RESOLUTION);

		        //float start_y = parent.bezierPoint(cornerPoints[0].y, bezierPoints[0].y, bezierPoints[7].y, cornerPoints[3].y, (float)j/GRID_RESOLUTION);
		        float start_y = parent.bezierPoint(cornerPoints[0].y, cornerPoints[0].controlPoint0.y, cornerPoints[3].controlPoint1.y, cornerPoints[3].y, (float)j/GRID_RESOLUTION);
		        //float end_y = parent.bezierPoint(cornerPoints[1].y, bezierPoints[3].y, bezierPoints[4].y, cornerPoints[2].y, (float)j/GRID_RESOLUTION);
		        float end_y = parent.bezierPoint(cornerPoints[1].y, cornerPoints[1].controlPoint1.y, cornerPoints[2].controlPoint0.y, cornerPoints[2].y, (float)j/GRID_RESOLUTION);

		        //float x = parent.bezierPoint(start_x, ((bezierPoints[1].x - bezierPoints[6].x) * (1.0f - (float)j/GRID_RESOLUTION)) + bezierPoints[6].x, ((bezierPoints[2].x - bezierPoints[5].x) * (1.0f - (float)j/GRID_RESOLUTION)) + bezierPoints[5].x, end_x, (float)i/GRID_RESOLUTION);
		        float x = parent.bezierPoint(start_x, ((cornerPoints[0].controlPoint1.x - cornerPoints[3].controlPoint0.x) * (1.0f - (float)j/GRID_RESOLUTION)) + cornerPoints[3].controlPoint0.x, ((cornerPoints[1].controlPoint0.x - cornerPoints[2].controlPoint1.x) * (1.0f - (float)j/GRID_RESOLUTION)) + cornerPoints[2].controlPoint1.x, end_x, (float)i/GRID_RESOLUTION);
		        //float y = parent.bezierPoint(start_y, ((bezierPoints[1].y - bezierPoints[6].y) * (1.0f - (float)j/GRID_RESOLUTION)) + bezierPoints[6].y, ((bezierPoints[2].y - bezierPoints[5].y) * (1.0f - (float)j/GRID_RESOLUTION)) + bezierPoints[5].y, end_y, (float)i/GRID_RESOLUTION);
		        float y = parent.bezierPoint(start_y, ((cornerPoints[0].controlPoint1.y - cornerPoints[3].controlPoint0.y) * (1.0f - (float)j/GRID_RESOLUTION)) + cornerPoints[3].controlPoint0.y, ((cornerPoints[1].controlPoint0.y - cornerPoints[2].controlPoint1.y) * (1.0f - (float)j/GRID_RESOLUTION)) + cornerPoints[2].controlPoint1.y, end_y, (float)i/GRID_RESOLUTION);

		        //the formula for Orthographic Projection
		        //x = cos(latitude) * sin(longitude-referenceLongitude);
		        //y = cos(referenceLatitude)*sin(latitude)-sin(referenceLatitude)*cos(latitude)*cos(longitude-referenceLongitude);
		        //http://mathworld.wolfram.com/OrthographicProjection.html
		        
		        float pi1 = (float) ((Math.PI)/GRID_RESOLUTION);
		        
		        float xfix = (float)(Math.cos((j-(GRID_RESOLUTION/2))*pi1)*Math.sin((i*pi1)-((float)(GRID_RESOLUTION/2)*pi1)))*horizontalForce;
		        float yfix = (float)(Math.cos((float)(GRID_RESOLUTION/2)*pi1)*Math.sin(j*pi1)-Math.sin((float)(GRID_RESOLUTION/2)*pi1)*Math.cos(j*pi1)*Math.cos((i*pi1)-((float)(GRID_RESOLUTION/2)*pi1)))*verticalForce;
		        
		        vertexPoints[i][j] = new PVector(x+xfix, y+yfix, 0);
			}
		}	
		
		poly = new Polygon();
		for(int w = 0; w < 4; w++){
			for(int i = 0; i < GRID_RESOLUTION; i++){
				switch(w){
				case 0:
					poly.addPoint((int)vertexPoints[i][0].x, (int)vertexPoints[i][0].y);
					break;
					
				case 1:
					poly.addPoint((int)vertexPoints[GRID_RESOLUTION][i].x, (int)vertexPoints[GRID_RESOLUTION][i].y);
					break;
					
				case 2:
					poly.addPoint((int)vertexPoints[GRID_RESOLUTION-i][GRID_RESOLUTION].x, (int)vertexPoints[GRID_RESOLUTION-i][GRID_RESOLUTION].y);
					break;
					
				case 3:
					poly.addPoint((int)vertexPoints[0][GRID_RESOLUTION-i].x, (int)vertexPoints[0][GRID_RESOLUTION-i].y);
					break;
				}
			}
		}
	}

	/**
	 * Translates a point on the screen into a point in the surface. (not implemented in Bezier Surfaces yet)
	 * @param x
	 * @param y
	 * @return
	 */
	public PVector screenCoordinatesToQuad(float x, float y) {
		//TODO :: maybe add this code
		return null;
	}

	/**
	 * Render method for rendering while in calibration mode
	 * @param g PGraphics to draw on
	 */
	public void render(PGraphics g) {
		if (this.MODE == this.MODE_CALIBRATE && !this.isHidden()) {
			this.renderGrid(g);
		}
	}

	/**
	 * Render method for rendering in RENDER mode. 
	 * Takes one PGraphics and one Texture. The Texture is the texture used for the surface, and is drawn to the offscreen buffer.
	 * @param g PGraphics to work on
	 * @param tex Texture to draw
	 */
	public void render(PGraphics g, PImage tex) {
		if(this.isHidden()) return;
		this.renderSurface(g, tex);
	}

	/**
	 * Actual rendering of the surface. Is called from the render method.
	 * Should normally not be accessed directly.
	 * @param g PGraphics to draw surface on
	 * @param tex Texture to apply to the surface
	 */
	private void renderSurface(PGraphics g, PImage tex) {
		g.beginDraw();
		//g.hint(PApplet.DISABLE_DEPTH_TEST); //this is probably needed, but could cause problems with surfaces adjacent to each other
		g.noStroke();
		
		for (int i = 0; i < GRID_RESOLUTION; i++) {
			for (int j = 0; j < GRID_RESOLUTION; j++) {
				
				g.beginShape();
				g.texture(tex);
				g.vertex(vertexPoints[i][j].x, 
						vertexPoints[i][j].y, 
						vertexPoints[i][j].z+currentZ,
						((float) i / GRID_RESOLUTION) * tex.width,
						((float) j / GRID_RESOLUTION) * tex.height);
				
				g.vertex(vertexPoints[i + 1][j].x, 
						vertexPoints[i + 1][j].y,
						vertexPoints[i + 1][j].z+currentZ, 
						(((float) i + 1) / GRID_RESOLUTION) * tex.width, 
						((float) j / GRID_RESOLUTION) * tex.height);
				
				g.vertex(vertexPoints[i + 1][j + 1].x, 
						vertexPoints[i + 1][j + 1].y,
						vertexPoints[i + 1][j + 1].z+currentZ, 
						(((float) i + 1) / GRID_RESOLUTION) * tex.width, 
						(((float) j + 1) / GRID_RESOLUTION) * tex.height);
				
				g.vertex(vertexPoints[i][j + 1].x, 
						vertexPoints[i][j + 1].y,
						vertexPoints[i][j + 1].z+currentZ, 
						((float) i / GRID_RESOLUTION) * tex.width,
						(((float) j + 1) / GRID_RESOLUTION) * tex.height);
				g.endShape();
				
			}
		}
		
		g.endDraw();
	}

	/**
	 * Renders the grid in the surface. (useful in calibration mode)
	 * @param g PGraphics to render to
	 */
	private void renderGrid(PGraphics g) {
		g.beginDraw();
		
		if (ccolor == 0) {
			g.fill(50, 80, 150);
		} else {
			g.fill(ccolor);
		}
		g.noStroke();
		for (int i = 0; i < GRID_RESOLUTION; i++) {
			for (int j = 0; j < GRID_RESOLUTION; j++) {
				
				g.beginShape();
				g.vertex(vertexPoints[i][j].x, vertexPoints[i][j].y);
				g.vertex(vertexPoints[i + 1][j].x, vertexPoints[i + 1][j].y);
				g.vertex(vertexPoints[i + 1][j + 1].x, vertexPoints[i + 1][j + 1].y);
				g.vertex(vertexPoints[i][j + 1].x, vertexPoints[i][j + 1].y);	
				g.endShape();
				
			}
		}
		
		g.textFont(sm.getIdFont());
		if (ccolor == 0) {
			g.fill(255);
		} else {
			g.fill(0);
		}

		g.textAlign(PApplet.CENTER, PApplet.CENTER);
		g.textSize(40);
		g.text("" + surfaceId, this.getCenter().x, this.getCenter().y);
		if (isLocked) {
			g.textSize(12);
			g.text("Surface locked", this.getCenter().x, this.getCenter().y+26);
		}
		

		g.noFill();
		g.stroke(BezierSurface.GRID_LINE_COLOR);
		g.strokeWeight(2);
		if (isSelected)
			g.stroke(BezierSurface.GRID_LINE_SELECTED_COLOR);

		if (!isLocked) {
			for(int i = 0; i <= GRID_RESOLUTION; i++){
				for(int j = 0; j <= GRID_RESOLUTION; j++){
					g.point(vertexPoints[i][j].x, vertexPoints[i][j].y, vertexPoints[i][j].z);
				}
			}
		}
		
		if (isSelected) {
			g.strokeWeight(4);
			g.stroke(BezierSurface.GRID_LINE_SELECTED_COLOR);
			
			//draw the outline here
			for(int i = 0; i < poly.npoints-1; i++){
				g.line(poly.xpoints[i], poly.ypoints[i], poly.xpoints[i+1], poly.ypoints[i+1]);
				if(i == poly.npoints-2) g.line(poly.xpoints[i+1], poly.ypoints[i+1], poly.xpoints[0], poly.ypoints[0]);
			}
		}
		
		g.strokeWeight(1);
		g.stroke(SELECTED_OUTLINE_INNER_COLOR);
		//draw the outline here
		for(int i = 0; i < poly.npoints-1; i++){
			g.line(poly.xpoints[i], poly.ypoints[i], poly.xpoints[i+1], poly.ypoints[i+1]);
			if(i == poly.npoints-2) g.line(poly.xpoints[i+1], poly.ypoints[i+1], poly.xpoints[0], poly.ypoints[0]);
		}

		
		
		if (!isLocked) {
			// Draw the control points.
			for (int i = 0; i < this.cornerPoints.length; i++) {
				this.renderCornerPoint(g, this.cornerPoints[i].x, this.cornerPoints[i].y, (this.activePoint == i), i);
				
			}
			
			for(int i = 0; i < this.bezierPoints.length; i++){
				this.renderBezierPoint(g, this.bezierPoints[i].x, this.bezierPoints[i].y, (this.selectedBezierControl == i), i);
				g.strokeWeight(1);
				g.stroke(255);
				g.line(this.bezierPoints[i].x, this.bezierPoints[i].y, this.cornerPoints[(i/2)].x, this.cornerPoints[(i/2)].y);
			}
			
		}

		g.endDraw();
	}

	/**
	 * Draws the Corner points
	 * @param g PGraphics to draw on
	 * @param x
	 * @param y
	 * @param selected
	 * @param cornerIndex
	 */
	private void renderCornerPoint(PGraphics g, float x, float y, boolean selected, int cornerIndex) {
		g.noFill();
		g.strokeWeight(2);
		if (selected) {
			g.stroke(BezierSurface.SELECTED_CORNER_MARKER_COLOR);
		} else {
			g.stroke(BezierSurface.CORNER_MARKER_COLOR);
		}
		if (cornerIndex == getSelectedCorner() && isSelected()) {
			g.fill(BezierSurface.SELECTED_CORNER_MARKER_COLOR, 100);
			g.stroke(BezierSurface.SELECTED_CORNER_MARKER_COLOR);
		}
		g.ellipse(x, y, 16, 16);
		g.line(x, y - 8, x, y + 8);
		g.line(x - 8, y, x + 8, y);
	}
	
	/**
	 * Draws the bezier points
	 * @param g
	 * @param x
	 * @param y
	 * @param selected
	 * @param cornerIndex
	 */
	private void renderBezierPoint(PGraphics g, float x, float y, boolean selected, int cornerIndex) {
		g.noFill();
		g.strokeWeight(1);
		if (selected) {
			g.stroke(BezierSurface.SELECTED_CORNER_MARKER_COLOR);
		} else {
			g.stroke(BezierSurface.CORNER_MARKER_COLOR);
		}
		if (cornerIndex == getSelectedBezierControl() && isSelected()) {
			g.fill(BezierSurface.SELECTED_CORNER_MARKER_COLOR, 100);
			g.stroke(BezierSurface.SELECTED_CORNER_MARKER_COLOR);
		}
		g.ellipse(x, y, 10, 10);
		g.line(x, y - 5, x, y + 5);
		g.line(x - 5, y, x + 5, y);
	}

}
