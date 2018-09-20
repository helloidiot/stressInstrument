import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import oscP5.*; 
import netP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MachineLearning_StressInstrument_04 extends PApplet {


//OSC communication with Wekinator:


OscP5 oscP5;
NetAddress dest;
boolean bUseOSC = true;

// Noise
OpenSimplexNoise simplexNoise;

// Motivator
String calmingWords[];
String word;
boolean bNeedEncouraging;
int startTime;
int displayDuration = 30;

PFont font;
int fontSize = 70;
PGraphics hud;
PGraphics buffer;

int meshIterator = 0;
int timer = 0;
float alpha = 0;

float contortionAmountRadius, contortionAmountScale, headRot, headTilt;

Instrument instrument;

boolean bDrawDebug = false;

public void setup() {
  
  

  // Define the font
  font = createFont("Airplane.ttf", fontSize);

  // Initialise the noise class
  simplexNoise = new OpenSimplexNoise();

  // Set the camera to be central, looking at the sphere with a wide draw distance.
  perspective(PI/3.0f,(float)width/height,0.001f,100000);

  // Setup OSC
  if (bUseOSC){
    // Listen for OSC messages on port 6449 (Where WekiInputHelper is sending)
    oscP5 = new OscP5(this,6449);
    // And send messages back to Wekinator on port 6448, localhost
    dest = new NetAddress("127.0.0.1",6448);
  }

  // Initialise values
  contortionAmountRadius = 0.0f;
  contortionAmountScale = 0.0f;
  headRot = 0.0f;
  headTilt = 0.0f;

  // Three OSC inputs
  String typeTag = "fff";

  // Initialise the instrument class
  instrument = new Instrument();

  // And run the setup()
  instrument.setup();

  // Initialise the motivator.
  initMotivator();
}

public void draw() {
  colorMode(RGB);
  // Blue background
  background(145, 209, 218);

  // Draw our instrument
  drawInstrument();

  // If the user needs encouragement, draw the motivator
  if (bNeedEncouraging) {

    if (timer - startTime > displayDuration){
      bNeedEncouraging = false;
    }
  }

  if (contortionAmountScale > 0.1f){
    startTime = timer;
    bNeedEncouraging = true;
    drawMotivator();
  }

  timer++;

  if (bDrawDebug) { drawDebug(); }
}

public void drawInstrument(){
  noStroke();
  fill(255);

  lightSpecular(128,128,128);
  directionalLight(255,255,255, 1, 1, -1);
  directionalLight(255,255,255, -1, -1, -1);

  // Update the mesh
  instrument.update(meshIterator);

  // Draw the mesh
  fill(224, 123, 141);
  specular(128,128,128);
  shininess(5.0f);
  ambient(255,255,255);
  pushMatrix();
  translate(width/2, height/2 + 10, 550);
  instrument.draw(this.g, meshIterator);
  popMatrix();
  meshIterator++;
}

public void initMotivator(){

  hud = createGraphics(width,height, P2D);

  // Fill the array with positive phrases
  calmingWords = new String[4];
  calmingWords[0] = "relax.";
  calmingWords[1] = "everything is fine.";
  calmingWords[2] = "you have nothing to fear.";
  calmingWords[3] = "its probably time for bed.";
}

public void drawMotivator(){

  // If the contortion amount is between the set threshold, choose a calming phrase
  if (contortionAmountScale >= 0.1f){ word = calmingWords[0]; }
  if (contortionAmountScale >= 0.2f){ word = calmingWords[1]; }
  if (contortionAmountScale >= 0.3f){ word = calmingWords[2]; }
  if (contortionAmountScale >= 0.4f){ word = calmingWords[3]; }

  // Assign the camera for the hud
  camera();
  noLights();

  // And draw the hud to a PGraphics buffer
  hud.beginDraw();
  hud.fill(255);
  hud.background(145, 209, 218, 0);
  hud.textAlign(CENTER,CENTER);
  hud.textFont(font, fontSize);
  hud.text(word, 0, -200 , width, height);
  hud.endDraw();

  // Finally, draw the buffer to the screen
  hint(DISABLE_DEPTH_TEST);
  image(hud, 0, 0);
  hint(ENABLE_DEPTH_TEST);

}

public void drawDebug(){
  // Simple console debug
  println("===");
  println("need encouragement? " + bNeedEncouraging);
  println("timer: " + timer);
  println("startTime: " + startTime);
  println("timer - starttime: " + (timer - startTime));
  println("scale: " + contortionAmountScale);
  println("alpha: " + alpha);
}

////////////////////////
//    OSC EVENTS
////////////////////////

//This is called automatically when OSC message is received
public void oscEvent(OscMessage theOscMessage) {

  // Listen for the three different OSC messages coming from WekiInputHelper
  if (theOscMessage.checkAddrPattern("/wek/outputsAvg")==true) {
     if(theOscMessage.checkTypetag("fff")) { // looking for 3 parameters

        // Assign the recieved values to variables
        float receivedContortion = theOscMessage.get(0).floatValue();
        float receivedRotation = theOscMessage.get(1).floatValue();
        float receivedTilt = theOscMessage.get(2).floatValue();

        // Map these values to the desired amount
        contortionAmountRadius = map(receivedContortion, 0, 1, 0.0f, 0.5f);
        contortionAmountScale = map(receivedContortion, 0, 1, 0.0f, 0.5f);
        headRot = map(receivedRotation, 0, 1, 1, -1);
        headTilt = map(receivedTilt, 0, 1, -1, 1);

     } else {
        println("Error: unexpected OSC message received by Processing: ");
        theOscMessage.print();
      }
 }
}

// This sends the current parameter to Wekinator
public void sendOscNames() {
  OscMessage msg = new OscMessage("/wekinator/control/setOutputNames");
  //Now send all 3 names
  msg.add("contortion");
  msg.add("rot");
  msg.add("tilt");
  oscP5.send(msg, dest);
}

public class Instrument{

  ////////////////////////
  // PROPERTIES
  //////////////////////

  ArrayList<Vert> vertices;
  PVector position;
  PVector rotation;
  PVector scale;
  PShape importedModel;
  int meshIterator = 0;

  ////////////////////////
  // CONSTRUCTOR
  //////////////////////
  Instrument(){
    // set initial transformtaions
    position = new PVector(0,0,0);
    rotation = new PVector(0,0,0);
    scale = new PVector(1.f,1.f,1.f);
    vertices = new ArrayList<Vert>();
    setup();
  }

  ////////////////////////
  // METHODS
  //////////////////////

  public void setup(){
    // create/empty the arrayList
    importedModel = loadShape("sphere6.obj");

    vertices = new ArrayList<Vert>();

    // Get all vertices of model
    getVertices(importedModel, vertices);
  }

  public void update(int it) {
    // update all vertices
    for(int i = 0; i < vertices.size(); i++){
      vertices.get(i).update(it);
      rotation.y = headRot;
      rotation.x = headTilt;
    }
    it++;
  }

  public void draw(PGraphics pg, int it){

    pushTransform(pg);
    pg.beginShape(QUADS);
    for (int i = 0; i < vertices.size(); i++){
      PVector p = vertices.get(i).position;
      pg.vertex(p.x, p.y, p.z);
    }
    pg.endShape();
    popTransform(pg);


  }

  public void getVertices(PShape shape, ArrayList<Vert> verts){
    // for each face in current mesh
    for (int i = 0; i < shape.getChildCount(); i++){

      PShape child = shape.getChild(i);
      int numChildren = child.getChildCount();

      //if has nested elements, recurse
      if (numChildren > 0){
        for (int j = 0; j < numChildren; j++){
          getVertices(child.getChild(j), verts);
        }
      }
      // otherwise, append the child's vertices
      else{
        // get each vertex and append it
        for (int k = 0; k < child.getVertexCount(); k++){
          pushVert(child.getVertex(k));
        }
      }
    }
  }

  ////////////////////////////////////////
  // HELPERS / CONVENIENCE / EQUATIONS
  //////////////////////////////////////

  public void pushTransform(PGraphics pg){
    // perform all transformations
    pg.pushMatrix();
    pg.translate(position.x, position.y, position.z);
    pg.rotateX(rotation.x);
    pg.rotateY(rotation.y);
    pg.rotateZ(rotation.z);
    pg.scale(scale.x, scale.y, scale.z);
  }

  // pop out of all transformations
  public void popTransform(PGraphics pg){
      pg.popMatrix();
  }

  // Push vertices to arraylist
  // Version A - vector
  public void pushVert(PVector v){
    vertices.add(new Vert(v.x,v.y,v.z));
  }
  // Version B - 3 floats
  public void pushVert(float x, float y, float z){
    vertices.add(new Vert(x,y,z));
  }

} // END class Instrument











//

public class Model extends Instrument{  // Plane is a type of Artifact

  // PROPERTIES
  PShape importedModel;

  // CONSTRUCTOR
  Model(){
    // Calls the contructor of the super class
    super();
  }

  public void setup(){
    importedModel = loadShape("sphere6.obj");

    vertices = new ArrayList<Vert>();

    // Get all vertices of model
    getVertices(importedModel, vertices);
  }

  public void draw(PGraphics pg, int it){

    pushTransform(pg);
    pg.beginShape(QUADS);
    for (int i = 0; i < vertices.size(); i++){
      PVector p = vertices.get(i).position;
      pg.vertex(p.x, p.y, p.z);
    }
    pg.endShape();
    popTransform(pg);

  }

  public void getVertices(PShape shape, ArrayList<Vert> verts){
    // for each face in current mesh
    for (int i = 0; i < shape.getChildCount(); i++){

      PShape child = shape.getChild(i);
      int numChildren = child.getChildCount();

      //if has nested elements, recurse
      if (numChildren > 0){
        for (int j = 0; j < numChildren; j++){
          getVertices(child.getChild(j), verts);
        }
      }
      // otherwise, append the child's vertices
      else{
        // get each vertex and append it
        for (int k = 0; k < child.getVertexCount(); k++){
          pushVert(child.getVertex(k));
        }
      }
    }
  }
} // END class Plane


//

class Vert{

  PVector initPosition;
  PVector position;

  // Constructor
  Vert(float x, float y, float z){
    initPosition  = new PVector(x, y, z);
    position = initPosition.copy();
  }

  public void update(int i){

    // modulate the current position by adding to initPos
    position = PVector.add(initPosition, contortion(i) );

  }

public PVector contortion(int i){
  PVector res = new PVector(0,0,0);
  float t = 1.0f * i / 24;

  float radius = contortionAmountRadius;
  float scale = contortionAmountScale;

  if (!bUseOSC){
    contortionAmountRadius = 0.2f;
    contortionAmountScale = 0.15f;
  }

  float ns = (float)simplexNoise.eval(scale * position.x,scale * position.y, radius * sin(TWO_PI * t), radius * cos(TWO_PI * t));

  float zPos = map(ns, -1, 1, -5, 5);
  float xPos = map(ns, -1, 1, -2, 2);
  float yPos = map(ns, -1, 1, 2, -2);

  res.z = zPos;
  res.x = xPos;
  res.y = yPos;
  i++;
  return res;

}


} // class Vert












//
  public void settings() {  size(1280, 720, P3D);  smooth(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MachineLearning_StressInstrument_04" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
