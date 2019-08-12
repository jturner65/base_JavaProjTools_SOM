package base_SOM_Objects.som_geom.geom_utils.geom_objs;

import java.util.HashMap;
import java.util.Map;

/**
 * holds types of geometric objects used for som training.  Value is minimum # of sample points required to uniquely define object
 * @author john
 */
public enum SOM_GeomObjTypes {
	point(0), line_2D(1), line_3D(2), plane(3), sphere(4);
	private int value; 
	private static String[] _typeName = new String[] {"Point","2D_Line","3D_Line","Plane","Sphere"};
	private static Map<Integer, SOM_GeomObjTypes> map = new HashMap<Integer, SOM_GeomObjTypes>(); 
	static { for (SOM_GeomObjTypes enumV : SOM_GeomObjTypes.values()) { map.put(enumV.value, enumV);}}
	private SOM_GeomObjTypes(int _val){value = _val;} 
	public int getVal(){return value;}
	public static SOM_GeomObjTypes getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	public String getName() {return _typeName[value];}
	@Override
    public String toString() { return Character.toString(_typeName[value].charAt(0)).toUpperCase() + _typeName[value].substring(1).toLowerCase(); }	

}//SOM_GeomObjTypes
