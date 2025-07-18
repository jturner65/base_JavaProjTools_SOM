package base_SOM_Objects.som_examples.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * describes the type of feature vector - either unmodified, normalized (to 1.0) or standardized (each feature scaled 0->1 across all training data)
 * @author john
 *
 */
public enum SOM_FtrDataType {
    UNNORMALIZED, FTR_NORM, EXMPL_NORM;
    private static final String[] 
            _typeExplanation = new String[]{"Unnormalized","Feature (0->1 per ftr across all training data)","Example (vector mag==1)"};
    private static final String[] 
            _typeName = new String[]{"Unnormalized","Feature (0->1 per ftr)","Example (vector mag==1)"};
    //used for file names
    private static final String[] 
            _typeBrfName = new String[]{"unNormFtrs","perFtrNorm","perExNorm"};
    public static String[] getListOfTypes() {return _typeName;}
    private static Map<Integer, SOM_FtrDataType> map = new HashMap<Integer, SOM_FtrDataType>(); 
    static { for (SOM_FtrDataType enumV : SOM_FtrDataType.values()) { map.put(enumV.ordinal(), enumV);}}
    
    public int getVal() {return ordinal();}
    public int getOrdinal() {return ordinal();}
    public static SOM_FtrDataType getEnumByIndex(int idx){return map.get(idx);}
    public static SOM_FtrDataType getEnumFromValue(int idx){return map.get(idx);}
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
