package base_SOM_Objects.som_utils.runners;

import java.util.*;
import java.util.concurrent.*;

import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_examples.SOM_Example;
import base_Utils_Objects.io.MessageObject;
import base_Utils_Objects.io.MsgCodes;


/**
 * manage ftr calculation processes 
 * @author john
 */
public class SOM_CalcExFtrs_Runner extends SOM_MapRunner{

	//approx # per partition, divied up among the threads
	public static final int rawNumPerPartition = 40000;
	//type of execution - 0 is build features, 1 is postftrveccalc
	private final int typeOfProc;
	private final String calcTypeStr;
	
	List<Future<Boolean>> testCalcMapperFtrs = new ArrayList<Future<Boolean>>();
	List<MapFtrCalc> testCalcMappers = new ArrayList<MapFtrCalc>();

	//type of calc, idxed by _typeOfProc
	protected static final String[] typeAra = new String[] {"Feature Calc","Post Indiv Feature Calc","Calcs called After All Example Ftrs built"};

	public SOM_CalcExFtrs_Runner(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOM_Example[] _exData, String _dataTypName, int _typeOfProc, boolean _forceST) {
		super( _mapMgr, _th_exec, _exData, _dataTypName,_forceST);
		typeOfProc = _typeOfProc;
		calcTypeStr = typeAra[typeOfProc];
	}

	@Override
	protected int getNumPerPartition() {return rawNumPerPartition;	}
		
	/**
	 * Multi threaded run - make sure there's no concurrency-violating functionality in ftr calc
	 * @param numPartitions : # of partitions/threads to allow
	 * @param numPerPartition : # of examples per thread to process
	 */
	@Override
	protected final void runMe_Indiv_MT(int numPartitions, int numPerPartition) {
		msgObj.dispMessage("SOM_MapExFtrCalcs_Runner","runMe_Indiv_MT","Starting performing "+calcTypeStr+" calcs for all " +exData.length + " "+dataTypName+" examples using " +numPartitions + " partitions of length " +numPerPartition +".", MsgCodes.info1);
		testCalcMappers = new ArrayList<MapFtrCalc>();
		int dataSt = 0;
		int dataEnd = numPerPartition;
		for(int pIdx = 0; pIdx < numPartitions-1;++pIdx) {
			testCalcMappers.add(new MapFtrCalc(mapMgr, dataSt, dataEnd, exData, pIdx, dataTypName, calcTypeStr, typeOfProc));
			dataSt = dataEnd;
			dataEnd +=numPerPartition;			
		}
		if(dataSt < exData.length) {testCalcMappers.add(new MapFtrCalc(mapMgr, dataSt, exData.length, exData, numPartitions-1, dataTypName, calcTypeStr, typeOfProc));}
		
		testCalcMapperFtrs = new ArrayList<Future<Boolean>>();
		try {testCalcMapperFtrs = th_exec.invokeAll(testCalcMappers);for(Future<Boolean> f: testCalcMapperFtrs) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		

		msgObj.dispMessage("SOM_MapExFtrCalcs_Runner","runMe_Indiv_MT","Finished performing "+calcTypeStr+" calcs for all " +exData.length + " "+dataTypName+" examples using " +numPartitions + " partitions of length " +numPerPartition +".", MsgCodes.info1);
		
	}//runMe_Indiv_MT
	
	/**
	 * single threaded run
	 */
	@Override
	protected final void runMe_Indiv_ST() {
		MapFtrCalc mapper = new MapFtrCalc(mapMgr, 0, exData.length, exData, 0, dataTypName, calcTypeStr, typeOfProc);
		mapper.call();
	}//runMe_Indiv_ST
	
	/**
	 * code to execute once all runners have completed
	 */
	@Override
	protected final void runMe_Indiv_End() {}
	
}//MapExFtrCalcs_Runner


class MapFtrCalc implements Callable<Boolean>{
	protected MessageObject msgObj;
	protected final int stIdx, endIdx, thdIDX, progressBnd, typeOfCalc;

	protected String dataType, calcType;
	protected static final float progAmt = .2f;
	protected double progress = -progAmt;
	protected SOM_Example[] exs;
	
	
	public MapFtrCalc(SOM_MapManager _mapMgr, int _stExIDX, int _endExIDX, SOM_Example[] _exs, int _thdIDX, String _datatype, String _calcType,int _typeOfCalc) {
		msgObj = _mapMgr.buildMsgObj();//make a new one for every thread
		exs=_exs;
		stIdx = _stExIDX;
		endIdx = _endExIDX;
		progressBnd = (int) ((endIdx-stIdx) * progAmt);
		thdIDX= _thdIDX;
		dataType = _datatype;
		typeOfCalc = _typeOfCalc;	
		calcType = _calcType;
	} 
	
	protected void incrProgress(int idx) {
		if(((idx-stIdx) % progressBnd) == 0) {		
			progress += progAmt;	
			msgObj.dispInfoMessage("MapFtrCalc","incrProgress::thdIDX=" + String.format("%02d", thdIDX)+" ", "Progress for "+calcType+ " with dataType : " +dataType +" at : " + String.format("%.2f",progress));
		}
		if(progress > 1.0) {progress = 1.0;}
	}

	public double getProgress() {	return progress;}
	
	@Override
	public Boolean call() {
		if(exs.length == 0) {
			msgObj.dispMessage("MapFtrCalc", "Run Thread : " +thdIDX, ""+dataType+" Data["+stIdx+":"+endIdx+"] is length 0 so nothing to do. Aborting thread.", MsgCodes.info5);
			return true;}
		msgObj.dispMessage("MapFtrCalc", "Run Thread : " +thdIDX, "Starting "+calcType+" on "+dataType+" Data["+stIdx+":"+endIdx+"]  (" + (endIdx-stIdx) + " exs), ", MsgCodes.info5);
		//typeOfCalc==0 means build features
		if(typeOfCalc==0) {			for (int i=stIdx;i<endIdx;++i) {exs[i].buildFeatureVector();incrProgress(i);}} 							//build ftrs
		else if(typeOfCalc==1) {	for (int i=stIdx;i<endIdx;++i) {exs[i].postFtrVecBuild();incrProgress(i);}	}							//typeOfCalc==1 means post ftr build calc - (Per example finalizing)
		else if(typeOfCalc==2) {	for (int i=stIdx;i<endIdx;++i) {exs[i].buildAfterAllFtrVecsBuiltStructs();incrProgress(i);}	}			//typeOfCalc==2 means after all ftr vecs have been build 
		msgObj.dispMessage("MapFtrCalc", "Run Thread : " +thdIDX, "Finished "+calcType+" on "+dataType+" Data["+stIdx+":"+endIdx+"] # calcs : " + (endIdx-stIdx), MsgCodes.info5);		
		return true;
	}
	
}//class MapFtrCalc
