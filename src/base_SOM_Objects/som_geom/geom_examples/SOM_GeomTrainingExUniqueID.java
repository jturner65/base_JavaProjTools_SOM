package base_SOM_Objects.som_geom.geom_examples;

import base_Math_Objects.MyHashCodeUtils;

/**
 * this class is a struct that will hold the idxs of the sample array that make up a single training example
 * The purpose of this class is to facilitate uniqueness in training examples - the hash for the object is built
 * using all the idxs - any object that has the same idxs should come back as equal to this object
 * @author john
 *
 */
public class SOM_GeomTrainingExUniqueID {
	/**
	 * idxs used to build this object - EXPECTED TO BE SORTED!
	 */
	public final Integer[] idxs;
	/**
	 * precomputed hash code
	 */
	public int hashCode = 0;

	public SOM_GeomTrainingExUniqueID(Integer[] _idxs) {
		idxs = _idxs;
		hashCode = hashCode();
	}

	@Override
	public int hashCode() {
		
		if(hashCode == 0) {
			int result = MyHashCodeUtils.SEED;
			for(int i=0;i<idxs.length;++i) {	result = MyHashCodeUtils.hash(result, (long)idxs[i]);	}
			hashCode =  result;
		} 
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {return true;}
		if (obj == null) {return false;}
		if (!(obj instanceof SOM_GeomTrainingExUniqueID)) {return false;}
		SOM_GeomTrainingExUniqueID other = (SOM_GeomTrainingExUniqueID) obj;
		if(other.idxs.length != this.idxs.length) {return false;}
		for(int i=0;i<idxs.length;++i) {if(other.idxs[i] != this.idxs[i]) {return false;}}
		return true;
	}
	
	
}//SOM_GeomTrainingExUniqueID
