package base_SOM_Objects.som_fileIO;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;

import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_Utils_Objects.io.file.FileIOManager;
import base_Utils_Objects.io.messaging.MessageObject;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * this class will load the pre-procced csv data into the prospect data structure owned by the SOMMapData object
 * @author John Turner
 *
 */
public abstract class SOM_ExCSVDataLoader implements Callable<Boolean>{
    protected SOM_MapManager mapMgr;
    private MessageObject msgObj;
    private String fileName, dispYesStr, dispNoStr;
    private int thdIDX;
    private FileIOManager fileIO;
    //ref to map to add to, either prospects or validation records
    private ConcurrentSkipListMap<String, SOM_Example> mapToAddTo;
    protected String type;
    
    public SOM_ExCSVDataLoader(SOM_MapManager _mapMgr, int _thdIDX, String _fileName, String _yStr, String _nStr, ConcurrentSkipListMap<String, SOM_Example> _mapToAddTo) {    
        mapMgr=_mapMgr;
        msgObj = MessageObject.getInstance();thdIDX=_thdIDX;fileName=_fileName;dispYesStr=_yStr;dispNoStr=_nStr; 
        mapToAddTo = _mapToAddTo;
        fileIO = new FileIOManager(msgObj,"SOMExCSVDataLoader TH_IDX_"+String.format("%02d", thdIDX));
        type="";
    }//ctor
    
    protected abstract SOM_Example buildExample(String oid, String str);
    
    @Override
    public Boolean call() throws Exception {    
        msgObj.dispMessage("SOMExCSVDataLoader", type+": call thd : " +String.format("%02d", thdIDX),"Start loading file :"+fileName, MsgCodes.info1);    
        String[] csvLoadRes = fileIO.loadFileIntoStringAra(fileName, dispYesStr, dispNoStr);
        //ignore first entry - header
        for (int j=1;j<csvLoadRes.length; ++j) {
            String str = csvLoadRes[j];
            int pos = str.indexOf(',');
            String oid = str.substring(0, pos);
            SOM_Example ex = buildExample(oid, str);
            SOM_Example oldEx = mapToAddTo.put(ex.OID, ex);    
            if(oldEx != null) {msgObj.dispMessage("SOMExCSVDataLoader", type+": call thd : " +String.format("%02d", thdIDX), "ERROR : "+thdIDX+" : Attempt to add duplicate record to prospectMap w/OID : " + oid, MsgCodes.error2);    }
        }        
        msgObj.dispMessage("SOMExCSVDataLoader", type+": call thd : " +String.format("%02d", thdIDX),"Finished loading file :"+fileName, MsgCodes.info1);    
        return true;
    }    
}//class SOMExCSVDataLoader
