package base_SOM_Objects.som_geom.geom_utils.geom_objs;

import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;

/**
 * this struct just expands on a myPointf object to add a unique ID and some owning object references
 * 
 * @author john
 *
 */
public class SOM_GeomSamplePointf extends myPointf {
    /**
     * unique id for this point
     */
    public final long ID;
    protected static long idIncr = 0;
    /**
     * owning object for this sample
     */
    protected SOM_GeomObj obj;
    
    /**
     * display label for this point
     */
    public final String name;
    
    /**
     * build a Geom_SamplePoint from given coordinates
     * @param _x : x coord
     * @param _y : y coord
     * @param _z : z coord
     */
    public SOM_GeomSamplePointf(float _x, float _y, float _z, String _name, SOM_GeomObj _obj){super(_x,_y,_z);name=_name; ID = idIncr++; obj=_obj;}                                
    /**
     * build a Geom_SamplePoint from given coordinates(as doubles)
     * @param _x : x coord
     * @param _y : y coord
     * @param _z : z coord
     */
    public SOM_GeomSamplePointf(double _x, double _y, double _z, String _name, SOM_GeomObj _obj){super((float)_x,(float)_y,(float)_z); name=_name;ID = idIncr++; obj=_obj;}          
    /**
     * build a Geom_SamplePoint from given point
     * @param p : point object to build Geom_SamplePoint from
     */
    public SOM_GeomSamplePointf(myPointf p, String _name, SOM_GeomObj _obj){ super(p.x, p.y, p.z); name=_name; ID = idIncr++; obj=_obj;}                                                        
    /**
     * copy constructor
     * @param p : Geom_SamplePoint object to copy, with same ID
     */
    public SOM_GeomSamplePointf(SOM_GeomSamplePointf p, String _name){ super(p.x, p.y, p.z); name=_name; ID = p.ID; obj=p.obj;}                                                 
    /**
     * build Geom_SamplePoint as displacement from point A by vector B
     * @param A : starting point
     * @param B : displacement vector
     */
    public SOM_GeomSamplePointf(myPointf A, myVectorf B, String _name, SOM_GeomObj _obj) {super(A.x+B.x,A.y+B.y,A.z+B.z);  name=_name;ID = idIncr++; obj=_obj;} 
    /**
     * Interpolate between A and B by s -> (0->1)
     * @param A : first point to interpolate from
     * @param s : value [0,1] to determine linear interpolation
     * @param B : second point to interpolate from
     */
    public SOM_GeomSamplePointf(myPointf A, float s, myPointf B, String _name, SOM_GeomObj _obj) {super(A, s, B);  name=_name;ID = idIncr++; obj=_obj;} 
    /**
     * constructor from csv string
     * @param _csvStrAra : array of string data in format of toCSVStr output :
     *     idx 0,1,2 : x,y,z
     */
    public SOM_GeomSamplePointf(String[] _csvStrAra, SOM_GeomObj _obj) {
        super(Float.parseFloat(_csvStrAra[0].trim()),Float.parseFloat(_csvStrAra[1].trim()),Float.parseFloat(_csvStrAra[2].trim()) );
        ID = Integer.parseInt(_csvStrAra[3].trim());
        idIncr = ID +1;
        name = _csvStrAra[4].trim();
         obj=_obj;
    }
    
    public SOM_GeomObj getObj() {return obj;}
    public void setObj(SOM_GeomObj _obj){obj=_obj;}

    
    /**
     * empty constructor
     */
    public SOM_GeomSamplePointf(String _name){ super(0,0,0);name=_name;ID = idIncr++;}       
    
    public final String toCSVHeaderStr() {return "x,y,z,ID,Name,";}
    
    public final String toCSVStr() {
        String res = super.toStrCSV("%.8f") +","+ID+","+name+",";
        return res;
    }
    
}//Geom_SamplePoint
