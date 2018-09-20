
class Vert{

  PVector initPosition;
  PVector position;

  // Constructor
  Vert(float x, float y, float z){
    initPosition  = new PVector(x, y, z);
    position = initPosition.copy();
  }

  void update(int i){

    // modulate the current position by adding to initPos
    position = PVector.add(initPosition, contortion(i) );

  }

PVector contortion(int i){
  PVector res = new PVector(0,0,0);
  float t = 1.0 * i / 24;

  float radius = contortionAmountRadius;
  float scale = contortionAmountScale;

  if (!bUseOSC){
    contortionAmountRadius = 0.2;
    contortionAmountScale = 0.15;
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
