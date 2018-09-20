
//OSC communication with Wekinator:
import oscP5.*;
import netP5.*;
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

void setup() {
  size(1280, 720, P3D);
  smooth();

  // Define the font
  font = createFont("Airplane.ttf", fontSize);

  // Initialise the noise class
  simplexNoise = new OpenSimplexNoise();

  // Set the camera to be central, looking at the sphere with a wide draw distance.
  perspective(PI/3.0,(float)width/height,0.001,100000);

  // Setup OSC
  if (bUseOSC){
    // Listen for OSC messages on port 6449 (Where WekiInputHelper is sending)
    oscP5 = new OscP5(this,6449);
    // And send messages back to Wekinator on port 6448, localhost
    dest = new NetAddress("127.0.0.1",6448);
  }

  // Initialise values
  contortionAmountRadius = 0.0;
  contortionAmountScale = 0.0;
  headRot = 0.0;
  headTilt = 0.0;

  // Three OSC inputs
  String typeTag = "fff";

  // Initialise the instrument class
  instrument = new Instrument();

  // And run the setup()
  instrument.setup();

  // Initialise the motivator.
  initMotivator();
}

void draw() {
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

  if (contortionAmountScale > 0.1){
    startTime = timer;
    bNeedEncouraging = true;
    drawMotivator();
  }

  timer++;

  if (bDrawDebug) { drawDebug(); }
}

void drawInstrument(){
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
  shininess(5.0);
  ambient(255,255,255);
  pushMatrix();
  translate(width/2, height/2 + 10, 550);
  instrument.draw(this.g, meshIterator);
  popMatrix();
  meshIterator++;
}

void initMotivator(){

  hud = createGraphics(width,height, P2D);

  // Fill the array with positive phrases
  calmingWords = new String[4];
  calmingWords[0] = "relax.";
  calmingWords[1] = "everything is fine.";
  calmingWords[2] = "you have nothing to fear.";
  calmingWords[3] = "its probably time for bed.";
}

void drawMotivator(){

  // If the contortion amount is between the set threshold, choose a calming phrase
  if (contortionAmountScale >= 0.1){ word = calmingWords[0]; }
  if (contortionAmountScale >= 0.2){ word = calmingWords[1]; }
  if (contortionAmountScale >= 0.3){ word = calmingWords[2]; }
  if (contortionAmountScale >= 0.4){ word = calmingWords[3]; }

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

void drawDebug(){
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
void oscEvent(OscMessage theOscMessage) {

  // Listen for the three different OSC messages coming from WekiInputHelper
  if (theOscMessage.checkAddrPattern("/wek/outputsAvg")==true) {
     if(theOscMessage.checkTypetag("fff")) { // looking for 3 parameters

        // Assign the recieved values to variables
        float receivedContortion = theOscMessage.get(0).floatValue();
        float receivedRotation = theOscMessage.get(1).floatValue();
        float receivedTilt = theOscMessage.get(2).floatValue();

        // Map these values to the desired amount
        contortionAmountRadius = map(receivedContortion, 0, 1, 0.0, 0.5);
        contortionAmountScale = map(receivedContortion, 0, 1, 0.0, 0.5);
        headRot = map(receivedRotation, 0, 1, 1, -1);
        headTilt = map(receivedTilt, 0, 1, -1, 1);

     } else {
        println("Error: unexpected OSC message received by Processing: ");
        theOscMessage.print();
      }
 }
}

// This sends the current parameter to Wekinator
void sendOscNames() {
  OscMessage msg = new OscMessage("/wekinator/control/setOutputNames");
  //Now send all 3 names
  msg.add("contortion");
  msg.add("rot");
  msg.add("tilt");
  oscP5.send(msg, dest);
}
