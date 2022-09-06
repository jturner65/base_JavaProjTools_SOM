package base_SOM_Objects.som_geom.geom_examples;

import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_SOM_Objects.som_examples.SOM_ExDataType;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjTypes;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomSamplePointf;

public abstract class SOM_GeomLineObj extends SOM_GeomObj {
	/**
	 * # of source points used to build object
	 */
	public static final int _numSrcPts = 2;
	/**
	 * direction vector for this line
	 */
	public myVectorf dir;
	/**
	 * normal to line
	 */
	public myVectorf norm;
	/**
	 * point closest to spatial origin for this line
	 */
	public myPointf origin;
	/**
	 * display points for this line to draw maximally based on world bounds
	 */
	public myPointf[] dispEndPts;

	
	/**
	 * an object to restrict the bounds on this line - min,max, diff s,t value within which to sample plane
	 */
	protected float[][] worldTBounds;
	
	protected String[] dispAra;

	/**
	 * Constructor for line object
	 * @param _mapMgr owning som map manager
 	 * @param _a, _b : 2 points on line
 	 * @param _numSmplPts : # of points to build
	 * @param _locClrAra color based on location
	 * @param _worldBounds 2d array of bounds for where reasonable points should be generated
	 * 		first idx 	: 0 is min; 1 is diff
	 * 		2nd idx 	: 0 is x, 1 is y
	 */
	
	public SOM_GeomLineObj(SOM_GeomMapManager _mapMgr, SOM_ExDataType _exType, String _id,
			SOM_GeomSamplePointf[] _srcSmpls, SOM_GeomObjTypes _GeoType, int _numSmplPts, boolean _is3D,
			boolean _shouldBuildSamples) {
		super(_mapMgr, _exType, _id, _srcSmpls, _GeoType, _is3D, _shouldBuildSamples);
		buildDirOriginAndDispPts("");		
		super.buildLocClrInitObjAndSamples(origin, _numSmplPts);
			
		boundOriginWithinLine();

	}//ctor

	public SOM_GeomLineObj(SOM_GeomMapManager _mapMgr, SOM_ExDataType _exType, String _oid, String _csvDat,
			SOM_GeomObjTypes _GeoType, boolean _is3D) {
		super(_mapMgr, _exType, _oid, _csvDat, _GeoType, _numSrcPts, _is3D);
		buildDirOriginAndDispPts("");		
		super.buildLocClrAndSamplesFromCSVStr(origin, _csvDat);
	}//csv ctor

	/**
	 * ctor to build object corresponding to bmu geometric object
	 * @param _mapMgr
	 * @param _mapNode
	 */
	public SOM_GeomLineObj(SOM_GeomMapManager _mapMgr, SOM_GeomMapNode _mapNode, SOM_GeomObjTypes _GeoType,
			boolean _is3D) {
		super(_mapMgr, _mapNode, _GeoType, _is3D);
		buildDirOriginAndDispPts("BMU");
		super.buildLocClrInitObjAndSamples(origin, _numSrcPts);
		
		boundOriginWithinLine();
	}//bmu ctor

	public SOM_GeomLineObj(SOM_GeomLineObj _otr) {
		super(_otr);
		dir = _otr.dir;
		origin = _otr.origin;
		dispEndPts = _otr.dispEndPts;		
	}//copy ctor
	
	/**
	 * build object-specific values for this object
	 * @param endPtPrefix whether the display string should include "bmu" or not
	 */
	protected final void buildDirOriginAndDispPts(String endPtPrefix) {
		if (!getGeomFlag(is3dIDX)) {
			//z is always 0 - making this in 2 d
			getSrcPts()[0].z = 0.0f;
			getSrcPts()[1].z = 0.0f;
		}

		dir = _buildDir();	
		//build normal and possibly binormal to line
		buildNorm();
		//origin is closest point to 0,0 on line
		origin = findClosestPointOnLine(myPointf.ZEROPT);
		if (!getGeomFlag(is3dIDX)) {
			//z is always 0 - making this in 2 d
			origin.z = 0;
		}
		
		//build bounds on s and t, if appropriate - by here equations define objects should be built
		worldTBounds = calcTBounds();
		dispEndPts = new myPointf[2];
		dispAra = new String[2];
		dispEndPts[0] = getPointOnLine(worldTBounds[0][0]);
		dispEndPts[1] = getPointOnLine(worldTBounds[0][0] + worldTBounds[1][0]);
		
		dispAra[0] = endPtPrefix + "End pt 0 w/min t : " + worldTBounds[0][0] + " | "+dispEndPts[0].toStrBrf();
		dispAra[1] = endPtPrefix + "End pt 1 w/max t : " + (worldTBounds[0][0]+worldTBounds[1][0])+ " | "+dispEndPts[1].toStrBrf();			
	}
	
	/**
	 * build the normal and possibly binormal to line
	 * @return
	 */
	protected abstract void buildNorm();
	
	
	/**
	 * Build line's direction vector
	 * @return
	 */	
	protected abstract myVectorf _buildDir();
	
	/**
	 * make sure that point chosen as closest to origin is bounded within the allowable bounds of the line - if not use one of the extremal points
	 */
	protected void boundOriginWithinLine() {
		float low_t = worldTBounds[0][0], hi_t = worldTBounds[0][0] + worldTBounds[1][0];
		float ctr_t = getTForPointOnLine(origin);
		if(ctr_t < low_t) {			origin.set(dispEndPts[0]);}
		else if (ctr_t > hi_t) {	origin.set(dispEndPts[1]);}	
	}
	
	
	/**
	 * calculate the bounds on s and t (if appropriate) for parametric formulation of object equation
	 * worldBounds is 
	 * 		first idx 	: 0 is min; 1 is diff
	 * 		2nd idx 	: 0 is x, 1 is y, 2 is z (if present)
	 * result is
	 * 		first idx 	: 0==min, 1==diff
	 * 		2nd idx 	: 0==t (only 1 value)
	 * @return result array
	 */
	protected final float[][] calcTBounds(){
		//eq  pt = pta + t * dir -> t = (pt-pta)/dir for each dof
		//mins has location for each dof
		float[] mins =  ((SOM_GeomMapManager) mapMgr).getWorldBounds()[0];		
		float[][] res = new float[2][mins.length-1];
		for(int i=0;i<mins.length-1;++i) {
			res[0][i]=100000000.0f;			
		}
		//for every bound, set t value in bound's ortho dim
		TreeMap<Float, Integer> tmpBnds = buildWorldBounds(mins);
		
		float a = tmpBnds.firstKey();
		float b = tmpBnds.lastKey();
		if(a < b) {
			res[0][0] = a;
			res[1][0] = b-a;			
		} else {			
			res[0][0] = b;
			res[1][0] = a-b;
		}	
		return res;
	}//calcTBounds()
	
	/**
	 * For every bound in line's space, set t value in bound's ortho dim
	 * @return
	 */
	protected abstract TreeMap<Float, Integer> buildWorldBounds(float[] mins);	
	
	/**
	 * return a random point on this object
	 */
	@Override
	public final myPointf getRandPointOnObj() {
		float t = ((float) ThreadLocalRandom.current().nextFloat() *worldTBounds[1][0])+worldTBounds[0][0];
		return getPointOnLine(t);
	}

	public final myPointf findClosestPointOnLine(myPointf p) {
		//find projection t of vector ap (from a to p) on dir, then find a + t * dir
		myVectorf proj = new myVectorf(getSrcPts()[0],p);
		return myVectorf._add(getSrcPts()[0], proj._dot(dir), dir);
	}
	
	
	/**
	 * return a point on the line 
	 * @param t
	 * @return
	 */
	public final myPointf getPointOnLine(float t) {return myVectorf._add(getSrcPts()[0], t, dir);}
	
	
	/**
	 * find t value for point on line - expects point to be on line!
	 * @param pt
	 * @return
	 */
	public abstract float getTForPointOnLine(myPointf pt);
	/**
	 * Instance-class specific column names of rawDescrForCSV data
	 * @return
	 */
	@Override
	protected final String getRawDescColNamesForCSV_Indiv() {	return "";}
	/**
	 * Instance-class specific required info for this example to build feature data - use this so we don't have to reload and rebuilt from data every time
	 * @return
	 */
	@Override
	protected final String getPreProcDescrForCSV_Indiv() {return "";}	
	
	@Override
	public final TreeMap<Integer, Integer> getTrainingLabels() {
		TreeMap<Integer, Integer> res = new TreeMap<Integer, Integer>();
		return res;
	}	


	
}//class SOM_GeomLineObj
