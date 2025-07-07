package base_SOM_Objects.som_managers.runners.callables;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_Utils_Objects.io.file.FileIOManager;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;

public class SOM_SaveExToBMUs_CSV implements Callable<Boolean>{
    protected final MessageObject msgObj;
    protected final int stIdx, endIdx, thdIDX, progressBnd;
    protected final FileIOManager fileIO;

    protected final String dataType;
    protected static final float progAmt = .2f;
    protected double progress = -progAmt;
    protected final SOM_Example[] exs;    
    protected final String fileName;
    //list of files to write
    protected ArrayList<String> outStrs;
    
    public SOM_SaveExToBMUs_CSV(int _stExIDX, int _endExIDX, SOM_Example[] _exs, int _thdIDX, String _datatype, String _fileNamePrfx) {
        msgObj = MessageObject.getInstance();
        exs=_exs;
        stIdx = _stExIDX;
        endIdx = _endExIDX;
        progressBnd = (int) ((endIdx-stIdx) * progAmt);
        thdIDX= _thdIDX;
        dataType = _datatype;
        fileIO = new FileIOManager(msgObj,"SOM_ExToBMUCSVWriter TH_IDX_"+String.format("%02d", thdIDX));
        fileName = _fileNamePrfx + "_Ex_"+String.format("%07d", stIdx)+"_"+String.format("%07d", endIdx) +".csv";
        outStrs = new ArrayList<String>();
        outStrs.add(exs[0].getBMU_NHoodHdr_CSV());
    } 
    
    protected void incrProgress(int idx) {
        if(((idx-stIdx) % progressBnd) == 0) {        
            progress += progAmt;    
            msgObj.dispInfoMessage("MapFtrCalc","incrProgress::thdIDX=" + String.format("%02d", thdIDX)+" ", "Progress for saving BMU mappings for examples of type : " +dataType +" at : " + String.format("%.2f",progress));
        }
        if(progress > 1.0) {progress = 1.0;}
    }

    public double getProgress() {    return progress;}
    
    @Override
    public Boolean call() {
        if(exs.length == 0) {
            msgObj.dispMessage("SOM_ExToBMUCSVWriter", "Run Thread : " +thdIDX, ""+dataType+" Data["+stIdx+":"+endIdx+"] is length 0 so nothing to do. Aborting thread.", MsgCodes.info5);
            return true;}
        msgObj.dispMessage("SOM_ExToBMUCSVWriter", "Run Thread : " +thdIDX, "Starting saving BMU mappings for "+dataType+" example Data["+stIdx+":"+endIdx+"] | # saved records : " + (endIdx-stIdx), MsgCodes.info5);        
        //typeOfCalc==0 means build features
        for(int i=stIdx; i<endIdx;++i) {
            SOM_Example ex = exs[i];
            String resStr= ex.getBMU_NHoodMbrship_CSV();
            outStrs.add(resStr);
        }    
        fileIO.saveStrings(fileName, outStrs);    
        msgObj.dispMessage("SOM_ExToBMUCSVWriter", "Run Thread : " +thdIDX, "Finished saving BMU mappings for "+dataType+" example Data["+stIdx+":"+endIdx+"] | # saved records : " + (endIdx-stIdx), MsgCodes.info5);        
        return true;
    }
    
}//class MapFtrCalc