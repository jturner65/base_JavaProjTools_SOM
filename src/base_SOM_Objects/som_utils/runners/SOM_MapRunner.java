package base_SOM_Objects.som_utils.runners;

import java.util.concurrent.ExecutorService;

import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_examples.SOM_Example;
import base_Utils_Objects.io.MessageObject;

/**
 * manage a runner that will launch a number of callables suitable 
 * for machine arch to manage multi-threaded calcs.  Instances of this class 
 * will manage instancing and invoking all threads to execute functionality 
 * in either MT or ST environment.
 * 
 * @author john
 *
 */
public abstract class SOM_MapRunner {
	//owning map manager
	protected final SOM_MapManager mapMgr;
	//msg object to handle console/log IO
	protected final MessageObject msgObj;
	//whether or not this calculation can be executed in multi-thread
	protected final boolean canMultiThread;
	//name of data type being operated on - for display purposes mainly
	protected final String dataTypName;
	//the example data being operated on
	protected final SOM_Example[] exData;
	//the # of usable threads available for MT exec
	protected final int numUsableThreads;
	//ref to thread executor
	protected final ExecutorService th_exec;	
	
	public SOM_MapRunner(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOM_Example[] _exData, String _dataTypName, boolean _forceST) {
		mapMgr = _mapMgr; 
		msgObj = mapMgr.getMsgObj();
		numUsableThreads = mapMgr.getNumUsableThreads()-1;
		canMultiThread = mapMgr.isMTCapable() && !_forceST;
		th_exec = _th_exec;
		exData = _exData;
		dataTypName = _dataTypName;
		
	}//ctor
	
	//determine how many values should be per thread, if 
	public int calcNumPerThd(int numVals, int numThds) {
		//return (int) Math.round((numVals + Math.round(numThds/2))/(1.0*numThds));
		return (int) ((numVals -1)/(1.0*numThds)) + 1;
	}//calcNumPerThd
	
	protected abstract int getNumPerPartition();

	/**
	 * launch this runner
	 */
	public final void runMe() {
		if(canMultiThread){
			int numPartitions = Math.round(exData.length/(1.0f*getNumPerPartition()) + .5f);
			if(numPartitions < 1) {numPartitions = 1;}
			int numPerPartition = calcNumPerThd(exData.length,numPartitions);
			runMe_Indiv_MT(numPartitions, numPerPartition);
		} else {
			runMe_Indiv_ST();
		}
		runMe_Indiv_End();
	}//runMe()
	
	protected abstract void runMe_Indiv_MT(int numPartitions, int numPerPartition);
	
	protected abstract void runMe_Indiv_ST();
	
	protected abstract void runMe_Indiv_End();
	
	
}// class SOM_MapRunners
