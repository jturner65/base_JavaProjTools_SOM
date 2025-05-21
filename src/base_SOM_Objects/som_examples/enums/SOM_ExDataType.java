package base_SOM_Objects.som_examples.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * enum used to specify each kind of example data point, primarily for visualization purposes
 * @author john turner 
 */
public enum SOM_ExDataType {
	Training, Testing, Validation, Product, MapNode, MouseOver;
	private final String[] _typeExplanation = new String[] {
			"Training Data (Used to train the SOM)", 
			"Testing Data (Held-out training data used to investigate training)",
			"Validation Data (Data to be clustered on map)",
			"Product Data (Treat as exemplars to be assigned to clusters)",
			"Map Node (Represents a node on the SOM)",
			"Mouse Over (Data query at mouse location)"};
	private static final String[] _typeName = new String[] {"Training","Testing","Validation","Product","MapNode","MouseOver"};
	public static String[] getListOfTypes() {return _typeName;}
	private static Map<Integer, SOM_ExDataType> map = new HashMap<Integer, SOM_ExDataType>(); 
	static { for (SOM_ExDataType enumV : SOM_ExDataType.values()) { map.put(enumV.ordinal(), enumV);}}
	public int getVal() {return ordinal();}
	public int getOrdinal() {return ordinal();}
	public static SOM_ExDataType getEnumByIndex(int idx){return map.get(idx);}
	public static SOM_ExDataType getEnumFromValue(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	public String getName() {return _typeName[ordinal()];}
	@Override
    public String toString() { return ""+ordinal() + ":"+_typeExplanation[ordinal()]; }	
}//enum ExDataType


