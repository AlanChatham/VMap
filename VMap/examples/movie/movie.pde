

/***********************************************************
* Example Code provided by                                 *
* IXAGON AB + Laboratory                                   *
* This example shows you how to setup the library and      *
* and display a movie to multiple surfaces.                *
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

import processing.video.*;
import VMap.*;

VMap vmap;
Movie movie;

void setup(){
  // Create our window. VMap only works in P3D mode!
  size(800,600, P3D);
  
  //Create new instance of the VMap buffer - this is actually
  // an extension of the PGraphics class, which we'll be drawing to
  // when we create our VMap surfaces
  vmap = new VMap(this, width, height);

  //Creates one surface with subdivision 3, at center of screen
  vmap.addQuadSurface("img.jpg", width/2, height/2);

  
}

void draw(){
  // Draw the background black
  background(0);
  
  //Updates the shaking of the surfaces in render mode
  sm.shake();
  
  // Now update the VMap buffer
  vmap.render();
  // Finally, draw the VMap buffer to the window
  image(vmap,0,0,width,height);
}

void movieEvent(Movie movie) {
  movie.read();
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
    //shake all surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'q'){
    sm.setShakeAll(50, 850, 20);
  }
    //shake all surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'w'){
    sm.setShakeAll(75, 650, 130);
  }
    //shake all surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'e'){
    sm.setShakeAll(100, 450, 300);
  }
    //shake only the selected surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'r'){
    for(SuperSurface ss : sm.getSelectedSurfaces()){
      ss.setShake(200,400, 50);
    }
  }
}
