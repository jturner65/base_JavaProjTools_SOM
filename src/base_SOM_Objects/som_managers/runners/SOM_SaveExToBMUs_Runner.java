package base_SOM_Objects.som_managers.runners;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_managers.runners.base.SOM_MapRunner;
import base_SOM_Objects.som_managers.runners.callables.SOM_SaveExToBMUs_CSV;

public class SOM_SaveExToBMUs_Runner extends SOM_MapRunner {
	
	//approx # per partition, divied up among the threads
	private final int rawNumPerPartition;
	protected final String fileNamePrefix;
	
	public SOM_SaveExToBMUs_Runner(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOM_Example[] _exData, String _dataTypName, boolean _forceST, String _fileNamePrefix, int _rawNumPerPartition) {
		super(_mapMgr, _th_exec, _exData, _dataTypName, _forceST);
		fileNamePrefix = _fileNamePrefix;
		rawNumPerPartition = _rawNumPerPartition;
	}

	@Override
	protected int getNumPerPartition() {return rawNumPerPartition;	}

	@Override
	protected void runMe_Indiv_ST() {
		SOM_SaveExToBMUs_CSV saver = new SOM_SaveExToBMUs_CSV(0, exData.length, exData, 0, dataTypName, fileNamePrefix);
		saver.call();
	}

	@Override
	protected void runMe_Indiv_End() {	}

	@Override
	protected void execPerPartition(List<Callable<Boolean>> ExMappers, int dataSt, int dataEnd, int pIdx, int numPartitions) {
		ExMappers.add(new SOM_SaveExToBMUs_CSV(dataSt, dataEnd, exData, pIdx, dataTypName, fileNamePrefix));
	}


}//SOM_SaveExToBMUs_Runner
