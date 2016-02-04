
/***********************************************************
* Example Code provided by 
* IXAGON AB + Laboratory                                   *
* This example shows you how to setup the library and      *
* and display single texture to multiple surfaces.         *
* Check the keyPressed method to see how to access         *
* different settings                                       *
***********************************************************/

import VMap.*;
VMap vmap;

void setup(){
  size(800,600, P3D);
  
  //Create new instance of the VMap buffer - this is actually
  // an extension of the PGraphics class, which we'll be drawing to
  // when we create our VMap surfaces
  vmap = new VMap(this, width, height);
  
  //Creates one surface with subdivision 3, at center of screen
  vmap.addQuadSurface("img.jpg", width/2, height/2);
  println("added surface");
}

void draw(){
  println("entered draw");
  // Draw the background black
  background(0);

  // Now update the VMap buffer
  vmap.render();
  // Finally, draw the VMap buffer to the window
  image(vmap,0,0,width,height);
}

void keyPressed(){
  //create a new QUAD surface at mouse pos
  if(key == 'a')
   vmap.addQuadSurface("img.jpg",mouseX,mouseY);
  //create new BEZIER surface at mouse pos
  if(key == 'z')
   vmap.addBezierSurface("img.jpg",mouseX,mouseY);
  //switch between calibration and render mode
  if(key == 'c')vmap.toggleCalibration();
  //increase subdivision of surface
  if(key == 'p'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      ss.increaseResolution();
    }
  }
  //decrease subdivision of surface
  if(key == 'o'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      ss.decreaseResolution();
    }
  }
  //save layout to xml
  if(key == 's')vmap.saveXML("bla.xml");
  //load layout from xml
  if(key == 'l')vmap.loadXML("bla.xml");
  //rotate how the texture is mapped in to the QUAD (clockwise)
  if(key == 'j'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      ss.rotateCornerPoints(0);
    }
  }
  //rotate how the texture is mapped in to the QUAD (counter clockwise)
  if(key == 'k'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      ss.rotateCornerPoints(1);
    }
  }
  //increase the horizontal force on a BEZIER surface
  if(key == 't'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      if (ss instanceof BezierSurface)
        ((BezierSurface)ss).increaseHorizontalForce();
    }
  }
  //decrease the horizontal force on a BEZIER surface  
  if(key == 'y'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      if (ss instanceof BezierSurface)
        ((BezierSurface)ss).decreaseHorizontalForce();
    }
  }
  //increase the vertical force on a BEZIER surface  
  if(key == 'g'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      if (ss instanceof BezierSurface)
        ((BezierSurface)ss).increaseVerticalForce();
    }
  }
  //decrease the vertical force on a BEZIER surface  
  if(key == 'h'){
    for(SuperSurface ss :vmap.getSelectedSurfaces()){
      if (ss instanceof BezierSurface)
        ((BezierSurface)ss).decreaseVerticalForce();
    }
  }
}
