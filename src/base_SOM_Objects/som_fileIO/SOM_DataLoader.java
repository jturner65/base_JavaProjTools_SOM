package base_SOM_Objects.som_fileIO;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import base_SOM_Objects.*;
import base_SOM_Objects.som_examples.*;
import base_SOM_Objects.som_utils.SOMProjConfigData;
import base_Utils_Objects.*;
import base_Utils_Objects.io.FileIOManager;
import base_Utils_Objects.io.MessageObject;
import base_Utils_Objects.io.MsgCodes;
import base_Utils_Objects.vectorObjs.Tuple;

/**
 * This class describes the hierarchy of functionality required for analysing a trained SOM's results
 * Will Process results from SOM execution
 * @author john
 */
public class SOM_DataLoader{
	public SOM_MapManager mapMgr;				//the map these files will use
	//object that manages message displays on screen
	private MessageObject msgObj;
	//manage IO in this object
	private FileIOManager fileIO;

	public SOMProjConfigData projConfigData;			//struct maintaining configuration information for entire project
	
	public final static float nodeDistThresh = 100000.0f;
	
	//type of data used to train - 0 : unmodded, 1:std'ized, 2:normalized 
	public int ftrTypeUsedToTrain;
	public boolean useChiSqDist;
		
	public SOM_DataLoader(SOM_MapManager _mapMgr, SOMProjConfigData _configData) {
		mapMgr = _mapMgr; 
		msgObj = mapMgr.buildMsgObj();
		fileIO = new FileIOManager(msgObj,"SOMDataLoader");
		projConfigData = _configData;
	}
	
	/**
	 * Don't make this asynch
	 * @return
	 */
	public Boolean callMe(){
		msgObj.dispMessage("SOMDataLoader","run","Starting Trained SOM map node data loader", MsgCodes.info5);	
			//load results from map processing - fnames needs to be modified to handle this
		ftrTypeUsedToTrain = mapMgr.getCurrentTrainDataFormat();
			//whether chi-sq dist or regular l2 dist should be used
		useChiSqDist = mapMgr.getUseChiSqDist();
			//load map weights for all map nodes
		boolean success = loadSOMWts();	
		if(success) {msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading SOM Weights successfully completed.", MsgCodes.info1);}
		else		{msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading SOM Weights failed.  Aborting further processing.", MsgCodes.error2);	return false;}
			//set u-matrix for all map nodes
		success = loadSOM_UMatrixDists();		
		if(success) {msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading UMatrix distances successfully completed.", MsgCodes.info1);}
		else		{msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading UMatrix distances failed.  Aborting further processing.", MsgCodes.error2);	return false;}
			//load mins and diffs of data used to train map
		success = mapMgr.loadDiffsMins();	
		if(success) {msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading Diffs/Mins of training data successfully completed.", MsgCodes.info1);}
		else		{msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading Diffs/Mins of training data failed.  Aborting further processing.", MsgCodes.error2);	return false;}		
			//load SOM's best matching units for training data - must be after map wts and training data has been loaded
		success = loadSOM_BMUs();
		if(success) {
			msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading BMUs for training data successfully completed.", MsgCodes.info1);
			mapMgr.setAllBMUsFromMap();
		} else {
			msgObj.dispMessage("SOMDataLoader", "execDataLoad", "Loading BMUs for training data failed.  Unable to match products to map nodes since BMU loading failed.  Aborting further processing.", MsgCodes.error2);	
			return false;
		}		
			//finish up 
		mapMgr.setSOMMapNodeDataIsLoaded(true);
		mapMgr.setLoaderRTN(true);
		mapMgr.setMapImgClrs();
		mapMgr.resetButtonState();
		msgObj.dispMessage("SOMDataLoader","run","Finished data loader", MsgCodes.info5);	
		return true;
	}//run

	//return file name from file name and path name
	protected String getFName(String fNameAndPath){
		File file = new File(fNameAndPath);
		String simpleFileName = file.getName();
		return simpleFileName;
	}
		
/////source independent file loading
	//verify file map dimensions agree
	private boolean checkMapDim(String[] tkns, String errorMSG){
		int tmapY = Integer.parseInt(tkns[0]), tmapX = Integer.parseInt(tkns[1]);
		if((tmapY != mapMgr.getMapNodeRows()) || (tmapX != mapMgr.getMapNodeCols())) { 
			msgObj.dispMessage("SOMDataLoader","checkMapDim","!!"+ errorMSG + " dimensions : " + tmapX +","+tmapY+" do not match dimensions of learned weights " + mapMgr.getMapNodeCols() +","+mapMgr.getMapNodeRows()+". Loading aborted.", MsgCodes.error2); 
			return false;} 
		return true;		
	}//checkMapDim
	
	//load map wts from file built by SOM_MAP - need to know format of original data used to train map	
	//Map nodes are similar in format to training examples but scaled based on -their own- data
	//consider actual map data to be feature data, scale map nodes based on min/max feature data seen in wts file
	private boolean loadSOMWts(){//builds mapnodes structure - each map node's weights 
		String wtsFileName = projConfigData.getSOMResFName(projConfigData.wtsIDX);
		if(wtsFileName.length() < 1){msgObj.dispMessage("SOMDataLoader","loadSOMWts","getSOMResFName call failed for wts : "+wtsFileName, MsgCodes.info5);return false;}
		msgObj.dispMessage("SOMDataLoader","loadSOMWts","Starting Loading SOM weight data from file : " + getFName(wtsFileName), MsgCodes.info5 );
		mapMgr.initMapNodes();
		mapMgr.clearBMUNodesWithNoExs(SOM_ExDataType.Training);//clear structures holding map nodes with and without training examples
		if(wtsFileName.length() < 1){return false;}
		String [] strs= fileIO.loadFileIntoStringAra(wtsFileName, "Loaded wts data file : "+wtsFileName, "Error wts reading file : "+wtsFileName);
		if(strs==null){return false;}
		String[] tkns,ftrNames;
		SOM_MapNode dpt;	
		int numEx = 0, mapX=1, mapY=1;//,numWtData = 0;
		Tuple<Integer,Integer> mapLoc;
		//# of training features in each map node
		int numTrainFtrs = 0; 
		
		float[] tmpMapMaxs = null;
		for (int i=0;i<strs.length;++i){//load in data 
			if(i < 2){//first 2 lines are map description : line 0 is map row/col count; map 2 is # ftrs
				tkns = strs[i].replace('%', ' ').trim().split(mapMgr.SOM_FileToken);
				//set map size in nodes
				if(i==0){mapY = Integer.parseInt(tkns[0]);mapMgr.setMapNumRows(mapY);mapX = Integer.parseInt(tkns[1]);mapMgr.setMapNumCols(mapX);	} 
				else {	//# ftrs in map
					numTrainFtrs = Integer.parseInt(tkns[0]);
					ftrNames = new String[numTrainFtrs];
					for(int j=0;j<ftrNames.length;++j){ftrNames[j]=""+j;}			//build temporary names for each feature idx in feature vector					
					//mapMgr.dataHdr = new dataDesc(mapMgr, ftrNames);				//assign numbers to feature name data header 
					tmpMapMaxs = mapMgr.initMapMgrMeanMinVar(ftrNames.length);
				}	
				continue;
			}//if first 2 lines in wts file
			tkns = strs[i].split(mapMgr.SOM_FileToken);
			if(tkns.length < 2){continue;}
			mapLoc = new Tuple<Integer, Integer>((i-2)%mapX, (i-2)/mapX);//map locations in som data are increasing in x first, then y (row major)
			dpt = mapMgr.buildMapNode(mapLoc, tkns);//give each map node its features		
			++numEx;
			mapMgr.addToMapNodes(mapLoc, dpt, tmpMapMaxs, numTrainFtrs);			
		}
		//make sure both unmoddified features and std'ized features are built before determining map mean/var
		mapMgr.finalizeMapNodes(tmpMapMaxs, numTrainFtrs, numEx);		
		msgObj.dispMessage("SOMDataLoader","loadSOMWts","Finished Loading SOM weight data from file : " + getFName(wtsFileName), MsgCodes.info5 );		
		return true;
	}//loadSOMWts	
	
	private void dbgDispFtrAra(float[] ftrData, String exStr) {
		msgObj.dispMessage("SOMDataLoader","dbgDispFtrAra",ftrData.length + " " +exStr + " vals : ", MsgCodes.warning1 );	
		String res = ""+String.format("%.4f",ftrData[0]);
		for(int d=1;d<ftrData.length;++d) {res += ", " + String.format("%.4f", ftrData[d]);		}
		res +="\n";
		msgObj.dispMessage("SOMDataLoader","dbgDispFtrAra",res, MsgCodes.warning1);
	}//dbgDispFtrAra	
	
	//load the u-matrix data used to build the node distance visualization
	private boolean loadSOM_UMatrixDists() {
		String uMtxBMUFname =  projConfigData.getSOMResFName(projConfigData.umtxIDX);
		if(uMtxBMUFname.length() < 1){msgObj.dispMessage("SOMDataLoader","loadSOM_UMatrixDists","getSOMResFName call failed for umatrix file : "+uMtxBMUFname, MsgCodes.info5);return false;}
		msgObj.dispMessage("SOMDataLoader","loadSOM_UMatrixDists","Start Loading U-Matrix File : "+uMtxBMUFname, MsgCodes.info5);
		if(uMtxBMUFname.length() < 1){return false;}
		String [] strs= fileIO.loadFileIntoStringAra(uMtxBMUFname, "Loaded U Matrix data file : "+uMtxBMUFname, "Error reading U Matrix data file : "+uMtxBMUFname);
		if(strs==null){return false;}
		int numEx = 0, mapX=1, mapY=1,numWtData = 0;
		String[] tkns;
		Tuple<Integer, Integer> mapLoc;
		SOM_MapNode dpt;
		int row;
		for (int i=0;i<strs.length;++i){//load in data 
			if(i < 1){//line 0 is map row/col count
				tkns = strs[i].replace('%', ' ').trim().split(mapMgr.SOM_FileToken);
				//set map size in nodes
				mapY = Integer.parseInt(tkns[0]);
				mapX = Integer.parseInt(tkns[1]);	
				//TODO compare values here to set values
				continue;
			}//if first 2 lines in wts file
			tkns = strs[i].trim().split(mapMgr.SOM_FileToken);
			//System.out.println("String : ---"+strs[i]+"---- has length : "+ tkns.length);
			if(tkns.length < 2){continue;}
			row = i-1;
			for (int col=0;col<tkns.length;++col) {
				mapLoc = new Tuple<Integer, Integer>(col, row);//map locations in som data are increasing in x first, then y (row major)
				dpt = mapMgr.getMapNodeByCoords(mapLoc);//mapMgr.MapNodes.get(mapLoc);//give each map node its features
				dpt.setUMatDist(Float.parseFloat(tkns[col].trim()));
			}	
		}//
		//update each map node's neighborhood member's UMatrix weight values
		mapMgr.buildAllMapNodeNeighborhood_Dists();//for(SOMMapNode ex : mapMgr.MapNodes.values()) {	ex.buildNeighborWtVals();	}
		//calculate segments of nodes
		mapMgr.buildUMatrixSegmentsOnMap();
		msgObj.dispMessage("SOMDataLoader","loadSOM_UMatrixDists","Finished loading and processing U-Matrix File : "+uMtxBMUFname, MsgCodes.info5);		
		return true;
	}//loadSOM_UMatrixDists
	
	//verify the best matching units file is as we expect it to be
	private boolean checkBMUHeader(String[] hdrStrAra, String bmFileName) {
		String[] tkns = hdrStrAra[0].replace('%', ' ').trim().split(mapMgr.SOM_FileToken);
		if(!checkMapDim(tkns,"Best Matching Units file " + getFName(bmFileName))){return false;}//otw continue
		tkns = hdrStrAra[1].replace('%', ' ').trim().split(mapMgr.SOM_FileToken);
		int tNumTDat = Integer.parseInt(tkns[0]);
		if(tNumTDat != mapMgr.numTrainData) { //don't forget added emtpy vector
			msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","!!Best Matching Units file " + getFName(bmFileName) + " # of training examples : " + tNumTDat +" does not match # of training examples in training data " + mapMgr.numTrainData+". Loading aborted.", MsgCodes.error2 ); 
			return false;}					
		return true;
	}//checkBMUHeader
	
	//load best matching units for each training example - has values : idx, mapy, mapx.  Uses file built by som code.  can be verified by comparing actual example distance from each node
	private boolean loadSOM_BMUs(){//modifies existing nodes and datapoints only
		String bmFileName = projConfigData.getSOMResFName(projConfigData.bmuIDX);
		if(bmFileName.length() < 1){msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","getSOMResFName call failed : "+bmFileName, MsgCodes.info5);return false;}
		//clear out listing of bmus that have training examples already
		mapMgr.clearBMUNodesWithExs(SOM_ExDataType.Training);
		msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","Start Loading BMU File : "+bmFileName, MsgCodes.info5);
		String[] tkns;			
		String[] strs= fileIO.loadFileIntoStringAra(bmFileName, "Loaded best matching unit data file : "+bmFileName, "Error reading best matching unit file : "+bmFileName);			
		if((strs==null) || (strs.length == 0)){return false;}
		if (! checkBMUHeader(strs, bmFileName)) {return false;}
		int numThds =  mapMgr.getNumUsableThreads();
		int bmuListIDX = 0;
		int numMapCols = mapMgr.getMapNodeCols();
		//build array of maps keyed by map node, values being arrays of training examples that consider map node key to be their BMU
		HashMap<SOM_MapNode, ArrayList<SOM_Example>>[] bmusToExs = new HashMap[numThds];
		for (int i=0;i<numThds;++i) {
			bmusToExs[i] = new HashMap<SOM_MapNode, ArrayList<SOM_Example>>();
		}
		int mapNodeX, mapNodeY, dpIdx;
		SOM_Example[] _trainData = mapMgr.getTrainingData();
		//this should always be training data
		int typeOfData = _trainData[0].getTypeVal();
		for (int i=2;i<strs.length;++i){//load in data on all bmu's
			tkns = strs[i].split(mapMgr.SOM_FileToken);
			if(tkns.length < 2){continue;}//shouldn't happen	
			mapNodeX = Integer.parseInt(tkns[2]);
			mapNodeY = Integer.parseInt(tkns[1]);
			dpIdx = Integer.parseInt(tkns[0]);	//datapoint index in training data	
			
			//map locations in bmu data are in (y,x) order (row major)
			Tuple<Integer,Integer> mapLoc = new Tuple<Integer, Integer>(mapNodeX,mapNodeY);
			SOM_MapNode tmpMapNode = mapMgr.getMapNodeByCoords(mapLoc);
				//should never happen, if map was built with specified configuration - this would mean trying to load data from different maps
			if(null==tmpMapNode){ msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","!!!!!!!!!Map node stated as best matching unit for training example " + tkns[0] + " not found in map ... somehow. ", MsgCodes.error2); return false;}//catastrophic error shouldn't be possible
			
			//get data point at dpIdx - this is node for which tmpMapNode is BMU
			SOM_Example tmpDataPt = _trainData[dpIdx];
				//should never happen, if map was built with this data 
			if(null==tmpDataPt){ msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","!!Training Datapoint given by idx in BMU file str tok : " + tkns[0] + " of string : --" + strs[i] + "-- not found in training data. ", MsgCodes.error2); return false;}//catastrophic error shouldn't happen
			
			//this is for spot verification - check if 0 0 bmus match what is expected, and look like each other
			//if((mapNodeX==0) &&(mapNodeY==0)) {				msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","------------- > Map Node 0 0 is BMU for data point IDX : " + dpIdx + " : "+ tmpDataPt.toSVMString(ftrTypeUsedToTrain), MsgCodes.info3);}
			
			//for multi-threading : partition bmu and its subsequent child examples to a different list depending on location of bmu
			//will always result in any particular map node only appearing within a single partition
			bmuListIDX = ((mapNodeX * numMapCols) + mapNodeY) % numThds;
			ArrayList<SOM_Example> bmuExs = bmusToExs[bmuListIDX].get(tmpMapNode);
			if(bmuExs == null) {bmuExs = new ArrayList<SOM_Example>(); bmusToExs[bmuListIDX].put(tmpMapNode, bmuExs);}
			bmuExs.add(tmpDataPt);				
			//debug to verify node row/col order
			//dbgVerifyBMUs(tmpMapNode, tmpDataPt,Integer.parseInt(tkns[1]) ,Integer.parseInt(tkns[2]));
			mapMgr.addExToNodesWithExs(tmpMapNode, SOM_ExDataType.Training);
			//mapMgr.nodesWithNoEx.remove(tmpMapNode);
			//msgObj.dispMessage("DataLoader : Tuple "  + mapLoc + " from str @ i-2 = " + (i-2) + " node : " + tmpMapNode.toString());
		}//for each training data point			
		msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","Built bmus map, now calc dists for examples", MsgCodes.info1);
		boolean doMT = mapMgr.isMTCapable();
		//mapping bmus to training examples
		mapMgr.setTrainDataBMUsRdyToSave(false);
		if (doMT) {
			List<Future<Boolean>> bmuDataBldFtrs;
			List<SOM_ExBMULoader> bmuDataLoaders = new ArrayList<SOM_ExBMULoader>();
			////////////////////
			for(int i=0;i<numThds;++i) {bmuDataLoaders.add(new SOM_ExBMULoader(mapMgr.buildMsgObj(),ftrTypeUsedToTrain,useChiSqDist, typeOfData, bmusToExs[i],i));	}
			try {bmuDataBldFtrs = mapMgr.getTh_Exec().invokeAll(bmuDataLoaders);for(Future<Boolean> f: bmuDataBldFtrs) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
		} else {		
			//single threaded version of above - do not use SOMExBMULoader since we have already partitioned structure holding bmus bmusToExs and may not hold only 1 hashmap
			if (useChiSqDist) {
				for (HashMap<SOM_MapNode, ArrayList<SOM_Example>> bmuToExsMap : bmusToExs) {
					for (SOM_MapNode tmpMapNode : bmuToExsMap.keySet()) {
						ArrayList<SOM_Example> exs = bmuToExsMap.get(tmpMapNode);
						for(SOM_Example ex : exs) {ex.setTrainingExBMU_ChiSq(tmpMapNode, ftrTypeUsedToTrain);tmpMapNode.addTrainingExToBMUs(ex,typeOfData);	}
					}
				}				
			} else {
				for (HashMap<SOM_MapNode, ArrayList<SOM_Example>> bmuToExsMap : bmusToExs) {
					for (SOM_MapNode tmpMapNode : bmuToExsMap.keySet()) {
						ArrayList<SOM_Example> exs = bmuToExsMap.get(tmpMapNode);
						for(SOM_Example ex : exs) {ex.setTrainingExBMU(tmpMapNode, ftrTypeUsedToTrain);tmpMapNode.addTrainingExToBMUs(ex,typeOfData);	}
					}
				}				
			}
		}//if mt else single thd
		mapMgr.setTrainDataBMUsRdyToSave(true);
		msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","Start Pruning No-Example list", MsgCodes.info5);
		
		mapMgr._finalizeBMUProcessing(SOM_ExDataType.Training);
		
		msgObj.dispMessage("SOMDataLoader","loadSOM_BMUs","Finished Loading SOM BMUs from file : " + getFName(bmFileName) + "| Found "+mapMgr.getNumNodesWithBMUExs(SOM_ExDataType.Training)+" nodes with training example mappings.", MsgCodes.info5);
		return true;
	}//loadSOM_BMs
	
	
}//SOM_DataLoader

