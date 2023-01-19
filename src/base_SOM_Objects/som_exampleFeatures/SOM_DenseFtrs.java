package base_SOM_Objects.som_exampleFeatures;

import java.util.ArrayList;
import java.util.TreeMap;

import base_SOM_Objects.som_exampleFeatures.base.SOM_Features;
import base_SOM_Objects.som_examples.base.SOM_Example;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * These are dense features, where most feature values are non-0.
 * These are represented by arrays.
 * 
 * @author john
 *
 */
public class SOM_DenseFtrs extends SOM_Features {	
	//use a 2d array to hold features - 1st idx is type of feature, 2nd idx is actual feature idx
	private Float[][] ftrMaps;	
	//this 2d array is what is used by examples to compare for mappings - this may include combinations of other features or values
	//when all ftrs are calculated (unnormalized, normed per feature across all data, normed per example) they need to be mapped to this structure, possibly in
	//combination with an alternate set of features or other calculations
	private Float[][] compFtrMaps;

	public SOM_DenseFtrs(SOM_Example _ex, int _numFtrs) {	super(_ex, _numFtrs);	}
	
	public SOM_DenseFtrs(SOM_DenseFtrs _otr) {
		super(_otr);
		ftrMaps = _otr.ftrMaps;
		compFtrMaps = _otr.compFtrMaps;
	}//copy ctor
	
	/**
	 * Initialize all ftr values.  Initially called from base class constructor
	 */
	@Override
	protected final void initFtrs() {
		ftrMaps = new Float[ftrMapTypeKeysAra.length][];
		for (int i=0;i<ftrMaps.length;++i) {			ftrMaps[i] = new Float[numFtrs]; 		}
		compFtrMaps = new Float[ftrMapTypeKeysAra.length][];	
		for (int i=0;i<compFtrMaps.length;++i) {			compFtrMaps[i] = new Float[numFtrs]; 	}
	}
	
	//minimal benfit for dense features
	@Override
	protected final synchronized void clearAllFtrMaps() {for (int i=0;i<ftrMaps.length;++i) {	clearFtrMap(i);}}	
	@Override
	protected final synchronized void clearFtrMap(int idx) {ftrMaps[idx] = new Float[numFtrs]; ; clearCompFtrMap(idx);}
	//clear instead of reinstance
	@Override
	protected final synchronized void clearAllCompFtrMaps() {for (int i=0;i<compFtrMaps.length;++i) {	clearCompFtrMap(i);}}
	@Override
	protected final synchronized void clearCompFtrMap(int idx) {compFtrMaps[idx] = new Float[numFtrs];}

	
	@Override
	protected void buildFtrRprtStructs_Indiv() {		
		for(Integer mapToGet : ftrMapTypeKeysAra) {//type of features
			int rank = 0;
			Float[] ftrMap = ftrMaps[mapToGet];
			//go through features ara, for each ftr idx find rank 
			TreeMap<Float, ArrayList<Integer>> mapOfFtrsToIdxs = mapOfWtsToFtrIDXs[mapToGet];
			//shouldn't be null - means using inappropriate key
			if(mapOfFtrsToIdxs == null) {ex.mapMgr.getMsgObj().dispMessage("SOMExample" + ex.OID,"buildFtrRprtStructs","Using inappropriate key to access mapOfWtsToFtrIDXs : " + mapToGet + " No submap exists with this key.", MsgCodes.warning2); return;}	
			for (int ftrIDX = 0; ftrIDX < ftrMap.length; ++ftrIDX) {
				float wt = ftrMap[ftrIDX];
				ArrayList<Integer> ftrIDXsAtWt = mapOfFtrsToIdxs.get(wt);
				if (ftrIDXsAtWt == null) {ftrIDXsAtWt = new ArrayList<Integer>(); }
				ftrIDXsAtWt.add(ftrIDX);
				mapOfFtrsToIdxs.put(wt, ftrIDXsAtWt);
			}
			//after every feature is built, then poll mapOfFtrsToIdxs for ranked features
			TreeMap<Integer,Integer> mapOfRanks = mapOfFtrIDXVsWtRank[mapToGet];
			for (Float wtVal : mapOfFtrsToIdxs.keySet()) {
				ArrayList<Integer> ftrsAtRank = mapOfFtrsToIdxs.get(wtVal);
				for (Integer ftr : ftrsAtRank) {	mapOfRanks.put(ftr, rank);}
				++rank;
			}
			mapOfFtrIDXVsWtRank[mapToGet] = mapOfRanks;//probably not necessary since already initialized (will never be empty or deleted)					
		}//for ftrType	
	}//buildFtrRprtStructs_Indiv

	/**
	 * with dense features the entire vector is full, whether zero value or not.
	 */
	@Override
	public void buildAllNonZeroFtrIDXs() {
		allNonZeroFtrIDXs = new ArrayList<Integer>();
		for(int idx=0;idx<ftrMaps[unNormFtrMapTypeKey].length;++idx) {	allNonZeroFtrIDXs.add(idx);}
	}//buildAllNonZeroFtrIDXs
	
	@Override
	public final String _toSVMString(int _type) {
		Float[] ftrs = ftrMaps[_type];
		String res = "";
		for (Integer ftrIdx : allNonZeroFtrIDXs) {res += "" + ftrIdx + ":" + String.format("%1.7g", ftrs[ftrIdx]) + " ";}
		return res;
	}//_toSVMString
	
	/**
	 * build a feature vector from the map of ftr values
	 * @param _type type of features to retrieve
	 * @return array holding all n features, including 0 values
	 */
	@Override
	public final float[] _getFtrsFromMap(int _type) {
		float[] ftrs = new float[ftrMaps[_type].length];
		System.arraycopy(ftrMaps[_type].length, 0, ftrs, 0, ftrs.length);
		return ftrs;
	}
	/**
	 * build a map of features, keyed by ftr idx and value == ftr val @ idx
	 * @param _type type of features to retrieve
	 * @return array holding all n features, including 0 values
	 */
	@Override
	public final TreeMap<Integer, Float> _getFtrMap(int _type){ 
		TreeMap<Integer, Float> res = new TreeMap<Integer, Float>();
		for (Integer ftrIdx : allNonZeroFtrIDXs) {res.put(ftrIdx, ftrMaps[_type][ftrIdx]);}		
		return res;
	}


}//class SOM_DenseFtrs
