
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