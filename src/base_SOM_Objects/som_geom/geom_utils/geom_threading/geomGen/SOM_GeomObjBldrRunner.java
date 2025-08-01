package base_SOM_Objects.som_geom.geom_utils.geom_threading.geomGen;


import java.util.concurrent.ExecutorService;

import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;
import base_Utils_Objects.threading.runners.myThreadRunner;

/**
 * This class will build graphical objects
 * @author john
 *
 */
public abstract class SOM_GeomObjBldrRunner extends myThreadRunner {
    /**
     * owning map manager
     */
    protected final SOM_GeomMapManager mapMgr;
    /**
     * number of samples per object to build
     */
    protected int numSmplsPerObj;
    /**
     * what kind of task this runner will manage
     */
    protected SOM_GeomObjBldrTasks taskType;
    
    /**
     * # of examples to manage per threading partition, approximately
     */
    protected static final int numPerPartiton = 50000;
    
    /**
     * data name being processed
     */
    protected final String dataTypeName;
    
    /**
     * ref to array of data to be processed - should be predefined/prebuilt and just populated in runner
     */
    protected SOM_GeomObj[] objArray;

    /**
     * 
     * @param _mapMgr map manager
     * @param _th_exec thread executor
     * @param _objArray prebuilt object array to be populated/modified by runner
     * @param _dataTypName type of data being worked with
     * @param _forceST force process to be single threaded
     * @param _numVals array of numbers ( idx 0 : # of objects to work with, idx 1 : # of samples per object)
     * @param _taskType type of task to execute
     */
    public SOM_GeomObjBldrRunner(SOM_GeomMapManager _mapMgr, ExecutorService _th_exec, SOM_GeomObj[] _objArray, boolean _forceST, int[] _numVals, SOM_GeomObjBldrTasks _taskType) {
        super(_th_exec, _mapMgr.isMTCapable() && !_forceST, _mapMgr.getNumUsableThreads()-1, _numVals[0]);        
        mapMgr = _mapMgr;
        numSmplsPerObj = _numVals[1];
        taskType = _taskType;
        dataTypeName = mapMgr.getGeomObjTypeName();
        objArray = _objArray;
    }
    
    @Override
    protected int getNumPerPartition() {        return numPerPartiton;}

    public void setNumObjs(int _num) {    this.setNumWorkUnits(_num); }
    public void setNumSamplesPerObj(int _num) {numSmplsPerObj = _num;}
    public void setTaskType(SOM_GeomObjBldrTasks _task) {taskType = _task;}
    public void setObjArray(SOM_GeomObj[] _objArray) {objArray = _objArray; this.setNumWorkUnits(objArray.length);}
    /**
     * return results of calculations
     * @return
     */
    public SOM_GeomObj[] getObjArray() {return objArray;}

}//class SOM_GeomObjBuilder
