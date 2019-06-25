package base_SOM_Objects.som_utils.runners;

import java.util.concurrent.ExecutorService;

import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_examples.SOM_Example;
import base_Utils_Objects.threading.runners.myThreadRunner;

/**
 * Struct to manage a runner specifically for SOM data
 * This will launch a number of callables suitable 
 * for machine arch to manage multi-threaded calcs.  Instances of this class 
 * will manage instancing and invoking all threads to execute functionality 
 * in either MT or ST environment.
 * 
 * @author john
 *
 */
public abstract class SOM_MapRunner extends myThreadRunner {
	//owning map manager
	protected final SOM_MapManager mapMgr;
	//name of data type being operated on - for display purposes mainly
	protected final String dataTypName;
	//the example data being operated on
	protected final SOM_Example[] exData;
	
	public SOM_MapRunner(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOM_Example[] _exData, String _dataTypName, boolean _forceST) {
		super(_mapMgr.getMsgObj(), _th_exec, _mapMgr.isMTCapable() && !_forceST, _mapMgr.getNumUsableThreads()-1, _exData.length);		
		mapMgr = _mapMgr; 
		exData = _exData;
		dataTypName = _dataTypName;		
	}//ctor
	

	
}// class SOM_MapRunners
