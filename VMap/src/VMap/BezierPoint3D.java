package VMap;

public class BezierPoint3D extends Point3D{
	public Point3D controlPoint0;
	public Point3D controlPoint1;

	public BezierPoint3D(float x, float y){
		this.x = x;
		this.y = y;
		controlPoint0.x = x - 25;
		controlPoint0.y = y - 25;
		controlPoint1.x = x + 25;
		controlPoint1.y = y + 25;
	}
}
