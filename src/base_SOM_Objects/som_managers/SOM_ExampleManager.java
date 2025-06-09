package base_SOM_Objects.som_managers;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_managers.runners.SOM_MapExDataToBMUs_Runner;
import base_SOM_Objects.som_managers.runners.SOM_SaveExToBMUs_Runner;
import base_SOM_Objects.som_utils.SOM_ProjConfigData;
import base_Utils_Objects.io.file.FileIOManager;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * this class will manage data handling for all examples of a particular type. 
 * Instances of this class are owned by a map manager; 
 * @author john
 */
public abstract class SOM_ExampleManager {
	/**
	 * owning map manager
	 */
	public SOM_MapManager mapMgr;
	/**
	 * ref to mt executor
	 */
	protected ExecutorService th_exec;

	/**
	 * message object for logging and to display to screen
	 */
	protected MessageObject msgObj;
	/**
	 * fileIO manager
	 */
	protected FileIOManager fileIO;
	/**
	 * struct maintaining complete project configuration and information from config files.
	 * all file name data and building needs to be done by this object
	 */
	public static SOM_ProjConfigData projConfigData;	
	/**
	 * short name of example type - this is application-specified, and may not coincide 
	 * with ExDataType 
	 */
	public final String exampleName;
	/**
	 * descriptive name of example type
	 */
	public final String longExampleName;
	/**
	 * a map keyed by example ID of this specific type of examples
	 */
	protected ConcurrentSkipListMap<String, SOM_Example> exampleMap;
	/**
	 * descriptive string of date and time of when the data this mapper manages was created.
	 * this should be saved with data and loaded from files
	 */
	protected String dateAndTimeOfDataCreation;
	/**
	 * per file partition size for pre-processed example csv data - no more than this many records will be s
	 */
	protected final int preProcDatPartSz;

	
	/**
	 * current state of examples
	 */
	private int[] stFlags;
	public static final int
		shouldValidateIDX		= 0,		//whether or not this data should be validated
		dataIsPreProccedIDX 	= 1,		//raw data has been preprocessed
		dataIsLoadedIDX 		= 2,		//preprocessed data has been loaded
		dataFtrsPreparedIDX 	= 3,		//loaded data features have been pre-procced
		dataFtrsCalcedIDX 		= 4,		//features have been calced
		dataPostFtrsBuiltIDX	= 5,		//post feature calc data has been calculated
		exampleArrayBuiltIDX	= 6,		//array of examples to be used by SOM(potentially) built
		saveBMUViaSTIDX 		= 7;		//bmus should be saved in a single thread
	public static final int numFlags = 8;
	
	/**
	 * array of examples actually interacted with by SOM - will be a subset of examples, smaller due to some examples being "bad"
	 */
	protected SOM_Example[] SOMexampleArray;
	/**
	 * # of actual examples used by SOM of this type
	 */
	protected int numSOMExamples;
	/**
	 * constructor for SOM example manager
	 * @param _mapMgr owning map manager
	 * @param _exName descriptive name of data this example manager is handling
	 * @param _longExampleName longer name
	 * @param _flagVals array of flag vals : idx 0 : if should validate or not, idx 1 : if bmus should be saved via single thread or multi-thread mechanism
	 */
	public SOM_ExampleManager(SOM_MapManager _mapMgr, String _exName, String _longExampleName, boolean[] _flagVals) {
		mapMgr = _mapMgr;
		th_exec = mapMgr.getTh_Exec();
		projConfigData = mapMgr.projConfigData;
		exampleName = _exName;
		longExampleName = _longExampleName;
		msgObj = MessageObject.getInstance();
		//fileIO is used to load and save info from/to local files except for the raw data loading, which has its own handling
		fileIO = new FileIOManager(msgObj,"SOM_ExampleManager::"+exampleName);
		preProcDatPartSz = mapMgr.getPreProcDatPartSz();
		exampleMap = new ConcurrentSkipListMap<String, SOM_Example>();
		initFlags();
		setFlag(shouldValidateIDX, _flagVals[0]);
		setFlag(saveBMUViaSTIDX, _flagVals[1]);
		
	}//ctor
	
	//reset the data held by this example manager
	public final void reset() {
		//faster than rebuilding
		exampleMap.clear();
		SOMexampleArray = new SOM_Example[0];
		numSOMExamples = 0;
		//set date and time of reset == date and time of data creation.  will be overwritten by load
		dateAndTimeOfDataCreation = msgObj.getCurrWallTime();
		//instance-specific code
		reset_Priv();
		//flag settings
		clearDataStateFlags();
	}//reset	
	protected abstract void reset_Priv();
	
	/**
	 * clear all flags related to data state - this is called on reset
	 */
	private void clearDataStateFlags() {
		setFlag(dataIsPreProccedIDX, false); 
		setFlag(dataIsLoadedIDX, false); 	
		setFlag(dataFtrsPreparedIDX, false); 
		setFlag(dataFtrsCalcedIDX, false); 	
		setFlag(dataPostFtrsBuiltIDX, false);
		setFlag(exampleArrayBuiltIDX, false);
	}//clearDataStateFlags
	
	///////////////////////////////
	// prepare and calc feature vectors
	
	/**
	 * pre-condition all examples to prepare for building feature vectors, after pre-processed examples are loaded and all data values are aggregated but before any calculations are performed
	 */	
	public final boolean finalizeAllExamples() {
		if((!getFlag(dataIsLoadedIDX)) || (exampleMap.size() == 0)) {
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"finalizeAllExamples","Unable to finalizeAllExamples " + exampleName+ " examples due to them not having been loaded.  Aborting.", MsgCodes.warning1);
			return false;
		} else if (getFlag(dataFtrsPreparedIDX)) {
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"finalizeAllExamples","Data has already been finalized for " + exampleName+ " examples.", MsgCodes.warning1);
			return false;			
		} 
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildFtrVec","Begin finalizing all " +exampleMap.size()+ " " + exampleName+ " examples to prepare them for ftr calc.", MsgCodes.info1);
		//finalize each example - this will aggregate all the ftrs's that are seen in src data and prepare example for calculating ftr vector
		for (SOM_Example ex : exampleMap.values()) {			ex.finalizeBuildBeforeFtrCalc();		}	
		setFlag(dataFtrsPreparedIDX, true);
		setFlag(dataFtrsCalcedIDX, false);
		setFlag(dataPostFtrsBuiltIDX, false);
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildFtrVec","Finished finalizing all " +exampleMap.size()+ " " + exampleName+ " examples to prepare them for ftr calc.", MsgCodes.info1);
		return true;
	}//finalizeAllExamples()

	/**
	 * build feature vectors for all examples this object maps
	 */
	public final boolean buildFeatureVectors() {
		if(!getFlag(dataFtrsPreparedIDX)) {
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildFtrVec","Unable to build feature vectors for " + exampleName+ " examples due to them not having been finalized.  Aborting.", MsgCodes.warning1);
			return false;
		}
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildFtrVec","Begin building feature vectors for " +exampleMap.size()+ " " + exampleName+ " examples.", MsgCodes.info1);
		//instance-specific feature vector building
		buildFtrVec_Priv();		
		setFlag(dataFtrsCalcedIDX, true);
		setFlag(dataPostFtrsBuiltIDX, false);
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildFtrVec","Finished building feature vectors for " +exampleMap.size()+ " " + exampleName+ " examples.", MsgCodes.info1);
		return true;
	}//buildFtrVec	
	/**
	 * code to execute after examples have had ftrs prepared - this calculates feature vectors
	 */
	protected abstract void buildFtrVec_Priv();
	
	/** 
	 * code to execute after every example has had ftr vectors calculated - this will calculate std-ized ftrs, along with other things
	 */
	public final boolean buildAfterAllFtrVecsBuiltStructs() {
		if(!getFlag(dataFtrsCalcedIDX)) {
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildAfterAllFtrVecsBuiltStructs","Unable to execute Post-feature calc process for " + exampleName+ " examples due to them not having had features calculated.  Aborting.", MsgCodes.warning1);
			return false;
		} else if(getFlag(dataPostFtrsBuiltIDX)){
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildAfterAllFtrVecsBuiltStructs","Post-feature calc process for " + exampleName+ " examples already executed.", MsgCodes.warning1);
			return false;
		}
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildAfterAllFtrVecsBuiltStructs","Begin building Post-feature vector data for " +exampleMap.size()+ " " + exampleName+ " examples.", MsgCodes.info1);
		//instance-specific feature vector building - here primarily are the standardized feature vectors built
		buildAfterAllFtrVecsBuiltStructs_Priv();
		setFlag(dataPostFtrsBuiltIDX, true);
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"buildAfterAllFtrVecsBuiltStructs","Finished building Post-feature vector data for " +exampleMap.size()+ " " + exampleName+ " examples.", MsgCodes.info1);
		return true;
	}//buildFtrVec	
	/**
	 * code to execute after examples have had ftrs calculated - this will calculate std features and any alternate ftr mappings if used
	 */
	protected abstract void buildAfterAllFtrVecsBuiltStructs_Priv();
		
	/////////////////////////////////////////////
	// load and save preprocced data
//	public abstract void loadAllPreProccedExampleData(String subDir);
//	public abstract boolean saveAllPreProccedExampleData();
	
	/**
	 * load preprocessed data into examples from disk - must reset SOM maps before this is called
	 */
	public final void loadAllPreProccedExampleData(String subDir) {
		//perform in multiple threads if possible
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"loadAllPreProccedMapData","Loading all " + exampleName+ " example data from : " +subDir, MsgCodes.info5);
		//all data managed by this example mapper needs to be reset
		reset();
		String[] loadSrcFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Load(subDir, exampleName+ "MapSrcData");
		//get number of paritions
		int numPartitions = getNumSrcFilePartitions(loadSrcFNamePrefixAra,subDir);
		//error loading
		if(numPartitions == -1) {return;}
		//load data creation date time, if exists
		loadDataCreateDateTime(subDir);
		
		boolean canMultiThread=mapMgr.isMTCapable();
		if((canMultiThread) && (numPartitions > 1)) {			buildMTLoader(loadSrcFNamePrefixAra, numPartitions);	} 
		else {							buildSTLoader(loadSrcFNamePrefixAra, numPartitions);	}
		setAllDataLoaded();
		setAllDataPreProcced();
		msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"loadAllProductMapData","Finished loading and processing all " + exampleName+ " example data and calculating features.  Number of entries in example map : " + exampleMap.size(), MsgCodes.info5);
	}//loadAllPropsectMapData	
	
	/**
	 * load example data format file holding # of csv files of this kind of prospect data and return value
	 * @param subDir
	 * @return number of source file partitions of this type of preprocessed data
	 */
	protected final int getNumSrcFilePartitions(String[] loadSrcFNamePrefixAra, String subDir) {
		String fmtFile = loadSrcFNamePrefixAra[0]+"_format.csv";
		String[] loadRes = fileIO.loadFileIntoStringAra(fmtFile, exampleName+" Format file loaded", exampleName+" Format File Failed to load");
		
		int numPartitions = 0;
		try {
			numPartitions = Integer.parseInt(loadRes[0].split(" : ")[1].trim());
		} catch (Exception e) {e.printStackTrace(); msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"loadAllPreProccedMapData","Due to error with not finding format file : " + fmtFile+ " no data will be loaded.", MsgCodes.error1); return -1;} 
		return numPartitions;
	}//getNumSrcFilePartitions
	
	/**
	 * multi-threaded and single threaded preproc data loaders
	 * @param loadSrcFNamePrefixAra
	 * @param numPartitions
	 */
	
	protected abstract void buildMTLoader(String[] loadSrcFNamePrefixAra, int numPartitions);
	protected abstract void buildSTLoader(String[] loadSrcFNamePrefixAra, int numPartitions);	
	
	/**
	 * save all pre-processed prospect data
	 * @return
	 */
	public final boolean saveAllPreProccedExampleData() {
		if ((null != exampleMap) && (exampleMap.size() > 0)) {
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Saving all "+exampleName+" map data : " + exampleMap.size() + " examples to save.", MsgCodes.info5);
			//save date/time of data creation
			saveDataCreateDateTime();
			
			String[] saveDestFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Save(exampleName+"MapSrcData");
			ArrayList<String> csvResTmp = new ArrayList<String>();		
			int counter = 0;
			SOM_Example ex1 = exampleMap.get(exampleMap.firstKey());
			String hdrStr = ex1.getRawDescColNamesForCSV();
			csvResTmp.add( hdrStr);
			int nameCounter = 0, numFiles = (1+((int)((exampleMap.size()-1)/preProcDatPartSz)));
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Start Building "+exampleName+" String Array : " +nameCounter + " of "+numFiles+".", MsgCodes.info1);
			for (SOM_Example ex : exampleMap.values()) {			
				csvResTmp.add(ex.getPreProcDescrForCSV());
				++counter;
				if(counter % preProcDatPartSz ==0) {
					String fileName = saveDestFNamePrefixAra[0]+"_"+nameCounter+".csv";
					msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Done Building "+exampleName+" String Array : " +nameCounter + " of "+numFiles+".  Saving to file : "+fileName, MsgCodes.info1);
					//csvRes.add(csvResTmp); 
					fileIO.saveStrings(fileName, csvResTmp);
					csvResTmp = new ArrayList<String>();
					csvResTmp.add( hdrStr);
					counter = 0;
					++nameCounter;
					msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Start Building "+exampleName+" String Array : " +nameCounter + " of "+numFiles+".", MsgCodes.info1);
				}
			}
			//last array if has values
			if(csvResTmp.size() > 1) {	
				String fileName = saveDestFNamePrefixAra[0]+"_"+nameCounter+".csv";
				msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Done Building "+exampleName+" String Array : " +nameCounter + " of "+numFiles+".  Saving to file : "+fileName, MsgCodes.info1);
				//csvRes.add(csvResTmp);
				fileIO.saveStrings(fileName, csvResTmp);
				csvResTmp = new ArrayList<String>();
				++nameCounter;
			}			
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Finished partitioning " + exampleMap.size()+ " "+exampleName+" records into " + nameCounter + " "+exampleName+" record files, each holding up to " + preProcDatPartSz + " records and saving to files.", MsgCodes.info1);
			//save the data in a format file
			String[] data = new String[]{"Number of file partitions for " + saveDestFNamePrefixAra[1] +" data : "+ nameCounter + "\n"};
			fileIO.saveStrings(saveDestFNamePrefixAra[0]+"_format.csv", data);		
			msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","Finished saving all "+exampleName+" map data", MsgCodes.info5);
			return true;
		} else {msgObj.dispMessage("SOM_ExampleManager::"+exampleName,"saveAllExampleMapData","No "+exampleName+" example data to save. Aborting", MsgCodes.error2); return false;}
	}//saveAllPreProccedMapData
	
	/**
	 * load the file where the preprocessed data's time and date of creation is stored - call after reset is called in load
	 * @param fileName preprocced data's datetime file name
	 * @param dataDesc type of preprocced data being loaded
	 */
	private final void loadDateAndTimeOfDataCreation(String fileName, String dataDesc) {
		String[] csvLoadRes = fileIO.loadFileIntoStringAra(fileName, dataDesc + " creation date file loaded.", dataDesc + " creation date file Failed to load");
		//should consist of a single string, comma sep between dataDesc + " creation date/time" and actual creation date time
		//if doesn't exist then use current date time - will be set during reset()
		if(csvLoadRes.length < 1) {return;}
		String[] dateTimeStrAra = csvLoadRes[0].split(",");
		if(dateTimeStrAra.length < 2) {return;}
		dateAndTimeOfDataCreation = dateTimeStrAra[1].trim();
	}
	
	/**
	 * load date and time of data creation, if exists
	 * @param subDir
	 */
	private final void loadDataCreateDateTime(String subDir) {
		String[] loadDateTimeFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Load(subDir, exampleName+ "CreationDateTime");
		String dateTimeFileName = loadDateTimeFNamePrefixAra[0]+".csv";
		loadDateAndTimeOfDataCreation(dateTimeFileName, exampleName);
	}
	
	/**
	 * save date and time of data creation
	 */
	private final void saveDataCreateDateTime() {
		String[] saveDataFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Save(exampleName+"CreationDateTime");
		String dateTimeFileName = saveDataFNamePrefixAra[0]+".csv";
		saveDateAndTimeOfDataCreation(dateTimeFileName, exampleName);
	}
	/**
	 * This exists for very large data sets, to warrant and enable 
	 * loading, mapping and saving bmu mappings per perProcData File, 
	 * as opposed to doing each step across all data
	 * @param subdir subdir location of preproc example data
	 * @param dataType type of data being processed
	 * @param dataMappedIDX index in boolean state flags in map manager denoting whether this data type has been mapped or not
	 * @return
	 */
	public final boolean loadDataMapBMUAndSavePerPreProcFile(String subDir, SOM_ExDataType dataType, int dataMappedIDX) {
		//first load individual file partition
		String[] loadSrcFNamePrefixAra = projConfigData.buildPreProccedDataCSVFNames_Load(subDir, exampleName+ "MapSrcData");
		int numPartitions = getNumSrcFilePartitions(loadSrcFNamePrefixAra,subDir);
		//load each paritition 1 at a time, calc all features for partition, map to bmus and save mappings
		for(int i=0;i<numPartitions;++i) {
			//clear out all data
			reset();
			
			String dataFile = loadSrcFNamePrefixAra[0]+"_"+i+".csv";
			String[] csvLoadRes = fileIO.loadFileIntoStringAra(dataFile, exampleName+ " Data file " + i +" of " +numPartitions +" loaded",  exampleName+ " Data File " + i +" of " +numPartitions +" Failed to load");
			//ignore first entry - header
			for (int j=1;j<csvLoadRes.length; ++j) {
				String str = csvLoadRes[j];
				int pos = str.indexOf(',');
				String oid = str.substring(0, pos);
				SOM_Example ex = buildSingleExample(oid, str);
				exampleMap.put(oid, ex);			
			}
			setAllDataLoaded();
			setAllDataPreProcced();
				//data is loaded here, now finalize before ftr calc
			finalizeAllExamples();
				//now build feature vectors
			buildFeatureVectors();	
				//build post-feature vectors - build STD vectors, build alt calc vec mappings
			buildAfterAllFtrVecsBuiltStructs();
				//build array - gets rid of bad examples (have no ftr vector values at all)
			buildExampleArray();
				//launch a MapTestDataToBMUs_Runner to manage multi-threaded calc
			SOM_MapExDataToBMUs_Runner mapRunner = new SOM_MapExDataToBMUs_Runner(mapMgr, th_exec, SOMexampleArray, exampleName, dataType,dataMappedIDX, false);	
			mapRunner.runMe();
				//build array again to remove any non-BMU-mapped examples (?)
			//buildExampleArray();
			//(SOM_MapManager _mapMgr, ExecutorService _th_exec, SOMExample[] _exData, String _dataTypName, boolean _forceST, String _fileNamePrefix)
			String _fileNamePrefix = projConfigData.getExampleToBMUFileNamePrefix(exampleName)+"_SrcFileIDX_"+String.format("%02d", i);
			SOM_SaveExToBMUs_Runner saveRunner = new SOM_SaveExToBMUs_Runner(mapMgr, th_exec, SOMexampleArray, exampleName, true,  _fileNamePrefix, preProcDatPartSz);
			saveRunner.runMe();	
		}
		return true;
	}//loadDataMapBMUAndSavePerPreProcFile
	
	/**
	 * Save all example -> BMU mappings
	 * @param _approxNumPerPartition approximate # of examples per partition for saving
	 */
	public final boolean saveExampleBMUMappings(int _approxNumPerPartition) {
		//if((!isExampleArrayBuilt()) ||  forceRebuildForSave) {			buildExampleArray();		}			//force rebuilding
		SOM_Example[] exToSaveBMUs = getExToSave();
		String _fileNamePrefix = projConfigData.getExampleToBMUFileNamePrefix(exampleName);
		SOM_SaveExToBMUs_Runner saveRunner = new SOM_SaveExToBMUs_Runner(mapMgr, th_exec, exToSaveBMUs, exampleName, getFlag(saveBMUViaSTIDX),  _fileNamePrefix, _approxNumPerPartition);
		saveRunner.runMe();
		return true;
	}//saveExampleBMUMappings
	/**
	 * return array of examples to save their bmus
	 * @return
	 */
	protected abstract SOM_Example[] getExToSave();
	/**
	 * build a single SOM Example instance using passed OID and csv-formatted data string
	 * @param _oid ID for example
	 * @param _str CSV string of example data
	 * @return correctly cast example
	 */
	protected abstract SOM_Example buildSingleExample(String _oid, String _str);
	
	////////////////////////////////////
	// build SOM arrays
	
	/**
	 * build the array of examples, and return the array - if validate then only include records that are not "bad"
	 * @param validate whether or not this data should be validated (guaranteed to be reasonable training data - only necessary for data that is going to be used to train)
	 * @return
	 */
	public final SOM_Example[] buildExampleArray() {
		boolean validate = getFlag(shouldValidateIDX);
		if(validate) {
			ArrayList<SOM_Example> tmpList = new ArrayList<SOM_Example>();
			for (String key : exampleMap.keySet()) {			validateAndAddExToArray(tmpList, exampleMap.get(key));	}	//potentially different for every instancing class		
			SOMexampleArray = castArray(tmpList);																	//every instancing class will manage different instancing classes of examples - this should provide arrays of the appropriate classes		
		} 
		else {	SOMexampleArray = noValidateBuildExampleArray();}													//every instancing class will manage different classes of examples - this provides array of appropriate class
		buildExampleArrayEnd_Priv(validate);																		//any example-based functionality specific to instancing class to be performed after examples are built
		numSOMExamples = SOMexampleArray.length;
		setFlag(exampleArrayBuiltIDX, true);
		return SOMexampleArray;
	}//buildExampleArray		
	/**
	 * Validate and add preprocessed data example to list that is then used to consume these examples by SOM code.  
	 * Some examples should be added only if they meet certain criteria (i.e. training data must meet certain criteria)
	 * Many example types have no validation.
	 * @param tmpList list to add data to 
	 * @param ex specific example to add to list
	 */
	protected abstract void validateAndAddExToArray(ArrayList<SOM_Example> tmpList, SOM_Example ex);
	/**
	 * Provide array of appropriately cast examples for use as training/testing/validation data
	 * @param tmpList
	 * @return
	 */
	protected abstract SOM_Example[] castArray(ArrayList<SOM_Example> tmpList);
	/**
	 * Build example array without any validation process - just take existing example map and convert to appropriately cast array of examples
	 * @return
	 */
	protected abstract SOM_Example[] noValidateBuildExampleArray();
	/**
	 * Any instancing-class specific code to perform
	 * @param validate whether data should be validated or not (to meet certain criteria for the SOM)
	 */
	protected abstract void buildExampleArrayEnd_Priv(boolean validate);

	
	////////////////////////////////
	// add/remove examples to map
		//reset function acts as map initializer
		//add an example, return old example if one existed
	public final SOM_Example addExampleToMap(SOM_Example ex) {return exampleMap.put(ex.OID, ex);	}
		//remove an example by key
	public final SOM_Example removeExampleFromMap(String key) {return exampleMap.remove(key);}
		//remove an example - use example's OID
	public final SOM_Example removeExampleFromMap(SOM_Example ex) {return exampleMap.remove(ex.OID);}
		//return an example by key 
	public final SOM_Example getExample(String key) {return exampleMap.get(key);}
		//return the entire example map
	public final ConcurrentSkipListMap<String, SOM_Example> getExampleMap(){return exampleMap;}
		//return set of keys in example map
	public Set<String> getExampleKeySet(){return exampleMap.keySet();}
	
	
	public final int getNumSOMExamples() {return numSOMExamples;}	
	public final int getNumMapExamples() {return exampleMap.size();}
	//get date and time of the data this mapper manages' creation
	public final String dateAndTimeOfDataCreation() {return dateAndTimeOfDataCreation;}

	
	/**
	 * save the creation date/time for the preprocced data  
	 * @param fileName file name to save the date/time to
	 * @param dataDesc string describing type of data
	 */
	protected final void saveDateAndTimeOfDataCreation(String fileName, String dataDesc) {
		String[] stringsToWrite = new String[]{""+dataDesc + " creation date/time,"+dateAndTimeOfDataCreation};
		fileIO.saveStrings(fileName, stringsToWrite);	
	}//
		
	////////////////////////
	// state flag handling
	protected final void initFlags() {stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
	public void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}
	public void setFlag(int idx, boolean val) {
		int flIDX = idx/32, mask = 1<<(idx%32);
		stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case shouldValidateIDX		: {break;}	
			case dataIsPreProccedIDX 	: {break;}	
			case dataIsLoadedIDX 		: {break;}	
			case dataFtrsPreparedIDX 	: {break;}	
			case dataFtrsCalcedIDX 		: {break;}	
			case dataPostFtrsBuiltIDX 	: {break;}
			case exampleArrayBuiltIDX	: {break;}
			case saveBMUViaSTIDX		: {break;}
		}
	}//setFlag
	
	public boolean isDataPreProcced() {return getFlag(dataIsPreProccedIDX);}
	public boolean isDataLoaded() {return getFlag(dataIsLoadedIDX);}
	public boolean isDataFtrsPrepared() {return getFlag(dataFtrsPreparedIDX);}
	public boolean isDataFtrsCalced() {return getFlag(dataFtrsCalcedIDX);}
	public boolean isExampleArrayBuilt() {return getFlag(exampleArrayBuiltIDX);}
	
	public void setAllDataLoaded() {setFlag(dataIsLoadedIDX, true);}
	public void setAllDataPreProcced() {setFlag(dataIsPreProccedIDX, true);}


	
	@Override
	public String toString() {
		String res = "Example type name : " + exampleName + " | # of examples : " + exampleMap.size();
		
		return res;
	}
}//class SOM_ExampleManager
