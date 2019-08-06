package base_SOM_Objects.som_geom.geom_examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_examples.SOM_ExDataType;
import base_SOM_Objects.som_examples.SOM_Example;
import base_SOM_Objects.som_examples.SOM_ExampleManager;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomSamplePointf;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomSmplDataForEx;
import base_SOM_Objects.som_geom.geom_utils.geom_threading.geomGen.SOM_GeomObjBldrRunner;
import base_Utils_Objects.io.MsgCodes;
/**
 * example manager base class for Geometry-based SOM projects
 * @author john
 *
 */
public abstract class SOM_GeomExampleManager extends SOM_ExampleManager {
	/**
	  * runnable object to manage various tasks
	  */
	protected SOM_GeomObjBldrRunner objRunner;
	/**
	 * all the constituent samples from the geometric objects this example manager manages
	 */
	protected SOM_GeomSmplDataForEx[] allSamples;

	/**
	 * set this to data type being managed by this example manager (training, validation, etc) 
	 */
	protected final SOM_ExDataType curDataType;
	
	protected final String exMgrName;

	public SOM_GeomExampleManager(SOM_MapManager _mapMgr, String _exName, String _longExampleName, SOM_ExDataType _curDataType, boolean _shouldValidate, String _exMgrName) {
		super(_mapMgr, _exName, _longExampleName, new boolean[] {_shouldValidate, true});	
		curDataType = _curDataType;exMgrName=_exMgrName;
	}
	
	@Override
	protected final void reset_Priv() {
		msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "reset_Priv", "Example manager : " + exMgrName + " reset called.");
		allSamples = new SOM_GeomSmplDataForEx[0];
	}

	
	/**
	 * set obj runner so that example manager can consume it
	 * @param _objRunner
	 */
	public final void setObjRunner(SOM_GeomObjBldrRunner _objRunner) {objRunner=_objRunner;}

	@Override
	protected final void buildFtrVec_Priv() {
		//call to buildFeatureVector for all examples
		msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "buildFtrVec_Priv", "Example manager : " + exMgrName + " buildFtrVec_Priv called : " + exampleMap.size()+ " examples present.");
		mapMgr._ftrVecBuild(exampleMap.values(),0,exampleName, true);		
	}	

	/**
	 * this will build the training data in this example manager based on the geometric data passed in geomExMgr
	 * @param geomExMgr
	 */
	public final void buildTrainingDataFromGeomObjs(SOM_GeomExampleManager geomExMgr, boolean setUniqueIDXs, int ttlNumTrainEx) {
		msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "buildTrainingDataFromGeomObjs", "Start building example data in " + exampleName +"'s "+exMgrName + " ex mgr using geometric data from " + geomExMgr.exampleName +"'s " + geomExMgr.exMgrName+ " examples present.");
		reset();
		//all geomgetric objects from geometry example manager
		SOM_GeomObj[] geomEx = (SOM_GeomObj[]) geomExMgr.buildExampleArray();
		int numSamplesTTL = geomEx.length * geomEx[0].getNumSamples();
		allSamples = new SOM_GeomSmplDataForEx[numSamplesTTL];
		int idx = 0;
		for(SOM_GeomObj ex : geomEx) {
			SOM_GeomSamplePointf[] smpls = ex.getAllSamplePts();
			for(int i=0;i<smpls.length;++i) {	allSamples[idx++]=new SOM_GeomSmplDataForEx(ex,smpls[i]);}			
		}
		SOM_GeomTrainingExUniqueID[] _idxsToUse = null;
		//if desired to have unique samples build every training example
		if(setUniqueIDXs) {
			long maxExPossible = getMaxNumUniqueTrainingEx(allSamples.length);
			if(maxExPossible < ttlNumTrainEx) {			//not setting unique due to too many being requested
				msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "buildTrainingDataFromGeomObjs", "In " + exampleName +"'s "+exMgrName + " ex mgr : Unable to build requisted # " +ttlNumTrainEx + " of UNIQUE training examples given : " + allSamples.length+" Samples to choose from due to not enough possible combinations :  " +maxExPossible+ ", so allowing for duplication.");
			} else {
				msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "buildTrainingDataFromGeomObjs", "Start building " +ttlNumTrainEx + " UNIQUE training " + exampleName +" examples from " + allSamples.length+" samples; Max unique possible combinations :  " +maxExPossible);
				_idxsToUse = buildUniqueIDXsForObjType(allSamples,ttlNumTrainEx);
				msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "buildTrainingDataFromGeomObjs", "Finished building " +ttlNumTrainEx + " UNIQUE training " + exampleName +" examples from " + allSamples.length+" samples; Max unique possible combinations :  " +maxExPossible);
			}			
		}//if requested unique
		//now need to build # of training examples - need to do this in multi-threaded environment
		buildAllEx_MT(allSamples, mapMgr.getNumUsableThreads(),ttlNumTrainEx,_idxsToUse); 
		
		setAllDataLoaded();
		setAllDataPreProcced();
		msgObj.dispInfoMessage("SOM_GeomExampleManager::"+exampleName, "buildTrainingDataFromGeomObjs", "Finished building example data in " + exampleName +"'s "+exMgrName + " ex mgr using geometric data from " + geomExMgr.exampleName +"'s " + geomExMgr.exMgrName+ " examples present.");
	}//	buildTrainingDataFromGeom

	/**
	 * determine max # of unique training examples able to be built for object type given # of samples
	 */
	protected abstract long getMaxNumUniqueTrainingEx(long ttlNumSamples);

	/**
	 * build a single list of sorted, unique idxs in allSamples that satisfy object creation constraints
	 * @param allSamples list of all object samples available
	 * @param rnd the current thread's rng engine
	 * @return sorted list of idxs
	 */
	protected abstract Integer[] genUniqueObjIDXs(SOM_GeomSmplDataForEx[] allSamples, ThreadLocalRandom rnd);
	
	/**
	 * build ttlNumTrainEx x <# objs for object type> 2 d array of unique, appropriate idxs for training data
	 * @param ttlNumTrainEx
	 * @return
	 */	
	protected final SOM_GeomTrainingExUniqueID[] buildUniqueIDXsForObjType(SOM_GeomSmplDataForEx[] allSamples, int ttlNumTrainEx) {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		HashMap<SOM_GeomTrainingExUniqueID, Integer> res = new HashMap<SOM_GeomTrainingExUniqueID, Integer>();
		SOM_GeomTrainingExUniqueID ex;
		while (res.size() < ttlNumTrainEx) {
			ex = new SOM_GeomTrainingExUniqueID(genUniqueObjIDXs(allSamples, rnd));	
			res.put(ex, 2);	
		}		
		return res.keySet().toArray(new SOM_GeomTrainingExUniqueID[0]);
	}//buildUniqueIDXsForObjType	
	/**
	 * build all training examples for objed type
	 * @param allSamples
	 * @param numThdCallables
	 * @param ttlNumTrainEx
	 * @param _idxsToUse precalculated idxs for each object to use - null if allowing for duplication, otherwise ttlNumTrainEx long
	 */
	protected abstract void buildAllEx_MT(SOM_GeomSmplDataForEx[] allSamples, int numThdCallables, int ttlNumTrainEx, SOM_GeomTrainingExUniqueID[] _idxsToUse);

	/**
	 * code to execute after examples have had ftrs calculated - this will calculate std features and any alternate ftr mappings if used
	 */
	@Override
	protected void buildAfterAllFtrVecsBuiltStructs_Priv() {
		//call to buildFeatureVector for all examples to perform -finalization- after all feature vectors of this type have been built
		mapMgr._ftrVecBuild(exampleMap.values(),2,exampleName, true);	
	}
	/**
	 * determine how many work elements should be assigned per thread 
	 * @param numVals total number of work elements to execute
	 * @param numThds total number of threads available
	 * @return number of work elements per thread to assign
	 */
	protected final int calcNumPerThd(int numVals, int numThds) {	return (int) ((numVals -1)/(1.0*numThds)) + 1;	}//calcNumPerThd
	
	@Override
	/**
	 * after example array has been built, add specific funcitonality for these types of examples, especially if validation should occur
	 */
	protected final void buildExampleArrayEnd_Priv(boolean validate) {}
	
	/**
	 * 
	 */
	@Override
	protected final void buildSTLoader(String[] loadSrcFNamePrefixAra, int numPartitions) {
		for (int i=numPartitions-1; i>=0;--i) {
			String dataFile = loadSrcFNamePrefixAra[0]+"_"+i+".csv";
			String[] csvLoadRes = fileIO.loadFileIntoStringAra(dataFile,  exampleName+ " Data file " + i +" of " +numPartitions +" loaded",  exampleName+ " Data File " + i +" of " +numPartitions +" Failed to load");
			//ignore first entry - header
			for (int j=1;j<csvLoadRes.length; ++j) {
				String str = csvLoadRes[j];
				int pos = str.indexOf(',');
				String oid = str.substring(0, pos);
				SOM_Example ex = buildSingleExample(oid, str);
				exampleMap.put(oid, ex);			
			}
		}	
	}//buildSTLoader
	
	/**
	 * save and load the UI values used to build the preprocessed anim data for this project
	 * @param uiVals
	 */	
	public final boolean saveGeomObjsUIVals(TreeMap<String,String> uiVals) {
		msgObj.dispMessage("SOM_GeomExampleManager::"+exampleName,"saveGeomObjsUIVals","Saving UI Values used to synthesize geometric data.", MsgCodes.info5);
		String[] saveGeomUIDestFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Save(exampleName+"GeomSrcData_UIVals");
		ArrayList<String> csvResTmp = new ArrayList<String>();		
		String uiGeomConfigFileName = saveGeomUIDestFNamePrefixAra[0]+".csv";
		for(String key : uiVals.keySet()) {	csvResTmp.add(key+","+uiVals.get(key));}
		boolean success = fileIO.saveStrings(uiGeomConfigFileName, csvResTmp);
		
		msgObj.dispMessage("SOM_GeomExampleManager::"+exampleName,"saveGeomObjsUIVals","Finished saving UI Values used to synthesize geometric data : Success : " +success, MsgCodes.info5);
		return success;
	}
	
	public final TreeMap<String,String> loadGeomObjsUIVals(String subDir) {
		msgObj.dispMessage("SOM_GeomExampleManager::"+exampleName,"loadGeomObjsUIVals","Loading UI Values used to synthesize geometric data from : " +subDir, MsgCodes.info5);				
		String[] loadGeomUISrcFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Load(subDir, exampleName+ "GeomSrcData_UIVals");
		String uiGeomConfigFileName = loadGeomUISrcFNamePrefixAra[0]+".csv";
		String[] csvLoadRes = fileIO.loadFileIntoStringAra(uiGeomConfigFileName, "Geom Object UI Values file loaded.", "Geom Object UI Values file Failed to load");
		TreeMap<String,String> res = new TreeMap<String,String>();
		if(csvLoadRes.length == 0) {
			msgObj.dispMessage("SOM_GeomExampleManager::"+exampleName,"loadGeomObjsUIVals","Failed to load UI Values used to synthesize geometric data from : "+ subDir, MsgCodes.info5);
			return res;
		}
		for(String s : csvLoadRes) {
			String[] vals = s.split(",");
			res.put(vals[0].trim(), vals[1].trim());			
		}		
		setAllDataLoaded();
		setAllDataPreProcced();
		msgObj.dispMessage("SOM_GeomExampleManager::"+exampleName,"loadGeomObjsUIVals","Finished loading " + res.size() +" UI Values used to synthesize geometric data.", MsgCodes.info5);
		return res;		
	}
	

	/**
	 * return array of examples to save their bmus
	 * @return
	 */
	@Override
	protected SOM_Example[] getExToSave() {
		if(!isExampleArrayBuilt()) {		buildExampleArray();	}	
		msgObj.dispInfoMessage("Geom_SOMExampleManager::"+exampleName,"getExToSave","Size of exToSaveBMUs : " + SOMexampleArray.length);
		return SOMexampleArray;
	}


}//class SOM_GeomExampleManager
