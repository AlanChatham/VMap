
/***********************************************************
* Example Code provided by                                 *
* IXAGON AB + Laboratory                                   *
* This example shows you how to give surfaces names        *
* to make selecting and using surfaces easier.             *
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

PImage img;
PImage nameImage;
VMap vmap;
Movie movie;

void setup(){
  // Create our window. VMap only works in P3D mode!
  size(800,600, P3D);
  
  // Create new instance of VMap
  vmap = new VMap(this, width, height);
  // Creates one surface with subdivision 3, at center of screen
  vmap.createQuadSurface(3,width/2,height/2);
  
  // Set the name of the surface
  //  To do this, we'll get the list of mapped surfaces,
  //  pick one, then give it a name
  ArrayList<SuperSurface> listOfSurfaces = vmap.getSurfaces();
  SuperSurface surfaceToName = listOfSurfaces.get(0);
  surfaceToName.setSurfaceName("name");
  
  // You can do all of that in one line this way:
  //  vmap.getSurfaces().get(0).setSurfaceName("name");
  
  // Load some images to use
  img = loadImage("img.jpg");
  nameImage = loadImage("name.jpg");
  
  // And start a movie playing
  movie = new Movie(this, "streets.mp4");
  movie.loop();
}

void draw(){
  background(0);
  
  // Loop through all the surfaces to map
  for(SuperSurface ss : vmap.getSurfaces()){
  
    // Use nameImage as texture if the surfaces name is 'name'
    if(ss.getSurfaceName().equals("name")){
      ss.setTexture(nameImage);
    }
    else{
      // use the movie as texture is the surface's id
      //  is an even number, use the image if it's odd.
      if(ss.getId() % 2 == 0)
        ss.setTexture(movie);
      else
        ss.setTexture(img);
    }
  }
  
  // Update the VMap buffer
  vmap.render();
  // Then draw it to the screen
  image(vmap,0,0,width,height);
}

// Update the movie's framebuffer when it gets a new frame
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
  if(key == 'a')vmap.createQuadSurface(3,mouseX,mouseY);
  //create new BEZIER surface at mouse pos
  if(key == 'z')vmap.createBezierSurface(3,mouseX,mouseY);
  //switch between calibration and render mode
  if(key == 'c')vmap.toggleCalibration();
  //increase subdivision of surface
  if(key == 'p'){
    for(SuperSurface ss : vmap.getSelectedSurfaces()){
      ss.increaseResolution();
    }
  }
  //decrease subdivision of surface
  if(key == 'o'){
    for(SuperSurface ss : vmap.getSelectedSurfaces()){
      ss.decreaseResolution();
    }
  }
  //save layout to xml
  if(key == 's')
    vmap.saveXML("positions.xml");
  //load layout from xml
  if(key == 'l')
    vmap.loadXML("positions.xml");
  //rotate how the texture is mapped in to the QUAD (clockwise)
  if(key == 'j'){
    for(SuperSurface ss : vmap.getSelectedSurfaces()){
      ss.rotateCornerPoints(0);
    }
  }
  //rotate how the texture is mapped in to the QUAD (counter clockwise)
  if(key == 'k'){
    for(SuperSurface ss : vmap.getSelectedSurfaces()){
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
    //shake all surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'q'){
    vmap.setShakeAll(50, 850, 20);
  }
    //shake all surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'w'){
    vmap.setShakeAll(75, 650, 130);
  }
    //shake all surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'e'){
    vmap.setShakeAll(100, 450, 300);
  }
    //shake only the selected surfaces with strength (max z displacement), speed, and duration (0 - 1000)
  if(key == 'r'){
    for(SuperSurface ss : vmap.getSelectedSurfaces()){
      ss.setShake(200,400, 50);
    }
  }
}
