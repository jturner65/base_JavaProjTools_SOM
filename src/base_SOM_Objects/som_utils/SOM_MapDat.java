package base_SOM_Objects.som_utils;

/**
 * This object holds the configuration information for a SOMOCLU-based map to be trained and consumed.  
 * An instance of this class object must be the prime source for all actual map-based configuration and 
 * execution commands - all consumption/access should be subordinate to this object
 * 
 * @author john
 */

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;

public class SOM_MapDat{
	/**
	 * project config object
	 */
	private SOM_ProjConfigData config;
	/**
	 * object to faciliate printing to screen or log file
	 */
	private MessageObject msgObj = null;
	/**
	 * os currently running - use to modify exec string for mac/linux
	 */
	private final String curOS;		
	/**
	 * SOM_MAP execution directory
	 */
	private String execDir;		
	/**
	 * actual string used to execute som program
	 */
	private String execSOMStr;
	/**
	 * Integer command-line arguments for SOM executable
	 */
	private HashMap<String, Integer> somExeArgs_Ints;			// mapCols (x), mapRows (y), mapEpochs, mapKType, mapStRad, mapEndRad;
	/**
	 * Float command-line arguments for SOM executable
	 */
	private HashMap<String, Float> somExeArgs_Floats;			// mapStLrnRate, mapEndLrnRate;
	/**
	 * String command-line arguments for SOM executable
	 */
	private HashMap<String, String> somExeArgs_Strings;			// mapGridShape, mapBounds, mapRadCool, mapNHood, mapLearnCool;	
	
	/**
	 * instance directory within execDir for som data - does not have separator
	 */
	private String SOM_instanceDirName;
	/**
	 * file names used for SOM
	 */
	private String trainDataDenseFN,		//training data file name for dense data, including -relative- path
		trainDataSparseFN,				//for sparse data, including -relative- path
		outFilesPrefix;					//output from map prefix, including -relative- path
	private String trainDataDenseFN_fullPath,		//training data file name for dense data, including -relative- path
		trainDataSparseFN_fullPath,				//for sparse data, including -relative- path
		outFilesPrefix_fullPath;					//output from map prefix, including -relative- path
	private boolean isSparse;
	private String[] execStrAra;			//holds arg list sent to som executable
	private String dbgExecStr;				//string to be executed on command line, built for ease in debugging		
	/**
	 * types of data kernel (config) supported by somoclu-based som executable - describes data format and whether computed via strictly cpu computation or using cuda
	 */
	private static final String[] kernelTypes = new String[] {"Dense CPU","Dense GPU","Sparse CPU"};

	/**
	 * Build a construct to hold logistical information about som executable. This is not ready to be used until given data
	 * @param _config
	 * @param _curOS
	 */
	public SOM_MapDat(SOM_ProjConfigData _config, String _curOS) {
		curOS = _curOS;
		config = _config;
		somExeArgs_Ints = new HashMap<String, Integer>();			// mapCols (x), mapRows (y), mapEpochs, mapKType, mapStRad, mapEndRad;
		somExeArgs_Floats = new HashMap<String, Float>();			// mapStLrnRate, mapEndLrnRate;
		somExeArgs_Strings	= new HashMap<String, String>();		// mapGridShape, mapBounds, mapRadCool, mapNHood, mapLearnCool;	
		msgObj = config.buildMsgObj();
	}//ctor
		
	/**
	 * set current configuration of map
	 * @param _mapInts
	 * @param _mapFloats
	 * @param _mapStrings
	 */
	public void setArgsMapData(HashMap<String, Integer> _mapInts, HashMap<String, Float> _mapFloats, HashMap<String, String> _mapStrings){
		somExeArgs_Ints.clear();
		somExeArgs_Floats.clear();
		somExeArgs_Strings.clear();
		for(String key : _mapInts.keySet()) {somExeArgs_Ints.put(key, _mapInts.get(key));}
		for(String key : _mapFloats.keySet()) {somExeArgs_Floats.put(key, _mapFloats.get(key));}
		for(String key : _mapStrings.keySet()) {somExeArgs_Strings.put(key, _mapStrings.get(key));}
		
		isSparse = (somExeArgs_Ints.get("mapKType") > 1);//0 and 1 are dense cpu/gpu, 2 is sparse cpu
		updateMapDescriptorState();
	}//SOM_MapDat ctor from data	
	
	/**
	 * This sets all the internal references to the SOM directories and initializes the structures used to hold the command line arguments. 
	 * This must be called -every time- any internal map data is changed
	 * 
	 */
	public void updateMapDescriptorState() {
		setAllDirs();
		init();
	}//setAllDirs
	
	/**
	 * set local refs to directories
	 */
	private void setAllDirs() {
		execDir = config.getSOMExec_FullPath();
		execSOMStr = config.getSOM_Map_EXECSTR();
		SOM_instanceDirName = config.getSOMMap_CurrSubDirNoSep();
		
		trainDataDenseFN = config.getSOMMap_lclLRNFileName();
		trainDataSparseFN = config.getSOMMap_lclSVMFileName();
		outFilesPrefix = config.getSOMMap_lclOutFileBase(getOutNameSuffix());	
		setAllFullPathNames();
	}//setAllDirs	
	
	private void setAllFullPathNames() {
		trainDataDenseFN_fullPath = execDir +SOM_instanceDirName + File.separator + trainDataDenseFN;
		trainDataSparseFN_fullPath = execDir +SOM_instanceDirName + File.separator + trainDataSparseFN;
		outFilesPrefix_fullPath = execDir +SOM_instanceDirName + File.separator + outFilesPrefix;		
	}
	
	//after-construction initialization code whenever map values are changed
	private void init() {
		execStrAra = buildExecStrAra();					//build execution string array used by processbuilder
		dbgExecStr = execDir + execSOMStr;
		for(int i=0;i<execStrAra.length;++i) {	dbgExecStr += " "+execStrAra[i];}
	}//init		

	/**
	 * update map descriptor int values
	 * @param key 
	 * @param val
	 */
	public void updateMapDat_Integer(String key, Integer val) {
		Integer oldVal = somExeArgs_Ints.get(key);
		if(oldVal==val) {return;}
		somExeArgs_Ints.put(key, val);
		isSparse = (somExeArgs_Ints.get("mapKType") > 1);//0 and 1 are dense cpu/gpu, 2 is sparse cpu
		init();
	}//updateMapDat_Int
	
	/**
	 * update map descriptor Float values
	 * @param key 
	 * @param val
	 */
	public void updateMapDat_Float(String key, Float val) {
		Float oldVal = somExeArgs_Floats.get(key);
		if(oldVal==val) {return;}
		somExeArgs_Floats.put(key, val);
		init();
	}//updateMapDat_Int
	
	/**
	 * update map descriptor String values
	 * @param key 
	 * @param val
	 */
	public void updateMapDat_String(String key, String val) {
		String oldVal = somExeArgs_Strings.get(key);
		if(oldVal==val) {return;}
		somExeArgs_Strings.put(key, val);
		init();
	}//updateMapDat_Int
	
	
	/**
	 * return output name suffix used for this map's data files
	 * @return
	 */
	public String getOutNameSuffix() {	return "_x"+somExeArgs_Ints.get("mapCols")+"_y"+somExeArgs_Ints.get("mapRows")+"_k"+somExeArgs_Ints.get("mapKType");}	
	
	/**
	 * build an object based on an array of strings read from a file 
	 * @param _descrAra array of strings holding comma sep key-value pairs, grouped by construct, with tags denoting which construct
	 */
	public void buildFromStringArray(String[] _descrAra) {		
		somExeArgs_Ints = new HashMap<String, Integer>();			// mapCols (x), mapRows (y), mapEpochs, mapKType, mapStRad, mapEndRad;
		somExeArgs_Floats = new HashMap<String, Float>();			// mapStLrnRate, mapEndLrnRate;
		somExeArgs_Strings	= new HashMap<String, String>();			// mapGridShape, mapBounds, mapRadCool, mapNHood, mapLearnCool;	
		HashMap<String, String> tmpVars = new HashMap<String, String>();
		int idx = 0;
		boolean foundDataPartition = false;
		int numLines = _descrAra.length;
		while ((!foundDataPartition) && (idx < numLines)) {
			if(_descrAra[idx].contains(SOM_ProjConfigData.fileComment + " Base Vars")) {foundDataPartition=true;}
			++idx;		
		}
		if(idx == numLines) {msgObj.dispMessage("SOM_MapDat","buildFromStringArray","Array of description information not correct format to build SOM_MapDat object.  Aborting.",  MsgCodes.error2);	return;	}
		//use ara to pass index via ptr
		int[] idxAra = new int[] {idx};
		//read in method vars
		tmpVars = _readArrayIntoStringMap(idxAra, numLines, SOM_ProjConfigData.fileComment, _descrAra);// somExeArgs_Ints descriptors", _descrAra);
		if (tmpVars == null) {return;}
		//need to set these from application call/config?
		//execDir = tmpVars.get("execDir").trim();	//should always be retrieved from current execution environment
		execDir = config.getSOMExec_FullPath();
		execSOMStr = config.getSOM_Map_EXECSTR();
		SOM_instanceDirName = tmpVars.get("SOM_instanceDirName").trim();
		trainDataDenseFN = tmpVars.get("trainDataDenseFN").trim();
		trainDataSparseFN = tmpVars.get("trainDataSparseFN").trim();
		outFilesPrefix = tmpVars.get("outFilesPrefix").trim();
		setAllFullPathNames();
		
		isSparse = (tmpVars.get("isSparse").trim().toLowerCase().contains("true") ? true : false);
		//integer SOM cmnd line args
		tmpVars = _readArrayIntoStringMap(idxAra, numLines, SOM_ProjConfigData.fileComment, _descrAra);// somExeArgs_Floats descriptors", _descrAra);
		if (tmpVars == null) {return;}
		for(String key : tmpVars.keySet()) {somExeArgs_Ints.put(key, Integer.parseInt(tmpVars.get(key).trim()));}
		//float SOM Cmnd Line Args
		tmpVars = _readArrayIntoStringMap(idxAra, numLines, SOM_ProjConfigData.fileComment, _descrAra);// somExeArgs_Strings descriptors", _descrAra);
		if (tmpVars == null) {return;}
		for(String key : tmpVars.keySet()) {somExeArgs_Floats.put(key, Float.parseFloat(tmpVars.get(key).trim()));}		
		//String SOM Cmnd Line Args
		tmpVars = _readArrayIntoStringMap(idxAra, numLines, SOM_ProjConfigData.fileComment, _descrAra);// End Descriptor Data", _descrAra);
		if (tmpVars == null) {return;}
		for(String key : tmpVars.keySet()) {somExeArgs_Strings.put(key, tmpVars.get(key).trim());}
		init();
	}//SOM_MapDat ctor from string ara 	
	
	/**
	 * read string array into map of string-string key-value pairs.  idx passed as reference (in array)
	 * @param idx
	 * @param numLines
	 * @param _partitionStr
	 * @param _descrAra
	 * @return
	 */
	private HashMap<String, String> _readArrayIntoStringMap(int[] idx, int numLines, String _partitionStr, String[] _descrAra){
		HashMap<String, String> tmpVars = new HashMap<String, String>();
		boolean foundDataPartition = false;
		//load base vars here
		while ((!foundDataPartition) && (idx[0] < numLines)) {
			String desc = _descrAra[idx[0]];
			if(desc.contains(_partitionStr)) {foundDataPartition=true; ++idx[0]; continue;}
			String[] dat = desc.trim().split(",");
			//System.out.println("IDX : " + idx[0] + " == "+  desc +"  | Split : "+ dat[0] +" | " + dat[1]);
			tmpVars.put(dat[0], dat[1]);
			++idx[0];	
		}	
		if(!foundDataPartition) {msgObj.dispMessage("SOM_MapDat","buildFromStringArray","Array of description information not correct format to build SOM_MapDat object - failed finding partition bound : " +_partitionStr + ".  Aborting.", MsgCodes.error2);	return tmpVars;}
		return tmpVars;
	}//read array into map of strings, to be processed into object variables

	/**
	 * return string array describing this SOM map execution in csv format so can be saved to a file - group each construct by string title
	 * @return
	 */
	public ArrayList<String> buildStringDescAra() {
		ArrayList<String> res = new ArrayList<String>();
		res.add(SOM_ProjConfigData.fileComment + " This file holds description of SOM map experiment execution settings");
		res.add(SOM_ProjConfigData.fileComment + " It should be used to build a SOM_MapDat object which then is consumed to control the execution of the SOM.");
		res.add(SOM_ProjConfigData.fileComment + " Base Vars");
		//res.add("execDir,"+execDir); //should always be retrieved from current execution environment
		//res.add("execSOMStr,"+execSOMStr); //should always be retrieved from current execution environment
		res.add("isSparse,"+isSparse);
		res.add("SOM_instanceDirName,"+SOM_instanceDirName);
		res.add("trainDataDenseFN,"+trainDataDenseFN);
		res.add("trainDataSparseFN,"+trainDataSparseFN);
		res.add("outFilesPrefix,"+outFilesPrefix);
		res.add(SOM_ProjConfigData.fileComment + " somExeArgs_Ints descriptors");
		for (String key : somExeArgs_Ints.keySet()) {res.add(""+key+","+somExeArgs_Ints.get(key));}
		res.add(SOM_ProjConfigData.fileComment + " somExeArgs_Floats descriptors");
		for (String key : somExeArgs_Floats.keySet()) {res.add(""+key+","+String.format("%.6f", somExeArgs_Floats.get(key)));}
		res.add(SOM_ProjConfigData.fileComment + " somExeArgs_Strings descriptors");
		for (String key : somExeArgs_Strings.keySet()) {res.add(""+key+","+somExeArgs_Strings.get(key));}
		res.add(SOM_ProjConfigData.fileComment + " End Descriptor Data");		
		return res;		
	}//buildStringDescAra	
	
	/**
	 * build execution string for SOM_MAP - should always be even in length
	 * @return
	 */
	private String[] buildExecStrAra(){
		String useQts = (curOS.toLowerCase().contains("win") ? "\"" : "");
		String[] res;
		res = new String[]{//execDir, execSOMStr,
		"-k",""+somExeArgs_Ints.get("mapKType"),"-x",""+somExeArgs_Ints.get("mapCols"),"-y",""+somExeArgs_Ints.get("mapRows"), "-e",""+somExeArgs_Ints.get("mapEpochs"),"-r",""+somExeArgs_Ints.get("mapStRad"),"-R",""+somExeArgs_Ints.get("mapEndRad"),
		"-l",""+String.format("%.4f",somExeArgs_Floats.get("mapStLrnRate")),"-L",""+String.format("%.4f",somExeArgs_Floats.get("mapEndLrnRate")), 
		"-m",""+somExeArgs_Strings.get("mapBounds"),"-g",""+somExeArgs_Strings.get("mapGridShape"),"-n",""+somExeArgs_Strings.get("mapNHood"), "-T",""+somExeArgs_Strings.get("mapLearnCool"), 
		"-v", "2",
		"-t",""+somExeArgs_Strings.get("mapRadCool"), useQts +(isSparse ? trainDataSparseFN_fullPath : trainDataDenseFN_fullPath) + useQts , useQts + outFilesPrefix_fullPath +  useQts};
		return res;		
	}//execString
	
	public HashMap<String, Integer> getMapInts(){return somExeArgs_Ints;}
	public HashMap<String, Float> getMapFloats(){return somExeArgs_Floats;}
	public HashMap<String, String> getMapStrings(){return somExeArgs_Strings;}
	/**
	 * return execution string array used by processbuilder
	 * @return
	 */
	public String[] getExecStrAra(){		return execStrAra;	}
	/**
	 * return debug execution string array used by processbuilder
	 * @return
	 */
	public String getDbgExecStr() {			return dbgExecStr;}
	/**
	 * get working directory
	 * @return
	 */
	public String getExeWorkingDir() {		return execDir;}
	/**
	 * get executable invocation
	 * @return
	 */
	public String getExename() {		return execSOMStr;}
		
	public boolean isToroidal(){return (somExeArgs_Strings.get("mapBounds").equals("toroid"));}
	
	//purely descriptive
	@Override
	public String toString(){
		String res = "Map config : SOM_MAP Dir : " + execDir +"\n";
		res += "Data Format and Computation Method(k) : "+kernelTypes[somExeArgs_Ints.get("mapKType")] + "\t#Cols : " + somExeArgs_Ints.get("mapCols") + "\t#Rows : " + somExeArgs_Ints.get("mapRows") + "\t#Epochs : " + somExeArgs_Ints.get("mapEpochs") + "\tStart Radius : " +somExeArgs_Ints.get("mapStRad") + "\tEnd Radius : " + +somExeArgs_Ints.get("mapEndRad")+"\n";
		res += "Start Learning Rate : " + String.format("%.4f",somExeArgs_Floats.get("mapStLrnRate"))+"\tEnd Learning Rate : " + String.format("%.4f",somExeArgs_Floats.get("mapEndLrnRate"))+"\n";
		res += "Boundaries : "+somExeArgs_Strings.get("mapBounds") + "\tGrid Shape : "+somExeArgs_Strings.get("mapGridShape")+"\tNeighborhood Function : " + somExeArgs_Strings.get("mapNHood") + "\nLearning Cooling: " + somExeArgs_Strings.get("mapLearnCool") + "\tRadius Cooling : "+ somExeArgs_Strings.get("mapRadCool")+"\n";		
		res += "Training data (full path) : "+(isSparse ? ".svm (Sparse) file name : " + trainDataSparseFN_fullPath :  ".lrn (dense) file name : " + trainDataDenseFN_fullPath) + "\nOutput files prefix (full path) : " + outFilesPrefix_fullPath +"\n";
		res += "Training data : "+(isSparse ? ".svm (Sparse) file name : " + trainDataSparseFN :  ".lrn (dense) file name : " + trainDataDenseFN) + "\nOutput files prefix : " + outFilesPrefix +"\n";
		res +="\n dbgString of exec string : "+dbgExecStr+"\n";
		return res;
	}
	
}//SOM_MapDat
