package base_SOM_Objects.som_exampleFeatures.base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * instances of this class will hold all the different variants 
 * of features for a single training example.  Instances will manage whether 
 * the example is sparse or dense, as well as all access.
 * There will be 2 instances of this class - sparse and dense.  The sparse
 * features will hold the feature data in a map, while the dense features will
 * hold the data in an array.
 * 
 * @author john
 *
 */
public abstract class SOM_Features {
    //owning example
    protected SOM_Example ex;
    //number of features in this example - make static?  should always be the same for all examples
    //this will stay the same across all examples and map nodes
    protected int numFtrs;    
    //magnitude of this feature vector
    public float ftrVecMag;    
    
    //keys for ftr map arrays
    /**
     *     keys for ftr map arrays
     */
    protected static final int 
        unNormFtrMapTypeKey = SOM_FtrDataType.UNNORMALIZED.getVal(), 
        perFtrNormMapTypeKey = SOM_FtrDataType.FTR_NORM.getVal(), 
        perExNormMapTypeKey = SOM_FtrDataType.EXMPL_NORM.getVal();    
    protected static final Integer[] ftrMapTypeKeysAra = new Integer[] {unNormFtrMapTypeKey, perFtrNormMapTypeKey, perExNormMapTypeKey};
    
    //idx's in feature vector that have non-zero values
    public ArrayList<Integer> allNonZeroFtrIDXs;    
    
    //these objects are for reporting on individual examples.  
    //use a map per feature type : (unnormalized, normed per feature across all data, normed per example),to hold the features sorted by weight as key, value is array of ftrs at a particular weight -submap needs to be instanced in descending key order
    protected TreeMap<Float, ArrayList<Integer>>[] mapOfWtsToFtrIDXs;    
    //a map per feature type : unmodified, normalized, standardized, of ftr IDXs and their relative "rank" in this particular example, as determined by the weight calc
    protected TreeMap<Integer,Integer>[] mapOfFtrIDXVsWtRank;        

    public SOM_Features(SOM_Example _ex,int _numFtrs) {
        ex = _ex;
        numFtrs = _numFtrs;
        initFtrs();
    }    
    
    public SOM_Features(SOM_Features _otr) {
        ex = _otr.ex;
        numFtrs = _otr.numFtrs;
        ftrVecMag = _otr.ftrVecMag;
        allNonZeroFtrIDXs = _otr.allNonZeroFtrIDXs;
        mapOfWtsToFtrIDXs = _otr.mapOfWtsToFtrIDXs;
        mapOfFtrIDXVsWtRank = _otr.mapOfFtrIDXVsWtRank;        
    }//copy ctor
    
    /**
     * Initialize all ftr values.  Initially called from base class constructor
     */
    protected abstract void initFtrs();
    //clear instead of reinstance - if ftr maps are cleared then compFtrMaps should be cleared as well
    protected abstract void clearAllFtrMaps() ;    
    protected abstract void clearFtrMap(int idx);
    //clear instead of reinstance
    protected abstract void clearAllCompFtrMaps();
    protected abstract void clearCompFtrMap(int idx);


    //initialize structures used to aggregate and report the ranking of particular ftrs for this example
    @SuppressWarnings("unchecked")
    private final void initPerFtrObjs() {
        mapOfWtsToFtrIDXs = new TreeMap[ftrMapTypeKeysAra.length];
        mapOfFtrIDXVsWtRank = new TreeMap[ftrMapTypeKeysAra.length];
        for(Integer ftrType : ftrMapTypeKeysAra) {//type of features
            mapOfWtsToFtrIDXs[ftrType] = new TreeMap<Float, ArrayList<Integer>>(new Comparator<Float>() { @Override public int compare(Float o1, Float o2) {   return o2.compareTo(o1);}});//descending key order
            mapOfFtrIDXVsWtRank[ftrType] = new TreeMap<Integer,Integer>();
        }
    }//initPerFtrObjs()
    
    //to execute per-example ftr-based reporting, first call buildFtrRptStructs for all examples, then iterate through getters for ftr type == mapToGet, then call clearFtrRptStructs to release memory
    //called to report on this example's feature weight rankings 
    public final void buildFtrRprtStructs() {
        if(ex.getFlag(SOM_Example.ftrWtRptBuiltIDX)) {return;}
        initPerFtrObjs();
        //for every feature type build report structures        
        buildFtrRprtStructs_Indiv();

        ex.setFlag(SOM_Example.ftrWtRptBuiltIDX, true);
    }//buildFtrReports
    /**
     * build feature report structures for specific type of features
     */
    protected abstract void buildFtrRprtStructs_Indiv();
    
    
    /**
     * return mapping of ftr IDXs to rank for this example
     * @param mapToGet type of map to get
     * @return
     */
    public final TreeMap<Integer,Integer> getMapOfFtrIDXBsWtRank(int mapToGet){
        if(!ex.getFlag(SOM_Example.ftrWtRptBuiltIDX)) {
            ex.mapMgr.getMsgObj().dispMessage("SOMExample" + ex.OID,"getMapOfFtrIDXBsWtRank","Feature-based report structures not yet built. Aborting.", MsgCodes.warning2);
            return null;
        }
        return mapOfFtrIDXVsWtRank[mapToGet];
    }//getMapOfFtrIDXBsWtRank
    
    public final TreeMap<Float, ArrayList<Integer>> getMapOfWtsToFtrIDXs(int mapToGet){
        if(!ex.getFlag(SOM_Example.ftrWtRptBuiltIDX)) {
            ex.mapMgr.getMsgObj().dispMessage("SOMExample" + ex.OID,"getMapOfWtsToFtrIDXs","Feature-based report structures not yet built. Aborting.", MsgCodes.warning2);
            return null;
        }
        return mapOfWtsToFtrIDXs[mapToGet];
    }//getMapOfWtsToFtrIDXs
        
    //clears out structures used for reports, to minimize memory footprint
    public final void clearFtrRprtStructs() {        
        mapOfWtsToFtrIDXs = null;
        mapOfFtrIDXVsWtRank = null;        
        ex.setFlag(SOM_Example.ftrWtRptBuiltIDX, false);        
    }//clearFtrReports
    
    /**
     * set all non-zero features
     */
    public abstract void buildAllNonZeroFtrIDXs();
    
    /**
     * return SVM-format (sparse) string of this object's features, depending on which type is selected - check to make sure 2ndary features exist before attempting to build data strings
     * @param _type type of features
     * @return string of appropriately formatted features
     */
    public abstract String _toSVMString(int _type);
    
    /**
     * build a feature vector from the map of ftr values
     * @param _type type of features to retrieve
     * @return array holding all n features, including 0 values
     */
    public abstract float[] _getFtrsFromMap(int _type);    
    
    /**
     * build a map of features, keyed by ftr idx and value == ftr val @ idx
     * @param _type type of features to retrieve
     * @return array holding all n features, including 0 values
     */
    public abstract TreeMap<Integer, Float> _getFtrMap(int _type);    
    
    
    /**
     * set # of feature to be seen in dataset owned by this example
     * when this value changes, need to reinitialize ftr structs
     * @param _numFtrs
     */
    public void setNumFtrs(int _numFtrs) {
        numFtrs = _numFtrs;
        initFtrs();
    }


}//SOMFeatures
