package base_SOM_Objects.som_managers.runners;

import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import base_Math_Objects.vectorObjs.tuples.Tuple;
import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_managers.runners.base.SOM_MapRunner;
import base_SOM_Objects.som_managers.runners.callables.SOM_MapExDataToBMUs;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * this will build a runnable to perform mapping in its own thread, to find bmus for passed examples
 * @author john
 *
 */
public class SOM_MapExDataToBMUs_Runner extends SOM_MapRunner{

	SOM_ExDataType dataType;
	SOM_FtrDataType curMapTrainFtrType;
	boolean useChiSqDist;
	TreeMap<Tuple<Integer,Integer>, SOM_MapNode> MapNodes;
	//map of ftr idx and all map nodes that have non-zero presence in that ftr
	protected TreeMap<Integer, HashSet<SOM_MapNode>> MapNodesByFtr;
	//ref to flags idx of boolean denoting this type of data is ready to save mapping results
	protected int flagsRdyToSaveIDX;
	//approx # per partition, divied up among the threads
	private static final int rawNumPerPartition = 200000;
	
	protected double ttlProgress=-.1;
		
	public SOM_MapExDataToBMUs_Runner(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOM_Example[] _exData, String _dataTypName, SOM_ExDataType _dataType, int _readyToSaveIDX, boolean _forceST) {
		super( _mapMgr, _th_exec, _exData, _dataTypName,_forceST);
		useChiSqDist = _mapMgr.getUseChiSqDist();		
		MapNodes = _mapMgr.getMapNodes();
		MapNodesByFtr = _mapMgr.getMapNodesByFtr();
		curMapTrainFtrType = _mapMgr.getCurrentTrainDataFormat();			
		dataType = _dataType;
		flagsRdyToSaveIDX = _readyToSaveIDX;
	}//ctor
	

	@Override
	protected int getNumPerPartition() {return rawNumPerPartition;	}
	
	@Override
	protected void execPerPartition(List<Callable<Boolean>> ExMappers, int dataSt, int dataEnd, int pIdx, int ttlParts) {
		int numEx = dataEnd-dataSt;
		int numForEachThrd = calcNumPerThd(numEx, numUsableThreads);
		//use this many for every thread but last one
		int stIDX = dataSt;
		int endIDX = dataSt + numForEachThrd;		
		String partStr = " in partition "+pIdx+" of "+ttlParts;
		int numExistThds = ExMappers.size();
		for (int i=0; i<(numUsableThreads-1);++i) {				
			ExMappers.add(new SOM_MapExDataToBMUs(mapMgr,stIDX, endIDX,  exData, i+numExistThds, dataTypName+partStr, useChiSqDist));
			stIDX = endIDX;
			endIDX += numForEachThrd;
		}
		//last one probably won't end at endIDX, so use length
		if(stIDX < dataEnd) {ExMappers.add(new SOM_MapExDataToBMUs(mapMgr,stIDX, dataEnd, exData, numUsableThreads-1 + numExistThds, dataTypName+partStr,useChiSqDist));}
	}//runner
	
	/**
	 * single threaded run
	 */
	@Override
	protected final void runMe_Indiv_ST() {
		SOM_MapExDataToBMUs runner = new SOM_MapExDataToBMUs(mapMgr,0, exData.length,  exData, 0, dataTypName+" Running Single Threaded", useChiSqDist);
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


}//SOM_MapExDataToBMUs