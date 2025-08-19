package base_SOM_Objects.som_geom;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Render_Interface.IGraphicsAppInterface;
import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_geom.geom_UI.SOM_AnimWorldWin;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomExampleManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomFtrBndMon;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomMapNode;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;
import base_SOM_Objects.som_geom.geom_utils.SOM_GeomProjConfig;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjTypes;
import base_SOM_Objects.som_geom.geom_utils.geom_threading.geomGen.SOM_GeomObjBldrRunner;
import base_SOM_Objects.som_geom.geom_utils.geom_threading.geomGen.SOM_GeomObjBldrTasks;
import base_SOM_Objects.som_managers.SOM_ExampleManager;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MapUIWin;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_UIToMapCom;
import base_SOM_Objects.som_utils.SOM_ProjConfigData;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * extension of SOM_MapManager intended for geometric/graphical objects
 * intended to eventually be abstract
 * @author john
 *
 */
public abstract class SOM_GeomMapManager extends SOM_MapManager {
    /**
     * owning window to display samples in sim world
     */
    public SOM_AnimWorldWin dispWin;    
    /**
     * actual represented random/generated uiObjs - source examples used to derive training examples used by this experiment
     */
    public SOM_GeomObj[] sourceGeomObjects;    
    
    /**
     * training example ftr data bounds manager
     */
    public SOM_GeomFtrBndMon trainDatObjBnds;
    
    /**
     * actual represented random/generated uiObjs - source examples used to derive training examples used by this experiment
     */
    public SOM_GeomObj[] trainDatGeomObjects;    

    /**
     * mapper to manage the example training data built from the geometric objects
     */
    protected SOM_GeomExampleManager geomSrcToTrainExDataManager;
    
    /**
     * mapper for -training- data - geometric source data must be mapped into this construction, which is then used as source for training
     */
    protected SOM_GeomExampleManager trainExDataManager;

    
    /**
     * extended flags from base class
     */
    public static final int 
        srcGeomObjsAllBuiltIDX         = numBaseFlags + 0,
        trainDatObjsAllBuiltIDX     = numBaseFlags + 1,
        allTrainExUniqueIDX            = numBaseFlags + 2;                            //all training data examples should be unique - this might not be possible if more samples are asked for than are possible to be unique for object type
    
    public static final int numGeomBaseFlags = numBaseFlags + 3;
    

    /**
     * # of preprocessed examples to save to a single file
     */
    private static final int preProcDataPartSz = 50000;
    
    /**
     * # of geometric objects to build
     */
    protected int numObjsToBuild;
    
    /**
     * # of samples per object
     */
    protected int numSamplesPerObj;
    
    /**
     * total # of samples in world
     */
    protected int ttlNumSamples;
    /**
     * total # of training examples to build
     */
    protected int ttlNumTrainExamples;
    
     /**
      * runnable object to manage various tasks
      */
    protected SOM_GeomObjBldrRunner objRunner;
    
    /** 
     * Type of geometric object
     */
    private final String geomObjTypeName;
    
    /**
     * coordinate bounds in world for the objects this map manager owns 
     *         first idx : 0 is min; 1 is diff
     *         2nd idx : 0 is x, 1 is y, 2 is z
     */
    protected final float[][] worldBounds;
    
    //disp message string to facilitate message output
    private String dispClassGeomName;
        
    public SOM_GeomMapManager(SOM_MapUIWin _win, SOM_AnimWorldWin _dispWin, float[][] _worldBounds, Map<String, Object> _argsMap, SOM_GeomObjTypes _geomObjType, int _numFtrs) {
        super(_win, _argsMap);
            //# of training features determined by type of object
        setNumTrainFtrs(_numFtrs);
        geomObjTypeName = _geomObjType.getName();
        projConfigData.setSOMProjName(geomObjTypeName);    
        dispWin = _dispWin;
        String dispWinStatus  = geomObjTypeName + " World bounds set to ";
        if(dispWin != null) {
            dispWinStatus += "value!";
            worldBounds=_worldBounds;
            objRunner = buildObjRunner();        
            geomSrcToTrainExDataManager.setObjRunner(objRunner);
        } else {
            dispWinStatus += "null!";
            worldBounds = null;
            objRunner = null;
        }
        trainDatObjBnds = buildTrainDatFtrBndMgr();
        dispClassGeomName = "SOM_GeomMapManager::"+geomObjTypeName+"::"+name;
        getMsgObj().dispMessage(dispClassGeomName,"ctor", dispWinStatus,MsgCodes.info5);
        
    }//ctor    
    
    /**
     * Any instancing-class-specific functionality for after MapUIWindow is set - name has changed once window is set
     */
    protected final void setDispWinDataIndiv() {
        dispClassGeomName = "SOM_GeomMapManager::"+geomObjTypeName+"::"+name;
    }
    
    /**
     * build the thread runner for this map manager that will manage the various tasks related to the geometric objects
     * @return
     */
    protected abstract SOM_GeomObjBldrRunner buildObjRunner();    
    /**
     * build the training data bounds manager
     */
    protected final SOM_GeomFtrBndMon buildTrainDatFtrBndMgr() {
        //use # of ftrs mapped 
        return new SOM_GeomFtrBndMon(getNumTrainFtrs());
    };
    /**
     * build instance-specific project file configuration 
     */
    @Override
    protected final SOM_ProjConfigData buildProjConfigData(Map<String, Object> _argsMap) {                return new SOM_GeomProjConfig(this,_argsMap);    }    

    /**
     * build an interface to manage communications between UI and SOM map dat
     * This interface will need to include a reference to an application-specific UI window
     */
    @Override
    protected final SOM_UIToMapCom buildSOM_UI_Interface() {    return new SOM_UIToMapCom(this, win);}    
    
    /**
     * build the map of example mappers used to manage all the data the SOM will consume - called from base class ctor
     */
    @Override
    protected final void buildExampleDataMappers() {
        SOM_ExampleManager ex = buildExampleDataMappers_Indiv("geomSrcExData"); // this manages TRAINING data
        exampleDataMappers.put("geomSrcExData",  ex);
        SOM_ExampleManager exTrain = buildExampleDataMappers_Indiv("trainingData"); // this manages TRAINING data
        exampleDataMappers.put("trainingData",  exTrain);
        
        geomSrcToTrainExDataManager = (SOM_GeomExampleManager) exampleDataMappers.get("geomSrcExData");
        trainExDataManager = (SOM_GeomExampleManager) exampleDataMappers.get("trainingData");
    }
    
    /**
     * build the example data mapper specific to instancing class
     * @return
     */
    protected abstract SOM_GeomExampleManager buildExampleDataMappers_Indiv(String _name);
    
    /**
     * Load and process raw data, and save results as preprocessed csvs - this is only necessary
     * when building a SOM from a pre-built dataset.  This project will use synthesized geometric objects
     * to build SOM data, and so this won't be used
     * @param fromCSVFiles : whether loading data from csv files or from SQL calls
     * 
     */
    @Override
    public final void loadAndPreProcAllRawData(boolean fromCSVFiles) {}
    
    /**
     * set number of objects to build and number of samples per object
     * @param _numObjs
     * @param _numSamples
     */
    public final void setUIObjData(int _numObjs, int _numSamples, int _numTrainExamples) {
        if(numObjsToBuild!=_numObjs) {        //reset if forcing regen of all objects, required if # of objs changes
            numObjsToBuild=_numObjs;            
                //geom object data in example manager is now out of synch - reset to require rebuild of base array
            resetGeomAndTrainDataObjs();
        }
        numSamplesPerObj=_numSamples;    
        ttlNumSamples = numObjsToBuild * numSamplesPerObj;
        if(ttlNumTrainExamples != _numTrainExamples) {
            ttlNumTrainExamples = _numTrainExamples;
            resetTrainDataObjs();
        }
    }
    
    /**
     * Find the number of training examples that are necessary so that every base object has been sampled with the given probability.
     * @param numObjsMin estimate to # of underlying objects
     * @param prob desired probability
     * @return
     */
    public final long calcOptNumObjsForDesiredProb(int numObjsMin, float probDes) {
        ////////////////////////////
        // build urn
        
        //have # of samples per obj
        Long numSmplsToBuildObj = (long) getNumSamplesToBuildObject();
        //total ways to build an example object with all points coming from a given object
        Long numExPerObj = MyMathUtils.choose(numSamplesPerObj, numSmplsToBuildObj);
        //total # of example builds possible matching an object from unique samples for all objects
        Long ttlNumExampleObjs = numObjsToBuild * numExPerObj;
        //total # of ways to build an example object from all samples (i.e. # of unique examples possible)
        Long ttlObjsPos = MyMathUtils.choose(ttlNumSamples, numSmplsToBuildObj);
        MathContext MC = new MathContext(10);
        //Chance per draw that we built a specific object from sample set
        BigDecimal chance1ObjPerDraw = BigDecimal.valueOf(numExPerObj).divide(new BigDecimal(ttlObjsPos), MC);
        BigDecimal chanceObjPerDraw = chance1ObjPerDraw.multiply(BigDecimal.valueOf(numObjsToBuild));
        
        getMsgObj().dispMessage(dispClassGeomName,"calcOptNumObjsForDesiredProb",
                "URN : # samples : " + ttlNumSamples + " | # objects : " + numObjsToBuild + 
                " | # Exs from 1 obj : " + numExPerObj + " | # Exs for All Objs : " +  
                        ttlNumExampleObjs + " | # examples from all samples : " + ttlObjsPos.toString() + 
                        " | Chance per draw to have a specific object : " + chance1ObjPerDraw.toString()+ 
                        " | Chance per draw to have any objects : " + chanceObjPerDraw.toString(), MsgCodes.info1);

        
        /////////////////////////////////////////////////
        // urn has N samples, with numExPerObj examples built from each object, and N - (numObjsToBuild * numExPerObj) erroneous examples
        BigInteger NumObjDrawsPerColor = MyMathUtils.choose_BigInt(numExPerObj, numObjsMin);
        BigInteger NumTTLObjDraws = BigInteger.ONE;
        int ttlObjDraws = numObjsMin * numObjsToBuild;
        for(int i =0; i<numObjsToBuild;++i) { NumTTLObjDraws = NumTTLObjDraws.multiply(NumObjDrawsPerColor);}
        //# Of non-object spheres
        BigInteger denomTtlNumObjsDrawn = MyMathUtils.choose_BigInt(ttlObjsPos, ttlObjDraws);
        
        getMsgObj().dispMessage(dispClassGeomName,"calcOptNumObjsForDesiredProb","# objs min per color : " + 
            numObjsMin + " | # obj draws per color : "+NumObjDrawsPerColor.toString() + 
            " | # ttl obj draws : "+NumTTLObjDraws.toString(), MsgCodes.info1);
        getMsgObj().dispMessage(dispClassGeomName,"calcOptNumObjsForDesiredProb","ttl num obj draws  :"+denomTtlNumObjsDrawn.toString(), MsgCodes.info1);

        
        
        //use probDes to denote the probability that numObjsMin examples for each object are present in final draw.

//        Long mK = K * numObjsMin;        
//        Long mKm1 = mK - numObjsMin - 1;
//        BigDecimal KtoMPwrOvNtoM = BigDecimal.ONE;
//        for(int i =0; i<numObjsMin;++i) {KtoMPwrOvNtoM = KtoMPwrOvNtoM.multiply(BigDecimal.valueOf(K).divide(new BigDecimal(N)));}
//        
//        getMsgObj().dispMessage(dispClassGeomName,"calcOptNumObjsForDesiredProb", " (mK-1) : " +(mK-1) + " | N : " + N.toString() + " | KtoMPwrOvNtoM : " + KtoMPwrOvNtoM.toString(), MsgCodes.info1);
//        double prob = 0.0;
//        BigDecimal probBD = new BigDecimal(0.0);
//        //BigDecimal constVal = KtoMPwr.divide(Nmi_denom);
//        //double divis = KtoMPwr.doubleValue() / Nmi_denom.doubleValue();
//        //BigDecimal constVal = new BigDecimal( divis);
//        int incr = numSamplesPerObj;
//        boolean done = false;
//        BigDecimal TmIProd, NTiProdBD;
//        BigInteger NTiProd;
//        int iter = 0;
//        while(!done) {
//            TmIProd = BigDecimal.ONE;
//            for(int i=0;i<(numObjsMin-1);++i) {    TmIProd = TmIProd.multiply(BigDecimal.valueOf(T-i));}
//            NTiProd = BigInteger.ONE;
//            for(long i=0L;i<mKm1;++i) {            NTiProd = NTiProd.multiply(N.subtract(BigInteger.valueOf(T+i)));    }
//            NTiProdBD = new BigDecimal(NTiProd);
//            probBD = KtoMPwrOvNtoM.multiply(TmIProd).multiply(NTiProdBD);
//            prob = probBD.doubleValue();
//            getMsgObj().dispMessage(dispClassGeomName,"calcOptNumObjsForDesiredProb", "iter : " + iter + " KtoMPwrOvNtoM : " + KtoMPwrOvNtoM+  " | K : " + K + " | T Value : " + T +" | Calced prob : " + probBD.toString() + " | TmIProd : " + TmIProd.toString() + " | NTiProd : " + NTiProd.toString(),MsgCodes.info1);
//            
//            done = (prob >= probDes);
//            if(!done) {T+=incr; ++iter;}
//        }
        return 0L;
    }//calcOptNumObjsForDesiredProb
                
    /**
     * actually configre and execute objRunner task for specified objects, task to perform
     * @param _objs array of objs to perform task upon
     * @param _task task to perform
     * @param _callingMethod method which invoked this method
     * @param _taskDescr string description of task to perform
     */
    private final void execGeomObjRunnerTask(SOM_GeomObj[] _objs, SOM_GeomObjBldrTasks _task, String _callingMethod, String _taskDescr) {
        //set task type to perform
        objRunner.setTaskType(_task);
        //set instance-specific values
        execObjRunner_Pre_Indiv(_task);
        //set array to work with in obj runner
        objRunner.setObjArray(_objs);    //sets # of work units/objects to build based on size of array
        getMsgObj().dispMessage(dispClassGeomName,"execGeomObjRunnerTask::"+_callingMethod, "Start " + _taskDescr,MsgCodes.info1);
        objRunner.runMe();
        getMsgObj().dispMessage(dispClassGeomName,"execGeomObjRunnerTask::"+_callingMethod, "Finished " + _taskDescr,MsgCodes.info1);
    }//execObjRunnerTask    
    
    public final void resetTrainDataObjs() { 
        resetTrainDataAras();
        trainExDataManager.reset();
        trainDatGeomObjects = new SOM_GeomObj[0];
        setFlag(trainDatObjsAllBuiltIDX,false);
    }
    
    public final void resetGeomAndTrainDataObjs() {
        resetTrainDataObjs();
        geomSrcToTrainExDataManager.reset();
        sourceGeomObjects = new SOM_GeomObj[0];
        setFlag(srcGeomObjsAllBuiltIDX,false);
    }
    
    /**
     * (re)build base examples setFlag(trainDatObjsAllBuiltIDX,true);
     */
    public final void buildGeomExampleObjs() {
        objRunner.setNumSamplesPerObj(numSamplesPerObj);    
        resetGeomAndTrainDataObjs();
        
        execGeomObjRunnerTask(buildEmptyObjArray(), SOM_GeomObjBldrTasks.buildBaseObj, "buildGeomExampleObjs","building "+ numObjsToBuild+" geom example objects of type : " + geomObjTypeName);
        sourceGeomObjects = objRunner.getObjArray();
        setFlag(srcGeomObjsAllBuiltIDX, true);
        for(SOM_GeomObj obj : sourceGeomObjects) {    geomSrcToTrainExDataManager.addExampleToMap(obj);    }        
        geomSrcToTrainExDataManager.setAllDataLoaded();
        geomSrcToTrainExDataManager.setAllDataPreProcced();
            //finalize and calc ftr vecs on geometry if we have loaded new data 
        finishSOMExampleBuild(geomSrcToTrainExDataManager,  ""+geomObjTypeName +" geometric (graphical source) example");        
    }
    
    /**
     * regenerate samples for all existing objects - use existing array 
     */
    public final void rebuildGeomExObjSamples() {
        objRunner.setNumSamplesPerObj(numSamplesPerObj);
        //reset training data executor since training data is built off # of samples
        resetTrainDataObjs();
        execGeomObjRunnerTask(sourceGeomObjects, SOM_GeomObjBldrTasks.regenSamplesBaseObj, "rebuildGeomExObjSamples","rebuilding " + numSamplesPerObj +" samples for all "+ numObjsToBuild+" geom example objects of type : " + geomObjTypeName);    
    }
    
    /**
     * send any instance-specific control/ui values to objRunners, based on task
     */
    protected abstract void execObjRunner_Pre_Indiv(SOM_GeomObjBldrTasks _task);
    
    /**
     * This will build an appropriately sized empty object array, to be passed to the runner so that it can fill the array
     * @return
     */
    protected abstract SOM_GeomObj[] buildEmptyObjArray();

    @Override
    /**
     * this function will build the input data used by the SOM - this will be partitioned by some amount into test and train data (usually will use 100% train data, but may wish to test label mapping)
     */
    protected final SOM_Example[] buildSOM_InputData() {
        SOM_Example[] res = trainExDataManager.buildExampleArray();    //cast to appropriate mapper when flag custOrdersAsTrainDataIDX is set
        getMsgObj().dispMessage(dispClassGeomName,"buildSOM_InputData", "Size of input data : " + res.length,MsgCodes.info5);
        return res;
    }
    /**
     * load some previously saved geometric object information
     */
    @Override
    public final void loadPreProcTrainData(String subDir, boolean forceLoad) {
        getMsgObj().dispMessage(dispClassGeomName,"loadPreProcTrainData","Begin loading preprocced data from " + subDir +  " directory.", MsgCodes.info5);
            //load geometry data
        if(!geomSrcToTrainExDataManager.isDataPreProcced() || forceLoad) {            
                //first need to load UI info about preprocced data (settings used to generate data)
            LinkedHashMap<String,String> uiToBuildGeom = geomSrcToTrainExDataManager.loadGeomObjsUIVals(subDir);
            if((null != uiToBuildGeom) && (uiToBuildGeom.size()!=0)) {        //loaded successfully
                dispWin.forceNewUIAllValsFromPreProcLoad(uiToBuildGeom);
            } else {
                getMsgObj().dispWarningMessage(dispClassGeomName,"loadPreProcTrainData","UI Values used to build Geom failed to load - UI may be out of sync with current Geometry objects.");
            }
                //now load actual object data
            geomSrcToTrainExDataManager.loadAllPreProccedExampleData(subDir);
                //copy mapper geom examples to example array
            sourceGeomObjects = (SOM_GeomObj[]) geomSrcToTrainExDataManager.buildExampleArray();
            setFlag(srcGeomObjsAllBuiltIDX, true);
                //finalize and calc ftr vecs on geometry if we have loaded new data 
            finishSOMExampleBuild(geomSrcToTrainExDataManager,  ""+geomObjTypeName +" geometric (graphical source) example");        
            this.resetTrainDataObjs();
            
        } else {getMsgObj().dispMessage(dispClassGeomName,"loadPreProcTrainData","Not loading preprocessed " + geomObjTypeName +" geometric examples since they are already loaded.", MsgCodes.info1);}
        
        getMsgObj().dispMessage(dispClassGeomName,"loadAllPreProccedData","Finished loading preprocced data from " + subDir +  " directory.", MsgCodes.info5);
    }//loadPreProcTrainData
    
//    
//    /**
//     * train map with currently set SOM control values - UI will set values as they change, if UI is being used, otherwise values set via config files
//     * this is what is called by "build som" button on UI
//     */
//    @Override
//    public final boolean loadTrainDataMapConfigAndBuildMap(boolean mapNodesToData) {    
//        getMsgObj().dispMessage(dispClassGeomName,"loadTrainDataMapConfigAndBuildMap","Start Loading training data and building map. Mapping examples to SOM Nodes : "+mapNodesToData, MsgCodes.info1);
//        //load all training data and build test and training data partitions
//        //loadPreprocAndBuildTestTrainPartitions(projConfigData.getTrainTestPartition(), false);
//        loadPreProcTrainData(projConfigData.getPreProcDataDesiredSubDirName(),false);
//        //build test/train data partitions only if changed
//        buildTrainTestFromPartition(projConfigData.getTrainTestPartition());            
//        //build experimental directories, save training, testing and diffs/mins data to directories - only should be called when building a new map
//        initNewSOMDirsAndSaveData();        
//        //reload currently set default config for SOM - IGNORES VALUES SET IN UI
//        getMsgObj().dispMessage(dispClassGeomName,"loadTrainDataMapConfigAndBuildMap","Finished Loading training data and setting directories.", MsgCodes.info1);
//        boolean res = _ExecSOM(mapNodesToData);
//        getMsgObj().dispMessage(dispClassGeomName,"loadTrainDataMapConfigAndBuildMap","Finished Loading training data and building map. Success : "  + res+ " | Mapped examples to SOM Nodes :"+mapNodesToData, MsgCodes.info1);
//        return res;
//    }//loadTrainDataMapConfigAndBuildMap
    
    
    /**
     * save all currently preprocced loaded data - source data along with their constituent samples
     */
    public void saveAllPreProcExamples() {
        getMsgObj().dispMessage(dispClassGeomName,"saveAllPreProcExamples","Start Saving all Preproccessed " + geomObjTypeName + " Examples.", MsgCodes.info5);
            //save current data as preprocced data
        boolean saveSuccess = geomSrcToTrainExDataManager.saveAllPreProccedExampleData();        
            //need to save current UI configuration used to generate geometric data
        boolean saveUISuccess = geomSrcToTrainExDataManager.saveGeomObjsUIVals(dispWin.getAllUIValsForPreProcSave());
        getMsgObj().dispMessage(dispClassGeomName,"saveAllPreProcExamples","Finished Saving all Preproccessed " + geomObjTypeName + " Examples. Data Success : " + saveSuccess + " | UI Vals Success : " + saveUISuccess, MsgCodes.info5);
    }//saveAllPreProcExamples
    
    
    //get mins/diffs for ftr vals per ftr jp and for all vals per all jps
    public Float[][] getMinBndsAra() {
        ArrayList<Float[]> tmpBnds = new ArrayList<Float[]>();
        tmpBnds.add(0,trainDatObjBnds.getMinBndsAra());    
        return tmpBnds.toArray(new Float[1][] );
    }
    public Float[][] getDiffsBndsAra() {        
        ArrayList<Float[]> tmpBnds = new ArrayList<Float[]>();
        tmpBnds.add(0,trainDatObjBnds.getDiffBndsAra());    
        return tmpBnds.toArray(new Float[1][] );
    }    
    
    
    public void saveCurrentTrainData() {
        getMsgObj().dispMessage(dispClassGeomName,"saveCurrentTrainData","Begin saving current data as training data, mins and diffs. Train size : " + numTrainData+ " Testing size : " + numTestData+".", MsgCodes.info5);    
        initNewSOMDirsAndSaveData();    
        
        getMsgObj().dispMessage(dispClassGeomName,"saveCurrentTrainData","Finished saving current data as training data, mins and diffs. Train size : " + numTrainData+ " Testing size : " + numTestData+".", MsgCodes.info5);    
    }
    
    /**
     * finish building the training example data - finalize each example and then perform calculation to derive weight vector
     * @param dataManager
     * @param exampleMapperDesc
     */
    protected void finishSOMExampleBuild(SOM_GeomExampleManager dataManager, String exampleMapperDesc) {
        getMsgObj().dispMessage(dispClassGeomName,"finishSOMExampleBuild","Begin finalize mappers, calculate feature data, diffs, mins, and calculate post-global-ftr-data calcs.", MsgCodes.info5);
            //current SOM map, if there is one, is now out of date, do not use
        setSOMMapNodeDataIsLoaded(false);
            //finalize customer prospects and products (and true prospects if they exist) - customers are defined by having criteria that enable their behavior to be used as to train the SOM        
        getMsgObj().dispInfoMessage(dispClassGeomName,"_finalizeAllMappersBeforeFtrCalc","Begin finalize of all example data, preparing each example for feature calculation.");
            //finalize examples before feature calcs
        dataManager.finalizeAllExamples();
        getMsgObj().dispInfoMessage(dispClassGeomName,"_finalizeAllMappersBeforeFtrCalc","Finished finalizing examples before feature calc.");
            //feature vector only corresponds to actual -customers- since this is what is used to build the map - build feature vector for customer prospects                
        boolean geomObjFtrBldSuccess = dataManager.buildFeatureVectors();    
        if(!geomObjFtrBldSuccess) {getMsgObj().dispMessage(dispClassGeomName,"finishSOMExampleBuild","Building " + exampleMapperDesc +" Feature vectors failed due to above error (no data available).  Aborting - No features have been calculated for any examples!", MsgCodes.error1);    return;    }

        getMsgObj().dispMessage(dispClassGeomName,"finishSOMExampleBuild","Finished buildFeatureVectors | Begin calculating diffs and mins", MsgCodes.info1);    
            //now get mins and diffs from calc object
        setMinsAndDiffs(getMinBndsAra(), getDiffsBndsAra());  

        getMsgObj().dispMessage(dispClassGeomName,"finishSOMExampleBuild","Finished calculating diffs and mins | Begin building post-feature calc structs for " + exampleMapperDesc +"s (i.e. per-ftr-normed ftrs) dependent on diffs and mins", MsgCodes.info1);    
            //now finalize post feature calc -this will do per-feature-normed features            
        dataManager.buildAfterAllFtrVecsBuiltStructs();        
        
        getMsgObj().dispMessage(dispClassGeomName,"finishSOMExampleBuild","Finished finalize mappers, calculate feature data, diffs, mins, and calculate post-global-ftr-data calcs.", MsgCodes.info5);                        
        //} else {    getMsgObj().dispMessage(dispClassGeomName,"finishSOMExampleBuild","No prospects or products loaded to calculate/finalize.", MsgCodes.warning2);    }
    }//finishSOMExampleBuild    

    /**
     * synthesize training data from currently specified geometric data 
     */
    public void generateTrainingData() {
        float trainTestPartition = this.projConfigData.getTrainTestPartition();
        getMsgObj().dispMessage(dispClassGeomName,"generateAllPreProcExamples","Start Processing all " + geomObjTypeName + " Examples into SOM Training/Testing Examples with " + String.format("%1.2f", (trainTestPartition*100.0f)) +"% Training Data.", MsgCodes.info5);
        buildTrainTestFromPartition(trainTestPartition);
        getMsgObj().dispMessage(dispClassGeomName,"generateAllPreProcExamples","Finished Processing all Preproccessed " + geomObjTypeName + " Examples.  ", MsgCodes.info5);
    }//generateTrainingData
    

    /**
     * using the passed partition information, build the testing and training data partitions
     */
    @Override
    protected void buildTrainTestFromPartition(float trainTestPartition) {
        getMsgObj().dispMessage(dispClassGeomName,"buildTrainTestFromPartition","Starting Building Input, Test, Train data arrays.", MsgCodes.info5);        
        if(!getFlag(srcGeomObjsAllBuiltIDX)) {
            getMsgObj().dispWarningMessage(dispClassGeomName,"buildTrainTestFromPartition","Rebuilding Geometric Objects using current UI Values.");
            buildGeomExampleObjs();
        } 
        if(!getFlag(trainDatObjsAllBuiltIDX)) {
            getMsgObj().dispMessage(dispClassGeomName,"buildTrainTestFromPartition","Attempt to build test/train since not built yet!",MsgCodes.info1);
            resetTrainDataObjs();        
                //synthesize training data using current configuration from UI with loaded data
            trainExDataManager.buildTrainingDataFromGeomObjs(geomSrcToTrainExDataManager, getFlag(allTrainExUniqueIDX), ttlNumTrainExamples);
            trainDatGeomObjects = (SOM_GeomObj[]) trainExDataManager.buildExampleArray();
            setFlag(trainDatObjsAllBuiltIDX,true);
                //finalize and calc ftr vecs on geometry if we have loaded new data 
            finishSOMExampleBuild(trainExDataManager,  ""+geomObjTypeName +" Geom object-derived training example");
            getMsgObj().dispMessage(dispClassGeomName,"buildTrainTestFromPartition","End build test/train since not built yet! - built " + trainDatGeomObjects.length+ " objs in example array.",MsgCodes.info1);
        } 
        //set input data, shuffle it and set test and train partitions
        //only build test and train partitions if training data has been synthesized from geometric examples
        setInputTrainTestShuffleDataAras(trainTestPartition);        
        getMsgObj().dispMessage(dispClassGeomName,"buildTrainTestFromPartition","Finished Building Input, Test, Train, data arrays.", MsgCodes.info5);
    }

    /**
     * this is used to load preprocessed data, calculate features, load specified prebuilt map, map all examples and save results
     * This functionality doesn't need to be available for this application
     */
    @Override
    public final void loadAllDataAndBuildMappings() {}

    @Override
    /**
     * no secondary maps for this project
     */
    protected final int _getNumSecondaryMaps() {        return 0;}
    
    /**
     * we never ignore zero features since these objects will be sufficiently low dimensional to use dense training
     */
    @Override
    public final void setMapExclZeroFtrs(boolean val) {}

    @Override
    protected final void saveAllSegment_BMUReports_Indiv() {}
    
    /**
     * products are zone/segment descriptors corresponding to certain feature, class or category configurations that are descriptive of training data
     */
    @Override
    protected final void setProductBMUs() {    }

    @Override
    /**
     * any instance-class specific code to execute when new map nodes are being loaded
     */
    protected final void initMapNodesPriv() {}

    @Override
    protected final Integer[] getAllClassLabels() {        return new Integer[0];    }

    @Override
    protected final String getClassSegMappingDescrStr() {        return "Sample Point Class Membership";    }

    @Override
    protected final Integer[] getAllCategoryLabels() {        return new Integer[0];    }

    @Override
    protected final String getCategorySegMappingDescrStr() {    return "Sample Point Category Membership";}
    
    @Override
    //return appropriately pathed file name for map image of specified ftr idx
    public String getSOMLocClrImgForFtrFName(int ftrIDX) {        return projConfigData.getSOMLocClrImgForFtrFName(ftrIDX);    }

    /**
     * augment a map of application-specific descriptive quantities and their values, for the SOM Execution human-readable report
     * @param res map already created holding exampleDataMappers create time
     */
    @Override
    public void getSOMExecInfo_Indiv(TreeMap<String, String> res) {

    }

    /**
     * geom-specific SOM value display - TODO
     */
    @Override
    protected final void getDataPointAtLoc_Priv(float x, float y, float sensitivity, SOM_MapNode nearestNode, myPointf locPt, int uiMseDispData) {
        setMseDataExampleNodeName(locPt,nearestNode,sensitivity);        
    }
    /**
     * instancing application should determine whether we want to display features sorted in magnitude order, or sorted in idx order
     * @param ptrLoc
     * @param ftrs
     * @param sens
     * @return
     */
    @Override
    public final void setMseDataExampleFtrs(myPointf ptrLoc, TreeMap<Integer, Float> ftrs, float sens) {
        setMseDataExampleFtrs_IdxSorted(ptrLoc, ftrs, sens);
    }
    ////////////////////////
    // mouse functions
    
    @Override
    protected final boolean checkMouseClick_Indiv(int mouseX, int mouseY, float mapX, float mapY, SOM_MapNode nearestNode,myPoint mseClckInWorld, int btn, boolean _wasSelNotDeSel) {
        return _wasSelNotDeSel;
    }

    @Override
    public final boolean checkMouseDragMove_Indiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D,
            myVector mseDragInWorld, int mseBtn) {
        return false;
    }

    @Override
    protected final void checkMouseRelease_Indiv() {        
    }
    
    /**
     * called from SOM_DataLoarder after all BMUs are loaded and training data bmus are set from bmu 
     * file. Application-specific functionality to handle mapping test data, for example
     */
    @Override
    public void setAllBMUsFromMap() {    }//setAllBMUsFromMap
    
    @Override
    public final int getPreProcDatPartSz() {return preProcDataPartSz;}

    public final float[][] getWorldBounds(){return worldBounds;}
    
    @Override
    public Float[] getTrainFtrMins() {return this.getMinVals(0);}
    @Override
    public Float[] getTrainFtrDiffs() {return this.getDiffVals(0);}
    
    
    public String getGeomObjTypeName() {return geomObjTypeName;}
    
    /**
     * The minimum number of sample points required to build this geometric object
     */
    public abstract int getNumSamplesToBuildObject();
    
    /**
     * The maximum number of training examples to draw to prevent lag/crashing/overflow
     */
    public final int getNumTrainingExsToShow() {
        int maxNumToShow = getMaxNumExsToShow();
        return trainDatGeomObjects.length > maxNumToShow ? maxNumToShow : trainDatGeomObjects.length;
    }
    /**
     * The maximum number of training examples to draw to prevent lag/crashing/overflow
     */
    public abstract int getMaxNumExsToShow();
    
    public final void setGeomObjsBuilt(boolean val) {setFlag(srcGeomObjsAllBuiltIDX, val);}
    public final boolean getGeomObjsBuilt() {return getFlag(srcGeomObjsAllBuiltIDX);}    
    
    public final void setTrainDataObjsBuilt(boolean val) {setFlag(trainDatObjsAllBuiltIDX, val);}
    public final boolean getTrainDataObjsBuilt() {return getFlag(trainDatObjsAllBuiltIDX);}    
    
    public final void setTrainExShouldBeUnqiue(boolean val) {setFlag(allTrainExUniqueIDX, val);}
    public final boolean getTrainExShouldBeUnqiue() {return getFlag(allTrainExUniqueIDX);}    
        
    @Override
    protected final int getNumFlags() {    return getNumGeomFlags_Indiv();}
    protected abstract int getNumGeomFlags_Indiv();
    @Override
    protected final void setFlag_Indiv(int idx, boolean val) {
        switch (idx) {//special actions for each flag
            case srcGeomObjsAllBuiltIDX         : {break;}
            case trainDatObjsAllBuiltIDX         : {break;}
            case allTrainExUniqueIDX            : {break;}
            default : {setGeomFlag_Indiv(idx, val); break;}
        }
    }
    /**
     * instancing-class specific flag handling
     * @param idx
     * @param val
     */
    protected abstract void setGeomFlag_Indiv(int idx, boolean val);

    ////////////////////////////
    // draw functions
    
    @Override
    protected void initMapArasIndiv(int w, int h, int format, int num2ndFtrVals) {        
    }

    @Override
    protected void drawSegmentsUMatrixDispIndiv(IGraphicsAppInterface pa) {            
    }
    
    public final void drawSelectedMapNodeGeomObjs(IGraphicsAppInterface pa, float animTimeMod, boolean showLabels, boolean useLocAsClr) {
        if((SelectedMapNodes!= null) && (SelectedMapNodes.size() > 0)) {
            if(useLocAsClr) {
                for(SOM_MapNode node : SelectedMapNodes.values()) {    ((SOM_GeomMapNode) node).getVisObj().drawMeSelected_ClrLoc(pa, animTimeMod, false);}
            } else {
                for(SOM_MapNode node : SelectedMapNodes.values()) {    ((SOM_GeomMapNode) node).getVisObj().drawMeSelected_ClrRnd(pa, animTimeMod, false);}            
            }
            if(showLabels) {for(SOM_MapNode node : SelectedMapNodes.values()) {    ((SOM_GeomMapNode) node).getVisObj().drawMyLabel(pa, dispWin);}    }
        }
    }
    
    @Override
    protected void drawMapRectangle_Indiv(IGraphicsAppInterface pa, int curImgNum) {    
        drawMseOverData(pa);            
    }

    @Override
    protected void drawPerFtrMap_Indiv(IGraphicsAppInterface pa) {        
    }

    @Override
    protected final float getPreBuiltMapInfoDetail(IGraphicsAppInterface pa, String[] str, int i, float yOff, boolean isLoaded) {
        return yOff;
    }
    
    @Override
    protected final float drawResultBarPriv1(IGraphicsAppInterface pa, float yOff) {
        return yOff;
    }

    @Override
    protected final float drawResultBarPriv2(IGraphicsAppInterface pa, float yOff) {
        return yOff;
    }

    @Override
    protected final float drawResultBarPriv3(IGraphicsAppInterface pa, float yOff) {
        return yOff;
    }
    
    /**
     * for saving bmu reports based on ftr vals
     */
    @Override
    public String getFtrWtSegmentTitleString(SOM_FtrDataType ftrCalcType, int ftrIDX) {        
        String ftrTypeDesc = getDataDescFromInt(ftrCalcType);
        return "Feature Weight Segment using " + ftrTypeDesc +" examples for ftr idx : " + ftrIDX;
    }

}//class SOM_GeomMapManager
