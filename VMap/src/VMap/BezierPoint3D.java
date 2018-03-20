package VMap;

import processing.core.PVector;

public class BezierPoint3D extends PVector{
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * First control point, relative to the 
	 */
	public PVector controlPoint0;
	public PVector controlPoint1;
	
	public BezierPoint3D(){
		this.x = 0;
		this.y = 0;
		this.z = 0;
		// Put some default values for these. We assume these get changed by the program
		controlPoint0 = new PVector(-25, -25, 0);
	    controlPoint1 = new PVector(25, 25, 0);
	}

	public BezierPoint3D(float x, float y){
		this.x = x;
		this.y = y;
		// Put some default values for these. We assume these get changed by the program
		controlPoint0 = new PVector(x - 25, y - 25, 0);
	    controlPoint1 = new PVector(x + 25, y + 25, 0);
	}
	
	public void addControl(float x, float y){ // formerly known as add, maybe should put back, and set output to PVector
		PVector additional = new PVector(x,y);
		this.add(additional);
		this.controlPoint0.add(additional);
		this.controlPoint1.add(additional);
	}

	public void rotateControl(float theta){// formerly known as rotate, maybe should put back, and set output to PVector
		PVector root = new PVector(this.x,this.y);
		controlPoint0.sub(root);
		controlPoint0.rotate(theta);
		controlPoint0.add(root);
		controlPoint1.sub(root);
		controlPoint1.rotate(theta);
		controlPoint1.add(root);
	}
}
