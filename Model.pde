
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
} // END class Plane


//
