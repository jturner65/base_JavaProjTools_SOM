package base_SOM_Objects.som_geom.geom_utils.geom_threading.trainDataGen;

import java.util.HashSet;

import base_Math_Objects.MyMathUtils;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomExampleManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomTrainingExUniqueID;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomSamplePointf;
import base_SOM_Objects.som_geom.geom_utils.geom_threading.base.SOM_GeomCallable;
/**
 * build training data
 * @author john
 *
 */
public abstract class SOM_GeomTrainExBuilder extends SOM_GeomCallable {
    /**
     * # of examples for each callable of this type to build 
     */
    protected final int numExToBuildThisThread;
    /**
     * this is the example manager that will receive the examples synthesized from this callable
     */
    protected final SOM_GeomExampleManager exMgr;
    
    /**
     * ref to array of all example samples from which to build geometric objects
     */
    protected final SOM_GeomSamplePointf[] allExamples;
    
    /**
     * the number of samples required to build an object
     */
    protected final int numExPerObj;    
    /**
     * idxs to use to build the objects - these are known to be unique and to satisfy the requirements of the object.
     * This array is the same for all builders
     */
    protected final SOM_GeomTrainingExUniqueID[] idxsToUse;
    
    public SOM_GeomTrainExBuilder(SOM_GeomMapManager _mapMgr, SOM_GeomExampleManager _exMgr, SOM_GeomSamplePointf[] _allExs, int[] _intVals, SOM_GeomTrainingExUniqueID[] _idxsToUse) {
        super(_mapMgr, _intVals[0],_intVals[1],_intVals[2]);
        //idxs 0,1,2 are st,end,thrdIDX
        numExToBuildThisThread = endIdx -stIdx;//calcNumPerThd(_intVals[3], _intVals[4]);
        this.msgObj.dispInfoMessage("SOM_GeomTrainExBuilder::"+ dataType,"ctor","Thd IDX : " + thdIDX + " Building total : " + _intVals[3] + " over "+ _intVals[4] + " threads : " + numExToBuildThisThread + " this thread");
        exMgr = _exMgr;
        idxsToUse = _idxsToUse;
        allExamples = _allExs;
        numExPerObj = mapMgr.getNumSamplesToBuildObject();
    }

    private void buildTrainExData() {
        SOM_GeomSamplePointf[] exAra = new SOM_GeomSamplePointf[numExPerObj];
        for(int i=0;i<numExToBuildThisThread;++i) {
            exAra = genPtsForObj();
            SOM_GeomObj obj = _buildSingleObjectFromSamples(SOM_ExDataType.Training,exAra, i); incrProgress(i,"Building Training Data");
            exMgr.addExampleToMap(obj);        
        }    
    }//buildTrainExData
    
    private void buildTrainExData_UniqueIDXs() {
        //ThreadLocalRandom rnd = ThreadLocalRandom.current();
        SOM_GeomSamplePointf[] exAra = new SOM_GeomSamplePointf[numExPerObj];
        for(int i=this.stIdx; i<this.endIdx;++i) {
            Integer[] objIDXs = idxsToUse[i].idxs;
            exAra = new SOM_GeomSamplePointf[objIDXs.length];
            for(int j=0;j<exAra.length;++j) {        exAra[j] = allExamples[objIDXs[j]];        }
            SOM_GeomObj obj = _buildSingleObjectFromSamples(SOM_ExDataType.Training,exAra, i); incrProgress(i,"Building Training Data");
            exMgr.addExampleToMap(obj);        
        }            
        
    }//buildTrainExData
    
    protected String getObjID(int idx) {return String.format("%05d", stIdx + idx);}
    
    /**
     * for lines just need 2 points; planes need 3 non-colinear points; spheres need 4 non-coplanar points, no 3 of which are colinear
     * @return
     */
    protected abstract SOM_GeomSamplePointf[] genPtsForObj();
    
    protected Integer[] genUniqueIDXs(int numToGen){
        HashSet<Integer> idxs = new HashSet<Integer>();
        while(idxs.size() < numToGen) {    idxs.add(MyMathUtils.randomInt(0,allExamples.length));}        
        return idxs.toArray(new Integer[0]);
    }
    //for(int i=0;i<res.length;++i) {    res[i]=allExamples[rnd.nextInt(0,allExamples.length)];}
    //return res;
    
    /**
     * build a single object to be stored at idx
     * @param idx idx of object in resultant array
     * @return build object
     */
    protected abstract SOM_GeomObj _buildSingleObjectFromSamples(SOM_ExDataType _exDataType, SOM_GeomSamplePointf[] exAra, int idx);

    
    @Override
    public Boolean call() throws Exception {
        msgObj.dispInfoMessage("SOM_GeomTrainExBuilder::"+this.dataType, "call::thdIDX="+this.thdIDX, "Start building " + numExToBuildThisThread + " " +dataType +" training example objects from geom obj samples.");
        if((idxsToUse == null) || (idxsToUse.length == 0)){    //if no idxs specified then allowing for duplicate training data examples
            buildTrainExData();
        } else {
            buildTrainExData_UniqueIDXs();
        }
        msgObj.dispInfoMessage("SOM_GeomTrainExBuilder::"+this.dataType, "call::thdIDX="+this.thdIDX, "Finished building " + numExToBuildThisThread + " " +dataType +" training example objects from geom obj samples.");
        
        return true;
    }

}//SOM_GeomTrainExBuilder
