package base_SOM_Objects.som_managers.runners.callables;

import java.util.concurrent.Callable;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * Perform feature calculations. Consumed by SOM_CalcExFtrs_Runner 
 * @author John Turner
 *
 */
public class SOM_CalcExFtrs implements Callable<Boolean>{
    protected MessageObject msgObj;
    protected final int stIdx, endIdx, thdIDX, progressBnd, typeOfCalc;

    protected String dataType, calcType;
    protected static final float progAmt = .2f;
    protected double progress = -progAmt;
    protected SOM_Example[] exs;
    
    
    public SOM_CalcExFtrs(int _stExIDX, int _endExIDX, SOM_Example[] _exs, int _thdIDX, String _datatype, String _calcType,int _typeOfCalc) {
        msgObj = MessageObject.getInstance();
        exs=_exs;
        stIdx = _stExIDX;
        endIdx = _endExIDX;
        progressBnd = (int) ((endIdx-stIdx) * progAmt);
        thdIDX= _thdIDX;
        dataType = _datatype;
        typeOfCalc = _typeOfCalc;    
        calcType = _calcType;
    } 
    
    protected void incrProgress(int idx) {
        if(((idx-stIdx) % progressBnd) == 0) {        
            progress += progAmt;    
            msgObj.dispInfoMessage("MapFtrCalc","incrProgress::thdIDX=" + String.format("%02d", thdIDX)+" ", "Progress for "+calcType+ " with dataType : " +dataType +" at : " + String.format("%.2f",progress));
        }
        if(progress > 1.0) {progress = 1.0;}
    }

    public double getProgress() {    return progress;}
    
    @Override
    public Boolean call() {
        if(exs.length == 0) {
            msgObj.dispMessage("MapFtrCalc", "Run Thread : " +thdIDX, ""+dataType+" Data["+stIdx+":"+endIdx+"] is length 0 so nothing to do. Aborting thread.", MsgCodes.info5);
            return true;}
        msgObj.dispMessage("MapFtrCalc", "Run Thread : " +thdIDX, "Starting "+calcType+" on "+dataType+" Data["+stIdx+":"+endIdx+"]  (" + (endIdx-stIdx) + " exs), ", MsgCodes.info5);
        //typeOfCalc==0 means build features
        if(typeOfCalc==0) {            for (int i=stIdx;i<endIdx;++i) {exs[i].buildFeatureVector();incrProgress(i);}}                             //build ftrs
        else if(typeOfCalc==1) {    for (int i=stIdx;i<endIdx;++i) {exs[i].postFtrVecBuild();incrProgress(i);}    }                            //typeOfCalc==1 means post ftr build calc - (Per example finalizing)
        else if(typeOfCalc==2) {    for (int i=stIdx;i<endIdx;++i) {exs[i].buildAfterAllFtrVecsBuiltStructs();incrProgress(i);}    }            //typeOfCalc==2 means after all ftr vecs have been build 
        msgObj.dispMessage("MapFtrCalc", "Run Thread : " +thdIDX, "Finished "+calcType+" on "+dataType+" Data["+stIdx+":"+endIdx+"] # calcs : " + (endIdx-stIdx), MsgCodes.info5);        
        return true;
    }
    
}//class MapFtrCalc