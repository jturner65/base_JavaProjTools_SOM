package base_SOM_Objects.som_mapnodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Render_Interface.IRenderInterface;
import base_SOM_Objects.som_examples.base.SOM_Example;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;


//this class will hold a structure to aggregate and process the examples of a particular type that consider the owning node a BMU
public class SOM_MapNodeBMUExamples{
	//owning node of these examples
	private SOM_MapNode node;
	//map of examples that consider node to be their bmu; keyed by euclidian distance
	private TreeMap<Double,HashSet<SOM_Example>> examplesBMU;
	//size of examplesBMU
	private int numMappedEx;
	//log size of examplesBMU +1, used for visualization radius
	private float logExSize;	
	//detail of rendered point representing parent node - should be based roughly on size of population
	private int nodeSphrDet;
	//string array holding relevant info for visualization
	private String[] visLabel;
	//data type of examples this is managing
	private SOM_ExDataType dataType;
	//whether or not this BMU node has examples
	private boolean hasExamples;
	//color to show node with, based on whether it has examples or not
	private int[] dispClrs;
	//this is node we copied from, if this node is copied; otherwise it is the same as node
	private SOM_MapNode copyNode;
	//this is the distance from copyNode that this object's owning node is
	private double sqDistToCopyNode;
	//
	public SOM_MapNodeBMUExamples(SOM_MapNode _node, SOM_ExDataType _dataType) {	
		node = _node;
		dataType = _dataType;
		examplesBMU = new TreeMap<Double,HashSet<SOM_Example>>(); 
		init();
		copyNode = node;
	}//ctor
	/**
	 * (re)initialize this structure;
	 */
	public void init() {
		examplesBMU.clear();
		numMappedEx = 0;
		logExSize = 0;
		nodeSphrDet = 2;
		hasExamples = false;
		dispClrs = node.getMapAltClrs();
		sqDistToCopyNode = 0.0;
		copyNode = node;
		visLabel = new String[] {""+node.OID+" : ", ""+numMappedEx};
	}//init
	
	//set this example to be a copy of passed example
	public void setCopyOfMapNode(double sqdist, SOM_MapNodeBMUExamples otrEx) {
		examplesBMU.clear();
		TreeMap<Double,HashSet<SOM_Example>> otrExamples = otrEx.examplesBMU;
		for(Double dist : otrExamples.keySet()) {
			HashSet<SOM_Example> otrExs = otrExamples.get(dist);
			HashSet<SOM_Example> myExs = new HashSet<SOM_Example>();
			for(SOM_Example ex : otrExs) {myExs.add(ex);}
			examplesBMU.put(dist, myExs);
		}		
		sqDistToCopyNode = sqdist;
		copyNode = otrEx.node;
		hasExamples = false;
	}
	
	//add passed example
	/**
	 * add passed example to this map node as an example that considers this map node to be its bmu
	 * @param sqdist the squared distance from ex to the map node
	 * @param _ex the example treating this map node as bmu
	 */
	public void addExample(double sqdist, SOM_Example _ex) {
		//synching on examplesBMU since owning map node might be called multiple times by different examples in a multi-threaded environment
		synchronized(examplesBMU) {
			HashSet<SOM_Example> tmpList = examplesBMU.get(sqdist);
			if(tmpList == null) {tmpList = new HashSet<SOM_Example>();}
			tmpList.add(_ex);		
			examplesBMU.put(sqdist, tmpList);	
			
		}
		hasExamples = true;
	}//addExample
	
	//finalize calculations - perform after all examples are mapped - used for visualizations
	public void finalize() {	
		int numEx = 0;
		for(Double sqDist : examplesBMU.keySet()) {		numEx += examplesBMU.get(sqDist).size();	}
		numMappedEx = numEx;		
		logExSize = (float) Math.log(numMappedEx + 1)*1.5f;	
		nodeSphrDet = (int)( Math.log(logExSize+1)+2);
		visLabel = new String[] {""+node.OID+" : ", ""+numMappedEx};
		dispClrs = hasExamples ? node.getMapNodeClrs() : node.getMapAltClrs();
		if(!hasExamples && (dataType==SOM_ExDataType.Training)) {
			node.mapMgr.getMsgObj().dispInfoMessage("SOMMapNodeBMUExamples", "finalize", "Finalize for " +dataType.getName() + " non-example map node in SOMMapNodeBMUExamples with "+numMappedEx+" copied ex | dispClrs : ["+dispClrs[0]+","+dispClrs[1]+","+dispClrs[2]+"] | node addr : " + node.mapNodeCoord +" | copied node addr : "+copyNode.mapNodeCoord+" | dist to copy node : " + sqDistToCopyNode+".");
		}
	}
	//whether this map node is a copy of another or not
	public boolean hasMappedExamples(){return hasExamples;}
	public int getNumExamples() {return numMappedEx;}

	/**
	 * return the average magnitude of all examples mapped to this node, weighted by distance
	 * @return
	 */
	public float getAvgWtMagOfAllMappedExs() {
		double ttlMag = 0.0, ttlsqDistP1Inv = 0.0;
		//contribution is inversely proportional to 1 + distance from node
		for(Double sqDist : examplesBMU.keySet()) {
			Double sqDistP1Inv = 1.0/(1.0 + sqDist); //want inverse proportional to distance - very close should be higher than far away
			HashSet<SOM_Example> setOfEx = examplesBMU.get(sqDist);
			for(SOM_Example ex : setOfEx) {			ttlMag += ex.ftrVecMag * sqDistP1Inv;			ttlsqDistP1Inv += sqDistP1Inv;	}
		}
		return (float) (ttlsqDistP1Inv != 0.0 ? ttlMag/ttlsqDistP1Inv : 1.0);
	}//getAvgMagOfAllMappedExs
	
	/////////////////////
	// drawing routines for owning node
	public void drawMapNodeWithLabel(IRenderInterface p) {
		p.pushMatState();	
			p.showTextAra(node.mapLoc, logExSize, nodeSphrDet, dispClrs,  visLabel); 		
		p.popMatState();		
	}

	public void drawMapNodeNoLabel(IRenderInterface p) {
		p.pushMatState();	
			p.showPtAsSphere(node.mapLoc, logExSize, nodeSphrDet, dispClrs[0], dispClrs[1]); 		
		p.popMatState();		
	}
	public void drawMapNodeWithLabel_Clr(IRenderInterface p, int[] _dispClrs) {
		p.pushMatState();		
			p.translate(node.mapLoc.x,node.mapLoc.y,node.mapLoc.z); 	
			p.setFill(_dispClrs,255); p.setStroke(_dispClrs,255);		
			p.drawSphere(myPointf.ZEROPT, logExSize, nodeSphrDet);

			p.showTextAra(1.2f * logExSize,IRenderInterface.gui_Cyan, visLabel);		
		p.popMatState();		
	}

	public void drawMapNodeNoLabel_Clr(IRenderInterface p, int[] _dispClrs) {
		p.pushMatState();			 			
			p.setFill(_dispClrs,255); p.setStroke(_dispClrs,255);	
			p.drawSphere(node.mapLoc, logExSize, nodeSphrDet);
		p.popMatState();		
	}
	

	public float getPopNodeSize() {return logExSize;}
	
	//return a listing of all examples and their distance from this BMU
	public HashMap<SOM_Example, Double> getExsAndDist(){
		HashMap<SOM_Example, Double> res = new HashMap<SOM_Example, Double>();
		for(double dist : examplesBMU.keySet() ) {
			HashSet<SOM_Example> tmpList = examplesBMU.get(dist);
			if(tmpList == null) {continue;}//should never happen			
			for (SOM_Example ex : tmpList) {res.put(ex, dist);}
		}
		return res;
	}//getExsAndDist()
	
	//return all example OIDs in array of CSV form, with key being distance and columns holding all examples that distance away
	public String[] getAllExampleDescs() {
		ArrayList<String> tmpRes = new ArrayList<String>();
		for(double dist : examplesBMU.keySet() ) {
			HashSet<SOM_Example> tmpList = examplesBMU.get(dist);
			if(tmpList == null) {continue;}//should never happen
			String tmpStr = String.format("%.6f", dist);
			for (SOM_Example ex : tmpList) {tmpStr += "," + ex.OID;	}
			tmpRes.add(tmpStr);
		}		
		return tmpRes.toArray(new String[1]);		
	}//getAllTestExamples
}//class SOMMapNodeExamples

