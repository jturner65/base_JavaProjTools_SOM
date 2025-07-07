package base_SOM_Objects.som_managers.runners.callables.base;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import base_Math_Objects.vectorObjs.tuples.Tuple;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_SOM_Objects.som_segments.segments.SOM_MappedSegment;
import base_Utils_Objects.io.messaging.MessageObject;

//class to manage mapping of examples to bmus
public abstract class SOM_MapDataToBMUs implements Callable<Boolean>{
    protected MessageObject msgObj;
    protected final SOM_FtrDataType curMapFtrType;
    protected final int stIdx, endIdx, thdIDX, progressBnd;
    //calculate the exclusionary feature distance(only measure distance from map via features that the node has non-zero values in)
    protected final boolean useChiSqDist;
    protected final TreeMap<Tuple<Integer,Integer>, SOM_MapNode> MapNodes;
    //map of ftr idx and all map nodes that have non-zero presence in that ftr
    protected TreeMap<Integer, HashSet<SOM_MapNode>> MapNodesByFtr;
    //Map of classes to segment - segment contains the map nodes that participate in that segment
    protected TreeMap<Integer, SOM_MappedSegment> Class_Segments;
    //map of categories to segment
    protected TreeMap<Integer, SOM_MappedSegment> Category_Segments;
    
    protected String ftrTypeDesc, dataType;
    protected static final float progAmt = .2f;
    protected double progress = -progAmt;
    
    public SOM_MapDataToBMUs(SOM_MapManager _mapMgr, int _stProdIDX, int _endProdIDX, int _thdIDX, String _type, boolean _useChiSqDist){
        MapNodes = _mapMgr.getMapNodes();
        MapNodesByFtr = _mapMgr.getMapNodesByFtr();
        //segments own the map nodes that map to them
        Class_Segments = _mapMgr.getClass_Segments();
        Category_Segments = _mapMgr.getCategory_Segments();
        msgObj = MessageObject.getInstance();//make a new one for every thread
        stIdx = _stProdIDX;
        endIdx = _endProdIDX;
        progressBnd = (int) ((endIdx-stIdx) * progAmt);
        thdIDX= _thdIDX;
        dataType = _type;
        curMapFtrType = _mapMgr.getCurrentTrainDataFormat();
        ftrTypeDesc = _mapMgr.getDataDescFromCurFtrTrainType();
        useChiSqDist = _useChiSqDist;        
    }//ctor
    
    protected void incrProgress(int idx) {
        if(((idx-stIdx) % progressBnd) == 0) {        
            progress += progAmt;    
            msgObj.dispInfoMessage("MapDataToBMUs","incrProgress::thdIDX=" + String.format("%02d", thdIDX)+" ", "Progress for dataType : " +dataType +" at : " + String.format("%.2f",progress));
        }
        if(progress > 1.0) {progress = 1.0;}
    }
    
    public double getProgress() {    return progress;}
    
    protected abstract boolean mapAllDataToBMUs();
    @Override
    public final Boolean call() {    
        progress = -progAmt;
        boolean retCode = mapAllDataToBMUs();        
        return retCode;
    }
    
}//mapDataToBMUs

