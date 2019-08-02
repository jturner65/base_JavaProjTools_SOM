package base_SOM_Objects.som_ui.win_disp_ui;

import java.util.HashMap;
import java.util.Map;

public enum SOM_MseOvrDispTypeVals {
	mseOvrMapNodeLocIDX (0), mseOvrUMatDistIDX (1), mseOvrMapNodePopIDX (2),  mseOvrFtrIDX (3), mseOvrClassIDX  (4),  mseOvrCatIDX (5),  mseOvrNoneIDX(6),  mseOvrOtherIDX(7);	
	private int value; 
	private static final String[] 
			_typeExplanation = new String[] {
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
			_typeName = new String[] {"mseOvrMapNodeLocIDX","mseOvrUMatDistIDX","mseOvrMapNodePopIDX","mseOvrFtrIDX","mseOvrClassIDX","mseOvrCatIDX","mseOvrNoneIDX","mseOvrOtherIDX"};
	//used for file names
	private static final String[] 
			_typeBrfName = new String[] {"MapNodeLoc","UMatDist","Population","Features","Classes","Categories","None","Other"};
	public static String[] getListOfTypes() {return _typeName;}
	private static Map<Integer, SOM_MseOvrDispTypeVals> map = new HashMap<Integer, SOM_MseOvrDispTypeVals>(); 
		static { for (SOM_MseOvrDispTypeVals enumV : SOM_MseOvrDispTypeVals.values()) { map.put(enumV.value, enumV);}}
	private SOM_MseOvrDispTypeVals(int _val){value = _val;} 
	public int getVal(){return value;}
	public static SOM_MseOvrDispTypeVals getVal(int idx){return map.get(idx);}
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
