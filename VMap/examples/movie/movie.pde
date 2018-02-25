

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

  //Set up a movie
  movie = new Movie(this, "streets.mp4");
  movie.loop();
  
  //Creates one surface with subdivision 3, at center of screen
  vmap.addQuadSurface(width/2, height/2);

  
}

void draw(){
  // Draw the background black
  background(0);
  
  // Loop through all the surfaces to map  
  for(SuperSurface ss : vmap.getSurfaces()){
    // Use movie as the texture for all surfaces
    ss.setTexture(movie);
  }
  
  // Now update the VMap buffer
  vmap.render();
  // Finally, draw the VMap buffer to the window
  image(vmap,0,0,width,height);
}

void movieEvent(Movie movie) {
  movie.read();
}