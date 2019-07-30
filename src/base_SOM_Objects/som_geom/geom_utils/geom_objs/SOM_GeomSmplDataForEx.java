package base_SOM_Objects.som_geom.geom_utils.geom_objs;

import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;

/**
 * This object will hold a reference to a point and the SOM_GeomObj that the point comes from
 * A single SOM Example will be made up of 2 or more of these : 
 * 		2 for lines, 
 * 		3 for planes,		(triangle)
 * 		4 non-coplanar points for spheres (non-degenerate tet)
 * @author john
 */
public class SOM_GeomSmplDataForEx {
	public final int ID;
	private static int idGen = 0;
	/**
	 * point for this sample
	 */
	protected SOM_GeomSamplePointf pt;
	/**
	 * owning object for this sample
	 */
	protected SOM_GeomObj obj;
	
	public SOM_GeomSmplDataForEx(SOM_GeomObj _obj, SOM_GeomSamplePointf _pt) {
		ID = idGen++;
		obj = _obj;
		pt = _pt;
	}
	
	public SOM_GeomSamplePointf getPoint() {return pt;}
	public void setPoint(SOM_GeomSamplePointf _pt) {pt=_pt;}
	public SOM_GeomObj getObj() {return obj;}
	public void setObj(SOM_GeomObj _obj){obj=_obj;}
	public String toString() {
		String res=  "ID :"+ID+" | Src Obj ID : " + (obj==null? "None" : obj.getDispLabel())+" | Pt : " + pt.toStrBrf();
		return res;				
	}

}//class SOM_GeomSample
