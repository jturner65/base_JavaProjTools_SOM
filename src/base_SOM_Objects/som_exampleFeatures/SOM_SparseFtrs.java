package base_SOM_Objects.som_exampleFeatures;

import java.util.ArrayList;
import java.util.TreeMap;

import base_SOM_Objects.som_examples.SOM_Example;
import base_Utils_Objects.io.MsgCodes;

/**
 * These are sparse features for an example - these will use
 * the sparse feature training mechanism in the SOM code.
 * 
 * Sparse features are mostly 0.  These will take up less space in memory
 * since the feature data is represented by a map instead of an array
 * 
 * @author john
 *
 */
public class SOM_SparseFtrs extends SOM_Features {

	//use a map to hold only sparse data of each frmt for feature vector
	private TreeMap<Integer, Float>[] ftrMaps;	
	//this map is what is used by examples to compare for mappings - this may include combinations of other features or values
	//when all ftrs are calculated (unmodded, normalized and stdizd) they need to be mapped to this structure, possibly in
	//combination with an alternate set of features or other calculations
	private TreeMap<Integer, Float>[] compFtrMaps;

	public SOM_SparseFtrs(SOM_Example _ex, int _numFtrs) {
		super(_ex, _numFtrs);
	}
	
	public SOM_SparseFtrs(SOM_SparseFtrs _otr) {
		super(_otr);
		ftrMaps = _otr.ftrMaps;
		compFtrMaps = _otr.compFtrMaps;
	}
	
	/**
	 * Initialize all ftr values.  Initially called from base class constructor
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final void initFtrs() {
		ftrMaps = new TreeMap[ftrMapTypeKeysAra.length];
		for (int i=0;i<ftrMaps.length;++i) {			ftrMaps[i] = new TreeMap<Integer, Float>(); 		}
		compFtrMaps = new TreeMap[ftrMapTypeKeysAra.length];		
		for (int i=0;i<compFtrMaps.length;++i) {			compFtrMaps[i] = new TreeMap<Integer, Float>(); 		}
	}//initFtrs()	
	
	//clear instead of reinstance - if ftr maps are cleared then compFtrMaps should be cleared as well
	@Override
	protected final synchronized void clearAllFtrMaps() {for (int i=0;i<ftrMaps.length;++i) {	clearFtrMap(i);}}	
	@Override
	protected final synchronized void clearFtrMap(int idx) {ftrMaps[idx].clear(); clearCompFtrMap(idx);}
	//clear instead of reinstance
	@Override
	protected final synchronized void clearAllCompFtrMaps() {for (int i=0;i<compFtrMaps.length;++i) {	clearCompFtrMap(i);}}
	@Override
	protected final synchronized void clearCompFtrMap(int idx) {compFtrMaps[idx].clear();}
	

	@Override
	protected void buildFtrRprtStructs_Indiv() {		
		for(Integer mapToGet : ftrMapTypeKeysAra) {//type of features
			int rank = 0;
			TreeMap<Integer, Float> ftrMap = ftrMaps[mapToGet];
			//go through features ara, for each ftr idx find rank 
			TreeMap<Float, ArrayList<Integer>> mapOfFtrsToIdxs = mapOfWtsToFtrIDXs[mapToGet];
			//shouldn't be null - means using inappropriate key
			if(mapOfFtrsToIdxs == null) {ex.mapMgr.getMsgObj().dispMessage("SOMExample" + ex.OID,"buildFtrRprtStructs","Using inappropriate key to access mapOfWtsToFtrIDXs : " + mapToGet + " No submap exists with this key.", MsgCodes.warning2); return;}	
			for (Integer ftrIDX : ftrMap.keySet()) {
				float wt = ftrMap.get(ftrIDX);
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
	
	@Override
	public void buildAllNonZeroFtrIDXs() {
		allNonZeroFtrIDXs = new ArrayList<Integer>();
		for(Integer idx : ftrMaps[unNormFtrMapTypeKey].keySet()) {		allNonZeroFtrIDXs.add(idx);	}
	}//buildAllNonZeroFtrIDXs
	
	@Override
	public final String _toSVMString(int ftrType) {
		TreeMap<Integer, Float> ftrs = ftrMaps[ftrType];
		String res = "";
		for (Integer ftrIdx : allNonZeroFtrIDXs) {res += "" + ftrIdx + ":" + String.format("%1.7g", ftrs.get(ftrIdx)) + " ";}
		return res;
	}//_toSVMString
	/**
	 * build a feature vector from the map of ftr values
	 * @param _type type of features to retrieve
	 * @return array holding all n features, including 0 values
	 */
	@Override
	public final float[] _getFtrsFromMap(int _type) {
		TreeMap<Integer, Float> ftrMap = ftrMaps[_type];
		float[] ftrs = new float[numFtrs];
		for (Integer ftrIdx : ftrMap.keySet()) {ftrs[ftrIdx]=ftrMap.get(ftrIdx);		}
		return ftrs;
	}
	/**
	 * build a map of features, keyed by ftr idx and value == ftr val @ idx
	 * @param _type type of features to retrieve
	 * @return array holding all n features, including 0 values
	 */
	@Override
	public final TreeMap<Integer, Float> _getFtrMap(int _type){ return ftrMaps[_type];}


}//class SOM_SparseFtrs
