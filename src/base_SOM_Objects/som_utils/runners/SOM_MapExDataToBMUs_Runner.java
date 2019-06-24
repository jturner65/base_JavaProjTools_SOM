package base_SOM_Objects.som_utils.runners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_examples.*;
import base_SOM_Objects.som_utils.SOM_MapDataToBMUs;
import base_Utils_Objects.io.MessageObject;
import base_Utils_Objects.io.MsgCodes;
import base_Utils_Objects.vectorObjs.Tuple;

/**
 * this will build a runnable to perform mapping in its own thread, to find bmus for passed examples
 * @author john
 *
 */
public class SOM_MapExDataToBMUs_Runner extends SOM_MapRunner{

	SOM_ExDataType dataType;
	int curMapTestFtrType;
	boolean useChiSqDist;
	TreeMap<Tuple<Integer,Integer>, SOM_MapNode> MapNodes;
	//map of ftr idx and all map nodes that have non-zero presence in that ftr
	protected TreeMap<Integer, HashSet<SOM_MapNode>> MapNodesByFtr;
	//ref to flags idx of boolean denoting this type of data is ready to save mapping results
	protected int flagsRdyToSaveIDX;
	//approx # per partition, divied up among the threads
	private static final int rawNumPerPartition = 200000;
	
	protected double ttlProgress=-.1;
	
	List<Future<Boolean>> ExMapperFtrs = new ArrayList<Future<Boolean>>();
	List<MapExampleDataToBMUs> ExMappers = new ArrayList<MapExampleDataToBMUs>();
		
	public SOM_MapExDataToBMUs_Runner(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOM_Example[] _exData, String _dataTypName, SOM_ExDataType _dataType, int _readyToSaveIDX, boolean _forceST) {
		super( _mapMgr, _th_exec, _exData, _dataTypName,_forceST);
		useChiSqDist = mapMgr.getUseChiSqDist();		
		MapNodes = mapMgr.getMapNodes();
		MapNodesByFtr = mapMgr.getMapNodesByFtr();
		curMapTestFtrType = mapMgr.getCurrentTestDataFormat();			
		dataType = _dataType;
		flagsRdyToSaveIDX = _readyToSaveIDX;
	}//ctor
	

	@Override
	protected int getNumPerPartition() {return rawNumPerPartition;	}
	
//	
//	protected void incrTTLProgress(int len, int idx) {
//		ttlProgress = idx/(1.0 * len);
//		if((ttlProgress * 100) % 10 == 0) {
//			msgObj.dispInfoMessage("MapTestDataToBMUs_Runner","incrTTLProgress", "Total Progress at : " + String.format("%.4f",ttlProgress));
//		}
//		if(ttlProgress > 1.0) {ttlProgress = 1.0;}	
//	}
//	
//	public double getTtlProgress() {return ttlProgress;}
	
	protected void runner(int dataSt, int dataEnd, int pIdx, int ttlParts) {
		int numEx = dataEnd-dataSt;
		int numForEachThrd = calcNumPerThd(numEx, numUsableThreads);
		//use this many for every thread but last one
		int stIDX = dataSt;
		int endIDX = dataSt + numForEachThrd;		
		String partStr = " in partition "+pIdx+" of "+ttlParts;
		int numExistThds = ExMappers.size();
		for (int i=0; i<(numUsableThreads-1);++i) {				
			ExMappers.add(new MapExampleDataToBMUs(mapMgr,stIDX, endIDX,  exData, i+numExistThds, dataTypName+partStr, useChiSqDist));
			stIDX = endIDX;
			endIDX += numForEachThrd;
		}
		//last one probably won't end at endIDX, so use length
		if(stIDX < dataEnd) {ExMappers.add(new MapExampleDataToBMUs(mapMgr,stIDX, dataEnd, exData, numUsableThreads-1 + numExistThds, dataTypName+partStr,useChiSqDist));}
	}//runner
	
	/**
	 * Multi threaded run
	 * @param numPartitions : # of partitions/threads to allow
	 * @param numPerPartition : # of examples per thread to process
	 */
	@Override
	protected final void runMe_Indiv_MT(int numPartitions, int numPerPartition) {
		ExMappers = new ArrayList<MapExampleDataToBMUs>();			
		msgObj.dispMessage("SOM_MapExampleDataToBMUs_Runner","runMe_Indiv_MT","Starting finding bmus for all " +exData.length + " "+dataTypName+" data using " +numPartitions + " partitions of length " +numPerPartition +".", MsgCodes.info1);
		int dataSt = 0;
		int dataEnd = numPerPartition;
		for(int pIdx = 0; pIdx < numPartitions-1;++pIdx) {
			runner(dataSt, dataEnd, pIdx,numPartitions);
			dataSt = dataEnd;
			dataEnd +=numPerPartition;			
		}
		runner(dataSt, exData.length, numPartitions-1,numPartitions);			
		
		ExMapperFtrs = new ArrayList<Future<Boolean>>();
		try {ExMapperFtrs = th_exec.invokeAll(ExMappers);for(Future<Boolean> f: ExMapperFtrs) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
		msgObj.dispMessage("SOM_MapExampleDataToBMUs_Runner","runMe_Indiv_MT","Finished finding bmus for all " +exData.length + " "+dataTypName+" data using " +numPartitions + " partitions of length " +numPerPartition +".", MsgCodes.info1);
	}//runMe_Indiv_MT
	
	/**
	 * single threaded run
	 */
	@Override
	protected final void runMe_Indiv_ST() {
		MapExampleDataToBMUs runner = new MapExampleDataToBMUs(mapMgr,0, exData.length,  exData, 0, dataTypName+" Running Single Threaded", useChiSqDist);
		runner.call();
	}//runMe_Indiv_ST
	
	/**
	 * code to execute once all runners have completed
	 */
	@Override
	protected final void runMe_Indiv_End() {	
		//go through every test example, if any, and attach prod to bmu - needs to be done synchronously because don't want to concurrently modify bmus from 2 different test examples		
		msgObj.dispMessage("SOM_MapExDataToBMUs_Runner","run","Finished finding bmus for all " +exData.length + " "+dataTypName+" data. Start adding "+dataTypName+" data to appropriate bmu's list.", MsgCodes.info1);
		//below must be done when -all- dataType examples are done
		mapMgr._completeBMUProcessing(exData, dataType, canMultiThread);
		msgObj.dispMessage("SOM_MapExDataToBMUs_Runner","run","Finished Mapping "+dataTypName+" data to best matching units.", MsgCodes.info5);		

		//Set some flag here stating that saving/further processing results is now available
		mapMgr.setFlag(flagsRdyToSaveIDX, true);		
	}


}//MapTestDataToBMUs_Runner

class MapExampleDataToBMUs extends SOM_MapDataToBMUs{
	protected SOM_Example[] exs;
	
	public MapExampleDataToBMUs(SOM_MapManager _mapMgr, int _stProdIDX, int _endProdIDX, SOM_Example[] _exs, int _thdIDX, String _type, boolean _useChiSqDist) {
		super(_mapMgr, _stProdIDX,  _endProdIDX,  _thdIDX, _type, _useChiSqDist);			
		exs = _exs;		//make sure these are cast appropriately
	}	
	
	private void mapExample(int i, BiFunction<TreeMap<Integer, Float>, TreeMap<Integer, Float>, Double> _distFunc) {
		TreeMap<Double, ArrayList<SOM_MapNode>> mapNodesByDist = exs[i].findBMUFromFtrNodes(MapNodesByFtr,_distFunc, curMapFtrType);
		if(mapNodesByDist == null) {msgObj.dispMessage("SOM_MapExDataToBMUs_Runner::MapExampleDataToBMUs", "Run Thread : " +thdIDX, "ERROR!!! " + dataType + " ex " + exs[i].OID + " does not have any features that map to Map Nodes in SOM!", MsgCodes.error5);}
		incrProgress(i);
		//example has probabilities for specific class and categories based on BMU
		exs[i].setSegmentsAndProbsFromBMU();
	}//mapExample
	
	
	@Override
	protected boolean mapAllDataToBMUs() {
		if(exs.length == 0) {
			msgObj.dispMessage("SOM_MapExDataToBMUs_Runner::MapExampleDataToBMUs", "Run Thread : " +thdIDX, ""+dataType+" Data["+stIdx+":"+endIdx+"] is length 0 so nothing to do. Aborting thread.", MsgCodes.info5);
			return true;}
		msgObj.dispMessage("SOM_MapExDataToBMUs_Runner::MapExampleDataToBMUs", "Run Thread : " +thdIDX, "Starting "+dataType+" Data["+stIdx+":"+endIdx+"]  (" + (endIdx-stIdx) + " exs), to BMU mapping using " + ftrTypeDesc + " Features and including only shared ftrs in distance.", MsgCodes.info5);
		if(MapNodesByFtr==null) {msgObj.dispMessage("mapTestDataToBMUs", "Run Thread : " +thdIDX, " ALERT!!! MapNodesByFtr is null!!!", MsgCodes.error5);}
//		if (useChiSqDist) {		for (int i=stIdx;i<endIdx;++i) {exs[i].findBMUFromFtrNodes(MapNodesByFtr,exs[i]::getSqDistFromFtrType_ChiSq_Exclude, curMapFtrType);incrProgress(i);}} 
//		else {					for (int i=stIdx;i<endIdx;++i) {exs[i].findBMUFromFtrNodes(MapNodesByFtr,exs[i]::getSqDistFromFtrType_Exclude, curMapFtrType);incrProgress(i);}} 	
		if (useChiSqDist) {		for (int i=stIdx;i<endIdx;++i) {mapExample(i, exs[i]::getSqDistFromFtrType_ChiSq_Exclude);}} 
		else {					for (int i=stIdx;i<endIdx;++i) {mapExample(i, exs[i]::getSqDistFromFtrType_Exclude);}} 	
		
		msgObj.dispMessage("SOM_MapExDataToBMUs_Runner::MapExampleDataToBMUs", "Run Thread : " +thdIDX, "Finished "+dataType+" Data["+stIdx+":"+endIdx+"] to BMU mapping", MsgCodes.info5);		
		return true;
	}		
}//MapExampleDataToBMUs