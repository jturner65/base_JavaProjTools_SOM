package base_SOM_Objects.som_fileIO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;


/**
 * This class will map the BMUs as determined by SOM training code (loaded form bmu file) to each 
 * training example - these bmus were not derived through distance calculations in this application, 
 * but from provided bmu file built during SOM training.
 * @author john
 *
 */
public class SOM_ExBMULoader implements Callable<Boolean>{
	//object that manages message displays on screen
	private MessageObject msgObj;
	//thread index
	int thdIDX;
	//whether or not we are using chi sq distance (normalized by variance per ftr)
	boolean useChiSqDist;
	//what kind of features used to train map (unnormalized, normed per feature across all data, normed per example)
	int ftrTypeUsedToTrainIDX;
	//should always be training, not necessary
	int typeOfEx;	
	//map of map nodes to arrays of examples that consider these map nodes their bmus - loaded in from file written by SOM training code
	//any particular map node will only exist in one map, so no map nodes will be concurrently modified by this structure
	HashMap<SOM_MapNode, ArrayList<SOM_Example>> bmusToExmpl;
	
	public SOM_ExBMULoader(MessageObject _msgObj, int _ftrTypeUsedToTrain, boolean _useChiSqDist, int _typeOfEx, HashMap<SOM_MapNode, ArrayList<SOM_Example>> _bmusToExmpl, int _thdIDX) {
		msgObj = _msgObj;
		ftrTypeUsedToTrainIDX = _ftrTypeUsedToTrain;
		useChiSqDist =_useChiSqDist;
		thdIDX= _thdIDX;	
		typeOfEx = _typeOfEx;
		bmusToExmpl = _bmusToExmpl;
		int numExs = 0;
		for (SOM_MapNode tmpMapNode : bmusToExmpl.keySet()) {
			ArrayList<SOM_Example> exs = bmusToExmpl.get(tmpMapNode);
			numExs += exs.size();
		}		
		msgObj.dispMessage("SOM_ExBMULoader","ctor : thd_idx : "+thdIDX, "# of bmus to proc : " +  bmusToExmpl.size() + " # exs : " + numExs, MsgCodes.info2);
	}//ctor

	@Override
	public Boolean call() throws Exception {
		if (useChiSqDist) {		
			msgObj.dispMessage("SOM_ExBMULoader","ctor : thd_idx : "+thdIDX, "Using Chi Sq dist", MsgCodes.info2);
			for (SOM_MapNode tmpMapNode : bmusToExmpl.keySet()) {
				ArrayList<SOM_Example> exs = bmusToExmpl.get(tmpMapNode);
				for(SOM_Example ex : exs) {ex.setTrainingExBMU_ChiSq(tmpMapNode, ftrTypeUsedToTrainIDX);tmpMapNode.addTrainingExToBMUs(ex,typeOfEx);	}
			}		
		} else {		
			msgObj.dispMessage("SOM_ExBMULoader","ctor : thd_idx : "+thdIDX, "Not using Chi Sq dist", MsgCodes.info2);
			for (SOM_MapNode tmpMapNode : bmusToExmpl.keySet()) {
				ArrayList<SOM_Example> exs = bmusToExmpl.get(tmpMapNode);
				for(SOM_Example ex : exs) {ex.setTrainingExBMU(tmpMapNode, ftrTypeUsedToTrainIDX); tmpMapNode.addTrainingExToBMUs(ex,typeOfEx);	}
			}
		}	
		return true;
	}//run	
}//SOMExBMULoader