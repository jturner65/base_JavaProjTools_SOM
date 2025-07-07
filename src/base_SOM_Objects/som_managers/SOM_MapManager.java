package base_SOM_Objects.som_managers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.tuples.Tuple;
import base_Render_Interface.IRenderInterface;
import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_fileIO.SOM_DataLoader;
import base_SOM_Objects.som_managers.runners.SOM_CalcExFtrs_Runner;
import base_SOM_Objects.som_managers.runners.SOM_MapExDataToBMUs_Runner;
import base_SOM_Objects.som_managers.runners.SOM_SaveExToBMUs_Runner;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_SOM_Objects.som_segments.segments.SOM_CategorySegment;
import base_SOM_Objects.som_segments.segments.SOM_ClassSegment;
import base_SOM_Objects.som_segments.segments.SOM_FtrWtSegment;
import base_SOM_Objects.som_segments.segments.SOM_MappedSegment;
import base_SOM_Objects.som_segments.segments.SOM_UMatrixSegment;
import base_SOM_Objects.som_ui.SOM_FtrMapVisImgBldr;
import base_SOM_Objects.som_ui.SOM_MseOvrDisplay;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MapUIWin;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MseOvrDispTypeVals;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_UIToMapCom;
import base_SOM_Objects.som_utils.SOM_MapDat;
import base_SOM_Objects.som_utils.SOM_ProjConfigData;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.renderer.ProcessingRenderer;
//import base_UI_Objects.IRenderInterface;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Utils_Objects.appManager.Java_AppManager;
import base_Utils_Objects.io.file.FileIOManager;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Utils_Objects.threading.myProcConsoleMsgMgr;
import base_Utils_Objects.threading.myProcessManager;
import processing.core.PConstants;
import processing.core.PImage;

public abstract class SOM_MapManager {
    
    /**
     * name to reference this map manager
     */
    protected String name;
    protected int ID;
    protected static int cnt = 0;
    /**
     * owning window - null if console
     */
    public SOM_MapUIWin win;
    /**
     * Gui Application Manager - null if console
     */
    public static GUI_AppManager AppMgr;
    /**
     * manage IO in this object
     */
    protected FileIOManager fileIO; 
    /**
     * struct maintaining complete project configuration and information from config files - all file name data and building needs to be done by this object
     */
    public SOM_ProjConfigData projConfigData;    
    /**
     * object to manage messages for display and potentially logging
     */
    private MessageObject msgObj;
    /**
     * object to manage interface with a UI, to make sure map data stays synchronized
     */
    public SOM_UIToMapCom mapUIAPI;
    
    //////////////////////////////
    //map descriptors    
    /**
     * all nodes of som map, keyed by node location as tuple of row/col coordinates
     */
    protected TreeMap<Tuple<Integer,Integer>, SOM_MapNode> MapNodes;
    /**
     * all nodes of current som map that have been selected by the user, keyed by node location as tuple of row/col coordinates
     */
    protected TreeMap<Tuple<Integer,Integer>, SOM_MapNode> SelectedMapNodes;    
    
    /**
     * map of all map nodes, keyed by population (# of mapped examples), value is map node map loc
     */
    protected TreeMap<Integer, ArrayList<Tuple<Integer,Integer>>>[] MapNodesByPopulation;    
    /**
     * map of ftr idx and all map nodes that have non-zero presence in that ftr
     */
    protected TreeMap<Integer, HashSet<SOM_MapNode>> MapNodesByFtrIDX;
    /**
     * map nodes that have/don't have  examples of specified type
     */
    private ConcurrentSkipListMap<SOM_ExDataType, HashSet<SOM_MapNode>> nodesWithEx, nodesWithNoEx;    
    /**
     * array of per ftr idx treemaps of map nodes keyed by ftr weight
     */
    private TreeMap<Float,ArrayList<SOM_MapNode>>[] PerFtrHiWtMapNodes;    
    
    /**
     * array of map clusters based in UMatrix Distance
     */
    protected ArrayList<SOM_MappedSegment> UMatrix_Segments;
    /**
     * array of map clusters based on ftr distance, keyed by Ftr IDX
     */
    protected TreeMap<Integer, SOM_MappedSegment> FtrWt_Segments;
    /**
     * index of pretrained map being displayed; if -1 then none
     */
    protected int pretrainedMapIDX = -1;
    
    //////////////////
    // class and category mapping
    // classes belong to training data - they are assigned to map nodes that are bmus for a particular training example
    // they are then used to define the nature of segments/clusters on the map, as well as give a probability of class 
    // membership to an unclassified sample that maps to a praticular BMU
    // categories are collections of similar classes
    /**
     * Map of classes to segment
     */
    protected TreeMap<Integer, SOM_MappedSegment> Class_Segments;
    /**
     * map with key being class and with value being collection of map nodes with that class present in mapped examples
     */
    protected TreeMap<Integer,Collection<SOM_MapNode>> MapNodesWithMappedClasses;
    /**
     * probabilities for each class for each map node
     */
    protected ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>> MapNodeClassProbs;

    /**
     * map of categories to segment
     */
    protected TreeMap<Integer, SOM_MappedSegment> Category_Segments;
    /**
     * map with key being category and with value being collection of map nodes with that category present in mapped examples
     */
    protected TreeMap<Integer,Collection<SOM_MapNode>> MapNodesWithMappedCategories;
    /**
     * probabilities for each category for each map node
     */
    protected ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>> MapNodeCategoryProbs;
    
    /**
     * data values directly from the trained map, populated upon load
     */
    private float[] 
            map_ftrsMean,                 
            map_ftrsVar,
            map_ftrsDiffs, 
            map_ftrsMin;                
    /**
     * min and diff umat dist seen from SOM calc
     */
    private float                         
        uMatDist_Min,
        uMatDist_Diff;
    
    /**
     * min and diff target span for per-feature normalizing - populated by project config
     */
    private float
        perFtrNorm_destMin,
        perFrtNorm_destDiff;
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //data descriptions
    /**
     * this map of mappers will manage the different kinds of raw data.  the instancing class should specify the keys and instancing members for this map
     */
    protected ConcurrentSkipListMap<String, SOM_ExampleManager> exampleDataMappers;    
    
    /**
     * full input data, data set to be training data and testing data (all of these examples 
     * are potential -training- data, in that they have all features required of training data) 
     * testing data will be otherwise valid training data that will be matched against map - 
     * having these is not going to be necessary for most cases since this is unsupervised,
     * but can be used to measure consistency
     */
    protected SOM_Example[] inputData;
    protected SOM_Example[] trainData;
    protected SOM_Example[] testData;    
    /**
     * validationData are example records failing to meet the training criteria or otherwise desired to be mapped against SOM these were not used to train the map    
     */
    protected SOM_Example[] validationData;        
    /**
     * sizes of example data arrays
     */
    public int numInputData, numTrainData, numTestData, numValidationData;
    
    /**
     * values to return scaled values to actual data points - multiply wts by diffsVals, add minsVal idx 0 is feature diffs/mins per (ftr idx); idx 1 is across all ftrs
     */
    private Float[][] diffsVals, minsVals;    
    /**
     * # of training features 
     */
    private int numTrnFtrs;
    
    //////////////////////////////
    //data in files created by SOM_MAP separated by spaces
    public static final String SOM_FileToken = " ", csvFileToken = "\\s*,\\s*";    

    ////////////////////        
    //data type to use to describe/train map
    //public static final int useUnmoddedDat = 0, useScaledDat = 1, Normalized = 2;
    public static final String[] uiMapTrainFtrTypeList = SOM_FtrDataType.getListOfTypes();
    /**
     * types of possible mappings to particular map node as bmu
     *  corresponds to these values : all ExDataTypes except last 2
     */
    private static String[] nodeBMUMapTypes;
    /**
     * feature type used for training currently trained/loaded map
     */
    protected SOM_FtrDataType curMapTrainFtrType;    
    /**
     * feature type used for testing/finding proposals currently - comparing features to map
     */
    protected SOM_FtrDataType BMU_DispFtrType;
    /**
     * distance to use :  1: chisq features or 0 : regular feature dists
     */
    protected boolean useChiSqDist;    
    /**
     * map dims is used to calculate distances for BMUs - based on screen dimensions - need to change this?
     */
    protected float[] mapDims;
    //default value to use if no window sent
    protected final float[] SOM_DefaultMapDims = new float[] {834.8f,834.8f};
    /**
     * # of nodes in SOM in x/y
     */
    protected int mapNodeCols = 0, mapNodeRows = 0;
    /**
     * ratio of nodes to map dim(pxls) in x/y
     */
    protected float nodeXPerPxl, nodeYPerPxl;
    /**
     * threshold of u-dist for nodes to belong to same segment
     */
    public static final Double initNodeInSegUMatrixDistThresh = .3;
    protected float nodeInSegUMatrixDistThresh = .3f;
    protected float mapMadeWithUMatrixSegThresh = 0.0f;
    /**
     * threshold for distance for ftr weight segment construction
     */
    public static final Double initNodeInSegFtrWtDistThresh = .01;
    protected float nodeInSegFtrWtDistThresh = .01f;
    protected float mapMadeWithFtrWtSegThresh = 0.0f;
    /**
     * state flags - bits in array holding relevant process info
     */
    private int[] stFlags;                        
    public static final int
            debugIDX                     = 0,
            isMTCapableIDX                = 1,
            SOMmapNodeDataLoadedIDX        = 2,            //som map data is cleanly loaded
            loaderFinishedRtnIDX        = 3,            //dataloader has finished - wait on this to draw map
            denseTrainDataSavedIDX         = 4,            //all current example data has been saved as a training data file for SOM (.lrn format) 
            sparseTrainDataSavedIDX        = 5,            //sparse data format using .svm file descriptions (basically a map with a key:value pair of ftr index : ftr value
            testDataSavedIDX            = 6,            //save test data in sparse format csv
        //data types mapped to bmus flags - ready to save/display
            trainDataMappedIDX            = 7,
            prodDataMappedIDX            = 8,
            testDataMappedIDX            = 9,
            validateDataMappedIDX        = 10,
            
            dispMseDataSideBarIDX        = 11,            //whether to display mouse data on side bar
            dispLdPreBuitMapsIDX        = 12;
        
    public static final int numBaseFlags = 13;    
    //numFlags is set by instancing map manager getMseOvrLblArray()
    
    /**
     * threading constructions - allow map manager to own its own threading executor
     */
    protected ExecutorService th_exec;    //to access multithreading - instance from calling program
    protected final int numUsableThreads;        //# of threads usable by the application
    
    
    //////////////////////////////
    // UI Values - should only be inited or accessed if application is win != null
    //images displaying map data
    
    /**
     * start location of SOM image - stX, stY; 
     */
    protected float[] SOM_mapLoc;

    /**
     * array of per-ftr map wts
     */
    protected PImage[] mapPerFtrWtImgs;
    /**
     * image of umatrix (distance between nodes)
     */
    protected PImage mapCubicUMatrixImg;
    /**
     * image of segments suggested by UMat Dist
     */
    protected PImage mapUMatrixCubicSegmentsImg;
    /**
     * scaling value - use this to decrease the image size and increase the scaling so it is rendered the same size
     */
    public static final float mapScaleVal = 10.0f;
    
    /**
     * which ftr map is currently being shown
     */
    protected int curFtrMapImgIDX;
    /**
     * which category idx is currently selected
     */
    protected int curCategoryIDX;
    /**
     * which category _label_ is currently selected
     */
    protected int curCategoryLabel;
    
    /**
     * which class idx is currently selected
     */
    protected int curClassIDX;
    /**
     * which class _label_ is currently selected
     */
    protected int curClassLabel;
    /**
     * threshold of wt value to display map node
     */
    protected float mapNodeWtDispThresh = 0.01f;
    /**
     * % of max node pop threshold of populated map node size to display - should always be positive
     */
    protected float mapNodePopDispThreshPct = 0.0f;
    protected int numMapNodeByPopNowShown = 0;
    /**
     * for display only - threshold of count of type 
     */
    protected float[] mapNodePopDispThreshVals;
    /**
     * type of examples using each map node as a bmu to display
     */
    protected SOM_ExDataType mapNodeDispType;
    /**
     * current choice for default prebuilt map index, if any exist
     */
    protected int curPreBuiltMapIDX = -1;

    /**
     * location and label of mouse-over point in map 
     */
    protected SOM_MseOvrDisplay mseOverExample;
    /**
     * UI-chosen desired mouse display data
     */
    protected SOM_MseOvrDispTypeVals uiMseDispData = SOM_MseOvrDispTypeVals.mseOvrNoneIDX;
    protected int custUIMseDispData = -1;
    /**
     * PImage object to represent map node population graph
     */
    protected PImage[] mapNodePopGraph;     
    
    /**
     * This construct manages process launching.
     */
    protected myProcessManager<mySOMProcConsoleMgr> processManager;
    
    /**
     * @param _win owning window(may be null)
     * @param _dims dimensions of SOM Map in pxls (used for topological distances)
     * @param _argsMap String[] _dirs : idx 0 is config directory, as specified by cmd line; idx 1 is data directory, as specified by cmd line
     *                     String[] _args : command line arguments other than directory info
     */
    public SOM_MapManager(SOM_MapUIWin _win, Map<String, Object> _argsMap) {
        setWinAndWinData(_win);//win=_win;    
        ID = cnt++;        
        mapUIAPI = buildSOM_UI_Interface();
        initFlags();        
        //message object manages displaying to screen and potentially to log files - needs to be built first
        msgObj = MessageObject.getInstance();

        //build project configuration data object - this manages all file locations and other configuration options
        //needs to have msgObj defined before called
        projConfigData = buildProjConfigData(_argsMap);
        Integer _logLevel = (Integer)_argsMap.get("logLevel");
        msgObj.setOutputMethod(projConfigData.getFullLogFileNameString(), _logLevel);

        //fileIO is used to load and save info from/to local files except for the raw data loading, which has its own handling
        fileIO = new FileIOManager(msgObj,"SOM_MapManager::"+name);
        //want # of usable background threads.  Leave 2 for primary process (and potential draw loop)
        numUsableThreads = Java_AppManager.getNumThreadsAvailable() - 2;
        //set if this is multi-threaded capable - need more than 1 outside of 2 primary threads (i.e. only perform multithreaded calculations if 4 or more threads are available on host)
        setFlag(isMTCapableIDX, numUsableThreads>1);
        //default to have mouse location display on side of screen
        setFlag(dispMseDataSideBarIDX, true);
        //default to have # of prebuilt map directories loaded display on side of screen
        setFlag(dispLdPreBuitMapsIDX, true);
        //th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
        if(getFlag(isMTCapableIDX)) {
            //th_exec = Executors.newFixedThreadPool(numUsableThreads+1);
            th_exec = Executors.newCachedThreadPool();// this is performing much better even though it is using all available threads
        } else {//setting this just so that it doesn't fail somewhere - won't actually be exec'ed
            //TODO get rid of this when not MTCapable
            th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
        }
        //must be built after th_exec and msgObj are built
        processManager = new myProcessManager<>("SOM_MapManager::"+name, msgObj, th_exec);        
        //data mappers - eventually will replace all example maps, and will derive all training data arrays
        exampleDataMappers = new ConcurrentSkipListMap<String, SOM_ExampleManager>();
        //build mappers that will manage data read from disk in order to calculate features and build data arrays used by SOM
        buildExampleDataMappers();
        
        resetTrainDataAras();
        mseOverExample = buildMseOverExample();
    }//ctor
    /**
     * set window and win-related variables
     */
    private void setWinAndWinData(SOM_MapUIWin _win) {
        win=_win;
        if(win != null) {            //update dims with window size values
            setName(win.getName());
            mapDims = win.getWinUIMapDims();
            AppMgr = SOM_MapUIWin.AppMgr;
        } else {
            setName("No_Win");
            mapDims = SOM_DefaultMapDims;
            AppMgr = null;
        }
    }//setWinAndWinData
    /**
     * only set if win != null
     */
    private void setName(String _winName) {name = _winName + "_MapMgr_ID_"+ID;}
    public String getName() {return name;}
    /**
     * use this to set window/UI components, if exist
     * @param _win
     * @param _hasGraphics
     */
    public void setDispWinData(SOM_MapUIWin _win) {
        setWinAndWinData(_win);
        projConfigData.setUIValsFromLoad();
        setDispWinDataIndiv();
    }//setPAWindowData

    /**
     * Any instancing-class-specific functionality for after MapUIWindow is set
     */
    protected abstract void setDispWinDataIndiv();
    
    /**
     * build the map of example mappers used to manage all the data the SOM will consume
     */
    protected abstract void buildExampleDataMappers();

    /**
     * build instance-specific project file configuration - necessary if using project-specific config file
     */    
    protected abstract SOM_ProjConfigData buildProjConfigData(Map<String, Object> _argsMap);
    
    /**
     * build mouse-over example
     */
    protected abstract SOM_MseOvrDisplay buildMseOverExample();
    
    /**
     * build an interface to manage communications between UI and SOM map dat
     * This interface will need to include a reference to an application-specific UI window
     */
    protected abstract SOM_UIToMapCom buildSOM_UI_Interface();
    
    public static String[] getNodeBMUMapTypes() {
        String[] typeList = SOM_ExDataType.getListOfTypes();
        if (nodeBMUMapTypes==null) {
            nodeBMUMapTypes = new String[typeList.length-2];
            for(int i=0;i<nodeBMUMapTypes.length;++i) {    nodeBMUMapTypes[i]=typeList[i];    }            
        }
        return nodeBMUMapTypes;
    }
    
    /**
     * determine how many values should be per thread
     * @param numVals # of values total
     * @param numThds # of threads available
     * @return # of values per thread
     */
    public int calcNumPerThd(int numVals, int numThds) {
        //return (int) Math.round((numVals + Math.round(numThds/2))/(1.0*numThds));
        return (int) ((numVals -1)/(1.0*numThds)) + 1;
        //=\operatorname{round}\left(\frac{x+\operatorname{floor}\left(\frac{7}{2}\right)}{7}\ \right)
    }//calcNumPerThd
    
    /**
     * reset all training data arrays and information
     */
    public void resetTrainDataAras() {
        msgObj.dispMessage("SOM_MapManager::"+name,"resetTrainDataAras","Init Called to reset all train and test data.", MsgCodes.info5);
        inputData = new SOM_Example[0];
        testData = new SOM_Example[0];
        trainData = new SOM_Example[0];
        validationData = new SOM_Example[0];
        numInputData=0;
        numTrainData=0;
        numTestData=0;        
        numValidationData =0;
        nodesWithEx = new ConcurrentSkipListMap<SOM_ExDataType, HashSet<SOM_MapNode>>();
        nodesWithNoEx = new ConcurrentSkipListMap<SOM_ExDataType, HashSet<SOM_MapNode>>();
        for (SOM_ExDataType _type : SOM_ExDataType.values()) {
            nodesWithEx.put(_type, new HashSet<SOM_MapNode>());
            nodesWithNoEx.put(_type, new HashSet<SOM_MapNode>());        
        }
        pretrainedMapIDX = -1;
        msgObj.dispMessage("SOM_MapManager::"+name,"resetTrainDataAras","Init Finished", MsgCodes.info5);
    }//resetTrainDataAras()
    //
    //curMapTrainFtrType.getBrfName();public String getDataTypeNameFromCurFtrTrainType_Brf() {return curMapTrainFtrType.getBrfName();}    
    public String getDataTypeNameFromBMU_DispFtrType_Brf() {return BMU_DispFtrType.getBrfName();}    
    //Unnormalized = 0, Feature Normalized = 1, Example Normalized = 2
    public String getDataTypeNameFromInt_Brf(SOM_FtrDataType dataFrmt) { return dataFrmt.getBrfName();}//getDataTypeNameFromInt
    
    public String getDataDescFromCurFtrTrainType()  {return curMapTrainFtrType.getExplanation();}
    public String getDataDescFromBMU_DispFtrType()  {return BMU_DispFtrType.getExplanation();}
    public String getDataDescFromInt(SOM_FtrDataType dataFrmt) {return dataFrmt.getExplanation();}//getDataTypeNameFromInt
    
    public String getDataDescFromInt_Short(SOM_FtrDataType dataFrmt) {
        switch(dataFrmt) {
        case UNNORMALIZED : {return "Unnormalized";}
        case FTR_NORM : {return "Feature-based";}
        case EXMPL_NORM : {return "Example-based";}
        default : {return null;}        //unknown data frmt type
        }
    }//getDataTypeNameFromInt
    
    //return data format enum val based on string name
    public SOM_FtrDataType getDataFrmtTypeFromName(String dataFrmtName) {
        String comp = dataFrmtName.toLowerCase();
        switch(comp) {
        //old strings
        case "unmodftrs": {return SOM_FtrDataType.UNNORMALIZED;}
        case "stdftrs"    : {return SOM_FtrDataType.FTR_NORM;}
        case "normftrs"    : {return SOM_FtrDataType.EXMPL_NORM;}
        //new string brief keys : {"unNormFtrs","perFtrNorm","perExNorm"}
        case "unnormftrs": {return SOM_FtrDataType.UNNORMALIZED;}
        case "perftrnorm"    : {return SOM_FtrDataType.FTR_NORM;}
        case "perexnorm"    : {return SOM_FtrDataType.EXMPL_NORM;}
        
        default : {return SOM_FtrDataType.UNNORMALIZED;}        //unknown data frmt type
        }        
    }//getDataFrmtTypeFromName
    //load raw data and preprocess, partitioning into different data types as appropriate
    public abstract void loadAndPreProcAllRawData(boolean fromCSVFiles);

    /**
     * execute post-feature vector build code in multiple threads if supported by architecture
     * @param exs collection of examples
     * @param _typeOfProc type of process to execute : 
     *             0 == build feature vector; 
     *             1 == per-example finalizing (after all features are built for current type of examples); 
     *             2 = after all feature vectors are built (if there are different types of example data all using same equations to calculate features and all contributing to global mins/maxs, etc)
     * @param exType type of examples 
     * @param forceST whether to force single threaded execution (this needs to be the default for feature building, since feature building constructs may not be thread safe due to performance concerns)
     */
    public void _ftrVecBuild(Collection<SOM_Example> exs, int _typeOfProc, String exType, boolean forceST) {
        getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Begin "+exs.size()+" example processing.", MsgCodes.info1);
        boolean canMultiThread=isMTCapable() && !forceST;//if false this means the current machine only has 1 or 2 available processors, numUsableThreads == # available - 2
        //if((canMultiThread) && (exs.size()>0)) {//MapExFtrCalcs_Runner.rawNumPerPartition*10)){
        if((canMultiThread) && (exs.size()>SOM_CalcExFtrs_Runner.rawNumPerPartition*numUsableThreads)){
            //shuffling examples to attempt to spread out calculations more evenly - the examples that require the alt comp vector calc are expensive to calculate
            //should not be multithread unless ftr calc is known to be devoid of concurrency issues
            SOM_CalcExFtrs_Runner calcRunner = new SOM_CalcExFtrs_Runner(this, th_exec, shuffleTrainingData(exs.toArray(new SOM_Example[0]),12345L) , exType, _typeOfProc, false);
            calcRunner.runMe();
        } else {//called after all features of this kind of object are built - this calculates alternate compare object
            int curIDX = 0, ttlNum = exs.size(), modAmt = (ttlNum > 100 ? ttlNum/10 : ttlNum);
            if(_typeOfProc==0) {
                getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Begin build "+exs.size()+" feature vector.", MsgCodes.info1);
                for (SOM_Example ex : exs) {            ex.buildFeatureVector();    ++curIDX; if(curIDX % modAmt == 0) {_dbg_ftrVecBuild_dispProgress(curIDX, ttlNum,exType,"build "+ttlNum+" feature vector.");}}
                getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Finished build "+exs.size()+" feature vector.", MsgCodes.info1);
            } else if(_typeOfProc==1) {
                getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Begin "+exs.size()+" After Feature Vector Build (Per example finalizing).", MsgCodes.info1);
                for (SOM_Example ex : exs) {            ex.postFtrVecBuild();    ++curIDX; if(curIDX % modAmt == 0) {_dbg_ftrVecBuild_dispProgress(curIDX, ttlNum,exType,"build "+ttlNum+" After Feature Vector Build (Per example finalizing).");}}        
                getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Finished "+exs.size()+" After Feature Vector Build (Per example finalizing).", MsgCodes.info1);
            } else if(_typeOfProc==2) {
                getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Begin "+exs.size()+" Post Feature Vector Structures (STD Vecs) Build.", MsgCodes.info1);
                for (SOM_Example ex : exs) {            ex.buildAfterAllFtrVecsBuiltStructs();    ++curIDX; if(curIDX % modAmt == 0) {_dbg_ftrVecBuild_dispProgress(curIDX, ttlNum,exType,"build "+ttlNum+" Post Feature Vector Structures (STD Vecs and possibly alternate comparison ftrs).");}}        
                getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Finished "+exs.size()+" Post Feature Vector Structures (STD Vecs) Build.", MsgCodes.info1);
            }
        }
        getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Finished "+exs.size()+" example processing.", MsgCodes.info1);
    }//_postFtrVecBuild
    
    private void _dbg_ftrVecBuild_dispProgress(int curVal, int ttlNum, String exType, String proc) {
        getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples"," Finished :  " +Math.round((100.0f*curVal)/ttlNum) + "% : " + curVal+ " of " + ttlNum +" for process : " + proc,MsgCodes.info1);        
    }
    
//    //execute post-feature vector build code in multiple threads if supported
//    public void _ftrVecBuild(Collection<SOMExample> exs, int _typeOfProc, String exType) {
//        getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Begin "+exs.size()+" example processing.", MsgCodes.info1);
//        //shuffling examples to attempt to spread out calculations more evenly - the examples that require the alt comp vector calc are expensive to calculate
//        //runner handles whether ST or MT - force to single thread
//        MapExFtrCalcs_Runner calcRunner = new MapExFtrCalcs_Runner(this, th_exec, shuffleTrainingData(exs.toArray(new SOMExample[0]),12345L) , exType, _typeOfProc, true);
//        calcRunner.runMe();
//        getMsgObj().dispMessage("SOM_MapManager::"+name,"_ftrVecBuild : " + exType + " Examples","Finished "+exs.size()+" example processing.", MsgCodes.info1);
//    }//_postFtrVecBuild
    
    //this function will build the input data used by the SOM - this will be partitioned by some amount into test and train data (usually will use 100% train data, but may wish to test label mapping)
    protected abstract SOM_Example[] buildSOM_InputData();
    //set input data and shuffle it; partition test and train arrays 
    protected void setInputTrainTestShuffleDataAras(float trainTestPartition) {        
        msgObj.dispMessage("SOM_MapManager::"+name,"setInputTestTrainDataArasShuffle","Shuffling Input, Building Training and Testing Partitions.", MsgCodes.info5);
        //set partition size in project config
        projConfigData.setTrainTestPartition(trainTestPartition);
        //build input data appropriately for project
        inputData = buildSOM_InputData();        
        //performed in place - use same key so is reproducible training, always has same shuffled order
        inputData = shuffleTrainingData(inputData, 12345L);
        numTrainData = (int) (inputData.length * trainTestPartition);            
        numTestData = inputData.length - numTrainData;        
        //build train and test partitions
        trainData = new SOM_Example[numTrainData];    
        msgObj.dispMessage("SOM_MapManager::"+name,"setInputTestTrainDataArasShuffle","# of training examples : " + numTrainData + " inputData size : " + inputData.length, MsgCodes.info3);
        for (int i=0;i<trainData.length;++i) {trainData[i]=inputData[i];trainData[i].setIsTrainingDataIDX(true, i);}
        testData = new SOM_Example[numTestData];
        for (int i=0;i<testData.length;++i) {testData[i]=inputData[i+numTrainData];testData[i].setIsTrainingDataIDX(false, i+numTrainData);}        

        msgObj.dispMessage("SOM_MapManager::"+name,"setInputTestTrainDataArasShuffle","Finished Shuffling Input, Building Training and Testing Partitions. Train size : " + numTrainData+ " Testing size : " + numTestData+".", MsgCodes.info5);
    }//setInputTestTrainDataArasShuffle
    
    /**
     * build file names, including info for data type used to train map, save training data using specified ftr structure, and save mins/diffs
     */
    protected void initNewSOMDirsAndSaveData() {
        msgObj.dispMessage("SOM_MapManager::"+name,"initNewSOMDirsAndSaveData","Begin building new directories, saving Train, Test data and data Mins and Diffs. Train size : " + numTrainData+ " Testing size : " + numTestData+".", MsgCodes.info5);    
        //set ftr data type used to train
        projConfigData.setFtrDataTypeUsedToTrain(curMapTrainFtrType.getBrfName());
        //build directories for this experiment
        projConfigData.buildDateTimeStrAraAndDType();
        //save partitioned data in built directories
        projConfigData.launchTestTrainSaveThrds(th_exec, curMapTrainFtrType, numTrnFtrs,trainData,testData);                //save testing and training data    
        //save mins and diffs of current training data
        saveMinsAndDiffs();        
        msgObj.dispMessage("SOM_MapManager::"+name,"initNewSOMDirsAndSaveData","Finished building new directories, saving Train, Test data and data Mins and Diffs. Train size : " + numTrainData+ " Testing size : " + numTestData+".", MsgCodes.info5);        
    }

    protected abstract void loadPreProcTrainData(String subDir, boolean forceLoad);
    //build the testing and training data partitions and save them to files
    protected abstract void buildTrainTestFromPartition(float trainTestPartition);
    
    /**
     * load preproc customer csv and build training and testing partitions - testing partition not necessary 
     * @param trainTestPartition
     * @param forceLoad
     */
    public void loadPreprocAndBuildTestTrainPartitions(float trainTestPartition, boolean forceLoad) {
        msgObj.dispMessage("SOM_MapManager::"+name,"loadPreprocAndBuildTestTrainPartitions","Start Loading all CSV example Data to train map.", MsgCodes.info5);
        loadPreProcTrainData(projConfigData.getPreProcDataDesiredSubDirName(),forceLoad);
        //build test/train data partitions
        buildTrainTestFromPartition(trainTestPartition);    
        msgObj.dispMessage("SOM_MapManager::"+name,"loadPreprocAndBuildTestTrainPartitions","Finished Loading all CSV example Data to train map.", MsgCodes.info5);
    }//loadPreprocAndBuildTestTrainPartitions
    
    /**
     * build new SOM_MAP map using UI-entered values, then load resultant data with maps of 
     * required SOM exe params TODO this will be changed to not pass values from UI, but 
     * rather to finalize and save values already set in SOM_MapDat object from UI or other 
     * user input
     * @param mapInts
     * @param mapFloats
     * @param mapStrings
     */
    public void updateAllMapArgsFromUI(HashMap<String, Integer> mapInts, HashMap<String, Float> mapFloats, HashMap<String, String> mapStrings){
        //set and save configurations
        projConfigData.setSOM_MapArgs(mapInts, mapFloats, mapStrings);
    }
    
    //this will load the map into memory, bmus, umatrix, etc - this is necessary to consume map
    protected void loadMapAndBMUs() {
        msgObj.dispMessage("SOM_MapManager::"+name,"loadMapAndBMUs_Synch","Building Mappings synchronously.", MsgCodes.info1);
        SOM_DataLoader ldr = new SOM_DataLoader(this,projConfigData);//can be run in separate thread, but isn't here
        boolean success = ldr.callMe();    
        msgObj.dispMessage("SOM_MapManager::"+name,"loadMapAndBMUs_Synch","Finished data loader : SOM Data Loaded successfully : " + success, MsgCodes.info5 );        
    }//loadMapAndBMUs_Synch
    
    /**
     * Load a prebuilt map - this is called only from UI version
     * Load preprocessed training data and process it (assumed to be data used to build map, if not, then map will be corrupted)
     * Load SOM data; partition pretrained data; map loaded training data to map nodes
     */
    public void loadPretrainedExistingMap(int mapID, boolean forceReLoad) {
        //load preproc data used to train map - it is assumed this data is in default directory
        msgObj.dispMessage("SOM_MapManager::"+name,"loadPretrainedExistingMap","First load pretrained map # " + mapID + " using directory specified in project config file.", MsgCodes.info1);
        //set default map id
        setDefaultPretrainedMapIDX(mapID);
        //for prebuilt map - load config used in prebuilt map
        boolean dfltmapLoaded = projConfigData.setSOM_UsePreBuilt();    
        if(!dfltmapLoaded) {
            msgObj.dispMessage("SOM_MapManager::"+name,"loadPretrainedExistingMap","No Default map loaded, probably due to no default map directories specified in config file.  Aborting ", MsgCodes.info1);
            return;
        }
        msgObj.dispMessage("SOM_MapManager::"+name,"loadPretrainedExistingMap","Next load training data used to build map - it is assumed this data is in default preproc directory.", MsgCodes.info1);
        //load training data into preproc  -this must be data used to build map and build data partitions - use partition size set via constants in debug
        loadPreprocAndBuildTestTrainPartitions(projConfigData.getTrainTestPartition(),forceReLoad);
        
        msgObj.dispMultiLineInfoMessage("SOM_MapManager::"+name,"loadPretrainedExistingMap","Now map all training data to loaded map.");
        //don't execute in a thread, execute synchronously so we can use results immediately upon return
        loadMapAndBMUs();
        //setSOM_UsePreBuilt may change default map being built, if not passed a valid idx
        pretrainedMapIDX = projConfigData.getSOM_DefaultPreBuiltMap();
        msgObj.dispMessage("SOM_MapManager::"+name,"loadPretrainedExistingMap","Data loader finished loading map nodes and matching training data and products to BMUs using pretrained map IDX : " + pretrainedMapIDX +"." , MsgCodes.info3);
    }//loadPretrainedExistingMap
    /**
     * set default map id - does not perform value checking
     * @param _idx
     */
    public void setDefaultPretrainedMapIDX(int _idx) {projConfigData.setSOM_DefaultPreBuiltMap(_idx);}
    
    /**
     * Use this method to 
     * 1) load all training data and example/validation data to map
     * 2) load map data and derive map node bmus for examples, building class and category
     * 3) find bmus for all validation data, as well as class and category membership, if appropriate
     * 4) save all mappings 
     */
    public abstract void loadAllDataAndBuildMappings();
        
    //this will load the default map training configuration
    public void loadSOMConfig() {    projConfigData.loadDefaultSOMExp_Config();    }//loadSOMConfig
    
    /**
     * train map with currently set SOM control values - UI will set values as they change, if UI is being used, otherwise values set via config files
     */
    public boolean loadTrainDataMapConfigAndBuildMap(boolean mapNodesToData) {    
        msgObj.dispMessage("SOM_MapManager::"+name,"loadTrainDataMapConfigAndBuildMap","Start Loading training data and building map. Mapping examples to SOM Nodes : "+mapNodesToData, MsgCodes.info1);
        //load all training data and build test and training data partitions
        loadPreprocAndBuildTestTrainPartitions(projConfigData.getTrainTestPartition(), false);
        //build experimental directories, save training, testing and diffs/mins data to directories - only should be called when building a new map
        initNewSOMDirsAndSaveData();        
        //reload currently set default config for SOM - IGNORES VALUES SET IN UI
        msgObj.dispMessage("SOM_MapManager::"+name,"loadTrainDataMapConfigAndBuildMap","Finished Loading training data and setting directories.", MsgCodes.info1);
        boolean res = _ExecSOM(mapNodesToData);
        msgObj.dispMessage("SOM_MapManager::"+name,"loadTrainDataMapConfigAndBuildMap","Finished Loading training data and building map. Success : "  + res+ " | Mapped examples to SOM Nodes :"+mapNodesToData, MsgCodes.info1);
        return res;
    }//loadTrainDataMapConfigAndBuildMap
    
    /**
     * execute some training and map data to to BMUs if specified
     * @param mapNodesToData
     * @return
     */
    protected boolean _ExecSOM(boolean mapNodesToData) {
        msgObj.dispMessage("SOM_MapManager::"+name,"_ExecSOM","Start building map.", MsgCodes.info1);
        //execute map training
        SOM_MapDat SOMExeDat = projConfigData.getSOMExeDat();
        //set currently defined directories and values in SOM manager
        SOMExeDat.updateMapDescriptorState();
        msgObj.dispMultiLineMessage("SOM_MapManager::"+name,"runSOMExperiment","SOM map descriptor : \n" + SOMExeDat.toString() + "SOMOutExpSffx str : " +projConfigData.getSOMOutExpSffx(), MsgCodes.info5);        
        //save configuration data
        projConfigData.saveSOM_Exp();
        //save start time
        String startTime = msgObj.getCurrWallTimeAndTimeFromStart();
        //launch in a thread? - needs to finish to be able to proceed, so not necessary
        boolean runSuccess = buildNewMap(SOMExeDat);        
        if(runSuccess) {                
            //build values for human-readable report
            TreeMap<String, String> externalVals = getSOMExecInfo();
            externalVals.put("SOM Training Begin Wall Time and Time elapsed from Start of program execution", startTime);
            externalVals.put("SOM Training Finished Wall Time and Time elapsed from Start of program execution", msgObj.getCurrWallTimeAndTimeFromStart());
            //save report of results of execution in human-readable report format        
            projConfigData.saveSOM_ExecReport(externalVals);
            //set flags before calling loader
            setLoaderRTN(false);
            //map training data to map nodes
            if(mapNodesToData) {            loadMapAndBMUs();        }
        }        
        msgObj.dispMessage("SOM_MapManager::"+name,"_ExecSOM","Finished building map.", MsgCodes.info1);
        return runSuccess;
    }//_ExecSOM    
    
    /**
     * Build map from data by aggregating all training data, building SOM exec string from UI input, and calling OS cmd to run SOM_MAP
     * @param mapExeDat SOM descriptor
     * @return whether training succeeded or not
     */
    private boolean buildNewMap(SOM_MapDat mapExeDat){
        boolean success = true;        
        msgObj.dispMessage("SOM_MapManager::"+name,"buildNewMap","buildNewMap Starting", MsgCodes.info5);
        msgObj.dispMultiLineMessage("SOM_MapManager::"+name,"buildNewMap","Execution String for running manually : \n"+mapExeDat.getDbgExecStr(), MsgCodes.warning2);
        String[] cmdExecStr = mapExeDat.getExecStrAra();

        msgObj.dispMessage("SOM_MapManager::"+name,"buildNewMap","Execution Arguments passed to SOM, parsed by flags and values: ", MsgCodes.info2);
        msgObj.dispMessageAra("SOM_MapManager::"+name,"buildNewMap",cmdExecStr,2, MsgCodes.info2);//2 strings per line, display execution command    

        String wkDirStr = mapExeDat.getExeWorkingDir(), 
                cmdStr = mapExeDat.getExename(),
                argsStr = "";
        String[] execStr = new String[cmdExecStr.length +1];
        execStr[0] = wkDirStr + cmdStr;
        //for(int i =2; i<cmdExecStr.length;++i){execStr[i-1] = cmdExecStr[i]; argsStr +=cmdExecStr[i]+" | ";}
        for(int i = 0; i<cmdExecStr.length;++i){execStr[i+1] = cmdExecStr[i]; argsStr +=cmdExecStr[i]+" | ";}
        msgObj.dispMultiLineMessage("SOM_MapManager::"+name,"buildNewMap","\nwkDir : "+ wkDirStr + "\ncmdStr : " + cmdStr + "\nargs : "+argsStr, MsgCodes.info1);
        
        //monitor in multiple threads, either msgs or errors
        success = processManager.launch(execStr, wkDirStr, new mySOMProcConsoleMgr(msgObj, "Input" ), new mySOMProcConsoleMgr(msgObj, "Error" ));

        msgObj.dispMessage("SOM_MapManager::"+name,"buildNewMap","buildNewMap Finished", MsgCodes.info5);            
        return success;
    }//buildNewMap
    
    
    //////////////////////////////////
    // map images and segments    
    
    ///////////////////////////////////////////
    // map image init    
    public final void initFromUIWinInitMe(int _trainDatFrmt,int _BMUDispDatFrmt, float _mapNodeWtDispThresh, float _mapNodePopDispThreshPct, int _mapNodeDispType) {
        setCurrentTrainDataFormat(SOM_FtrDataType.getEnumByIndex(_trainDatFrmt));
        setBMU_DispFtrTypeFormat(SOM_FtrDataType.getEnumByIndex(_BMUDispDatFrmt));
        mapNodeWtDispThresh = _mapNodeWtDispThresh;
        mapNodeDispType = SOM_ExDataType.getEnumByIndex(_mapNodeDispType);
        if(mapNodePopDispThreshPct != _mapNodePopDispThreshPct) {
            mapNodePopDispThreshPct = _mapNodePopDispThreshPct;
            buildMapNodePopGraphImage();
        }
        mseOverExample.clearMseDat();
        //mseOvrData = null;    
    }
    
    //private final void reInitMapCubicSegments() {    if (win != null) {    mapUMatrixCubicSegmentsImg = win.ri.createImage(mapCubicUMatrixImg.width,mapCubicUMatrixImg.height, win.ri.ARGB);}}//ARGB to treat like overlay
    public final void initMapAras(int numFtrVals, int num2ndryMaps) {
        if (win != null) {    
            msgObj.dispMessage("SOM_MapManager::"+name,"initMapAras","Start Initializing per-feature map display to hold : "+ numFtrVals +" primary feature and " +num2ndryMaps + " secondary feature map images.", MsgCodes.info1);
            curFtrMapImgIDX = 0;
            int format = PConstants.RGB; 
            //int w = (int) (SOM_mapDims[0]/mapScaleVal), h = (int) (SOM_mapDims[1]/mapScaleVal);
            int w = (int) (mapDims[0]/mapScaleVal), h = (int) (mapDims[1]/mapScaleVal);
            mapPerFtrWtImgs = new PImage[numFtrVals];
            for(int i=0;i<mapPerFtrWtImgs.length;++i) {            mapPerFtrWtImgs[i] = ((ProcessingRenderer)Base_DispWindow.ri).createImage(w, h, format);    }    
            
            mapCubicUMatrixImg = ((ProcessingRenderer)Base_DispWindow.ri).createImage(w, h, format);            
            //reInit MapCubicSegments 
            mapUMatrixCubicSegmentsImg = ((ProcessingRenderer)Base_DispWindow.ri).createImage(mapCubicUMatrixImg.width,mapCubicUMatrixImg.height, PConstants.ARGB);
            //instancing-window specific initializations
            initMapArasIndiv(w,h, format,num2ndryMaps);
        }
    }//initMapAras    
    
    protected abstract void initMapArasIndiv(int w, int h, int format, int num2ndFtrVals);
    //only appropriate if using UI
    public void initMapFtrVisAras(int numTrainFtrs) {
        if (win != null) {
            int num2ndTrainFtrs = _getNumSecondaryMaps();
            msgObj.dispMessage("SOM_MapManager::"+name,"initMapFtrVisAras","Initializing per-feature map display to hold : "+ numTrainFtrs +" primary feature and " +num2ndTrainFtrs + " secondary feature map images.", MsgCodes.info1);
            initMapAras(numTrainFtrs, num2ndTrainFtrs);
        } else {msgObj.dispMessage("SOM_MapManager::"+name,"initMapFtrVisAras","Display window doesn't exist, can't build map visualization image arrays; ignoring.", MsgCodes.warning2);}
    }//initMapAras

    //given pixel location relative to upper left corner of map, return map node float - this measures actual distance in map node coords
    //so rounding to ints give map node tuple coords, while float gives interp between neighbors
    protected final float[] getMapNodeLocFromPxlLoc(float mapPxlX, float mapPxlY, float sclVal){    return new float[]{(sclVal* mapPxlX * nodeXPerPxl) - .5f, (sclVal* mapPxlY * nodeYPerPxl) - .5f};}    
    
    //val is 0.0f->256.0f
    private final int getDataClrFromFloat(Float val) {
        int ftr = Math.round(val);        
        int clrVal = ((ftr & 0xff) << 16) + ((ftr & 0xff) << 8) + (ftr & 0xff);
        return clrVal;
    }//getDataClrFromFloat
    
//    /**
//     * make color based on ftr value at particular index call this if map is trained on scaled or normed ftr data
//     * @param ftrMap ftr map
//     * @param classIDX index in feature vector we are querying
//     * @return hex clr
//     */
//    private final int getDataClrFromFtrVec(TreeMap<Integer, Float> ftrMap, Integer classIDX) {
//        Float ftrVal = ftrMap.get(classIDX);
////        if(ftrVal == null) {    ftrVal=0.0f;        }
////        if (minFtrValSeen[classIDX] > ftrVal) {minFtrValSeen[classIDX]=ftrVal;}
////        else if (maxFtrValSeen[classIDX] < ftrVal) {maxFtrValSeen[classIDX]=ftrVal;}
//        if(ftrVal == null) {return 0;}
//        float ftrClrRaw = 255.0f *((ftrVal-trainDat_destMin)/trainDat_destDiff);
//        int ftr = Math.round(ftrClrRaw);        
//
//        
//        int clrVal = ((ftr & 0xff) << 16) + ((ftr & 0xff) << 8) + (ftr & 0xff);
//        return clrVal;
//    }//getDataClrFromFtrVec
    
    //set colors of image of umatrix map
    public final void setMapUMatImgClrs() {
        mapCubicUMatrixImg.loadPixels();
        //float[] c;    
        //mapUMatrixImg
        //single threaded exec
        for(int y = 0; y<mapCubicUMatrixImg.height; ++y){
            int yCol = y * mapCubicUMatrixImg.width;
            for(int x = 0; x < mapCubicUMatrixImg.width; ++x){
                //c = getMapNodeLocFromPxlLoc(x, y,mapScaleVal);
                Float valC = getBiCubicInterpUMatVal(getMapNodeLocFromPxlLoc(x, y,mapScaleVal));
                mapCubicUMatrixImg.pixels[x+yCol] = getDataClrFromFloat(valC);
            }
        }
        mapCubicUMatrixImg.updatePixels();    
    }//setMapUMatImgClrs
    //set colors of image of umatrix map
    public final void setMapSegmentImgClrs_UMatrix() {
        if(win!=null) {
            msgObj.dispInfoMessage("SOM_MapManager::"+name, "setMapSegmentImgClrs_UMatrix", "Start building mapUMatrixCubicSegmentsImg for UMatrix Display.");
            //reinitialize map array
            mapUMatrixCubicSegmentsImg = ((ProcessingRenderer)Base_DispWindow.ri).createImage(mapCubicUMatrixImg.width,mapCubicUMatrixImg.height, PConstants.ARGB);
            mapUMatrixCubicSegmentsImg.loadPixels();
            //float[] c;    
            //single threaded exec
            for(int y = 0; y<mapUMatrixCubicSegmentsImg.height; ++y){
                int yCol = y * mapUMatrixCubicSegmentsImg.width;
                for(int x = 0; x < mapUMatrixCubicSegmentsImg.width; ++x){
                    //c = getMapNodeLocFromPxlLoc(x, y,mapScaleVal);
                    int valC = getUMatrixSegementColorAtPxl(getMapNodeLocFromPxlLoc(x, y,mapScaleVal));
                    mapUMatrixCubicSegmentsImg.pixels[x+yCol] = valC;
                }
            }
            mapUMatrixCubicSegmentsImg.updatePixels();
            msgObj.dispInfoMessage("SOM_MapManager::"+name, "setMapSegmentImgClrs_UMatrix", "Finished building mapUMatrixCubicSegmentsImg for UMatrix Display.");
        }
    }//setMapUMatImgClrs
    
    //sets colors of background image of map -- partition pxls for each thread
    public final void setMapImgClrs(){ //mapRndClrImg
        if (win != null) {
            msgObj.dispMessage("SOM_MapManager::"+name, "setMapImgClrs", "Start building all vis imgs for SOM Map Results Display.", MsgCodes.info5);
            for (int i=0;i<mapPerFtrWtImgs.length;++i) {    mapPerFtrWtImgs[i].loadPixels();}//needed to retrieve pixel values
            //build uMatrix image
            setMapUMatImgClrs();
            //build segmentation image based on UMatrix distance
            setMapSegmentImgClrs_UMatrix();
            //check if single threaded
            int numThds = getNumUsableThreads();
            boolean mtCapable = isMTCapable();
            for(int i=0;i<this.map_ftrsMin.length;++i) {
                msgObj.dispInfoMessage("SOM_MapManager::"+name, "setMapImgClrs", "\tidx:"+i+" | min ftr val seen : " + map_ftrsMin[i] + " | Diffs " + this.map_ftrsDiffs[i]);
            }

            //map_ftrsDiffs, 
            //map_ftrsMin
            
            if(mtCapable) {            
                msgObj.dispMessage("SOM_MapManager::"+name, "setMapImgClrs", "Building Feature Map Vis Images in Multiple threads.", MsgCodes.info5);
                //partition into numUsableThreads threads - split x values by this #, use all y values
                int numPartitions = numThds;
                int numXPerPart = mapPerFtrWtImgs[0].width / numPartitions;            
                int numXLastPart = (mapPerFtrWtImgs[0].width - (numXPerPart*numPartitions)) + numXPerPart;
                //List<Future<Boolean>> mapImgFtrs = new ArrayList<Future<Boolean>>();
                List<SOM_FtrMapVisImgBldr> mapImgBuilders = new ArrayList<SOM_FtrMapVisImgBldr>();
                int[] xVals = new int[] {0,0};
                int[] yVals = new int[] {0,mapPerFtrWtImgs[0].height};
                //each thread builds columns of every map
                for (int i=0; i<numPartitions-1;++i) {    
                    xVals[1] += numXPerPart;
                    mapImgBuilders.add(new SOM_FtrMapVisImgBldr(this,curMapTrainFtrType,  mapPerFtrWtImgs, xVals, yVals,map_ftrsMin,map_ftrsDiffs, mapScaleVal));
                    xVals[0] = xVals[1];                
                }
                //last one
                xVals[1] += numXLastPart;
                mapImgBuilders.add(new SOM_FtrMapVisImgBldr(this,curMapTrainFtrType, mapPerFtrWtImgs, xVals, yVals,map_ftrsMin,map_ftrsDiffs, mapScaleVal));
                invokeSOMFtrDispBuild(mapImgBuilders);
                //try {mapImgFtrs = ri.th_exec.invokeAll(mapImgBuilders);for(Future<Boolean> f: mapImgFtrs) { f.get(); }} catch (Exception e) { e.printStackTrace(); }                    
            } else {
                msgObj.dispMessage("SOM_MapManager::"+name, "setMapImgClrs", "Building Feature Map Vis Images in a Single thread.", MsgCodes.info5);
                //single threaded exec
                for(int y = 0; y<mapPerFtrWtImgs[0].height; ++y){
                    int yCol = y * mapPerFtrWtImgs[0].width;
                    for(int x = 0; x < mapPerFtrWtImgs[0].width; ++x){
                        int pxlIDX = x+yCol;
                        //c = getMapNodeLocFromPxlLoc(x, y,mapScaleVal);
                        TreeMap<Integer, Float> clrftrs = getInterpFtrs(getMapNodeLocFromPxlLoc(x, y,mapScaleVal),curMapTrainFtrType,1.0f, 255.0f);
                        for (Integer ftrIDX : clrftrs.keySet()) {
                            Float ftrVal = clrftrs.get(ftrIDX);
                            if((ftrVal == null) || (map_ftrsDiffs[ftrIDX] == 0)) {mapPerFtrWtImgs[ftrIDX].pixels[pxlIDX] = 0;}
                            else {
                                float ftrClrRaw = 255.0f *((ftrVal-map_ftrsMin[ftrIDX])/map_ftrsDiffs[ftrIDX]);
                                int ftr = Math.round(ftrClrRaw);
                                mapPerFtrWtImgs[ftrIDX].pixels[pxlIDX] = ((ftr & 0xff) << 16) + ((ftr & 0xff) << 8) + (ftr & 0xff);
    
                            }
                        }
                    }
                }
            }
            for (int i=0;i<mapPerFtrWtImgs.length;++i) {    mapPerFtrWtImgs[i].updatePixels();        }
            msgObj.dispMessage("SOM_MapManager::"+name, "setMapImgClrs", "Finished building all vis imgs | Threading : " + (mtCapable ? "Multi ("+numThds+")" : "Single" ), MsgCodes.info5);} 
        else {msgObj.dispMessage("SOM_MapManager::"+name,"setMapImgClrs","Display window doesn't exist, can't build visualization images; ignoring.", MsgCodes.warning2);}
    }//setMapImgClrs
    
    protected abstract int _getNumSecondaryMaps();
    //only appropriate if using UI
    public void setSaveLocClrImg(boolean val) {if (win != null) { win.setPrivFlag(SOM_MapUIWin.saveLocClrImgIDX,val);}}
    //whether or not distances between two datapoints assume that absent features in smaller-length datapoints are 0, or to ignore the values in the larger datapoints
    public abstract void setMapExclZeroFtrs(boolean val);

    //take existing map and use U-Matrix-distances to determine segment membership.Large distances > thresh (around .7) mean nodes are on a boundary
    public final void buildUMatrixSegmentsOnMap() {//need to find closest
        if (nodeInSegUMatrixDistThresh == mapMadeWithUMatrixSegThresh) {return;}
        if ((MapNodes == null) || (MapNodes.size() == 0)) {return;}
        msgObj.dispMessage("SOM_MapManager::"+name,"buildSegmentsOnMap","Started building UMatrix Distance-based cluster map", MsgCodes.info5);    
        //clear existing segments 
        for (SOM_MapNode ex : MapNodes.values()) {ex.clearUMatrixSeg();}
        UMatrix_Segments.clear();
        SOM_UMatrixSegment seg;
        for (SOM_MapNode ex : MapNodes.values()) {
            seg = new SOM_UMatrixSegment(this, nodeInSegUMatrixDistThresh);
            if(seg.doesMapNodeBelongInSeg(ex)) {
                seg.addMapNodeToSegment(ex, MapNodes);        //this does dfs
                UMatrix_Segments.add(seg);                
            }
        }        
        mapMadeWithUMatrixSegThresh = nodeInSegUMatrixDistThresh;
        if(win!=null) {setMapSegmentImgClrs_UMatrix();}
        msgObj.dispMessage("SOM_MapManager::"+name,"buildSegmentsOnMap","Finished building UMatrix Distance-based cluster map", MsgCodes.info5);            
    }//buildUMatrixSegmentsOnMap()
    
    //build feature-based segments on map - will overlap
    public final void buildFtrWtSegmentsOnMap() {
        if (nodeInSegFtrWtDistThresh == mapMadeWithFtrWtSegThresh) {return;}        
        if ((MapNodes == null) || (MapNodes.size() == 0)) {return;}
        msgObj.dispMessage("SOM_MapManager::"+name,"buildFtrWtSegmentsOnMap","Started building feature-weight-based cluster map", MsgCodes.info5);    
        //clear existing segments 
        for (SOM_MapNode ex : MapNodes.values()) {ex.clearFtrWtSeg();}
        //for every feature IDX, for every map node
        SOM_FtrWtSegment ftrSeg;
        FtrWt_Segments.clear();
        for(int ftrIdx = 0; ftrIdx<PerFtrHiWtMapNodes.length;++ftrIdx) {
            //build 1 segment per ftr idx
            ftrSeg = new SOM_FtrWtSegment(this, ftrIdx);
            //FtrWtSegments.add(ftrSeg);
            FtrWt_Segments.put(ftrIdx, ftrSeg);
            for(SOM_MapNode ex : MapNodes.values()) {
                if(ftrSeg.doesMapNodeBelongInSeg(ex)) {                    ftrSeg.addMapNodeToSegment(ex, MapNodes);        }//this does dfs to find neighbors who share feature value     
            }            
        }
        mapMadeWithFtrWtSegThresh = nodeInSegFtrWtDistThresh;
        //if(win!=null) {win.setMapSegmentImgClrs_UMatrix();}
        msgObj.dispMessage("SOM_MapManager::"+name,"buildFtrWtSegmentsOnMap","Finished building feature-weight-based cluster map", MsgCodes.info5);            
    }//buildFtrWtSegmentsOnMap
    
    /**
     * build class-based segments on map
     */
    protected final void buildClassSegmentsOnMap() {    
        if ((MapNodes == null) || (MapNodes.size() == 0)) {return;}
        String descStr = getClassSegMappingDescrStr();
        getMsgObj().dispMessage("SOM_MapManager::"+name,"buildClassSegmentsOnMap","Started building " + descStr, MsgCodes.info5);    
        //clear existing segments 
        for (SOM_MapNode ex : MapNodes.values()) {ex.clearClassSeg();}
        Class_Segments.clear();
        MapNodesWithMappedClasses.clear();
        MapNodeClassProbs.clear();        
        ConcurrentSkipListMap<Tuple<Integer,Integer>, Float> tmpMapOfNodeProbs = new ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>();
        SOM_ClassSegment classSeg;
        //class labels are determined by instancing application - should be array of label values
        Integer[] allTrainClasses = getAllClassLabels();
        //check every map node for every class to see if it has class membership
        for(int clsIdx = 0; clsIdx<allTrainClasses.length;++clsIdx) {        //build class segment for every class
            Integer cls = allTrainClasses[clsIdx];
            classSeg = new SOM_ClassSegment(this, cls);
            Class_Segments.put(cls,classSeg);
            for(SOM_MapNode ex : MapNodes.values()) {if(classSeg.doesMapNodeBelongInSeg(ex)) {    classSeg.addMapNodeToSegment(ex, MapNodes);        }}//addMapNodeToSegment performs DFS to find neighbors who share segment membership     
            Collection<SOM_MapNode> mapNodesForClass = classSeg.getAllMapNodes();
            MapNodesWithMappedClasses.put(cls, mapNodesForClass);
            //getMsgObj().dispMessage("Straff_SOMMapManager","buildClassSegmentsOnMap","Class : " + cls + " has " + mapNodesForClass.size()+ " map nodes in its segment.", MsgCodes.info5);            
            tmpMapOfNodeProbs = new ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>();
            for(SOM_MapNode mapNode : mapNodesForClass) {        tmpMapOfNodeProbs.put(mapNode.mapNodeCoord, mapNode.getClassProb(cls));}
            MapNodeClassProbs.put(cls, tmpMapOfNodeProbs);
        }
        
        getMsgObj().dispMessage("SOM_MapManager::"+name,"buildClassSegmentsOnMap","Finished building "+ descStr +" : "+ MapNodesWithMappedClasses.size()+" classes have map nodes mapped to them.", MsgCodes.info5);            
    }//buildClassSegmentsOnMap    

    /**
     * build category-based segments on map
     */
    protected final void buildCategorySegmentsOnMap() {        
        if ((MapNodes == null) || (MapNodes.size() == 0)) {return;}
        String descStr = getCategorySegMappingDescrStr();
        getMsgObj().dispMessage("SOM_MapManager::"+name,"buildCategorySegmentsOnMap","Started building " + descStr, MsgCodes.info5);    
        //clear existing segments 
        for (SOM_MapNode ex : MapNodes.values()) {ex.clearCategorySeg();}
        Category_Segments.clear();
        MapNodesWithMappedCategories.clear();
        MapNodeCategoryProbs.clear();        
        ConcurrentSkipListMap<Tuple<Integer,Integer>, Float> tmpMapOfNodeProbs = new ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>();
        SOM_CategorySegment catSeg;
        //category labels are determined by instancing application
        Integer[] allTrainCategories = getAllCategoryLabels();        
        
        for(int catIdx = 0; catIdx<allTrainCategories.length;++catIdx) {        //build category segment for every category label
            Integer cat = allTrainCategories[catIdx];
            catSeg = new SOM_CategorySegment(this, cat);
            Category_Segments.put(cat,catSeg);
            for(SOM_MapNode ex : MapNodes.values()) {if(catSeg.doesMapNodeBelongInSeg(ex)) {        catSeg.addMapNodeToSegment(ex, MapNodes);}}//addMapNodeToSegment performs DFS to find neighbors who share segment membership     
                
            Collection<SOM_MapNode> mapNodesForCat = catSeg.getAllMapNodes();
            MapNodesWithMappedCategories.put(cat, mapNodesForCat);
            //getMsgObj().dispMessage("SOM_MapManager::"+name,"buildCategorySegmentsOnMap","Category : " + cat + " has " + mapNodesForCat.size()+ " map nodes in its segment.", MsgCodes.info5);
            tmpMapOfNodeProbs = new ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>();
            for(SOM_MapNode mapNode : mapNodesForCat) {    tmpMapOfNodeProbs.put(mapNode.mapNodeCoord, mapNode.getCategoryProb(cat));    }
            MapNodeCategoryProbs.put(cat, tmpMapOfNodeProbs);
        }
        getMsgObj().dispMessage("SOM_MapManager::"+name,"buildCategorySegmentsOnMap","Finished building " + descStr + " : " + MapNodesWithMappedCategories.size() + " categories have map nodes mapped to them.", MsgCodes.info5);            
    }//buildCategorySegmentsOnMap    
    
    public ConcurrentSkipListMap<Tuple<Integer,Integer>, Float> getMapNodeClassProbsForClass(Integer cls){return MapNodeClassProbs.get(cls);}        
    public ConcurrentSkipListMap<Tuple<Integer,Integer>, Float> getMapNodeCategoryProbsForCategory(Integer cat){return MapNodeCategoryProbs.get(cat);}    

    
    
    /**
     * return the class labels used for the classification of training examples to 
     * their bmus.  bmus then represent a probability distribution of class membership
     * @return
     */
    protected abstract Integer[] getAllClassLabels();
    /**
     * display message relating to class segments
     * @return
     */
    protected abstract String getClassSegMappingDescrStr();
    /**
     * return the category labels used for the classification of training examples to 
     * their bmus.  bmus then represent a probability distribution of category membership
     * @return
     */
    protected abstract Integer[] getAllCategoryLabels();
    /**
     * display message relating to category segments
     * @return
     */
    protected abstract String getCategorySegMappingDescrStr();
    
    ////////////////////////////////
    // segments and segment reporting    
    //save passed segments to passed segment report directory    
    /**
     * Save segment mappings
     */
    public void saveAllSegment_BMUReports() {
        if(!getSOMMapNodeDataIsLoaded()) {
            msgObj.dispMessage("SOM_MapManager::"+name,"saveAllSegment_BMUReports","SOM not yet loaded/mapped, so cannot build segment report.  Aborting.", MsgCodes.info5);
            return;
        }
        msgObj.dispMessage("SOM_MapManager::"+name,"saveAllSegment_BMUReports","Start saving all segment-to-bmu mapping data.", MsgCodes.info5);
        projConfigData.saveSOMUsedForSegmentReport();
        saveClassSegment_BMUReport();
        saveCategorySegment_BMUReport();
        saveFtrWtSegment_BMUReport();
        saveAllSegment_BMUReports_Indiv();
        msgObj.dispMessage("SOM_MapManager::"+name,"saveAllSegment_BMUReports","Finished saving all segment-to-bmu mapping data.", MsgCodes.info5);
    }
    
    /**
     * save all segment reports based on instancing-app-specific segments
     */
    protected abstract void saveAllSegment_BMUReports_Indiv();
    public void saveClassSegment_BMUReport(){    
        if((null==Class_Segments) || (Class_Segments.size()==0)) {
            msgObj.dispMessage("SOM_MapManager::"+name,"saveClassSegment_BMUReport","Class Segments Not yet built, so cannot save report.  Aborting", MsgCodes.info5);
            return;
        }
        String classFileNamePrefix = projConfigData.getClassSegmentFileNamePrefix();
        msgObj.dispMessage("SOM_MapManager::"+name,"saveClassSegment_BMUReport","Start saving "+Class_Segments.size()+" class segments", MsgCodes.info5);
        _saveSegmentReports(Class_Segments, classFileNamePrefix);
        _saveBMU_SegmentReports("class", classFileNamePrefix);
        
        msgObj.dispMessage("SOM_MapManager::"+name,"saveClassSegment_BMUReport","Finished saving "+Class_Segments.size()+" class segments", MsgCodes.info5);
    }
    public void saveCategorySegment_BMUReport(){    
        if((null==Category_Segments) || (Category_Segments.size()==0)) {
            msgObj.dispMessage("SOM_MapManager::"+name,"saveCategorySegment_BMUReport","Category Segments Not yet built, so cannot save report.  Aborting", MsgCodes.info5);
            return;
        }
        msgObj.dispMessage("SOM_MapManager::"+name,"saveCategorySegment_BMUReport","Start saving "+Category_Segments.size()+" category segments", MsgCodes.info5);
        String catFileNamePrefix = projConfigData.getCategorySegmentFileNamePrefix();
        _saveSegmentReports(Category_Segments, catFileNamePrefix);
        _saveBMU_SegmentReports("category", catFileNamePrefix);
        msgObj.dispMessage("SOM_MapManager::"+name,"saveCategorySegment_BMUReport","Finished saving "+Category_Segments.size()+" category segments", MsgCodes.info5);
    }
    public void saveFtrWtSegment_BMUReport(){        
        buildFtrWtSegmentsOnMap();
        String ftrWtFileNamePrefix = projConfigData.getFtrWtSegmentFileNamePrefix();
        msgObj.dispMessage("SOM_MapManager::"+name,"saveFtrWtSegment_BMUReport","Start saving " + FtrWt_Segments.size() + " Feature weight segments.", MsgCodes.info5);
        _saveSegmentReports(FtrWt_Segments, ftrWtFileNamePrefix);
        _saveBMU_SegmentReports("ftrwt", ftrWtFileNamePrefix);
        msgObj.dispMessage("SOM_MapManager::"+name,"saveFtrWtSegment_BMUReport","Finished saving " + FtrWt_Segments.size() + " Feature weight segments.", MsgCodes.info5);    
    }
    
    
    /**
     * save specified segment report information - per-segment file of bmu participation and # of participating examples per bmu (if appropriate)
     * @param _segments map of segments to save report for
     * @param _fileNamePrefix file name prefix used to name the files
     */
    protected void _saveSegmentReports(TreeMap<Integer, SOM_MappedSegment> _segments, String _fileNamePrefix) {
        if((null==_segments) || (_segments.size()==0)) {return;}
        int numFrmt = (int) (Math.log10(_segments.size())) + 1;
        String frmtStr = "%0"+numFrmt+"d";
        ArrayList<String> csvDataToMap;
        for(Integer segKey : _segments.keySet()) {
            SOM_MappedSegment seg = _segments.get(segKey);
            String fileName = _fileNamePrefix + "_"+ String.format(frmtStr, segKey) + ".csv";            
            csvDataToMap = seg.buildBMUMembership_CSV();
            fileIO.saveStrings(fileName, csvDataToMap);            
        }        
    }//_saveSegmentReports
    
    /**
     * save specified segment data for all bmus - holds per-segment-ID probabilities/weights for each bmu, keyed by BMU map loc
     * @param _segmentType
     * @param _fileNamePrefix
     */
    protected void _saveBMU_SegmentReports(String _segmentType, String _fileNamePrefix) {
        String fileName = _fileNamePrefix + "_All_BMUs.csv";    
        ArrayList<String> outStrs = new ArrayList<String>();
        outStrs.add(MapNodes.get(MapNodes.firstKey()).getSegment_Hdr_CSVStr(_segmentType));
        for(SOM_MapNode bmu : MapNodes.values()) {        outStrs.add(bmu.getSegment_CSVStr(_segmentType));    }
        fileIO.saveStrings(fileName, outStrs);    
    }//_saveBMU_SegmentReports
    
    /**
     * save example to bmu mappings for passed array of example data
     * @param exData examples to save bmu mappings for
     * @param dataTypName
     */
    protected void saveExamplesToBMUMappings(SOM_Example[] exData, String dataTypName, int _rawNumPerParition) {
        msgObj.dispMessage("SOM_MapManager::"+name,"saveExamplesToBMUMappings","Start Saving " +exData.length + " "+dataTypName+" bmu mappings to file.", MsgCodes.info5);
        String _fileNamePrefix = projConfigData.getExampleToBMUFileNamePrefix(dataTypName);
        if(exData.length > 0) {
            SOM_SaveExToBMUs_Runner saveRunner = new SOM_SaveExToBMUs_Runner(this, th_exec, exData, dataTypName, false, _fileNamePrefix,_rawNumPerParition);
            saveRunner.runMe();                
        } else {            msgObj.dispMessage("SOM_MapManager::"+name,"saveExamplesToBMUMappings","No "+dataTypName+" examples so cannot save bmus. Aborting.", MsgCodes.warning5);    return;    }
        msgObj.dispMessage("SOM_MapManager::"+name,"saveExamplesToBMUMappings","Finished Saving " +exData.length + " "+dataTypName+" bmu mappings to file.", MsgCodes.info5);
    }//saveExamplesToBMUMappings
    
    ////////////////////////////////
    // end segments and segment reporting    
    
    
 
    /////////////////////////////////////
    // map node management - map nodes are represented by SOMExample objects called SOMMapNodes
    // and are classified as either having examples that map to them (they are bmu's for) or not having any
    //   furthermore, they can have types of examples mapping to them - training, product, validation

    /**
     * build the array of data to be clustered by the trained map
     */
    protected abstract void buildValidationDataAra();
    
    /**
     * products are zone/segment descriptors corresponding to certain feature configurations
     */
    protected abstract void setProductBMUs();
    
    //once map is built, find bmus on map for each test data example
    protected final void setTestBMUs() {    _setExamplesBMUs(testData, "Testing", SOM_ExDataType.Testing,testDataMappedIDX);    }//setTestBMUs    
    //once map is built, find bmus on map for each validation data example
    protected final void setValidationDataBMUs() {_setExamplesBMUs(validationData, "Validation", SOM_ExDataType.Validation,validateDataMappedIDX);}//setValidationDataBMUs
    
    //set examples - either test data or validation data
    protected void _setExamplesBMUs(SOM_Example[] exData, String dataTypName, SOM_ExDataType dataType, int _rdyToSaveFlagIDX) {
        msgObj.dispMessage("SOM_MapManager::"+name,"_setExamplesBMUs","Start Mapping " +exData.length + " "+dataTypName+" data to best matching units.", MsgCodes.info5);
        if(exData.length > 0) {        
            //launch a MapTestDataToBMUs_Runner to manage multi-threaded calc
            SOM_MapExDataToBMUs_Runner rnr = new SOM_MapExDataToBMUs_Runner(this, th_exec, exData, dataTypName, dataType, _rdyToSaveFlagIDX, false);    
            rnr.runMe();
        } else {            msgObj.dispMessage("SOM_MapManager::"+name,"_setExamplesBMUs","No "+dataTypName+" data to map to BMUs. Aborting.", MsgCodes.warning5);    return;    }
        msgObj.dispMessage("SOM_MapManager::"+name,"_setExamplesBMUs","Finished Mapping " +exData.length + " "+dataTypName+" data to best matching units.", MsgCodes.info5);
    }//_setExamplesBMUs
    
    //call 1 time for any particular type of data - all _exs should have their bmu's set by now
    //this will set the bmu lists for each map node to include the mapped examples
    public synchronized void _completeBMUProcessing(SOM_Example[] _exs, SOM_ExDataType _dataType, boolean isMT) {
        msgObj.dispMessage("SOM_MapManager::"+name,"_completeBMUProcessing","Start completion of " +_exs.length + " "+_dataType.getName()+" data bmu mappings - assign to BMU's example collection and finalize.", MsgCodes.info5);
        int dataTypeVal = _dataType.getVal();
        for(SOM_MapNode mapNode : MapNodes.values()){mapNode.clearBMUExs(dataTypeVal);addExToNodesWithNoExs(mapNode, _dataType);}        //must be done synchronously always    
        if(_dataType==SOM_ExDataType.Training) {            for (int i=0;i<_exs.length;++i) {            _exs[i].mapTrainingToBMU(dataTypeVal);    }        } 
        else {                                        for (int i=0;i<_exs.length;++i) {            _exs[i].mapToBMU(dataTypeVal);        }        }
        _finalizeBMUProcessing(_dataType);
        msgObj.dispMessage("SOM_MapManager::"+name,"_completeBMUProcessing","Finished completion of " +_exs.length + " "+_dataType.getName()+" data bmu mappings - assign to BMU's example collection and finalize.", MsgCodes.info5);
    }//_completeBMUProcessing
    
    /**
     * this will copy relevant info from map nodes with typeIDX examples that consider them BMU
     * to the closest "non-bmu"(un-mapped) map node that matches their features.
     * only should be performed for training data mappings - this is necessary to make sure that 
     * any validation data that maps to these non-training-data map nodes as bmus have some notion 
     * of similarity to the training data                             
     * 
     * @param withMap set of nodes with natural mappings
     * @param withOutMap set of nodes with no natural mappings - should be << than with map set
     * @param typeIDX type of data
     */
    private void addMappedNodesToEmptyNodes_FtrDist(HashSet<SOM_MapNode> withMap, HashSet<SOM_MapNode> withOutMap, int typeIDX) {
        msgObj.dispMessage("SOM_MapManager::"+name,"addMappedNodesToEmptyNodes_FtrDist","Start assigning " +withOutMap.size() + " map nodes that are not BMUs to any " +SOM_ExDataType.getEnumByIndex(typeIDX).getName() + " examples to have nearest map node to them as BMU.", MsgCodes.info5);        
        msgObj.dispMessage("SOM_MapManager::"+name,"addMappedNodesToEmptyNodes_FtrDist","Start building map of nodes with examples keyed by ftr idx of non-zero ftrs", MsgCodes.info5);        
        Double minSqDist;
        float minMapSqDist, mapSqDist;
        //build a map keyed by ftrIDX of all nodes that have non-zero ftr idx values for the key ftr idx and also have examples mapped to them
        //MapNodesByFtrIDX map can't be used - it holds -all- map nodes, not just those with examples
        TreeMap<Integer, HashSet<SOM_MapNode>> MapNodesWithExByFtrIDX = new TreeMap<Integer, HashSet<SOM_MapNode>>();
        
        //TODO very large maps (100x100) have many nodes that have no examples mapped to them. Need to improve this process
        msgObj.dispMessage("SOM_MapManager::"+name,"addMappedNodesToEmptyNodes_FtrDist","Very large maps (100x100) have many nodes that have no examples mapped to them. Need to improve this process.", MsgCodes.info5);        
        for(SOM_MapNode nodeWithEx : withMap){
            Integer[] nonZeroIDXs = nodeWithEx.getNonZeroIDXs();
            for(Integer idx : nonZeroIDXs) {
                HashSet<SOM_MapNode> nodeSet = MapNodesWithExByFtrIDX.get(idx);
                if(null==nodeSet) {nodeSet = new HashSet<SOM_MapNode>();}
                nodeSet.add(nodeWithEx);
                MapNodesWithExByFtrIDX.put(idx,nodeSet);
            }    
        }
        ArrayList<SOM_MapNode> closestNodeList = new ArrayList<SOM_MapNode>();
        Entry<Double, ArrayList<SOM_MapNode>> closestList;
        msgObj.dispMessage("SOM_MapManager::"+name,"addMappedNodesToEmptyNodes_FtrDist","Finished building map of nodes with examples keyed by ftr idx of non-zero ftrs | Start finding closest mapped nodes by ftr dist to "+withOutMap.size()+" non-mapped nodes.", MsgCodes.info5);        

        //for each map node without training example bmus...
        for(SOM_MapNode emptyNode : withOutMap){//node has no label mappings, so need to determine label        
            //find list of closest nodes based on ftr similarity
            //msgObj.dispMessage("SOM_MapManager::"+name,"addMappedNodesToEmptyNodes_FtrDist","Unmapped node :"+emptyNode.toString(), MsgCodes.info5);        
            closestList = emptyNode.findClosestMapNodes(MapNodesWithExByFtrIDX, emptyNode::getSqDistFromFtrType, SOM_FtrDataType.UNNORMALIZED);            
            minSqDist = closestList.getKey();    
            closestNodeList = closestList.getValue();    
            
            //now find closest actual map node
            SOM_MapNode closestMapNode  = emptyNode;                    //will never be added
            //go through list to find closest map dist node from closest ftr dist nodes in 
            if(closestNodeList.size()==1) {
                closestMapNode = closestNodeList.get(0);        //adds single closest -map- node we know has a label, or itself if none found                
            } else {
                //if more than 1 nodes is closest to the umapped node then find the closest of these in map topology
                minMapSqDist = 1000000.0f;
                if (isToroidal()) {//minimize in-loop if checks
                    for(SOM_MapNode node2 : closestNodeList){                    //this is adding a -map- node
                        mapSqDist = getSqMapDist_torr(node2, emptyNode);            //actual map topology dist - need to handle wrapping!
                        if (mapSqDist < minMapSqDist) {minMapSqDist = mapSqDist; closestMapNode = node2;}
                    }    
                } else {
                    for(SOM_MapNode node2 : closestNodeList){                    //this is adding a -map- node
                        mapSqDist = getSqMapDist_flat(node2, emptyNode);            //actual map topology dist - need to handle wrapping!
                        if (mapSqDist < minMapSqDist) {minMapSqDist = mapSqDist; closestMapNode = node2;}
                    }                    
                }
            }
            
            emptyNode.copyMapNodeExamples(minSqDist, closestMapNode, typeIDX);            //adds single closest -map- node we know has a label, or itself if none found
        }//for each non-mapped node
        msgObj.dispMessage("SOM_MapManager::"+name,"addMappedNodesToEmptyNodes_FtrDist","Finished assigning " +withOutMap.size() + " map nodes that are not BMUs to any "  +SOM_ExDataType.getEnumByIndex(typeIDX).getName() + " examples to have nearest map node to them as BMU.", MsgCodes.info5);
    }//addMappedNodesToEmptyNodes_FtrDist
    
    /**
     * this function will finalize bmus that have examples mapped to them if they have been trained using normalized features.
     * This should only be called after initial load from SOM results of bmu data
     * Since the bmu itself doesn't have a conception of the original magnitude of each training vector, the magnitude needs to be derived
     * from the average mag of all the training examples that mapped to the bmu - will be imprecise, but better than nothing
     */
    public synchronized void _finalizeInitTrainingDataBMUAssignments() {
        for (SOM_MapNode ex : MapNodes.values()) {            ex.finalSetUpFtrs_TrainedWithNormFtrs();}
        //do this after all map nodes have been finalized
        for(SOM_MapNode ex : MapNodes.values()) {            ex.buildMapNodeNeighborSqDistVals(MapNodes);}
        //all map node features are calculated by here
        float[] tmpMapMaxs = initMapMgrMeanMinVar(numTrnFtrs);
        int numEx = MapNodes.size();
        //build feature stats for all map nodes
        for(SOM_MapNode _mapNode : MapNodes.values()) {
            float[] ftrData = _mapNode.getFtrs(_mapNode.getFtrTypeUsedToTrain());
            for(int d = 0; d<numTrnFtrs; ++d){
                map_ftrsMean[d] += ftrData[d];
                tmpMapMaxs[d] = (tmpMapMaxs[d] < ftrData[d] ? ftrData[d]  : tmpMapMaxs[d]);
                map_ftrsMin[d] = (map_ftrsMin[d] > ftrData[d] ? ftrData[d]  : map_ftrsMin[d]);
            }
        }            
        for(int d = 0; d<map_ftrsMean.length; ++d){
            map_ftrsMean[d] /= 1.0f*numEx;
            map_ftrsDiffs[d]=tmpMapMaxs[d]-map_ftrsMin[d];
        }
        //build stats for map nodes
        float diff;
        float[] ftrData ;
        //for every node, now build standardized features 
        for(Tuple<Integer, Integer> key : MapNodes.keySet()){
            SOM_MapNode _mapNode = MapNodes.get(key);
            //accumulate map ftr moments
            ftrData = _mapNode.getFtrs(_mapNode.getFtrTypeUsedToTrain());
            for(int d = 0; d<map_ftrsMean.length; ++d){
                diff = map_ftrsMean[d] - ftrData[d];
                map_ftrsVar[d] += diff*diff;
            }
            setNodesArrayOfMapNodeByFtr(_mapNode);
        }
        for(int d = 0; d<map_ftrsVar.length; ++d){map_ftrsVar[d] /= 1.0f*numEx;}        

    }//_finalizeInitBMUForNormTrainedFtrs
    
    /**
     * this will take the map nodes and build a population based graph image of them, with x being proportional to population, and y being the node ID, sorted from largest pop(top) to least (bottom)
     * call whenever map vals change
     */
    public void setMapNodePopGraphImage() {
        if(win!=null) {
            //msgObj.dispMessage("SOM_MapManager::"+name,"setMapNodePopGraphImage","Start building map node population graph for all examples.", MsgCodes.info5);        
            //all map nodes of som map, sorted by mapped population of training data        
            for(int i=0;i<MapNodesByPopulation.length;++i) {        MapNodesByPopulation[i].clear();}
            int lstTypeIDX = MapNodesByPopulation.length-1;
            for(int i=0;i<lstTypeIDX;++i) {        //for each type of example data
                for(Tuple<Integer,Integer> key : MapNodes.keySet()) {
                    SOM_MapNode node = MapNodes.get(key);
                    Integer ttlForNode = node.getBMUMapNodePopulation(i);
                    ArrayList<Tuple<Integer,Integer>> nodesAtCount = MapNodesByPopulation[i].get(ttlForNode);
                    if(null==nodesAtCount) {nodesAtCount = new ArrayList<Tuple<Integer,Integer>> ();MapNodesByPopulation[i].put(ttlForNode,nodesAtCount);}
                    nodesAtCount.add(key);        
                }
            }
            for(Tuple<Integer,Integer> key : MapNodes.keySet()) {
                SOM_MapNode node = MapNodes.get(key);
                Integer ttlForNode = node.getAllBMUMapNodePopulation();
                ArrayList<Tuple<Integer,Integer>> nodesAtCount = MapNodesByPopulation[lstTypeIDX].get(ttlForNode);
                if(null==nodesAtCount) {nodesAtCount = new ArrayList<Tuple<Integer,Integer>> ();MapNodesByPopulation[lstTypeIDX].put(ttlForNode,nodesAtCount);}
                nodesAtCount.add(key);        
            }
        
            //use MapNodesByPopulation to build PShape
            buildMapNodePopGraphImage();
            //msgObj.dispMessage("SOM_MapManager::"+name,"setMapNodePopGraphImage","Finished building map node population graph for all examples.", MsgCodes.info5);        
        } else {            msgObj.dispMessage("SOM_MapManager::"+name,"setMapNodePopGraphImage","No UI To display Map Node Population Graph, so Aborting.", MsgCodes.info5);            }
    }//setMapNodePopGraphImage
    /**
     * build/rebuild image of map node population graph
     */
    protected void buildMapNodePopGraphImage() {
        if(MapNodesByPopulation == null) {return;}
        if(win!=null) {
            msgObj.dispMessage("SOM_MapManager::"+name,"buildMapNodePopGraphImage","Started building map nod population graph image for all examples with mapNodePopDispThreshPct : "+mapNodePopDispThreshPct+".", MsgCodes.info5);    
            TreeMap<Integer, ArrayList<Tuple<Integer,Integer>>> tmpMapNodesByPopForType;
            int whiteClr = 0xFFFFFFFF, greyClr = 0xFF888888;
            int clrToUse;            
            mapNodePopGraph = new PImage[MapNodesByPopulation.length];
            int numMapNodes = MapNodes.size();
            for(int i=0;i<MapNodesByPopulation.length;++i) {                        
                tmpMapNodesByPopForType = MapNodesByPopulation[i];
                if(0 == tmpMapNodesByPopForType.size()) {continue;}
                Integer largestCount = tmpMapNodesByPopForType.firstKey();        
                mapNodePopDispThreshVals[i]=largestCount*mapNodePopDispThreshPct;

                mapNodePopGraph[i] = ((ProcessingRenderer)Base_DispWindow.ri).createImage(largestCount, numMapNodes, PConstants.ARGB);
                mapNodePopGraph[i].loadPixels();
                int row = 0, pxlIDX = 0;
                for(Integer count : tmpMapNodesByPopForType.keySet()) {
                    ArrayList<Tuple<Integer,Integer>> nodesAtCount = tmpMapNodesByPopForType.get(count);
                    //mapNodePopDispThresh
                    clrToUse = (count > mapNodePopDispThreshVals[i] ? whiteClr : greyClr);
                    for(int j=0;j<nodesAtCount.size();++j) {
                        for(int p = 0;p<count;++p) {    mapNodePopGraph[i].pixels[pxlIDX+p] = clrToUse;}
                        for(int p=count;p<largestCount;++p) {mapNodePopGraph[i].pixels[pxlIDX+p] = 0x00000000;}
                        ++row;
                        pxlIDX = row * largestCount;
                    }                
                }        
                mapNodePopGraph[i].updatePixels();        
            }
    //            
            //msgObj.dispMessage("SOM_MapManager::"+name,"buildMapNodePopGraphImage","Finished building map nod population graph image for all examples.", MsgCodes.info5);        
        } else {            msgObj.dispMessage("SOM_MapManager::"+name,"buildMapNodePopGraphImage","No UI To display Map Node Population Graph, so Aborting.", MsgCodes.info5);            }
    }//buildMapNodePopGraphImage
    
    
    
//    private PShape buildMapNodePopGraphAsPShape(TreeMap<Integer, ArrayList<Tuple<Integer,Integer>>> MapNodesByPopulation) {
//        PShape grph = Base_DispWindow.ri.createShape();
//        grph.beginShape(PConstants.LINES);
//        grph.noFill();
//        grph.stroke(255,255,255,255);
//        grph.strokeWeight(1.0f);
//        int y = 0;
//        for(Integer key : MapNodesByPopulation.keySet()) {
//            grph.vertex(0, y);grph.vertex(key, y);
//            ++y;
//        }
//        grph.endShape();
//        return grph;
//    }

    
    /**
     * finalize the bmu processing - move som nodes that have been mapped to out of the list of nodes that have not been mapped to, copy the closest mapped som node to any som nodes without mappings, finalize all som nodes
     * @param dataType
     */
    public synchronized void _finalizeBMUProcessing(SOM_ExDataType dataType) {
        msgObj.dispMessage("SOM_MapManager::"+name,"_finalizeBMUProcessing","Start finalizing BMU processing for data type : "+ dataType.getName()+".", MsgCodes.info5);        
        
        HashSet<SOM_MapNode> withMap = nodesWithEx.get(dataType), withOutMap = nodesWithNoEx.get(dataType);
        //clear out all nodes that have examples from struct holding no-example map nodes
        //remove all examples that have been mapped to
        for (SOM_MapNode tmpMapNode : withMap) {            withOutMap.remove(tmpMapNode);        }
        
        int typeIDX = dataType.getVal();
        
        //copy closest som node with mapped training examples to each som map node that has none
        if(dataType == SOM_ExDataType.Training) {addMappedNodesToEmptyNodes_FtrDist(withMap,withOutMap,typeIDX);}
        
        //finalize all examples - needs to finalize all nodes to manage the SOMMapNodeBMUExamples for the nodes that have not been mapped to
        //This is what gives each map node the # of examples that have mapped to it
        for(SOM_MapNode node : MapNodes.values()){        node.finalizeAllBmus(typeIDX);    }
        //build image of map node population graph, if win exists
        setMapNodePopGraphImage();
        
        //calculate ftr segments of nodes
        buildFtrWtSegmentsOnMap();
        
        msgObj.dispMessage("SOM_MapManager::"+name,"_finalizeBMUProcessing","Finished finalizing BMU processing for data type : "+ dataType.getName()+".", MsgCodes.info5);        
    }//sa_finalizeBMUProcessing
        
    protected void _dispMappingNotDoneMsg(String callingClass, String callingMethod, String _datType) {
        msgObj.dispMessage(callingClass,callingMethod, "Mapping "+_datType+" examples to BMUs not yet complete so no mappings are being saved - please try again later", MsgCodes.warning4);        
    }
    
    public void clearBMUNodesWithExs(SOM_ExDataType _type) {                            nodesWithEx.get(_type).clear();}
    public void clearBMUNodesWithNoExs(SOM_ExDataType _type) {                            nodesWithNoEx.get(_type).clear();}
    public void addExToNodesWithExs(SOM_MapNode node, SOM_ExDataType _type) {            nodesWithEx.get(_type).add(node);}    
    public void addExToNodesWithNoExs(SOM_MapNode node, SOM_ExDataType _type) {            nodesWithNoEx.get(_type).add(node);}    
    public int getNumNodesWithBMUExs(SOM_ExDataType _type) {return nodesWithEx.get(_type).size();}
    public int getNumNodesWithNoBMUExs(SOM_ExDataType _type) {return nodesWithNoEx.get(_type).size();}
    public HashSet<SOM_MapNode> getNodesWithExOfType(SOM_ExDataType _type){return nodesWithEx.get(_type);}
    public HashSet<SOM_MapNode> getNodesWithNoExOfType(SOM_ExDataType _type){return nodesWithNoEx.get(_type);}
    
    //called when som wts are first loaded
    @SuppressWarnings("unchecked")
    public void initMapNodes() {
        MapNodes = new TreeMap<Tuple<Integer,Integer>, SOM_MapNode>();
        SelectedMapNodes = new TreeMap<Tuple<Integer,Integer>, SOM_MapNode>();
        
        //MapNodesByPopulation = new TreeMap<Integer, ArrayList<Tuple<Integer,Integer>>>[SOM_ExDataType.getNumVals()+1];//1 extra for "total"
        MapNodesByPopulation = new TreeMap[SOM_ExDataType.getNumVals()+1];//1 extra for "total"
        mapNodePopDispThreshVals = new float[MapNodesByPopulation.length];
        for(int i=0;i<MapNodesByPopulation.length;++i) {
            MapNodesByPopulation[i] = new TreeMap<Integer, ArrayList<Tuple<Integer,Integer>>>(new Comparator<Integer>() { @Override public int compare(Integer o1, Integer o2) {   return o2.compareTo(o1);}});
            mapNodePopDispThreshVals[i]=0;
        }
        
        
        //this will hold all map nodes keyed by the ftr idx where they have non-zero weight
        MapNodesByFtrIDX = new TreeMap<Integer, HashSet<SOM_MapNode>>();
        //reset segement holders
        //array of map segments based on UMatrix dist
        UMatrix_Segments = new ArrayList<SOM_MappedSegment>();
        //array of map clusters based on ftr distance
        //FtrWtSegments = new ArrayList<SOM_MapSegment>();
        FtrWt_Segments = new TreeMap<Integer, SOM_MappedSegment>();
        //Map of classes to segment
        Class_Segments = new TreeMap<Integer, SOM_MappedSegment>();
        //map of categories to segment
        Category_Segments = new TreeMap<Integer, SOM_MappedSegment>();
        //map with key being class and with value being collection of map nodes with that class present in mapped examples
        MapNodesWithMappedClasses = new TreeMap<Integer,Collection<SOM_MapNode>>();
        //map with key being category and with value being collection of map nodes with that category present in mapped examples
        MapNodesWithMappedCategories = new TreeMap<Integer,Collection<SOM_MapNode>>();
        //probabilities for each class for each map node
        MapNodeClassProbs = new ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>>();
        //probabilities for each category for each map node
        MapNodeCategoryProbs = new ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>, Float>>();
        //any instance-class specific code to execute when new map nodes are being loaded
        initMapNodesPriv();
    }//initMapNodes()
    protected abstract void initMapNodesPriv();    
    
    private float[] initMapMgrMeanMinVar(int _numTrainFtrs) {
        map_ftrsMean = new float[_numTrainFtrs];
        float[] tmpMapMaxs = new float[_numTrainFtrs];
        map_ftrsMin = new float[_numTrainFtrs];
        for(int l=0;l<map_ftrsMin.length;++l) {map_ftrsMin[l]=10000.0f;}//need to init to big number to get accurate min
        map_ftrsVar = new float[_numTrainFtrs];
        map_ftrsDiffs = new float[_numTrainFtrs];        
        return tmpMapMaxs;
    }//_initMapMgrMeanMinVar
        
    //process map node's ftr vals, add node to map, and add node to struct without any training examples (initial state for all map nodes)
    //public void addToMapNodes(Tuple<Integer,Integer> key, SOM_MapNode mapNode, float[] tmpMapMaxs, int numTrainFtrs) {
    public void addToMapNodes(Tuple<Integer,Integer> key, SOM_MapNode mapNode) {
        MapNodes.put(key, mapNode);    
        //set map nodes by ftr idx
        Integer[] nonZeroIDXs = mapNode.getNonZeroIDXs();
        for(Integer idx : nonZeroIDXs) {
            HashSet<SOM_MapNode> nodeSet = MapNodesByFtrIDX.get(idx);
            if(null==nodeSet) {nodeSet = new HashSet<SOM_MapNode>();}
            nodeSet.add(mapNode);
            MapNodesByFtrIDX.put(idx,nodeSet);
        }    
        //initialize : add all nodes to set, will remove nodes when they get mappings
        addExToNodesWithNoExs(mapNode, SOM_ExDataType.Training);//nodesWithNoTrainEx.add(dpt);                //initialize : add all nodes to set, will remove nodes when they get mappings
    }//addToMapNodes
    
    
    //returns sq distance between two map locations (using actual map distance, not feature similarity) - needs to handle wrapping if map built torroidally
    private float getSqMapDist_flat(SOM_MapNode a, SOM_MapNode b){        return (a.mapLoc._SqrDist(b.mapLoc));    }//    
    //returns sq distance between two map locations - needs to handle wrapping if map built torroidally
    private float getSqMapDist_torr(SOM_MapNode a, SOM_MapNode b){
        float 
            oldXa = a.mapLoc.x - b.mapLoc.x, oldXaSq = oldXa*oldXa,            //a is to right of b
            newXa = oldXa + mapDims[0], newXaSq = newXa*newXa,    //a is to left of b
            oldYa = a.mapLoc.y - b.mapLoc.y, oldYaSq = oldYa*oldYa,            //a is below b
            newYa = oldYa + mapDims[1], newYaSq = newYa*newYa;    //a is above b
        return (oldXaSq < newXaSq ? oldXaSq : newXaSq ) + (oldYaSq < newYaSq ? oldYaSq : newYaSq);
    }//
    
    /**
     * build all neighborhood values for UMatrix
     */
    public void buildAllMapNodeNeighborhood_UMatrixDists(float _diff, float _min) {
        uMatDist_Min = _min;
        uMatDist_Diff = _diff;
        for(SOM_MapNode ex : MapNodes.values()) {    ex.scaleUMatDist(_diff, _min);}
        for(SOM_MapNode ex : MapNodes.values()) {    ex.buildMapNodeNeighborUMatrixVals(MapNodes);}
    } //ex.buildMapNodeNeighborSqDistVals(MapNodes);    }}

    //set stats of map nodes based on passed features
    public void setMapNodeStatsFromFtr(float[] ftrData, float[] tmpMapMaxs, int numTrainFtrs) {
        for(int d = 0; d<numTrainFtrs; ++d){
            map_ftrsMean[d] += ftrData[d];
            tmpMapMaxs[d] = (tmpMapMaxs[d] < ftrData[d] ? ftrData[d]  : tmpMapMaxs[d]);
            map_ftrsMin[d] = (map_ftrsMin[d] > ftrData[d] ? ftrData[d]  : map_ftrsMin[d]);
        }
    }//setMapNodeStatsFromFtr

    @SuppressWarnings("unchecked")
    public void initPerFtrMapOfNodes(int numFtrs) {
        PerFtrHiWtMapNodes = new TreeMap[numFtrs];
        for (int i=0;i<PerFtrHiWtMapNodes.length; ++i) {PerFtrHiWtMapNodes[i] = new TreeMap<Float,ArrayList<SOM_MapNode>>(new Comparator<Float>() { @Override public int compare(Float o1, Float o2) {   return o2.compareTo(o1);}});}
    }//initPerFtrMapOfNodes    
    
    /**
     * put a map node in PerFtrHiWtMapNodes per-ftr array for the ftrs that it has non-zero values in
     * @param mapNode node to put in array
     */
    public void setNodesArrayOfMapNodeByFtr(SOM_MapNode mapNode) {
        TreeMap<Integer, Float> ftrNormMap = mapNode.getCurrentFtrMap(SOM_FtrDataType.FTR_NORM);        //using feature-based normalized values, these should always be between 0 and 1 for all features
        for (Integer ftrIDX : ftrNormMap.keySet()) {
            Float ftrVal = ftrNormMap.get(ftrIDX);
            ArrayList<SOM_MapNode> nodeList = PerFtrHiWtMapNodes[ftrIDX].get(ftrVal);
            if (nodeList== null) {            nodeList = new ArrayList<SOM_MapNode>();        }
            nodeList.add(mapNode);
            PerFtrHiWtMapNodes[ftrIDX].put(ftrVal, nodeList);
        }        
    }//setMapNodeFtrStr
        
    //after all map nodes are loaded
    public void finalizeMapNodeFtrWts(int _numTrainFtrs, int _numEx) {
        //initialize array of images to display map of particular feature with
        initMapFtrVisAras(_numTrainFtrs);        
        //reset this to manage all map nodes
        initPerFtrMapOfNodes(_numTrainFtrs);
        setNumTrainFtrs(_numTrainFtrs); 
    }//finalizeMapNodes

    //build a map node that is formatted specifically for this project
    public abstract SOM_MapNode buildMapNode(Tuple<Integer,Integer>mapLoc, SOM_FtrDataType _ftrTypeUsedToTrain,  String[] tkns);

    ///////////////////////////
    // end build and manage mapNodes 
    
    ///////////////////////////
    // map data <--> ui  update code
    
    /**
     * update map descriptor Float values from UI
     * @param key : key descriptor of value
     * @param val
     */
    public void updateMapDatFromUI_Integer(String key, Integer val) {    projConfigData.updateMapDat_Integer(key,val, true, false);    }//updateMapDatFromUI_Integer
    
    /**
     * update map descriptor Float values
     * @param key : key descriptor of value
     * @param val
     */
    public void updateMapDatFromUI_Float(String key, Float val) {    projConfigData.updateMapDat_Float(key,val, true, false);    }//updateMapDatFromUI_Float
    
    /**
     * update map descriptor String values
     * @param key : key descriptor of value
     * @param val
     */
    public void updateMapDatFromUI_String(String key, String val) {    projConfigData.updateMapDat_String(key,val, true, false);    }//updateMapDatFromUI_String
    
    
    /**
     * update UI from map data change (called from projConfig only
     * @param key : key descriptor of value
     * @param val
     */
    public void updateUIMapData_Integer(String key, Integer val) {    mapUIAPI.updateUIFromMapDat_Integer(key, val);}//updateUIMapData_Integer
    
    /**
     * update UI from map data change (called from projConfig only
     * @param key : key descriptor of value
     * @param val
     */
    public void updateUIMapData_Float(String key, Float val) {        mapUIAPI.updateUIFromMapDat_Float(key, val);}//updateUIMapData_Float
    
    /**
     * update UI from map data change (called from projConfig only
     * @param key : key descriptor of value
     * @param val
     */
    public void updateUIMapData_String(String key, String val) {    mapUIAPI.updateUIFromMapDat_String(key, val);}//updateUIMapData_String    
    
    ///////////////////////////
    // end map data <--> ui  update code
    
    
    ///////////////////////////
    // manage mins/diffs    
    
    private Float[] _convStrAraToFloatAra(String[] tkns) {
        ArrayList<Float> tmpData = new ArrayList<Float>();
        for(int i =0; i<tkns.length;++i){tmpData.add(Float.parseFloat(tkns[i]));}
        return tmpData.toArray(new Float[0]);        
    }//_convStrAraToFloatAra    
    
    /**
     * read file with scaling/min values for Map to convert data back to original feature space - single row of data
     * @param fileName
     * @return
     */
    private Float[][] loadCSVSrcDataPoint(String fileName){        
        if(fileName.length() < 1){return null;}
        String [] strs= fileIO.loadFileIntoStringAra(fileName, "Loaded data file : "+fileName, "Error reading file : "+fileName);
        if(strs==null){return null;}    
        //line 0 is # of entries in array
        int numEntries = Integer.parseInt(strs[0].trim());
        Float[][] resAra = new Float[numEntries][];
        for(int i=0;i<numEntries;++i) {        resAra[i] = _convStrAraToFloatAra(strs[i+1].split(csvFileToken));    }
        return resAra;
    }//loadCSVData
    
    public boolean loadDiffsMins() {
        String diffsFileName = projConfigData.getSOMMapDiffsFileName(), minsFileName = projConfigData.getSOMMapMinsFileName();
        //load normalizing values for datapoints in weights - differences and mins, used to scale/descale training and map data
        diffsVals = loadCSVSrcDataPoint(diffsFileName);
        if((null==diffsVals) || (diffsVals.length < 1)){msgObj.dispMessage("SOM_MapManager::"+name,"loadDiffsMins","!!error reading diffsFile : " + diffsFileName, MsgCodes.error2); return false;}
        minsVals = loadCSVSrcDataPoint(minsFileName);
        if((null==minsVals)|| (minsVals.length < 1)){msgObj.dispMessage("SOM_MapManager::"+name,"loadDiffsMins","!!error reading minsFile : " + minsFileName, MsgCodes.error2); return false;}    
        return true;
    }//loadMinsAndDiffs()
    
    /**
     * call after training data feature vectors have been constructed, and get the resultant 
     * mins and diffs of the training data : 2d array to manage mins and diffs for multiple feature 
     * types/feature vector configs.  Usually mins and diffs array each will be 1 x # of ftrs;
     * @param _mins - 2 d array of floats : first dof is type of ftrs used to derive mins 
     *                 (usually this will be only idx); 2nd dof is ftr idx
     * @param _diffs - 2 d array of floats : first dof is type of ftrs used to derive diffs 
     *                 (usually this will be only idx); 2nd dof is ftr idx
     */
    protected void setMinsAndDiffs(Float[][] _mins, Float[][] _diffs) {
        String dispStr = "MinsVals and DiffsVall being set : Mins is 2d ara of len : " + _mins.length + " with each array of len : [";        
        for(int i=0;i<_mins.length-1;++i) {dispStr+= " "+i+":"+_mins[i].length+",";}                            
        dispStr+= " "+(_mins.length-1)+":"+_mins[_mins.length-1].length+"] | Diffs is 2D ara of len : "+_diffs.length + " with each array of len : [";
        for(int i=0;i<_diffs.length-1;++i) {dispStr+= " "+i+":"+_diffs[i].length+",";}
        dispStr+=" "+(_diffs.length-1)+":"+_diffs[_diffs.length-1].length+"]";
        msgObj.dispMessage("SOM_MapManager::"+name,"setMinsAndDiffs",dispStr, MsgCodes.info2);
        minsVals = _mins;
        diffsVals = _diffs;
    }//setMinsAndDiffs

    /**
     * save mins and diffs of current training data
     */
    protected void saveMinsAndDiffs() {
        msgObj.dispMessage("SOM_MapManager::"+name,"saveMinsAndDiffs","Begin Saving Mins and Diffs Files", MsgCodes.info1);
        //save mins/maxes so this file data be reconstructed
        //save diffs and mins - csv files with each field value sep'ed by a comma
        //boundary region for training data
        String[] minsAra = new String[minsVals.length+1];
        String[] diffsAra = new String[diffsVals.length+1];    
        minsAra[0]=""+minsVals.length;
        diffsAra[0] = ""+diffsVals.length;
        for(int i =0; i<minsVals.length; ++i){
            minsAra[i+1] = "";
            diffsAra[i+1] = "";
            for(int j =0; j<minsVals[i].length; ++j){        minsAra[i+1] += String.format("%1.7g", minsVals[i][j]) + ",";    }
            for(int j =0; j<diffsVals[i].length; ++j){        diffsAra[i+1] += String.format("%1.7g", diffsVals[i][j]) + ",";    }
        }
        String minsFileName = projConfigData.getSOMMapMinsFileName();
        String diffsFileName = projConfigData.getSOMMapDiffsFileName();                
        fileIO.saveStrings(minsFileName,minsAra);        
        fileIO.saveStrings(diffsFileName,diffsAra);        
        msgObj.dispMessage("SOM_MapManager::"+name,"saveMinsAndDiffs","Finished Saving Mins and Diffs Files", MsgCodes.info1);    
    }//saveMinsAndDiffs
    ///////////////////////////
    // end manage mins/diffs    
    
    
    ///////////////////////////////////////
    // ftr interp routines    
    //return interpolated feature vector on map at location given by x,y, where x,y is float location of map using mapnodes as integral locations
    //only uses training features here
    public TreeMap<Integer, Float> getInterpFtrs(float[] c, SOM_FtrDataType _ftrType, float rowMult, float colMult){
        float xColShift = (c[0]+mapNodeCols), 
                yRowShift = (c[1]+mapNodeRows), 
                xInterp = (xColShift) %1, 
                yInterp = (yRowShift) %1;
        int xInt = (int) Math.floor(xColShift)%mapNodeCols, yInt = (int) Math.floor(yRowShift)%mapNodeRows, xIntp1 = (xInt+1)%mapNodeCols, yIntp1 = (yInt+1)%mapNodeRows;        //assume torroidal map        
        //always compare standardized feature data in test/train data to standardized feature data in map
        TreeMap<Integer, Float> LowXLowYFtrs = MapNodes.get(new Tuple<Integer, Integer>(xInt,yInt)).getCurrentFtrMap(_ftrType), LowXHiYFtrs= MapNodes.get(new Tuple<Integer, Integer>(xInt,yIntp1)).getCurrentFtrMap(_ftrType),
                 HiXLowYFtrs= MapNodes.get(new Tuple<Integer, Integer>(xIntp1,yInt)).getCurrentFtrMap(_ftrType),  HiXHiYFtrs= MapNodes.get(new Tuple<Integer, Integer>(xIntp1,yIntp1)).getCurrentFtrMap(_ftrType);
        try{
            TreeMap<Integer, Float> ftrs = interpTreeMap(interpTreeMap(LowXLowYFtrs, LowXHiYFtrs,yInterp,rowMult),interpTreeMap(HiXLowYFtrs, HiXHiYFtrs,yInterp,rowMult),xInterp,colMult);    
            return ftrs;
        } catch (Exception e){
            msgObj.dispMessage("SOM_MapManager::"+name,"getInterpFtrs","Exception triggered in SOM_MapManager::getInterpFtrs : \n"+e.toString() + "\n\tError Message : "+e.getMessage(), MsgCodes.error1);
            return null;
        }        
    }//getInterpFtrs
    
//    private TreeMap<Integer, Float> getInterpFtrs_Clrs(float[] c){
//        float xColShift = (c[0]+mapNodeCols), 
//                yRowShift = (c[1]+mapNodeRows), 
//                xInterp = (xColShift) %1, 
//                yInterp = (yRowShift) %1;
//        int xInt = (int) Math.floor(xColShift)%mapNodeCols, yInt = (int) Math.floor(yRowShift)%mapNodeRows, xIntp1 = (xInt+1)%mapNodeCols, yIntp1 = (yInt+1)%mapNodeRows;        //assume torroidal map        
//        //always compare standardized feature data in test/train data to standardized feature data in map
//        TreeMap<Integer, Float> LowXLowYFtrs = MapNodes.get(new Tuple<Integer, Integer>(xInt,yInt)).getCurrentFtrMap(curMapTrainFtrType), LowXHiYFtrs= MapNodes.get(new Tuple<Integer, Integer>(xInt,yIntp1)).getCurrentFtrMap(curMapTrainFtrType),
//                 HiXLowYFtrs= MapNodes.get(new Tuple<Integer, Integer>(xIntp1,yInt)).getCurrentFtrMap(curMapTrainFtrType),  HiXHiYFtrs= MapNodes.get(new Tuple<Integer, Integer>(xIntp1,yIntp1)).getCurrentFtrMap(curMapTrainFtrType);
//        try{
//            TreeMap<Integer, Float> ftrs = interpTreeMap(interpTreeMap(LowXLowYFtrs, LowXHiYFtrs,yInterp,1.0f),interpTreeMap(HiXLowYFtrs, HiXHiYFtrs,yInterp,1.0f),xInterp,255.0f);    
//            return ftrs;
//        } catch (Exception e){
//            msgObj.dispMessage("SOM_MapManager::"+name,"getInterpFtrs","Exception triggered in SOM_MapManager::getInterpFtrs : \n"+e.toString() + "\n\tMessage : "+e.getMessage(), MsgCodes.error1);
//            return null;
//        }        
//    }
    
    //get treemap of features that interpolates between two maps of features
    private TreeMap<Integer, Float> interpTreeMap(TreeMap<Integer, Float> a, TreeMap<Integer, Float> b, float t, float mult){
        TreeMap<Integer, Float> res = new TreeMap<Integer, Float>();
        float Onemt = 1.0f-t;
        if(mult==1.0) {
            //first go through all a values
            for(Integer key : a.keySet()) {
                Float aVal = a.get(key), bVal = b.get(key);
                if(bVal == null) {bVal = 0.0f;}
                res.put(key, (aVal*Onemt) + (bVal*t));            
            }
            //next all b values
            for(Integer key : b.keySet()) {
                Float aVal = a.get(key);
                if(aVal == null) {aVal = 0.0f;} else {continue;}        //if aVal is not null then calced already
                Float bVal = b.get(key);
                res.put(key, (aVal*Onemt) + (bVal*t));            
            }
        } else {//scale by mult - precomputes color values
            float m1t = mult*Onemt, mt = mult*t;
            //first go through all a values
            for(Integer key : a.keySet()) {
                Float aVal = a.get(key), bVal = b.get(key);
                if(bVal == null) {bVal = 0.0f;}
                res.put(key, (aVal*m1t) + (bVal*mt));            
            }
            //next all b values
            for(Integer key : b.keySet()) {
                Float aVal = a.get(key);
                if(aVal == null) {aVal = 0.0f;} else {continue;}        //if aVal is not null then calced already
                Float bVal = b.get(key);
                res.put(key, (aVal*m1t) + (bVal*mt));            
            }            
        }        
        return res;
    }//interpolate between 2 tree maps    
    
    //return interpolated UMatrix value on map at location given by x,y, where x,y  is float location of map using mapnodes as integral locations
    public Float getBiLinInterpUMatVal(float[] c){
        float xColShift = (c[0]+mapNodeCols), 
                yRowShift = (c[1]+mapNodeRows), 
                xInterp = (xColShift) %1, 
                yInterp = (yRowShift) %1;
        int xInt = (int) Math.floor(xColShift)%mapNodeCols, yInt = (int) Math.floor(yRowShift)%mapNodeRows, xIntp1 = (xInt+1)%mapNodeCols, yIntp1 = (yInt+1)%mapNodeRows;        //assume torroidal map        
        //always compare standardized feature data in test/train data to standardized feature data in map
        Float LowXLowYUMat = MapNodes.get(new Tuple<Integer, Integer>(xInt,yInt)).getUMatDist(), LowXHiYUMat= MapNodes.get(new Tuple<Integer, Integer>(xInt,yIntp1)).getUMatDist(),
                HiXLowYUMat= MapNodes.get(new Tuple<Integer, Integer>(xIntp1,yInt)).getUMatDist(),  HiXHiYUMat= MapNodes.get(new Tuple<Integer, Integer>(xIntp1,yIntp1)).getUMatDist();
        try{
            Float uMatVal = linInterpVal(linInterpVal(LowXLowYUMat, LowXHiYUMat,yInterp,1.0f),linInterpVal(HiXLowYUMat, HiXHiYUMat,yInterp,1.0f),xInterp,255.0f);    
            return uMatVal;
        } catch (Exception e){
            msgObj.dispMessage("SOM_MapManager::"+name,"getBiLinInterpUMatVal","Exception triggered in SOM_MapManager::getInterpFtrs : \n"+e.toString() + "\n\tMessage : "+e.getMessage(), MsgCodes.error1 );
            return 0.0f;
        }
    }//getInterpUMatVal    
    private float linInterpVal(float a, float b, float t, float mult) {        return mult*((a*(1.0f-t)) + (b*t));        }//interpVal
    
    /**
     * return interpolated UMatrix value on map at location given by x,y, where x,y  is float location of map using mapnodes as integral locations
     * @param c
     * @return
     */
    public Float getBiCubicInterpUMatVal(float[] c){
        float xColShift = (c[0]+mapNodeCols),     //shifted for modulo
                yRowShift = (c[1]+mapNodeRows), //shifted for modulo
                xInterp = (xColShift) %1, 
                yInterp = (yRowShift) %1;
        int xInt = (int) Math.floor(xColShift)%mapNodeCols, yInt = (int) Math.floor(yRowShift)%mapNodeRows;        //assume torroidal map        
        SOM_MapNode ex = MapNodes.get(new Tuple<Integer, Integer>(xInt,yInt));
        try{
            Float uMatVal = 255.0f*(ex.biCubicInterp_UMatrix(xInterp, yInterp));
            return uMatVal;
        } catch (Exception e){
            msgObj.dispMessage("SOM_MapManager::"+name,"getBiCubicInterpUMatVal","Exception triggered in SOM_MapManager::getInterpFtrs : \n"+e.toString() + "\n\tMessage : "+e.getMessage(), MsgCodes.error1 );
            return 0.0f;
        }
    }//getInterpUMatVal
    
    /**
     * synthesize umat value
     * @param c
     * @return
     */
    public int getUMatrixSegementColorAtPxl(float[] c) {
        float xColShift = (c[0]+mapNodeCols), 
                yRowShift = (c[1]+mapNodeRows), 
                xInterp = (xColShift) %1, 
                yInterp = (yRowShift) %1;
        int xInt = (int) Math.floor(xColShift)%mapNodeCols, yInt = (int) Math.floor(yRowShift)%mapNodeRows;        //assume torroidal map        
        SOM_MapNode ex = MapNodes.get(new Tuple<Integer, Integer>(xInt,yInt));
        try{
            Float uMatVal = (ex.biCubicInterp_UMatrix(xInterp, yInterp));
            return (uMatVal > nodeInSegUMatrixDistThresh ? 0 : ex.getUMatrixSegClrAsInt());
        } catch (Exception e){
            msgObj.dispMessage("SOM_MapManager::"+name,"getUMatrixSegementColorAtPxl","Exception triggered in SOM_MapManager::getInterpFtrs : \n"+e.toString() + "\n\tMessage : "+e.getMessage() , MsgCodes.error1);
            return 0;
        }
    }//getUMatrixSegementColorAtPxl    
    
    //build a string to display an array of floats
    protected String getFloatAraStr(float[] datAra, String fmtStr, int brk) {
        String res = "[";
        int numVals = datAra.length;
        for (int i =0;i<numVals-1;++i) {
            if(datAra[i] != 0) {res +=""+String.format(fmtStr, datAra[i])+", ";    } else {    res +="0, ";    }
            if((i+1) % brk == 0) {res+="\n\t";}
        }
        if(datAra[numVals-1] != 0) {    res +=""+String.format(fmtStr, datAra[numVals-1])+"]";} else {    res +="0]";    }
        return res;
    }    
    
    //provides a list of indexes 0->len-1 that are Durstenfeld shuffled
    protected int[] shuffleAraIDXs(int len) {
        int[] res = new int[len];
        for(int i=0;i<len;++i) {res[i]=i;}
        int swap = 0;
        for(int i=(len-1);i>0;--i){
            int j = MyMathUtils.randomInt(i + 1);//find random lower idx somewhere below current position, and swap current with this idx
            swap = res[i];
            res[i]=res[j];            
            res[j]=swap;            
        }
        return res;    
    }//shuffleAraIDXs    
    //performs Durstenfeld  shuffle, leaves 0->stIdx alone - for testing/training data
    protected String[] shuffleStrList(String[] _list, String type, int stIdx){
        String tmp = "";
        for(int i=(_list.length-1);i>stIdx;--i){
            int j = MyMathUtils.randomInt(i + 1-stIdx)+stIdx;//find random lower idx somewhere below current position but greater than stIdx, and swap current with this idx
            tmp = _list[i];
            _list[i] = _list[j];
            _list[j] = tmp;
        }
        return _list;
    }//shuffleStrList    
    //shuffle all training example data passed
    public SOM_Example[] shuffleTrainingData(SOM_Example[] _list, long seed) {
        SOM_Example tmp;
        Random tr = new Random(seed);
        for(int i=(_list.length-1);i>0;--i){
            int j = tr.nextInt(i + 1);//find random lower idx somewhere below current position but greater than stIdx, and swap current with this idx
            tmp = _list[i];
            _list[i] = _list[j];
            _list[j] = tmp;
        }
        return _list;
    }
    
    /**
     * invoke multi-threading call to build map imgs - called from UI window
     * @param mapImgBuilders
     */
    public void invokeSOMFtrDispBuild(List<SOM_FtrMapVisImgBldr> mapImgBuilders) {        
        try {
            List<Future<Boolean>> mapImgFtrs = th_exec.invokeAll(mapImgBuilders);
            for(Future<Boolean> f: mapImgFtrs) { f.get(); }
        } catch (Exception e) { e.printStackTrace(); }    
    }//
    
    ///////////////////////////////
    // mouse and draw routines    
    
    //set specific mouse-over display data/values
    /**
     * instancing application should determine whether we want to display features sorted in magnitude order, or sorted in idx order
     * @param ptrLoc
     * @param ftrs
     * @param sens
     * @return
     */
    public abstract void setMseDataExampleFtrs(myPointf ptrLoc, TreeMap<Integer, Float> ftrs, float sens);
    /**
     * display features in wt-descending order
     * @param ptrLoc
     * @param ftrs
     * @param sens
     * @return
     */
    public final void setMseDataExampleFtrs_WtSorted(myPointf ptrLoc, TreeMap<Integer, Float> ftrs, float sens) {mseOverExample.initMseDatFtrs_WtSorted(ftrs, sens); mseOverExample.setMapLoc(ptrLoc);}
    /**
     * display features in idx-ascending order
     * @param ptrLoc
     * @param ftrs
     * @param sens
     * @return
     */
    public final void setMseDataExampleFtrs_IdxSorted(myPointf ptrLoc, TreeMap<Integer, Float> ftrs, float sens) {mseOverExample.initMseDatFtrs_IdxSorted(ftrs, sens); mseOverExample.setMapLoc(ptrLoc);}
    
    public final void setMseDataExampleDists(myPointf ptrLoc, float dist, float distMin, float distDiff, float sens) {mseOverExample.initMseDatUMat(dist,distMin,distDiff, sens);mseOverExample.setMapLoc(ptrLoc);}
    public final void setMseDataExampleClassProb(myPointf ptrLoc, SOM_MapNode nearestNode, float sens) {mseOverExample.initMseDatProb(nearestNode, sens, true);mseOverExample.setMapLoc(ptrLoc);}
    public final void setMseDataExampleCategoryProb(myPointf ptrLoc, SOM_MapNode nearestNode, float sens) {mseOverExample.initMseDatProb(nearestNode, sens, false);mseOverExample.setMapLoc(ptrLoc);}
    public final void setMseDataExampleNodePop(myPointf ptrLoc, SOM_MapNode nearestNode, float sens) {mseOverExample.initMseDatCounts(nearestNode, sens, SOM_ExDataType.Training);mseOverExample.setMapLoc(ptrLoc);}
    public final void setMseDataExampleNodeName(myPointf ptrLoc, SOM_MapNode nearestNode, float sens) {mseOverExample.initMseDatNodeName(nearestNode, sens);mseOverExample.setMapLoc(ptrLoc);}    
    public final void setMseDataExampleNone() { mseOverExample.clearMseDat(); }
    
    /**
     * get datapoint at passed location in map coordinates (so should be in frame of map's upper right corner) - assume map is square and not hex
     * @param x
     * @param y
     * @param sensitivity
     * @param locPt
     */
    public final void setMouseOverDataText(float x, float y, float sensitivity, myPointf locPt){//, boolean useScFtrs){
        //float sensitivity = (float) guiObjs_Numeric[gIDX_MseRegionSensIDX].getVal();
        
        SOM_MapNode nearestNode = getMapNodeByCoords(new Tuple<Integer,Integer> ((int)(x+.5f), (int)(y+.5f)));
        switch(uiMseDispData) {
            case mseOvrMapNodeLocIDX : {            //Map loc
                setMseDataExampleNodeName(locPt,nearestNode,sensitivity);
                break;}
            case mseOvrUMatDistIDX : {            //UMatrix dist
                setMseDataExampleDists(locPt, getBiCubicInterpUMatVal(new float[] {x, y}),uMatDist_Min, uMatDist_Diff, sensitivity);    
                break;}
            case mseOvrMapNodePopIDX : {            //Population
                setMseDataExampleNodePop(locPt,nearestNode,sensitivity);
                break;}
            case mseOvrFtrIDX : {            //feature values
                TreeMap<Integer, Float> ftrs = getInterpFtrs(new float[] {x, y},BMU_DispFtrType, 1.0f, 1.0f);
                if(ftrs == null) {setMseDataExampleNone();return ;} 
                setMseDataExampleFtrs(locPt, ftrs, sensitivity);                
                break;}
            case mseOvrClassIDX : {            //class    
                setMseDataExampleClassProb(locPt,nearestNode,sensitivity);
                break;}
            case mseOvrCatIDX : {            //category
                setMseDataExampleCategoryProb(locPt,nearestNode,sensitivity);
                break;}
            case mseOvrNoneIDX : {            //none
                setMseDataExampleNone();//setMseDataExampleNodeName(locPt,nearestNode,sensitivity);
                break;}
            default : {
                getDataPointAtLoc_Priv(x, y, sensitivity,nearestNode, locPt, custUIMseDispData);
            }
        }//switch

        mseOverExample.setMapLoc(locPt);

    }//getDataPointAtLoc
    
    protected abstract void getDataPointAtLoc_Priv(float x, float y, float sensitivity, SOM_MapNode nearestNode, myPointf locPt, int uiMseDispData);
    

    public SOM_MseOvrDispTypeVals getUiMseDispData() {    return uiMseDispData;}
    public void setUiMseDispData(SOM_MseOvrDispTypeVals uiMseDispData) {    this.uiMseDispData = uiMseDispData;}
    
    public int getCustUIMseDispData() {    return custUIMseDispData;}
    public void setCustUIMseDispData(int _custUIMseDispData) {    custUIMseDispData = _custUIMseDispData;}
    
    public final boolean checkMouseOvr(int mouseX, int mouseY, float sens) {
        float mapMseX = mouseX - SOM_mapLoc[0], mapMseY = mouseY - SOM_mapLoc[1];//, mapLocX = mapX * mapMseX/mapDims[2],mapLocY = mapY * mapMseY/mapDims[3] ;
        if((mapMseX >= 0) && (mapMseY >= 0) && (mapMseX < mapDims[0]) && (mapMseY < mapDims[1])){    //within bounds of map
            float[] mapNLoc=getMapNodeLocFromPxlLoc(mapMseX,mapMseY, 1.0f);
            //msgObj.dispInfoMessage("SOM_MapManager::"+name,"chkMouseOvr","In Map : Mouse loc : " + mouseX + ","+mouseY+ "\tRel to upper corner ("+  mapMseX + ","+mapMseY +") | mapNLoc : ("+mapNLoc[0]+","+ mapNLoc[1]+")" );
            //mseOvrData = 
            //mseOverExample = 
                    setMouseOverDataText(mapNLoc[0], mapNLoc[1], sens, new myPointf(mapMseX, mapMseY,0));        
            
            return true;
        } else {
            setMseDataExampleNone();
            //mseOvrData = null;
            return false;
        }
    }

    /**
     * check mouse over/click in experiment; if btn == -1 then mouse over
     * @param mouseX mouse x in world
     * @param mouseY mouse y in world
     * @param mseClckInWorld
     * @param btn
     * @return
     */
    public final boolean checkMouseClick(int mouseX, int mouseY, myPoint mseClckInWorld, int btn) {
        float mapMseX = mouseX - SOM_mapLoc[0], mapMseY = mouseY - SOM_mapLoc[1];//, mapLocX = mapX * mapMseX/mapDims[2],mapLocY = mapY * mapMseY/mapDims[3] ;
        if((mapMseX >= 0) && (mapMseY >= 0) && (mapMseX < mapDims[0]) && (mapMseY < mapDims[1])){    //within bounds of map
            float[] mapNLoc=getMapNodeLocFromPxlLoc(mapMseX,mapMseY, 1.0f);
            Tuple<Integer, Integer> nodeCoords = new Tuple<Integer,Integer> ((int)(mapNLoc[0]+.5f), (int)(mapNLoc[1]+.5f));
            SOM_MapNode nearestNode = getMapNodeByCoords(nodeCoords);
            SOM_MapNode oldNode = SelectedMapNodes.get(nodeCoords);
            boolean _wasSelNotDeSel = (oldNode == null);
            if(_wasSelNotDeSel) {    SelectedMapNodes.put(nodeCoords,nearestNode);}
            else {                    SelectedMapNodes.remove(nodeCoords);}
            return checkMouseClick_Indiv(mouseX, mouseY, mapNLoc[0], mapNLoc[1], nearestNode, mseClckInWorld, btn,_wasSelNotDeSel);
        } else {//clicked in blank space, treat like release
            checkMouseRelease();
            return false;
        }        
    };
    protected abstract boolean checkMouseClick_Indiv(int mouseX, int mouseY, float mapX, float mapY, SOM_MapNode nearestNode, myPoint mseClckInWorld, int btn, boolean _wasSelNotDeSel);
    /**
     * check mouse drag/move in experiment; if btn == -1 then mouse over
     * @param mouseX mouse x in world
     * @param mouseY mouse y in world
     * @param mseClckInWorld
     * @param btn
     * @return
     */
    public final boolean checkMouseDragMove(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
        if(-1==mseBtn) {return false;}        //should be handled by mouse move 
        return checkMouseDragMove_Indiv( mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D, mseDragInWorld, mseBtn);
    };
    public abstract boolean checkMouseDragMove_Indiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn);
    
    public final void checkMouseRelease() {
        checkMouseRelease_Indiv();
    }
    protected abstract void checkMouseRelease_Indiv();
    
    /**
     * draw map rectangle and map nodes
     * @param ri
     */
    public final void drawSOMMapData(IRenderInterface ri) {
        PImage tmpImg;
        int curImgNum;
        if(win.getPrivFlag(SOM_MapUIWin.mapDrawUMatrixIDX)) {                
            tmpImg =  mapCubicUMatrixImg;
            curImgNum = -1;
        } else {
            tmpImg = mapPerFtrWtImgs[curFtrMapImgIDX];        
            curImgNum = curFtrMapImgIDX;
        }
        ri.pushMatState();
            ri.disableLights();
            ri.scale(mapScaleVal);
            //doing this in separate matrix stack frame because map is built small and scaled up
            ((ProcessingRenderer)Base_DispWindow.ri).image(tmpImg,SOM_mapLoc[0]/mapScaleVal,SOM_mapLoc[1]/mapScaleVal); if(win.getPrivFlag(SOM_MapUIWin.saveLocClrImgIDX)){tmpImg.save(getSOMLocClrImgForFtrFName(curImgNum));  win.setPrivFlag(SOM_MapUIWin.saveLocClrImgIDX,false);}            
            if(win.getPrivFlag(SOM_MapUIWin.mapDrawUMatSegImgIDX)) {((ProcessingRenderer)Base_DispWindow.ri).image(mapUMatrixCubicSegmentsImg,SOM_mapLoc[0]/mapScaleVal,SOM_mapLoc[1]/mapScaleVal);}
            ri.enableLights();
        ri.popMatState(); 
        ri.pushMatState();
            ri.disableLights();
            boolean drawLbl = win.getPrivFlag(SOM_MapUIWin.mapDrawNodeLblIDX);
            boolean draw0PopNodes = win.getPrivFlag(SOM_MapUIWin.mapDrawNodesWith0MapExIDX);
            ri.translate(SOM_mapLoc[0],SOM_mapLoc[1],0);    
            if(win.getPrivFlag(SOM_MapUIWin.mapDrawTrainDatIDX)){            drawTrainData(ri);}    
            if(win.getPrivFlag(SOM_MapUIWin.mapDrawTestDatIDX)) {            drawTestData(ri);}
            if(win.getPrivFlag(SOM_MapUIWin.mapDrawPopMapNodesIDX)) {    if(drawLbl) {drawPopMapNodes(ri, draw0PopNodes, mapNodeDispType.getVal());} else {drawPopMapNodesNoLbl(ri, draw0PopNodes, mapNodeDispType.getVal());}}
            else {numMapNodeByPopNowShown = 0;}
        
            if (curImgNum > -1) {
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawWtMapNodesIDX)){            drawNodesWithWt(ri, mapNodeWtDispThresh, curFtrMapImgIDX);} 
                //display ftr-wt, class and category images, if enabled
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawFtrWtSegMembersIDX)) {        drawFtrWtSegments(ri, mapNodeWtDispThresh, curFtrMapImgIDX);}
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawClassSegmentsIDX)) {         drawClassSegments(ri,curClassLabel);    }        
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawCategorySegmentsIDX)) {     drawCategorySegments(ri,curCategoryLabel);    }                
                drawPerFtrMap_Indiv(ri);
            } else {            
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawUMatSegMembersIDX)) {        drawUMatrixSegments(ri);}
                
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawFtrWtSegMembersIDX)) {        drawAllFtrWtSegments(ri, mapNodeWtDispThresh);}    //draw all segments - will overlap here, might look like garbage        
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawClassSegmentsIDX)) {         drawAllClassSegments(ri);}
                if(win.getPrivFlag(SOM_MapUIWin.mapDrawCategorySegmentsIDX)) {     drawAllCategorySegments(ri);}
                drawSegmentsUMatrixDispIndiv(ri);
            }
            //if draw all map nodes
            if(win.getPrivFlag(SOM_MapUIWin.mapDrawAllMapNodesIDX)){    if(drawLbl) {drawAllNodesWithLbl(ri);} else {drawAllNodesNoLbl(ri);} }
            //draw nodes that are selected
            if(SelectedMapNodes.size() > 0) {drawNodesIfSelected(ri);}
            //instance-specific stuff to draw on map, after nodes are drawn
            drawMapRectangle_Indiv(ri, curImgNum);
            //if selected, draw graph describing population of each map node
            if(win.getPrivFlag(SOM_MapUIWin.drawMapNodePopGraphIDX)) {drawMapNodePopGraph(ri, mapNodeDispType.getVal());}
            ri.enableLights();
        ri.popMatState();    
        
    }//drawMapRectangle
    
    protected final void drawMapNodePopGraph(IRenderInterface ri, int typIDX) {
        ri.pushMatState();
        ri.translate(mapDims[0],0.0f,0.0f);
        ri.scale(200.0f/(1.0f*mapNodePopGraph[typIDX].width),mapDims[1]/(1.0f*mapNodePopGraph[typIDX].height));
        ((ProcessingRenderer)Base_DispWindow.ri).image(mapNodePopGraph[typIDX], 0.0f, 0.0f);
        
        ri.popMatState();
    }//drawMapNodePopGraph
    
    
    protected abstract void drawMapRectangle_Indiv(IRenderInterface ri, int curImgNum);
    /**
     * draw instance-specific per-ftr map data display
     */
    protected abstract void drawPerFtrMap_Indiv(IRenderInterface ri);
    /**
     * Instancing class-specific segments and other data to render during UMatrix display
     */
    protected abstract void drawSegmentsUMatrixDispIndiv(IRenderInterface ri);

        
    private static int dispTrainDataFrame = 0, numDispTrainDataFrames = 20;
    //if connected to UI, draw data - only called from window
    public final void drawTrainData(IRenderInterface ri) {
        ri.pushMatState();
        if (trainData.length < numDispTrainDataFrames) {    for(int i=0;i<trainData.length;++i){        trainData[i].drawMeMap(ri);    }    } 
        else {
            for(int i=dispTrainDataFrame;i<trainData.length-numDispTrainDataFrames;i+=numDispTrainDataFrames){        trainData[i].drawMeMap(ri);    }
            for(int i=(trainData.length-numDispTrainDataFrames);i<trainData.length;++i){        trainData[i].drawMeMap(ri);    }                //always draw these (small count < numDispDataFrames
            dispTrainDataFrame = (dispTrainDataFrame + 1) % numDispTrainDataFrames;
        }
        ri.popMatState();
    }//drawTrainData
    private static int dispTestDataFrame = 0, numDispTestDataFrames = 20;
    //if connected to UI, draw data - only called from window
    public final void drawTestData(IRenderInterface ri) {
        ri.pushMatState();
        if (testData.length < numDispTestDataFrames) {    for(int i=0;i<testData.length;++i){        testData[i].drawMeMap(ri);    }    } 
        else {
            for(int i=dispTestDataFrame;i<testData.length-numDispTestDataFrames;i+=numDispTestDataFrames){        testData[i].drawMeMap(ri);    }
            for(int i=(testData.length-numDispTestDataFrames);i<testData.length;++i){        testData[i].drawMeMap(ri);    }                //always draw these (small count < numDispDataFrames
            dispTestDataFrame = (dispTestDataFrame + 1) % numDispTestDataFrames;
        }
        ri.popMatState();
    }//drawTrainData
    private static int dispValidationDataFrame = 0, numDispValidationDataFrames = 100;
    public final void drawValidationData(IRenderInterface ri) {
        ri.pushMatState();
        if (validationData.length < numDispValidationDataFrames) {    for(int i=0;i<validationData.length;++i){        validationData[i].drawMeMap(ri);    }    } 
        else {
            for(int i=dispValidationDataFrame;i<validationData.length-numDispValidationDataFrames;i+=numDispValidationDataFrames){        validationData[i].drawMeMap(ri);    }
            for(int i=(validationData.length-numDispValidationDataFrames);i<validationData.length;++i){        validationData[i].drawMeMap(ri);    }                //always draw these (small count < numDispDataFrames
            dispValidationDataFrame = (dispValidationDataFrame + 1) % numDispValidationDataFrames;
        }
        ri.popMatState();        
    }//drawValidationData
    
    public final void drawNodesIfSelected(IRenderInterface ri) {
        ri.pushMatState();
        for(SOM_MapNode node : SelectedMapNodes.values()){    node.drawMeSelected(ri);    }        
        ri.popMatState();
    }
    
    /**
     * draw boxes around each node representing umtrx values derived in SOM code - deprecated, now drawing image
     * @param ri
     */
    public final void drawUMatrixVals(IRenderInterface ri) {
        ri.pushMatState();
        for(SOM_MapNode node : MapNodes.values()){    node.drawMeUMatDist(ri);    }        
        ri.popMatState();
    }//drawUMatrix
    /**
     * draw boxes around each node representing UMatrix-distance-based segments these nodes belong to
     * @param ri
     */
    public final void drawUMatrixSegments(IRenderInterface ri) {
        ri.pushMatState();
        for(SOM_MapNode node : MapNodes.values()){    node.drawMeUMatSegClr(ri);    }        
        ri.popMatState();
    }//drawUMatrix
    
    /**
     * draw boxes around each node representing ftrwt-based segments that nodes belong to, so long as their ftr values are higher than threshold amount
     * @param ri
     * @param valThresh
     * @param curFtrIdx
     */
    public final void drawFtrWtSegments(IRenderInterface ri, float valThresh, int curFtrIdx) { 
        ri.pushMatState();
        TreeMap<Float,ArrayList<SOM_MapNode>> map = PerFtrHiWtMapNodes[curFtrIdx];
        //map holds map nodes keyed by wt of nodes that actually have curFtrIdx presence
        SortedMap<Float,ArrayList<SOM_MapNode>> headMap = map.headMap(valThresh);
        if(headMap.size()!=0) {
            float minVal = headMap.lastKey();
            float diffVal = headMap.firstKey() - minVal;
            if(diffVal == 0) {        minVal =0.0f;        diffVal = 1.0f;    }
            for(Float key : headMap.keySet()) {
                float wt = (key-minVal)/diffVal;
                ArrayList<SOM_MapNode> ara = headMap.get(key);
                //System.out.println("Min Val :  " + minVal + " | Diff Val : " + diffVal + " WT : " + wt);
                for (SOM_MapNode node : ara) {        node.drawMeFtrWtSegClr(ri, curFtrIdx, wt);}
            }    
        }
        ri.popMatState();
    }//drawFtrWtSegments
    
    /**
     * draw boxes around every node representing ftrwt-based segments that node belongs to, with color strength proportional to ftr val and different colors for each segment
     * @param ri
     * @param valThresh
     */
    public final void drawAllFtrWtSegments(IRenderInterface ri, float valThresh) {        
        for(int curFtrIdx=0;curFtrIdx<PerFtrHiWtMapNodes.length;++curFtrIdx) {        drawFtrWtSegments(ri, valThresh, curFtrIdx);    }        
    }//drawFtrWtSegments

    
    /**
     * draw boxes around each node representing class-based segments that node 
     * belongs to, with color strength proportional to probablity of class within that node and 
     * different colors for each segment
     * pass class -label- not class index
     * @param ri
     * @param classLabel - label corresponding to class to be displayed
     */
    public final void drawClassSegments(IRenderInterface ri, int classLabel) {
        Collection<SOM_MapNode> mapNodesWithClasses = MapNodesWithMappedClasses.get(classLabel);
        if(null==mapNodesWithClasses) {        return;}
        ri.pushMatState();
        for (SOM_MapNode node : mapNodesWithClasses) {        node.drawMeClassClr(ri, classLabel);}        
        ri.popMatState();
    }//drawClassSegments    
    
    public final void drawAllClassSegments(IRenderInterface ri) {    for(Integer key : Class_Segments.keySet()) {    drawClassSegments(ri,key);}    }

    /**
     * draw filled boxes around each node representing category-based segments 
     * that node belongs to, with color strength proportional to probablity 
     * and different colors for each segment
     * pass category -label- not category index
     * @param ri
     * @param categoryLabel - label corresponding to category to be displayed
     */
    public final void drawCategorySegments(IRenderInterface ri, int categoryLabel) {
        Collection<SOM_MapNode> mapNodes = MapNodesWithMappedCategories.get(categoryLabel);
        if(null==mapNodes) {return;}
        ri.pushMatState();
        for (SOM_MapNode node : mapNodes) {        node.drawMeCategorySegClr(ri, categoryLabel);}                
        ri.popMatState();
    }//drawCategorySegments
    public final void drawAllCategorySegments(IRenderInterface ri) {    for(Integer key : Category_Segments.keySet()) {    drawCategorySegments(ri,key);}    }
        
    public void drawAllNodesWithLbl (IRenderInterface ri) {//, int[] dpFillClr, int[] dpStkClr) {
        ri.pushMatState();
        //ri.setFill(dpFillClr);ri.setStroke(dpStkClr);
        for(SOM_MapNode node : MapNodes.values()){    node.drawMeSmall(ri);    }
        ri.popMatState();
    } 
    
    public void drawAllNodesNoLbl(IRenderInterface ri) {//, int[] dpFillClr, int[] dpStkClr) {
        ri.pushMatState();
        //ri.setFill(dpFillClr);ri.setStroke(dpStkClr);
        for(SOM_MapNode node : MapNodes.values()){    node.drawMeSmallNoLbl(ri);    }
        ri.popMatState();
    } 
    
    public void drawNodesWithWt(IRenderInterface ri, float valThresh, int curFtrIdx) {//, int[] dpFillClr, int[] dpStkClr) {
        ri.pushMatState();
        //ri.setFill(dpFillClr);ri.setStroke(dpStkClr);
        TreeMap<Float,ArrayList<SOM_MapNode>> map = PerFtrHiWtMapNodes[curFtrIdx];
        SortedMap<Float,ArrayList<SOM_MapNode>> headMap = map.headMap(valThresh);
        for(Float key : headMap.keySet()) {
            ArrayList<SOM_MapNode> ara = headMap.get(key);
            for (SOM_MapNode node : ara) {        node.drawMeWithWt(ri, 10.0f*key, new String[]{""+node.OID+" : ",String.format("%.4f",key)});}
        }
        ri.popMatState();
    }//drawNodesWithWt
    
    public void drawPopMapNodes(IRenderInterface ri, boolean draw0PopNodes, int _typeIDX) {
        ri.pushMatState();
        numMapNodeByPopNowShown = 0;
        if(draw0PopNodes) {            
            for(SOM_MapNode node : MapNodes.values()){    if(node.getBMUMapNodePopulation(_typeIDX) > mapNodePopDispThreshVals[_typeIDX]) {    node.drawMePopLbl(ri, _typeIDX);numMapNodeByPopNowShown++;}    }
        } else {
            for(SOM_MapNode node : MapNodes.values()){    if((node.getBMUMapNodePopulation(_typeIDX) > mapNodePopDispThreshVals[_typeIDX]) && (node.getHasMappedExamples(_typeIDX))) {    node.drawMePopLbl(ri, _typeIDX);numMapNodeByPopNowShown++;}    }
            
        }
        ri.popMatState();        
    }    
    public void drawPopMapNodesNoLbl(IRenderInterface ri, boolean draw0PopNodes, int _typeIDX) {
        ri.pushMatState();
        numMapNodeByPopNowShown = 0;
        if(draw0PopNodes) {            
            for(SOM_MapNode node : MapNodes.values()){    if(node.getBMUMapNodePopulation(_typeIDX) > mapNodePopDispThreshVals[_typeIDX]) {    node.drawMePopNoLbl(ri, _typeIDX);numMapNodeByPopNowShown++;}    }
        } else {
            for(SOM_MapNode node : MapNodes.values()){    if((node.getBMUMapNodePopulation(_typeIDX) > mapNodePopDispThreshVals[_typeIDX]) && (node.getHasMappedExamples(_typeIDX))) {    node.drawMePopNoLbl(ri, _typeIDX);numMapNodeByPopNowShown++;}    }
            
        }
        ri.popMatState();        
    }
    //public final void drawMseOverData(IRenderInterface ri) {    mseOvrData.drawMeLblMap(ri);}

    public final void drawMseOverData(IRenderInterface ri) {    mseOverExample.drawMeLblMap(ri);}
    
    //get ftr name/idx/instance-specific value based to save an image of current map
    public abstract String getSOMLocClrImgForFtrFName(int ftrIDX);
    
    protected int sideBarMseOvrDispOffset = 100;
    protected float sideBarYDisp = 10.0f;

    //draw right sidebar data
    public void drawResultBar(IRenderInterface ri, float yOff) {
        yOff-=4;
        //float sbrMult = 1.2f, lbrMult = 1.5f;//offsets multiplier for barriers between contextual ui elements
        ri.pushMatState();
        //display preloaded maps
        yOff=drawLoadedPreBuiltMaps(ri,yOff,curPreBuiltMapIDX);
        //display # of map nodes currently shown if showing by population
        yOff=drawNumMapNodesShownByPop(ri, yOff);
        //display mouse-over results in side bar
        yOff= drawMseRes(ri,yOff);
        ri.drawSphere(3.0f);
        yOff = drawResultBarPriv1(ri, yOff);
        
        yOff = drawResultBarPriv2(ri, yOff);

        yOff = drawResultBarPriv3(ri, yOff);

        ri.popMatState();    
    }//drawResultBar
    
    private final float drawNumMapNodesShownByPop(IRenderInterface ri,float yOff) {
        if(win.getPrivFlag(SOM_MapUIWin.mapDrawPopMapNodesIDX)) {
            ri.translate(10.0f, 0.0f, 0.0f);
            AppMgr.showOffsetText(0,IRenderInterface.gui_White,"# of Map Nodes Shown : " + numMapNodeByPopNowShown);
            yOff += sideBarYDisp;
            ri.translate(-10.0f,sideBarYDisp, 0.0f);
            
        }
        return yOff;
    }
    
    /**
     * draw mouse-over results
     * @param yOff
     * @return
     */
    private final float drawMseRes(IRenderInterface ri,float yOff) {
        if((getFlag(dispMseDataSideBarIDX)) && mseOverExample.canDisplayMseLabel()) {
            ri.translate(10.0f, 0.0f, 0.0f);
            AppMgr.showOffsetText(0,IRenderInterface.gui_White,"Mouse Values : ");
            yOff += sideBarYDisp;
            ri.translate(0.0f,sideBarYDisp, 0.0f);
            mseOverExample.drawMseLbl_Info(ri, new myPointf(0,0,0));
            float tmpOff = ((int)(mseOverExample.getMseLabelYOffset() / (1.0f*sideBarMseOvrDispOffset)) + 1 )*sideBarMseOvrDispOffset;
            yOff += tmpOff;
            ri.translate(-10.0f, tmpOff, 0.0f);
        }
        return yOff;
    }//drawMseRes
    
    protected final float drawLoadedPreBuiltMaps(IRenderInterface ri,float yOff, int curDefaultMap) {
        if(getFlag(dispLdPreBuitMapsIDX)) {    
            String[][] loadedPreBuiltMapData = projConfigData.getPreBuiltMapInfoAra();        
            ri.translate(0.0f, 0.0f, 0.0f);
            //float stYOff = yOff, tmpOff = sideBarMseOvrDispOffset;    
            if(loadedPreBuiltMapData.length==0) {                
                AppMgr.showOffsetText(0,IRenderInterface.gui_White,"No Pre-build Map Directories specified.");
                yOff += sideBarYDisp;
                ri.translate(10.0f, sideBarYDisp, 0.0f);
            } else {    
                AppMgr.showOffsetText(0,IRenderInterface.gui_White,"Pre-build Map Directories specified in config : ");
                yOff += sideBarYDisp;
                ri.translate(10.0f, sideBarYDisp, 0.0f);
                
                for(int i=0;i<loadedPreBuiltMapData.length;++i) {
                    boolean isDefault = i==curDefaultMap;
                    boolean isLoaded = i==pretrainedMapIDX;
                    int clrIDX = (isLoaded ? IRenderInterface.gui_Yellow : IRenderInterface.gui_White);
                    AppMgr.showOffsetText(0,clrIDX,""+String.format("%02d", i+1)+" | "+loadedPreBuiltMapData[i][0]);
                    yOff += sideBarYDisp;
                    ri.translate(10.0f,sideBarYDisp,0.0f);
                    AppMgr.showOffsetText(0,clrIDX,"Detail : ");
                    if(isDefault) {
                        ri.pushMatState();
                        ri.translate(-10.0f,20.0f,0.0f);
                        ri.setFill(255, 200,200, 255);
                        ri.setStroke(255, 200,200, 255);
                        ri.drawStar2D(myPointf.ZEROPT, 5.0f);
                        ri.popMatState();
                    }
                    yOff += sideBarYDisp;
                    ri.translate(10.0f, sideBarYDisp, 0.0f);
                    AppMgr.showOffsetText(0,clrIDX,loadedPreBuiltMapData[i][1]);        
                    yOff += sideBarYDisp;
                    ri.translate(0.0f, sideBarYDisp, 0.0f);            
                    AppMgr.showOffsetText(0,clrIDX,loadedPreBuiltMapData[i][2]);        
                    yOff += sideBarYDisp;
                    ri.translate(0.0f, sideBarYDisp, 0.0f);            
                    AppMgr.showOffsetText(0,clrIDX,loadedPreBuiltMapData[i][3]);        
                    yOff += sideBarYDisp;
                    ri.translate(0.0f, sideBarYDisp, 0.0f);            
                    
                    ri.translate(-10.0f, 0.0f, 0.0f);                
                    yOff = getPreBuiltMapInfoDetail(ri,loadedPreBuiltMapData[i], i, yOff, isLoaded);
                }
            }        
            ri.translate(-10.0f, 0.0f, 0.0f);
        }
        return yOff;
    }//drawLoadedPreBuiltMaps
    protected abstract float getPreBuiltMapInfoDetail(IRenderInterface ri, String[] str, int i, float yOff, boolean isLoaded);
    
    //draw app-specific sidebar data
    protected abstract float drawResultBarPriv1(IRenderInterface ri, float yOff);
    protected abstract float drawResultBarPriv2(IRenderInterface ri, float yOff);
    protected abstract float drawResultBarPriv3(IRenderInterface ri, float yOff);

    //////////////////////////////
    // getters/setters
    
    /**
     * return the per-file data partition size to use when saving preprocessed training data csv files
     * @return
     */
    public abstract int getPreProcDatPartSz();
    
    public final TreeMap<String,String> getSOMExecInfo(){
        TreeMap<String, String> res = new TreeMap<String, String>();        
        for(String key : exampleDataMappers.keySet()) {
            SOM_ExampleManager mapper = exampleDataMappers.get(key);
            res.put(mapper.longExampleName + " date and time of creation/pre-processing", mapper.dateAndTimeOfDataCreation());        
        }    
        getSOMExecInfo_Indiv(res);
        return res;
    }//getSOMExecInfo
    
    /**
     * return a map of application-specific descriptive quantities and their values, for the SOM Execution human-readable report
     * @param res map already created holding exampleDataMappers create time
     * @return map with application-specific information added to res
     */
    protected abstract void getSOMExecInfo_Indiv(TreeMap<String, String> res);
        
    /**
     * called from SOM_DataLoarder after all BMUs are loaded and training data bmus are set from bmu 
     * file. Application-specific functionality to handle mapping test data, for example
     */
    public abstract void setAllBMUsFromMap();

    /**
     * set the prebuilt map dir display to use based on prebuilt map dirs specified in config
     * this is called by config
     * @param _pbltMapArray  : list of strings represent pre built map directories specified in config
     */
    public void setPreBuiltMapDirList(String[] _pbltMapArray) {if(win!=null) { win.setPreBuiltMapArray(_pbltMapArray);}}
    
    public boolean getUseChiSqDist() {return useChiSqDist;}
    public void setUseChiSqDist(boolean _useChiSq) {useChiSqDist=_useChiSq;}

    //set current map ftr type, and update ui if necessary
    public void setCurrentTrainDataFormat(SOM_FtrDataType _frmt) {    curMapTrainFtrType = _frmt; msgObj.dispInfoMessage("SOM_MapManager::"+name,"setCurrentTrainDataFormat","curMapTrainFtrType set to : " +curMapTrainFtrType + "."); projConfigData.setFtrDataTypeUsedToTrain(curMapTrainFtrType.getBrfName());}//setCurrentDataFormat
    public void setCurrentTrainDataFormatFromConfig(int _frmt) {
        curMapTrainFtrType = SOM_FtrDataType.getEnumByIndex(_frmt); 
        msgObj.dispInfoMessage("SOM_MapManager::"+name,"setCurrentTrainDataFormatFromConfig","curMapTrainFtrType set to : " +curMapTrainFtrType + " from Config."); 
        if (win != null) {win.setFtrTrainTypeFromConfig(_frmt);}        
    }//setCurrentDataFormat
    public SOM_FtrDataType getCurrentTrainDataFormat() {    return curMapTrainFtrType;}
    
    public void setPerFtrMinAndDiffValsFromConfig(float _stdFtr_destMin,float _stdFtr_destDiff) {
        perFtrNorm_destMin = _stdFtr_destMin;
        perFrtNorm_destDiff =_stdFtr_destDiff;        
        msgObj.dispInfoMessage("SOM_MapManager::"+name,"setStdMinAndDiffValsFromConfig","stdFtr_destMin set to : " +perFtrNorm_destMin + " and stdFtr_destDiff set to : " + perFrtNorm_destDiff+" from Config.");     
    };
    public float getPerFtrNorm_destMin() {return perFtrNorm_destMin;}
    public float getPerFtrNorm_destDiff() {return perFrtNorm_destDiff;}
    
    public void setBMU_DispFtrTypeFormat(SOM_FtrDataType _frmt) {    BMU_DispFtrType = _frmt; }//setCurrentDataFormat
    public SOM_FtrDataType getBMU_DispFtrTypeFormat() {    return BMU_DispFtrType;}
    public MessageObject getMsgObj(){    return msgObj;}

    public float[] getSOM_mapLoc() {    return SOM_mapLoc;}
    public void setSOM_mapLoc(float[] _SOM_mapLoc) {            SOM_mapLoc = _SOM_mapLoc;}
    
    public float getMapWidth(){return mapDims[0];}
    public float getMapHeight(){return mapDims[1];}
    public int getMapNodeCols(){return mapNodeCols;}
    public int getMapNodeRows(){return mapNodeRows;}    
    
    public SOM_MapNode getMapNodeByCoords(Tuple<Integer,Integer> key) {return MapNodes.get(key);}
    public TreeMap<Tuple<Integer,Integer>, SOM_MapNode> getMapNodes(){return MapNodes;}
    public TreeMap<Integer, HashSet<SOM_MapNode>> getMapNodesByFtr(){return MapNodesByFtrIDX;}
    
    public float getNodePerPxlCol() {return nodeXPerPxl;}
    public float getNodePerPxlRow() {return nodeYPerPxl;}    
    //mean/var,mins/diffs of features of map nodes
    public float[] getMap_ftrsMean() {return map_ftrsMean;}            
    public float[] getMap_ftrsVar() {return map_ftrsVar;}            
    public float[] getMap_ftrsDiffs() {return map_ftrsDiffs;}            
    public float[] getMap_ftrsMin() {return map_ftrsMin;}
    
    public Float[] getMinVals(int idx){return minsVals[idx];}
    public Float[] getDiffVals(int idx){return diffsVals[idx];}
    public abstract Float[] getTrainFtrMins();
    public abstract Float[] getTrainFtrDiffs();

    public SOM_Example[] getTrainingData() {return trainData;}
    
    //# of features used to train SOM
    public int getNumTrainFtrs() {return numTrnFtrs;}
    public void setNumTrainFtrs(int _numTrnFtrs) {numTrnFtrs = _numTrnFtrs;}
    
    //project config manages this information now
    public Calendar getInstancedNow() { return projConfigData.getInstancedNow();}
    
    //getter/setter/convenience funcs to check for whether mt capable, and to return # of usable threads (total host threads minus some reserved for processing)
    public int getNumUsableThreads() {return numUsableThreads;}
    public ExecutorService getTh_Exec() {return th_exec;}

    public boolean isMTCapable() {return getFlag(isMTCapableIDX);}        
    

    ////////////////////////////////
    // segments and segment-related values
    
    //umatrix segment thresholds
    public float getNodeInUMatrixSegThresh() {return nodeInSegUMatrixDistThresh;}
    public void setNodeInUMatrixSegThresh(float _val) {nodeInSegUMatrixDistThresh=_val;}    
    public String getUMatrixSegmentTitleString() {return "UMatrix Dist calc from SOM Training.";}
    //ftr-wt segment thresholds
    public float getNodeInFtrWtSegThresh() {return nodeInSegFtrWtDistThresh;}
    public void setNodeInFtrWtSegThresh(float _val) {nodeInSegFtrWtDistThresh=_val;}        
    public abstract String getFtrWtSegmentTitleString(SOM_FtrDataType ftrCalcType, int ftrIDX);
    //class and category segments
    public TreeMap<Integer, SOM_MappedSegment> getClass_Segments(){ return Class_Segments; }
    public abstract String getClassSegmentTitleString(int classID);
    public TreeMap<Integer, SOM_MappedSegment> getCategory_Segments(){ return Category_Segments; }
    public abstract String getCategorySegmentTitleString(int catID);

    //win UI-driven values
    public int getCurFtrMapImgIDX() {    return curFtrMapImgIDX;}
    public void setCurFtrMapImgIDX(int curMapImgIDX) {    this.curFtrMapImgIDX = curMapImgIDX;}
    public int getCurCategoryIDX() {    return curCategoryIDX;}
    public void setCurCategoryIDX(int curCategoryIDX) {    this.curCategoryIDX = curCategoryIDX;}
    public int getCurCategoryLabel() {    return curCategoryLabel;}
    public void setCurCategoryLabel(int curCategoryLabel) {    this.curCategoryLabel = curCategoryLabel;}
    public int getCurClassIDX() {    return curClassIDX;}
    public void setCurClassIDX(int curClassIDX) {    this.curClassIDX = curClassIDX;}
    public int getCurClassLabel() {    return curClassLabel;}
    public void setCurClassLabel(int curClassLabel) {    this.curClassLabel = curClassLabel;}
    public int getCurPreBuiltMapIDX() {    return curPreBuiltMapIDX;}
    public void setCurPreBuiltMapIDX(int curPreBuiltMapIDX) {    this.curPreBuiltMapIDX = curPreBuiltMapIDX;}
    
    public float getMapNodeWtDispThresh() {    return mapNodeWtDispThresh;}
    public void setMapNodeWtDispThresh(float _mapNodeWtDispThresh) {    this.mapNodeWtDispThresh = _mapNodeWtDispThresh;}
    
    public float getMapNodePopDispThreshPct() {    return mapNodePopDispThreshPct;}
    public void setMapNodePopDispThreshPct(float _mapNodePopDispThresh) {    
        if(mapNodePopDispThreshPct != _mapNodePopDispThresh) {
            mapNodePopDispThreshPct = _mapNodePopDispThresh;
            buildMapNodePopGraphImage();
        }
    }
    
    public SOM_ExDataType getMapNodeDispType() {    return mapNodeDispType;}
    public void setMapNodeDispType(SOM_ExDataType _mapNodeDispType) {    this.mapNodeDispType = _mapNodeDispType;}    
    //set flag that SOM file loader is finished to false
    //public void setLoaderRtnFalse() {setFlag(loaderFinishedRtnIDX, false);}    
    // use functions to easily access states
    public void setIsDebug(boolean val) {setFlag(debugIDX, val);}
    public boolean getIsDebug() {return getFlag(debugIDX);}    
    public void setSOMMapNodeDataIsLoaded(boolean val) {setFlag(SOMmapNodeDataLoadedIDX, val);}
    public boolean getSOMMapNodeDataIsLoaded() {return getFlag(SOMmapNodeDataLoadedIDX);}    
    public void setLoaderRTN(boolean val) {setFlag(loaderFinishedRtnIDX, val);}
    public boolean getLoaderRTN() {return getFlag(loaderFinishedRtnIDX);}
    public void setDenseTrainDataSaved(boolean val) {setFlag(denseTrainDataSavedIDX, val);}
    public void setSparseTrainDataSaved(boolean val) {setFlag(sparseTrainDataSavedIDX, val);}
    public void setCSVTestDataSaved(boolean val) {setFlag(testDataSavedIDX, val);}    
    //set all training/testing data save flags to val
    public void setAllTrainDatSaveFlags(boolean val) {
        setFlag(denseTrainDataSavedIDX, val);
        setFlag(sparseTrainDataSavedIDX, val);
        setFlag(testDataSavedIDX, val);        
    }            
    public boolean getTrainDataBMUsRdyToSave() {return getFlag(trainDataMappedIDX);}
    public boolean getProdDataBMUsRdyToSave() {return getFlag(prodDataMappedIDX);}
    public boolean getTestDataBMUsRdyToSave() {return (getFlag(testDataMappedIDX) || testData.length==0);}
    public boolean getValidationDataBMUsRdyToSave() {return getFlag(validateDataMappedIDX);}
    /**
     * call on load/mapping of bmus
     * @param val
     */
    public void setTrainDataBMUsMapped(boolean val) {setFlag(trainDataMappedIDX,val);}
    /**
     * call on load/mapping of bmus
     * @param val
     */
    public void setTestDataBMUsMapped(boolean val) {setFlag(testDataMappedIDX,val);}
    /**
     * call on load/mapping of bmus
     * @param val
     */
    public void setValidationDataBMUsMapped(boolean val) {setFlag(validateDataMappedIDX,val);}
    /**
     * call on load/mapping of bmus
     * @param val
     */
    public void setProdDataBMUsMapped(boolean val) {setFlag(prodDataMappedIDX, val);}
    
    //////////////
    // private state flags
    protected abstract int getNumFlags();
    private void initFlags(){int _numFlags = getNumFlags(); stFlags = new int[1 + _numFlags/32]; for(int i = 0; i<_numFlags; ++i){setFlag(i,false);}}
    protected void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
    public void setFlag(int idx, boolean val){
        int flIDX = idx/32, mask = 1<<(idx%32);
        stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
        switch (idx) {//special actions for each flag
            case debugIDX                     : {break;}    
            case isMTCapableIDX             : {break;}                    //whether or not the host architecture can support multiple execution threads
            case SOMmapNodeDataLoadedIDX    : {break;}        
            case loaderFinishedRtnIDX         : {break;}
            case denseTrainDataSavedIDX : {
                if (val) {msgObj.dispMessage("SOM_MapManager::"+name,"setFlag","All "+ this.numTrainData +" Dense Training data saved to .lrn file", MsgCodes.info5);}
                break;}                //all examples saved as training data
            case sparseTrainDataSavedIDX : {
                if (val) {msgObj.dispMessage("SOM_MapManager::"+name,"setFlag","All "+ this.numTrainData +" Sparse Training data saved to .svm file", MsgCodes.info5);}
                break;}                //all examples saved as training data
            case testDataSavedIDX : {
                if (val) {msgObj.dispMessage("SOM_MapManager::"+name,"setFlag","All "+ this.numTestData + " saved to " + projConfigData.getSOMMapTestFileName() + " using "+(projConfigData.isUseSparseTestingData() ? "Sparse ": "Dense ") + "data format", MsgCodes.info5);}
                break;}    
            
            case trainDataMappedIDX            : {break;}
            case prodDataMappedIDX            : {break;}
            case testDataMappedIDX            : {break;}
            case validateDataMappedIDX        : {break;}
            
            case dispMseDataSideBarIDX        : {break;}
            case dispLdPreBuitMapsIDX        : {break;}
            
            default : { setFlag_Indiv(idx, val);}    //any flags not covered get set here in instancing class            
        }
    }//setFlag        
    public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}        
    protected abstract void setFlag_Indiv(int idx, boolean val);    
    
    //getter/setter/convenience funcs
    public boolean mapCanBeTrained(int kVal) {
        msgObj.dispMessage("SOM_MapManager::"+name,"mapCanBeTrained","denseTrainDataSavedIDX : " + getFlag(denseTrainDataSavedIDX), MsgCodes.info5);
        msgObj.dispMessage("SOM_MapManager::"+name,"mapCanBeTrained","sparseTrainDataSavedIDX : " + getFlag(sparseTrainDataSavedIDX), MsgCodes.info5);
        boolean val = ((kVal <= 1) && (getFlag(denseTrainDataSavedIDX)) || ((kVal == 2) && getFlag(sparseTrainDataSavedIDX)));
        msgObj.dispMessage("SOM_MapManager::"+name,"mapCanBeTrained","kVal : " + kVal + " | bool val : " + val, MsgCodes.info5);
        
        //eventually enable training map on existing files - save all file names, enable file names to be loaded and map built directly
        return ((kVal <= 1) && (getFlag(denseTrainDataSavedIDX)) || ((kVal == 2) && getFlag(sparseTrainDataSavedIDX)));
    }    
    //return true if loader is done and if data is successfully loaded
    public boolean isMapDrawable(){return getFlag(loaderFinishedRtnIDX) && getFlag(SOMmapNodeDataLoadedIDX);}
    public boolean isToroidal(){return projConfigData.isToroidal();}    
    //get fill, stroke and text color ID if win exists (to reference papplet) otw returns 0,0,0
    public int[] getClrFillStrkTxtAra(SOM_ExDataType _type) {
        if (win==null) {return new int[] {0,0,0};}                                                            //if null then not going to be displaying anything
        switch(_type) {
            case Training : {        return new int[] {IRenderInterface.gui_Cyan,IRenderInterface.gui_Cyan,IRenderInterface.gui_Blue};}            //corresponds to training example
            case Testing : {        return new int[] {IRenderInterface.gui_Magenta,IRenderInterface.gui_Magenta,IRenderInterface.gui_Red};}        //corresponds to testing/held-out example
            case Validation : {     return new int[] {IRenderInterface.gui_Magenta,IRenderInterface.gui_Magenta,IRenderInterface.gui_Red};}        //corresponds to examples to be mapped
            case Product : {        return new int[] {IRenderInterface.gui_Yellow,IRenderInterface.gui_Yellow,IRenderInterface.gui_White};}        //corresponds to product example
            case MapNode : {        return new int[] {IRenderInterface.gui_Green,IRenderInterface.gui_Green,IRenderInterface.gui_Cyan};}            //corresponds to map node example
            case MouseOver : {        return new int[] {IRenderInterface.gui_White,IRenderInterface.gui_White,IRenderInterface.gui_Black};}        //corresponds to mouse example
        }
        return new int[] {IRenderInterface.gui_White,IRenderInterface.gui_White,IRenderInterface.gui_White};
    }//getClrVal
    public int[] getAltClrFillStrkTxtAra() {
        if (win==null) {return new int[] {0,0,0};}        
        return new int[] {IRenderInterface.gui_Red,IRenderInterface.gui_Red,IRenderInterface.gui_White};
    }
    
    
    //find distance on map
    public myPoint buildScaledLoc(float x, float y){        
        float xLoc = (x + .5f) * (mapDims[0]/mapNodeCols), yLoc = (y + .5f) * (mapDims[1]/mapNodeRows);
        myPoint pt = new myPoint(xLoc, yLoc, 0);
        return pt;
    }
    
    //distance on map    
    public myPointf buildScaledLoc(Tuple<Integer,Integer> mapNodeLoc){        
        float xLoc = (mapNodeLoc.x + .5f) * (mapDims[0]/mapNodeCols), yLoc = (mapNodeLoc.y + .5f) * (mapDims[1]/mapNodeRows);
        myPointf pt = new myPointf(xLoc, yLoc, 0);
        return pt;
    }
    //return upper left corner of umat box x,y and width,height
    public float[] buildUMatBoxCrnr(Tuple<Integer,Integer> mapNodeLoc) {
        float w =  (mapDims[0]/mapNodeCols), h = (mapDims[1]/mapNodeRows);        
        float[] res = new float[] {mapNodeLoc.x * w, mapNodeLoc.y * h, w, h};
        return res;
    }
    //mapNodeCols, mapNodeRows
    public Tuple<Integer,Integer> getMapLocTuple(int xLoc, int yLoc){return new Tuple<Integer,Integer>((xLoc +mapNodeCols)%mapNodeCols, (yLoc+mapNodeRows)%mapNodeRows );}
    //set UI values from loaded map data, if UI is in use
    public void setUIValsFromLoad(SOM_MapDat mapDat) {if (win != null) {        win.setUIValues(mapDat);    }}//setUIValsFromLoad
    
    /**
     * set ui values from project config used by this map manager
     */
    public void setUIValsFromProjConfig() {projConfigData.setUIValsFromLoad();}
    
    public void resetButtonState() {if (win != null) {    win.resetButtonState();}}

    public void setMapNumCols(int _x){
        //need to update UI value in win
        mapNodeCols = _x;
        nodeXPerPxl = mapNodeCols/this.mapDims[0];
        projConfigData.updateMapDat_Integer_MapCols(_x, true,true);
    }//setMapX
    public void setMapNumRows(int _y){
        //need to update UI value in win
        mapNodeRows = _y;
        nodeYPerPxl = mapNodeRows/this.mapDims[1];
        projConfigData.updateMapDat_Integer_MapRows(_y, true,true);
    }//setMapY

    public String toString(){
        String res = "Map Manager : ";
        res += "UI Window class is : "+(win==null ? "null " : "present and non-null " );
        
        return res;    
    }


}//abstract class SOM_MapManager

/**
 * class to manage executing the SOM training in a console process
 * @author john
 *
 */
class mySOMProcConsoleMgr extends myProcConsoleMsgMgr{

    public mySOMProcConsoleMgr(MessageObject _msgObj, String _type) {    super(_msgObj,_type);}
    
    /**
     * SOM outputs info about time to train each epoch in stderr instead of stdout despite it not being an error, 
     * so we don't want to display these messages as being errors
     */
    @Override
    protected String getStreamType(String rawStr) { return (rawStr.toLowerCase().contains("time for epoch") ? "Input" : type);}    
}//class mySOMProcConsoleMgr

