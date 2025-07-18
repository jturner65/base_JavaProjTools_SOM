package base_SOM_Objects.som_examples.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;

import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.tuples.Tuple;
import base_SOM_Objects.som_examples.base.visualization.baseDataPtVis;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_SOM_Objects.som_segments.segments.SOM_MappedSegment;
import base_Utils_Objects.io.messaging.MsgCodes;


/**
 * NOTE : None of the data types in this file are thread safe so do not allow for opportunities for concurrent modification
 * of any instanced object in this file. This decision was made for speed concerns - 
 * concurrency-protected objects have high overhead that proved greater than any gains in 10 execution threads.
 * 
 * All multithreaded access of these objects should be designed such that any individual object is only accessed by 
 * a single thread.
 * 
 * This is the base class describing an example data point to be used to train a SOM
 * @author john turner
 */
public abstract class SOM_Example extends baseDataPtVis{    
    /**
     * Map manager reference
     */
    public SOM_MapManager mapMgr;
    /**
     * type of example data this is
     */
    protected SOM_ExDataType exampleDataType;

    //unique key field used for this example
    public final String OID;
    //structure holding all features for this example
    //private SOM_Features features;
    
    //# of features the dataset this example is from has (including zero features)
    //this will stay the same across all examples and map nodes
    protected int numFtrs;
    /**
     * use a map to hold only sparse data of each frmt for feature vector
     */
    protected TreeMap<Integer, Float>[] ftrMaps;    
    /**
     * this map is what is used by examples to compare for mappings - this may 
     * include combinations of other features or values when all ftrs are calculated 
     * (unnormalized, normed per feature across all data, normed per example) they need to be mapped to this structure, 
     * possibly in combination with an alternate set of features or other calculations
     *                                                        
     */
    protected TreeMap<Integer, Float>[] compFtrMaps;
    /**
     * magnitude of this feature vector
     */
    public float ftrVecMag;    
    /**
     * idx's in feature vector that have non-zero values
     */
    public ArrayList<Integer> allNonZeroFtrIDXs;    
    /**
     *     keys for ftr map arrays
     */
    protected static final int 
        unNormFtrMapTypeKey = SOM_FtrDataType.UNNORMALIZED.getVal(), 
        perFtrNormMapTypeKey = SOM_FtrDataType.FTR_NORM.getVal(), 
        perExNormMapTypeKey = SOM_FtrDataType.EXMPL_NORM.getVal();    
    protected static final Integer[] ftrMapTypeKeysAra = new Integer[] {unNormFtrMapTypeKey, perFtrNormMapTypeKey, perExNormMapTypeKey};
    
    /**
     * probability structure for this example - probability of every map node for each 
     * class this example covers, keyed by class, value is map keyed by mapnode tuple loc, value is 
     * probability of class (ratio of class against all classes mapped to map node)    
     */    
    protected ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>> perClassMapNodeProbMap;
    /**
     * probability structure for this example - probability of every map node for each category 
     * this example covers keyed by category ID, value is map keyed by mapnode tuple loc, value is 
     * probability of category (ratio of category to all categories mapped to map node)
     */
    protected ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>> perCategoryMapNodeProbMap;
    
    /**
     * map keyed by category/class and value is counts of this category/class occurrence (generally this will be 1)
     * Key is tuple t where t.x == category and t.y==class.  Category can be null, but if it is null for any 
     * examples it will be assumed to be null for all examples, and if it is not null for any then it must be non-null for all
     */
    private TreeMap<Tuple<Integer, Integer>, Integer> catClassCountsForExample;

    /**
     * state flags - bits in array holding relevant process info
     */
    private int[] stFlags;                        
    public static final int
            debugIDX                 = 0,
            ftrsBuiltIDX            = 1,            //whether particular kind of feature was built or not
            perFtrNormBuiltIDX        = 2,            //..per feature normalized examples (Across all examples per feature)
            perExmplNormBuiltIDX    = 3,            //..per example normalized (feature vector scaled to have magnitude 1
            isBadTrainExIDX            = 4,            //whether this example is a good one or not - if all feature values == 0 then this is a useless example for training. only set upon feature vector calc
            ftrWtRptBuiltIDX        = 5;            //whether or not the structures used to calculate feature-based reports for this example have been calculated
        
    public static final int numFlags = 6;    
    
    /**
     * reference to map node that best matches this example node
     */
    private SOM_MapNode bmu;            
    /**
     * this is the squared distance, using the chosen distance measure, to the best matching unit of the map for this example
     */
    private double _sqDistToBMU;
    //to hold 9 node neighborhood surrounding bmu - using array of nodes because nodes can be equidistant form multiple nodes
    //TODO set a list of these nodes for each SOMMapNodeExample upon their construction? will this speed up anything?
    private TreeMap<Double, ArrayList<SOM_MapNode>> mapNodeNghbrs;
    /**
     * hash code for using in a map
     */
    private final int _hashCode;
    
    /**
     * is this datapoint used for training; 
     */
    protected boolean isTrainingData;
    /**
     * this is index for this data point in training/testing data array; 
     */
    protected int testTrainDataIDX;
    /**
     * two maps of distances to each map node for each example, with 
     * each array either including unshared features or excluding unshared 
     * features in distance calc keyed by distance, array list is list 
     * of map nodes at that distance
     */
    protected TreeMap<Double,ArrayList<SOM_MapNode>>[] allMapNodesDists;    
    /**
     * idxs in allMapNodesDists
     */
    public static final int
        AllFtrsIDX = 0,                //looks at all features in this node for distance calculations
        SharedFtrsIDX = 1;            //looks only at non-zero features in this node for distance calculations
    protected static int numFtrCompVals = 2;

    /**
     * segments this product belongs to, based on features, classes and categories 
     * There will be 1 entry for all ftrs, classes and categories this example belongs to    
     */
    private TreeMap<Integer, SOM_MappedSegment> ftrWtSegData;            //keyed by non-zero ftr index    
    private TreeMap<Integer, SOM_MappedSegment> class_SegData;            //segment membership manager class mapping
    private TreeMap<Integer, SOM_MappedSegment> categorys_SegData;        //segment membership manager category mapping
    
    public SOM_Example(SOM_MapManager _map, SOM_ExDataType _type, String _id) {
        super(_map.getClrFillStrkTxtAra(_type), _map.getAltClrFillStrkTxtAra());
        mapMgr = _map;
        exampleDataType = _type;
        OID = _id;
        set_sqDistToBMU(0.0);
        initFlags();    
        initAllStructs();
        String tmp = OID + "" + exampleDataType;
        _hashCode = tmp.hashCode();
        _initSegStructs();
    }//ctor
    
    //copy ctor - shallow copy of _otr - used to provide casting between nearly identical example types
    //it is expected that _otr ref will be disposed
    public SOM_Example(SOM_Example _otr) {
        super(_otr);
        OID = _otr.OID;
        exampleDataType = _otr.exampleDataType;
        setBmu(_otr.getBmu());
        set_sqDistToBMU(_otr.get_sqDistToBMU());
        _hashCode = _otr._hashCode;
        
        //features = _otr.features;
        
        ftrMaps = _otr.ftrMaps;
        compFtrMaps = _otr.compFtrMaps;
        ftrVecMag = _otr.ftrVecMag;
        allNonZeroFtrIDXs = _otr.allNonZeroFtrIDXs;
        mapNodeNghbrs = _otr.mapNodeNghbrs;
        isTrainingData = _otr.isTrainingData;
        testTrainDataIDX = _otr.testTrainDataIDX;
        allMapNodesDists = _otr.allMapNodesDists;
        ftrWtSegData = _otr.ftrWtSegData;    
        class_SegData  = _otr.class_SegData;        
        categorys_SegData  = _otr.categorys_SegData;        
        perClassMapNodeProbMap = _otr.perClassMapNodeProbMap;
        perCategoryMapNodeProbMap = _otr.perCategoryMapNodeProbMap;

        stFlags = _otr.stFlags;        
    }//copy ctor
    
    public int getTypeVal() {return exampleDataType.getVal();}
    public SOM_ExDataType getType() {return exampleDataType;}
    /**
     * call only on construction or change of # of featrues, to enable 
     * structs holding feature values and other pertinent information 
     * to remain constructed throughout life of example
     */
    @SuppressWarnings("unchecked")
    private void initAllStructs() {
        catClassCountsForExample = new TreeMap<Tuple<Integer, Integer>, Integer>();
        mapNodeNghbrs = new TreeMap<Double, ArrayList<SOM_MapNode>>();
        ftrMaps = new TreeMap[ftrMapTypeKeysAra.length];
        for (int i=0;i<ftrMaps.length;++i) {            ftrMaps[i] = new TreeMap<Integer, Float>();         }
        compFtrMaps = new TreeMap[ftrMapTypeKeysAra.length];        
        for (int i=0;i<compFtrMaps.length;++i) {            compFtrMaps[i] = new TreeMap<Integer, Float>();         }        
    }
    /**
     * set # of feature to be seen in dataset owned by this example
     * @param _numFtrs
     */
    public void setNumFtrs(int _numFtrs) {numFtrs = _numFtrs;}
    
    private void _initSegStructs() {
        ftrWtSegData = new TreeMap<Integer, SOM_MappedSegment>();        //keyed by non-zero ftr index                       
        class_SegData    = new TreeMap<Integer, SOM_MappedSegment>();    //segment membership manager class mapping         
        categorys_SegData = new TreeMap<Integer, SOM_MappedSegment>();    //segment membership manager category mapping     
        perClassMapNodeProbMap = new ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>>();
        perCategoryMapNodeProbMap = new ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>>();
    }
    
    //mapped segments for this example
    public synchronized void addFtrSegment(int idx, SOM_MappedSegment seg) {        ftrWtSegData.put(idx, seg);}
    public synchronized void addClassSegment(int idx, SOM_MappedSegment seg) {        class_SegData.put(idx, seg);}
    public synchronized void addCategorySegment(int idx, SOM_MappedSegment seg) {    categorys_SegData.put(idx, seg);}
    
    public synchronized SOM_MappedSegment getFtrSegment(int idx) {        return ftrWtSegData.get(idx);}
    public synchronized SOM_MappedSegment getClassSegment(int idx) {        return class_SegData.get(idx);}
    public synchronized SOM_MappedSegment getCategorySegment(int idx) {        return categorys_SegData.get(idx);}
    /**
     * initialize the map of category/class-keyed counts of incidents for this example
     */
    protected final void initCatClassCountsForExample() {catClassCountsForExample.clear();}    
    /**
     * return a map keyed by category/class tuple, with value being count of 
     * incidents of this class in this example. these will be used as class data 
     * for the bmu node of this example.
     * If category is not used, null category should be supported by consuming functions.
     * THIS SHOULD ONLY BE USED WHEN THIS EXAMPLE IS A TRAINING EXAMPLE; OTHERWISE SHOULD BE ABLE TO RETURN NULL
     * @return
     */
    public final TreeMap<Tuple<Integer, Integer>, Integer> getCatClassCountsForExample(){return catClassCountsForExample;}
    
    /**
     * set the cat/class-keyed map of cat/class incident counts
     */
    public final void setCatClassCountsForExample(TreeMap<Tuple<Integer, Integer>, Integer> _map){catClassCountsForExample = _map;}
    
    


    /**
     * set this example's segment membership and probabilities from the mapped bmu 
     * class/category label-driven examples won't use this function but will rather 
     * poll -all- class/category mappings on map itself, using setSegmentsAndProbsFromAllMapNodes
     * to determine segment probs
     */
    public synchronized void setSegmentsAndProbsFromBMU() {
        //build this node's segment membership and probabilities based on its BMU
        //verify not null
        perClassMapNodeProbMap.clear();        
        perCategoryMapNodeProbMap.clear();
        if(isBmuNull()) {    msgObj.dispMessage("SOM_Example","setSegmentsAndProbsFromBMU","Error !!! No BMU defined for this example : " + OID + " | Aborting further segment calculations.",MsgCodes.warning2 ); return;    }
        Tuple<Integer,Integer> bmuCoords = getBMUMapNodeCoord();
        //set all class-based map node probabilities
        ConcurrentSkipListMap<Tuple<Integer,Integer>,Float> classPerMapNodes;
        Set<Integer> classKeySet = getBMUClassSegIDs();
        for(Integer cls : classKeySet) {
            addClassSegment(cls, getBMUClassSegment(cls));
            classPerMapNodes = new ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>();
            classPerMapNodes.put(bmuCoords, getBMUProbForClass(cls));
            perClassMapNodeProbMap.put(cls, classPerMapNodes);            
        }
        //set all category-based map node probabilities                
        ConcurrentSkipListMap<Tuple<Integer,Integer>,Float> catPerMapNodes;        
        Set<Integer> catKeySet = getBMUCategorySegIDs();
        for(Integer cat : catKeySet) {
            addCategorySegment(cat, getBMUCategorySegment(cat));
            catPerMapNodes = new ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>();
            catPerMapNodes.put(bmuCoords, getBMUProbForCategory(cat));
            perCategoryMapNodeProbMap.put(cat, catPerMapNodes);
        }        
    }//setAllMapNodeSegmentsAndProbs    
    /**
     * set this example's segment membership and probabilities from all map nodes' 
     * class/category membership.  This is for examples who are not BMU driven but rather
     * are class/category driven, such as product examples in a product-purchase-based
     * data environment
     * @param Class_Segments keyed by class ID, value is class segment
     * @param Category_Segments keyed by category ID, value is category segment
     */
    public synchronized void setSegmentsAndProbsFromAllMapNodes(TreeMap<Integer, SOM_MappedSegment> Class_Segments, TreeMap<Integer, SOM_MappedSegment> Category_Segments) {
        //set all class-based map node probabilities
        perClassMapNodeProbMap.clear();        
        ConcurrentSkipListMap<Tuple<Integer,Integer>,Float> clsPerMapNodes, tmpClsMapNodes;
        HashSet<Integer> allClassIDs = getAllClassIDsForClsSegmentCalc();
        for(Integer cls : allClassIDs) {
            clsPerMapNodes = mapMgr.getMapNodeClassProbsForClass(cls);
            tmpClsMapNodes = new ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>();
            for(Tuple<Integer,Integer> key : clsPerMapNodes.keySet()) {                tmpClsMapNodes.put(key, clsPerMapNodes.get(key));            }
            perClassMapNodeProbMap.put(cls, tmpClsMapNodes);
            addClassSegment(cls, Class_Segments.get(cls));
        }
        //set all category-based map node probabilities
        perCategoryMapNodeProbMap.clear();
        ConcurrentSkipListMap<Tuple<Integer,Integer>,Float> catPerMapNodes, tmpCatMapNodes;    
        HashSet<Integer> allCatIDs = getAllCategoryIDsForCatSegmentCalc();
        for(Integer cat : allCatIDs) {
            catPerMapNodes = mapMgr.getMapNodeCategoryProbsForCategory(cat);
            tmpCatMapNodes = new ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>();
            for(Tuple<Integer,Integer> key : catPerMapNodes.keySet()) {                tmpCatMapNodes.put(key, catPerMapNodes.get(key));            }
            perCategoryMapNodeProbMap.put(cat, tmpCatMapNodes);
            addCategorySegment(cat, Category_Segments.get(cat));
        }        
    }//setAllMapNodeSegmentsAndProbs    
    
    /**
     * Return all the relevant classes found in this example/that this example belongs 
     * to, for class segment calculation
     * @return class IDs present in this example
     */
    protected abstract HashSet<Integer> getAllClassIDsForClsSegmentCalc();
    /**
     * Return all the relevant categories found in this example or that this example 
     * belongs to, for category segment membership calculation
     * @return category IDs present in this example
     */
    protected abstract HashSet<Integer> getAllCategoryIDsForCatSegmentCalc();
    
    /**
     * class probability structure for this example - probability of every node for each class 
     * this example covers keyed by class ID, value is map keyed by mapnode tuple loc, 
     * value is probablity of class (ratio of class to all classes mapped to map node)
     * @return
     */
    public final ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>> getPerClassMapNodeProbMap(){return perClassMapNodeProbMap;};
    /**
     * category probability structure for this example - probability of every node for each 
     * category this example covers keyed by category ID, value is map keyed by mapnode 
     * tuple loc, value is probablity of category (ratio of category to all categories 
     * mapped to map node)
     * @return
     */
    public final ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Tuple<Integer,Integer>,Float>> getPerCategoryMapNodeProbMap(){return perCategoryMapNodeProbMap;}

    /**
     * clear instead of reinstance - if ftr maps are cleared then compFtrMaps should be cleared as well
     */
    protected synchronized void clearAllFtrMaps() {for (int i=0;i<ftrMaps.length;++i) {    clearFtrMap(i); ftrMaps[i].clear();compFtrMaps[i].clear();}}    
    /**
     * clear instead of reinstance - if ftr maps are cleared then compFtrMaps should be cleared as well
     */
    protected synchronized void clearFtrMap(int idx) {ftrMaps[idx].clear(); compFtrMaps[idx].clear();}
    /**
     * clear instead of reinstance
     */
    protected synchronized void clearAllCompFtrMaps() {for (int i=0;i<compFtrMaps.length;++i) {    compFtrMaps[i].clear();}}
    /**
     * clear instead of reinstance
     */
    protected synchronized void clearCompFtrMap(int idx) {compFtrMaps[idx].clear();}

    /**
     * build feature vector
     */
    protected abstract void buildFeaturesMap();    
    /**
     * normalize this feature vector per feature vector across all examples - scale each feature value by min and max feature value seen across all data
     */
    protected abstract void buildPerFtrNormMap();
    /**
     * required info for this example to build feature data - use this so we don't have to reload and rebuilt from data every time
     * @return
     */
    public abstract String getPreProcDescrForCSV();    
    /**
     * column names of rawDescrForCSV data
     * @return
     */
    public abstract String getRawDescColNamesForCSV();
    /**
     * finalization after being loaded from raw/preprocessed record but before ftrs are calculated
     */
    public abstract void finalizeBuildBeforeFtrCalc();
    
    public boolean isBadExample() {return getFlag(isBadTrainExIDX);}
    public void setIsBadExample(boolean val) { setFlag(isBadTrainExIDX,val);}
    
    //debugging tool to find issues behind occasional BMU seg faults
//    protected boolean checkForErrors(SOMMapNode _n, float[] dataVar){
//        if(mapMgr == null){                    mapMgr.msgObj.dispMessage("SOMMapNodeExample","checkForErrors","FATAL ERROR : SOMMapData object is null!");        return true;}//if mapdata is null then stop - should have come up before here anyway
//        if(_n==null){                            mapMgr.msgObj.dispMessage("SOMMapNodeExample","checkForErrors","_n is null!");         return true;} 
//        if(_n.mapLoc == null){                    mapMgr.msgObj.dispMessage("SOMMapNodeExample","checkForErrors","_n has no maploc!");    return true;}
//        if(dataVar == null){                    mapMgr.msgObj.dispMessage("SOMMapNodeExample","checkForErrors","map variance not calculated : datavar is null!");    return true;    }
//        return false;
//    }//checkForErrors
    
    /**
     * add passed map node, with passed feature distance, to neighborhood nodes using a map of arrays so that we can precalc distances 1 time
     * @param _n
     * @param _dist
     */
    protected final void addMapNodeToNeighbrhdMap(SOM_MapNode _n, double _dist) {
        ArrayList<SOM_MapNode> tmpMap = mapNodeNghbrs.get(_dist);
        if (null==tmpMap) {tmpMap = new ArrayList<SOM_MapNode>();}
        tmpMap.add(_n);
        mapNodeNghbrs.put(_dist, tmpMap);        
    }//addMapUnitToNeighbrhdMap
    
    /**
     * assign passed map node to be bmu
     * @param _n
     * @param _dist
     */
    protected final void _setBMUAddToNeighborhood(SOM_MapNode _n, double _dist) {
        //if (checkForErrors(_n, mapMgr.map_ftrsVar)) {return;}//if true then this is catastrophic error and should interrupt flow here
        setBmu(_n);    
        set_sqDistToBMU(_dist);
        //this is updated to exact location after neighbor hood map is built
        mapLoc.set(_n.mapLoc);    
        //once BMU and distToBMU is set, init map and add node to neighborhood map keyed by dist
        mapNodeNghbrs.clear();
        ArrayList<SOM_MapNode> tmpMap = new ArrayList<SOM_MapNode>();
        tmpMap.add(_n);
        //initialize mpNodeLoc to bmu location
        mapNodeLoc.set(_n.mapLoc);
        mapNodeNghbrs.put(_dist, tmpMap);        //to hold 9 neighbor nodes and their ftr distance        
    }//_setBMUAddToNeighborhood
    
    /**
     * this example has no values in any feature and so has no bmu
     */
    protected final void _setNullBMU() {
        setBmu(null);
        set_sqDistToBMU(Double.MAX_VALUE);
        mapLoc.set(0,0,0);
        mapNodeNghbrs.clear();
    }
    
    public boolean isBmuNull() {return (null==bmu);}
    
    /**
     * get class probability from bmu for passed class treat this example's 
     * probability for a particular class as the probability of its BMU 
     * for that class (# examples of that class divided by total # of class seen at that node)
     * @param cls
     * @return
     */
    public float getBMUProbForClass(Integer cls) {
        if(null==bmu) {return 0.0f;}
        return bmu.getClassProb(cls);
    }
    
    /**
     * get category probability from bmu for this example for passed category treat this example's 
     * probability for a particular category as the probability of its BMU 
     * for that category (# examples of that category divided by total # of category seen at that node)
     * @param cls
     * @return
     */    
    public float getBMUProbForCategory(Integer category) {
        if(null==bmu) {return 0.0f;}
        return bmu.getCategoryProb(category);
    }
    
    
    /**
     * assumes bmu exists and is not null
     * @return
     */
    public Set<Integer> getBMUClassSegIDs(){                return bmu.getClassSegIDs();}
    /**
     * assumes bmu exists and is not null
     * @return
     */
    public Set<Integer> getBMUCategorySegIDs(){                return bmu.getCategorySegIDs();}
    /**
     * assumes bmu exists and is not null
     * @return
     */
    public SOM_MappedSegment getBMUClassSegment(int cls) {    return bmu.getClassSegment(cls);}
    /**
     * assumes bmu exists and is not null
     * @return
     */
    public SOM_MappedSegment getBMUCategorySegment(int cat) {    return bmu.getCategorySegment(cat);}
    /**
     * assumes bmu exists and is not null
     * @return
     */
    public Tuple<Integer,Integer> getBMUMapNodeCoord(){    return bmu.mapNodeCoord;}
    
    ////////////////////////////////////
    // these always use ftr weight vectors because map nodes always only use ftrs

    /**
     * this adds the passed node as this _training_ example's best matching unit on 
     * the map - this is used for -training- examples having been set as bmus by the SOM 
     * executable, hence why it uses the full distance and does not exclude zero features 
     * in this example; also does not use comparison vector but actual ftrMaps/training 
     * vector for the above reason                                        
     * 
     * this also adds this data point to the map's node with a key of the distance 
     * @param _n bmu for this example
     * @param _ftrType type of features used for comparison (unnormalized, normed per feature across all data, normed per example)
     */
    public final void setTrainingExBMU(SOM_MapNode _n, int _ftrType){
        _setBMUAddToNeighborhood(_n,getSqDistFromFtrType(_n.ftrMaps[_ftrType],  ftrMaps[_ftrType]));
        //find ftr distance to all 8 surrounding nodes and add them to mapNodeNeighbors
        buildNghbrhdMapNodes( _ftrType, this::getSqDistFromFtrType);
    }//setBMU
    
    /**
     * this adds the passed node as this _training_ example's best matching unit on 
     * the map - this is used for -training- examples having been set as bmus by the SOM 
     * executable, hence why it uses the full distance and does not exclude zero features 
     * in this example; also does not use comparison vector but actual ftrMaps/training 
     * vector for the above reason                                        
     * 
     * this also adds this data point to the map's node with a key of the distance 
     * @param _n bmu for this example
     * @param _ftrType type of features used for comparison (unnormalized, normed per feature across all data, normed per example)
     */
    public final void setTrainingExBMU_ChiSq(SOM_MapNode _n, int _ftrType){
        _setBMUAddToNeighborhood(_n,getSqDistFromFtrType_ChiSq(_n.ftrMaps[_ftrType],  ftrMaps[_ftrType]));
        //find ftr distance to all 8 surrounding nodes and add them to mapNodeNeighbors
        buildNghbrhdMapNodes( _ftrType, this::getSqDistFromFtrType_ChiSq);
    }//setBMU
    
    /**
     * use 9 map node neighborhood around this node, accounting for torroidal map, 
     * to find exact location on map for this node (put in mapLoc) by using weighted 
     * average of mapNodeLocs of neighbor nodes, where the weight is the inverse feature 
     * distance mapNodeNghbrs (9 node neighborood) must be set before this is called, 
     * and has bmu set as closest, with key being distance                                                     
     * @param _ftrType type of feature used for calculations (unnormalized, normed per feature across all data, normed per example)
     * @param _distFunc function used to calculate distance (euclidean or chi-squared (scaled by variance))
     */
    protected final void buildNghbrhdMapNodes(int _ftrType, BiFunction<TreeMap<Integer, Float>, TreeMap<Integer, Float>, Double> _distFunc){
        int mapColsSize = mapMgr.getMapNodeCols(), mapRowSize = mapMgr.getMapNodeRows();
        int mapCtrX = bmu.mapNodeCoord.x, mapCtrY = bmu.mapNodeCoord.y;
        Integer xTup, yTup;
        TreeMap<Tuple<Integer,Integer>, SOM_MapNode> mapNodes = mapMgr.getMapNodes();
        //measuring distance to map node neighbors around bmu
        for (int x=-1; x<2;++x) {//should be 3 cols
            xTup = (mapCtrX + x + mapColsSize) % mapColsSize;
            for (int y=-1; y<2;++y) {//3 rows
                if((y==0) && (x==0)){continue;}//ignore "center" node - this is bmu
                yTup = (mapCtrY + y + mapRowSize) % mapRowSize;        
                Tuple<Integer,Integer> key = new Tuple<Integer, Integer>(xTup, yTup);
                SOM_MapNode mapNode = mapNodes.get(key);
                double dist = _distFunc.apply(mapNode.ftrMaps[_ftrType], ftrMaps[_ftrType]);
                addMapNodeToNeighbrhdMap(mapNode, dist);
            }//for each row/y
        }//for each column/x    
        setExactMapLoc();
    }//addAllMapNodeNeighbors
    
    //calculate appropriate modifier based on where neighbor node is related to where bmu node is - need to account for wrapping
    private final float calcWrapMod(Integer bmuCoord, Integer neighborCoord, float mapDim) {
        float mod = 0.0f;
        if (neighborCoord > bmuCoord+1) {        mod = mapDim; }//this means that the example is actually lower than bmu, but due to wrap it is on the other side of the map, so add
        else if (neighborCoord < bmuCoord-1) {    mod = -mapDim;}//this means that the example is actually higher coord than bmu, but wrapped around to the other side, so subtract dim 
        return mod;
    }//calcWrapMod
    
    //return location of passed map node, with value added or subtracted based on whether it wraps around map
    private final myPointf findNodeLocWrap(SOM_MapNode mapNode, Integer bmuX, Integer bmuY, float mapW, float mapH) {
        Integer mapNode_X = mapNode.mapNodeCoord.x, mapNode_Y = mapNode.mapNodeCoord.y;
        myPointf loc = new myPointf(mapNode.mapLoc);
        float locXMod = calcWrapMod(bmuX, mapNode_X, mapW);        //subtract or add map width or height depending on whether neighborhood wraps around torroidal map
        float locYMod = calcWrapMod(bmuY, mapNode_Y, mapH);        
        loc._sub(locXMod,locYMod, 0.0f);
        return loc;        
    }//findNodeLocWrap
    
    //determine map location based on neighborhood nodes, accounting for torroidal wrap
    private final void setExactMapLoc() {
        myPointf totalLoc = new myPointf(), locToAdd;
        float ttlInvDist = 0.0f,invDistP1;
        for (double _dist : mapNodeNghbrs.keySet()) {
            invDistP1 = (float) (1.0f/(1.0f+_dist));                    //handles 0 dist - max will be 0, min will be some fraction
            ArrayList<SOM_MapNode> tmpMap = mapNodeNghbrs.get(_dist);
            ttlInvDist += tmpMap.size() * invDistP1;
            //for (SOM_MapNode ex : tmpMap) {            ttlInvDist +=invDistP1;        }            
        }
        float mapW = mapMgr.getMapWidth(), mapH = mapMgr.getMapHeight();
        Integer bmuX = getBmu().mapNodeCoord.x,  bmuY = getBmu().mapNodeCoord.y;
        //scale by ttlInvDist so that all distance wts sum to 1
        for (double _dist : mapNodeNghbrs.keySet()) {
            invDistP1 = ((float) (1.0f/(1.0f+_dist))/ttlInvDist);                    //handles 0 dist - max will be 0, min will be some fraction
            ArrayList<SOM_MapNode> tmpMap = mapNodeNghbrs.get(_dist);
            for (SOM_MapNode mapNode : tmpMap) {        
                //if ex is more than 1 in x or y from bmu, then wrap arround, need to add (or subtract) x or y dim of map
                locToAdd = findNodeLocWrap(mapNode, bmuX, bmuY, mapW, mapH);
                totalLoc._add(myPointf._mult(locToAdd, invDistP1));                
            }            
        }
        totalLoc.x += mapW;totalLoc.x %= mapW;//filter 
        totalLoc.y += mapH;totalLoc.y %= mapH;
        
        this.mapLoc.set(totalLoc);
    }//setExactMapLoc
    
    //build feature vector - call externally after finalize
    public final void buildFeatureVector() {
        buildAllNonZeroFtrIDXs();
        buildFeaturesMap();
        setFlag(ftrsBuiltIDX,true);        
        buildNormFtrData();//once ftr map is built can normalize easily
        _buildFeatureVectorEnd_Priv();
    }//buildFeatureVector
    
    public abstract void postFtrVecBuild();
    
    //these are called before and after an individual example's features are built
    protected abstract void buildAllNonZeroFtrIDXs();
    protected abstract void _buildFeatureVectorEnd_Priv();
    
    public final void setIsTrainingDataIDX(boolean val, int idx) {
        isTrainingData=val; 
        testTrainDataIDX=idx;
        setIsTrainingDataIDX_Priv();
    }//setIsTrainingDataIDX
    
    //whether this record was used to train the current map or not
    public final boolean getIsTrainingData() {return isTrainingData;}
    public final int getTestTrainIDX() {return testTrainDataIDX;}
    
    protected abstract void setIsTrainingDataIDX_Priv();
    
    //build structures that require that the feature vector be built before hand
    public final void buildAfterAllFtrVecsBuiltStructs() {
        buildPerFtrNormMap();
        //default comparison vector setup - can be overridden
        buildCompFtrVector(0.0f);
    }//buildPostFeatureVectorStructs

    //build normalized vector of data - only after features have been set
    protected final void buildNormFtrData() {
        if(!getFlag(ftrsBuiltIDX)) {mapMgr.getMsgObj().dispMessage("SOMExample","buildNormFtrData","OID : " + OID + " : Features not built, cannot normalize feature data since marked as not built!", MsgCodes.warning2);return;}
        clearFtrMap(perExNormMapTypeKey);//ftrMaps[normFtrMapTypeKey].clear();
        if(this.ftrVecMag == 0) {return;}
        for (Integer IDX : ftrMaps[unNormFtrMapTypeKey].keySet()) {
            Float val  = ftrMaps[unNormFtrMapTypeKey].get(IDX)/this.ftrVecMag;
            ftrMaps[perExNormMapTypeKey].put(IDX,val);
            //setMapOfSrcWts(IDX, val, normFtrMapTypeKey);            
        }    
        setFlag(perExmplNormBuiltIDX,true);
    }//buildNormFtrData

    /**
     * scale each feature value to be between 0->1 based on min/max values seen for this 
     * feature.  all examples features will be scaled with respect to seen calc results 
     * do not use this for "exemplar" objects (i.e. objects whose features are set manually
     * to be some value explicitly intended to represent the feature itself)                   
     * 
     * MUST BE SET WITH APPROPRIATE MINS AND DIFFS                                                            
     * @param from_ftrs source of unmodified features
     * @param to_sclFtrs destination map holding scaled features
     * @param mins mins found per feature across all examples, used to scale
     * @param diffs diffs found per feature across all examples, used to scale
     */
//    protected final void calcPerFtrNormVector(TreeMap<Integer, Float> from_ftrs, TreeMap<Integer, Float> to_sclFtrs, Float[] mins, Float[] diffs) {
//        to_sclFtrs.clear();
//        for (Integer destIDX : from_ftrs.keySet()) {
//            Float lb = mins[destIDX],     diff = diffs[destIDX];
//            Float val = 0.0f;
//            if (diff==0) {//same min and max
//                if (lb > 0) { val = 1.0f;}//only a single value same min and max-> set feature value to 1.0
//                else {          val = 0.0f;}
//            } else {                val = (from_ftrs.get(destIDX)-lb)/diff;            }    
//            to_sclFtrs.put(destIDX,val);
//            
//        }//for each non-zero ftr
//    }//calcPerFtrNormVector        getSqDistFromFtrType
        
    protected final void calcPerFtrNormVector(TreeMap<Integer, Float> from_ftrs, TreeMap<Integer, Float> to_sclFtrs, Float[] mins, Float[] diffs) {
        to_sclFtrs.clear();
        float destDiff = mapMgr.getPerFtrNorm_destDiff(), destMin =  mapMgr.getPerFtrNorm_destMin();
        for (Integer destIDX : from_ftrs.keySet()) {
            Float lb = mins[destIDX],     diff = diffs[destIDX];
            Float val = 0.0f;
            if (diff==0) {//same min and max
                if (lb > 0) { val = destDiff + destMin;}//only a single value same min and max-> set feature value to 1.0
                else {          val = destMin;}
            } else {                val = destMin +  ((from_ftrs.get(destIDX)-lb)/diff)*destDiff;            }    
            to_sclFtrs.put(destIDX,val);
            
        }//for each non-zero ftr
    }//calcPerFtrNormVector        getSqDistFromFtrType
        
//    //called to report on this example's feature weight rankings 
//    public final void buildFtrRprtStructs() {features.buildFtrRprtStructs();}//buildFtrReports
//    
//    //return mapping of ftr IDXs to rank for this example
//    public final TreeMap<Integer,Integer> getMapOfFtrIDXBsWtRank(int mapToGet){return features.getMapOfFtrIDXBsWtRank(mapToGet);}//getMapOfFtrIDXBsWtRank    
//    public final TreeMap<Float, ArrayList<Integer>> getMapOfWtsToFtrIDXs(int mapToGet){return features.getMapOfWtsToFtrIDXs(mapToGet);}//getMapOfWtsToFtrIDXs        
//    //clears out structures used for reports, to minimize memory footprint
//    public final void clearFtrRprtStructs() {features.clearFtrRprtStructs();}//clearFtrReports
    
    
    ////////////////////////////////
    //distance measures between nodes
    /**
     * These functions will find the specified distance between the passed node and this node, using the specified feature type
     * chiSq is sqDist with each component divided by ftr variance
     * @param fromNode
     * @param _ftrType
     * @return
     */
    //return the chi-sq distance from this node to passed node
    //public final double getSqDistFromFtrType_ChiSq(SOMExample fromNode, int _ftrType){        
    public final double getSqDistFromFtrType_ChiSq(TreeMap<Integer, Float> fromftrMap, TreeMap<Integer, Float> thisFtrMap){
        double res = 0.0f;
        float[] mapFtrVar = mapMgr.getMap_ftrsVar();//divide by variance for chi-sq dist
//        Set<Integer> allIdxs = new HashSet<Integer>(fromftrMap.keySet());
//        allIdxs.addAll(thisFtrMap.keySet());
        Float frmVal,toVal, diff;
        for (Integer key : fromftrMap.keySet()) {//either map will have this key
            frmVal = fromftrMap.get(key);
            toVal = thisFtrMap.get(key);if(toVal == null) {toVal = 0.0f;}
            diff = toVal - frmVal;
            res += (diff * diff)/mapFtrVar[key];
        }
        for (Integer key : thisFtrMap.keySet()) {//either map will have this key
            frmVal = fromftrMap.get(key);if(frmVal == null) {frmVal = 0.0f;}
            toVal = thisFtrMap.get(key);
            diff = toVal - frmVal;
            res += (diff * diff)/mapFtrVar[key];
        }
        return res;
    }//getDistFromFtrType

    //return the chi-sq distance from this node to passed node, only measuring non-zero features in this node
    //public final double getSqDistFromFtrType_ChiSq_Exclude(SOMExample fromNode, int _ftrType){
    public final double getSqDistFromFtrType_ChiSq_Exclude(TreeMap<Integer, Float> fromftrMap, TreeMap<Integer, Float> thisFtrMap){
        double res = 0.0f;
        float[] mapFtrVar = mapMgr.getMap_ftrsVar();//divide by variance for chi-sq dist
        //Set<Integer> allIdxs = new HashSet<Integer>(thisFtrMap.keySet());
        Float frmVal,toVal, diff;
        for (Integer key : thisFtrMap.keySet()) {//either map will have this key
            frmVal = fromftrMap.get(key);    if(frmVal == null) {frmVal = 0.0f;}
            toVal = thisFtrMap.get(key);
            diff = toVal - frmVal;
            res += (diff * diff)/mapFtrVar[key];
        }
        return res;
    }//getDistFromFtrType
    
    //return the sq sdistance between this map's ftrs and the passed ftrMaps[ftrMapTypeKey]
    //public final double getSqDistFromFtrType(SOMExample fromNode, int _ftrType){
    public final double getSqDistFromFtrType(TreeMap<Integer, Float> fromftrMap, TreeMap<Integer, Float> thisFtrMap){
        double res = 0.0;
//        Set<Integer> allIdxs = new HashSet<Integer>(fromftrMap.keySet());
//        allIdxs.addAll(thisFtrMap.keySet());
        Float frmVal,toVal, diff;
        for (Integer key : fromftrMap.keySet()) {//either map will have this key
            frmVal = fromftrMap.get(key);
            toVal = thisFtrMap.get(key);if(toVal == null) {toVal = 0.0f;}
            diff = toVal - frmVal;
            res += (diff * diff);
        }
        for (Integer key : thisFtrMap.keySet()) {//either map will have this key
            frmVal = fromftrMap.get(key);if(frmVal == null) {frmVal = 0.0f;}
            toVal = thisFtrMap.get(key);
            diff = toVal - frmVal;
            res += (diff * diff);
        }
        return res;
    }//getDistFromFtrType    
    //return the distance between this map's ftrs and the passed ftrMaps[ftrMapTypeKey], only measuring -this- nodes non-zero features.
    //public double getSqDistFromFtrType_Exclude(SOMExample fromNode, int _ftrType){
    public final double getSqDistFromFtrType_Exclude(TreeMap<Integer, Float> fromftrMap, TreeMap<Integer, Float> thisFtrMap){
        double res = 0.0;
        //Set<Integer> allIdxs = new HashSet<Integer>(thisFtrMap.keySet());//this map's features - only ones that matter
        Float frmVal,toVal, diff;
        for (Integer key : thisFtrMap.keySet()) {//either map will have this key
            frmVal = fromftrMap.get(key);if(frmVal == null) {frmVal = 0.0f;}
            toVal = thisFtrMap.get(key);
            diff = toVal - frmVal;
            res += (diff * diff);
        }
        return res;
    }//getDistFromFtrType    

    ////////////////////////////////
    //end distance measures between nodes
    
    /////////////////////////////////
    // bmu handling/mapping
    
    //override for multi-example training data
    public void mapToBMU(int dataTypeVal) {
        if(null==bmu) {return;}
        bmu.addExToBMUs(this,dataTypeVal);    
        mapMgr.addExToNodesWithExs(bmu, exampleDataType);        
    }
    //override for multi-example training data
    public void mapTrainingToBMU(int trainingDataTypeVal) {
        if(null==bmu) {return;}
        bmu.addTrainingExToBMUs(this,trainingDataTypeVal);    
        mapMgr.addExToNodesWithExs(bmu, exampleDataType);        
    }
    
    //this will return the training label(s) of this example
    //the training label corresponds to a tag or a class referring to the data that can be assigned to a map node - a vote about the bmu from this example
    //if not training data then no label will exist;  might not exist if it is training data either, if fully unsupervised
    public abstract TreeMap<Integer,Integer> getTrainingLabels();    
    
    /**
     *  this will build the comparison feature vector array that is used as the comparison vector 
     *  in distance measurements - for most cases this will just be a copy of the ftr vector array
     *  but in some instances, there might be an alternate vector to be used to handle when, for 
     *  example, an example has ftrs that do not appear on the map
     * @param _ratio ratio of features to secondary features calculated to use to build comparison vector
     */
    public abstract void buildCompFtrVector(float _ratio);
    protected final void buildCompFtrVectorDefault() {        compFtrMaps = ftrMaps;    }

    
    //this value corresponds to training data type - we want to check training data type counts at each node
    private static final int _trainDataTypeIDX = SOM_ExDataType.Training.getVal();
    //given a sqdistance-keyed map of lists of mapnodes, this will find the best matching unit (min distance), with favor given to equi-distant units that have more examples
    private final void _setBMUFromMapNodeDistMap(TreeMap<Double, ArrayList<SOM_MapNode>> mapNodes) {
        ArrayList<Tuple<Integer,Integer>> bmuKeys = new ArrayList<Tuple<Integer,Integer>>();
        Entry<Double, ArrayList<SOM_MapNode>> topEntry = mapNodes.firstEntry();
        Double bmuDist = topEntry.getKey();
        ArrayList<SOM_MapNode>  bmuList = topEntry.getValue();
        int numBMUs = bmuList.size();
        for (int i=0;i<numBMUs;++i) {    bmuKeys.add(i, bmuList.get(i).mapNodeCoord);    }
        SOM_MapNode bestUnit = null;//keep null to break on errors - shouldn't happen // bmuList.get(0);//default to first entry
        if (numBMUs > 1) {//if more than 1 entry with same distance, find entry with most examples - if no entries have examples, then this will default to first entry
            int maxNumExamples = 0;
            for (int i=0;i<numBMUs;++i) {//# of map nodes sharing distance to this node
                SOM_MapNode node = bmuList.get(i);
                int numExamples = node.getNumExamples(_trainDataTypeIDX);//want # of training examples - use training examples as map node relevance weight
                if (numExamples >= maxNumExamples) {//need to manage if all map nodes that are "best" have no direct training examples (might happen on large maps), hence >= and not >
                    maxNumExamples = numExamples;
                    bestUnit = node;
                }        
            }
        } else {    bestUnit = bmuList.get(0);    }    
        _setBMUAddToNeighborhood(bestUnit, bmuDist);
    }//_setBMUFromMapNodeDistMap        
    
//    /**
//     * references current map of nodes, finds best matching unit and returns map of all map node tuple addresses and their ftr distances from this node     
//     * also build neighborhood nodes                                                                                                                        
//     * two methods to minimize if calls for chisq dist vs regular euclidean dist                                                                            
//     * Passing map of nodes keyed by non-zero ftr idx. 
//     * 
//     * @param _MapNodesByFtr : map of all mapnodes keyed by feature idx with non-zero features 
//     * @param _ftrtype : kind of features (un-normed, norm per feature, norm per example) to be used for comparison/distance calc
//     * @return
//     */
//    public final TreeMap<Double, ArrayList<SOMMapNode>> findBMUFromFtrNodes_ftrMaps(TreeMap<Integer, HashSet<SOMMapNode>> _MapNodesByFtr,  BiFunction<TreeMap<Integer, Float>, TreeMap<Integer, Float>, Double> _distFunc, int _ftrType) {
//        HashSet<SOMMapNode> _RelevantMapNodes = new HashSet<SOMMapNode>();
//        buildRelevantMapNodes(_RelevantMapNodes, ftrMaps[_ftrType].keySet(), _MapNodesByFtr);
//        TreeMap<Double, ArrayList<SOMMapNode>> mapNodesByDist = new TreeMap<Double, ArrayList<SOMMapNode>>();    
//        for (SOMMapNode mapNode : _RelevantMapNodes) {
//            double sqDistToNode = _distFunc.apply(mapNode.ftrMaps[_ftrType],  ftrMaps[_ftrType]);
//            ArrayList<SOMMapNode> tmpAra = mapNodesByDist.get(sqDistToNode);
//            if(tmpAra == null) {tmpAra = new ArrayList<SOMMapNode>();}
//            tmpAra.add(mapNode);
//            mapNodesByDist.put(sqDistToNode, tmpAra);        
//        }    
//        //handle if this node has no ftrs that map directly to map node ftrs - perhaps similarity groupings exist to build mappings from
//        //buildMapNodeDistsFromGroupings(mapNodesByDist, _MapNodesByFtr);
//        if(mapNodesByDist.size() == 0) {_setNullBMU(); return null;}
//        _setBMUFromMapNodeDistMap(mapNodesByDist);        
//        //find ftr distance to all 8 surrounding nodes and add them to mapNodeNeighbors
//        buildNghbrhdMapNodes( _ftrType, _distFunc);    
//        return mapNodesByDist;
//    }//findBMUFromNodes 
    
    /**
     * relevant map nodes are nodes with non-zero values in 1 or more of the ftrs this example also has non-zero values in
     * @param _RelevantMapNodes : pre-allocated empty set of map nodes that are relevant by direct feature membership
     * @param _RelevantMapNodesByFtrGroup : pre-allocated empty set of map nodes that are relevant by having weight in features that belong to some app-specific grouping of features that this node also has weight in
     * @param _MapNodesByFtr : map of all nodes keyed by feature index containing non-zero index
     * @return set of nodes that have non-zero values in features this node also has non-zero values in
     */
    private final void buildRelevantMapNodes(HashSet<SOM_MapNode> _RelevantMapNodes, Set<Integer> nonZeroFtrIDXs, TreeMap<Integer, HashSet<SOM_MapNode>> _MapNodesByFtr){
        //query _MapNodesByFtr by all non-zero features in this map
        HashSet<SOM_MapNode> nodesWithNonZeroFtrs;
        for(Integer idx : nonZeroFtrIDXs) {
            nodesWithNonZeroFtrs = _MapNodesByFtr.get(idx);
            if(null==nodesWithNonZeroFtrs) {continue;}
            for(SOM_MapNode node : nodesWithNonZeroFtrs) {        _RelevantMapNodes.add(node);}
        }        
    }//buildRelevantMapNodes
    
    /**
     * returns a map keyed by distance with values being a list of nodes at key distance
     * @param _MapNodesByFtr : map of all mapnodes keyed by feature idx with non-zero features
     * @param _distFunc : function to use to calculate distance
     * @param _ftrType : kind of features (unnormalized, normed per feature across all data, normed per example) used to train map
     * @return
     */    
    public final TreeMap<Double, ArrayList<SOM_MapNode>> findMapNodesByDist(TreeMap<Integer, HashSet<SOM_MapNode>> _MapNodesByFtr,  BiFunction<TreeMap<Integer, Float>, TreeMap<Integer, Float>, Double> _distFunc, SOM_FtrDataType _ftrType){
        HashSet<SOM_MapNode> _RelevantMapNodes = new HashSet<SOM_MapNode>();
        buildRelevantMapNodes(_RelevantMapNodes, compFtrMaps[_ftrType.getVal()].keySet(), _MapNodesByFtr);
        TreeMap<Double, ArrayList<SOM_MapNode>> mapNodesByDist = new TreeMap<Double, ArrayList<SOM_MapNode>>();    
        for (SOM_MapNode mapNode : _RelevantMapNodes) {
            double sqDistToNode = _distFunc.apply(mapNode.compFtrMaps[_ftrType.getVal()], compFtrMaps[_ftrType.getVal()]);
            ArrayList<SOM_MapNode> tmpAra = mapNodesByDist.get(sqDistToNode);
            if(tmpAra == null) {tmpAra = new ArrayList<SOM_MapNode>();}
            tmpAra.add(mapNode);
            mapNodesByDist.put(sqDistToNode, tmpAra);        
        }    
        return mapNodesByDist;
    }//findMapNodesByDist
    
    /**
     * returns the entry in a distance-keyed map of lists of nodes for the list of 1 or more nodes that have the least distance
     * Should not be called in a multi-threaded context
     * @param _MapNodesByFtr : map of all mapnodes keyed by feature idx with non-zero features
     * @param _distFunc : function to use to calculate distance
     * @param _ftrType : kind of features (unnormalized, normed per feature across all data, normed per example) to be used for comparison/distance calc
     * @return
     */
    public final Entry<Double, ArrayList<SOM_MapNode>> findClosestMapNodes(TreeMap<Integer, HashSet<SOM_MapNode>> _MapNodesByFtr,  BiFunction<TreeMap<Integer, Float>, TreeMap<Integer, Float>, Double> _distFunc, SOM_FtrDataType _ftrType){
        TreeMap<Double, ArrayList<SOM_MapNode>> mapNodesByDist = findMapNodesByDist(_MapNodesByFtr,_distFunc, _ftrType );
        if (mapNodesByDist.size() == 0) {
            mapMgr.getMsgObj().dispMessage("SOMExample", "findClosestMapNodes","ERROR!! Empty map of closest map nodes by dist.", MsgCodes.error1);    
        }
        return mapNodesByDist.firstEntry();    
    }
    
    /**
     * references current map of nodes, finds best matching unit and returns map of all map node tuple addresses and their ftr distances from this node     
     * also build neighborhood nodes                                                                                                                        
     * two methods to minimize if calls for chisq dist vs regular euclidean dist                                                                            
     * Passing map of nodes keyed by non-zero ftr idx. 
     * 
     * @param _MapNodesByFtr : map of all mapnodes keyed by feature idx with non-zero features
     * @param _distFunc : function to use to calculate distance
     * @param _ftrType : kind of features (unnormalized, normed per feature across all data, normed per example) to be used for comparison/distance calc
     * @return
     */
    public synchronized final TreeMap<Double, ArrayList<SOM_MapNode>> findBMUFromFtrNodes(TreeMap<Integer, HashSet<SOM_MapNode>> _MapNodesByFtr,  BiFunction<TreeMap<Integer, Float>, TreeMap<Integer, Float>, Double> _distFunc, SOM_FtrDataType _ftrType) {
        TreeMap<Double, ArrayList<SOM_MapNode>> mapNodesByDist = findMapNodesByDist(_MapNodesByFtr,_distFunc, _ftrType );
        //handle if this node has no ftrs that map directly to map node ftrs - perhaps similarity groupings exist to build mappings from
        //buildMapNodeDistsFromGroupings(mapNodesByDist, _MapNodesByFtr);
        if(mapNodesByDist.size() == 0) {_setNullBMU(); return null;}
        _setBMUFromMapNodeDistMap(mapNodesByDist);        
        //find ftr distance to all 8 surrounding nodes and add them to mapNodeNeighbors
        buildNghbrhdMapNodes( _ftrType.getVal(), _distFunc);    
        return mapNodesByDist;
    }//findBMUFromNodes 

    private final String _toCSVString(TreeMap<Integer, Float> ftrs) {
        String res = ""+getDenseTrainDataKey()+",";
        int numTrnFtrs = mapMgr.getNumTrainFtrs();
        for(int i=0;i<numTrnFtrs;++i){
            Float ftr = ftrs.get(i);            
            res += String.format("%1.7g", (ftr==null ? 0 : ftr)) + ",";
        }
        return res;}
    //return csv string of this object's features, depending on which type is selected - check to make sure 2ndary features exist before attempting to build data strings
    ////useUnmoddedDat = 0, useScaledDat = 1, useNormedDat
    public final String toCSVString(SOM_FtrDataType _type) {
        switch(_type){
            case UNNORMALIZED     : {return _toCSVString(ftrMaps[unNormFtrMapTypeKey]); }
            case EXMPL_NORM     : {return _toCSVString(getFlag(perExmplNormBuiltIDX) ? ftrMaps[perExNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey]);}
            case FTR_NORM      : {return _toCSVString(getFlag(perFtrNormBuiltIDX) ? ftrMaps[perFtrNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey]); }
            default : {return _toCSVString(ftrMaps[unNormFtrMapTypeKey]); }
        }
    }//toCSVString

    private final String _toLRNString(TreeMap<Integer, Float> ftrs, String sep) {
        String res = ""+getDenseTrainDataKey()+sep;
        int numTrnFtrs = mapMgr.getNumTrainFtrs();
        for(int i=0;i<numTrnFtrs;++i){
            Float ftr = ftrs.get(i);            
            res += String.format("%1.7g", (ftr==null ? 0 : ftr)) + sep;
        }
        return res;}    
    
    /**
     * return the appropriate string value for the dense training data - should be numeric key value to save in lrn or csv dense file
     * @return
     */
    protected abstract String getDenseTrainDataKey();
    //return LRN-format (dense) string of this object's features, depending on which type is selected - check to make sure 2ndary features exist before attempting to build data strings
    public final String toLRNString(SOM_FtrDataType _type, String sep) {
        switch(_type){
            case UNNORMALIZED     : {return _toLRNString(ftrMaps[unNormFtrMapTypeKey], sep); }
            case EXMPL_NORM      : {return _toLRNString(getFlag(perExmplNormBuiltIDX) ? ftrMaps[perExNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey], sep);}
            case FTR_NORM   : {return _toLRNString(getFlag(perFtrNormBuiltIDX) ? ftrMaps[perFtrNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey], sep); }
            default : {return _toLRNString(ftrMaps[unNormFtrMapTypeKey], sep); }
        }        
    }//toLRNString
    
    private final String _toSVMString(TreeMap<Integer, Float> ftrs) {
        String res = "";
        for (Integer ftrIdx : allNonZeroFtrIDXs) {res += "" + ftrIdx + ":" + String.format("%1.7g", ftrs.get(ftrIdx)) + " ";}
        return res;}//_toSVMString
    
    //return SVM-format (sparse) string of this object's features, depending on which type is selected - check to make sure 2ndary features exist before attempting to build data strings
    public final String toSVMString(SOM_FtrDataType _type) {
        switch(_type){
            case UNNORMALIZED     : {return _toSVMString(ftrMaps[unNormFtrMapTypeKey]); }
            case EXMPL_NORM      : {return _toSVMString(getFlag(perExmplNormBuiltIDX) ? ftrMaps[perExNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey]);}
            case FTR_NORM   : {return _toSVMString(getFlag(perFtrNormBuiltIDX) ? ftrMaps[perFtrNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey]); }
            default : {return _toSVMString(ftrMaps[unNormFtrMapTypeKey]); }
        }        
    }//toLRNString
    
    
    //build a feature vector from the map of ftr values
    private final float[] _getFtrsFromMap(TreeMap<Integer, Float> ftrMap) {
        float[] ftrs = new float[mapMgr.getNumTrainFtrs()];
        for (Integer ftrIdx : ftrMap.keySet()) {ftrs[ftrIdx]=ftrMap.get(ftrIdx);        }
        return ftrs;
    }
    //build feature vector on demand
    public final float[] getFtrs(SOM_FtrDataType _type) {
        switch(_type){
            case UNNORMALIZED     : {return getRawFtrs(); }
            case EXMPL_NORM      : {return (getFlag(perExmplNormBuiltIDX) ? getPerExNormFtrs() : getRawFtrs());}
            case FTR_NORM   : {return (getFlag(perFtrNormBuiltIDX) ? getPerFtrNormFtrs() : getRawFtrs()); }
            default : {return getRawFtrs(); }
        }        
    }

    //build feature vector on demand
    public final float[] getRawFtrs() {return _getFtrsFromMap(ftrMaps[unNormFtrMapTypeKey]);}
    //build per feature normalized vector on demand
    public final float[] getPerFtrNormFtrs() {return _getFtrsFromMap(ftrMaps[perFtrNormMapTypeKey]);}
    //build per example normalized vector on demand
    public final float[] getPerExNormFtrs() {return _getFtrsFromMap(ftrMaps[perExNormMapTypeKey]);}    
    
    public final TreeMap<Integer, Float> getCurrentFtrMap(SOM_FtrDataType _type){
        switch(_type){
            case UNNORMALIZED : {return ftrMaps[unNormFtrMapTypeKey]; }
            case EXMPL_NORM  : {return (getFlag(perExmplNormBuiltIDX) ? ftrMaps[perExNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey]);}
            case FTR_NORM   : {return (getFlag(perFtrNormBuiltIDX) ? ftrMaps[perFtrNormMapTypeKey] : ftrMaps[unNormFtrMapTypeKey]); }
            default : {return ftrMaps[unNormFtrMapTypeKey]; }
        }        
    }

    protected final TreeMap<Integer, Float> buildMapFromAra(float[] ara, float thresh) {
        TreeMap<Integer, Float> ftrs = new TreeMap<Integer, Float>();
        for (int i=0;i<ara.length;++i) {if(ara[i]> thresh) {ftrs.put(i, ara[i]);}}    
        return ftrs;
    }
    
    private final String dispFtrs(int _type) {
        TreeMap<Integer, Float> ftrs = ftrMaps[_type];
        String res = "";
        if((ftrs==null) || (ftrs.size() == 0)){res+=" None\n";} 
        else {
            int numFtrs = mapMgr.getNumTrainFtrs();
            res +="\n\t";for(int i=0;i<numFtrs;++i){
            Float ftr = ftrs.get(i);
            res += String.format("%1.4g",  (ftr==null ? 0 : ftr)) + " | "; if((numFtrs > 40) && ((i+1)%30 == 0)){res +="\n\t";}}}
        return res;
    }
    
    protected final String dispFtrMapVals(int _type) {
        TreeMap<Integer, Float> ftrs = ftrMaps[_type];
        String res = "";
        if((ftrs==null) || (ftrs.size() == 0)){res+=" None\n";} 
        else {            for(Integer i : ftrs.keySet()){                res += dispFtrVal(ftrs, i);}}        
        return res;
    }
    
    //return a string value corresponding to a specific feature index in the sparse ftr array
    protected abstract String dispFtrVal(TreeMap<Integer, Float> ftrs, Integer idx);
    @Override
    public final int hashCode() {        return _hashCode;    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)            return true;
        if (obj == null)            return false;
        if (getClass() != obj.getClass())            return false;
        SOM_Example other = (SOM_Example) obj;
        if (_hashCode != other._hashCode)            return false;
        return true;
    }
    
    private final void initFlags(){stFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlag(i,false);}}
    public final void setAllFlags(int[] idxs, boolean val) {for (int idx : idxs) {setFlag(idx, val);}}
    public final void setFlag(int idx, boolean val){
        int flIDX = idx/32, mask = 1<<(idx%32);
        stFlags[flIDX] = (val ?  stFlags[flIDX] | mask : stFlags[flIDX] & ~mask);
        switch (idx) {//special actions for each flag
            case debugIDX            : {break;}    
            case ftrsBuiltIDX        : {break;}
            case perFtrNormBuiltIDX    : {break;}
            case perExmplNormBuiltIDX    : {break;}
            case isBadTrainExIDX    : {break;}
        }
    }//setFlag        
    public final boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (stFlags[idx/32] & bitLoc) == bitLoc;}        
    public final double get_sqDistToBMU() {    return _sqDistToBMU;}
    public final void set_sqDistToBMU(double _sqDistToBMU) {this._sqDistToBMU = _sqDistToBMU;}
    public final SOM_MapNode getBmu() {    return bmu;    }
    public final void setBmu(SOM_MapNode _bmu) {    bmu = _bmu;}
    
    /**
     * this will return the bmu membership and neighborhood membership of this node
     * @return
     */
    public String getBMU_NHoodMbrship_CSV() {
        String res = ""+OID+",";
        if(null==bmu) {return res + "No Mapped BMU For this example";}
        //res += ""+String.format("%.8f", _sqDistToBMU)+","+bmu.mapNodeCoord.toCSVString()+",";  //1st entry is bmu
        if(mapNodeNghbrs.size() == 0){
            mapMgr.getMsgObj().dispInfoMessage("SOMExample", "getBMU_NHoodMbrship_CSV", "Error!!!! No mapNodeNghbrs exists or is size 0 for example ID : " + OID);
            return res;
        }
        for(Double _dist : mapNodeNghbrs.keySet()) {
            String distRes = ""+String.format("%.8f", _dist)+",";
            ArrayList<SOM_MapNode> _list = mapNodeNghbrs.get(_dist);
            for(SOM_MapNode _mapNode : _list) {        res += distRes+_mapNode.mapNodeCoord.toCSVString()+",";    }
        }
        return res;
    }//getBMU_NHoodMbrshipString
    
    /**
     * get descriptive header string for csv output of nodes and their bmus
     * @return descriptive header string describing format of bmu neighborhood output
     */
    public String getBMU_NHoodHdr_CSV() {
        String res = "OID,Sq Dist to BMU, BMU Map Loc,";
        //add heading for each neighbor
        for(int i =0;i<8;++i) {    res +="Sq Dist to Nbr Map Node, Neighbor Node Map Loc,";        }
        return res;
    }//getBMU_NHoodHdr_CSV()

    @Override
    public String toString(){
        String res = "Example OID# : "+OID ;
        if(null!=mapLoc){res+="Location on SOM map : " + mapLoc.toStrBrf();}
        int numTrnFtrs = mapMgr.getNumTrainFtrs();
        if (numTrnFtrs > 0) {
            res += "\nUnscaled Features (" +numTrnFtrs+ " ) :";
            res += dispFtrs(unNormFtrMapTypeKey);
            res +="\nScaled Features : ";
            res += dispFtrs(perFtrNormMapTypeKey);
            res +="\nNormed Features : ";
            res += dispFtrs(perExNormMapTypeKey);
        }
        return res;
    }

}//SOMExample 




////class description for a data point -
////TODO change this to some other structure, or other comparison mechanism?  allow for subset membership check?
//class dataClass implements Comparable<dataClass> {
//    
//    public String label;
//    public String lrnKey;
//    private String cls;
//    //color of this class, for vis rep
//    public int[] clrVal;
//    
//    public dataClass(String _lrnKey, String _lbl, String _cls, int[] _clrVal){
//        lrnKey=_lrnKey;
//        label = _lbl;    
//        cls = _cls;
//        clrVal = _clrVal;
//    }    
//    public dataClass(dataClass _o){this(_o.lrnKey, _o.label,_o.cls, _o.clrVal);}//copy ctor
//        
//    //this will guarantee that, so long as a string has only one period, the value returned will be in the appropriate format for this mocapClass to match it
//    //reparses and recalcs subject and clip from passed val
//    public static String getPrfxFromData(String val){
//        String[] valTkns = val.trim().split("\\.");
//        return String.format("%03d",(Integer.parseInt(valTkns[0]))) + "."+ String.format("%03d",(Integer.parseInt(valTkns[1])));        
//    }    
//    @Override
//    public int compareTo(dataClass o) {    return label.compareTo(o.label);}
//    public String toCSVString(){String res = "" + lrnKey +","+label+","+cls;    return res;}
//    public String getFullLabel(){return label +"|"+cls;}
//    //public static String buildPrfx(int val){return (val < 100 ? (val < 10 ? "00" : "0") : "") + val;}//handles up to 999 val to be prefixed with 0's    
//    public String toString(){
//        String res = "Label :  " +label + "\tLrnKey : " + lrnKey  +"\tDesc : "+cls;
//        return res;        
//    }    
//}//dataClass
