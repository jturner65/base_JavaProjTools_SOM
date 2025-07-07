package base_SOM_Objects.som_ui.win_disp_ui;

import java.util.HashMap;
import java.util.Map;

public enum SOM_MseOvrDispTypeVals {
    mseOvrMapNodeLocIDX, mseOvrUMatDistIDX, mseOvrMapNodePopIDX, mseOvrFtrIDX, mseOvrClassIDX, mseOvrCatIDX, mseOvrNoneIDX, mseOvrOtherIDX;
    private static final String[] 
            _typeExplanation = new String[]{
                    "Map Node Location and Coordinates",
                    "UMatrix Distance for Map Node",
                    "Population of examples mapped to Map Node",
                    "Feature values for Map Node",
                    "Class values for Map Node",
                    "Category values for Map Node",
                    "No Map Node Info Display",
                    "Custom Info Display"
                    };
    private static final String[] 
            _typeName = new String[]{"mseOvrMapNodeLocIDX","mseOvrUMatDistIDX","mseOvrMapNodePopIDX","mseOvrFtrIDX","mseOvrClassIDX","mseOvrCatIDX","mseOvrNoneIDX","mseOvrOtherIDX"};
    //used for file names
    private static final String[] 
            _typeBrfName = new String[]{"MapNodeLoc","UMatDist","Population","Features","Classes","Categories","None","Other"};
    public static String[] getListOfTypes() {return _typeName;}
    private static Map<Integer, SOM_MseOvrDispTypeVals> map = new HashMap<Integer, SOM_MseOvrDispTypeVals>(); 
        static { for (SOM_MseOvrDispTypeVals enumV : SOM_MseOvrDispTypeVals.values()) { map.put(enumV.ordinal(), enumV);}}
    public int getOrdinal() {return ordinal();}
    public static SOM_MseOvrDispTypeVals getEnumByIndex(int idx){return map.get(idx);}
    public static SOM_MseOvrDispTypeVals getEnumFromValue(int idx){return map.get(idx);}
    public static int getNumVals(){return map.size();}                        //get # of values in enum
    public String getName() {return _typeName[ordinal()];}
    public String getBrfName() {return _typeBrfName[ordinal()];}
    public String getExplanation() {return _typeExplanation[ordinal()];}
    public static String getNameByVal(int _val) {return _typeName[_val];}
    public static String getBrfNameByVal(int _val) {return _typeBrfName[_val];}
    public static String getExplanationByVal(int _val) {return _typeExplanation[_val];}
    @Override
    public String toString() { return ""+ordinal() + ":"+_typeExplanation[ordinal()]; }    
}
