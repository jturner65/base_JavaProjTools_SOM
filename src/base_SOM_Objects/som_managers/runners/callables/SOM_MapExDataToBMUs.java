package base_SOM_Objects.som_managers.runners.callables;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.function.BiFunction;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_managers.runners.callables.base.SOM_MapDataToBMUs;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_Utils_Objects.io.messaging.MsgCodes;

public class SOM_MapExDataToBMUs extends SOM_MapDataToBMUs{
	protected SOM_Example[] exs;
	
	public SOM_MapExDataToBMUs(SOM_MapManager _mapMgr, int _stProdIDX, int _endProdIDX, SOM_Example[] _exs, int _thdIDX, String _type, boolean _useChiSqDist) {
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