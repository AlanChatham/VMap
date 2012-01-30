package ixagon.SurfaceMapper;

public class Point3D {
	public float x = 0;
	public float y = 0;
	public float z = 0;

	public float u = 0;
	public float v = 0;
	
	public Point3D(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Point3D(float x, float y){
		this.x = x;
		this.y = y;
		this.z = 0;
	}
	
	public Point3D(){
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	void copyPoint(Point3D other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.v = other.v;
		this.u = other.u;
	}
}