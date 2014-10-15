import ixagon.SurfaceMapperP2.*;

/***********************************************************
* EXAMPLE PROVIDED WITH SURFACEMAPPER LIBRARY DEVELOPED BY *
* IXAGON AB.                                               *
* This example shows you how to setup the library and      *
* and display single texture to multiple surfaces.         *
* Check the keyPressed method to see how to access         *
* different settings                                       *
***********************************************************/

PImage tex;
PGraphics glos;
SurfaceMapper sm;

void setup(){
  size(800,600, P3D);
  glos = createGraphics(width, height, P3D);
  tex = loadImage("img.jpg");
  
  //Create new instance of SurfaceMapper
  sm = new SurfaceMapper(this, width, height);
  //Creates one surface with subdivision 3, at center of screen
  sm.createQuadSurface(3,width/2,height/2);
}

void draw(){
  background(0);
  glos.beginDraw();
  glos.clear();
  glos.endDraw();
  //Updates the shaking of the surfaces in render mode
  sm.shake();
  //render all surfaces in calibration mode
  if(sm.getMode() == sm.MODE_CALIBRATE)sm.render(glos);
  //render all surfaces in render mode
  if(sm.getMode() == sm.MODE_RENDER){
    for(SuperSurface ss : sm.getSurfaces()){
      //render this surface to GLOS, use TEX as texture
      ss.render(glos,tex);
    }
  }
  //display the GLOS to screen
  image(glos,0,0,width,height);
}

void keyPressed(){
  //create a new QUAD surface at mouse pos
  if(key == 'a')sm.createQuadSurface(3,mouseX,mouseY);
  //create new BEZIER surface at mouse pos
  if(key == 'z')sm.createBezierSurface(3,mouseX,mouseY);
  //switch between calibration and render mode
  if(key == 'c')sm.toggleCalibration();
  //increase subdivision of surface
  if(key == 'p'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.increaseResolution();
    }
  }
  //decrease subdivision of surface
  if(key == 'o'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.decreaseResolution();
    }
  }
  //save layout to xml
  if(key == 's')sm.save("bla.xml");
  //load layout from xml
  if(key == 'l')sm.load("bla.xml");
  //rotate how the texture is mapped in to the QUAD (clockwise)
  if(key == 'j'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.rotateCornerPoints(0);
    }
  }
  //rotate how the texture is mapped in to the QUAD (counter clockwise)
  if(key == 'k'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.rotateCornerPoints(1);
    }
  }
  //increase the horizontal force on a BEZIER surface
  if(key == 't'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.increaseHorizontalForce();
    }
  }
  //decrease the horizontal force on a BEZIER surface  
  if(key == 'y'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.decreaseHorizontalForce();
    }
  }
  //increase the vertical force on a BEZIER surface  
  if(key == 'g'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.increaseVerticalForce();
    }
  }
  //decrease the vertical force on a BEZIER surface  
  if(key == 'h'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.decreaseVerticalForce();
    }
  }
}
