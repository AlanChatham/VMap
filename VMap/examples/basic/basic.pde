
/***********************************************************
* Example Code provided by                                 *
* IXAGON AB + Laboratory                                   *
* This example shows you how to setup the library and      *
* and display single texture to multiple surfaces.         *
*                                                          *
* To add new surfaces, press 'a' to create Quad Surfaces   *
*                      press 'z' to create Bezier Surfaces *
*                                                          *
* To go between calibration and image mode, press 'c'      *
*                                                          *
* You can save mappings to a file by pressing 's',         *
* and load previously saved data by pressing 'l'           *
*                                                          *
* There are other options available in the keyPressed()    *
* function below, check them out!                          *
***********************************************************/

import VMap.*;
VMap vmap;

void setup(){
  // Create our window. VMap only works in P3D mode!
  size(800,600, P3D);
  
  //Create new instance of the VMap buffer - this is actually
  // an extension of the PGraphics class, which we'll be drawing to
  // when we create our VMap surfaces
  vmap = new VMap(this, width, height);
  
  //Creates one surface with subdivision 3, at center of screen
  vmap.addQuadSurface("img.jpg", width/2, height/2);
  vmap.addQuadSurface(100,100);
}

void draw(){
  // Draw the background black
  //background(0);

  vmap.beginDraw();
  vmap.fill(50,50,255, 50);
  vmap.rect(50,50,200,200);
  vmap.endDraw();
  // Now update the VMap buffer
  
  //Set a shader mode
//vmap.currentMainShader = vmap.projectiveShader;
  
  vmap.render();
  // Finally, draw the VMap buffer to the window
  image(vmap,0,0,width,height);
}

// Copy-paste the following block of code into your sketches
//  in order to use the standard keyboard commands
//  for manipulating VMap. We'd include it in the library,
//  but it wouldn't be clear that the keys were double-mapped,
//  making it opaque and harder to not run into conflicts
//  for people needing keyboard input. Feel free to change
//  the bindings in your programs, though.

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