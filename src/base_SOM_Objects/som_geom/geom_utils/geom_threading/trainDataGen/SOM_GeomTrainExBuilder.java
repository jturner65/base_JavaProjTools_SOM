package base_SOM_Objects.som_geom.geom_utils.geom_threading.trainDataGen;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import base_SOM_Objects.som_examples.SOM_ExDataType;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomExampleManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomSmplDataForEx;
import base_SOM_Objects.som_geom.geom_utils.geom_threading.base.SOM_GeomCallable;
/**
 * build training data
 * @author john
 *
 */
public abstract class SOM_GeomTrainExBuilder extends SOM_GeomCallable {
	/**
	 * # of examples for each callable of this type to build 
	 */
	protected final int numExToBuildThisThread;
	/**
	 * this is the example manager that will receive the examples synthesized from this callable
	 */
	protected final SOM_GeomExampleManager exMgr;
	
	/**
	 * ref to array of all example objects
	 */
	protected final SOM_GeomSmplDataForEx[] allExamples;
	
	/**
	 * the number of samples required to build an object
	 */
	protected final int numExPerObj;	

	
	public SOM_GeomTrainExBuilder(SOM_GeomMapManager _mapMgr, SOM_GeomExampleManager _exMgr, SOM_GeomSmplDataForEx[] _allExs, int[] _intVals) {
		super(_mapMgr, _intVals[0],_intVals[1],_intVals[2]);
		//idxs 0,1,2 are st,end,thrdIDX
		numExToBuildThisThread = endIdx -stIdx;//calcNumPerThd(_intVals[3], _intVals[4]);
		this.msgObj.dispInfoMessage("SOM_GeomTrainExBuilder::"+ dataType,"ctor","Thd IDX : " + thdIDX + " Building total : " + _intVals[3] + " over "+ _intVals[4] + " threads : " + numExToBuildThisThread + " this thread");
		exMgr = _exMgr;
		allExamples = _allExs;
		numExPerObj = mapMgr.getGeomObjType().getVal();
		progressBnd = (int) (numExToBuildThisThread * progAmt);
	}

	private void buildTrainExData() {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		SOM_GeomSmplDataForEx[] exAra = new SOM_GeomSmplDataForEx[numExPerObj];
		for(int i=0;i<numExToBuildThisThread;++i) {
			exAra = genPtsForObj(rnd);
			SOM_GeomObj obj = _buildSingleObjectFromSamples(SOM_ExDataType.Training,exAra, i); incrProgress(i,"Building Training Data");
			exMgr.addExampleToMap(obj);		
		}	
	}//buildTrainExData
	
	protected String getObjID(int idx) {return String.format("%05d", stIdx + idx);}
	
	/**
	 * for lines just need 2 points; planes need 3 non-colinear points; spheres need 4 non-coplanar points, no 3 of which are colinear
	 * @return
	 */
	protected abstract SOM_GeomSmplDataForEx[] genPtsForObj(ThreadLocalRandom rnd);
	
	protected Integer[] genUniqueIDXs(int numToGen,  ThreadLocalRandom rnd){
		HashSet<Integer> idxs = new HashSet<Integer>();
		while(idxs.size() < numToGen) {	idxs.add(rnd.nextInt(0,allExamples.length));}		
		return idxs.toArray(new Integer[0]);
	}
	//for(int i=0;i<res.length;++i) {	res[i]=allExamples[rnd.nextInt(0,allExamples.length)];}
	//return res;
	
	/**
	 * build a single object to be stored at idx
	 * @param idx idx of object in resultant array
	 * @return build object
	 */
	protected abstract SOM_GeomObj _buildSingleObjectFromSamples(SOM_ExDataType _exDataType, SOM_GeomSmplDataForEx[] exAra, int idx);

	
	@Override
	public Boolean call() throws Exception {
		msgObj.dispInfoMessage("SOM_GeomTrainExBuilder::"+this.dataType, "call::thdIDX="+this.thdIDX, "Start building " + numExToBuildThisThread + " " +dataType +" training example objects from geom obj samples.");
		buildTrainExData();
		msgObj.dispInfoMessage("SOM_GeomTrainExBuilder::"+this.dataType, "call::thdIDX="+this.thdIDX, "Finished building " + numExToBuildThisThread + " " +dataType +" training example objects from geom obj samples.");
		
		return true;
	}

}//SOM_GeomTrainExBuilder
