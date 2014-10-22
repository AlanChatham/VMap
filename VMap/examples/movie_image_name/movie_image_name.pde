import processing.video.*;
import VMap.*;

/***********************************************************
* EXAMPLE PROVIDED WITH SURFACEMAPPER LIBRARY DEVELOPED BY *
* IXAGON AB.                                               *
* This example shows you how to setup the library and      *
* and display a movie to multiple surfaces.                *
* Check the keyPressed method to see how to access         *
* different settings                                       *
***********************************************************/

PImage img;
PImage name;
VMap vmap;
Movie movie;

void setup(){
  size(800,600, P3D);
  
  img = loadImage("img.jpg");
  name = loadImage("name.jpg");
  //Create new instance of SurfaceMapper
  vmap = new VMap(this, width, height);
  //Creates one surface with subdivision 3, at center of screen
  vmap.createQuadSurface(3,width/2,height/2);
  //set the name of the surface
  vmap.getSurfaces().get(0).setSurfaceName("name");
  movie = new Movie(this, "streets.mp4");
  movie.loop();
}

void draw(){
  background(0);
  
 for(SuperSurface ss : vmap.getSurfaces()){
    //render this surface to GLOS, use name as texture if the surfaces name is 'name'
    if(ss.getSurfaceName().equals("name")){
      ss.setTexture(name);
    }else{
      //use the movie as texture is the surfaces id is an even number, use the image if it's odd.
      if(ss.getId() % 2 == 0)
        ss.setTexture(movie);
      else ss.setTexture(img);
    }
  }
  
  vmap.render();
  image(vmap,0,0,width,height);
}

void movieEvent(Movie movie) {
  movie.read();
}

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
  if(key == 's')vmap.saveXML("bla.xml");
  //load layout from xml
  if(key == 'l')vmap.loadXML("bla.xml");
  //rotate how the texture is mapped in the QUAD (clockwise)
  if(key == 'j'){
    for(SuperSurface ss : vmap.getSelectedSurfaces()){
      ss.rotateCornerPoints(0);
    }
  }
  //rotate how the texture is mapped in the QUAD (counter clockwise)
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
