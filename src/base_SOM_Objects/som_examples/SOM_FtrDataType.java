package base_SOM_Objects.som_examples;

import java.util.HashMap;
import java.util.Map;

/**
 * describes the type of feature vector - either unmodified, normalized (to 1.0) or standardized (each feature scaled 0->1 across all training data)
 * @author john
 *
 */
public enum SOM_FtrDataType {
	UNNORMALIZED(0), FTR_NORM(1), EXMPL_NORM(2);
	private int value; 
	private static final String[] 
			_typeExplanation = new String[] {"Unnormalized","Feature (0->1 per ftr across all training data)","Example (vector mag==1)"};
	private static final String[] 
			_typeName = new String[] {"Unnormalized","Feature (0->1 per ftr)","Example (vector mag==1)"};
	//used for file names
	private static final String[] 
			_typeBrfName = new String[] {"unNormFtrs","perFtrNorm","perExNorm"};
	public static String[] getListOfTypes() {return _typeName;}
	private static Map<Integer, SOM_FtrDataType> map = new HashMap<Integer, SOM_FtrDataType>(); 
		static { for (SOM_FtrDataType enumV : SOM_FtrDataType.values()) { map.put(enumV.value, enumV);}}
	private SOM_FtrDataType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static SOM_FtrDataType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	public String getName() {return _typeName[value];}
	public String getBrfName() {return _typeBrfName[value];}
	public String getExplanation() {return _typeExplanation[value];}
	public static String getNameByVal(int _val) {return _typeName[_val];}
	public static String getBrfNameByVal(int _val) {return _typeBrfName[_val];}
	public static String getExplanationByVal(int _val) {return _typeExplanation[_val];}
	@Override
    public String toString() { return ""+value + ":"+_typeExplanation[value]; }	

}
