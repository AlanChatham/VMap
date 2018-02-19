//package VMap;
//
//import com.jogamp.opengl.GL;
//
//import processing.core.PGraphics;
//import processing.opengl.PShader;
//
//final class OpenGLUtilities {
//
//	protected static void drawOpenGLGeometry(PGraphics drawBuffer, PShader shader){
//		// OpenGL rendering of our stuff
//		//  draws the actual surfaces
//		//  as OpenGL triangle-quads
//		
//		//Draw our test triangle
//		drawBuffer.beginDraw();
//		drawBuffer.beginPGL();
//		
//		
//		// Boo, we have to do a full draw routine for each
//		//  surface. Lame. Which means maybe we want
//		//  to move all the GL code to render?
//		// Bind shader
//		shader.bind();
//		
//		// bind our data - must happen every frame,
//		//  since Processing will throw out our data between frames?
//		setupOpenGLGeometry();
//		
//		setupGridTexture();
//
//		// draw our test triangle
//		int numVertices = this.quadVertices.size() / 9;
////		PApplet.println("number of vertices to draw: " +  numVertices);
//		gl.glDrawArrays(GL.GL_TRIANGLES, 0, numVertices);
//		
//		// test to make sure we're paying attention
//		drawBuffer.fill(255,200,255);
//		drawBuffer.ellipse(600, 600, 200, 200);
//		
//		shader.unbind();
//		
//		drawBuffer.endPGL();
//		drawBuffer.endDraw();
//		
//	}
//	
//}
