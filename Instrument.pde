
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
    scale = new PVector(1.,1.,1.);
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

  void getVertices(PShape shape, ArrayList<Vert> verts){
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
