package base_SOM_Objects.som_examples;

/**
 * this class will manage a single data bound multi-dim array, consisting of 
 * ftr arrays for min, max, diff, count, etc.
 * idxs are as follows : 
 * 	1st idx is what type of examples are being aggregated - this will be specified by the instancing class 
 * 	2nd idx is min/max/diff, etc; 
 * 	last example is ftr idx in source data
 * @author john
 *
 */
public abstract class SOM_FtrDataBoundMonitor {
	protected Float[][][] bndsAra;

	//first dim of bndsAra is specified by instancing class, based on # of different data types across which 
	//ftr values are to be aggregated
	//1 extra entry for totals
	//separates calculations based on whether calc is done on a customer example, or on a true prospect example
	protected final int ttlOfAllCalcIDX;				//aggregate totals across all types
	protected final int numExampleTypeObjs;
	//2nd dim of bndsAra
	//meaning of each idx in bndsAra 1st dimension 
	protected static final int 
			minBndIDX = 0,					//mins for each feature
			maxBndIDX = 1,					//maxs for each feature
			countBndIDX = 2,				//count of entries for each feature
			diffBndIDX = 3; 				//max-min for each feature
	protected static final int numBndTypes = 4;	
	//set initial values to properly initialize bnds ara
	protected static final float[] initBnd = new float[] {1000000000.0f,-1000000000.0f, 0.0f, 0.0f};//min, max, count, diff

	//# of individaul elements per bound type - 3rd dim of bndsAra
	public final int numElems;
	
	/**
	 * Constructor for this bounds monitoring object.  Pass the # different data types in the instancing class, as well as the # of data values per data type being monitored
	 * @param _numDataTypes # of different types of data being monitored (perhaps training and validation data, for example)
	 * @param _numFtrVals
	 */
	public SOM_FtrDataBoundMonitor(int _numDataTypes, int _numFtrVals) {
		numElems = _numFtrVals;
		ttlOfAllCalcIDX = _numDataTypes;
		numExampleTypeObjs = ttlOfAllCalcIDX +1;
		bndsAra = new Float[numExampleTypeObjs][][];
		for (int i=0;i<bndsAra.length;++i) {
			bndsAra[i] = new Float[numBndTypes][];
			for (int j=0;j<bndsAra[i].length;++j) {	bndsAra[i][j]=fastCopyAra(numElems, initBnd[j]);	}	
		}
	}//ctor
	
	protected final Float[] fastCopyAra(int len, float val) {
		Float[] res = new Float[len];
		res[0]=val;	
		for (int i = 1; i < len; i += i) {System.arraycopy(res, 0, res, i, ((len - i) < i) ? (len - i) : i);}
		return res;
	}//fastCopyAra
	
	/**
	 * check if value is in bnds array for particular jp, otherwise modify bnd
	 * @param typeIDX
	 * @param destIDX
	 * @param val
	 */
	public final void checkValInBnds(Integer typeIDX, Integer destIDX, float val) {
		if (val < bndsAra[typeIDX][minBndIDX][destIDX]) {bndsAra[typeIDX][minBndIDX][destIDX]=val;bndsAra[typeIDX][diffBndIDX][destIDX] = bndsAra[typeIDX][maxBndIDX][destIDX]-bndsAra[typeIDX][minBndIDX][destIDX]; checkInAllBounds( destIDX, val);}
		if (val > bndsAra[typeIDX][maxBndIDX][destIDX]) {bndsAra[typeIDX][maxBndIDX][destIDX]=val;bndsAra[typeIDX][diffBndIDX][destIDX] = bndsAra[typeIDX][maxBndIDX][destIDX]-bndsAra[typeIDX][minBndIDX][destIDX]; checkInAllBounds( destIDX, val);}
	}
	
	/**
	 * manages mins, maxs, diffs of all calc types (customers, validation, training examples
	 * @param destIDX
	 * @param val
	 */
	protected final void checkInAllBounds(Integer destIDX, float val) {
		if (val < bndsAra[ttlOfAllCalcIDX][minBndIDX][destIDX]) {bndsAra[ttlOfAllCalcIDX][minBndIDX][destIDX]=val;bndsAra[ttlOfAllCalcIDX][diffBndIDX][destIDX] = bndsAra[ttlOfAllCalcIDX][maxBndIDX][destIDX]-bndsAra[ttlOfAllCalcIDX][minBndIDX][destIDX]; }
		if (val > bndsAra[ttlOfAllCalcIDX][maxBndIDX][destIDX]) {bndsAra[ttlOfAllCalcIDX][maxBndIDX][destIDX]=val;bndsAra[ttlOfAllCalcIDX][diffBndIDX][destIDX] = bndsAra[ttlOfAllCalcIDX][maxBndIDX][destIDX]-bndsAra[ttlOfAllCalcIDX][minBndIDX][destIDX];}
	}

	//get mins/diffs for ftr vals per ftr jp and for all vals per all jps
	public final Float[] getMinBndsAra() {return bndsAra[ttlOfAllCalcIDX][minBndIDX];}
	public final Float[] getMaxBndsAra() {return bndsAra[ttlOfAllCalcIDX][maxBndIDX];}
	public final Float[] getDiffBndsAra() {return bndsAra[ttlOfAllCalcIDX][diffBndIDX];}
	//aggregate all counts
	public final Float[] getCountBndsAra() {	return bndsAra[ttlOfAllCalcIDX][countBndIDX];}
	
	//individual type of data getters/setters
	public final Float[] getMinBndsAra(int typeIDX) {return bndsAra[typeIDX][minBndIDX];}
	public final Float[] getMaxBndsAra(int typeIDX) {return bndsAra[typeIDX][maxBndIDX];}
	public final Float[] getDiffBndsAra(int typeIDX) {return bndsAra[typeIDX][diffBndIDX];}
	public final Float[] getCountBndsAra(int typeIDX) {return bndsAra[typeIDX][countBndIDX];}
	
	//increment count of training examples with jp data represented by destIDX, and total calc value seen
	public final void incrBnds(int typeIDX, int destIDX) {
		synchronized(bndsAra[typeIDX][countBndIDX]) {bndsAra[typeIDX][countBndIDX][destIDX] +=1; }
		synchronized(bndsAra[ttlOfAllCalcIDX][countBndIDX]) {bndsAra[ttlOfAllCalcIDX][countBndIDX][destIDX] +=1;}
	}	
	
	public final String getDescForIdx(int idx) {
		return String.format("%6d", (Math.round(bndsAra[ttlOfAllCalcIDX][countBndIDX][idx]))) + "\t| Min val : " +String.format("%6.4f", bndsAra[ttlOfAllCalcIDX][minBndIDX][idx]) + "\t| Max val : " +String.format("%6.4f", bndsAra[ttlOfAllCalcIDX][maxBndIDX][idx]);
	}
	public final String getDescForIdx(int typeIDX,int idx) {
		return String.format("%6d", (Math.round(bndsAra[typeIDX][countBndIDX][idx]))) + "\t| Min val : " +String.format("%6.4f", bndsAra[typeIDX][minBndIDX][idx]) + "\t| Max val : " +String.format("%6.4f", bndsAra[typeIDX][maxBndIDX][idx]);
	}
	
}//SOM_FtrDataBoundMonitor
