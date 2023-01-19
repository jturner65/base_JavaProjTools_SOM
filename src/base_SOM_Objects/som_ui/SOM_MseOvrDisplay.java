package base_SOM_Objects.som_ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;

public abstract class SOM_MseOvrDisplay {
	protected SOM_MapManager mapMgr;
	protected float dispThesh;
	protected String[] mseLabelAra;
	protected float[] mseLabelDims;
	protected boolean display = false;
	protected myPointf mapLoc;
	protected int[] nodeClrs;	
	//fill and stroke color for mouse over data
	public int[] dpFillClr, dpStkClr;
	

	public SOM_MseOvrDisplay(SOM_MapManager _mapMgr, float _dispThesh) {
		mapMgr=_mapMgr;
		clearMseDat();
		dispThesh =_dispThesh;
	}//ctor

	protected abstract int[] setNodeColors(); 
	
	
	private void initAllCtor(float _thresh) {
		//needs to be here since jpJpgrpMon won't exist when this is first made
		dispThesh = _thresh;	
		mseLabelAra = new String[0];
		mseLabelDims = new float[0];
		initAll_Indiv();
	}//initAllCtor
	/**
	 * instancing-specific initialization called for every data change for mouse object
	 */
	protected abstract void initAll_Indiv();
	
	//build display label arraylist from passed map of float-name labels, using line as header/desc
	private int buildDispArrayList(ArrayList<String> _mseLblDat, TreeMap<Float, ArrayList<String>> valsToDisp, String line, int valsPerLine) {
		int longestLine = line.length();
		if (valsToDisp.size()== 0) {
			line += "None";
			_mseLblDat.add(line);
			longestLine = longestLine >= line.length() ? longestLine : line.length();
		}
		else {
			_mseLblDat.add(line);
			longestLine = longestLine >= line.length() ? longestLine : line.length();
			line="";
			int valsOnLine = 0;
			for (Float val : valsToDisp.keySet()) {
				ArrayList<String> valNameList = valsToDisp.get(val);
				for(String valName : valNameList) {
					line += ""+valName+":" + String.format("%03f", val);
					if(valsOnLine < valsPerLine-1) {				line += " | ";			}				
					++valsOnLine;
					if (valsOnLine >= valsPerLine) {
						longestLine = longestLine >= line.length() ? longestLine : line.length();
						_mseLblDat.add(line);
						line="";
						valsOnLine = 0;
					}
				}
			}
			if(valsOnLine>0) {//catch last values
				longestLine = longestLine >= line.length() ? longestLine : line.length();
				_mseLblDat.add(line);
			}
		}	
		return longestLine;		
	}//buildDispArrayList

	/**
	 * build display label arraylist from passed map of float-name labels, using line as header/desc
	 * @param _mseLblDat
	 * @param _valsToDisp
	 * @param line
	 * @param valsPerLine
	 * @return
	 */
	private int buildDispArrayList_IDXSort(ArrayList<String> _mseLblDat, TreeMap<Float, ArrayList<String>> _valsToDisp, String line, int valsPerLine) {
		int longestLine = line.length();
		if (_valsToDisp.size()== 0) {
			line += "None";
			_mseLblDat.add(line);
			longestLine = longestLine >= line.length() ? longestLine : line.length();
		}
		else {
			_mseLblDat.add(line);
			longestLine = longestLine >= line.length() ? longestLine : line.length();
			line="";
			int valsOnLine = 0;
			for (Float val : _valsToDisp.keySet()) {
				ArrayList<String> valNameList = _valsToDisp.get(val);
				for(String valName : valNameList) {
					line += ""+valName;//+":" + String.format("%03f", val);
					if(valsOnLine < valsPerLine-1) {				line += " | ";			}				
					++valsOnLine;
					if (valsOnLine >= valsPerLine) {
						longestLine = longestLine >= line.length() ? longestLine : line.length();
						_mseLblDat.add(line);
						line="";
						valsOnLine = 0;
					}
				}
			}
			if(valsOnLine>0) {//catch last values
				longestLine = longestLine >= line.length() ? longestLine : line.length();
				_mseLblDat.add(line);
			}
		}	
		return longestLine;		
	}//buildDispArrayList
	
	/**
	 * final setup for mouse label and label dimensions
	 * @param _mseLblDat
	 * @param _longestLine
	 */
	private final void finalizeMseLblDatCtor(ArrayList<String> _mseLblDat, int _longestLine) {
		mseLabelAra = _mseLblDat.toArray(new String[1]);
		mseLabelDims = new float[] {10, -10.0f,_longestLine*6.0f+10, mseLabelAra.length*10.0f + 15.0f};	
		display = true;
		finalizeMseLblDatCtor_Indiv(_mseLblDat,_longestLine);
	}//finalizeMseLblDatCtor
	
	/**
	 * Instance-specific final setup for mouse label and label dimensions
	 * @param _mseLblDat
	 * @param _longestLine
	 */
	protected abstract void finalizeMseLblDatCtor_Indiv(ArrayList<String> _mseLblDat, int _longestLine);
	
	/**
	 * 
	 * @param _ftrs
	 * @param _thresh
	 */
	public final void initMseDatFtrs_WtSorted(TreeMap<Integer, Float> _ftrs, float _thresh) {
		initAllCtor(_thresh);
		//decreasing order
		TreeMap<Float, ArrayList<String>> strongestFtrs = new TreeMap<Float, ArrayList<String>>(Collections.reverseOrder());
		for(Integer ftrIDX : _ftrs.keySet()) {
			Float ftr = _ftrs.get(ftrIDX);
			if(Math.abs(ftr) >= dispThesh) {		buildPerFtrData(ftrIDX,ftr,strongestFtrs);	}
		}	
		int count = 0;
		for(ArrayList<String> list : strongestFtrs.values()) {	count+=list.size();}
		ArrayList<String> _mseLblDat = new ArrayList<String>();

		String dispLine = getFtrDispTitleString(count);		
		int longestLine = buildDispArrayList(_mseLblDat, strongestFtrs, dispLine, 3);
		
		finalizeMseLblDatCtor(_mseLblDat, longestLine);
	}//ctor	for ftrs
	
	/**
	 * construct per ftr val display string
	 * @param _ftrs
	 * @param _thresh
	 */
	public final void initMseDatFtrs_IdxSorted(TreeMap<Integer, Float> _ftrs, float _thresh) {
		initAllCtor(_thresh);
		//decreasing order
		TreeMap<Float, ArrayList<String>> strongestFtrs = new TreeMap<Float, ArrayList<String>>();
		for(Integer ftrIDX : _ftrs.keySet()) {
			Float ftrIDX_F = (float)ftrIDX;
			Float ftr = _ftrs.get(ftrIDX);
			if(Math.abs(ftr) >= dispThesh) {		
				ArrayList<String> vals = strongestFtrs.get(ftrIDX_F);
				if(null==vals) {vals = new ArrayList<String>();}
				vals.add("["+String.format("%3d", ftrIDX) + "] = "+String.format("%05f", ftr));
				strongestFtrs.put(ftrIDX_F, vals);
			}
		}	
		
		int count = 0;
		for(ArrayList<String> list : strongestFtrs.values()) {	count+=list.size();}
		ArrayList<String> _mseLblDat = new ArrayList<String>();

		String dispLine = getFtrDispTitleString(count);		
		int longestLine = buildDispArrayList_IDXSort(_mseLblDat, strongestFtrs, dispLine, 3);
		
		finalizeMseLblDatCtor(_mseLblDat, longestLine);
	}//ctor	for ftrs
	protected abstract String getFtrDispTitleString(int count);
	/**
	 * construct per feature display string
	 * @param ftrIDX : the index in the feature vector
	 * @param ftr : the value in the ftr vector
	 * @param strongestFtrs : the map being populated with the string arrays at each ftr value
	 */
	protected abstract void buildPerFtrData(Integer ftrIDX, Float ftr, TreeMap<Float, ArrayList<String>> strongestFtrs);
	
	/**
	 * need to support all ftr types from map - this is built by distance/UMatrix map
	 * @param distClr
	 * @param distMin
	 * @param distDiff
	 * @param _thresh
	 */
	public final void initMseDatUMat(float distClr, float distMin, float distDiff, float _thresh) {
		initAllCtor(_thresh);
		float distData = ((distClr/255.0f)*distDiff) + distMin;
		ArrayList<String> _mseLblDat = new ArrayList<String>();
		String line = "Dist : " + String.format("%05f", distData);
		int longestLine = line.length();
		_mseLblDat.add(line);
		
		finalizeMseLblDatCtor(_mseLblDat, longestLine);
	}//ctor for distance
	
	
	/**
	 * display nearest map node probabilities, either class-based or category-based
	 * @param _nearestMapNode
	 * @param _thresh
	 * @param _useClassProbs whether to use per-class or per-category probabilities
	 */
	public final void initMseDatProb(SOM_MapNode _nearestMapNode, float _thresh, boolean _useClassProbs) {
		initAllCtor(_thresh);
		ArrayList<String> mseLblDat = new ArrayList<String>();
		TreeMap<Float, ArrayList<String>> mapNodeProbs = new TreeMap<Float, ArrayList<String>>(Collections.reverseOrder());
		String dispLine;
		TreeMap<Integer, Float> perGroupingProbs;
		if(_useClassProbs) {
			perGroupingProbs = _nearestMapNode.getClass_SegDataRatio();
			dispLine = getClassProbTitleString(_nearestMapNode, Math.round(_nearestMapNode.getTtlNumMappedClassInstances()));
		} else {
			perGroupingProbs = _nearestMapNode.getCategory_SegDataRatio();
			dispLine = getCategoryProbTitleString(_nearestMapNode, Math.round(_nearestMapNode.getTtlNumMappedCategoryInstances()));
		}
		//Either category or class probabilities
		for(Integer grp : perGroupingProbs.keySet()) {		
			float prob = perGroupingProbs.get(grp);
			ArrayList<String> valsAtProb = mapNodeProbs.get(prob);
			if(null==valsAtProb) {valsAtProb = new ArrayList<String>();}
			valsAtProb.add(""+grp);
			mapNodeProbs.put(prob, valsAtProb);				
		}	
		int count = 0;
		for(ArrayList<String> list : mapNodeProbs.values()) {	count+=list.size();}
		dispLine += " count : "+ count;		
		int longestLine = buildDispArrayList(mseLblDat, mapNodeProbs, dispLine, 3);		
		finalizeMseLblDatCtor(mseLblDat, longestLine);
	}//ctor for nearest map node probs
	
	/**
	 * display nearest map node's count of mapped examples
	 * @param _nearestMapNode
	 * @param _thresh
	 * @param _type
	 */
	public final void initMseDatCounts(SOM_MapNode _nearestMapNode, float _thresh, SOM_ExDataType _type) {
		initAllCtor(_thresh);
		ArrayList<String> mseLblDat = new ArrayList<String>();		
		int _typeIDX = _type.getVal();
		String dispLine = "# of mapped " + _type.getName() +" examples : " + _nearestMapNode.getNumExamples(_typeIDX);
		int longestLine = dispLine.length();
		mseLblDat.add(dispLine);		
		dispLine = "Has Mapped Examples : " + _nearestMapNode.getHasMappedExamples(_typeIDX);
		mseLblDat.add(dispLine);
		longestLine = longestLine >= dispLine.length() ? longestLine : dispLine.length();
		finalizeMseLblDatCtor(mseLblDat, longestLine);
	}//ctor for nearest map nod population of mapped training examples
	
	/**
	 * Display nearest map node's name
	 * @param nearestMapNode
	 * @param _thresh
	 */
	public final void initMseDatNodeName(SOM_MapNode _nearestMapNode, float _thresh) {
		initAllCtor(_thresh);
		ArrayList<String> mseLblDat = new ArrayList<String>();		
		String dispLine ="Nearest Map Node Name : "+ _nearestMapNode.OID;
		int longestLine = dispLine.length();
		mseLblDat.add(dispLine);				
		finalizeMseLblDatCtor(mseLblDat, longestLine);
	}//initMseDatNodeName
	
	public final void clearMseDat() {
		initAllCtor(0.0f);
		mapLoc = new myPointf(0,0,0);
		nodeClrs = setNodeColors(); 
		dpFillClr =  new int[] {0,0,0,255}; 
		dpStkClr = new int[] {0,0,0,255}; 
		display = false;
		clearMseDat_Indiv();
	}
	/**
	 * instancing-specific clear used for mouse object
	 */
	protected abstract void clearMseDat_Indiv();
		

	protected abstract String getClassProbTitleString(SOM_MapNode nearestMapNode, int ttlNumClasses);
	protected abstract String getCategoryProbTitleString(SOM_MapNode nearestMapNode, int ttlNumCategories);


	//get mse-ovr label array
	public String[] getMseOvrLblArray() {	return mseLabelAra;}
	//mse label is able to be displayed
	public boolean canDisplayMseLabel() {	return display;}	
//	@Override
//	public void buildMseLbl_Ftrs() {	}
//	@Override
//	public void buildMseLbl_Dists() {	}
	/**
	 * return height of mouse label
	 * @return
	 */
	public float getMseLabelYOffset() {return display ? mseLabelDims[3] : 0.0f;}
	/**
	 * draw current mouse label data at current position
	 * @param p
	 */	
	public void drawMeLblMap(IRenderInterface pa){drawMseLbl_Info(pa,mapLoc);}
	/**
	 * draw current mouse label data at passed position
	 * @param p
	 */
	public void drawMseLbl_Info(IRenderInterface pa, myPointf drawLoc) {
		if(!display) {return;}
		pa.pushMatState();
		pa.setFill(dpFillClr, dpFillClr[3]);pa.setStroke(dpStkClr,dpStkClr[3]);
		//draw point of radius rad at maploc with label	
		//p.showBox(mapLoc, rad, 5, clrVal,clrVal, IRenderInterface.gui_LightGreen, mseLabelDat);
		//(myPointf P, float rad, int det, int[] clrs, String[] txtAra, float[] rectDims)
		pa.showBoxTxtAra(drawLoc, 5, 5, nodeClrs, mseLabelAra, mseLabelDims);
		pa.popMatState();
	}
	public void setMapLoc(myPointf _pt) {mapLoc.set(_pt);}
	
}//SOM_MseOvrDisplay
