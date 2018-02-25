import processing.video.*;

import VMap.*;

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

VMap vmap;
Movie movie;
PImage img;

void setup(){
  // Create our window. VMap only works in P3D mode!
  size(800,600, P3D);
  
  // Create new instance of VMap
  vmap = new VMap(this, width, height);
  // Creates one surface with subdivision 3, at center of screen
  vmap.createQuadSurface(3,width/2,height/2);
  
  // Load in an image
  img = loadImage("img.jpg");
  // And start a movie
  movie = new Movie(this, "streets.mp4");
  movie.loop();
}

void draw(){
  // Draw a black background
  background(0);
  
  // Loop through all the surfaces to map  
  for(SuperSurface ss : vmap.getSurfaces()){
    // Use movie as the texture for all even-numbered surfaces
    if(ss.getId() % 2 == 0){
      ss.setTexture(movie);
    }
    else{
      // And the static image for all the odd-numbered ones
      ss.setTexture(img);
    }
  }
  
  // Now update the VMap buffer
  vmap.render();
  // Finally, draw the VMap buffer to the window
  image(vmap,0,0,width,height);
}

// Update the movie buffer when it gets a new frame
void movieEvent(Movie movie) {
  movie.read();
}