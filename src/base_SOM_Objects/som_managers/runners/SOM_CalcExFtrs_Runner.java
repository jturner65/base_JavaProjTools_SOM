package base_SOM_Objects.som_managers.runners;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_managers.runners.base.SOM_MapRunner;
import base_SOM_Objects.som_managers.runners.callables.SOM_CalcExFtrs;


/**
 * manage ftr calculation processes - NOTE performing these calculations in a multi-threaded environment may not be threadsafe
 * @author john
 */
public class SOM_CalcExFtrs_Runner extends SOM_MapRunner{

	/**
	 * approx # per partition, divied up among the threads
	 */
	public static final int rawNumPerPartition = 40000;
	//type of execution - 0 is build features, 1 is postftrveccalc
	private final int typeOfProc;
	private final String calcTypeStr;
	
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
	 * single threaded run
	 */
	@Override
	protected final void runMe_Indiv_ST() {
		SOM_CalcExFtrs mapper = new SOM_CalcExFtrs(0, exData.length, exData, 0, dataTypName, calcTypeStr, typeOfProc);
		mapper.call();
	}//runMe_Indiv_ST
	
	/**
	 * code to execute once all runners have completed
	 */
	@Override
	protected final void runMe_Indiv_End() {}

	@Override
	protected void execPerPartition(List<Callable<Boolean>> ExMappers, int dataSt, int dataEnd, int pIdx, int ttlParts) {
		ExMappers.add(new SOM_CalcExFtrs(dataSt, dataEnd, exData, pIdx, dataTypName, calcTypeStr, typeOfProc));		
	}
	
}//SOM_CalcExFtrs_Runner
