package base_SOM_Objects.som_ui.win_disp_ui;


import java.io.File;
import java.util.*;

import base_SOM_Objects.*;
import base_SOM_Objects.som_examples.*;
import base_UI_Objects.*;
import base_UI_Objects.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.myDispWindow;
import base_UI_Objects.windowUI.myGUIObj;
import base_Utils_Objects.*;
import base_Utils_Objects.io.MsgCodes;
import base_Utils_Objects.vectorObjs.myPoint;
import base_Utils_Objects.vectorObjs.myVector;

/**
 * base UI window functionality to be used for any SOM-based projects
 * @author john
 *
 */
public abstract class SOM_MapUIWin extends myDispWindow implements ISOM_UIWinMapDat{
	//map manager that is instanced 
	public SOM_MapManager mapMgr;
	//interface to facilitate keeping UI and the SOM MapData object synched w/respect to map data values
	public SOM_UIToMapCom mapUIAPI;	
	//idxs of boolean values/flags
	public static final int 
		buildSOMExe 				= 0,			//command to initiate SOM-building
		resetMapDefsIDX				= 1,			//reset default UI values for map
		mapDataLoadedIDX			= 2,			//whether map has been loaded or not	
		mapUseChiSqDistIDX			= 3,			//whether to use chi-squared (weighted by variance) distance for features or regular euclidean dist
		mapExclProdZeroFtrIDX		= 4,			//whether or not distances between two datapoints assume that absent features in source data point should be zero or ignored when comparing to map node ftrs

		//display/interaction
		mapDrawTrainDatIDX			= 5,			//draw training examples
		mapDrawTestDatIDX 			= 6,			//draw testing examples - data held out and not used to train the map 
		mapDrawNodeLblIDX			= 7,			//draw labels for nodes
		mapDrawWtMapNodesIDX		= 8,			//draw map nodes with non-0 (present) wt vals
		mapDrawPopMapNodesIDX	   	= 9,			//draw map nodes that are bmus for training examples
		mapDrawAllMapNodesIDX		= 10,			//draw all map nodes, even empty
		//UMatrix 		
		mapDrawUMatrixIDX			= 11,			//draw visualization of u matrix - distance between nodes
		mapDrawUMatSegImgIDX		= 12,			//draw the image of the interpolated segments based on UMatrix Distance
		mapDrawUMatSegMembersIDX	= 13,			//draw umatrix-based segments around regions of maps - visualizes clusters with different colors
		//ftr and ftr-dist-based
		mapDrawDistImageIDX			= 14,			//draw umatrix-like rendering based on sq dist between adjacent node vectors
		mapDrawFtrWtSegMembersIDX	= 15,			//draw ftr-wt-based segments around regions of map - display only segment built from currently display ftr on ftr map
		//class and category-based segments
		mapDrawClassSegmentsIDX		= 16,			//show class segments
		mapDrawCategorySegmentsIDX	= 17,			//show category (collection of classes) segments
		_categoryCanBeShownIDX		= 18,			//whether category values are used and can be shown on UI/interracted with
		_classCanBeShownIDX			= 19,			//whether class values are used and can be shown on UI/interracted with
		mapLockClassCatSegmentsIDX  = 20,			//lock category to cycle through classes
		
		showSelRegionIDX			= 21,			//highlight a specific region of the map, either all nodes above a certain threshold for a chosen ftr
		//train/test data managemen
		somTrainDataLoadedIDX		= 22,			//whether data used to build map has been loaded yet
		saveLocClrImgIDX			= 23,			//
		//save segment mappings
		saveAllSegmentMapsIDX		= 24;			//this will save all the segment mappings that have been defined
	
	public static final int numSOMBasePrivFlags = 25;
	//instancing class will determine numPrivFlags based on how many more flags are added
	
	/**
	 * # of priv flags from base class and instancing class
	 */
	private int numPrivFlags;
	
	//	//GUI Objects	
	public final static int 
		uiTrainDataFrmtIDX			= 0,			//format that training data should take : unmodified, normalized or standardized
		//uiTestDataFrmtIDX			= 1,			//format of vectors to use when comparing examples to nodes on map
		uiTrainDatPartIDX			= 1,			//partition % of training data out of total data (rest is testing)
		
		uiMapRowsIDX 				= 2,            //map rows
		uiMapColsIDX				= 3,			//map cols
		uiMapEpochsIDX				= 4,			//# of training epochs
		uiMapShapeIDX				= 5,			//hexagonal or rectangular
		uiMapBndsIDX				= 6,			//planar or torroidal bounds
		uiMapKTypIDX				= 7,			//0 : dense cpu, 1 : dense gpu, 2 : sparse cpu.  dense needs appropriate lrn file format
		uiMapNHdFuncIDX				= 8,			//neighborhood : 0 : gaussian, 1 : bubble
		uiMapRadCoolIDX				= 9,			//radius cooling 0 : linear, 1 : exponential
		uiMapLrnCoolIDX				= 10,			//learning rate cooling 0 : linear 1 : exponential
		uiMapLrnStIDX				= 11,			//start learning rate
		uiMapLrnEndIDX				= 12,			//end learning rate
		uiMapRadStIDX				= 13,			//start radius
		uiMapRadEndIDX				= 14,			//end radius
		uiMapPreBuiltDirIDX			= 15,			//list of prebuilt maps as defined in config - this specifies which prebuilt map to use
		uiMapNodeBMUTypeToDispIDX 	= 16,			//type of examples mapping to a particular node to display in visualization
		uiNodeWtDispThreshIDX 		= 17,			//threshold for display of map nodes on individual weight maps
		uiNodeInSegThreshIDX		= 18,			//threshold of u-matrix weight for nodes to belong to same segment
		uiMseRegionSensIDX			= 19,			//senstivity threshold for mouse-over
		uiPopMapNodeDispSizeIDX		= 20,			//only display populated map nodes that are this size or larger (log of population determines size)
		uiFtrSelectIDX				= 21,			//pick the feature to display, if ftr-idx wt graphs are being displayed
		uiCategorySelectIDX			= 22,			//pick the category to display, if category mapping is available/enabled
		uiClassSelectIDX			= 23;			//pick the class to display, if class mapping is available/enabled
	
	public static final int numSOMBaseGUIObjs = 24;
	//instancing class will specify numGUIObjs	
	protected double[] uiVals;				//raw values from ui components
	//
	//match descriptor string to index and index to string, to facilitate access
	public TreeMap<String, Integer> mapDatDescrToUIIdx;
	public TreeMap<Integer,String> mapUIIdxToMapDatDescr;
	//array of gui object idxs corresponding positionally with map dat names specified above
	public static final int[] mapObjUIIdxs = new int[] {
		uiMapColsIDX, uiMapRowsIDX, uiMapEpochsIDX, uiMapKTypIDX,uiMapRadStIDX, uiMapRadEndIDX,uiMapLrnStIDX,uiMapLrnEndIDX,
		uiMapShapeIDX ,uiMapBndsIDX,uiMapRadCoolIDX,uiMapNHdFuncIDX, uiMapLrnCoolIDX
	};	
	
	/**
	 * default scale factor for size of SOM Map
	 */
	private float SOMDimScaleFact = .95f;
	/**
	 * window constructor
	 * @param _p
	 * @param _n
	 * @param _flagIdx
	 * @param fc
	 * @param sc
	 * @param rd
	 * @param rdClosed
	 * @param _winTxt
	 * @param _canDrawTraj
	 */

	
	public SOM_MapUIWin(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		initAndSetSOMDatUIMaps();
	}//ctor
	
	//set convenience maps to more easily access UI objects related to map data/args
	private void initAndSetSOMDatUIMaps() {
		//build map descriptor index to mapdat descriptor string map. 
		mapDatDescrToUIIdx = new TreeMap<String, Integer>();
		mapUIIdxToMapDatDescr = new TreeMap<Integer,String> ();
		//mapDatNames mapObjUIIdxs
		for(int i=0;i<mapDatNames.length;++i) {
			String mapDatName = mapDatNames[i];
			Integer uiObjIDX = mapObjUIIdxs[i];
			mapDatDescrToUIIdx.put(mapDatName, uiObjIDX);
			mapUIIdxToMapDatDescr.put(uiObjIDX,mapDatName);
		}
	}//initAndSetSOMDatUIMaps
	
	
	/**
	 * Given a UI object's IDX value, provide the string MapDat key corresponding to it
	 * @param UIidx
	 * @return
	 */
	@Override
	public String getMapKeyStringFromUIidx(int UIidx) {		return mapUIIdxToMapDatDescr.get(UIidx);	}

	/**
	 * Given MapDat key, return an int corresponding to the appropriate ui object in the instancing window
	 * @param UIidx
	 * @return
	 */
	@Override
	public int getUIidxFromMapKeyString(String mapKey){		return mapDatDescrToUIIdx.get(mapKey);	}

	@Override
	protected final void initMe() {
		//initUIBox();				//set up ui click region to be in sidebar menu below menu's entries	
		mapMgr = buildMapMgr();
		initAfterMapMgrSet(new boolean[] {true,true});
	}//initMe()	
	
	/**
	 * initialize the map manager this window uses after it is built or set
	 * @param flagsToSet : idx 0 is mapDrawUMatrixIDX, idx 1 is mapExclProdZeroFtrIDX; either set these to initial values or copy current values
	 */
	private void initAfterMapMgrSet(boolean[] flagsToSet) {
		mapUIAPI = mapMgr.mapUIAPI;
		setVisScreenWidth(rectDim[2]);
		//only set for visualization - needs to reset static refs in msgObj
		mapMgr.setPADispWinData(this, pa);
		
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);		//may need some re-scaling to keep things in the middle and visible
		
		//init specific sim flags
		initPrivFlags(numPrivFlags);
		/**
		 * set these values from when UI was created 
		 */
		setPrivFlags(_categoryCanBeShownIDX, _catExistsAndIsShown);
		setPrivFlags(_classCanBeShownIDX, _classExistsAndIsShown);		
		setPrivFlags(mapDrawTrainDatIDX,getPrivFlags(mapDrawTrainDatIDX));
		setPrivFlags(mapDrawWtMapNodesIDX,getPrivFlags(mapDrawWtMapNodesIDX));
		setPrivFlags(mapUseChiSqDistIDX,getPrivFlags(mapUseChiSqDistIDX));
		setPrivFlags(mapDrawUMatrixIDX, flagsToSet[0]);
		setPrivFlags(mapExclProdZeroFtrIDX, flagsToSet[1]);
		//set initial values for UI
		//mapMgr.initFromUIWinInitMe((int)(this.guiObjs[uiTrainDataFrmtIDX].getVal()), (int)(this.guiObjs[uiTestDataFrmtIDX].getVal()),(float)(this.guiObjs[uiNodeWtDispThreshIDX].getVal()),(float)(this.guiObjs[uiPopMapNodeDispSizeIDX].getVal()), (int)(this.guiObjs[uiMapNodeBMUTypeToDispIDX].getVal()));
		mapMgr.initFromUIWinInitMe((int)(this.guiObjs[uiTrainDataFrmtIDX].getVal()), (float)(this.guiObjs[uiNodeWtDispThreshIDX].getVal()),(float)(this.guiObjs[uiPopMapNodeDispSizeIDX].getVal()), (int)(this.guiObjs[uiMapNodeBMUTypeToDispIDX].getVal()));

		initMeIndiv();
	}
	
	/**
	 * build instancing app's map manager
	 * @param _mapDims : dimensions of visible rep of map calculated based on visible size of window
	 * @return
	 */
	protected abstract SOM_MapManager buildMapMgr();
	
	/**
	 * set map manager from instancing app and reset all mapMgr-governed values in window
	 */
	public void setMapMgr(SOM_MapManager _mapMgr) {
		this.msgObj.dispInfoMessage("SOM_MapUIWin", "setMapMgr", "Start setting map manager from " +  mapMgr.getName() + " to " + _mapMgr.getName());		
		//set passed map manager as current
		mapMgr = _mapMgr;
		//re-init window to use this map manager
		initAfterMapMgrSet(new boolean[] {getPrivFlags(mapDrawUMatrixIDX), getPrivFlags(mapExclProdZeroFtrIDX)});
		//send new mapMgr's config data
		mapMgr.setUIValsFromProjConfig();
	}
//	private void setPassedIntFlags(int _numFlags, int[] _intFlagAra){
//		for(int idx=0;idx<_numFlags; ++idx) {	
//		int bitLoc = 1<<(idx%32);
//		setPrivFlags(idx,(_intFlagAra[idx/32] & bitLoc) == bitLoc);
//		}
//	}	
	
	
	protected abstract void initMeIndiv();	
	
	@Override
	/**
	 * initialize all private-flag based UI buttons here - called by base class before initMe
	 */
	public final void initAllPrivBtns(){	
		//add an entry for each button, in the order they are wished to be displayed		
		ArrayList<Object[]> tmpBtnNamesArray = new ArrayList<Object[]>();
		tmpBtnNamesArray.add(new Object[]{"Building SOM","Build SOM ",buildSOMExe});
		tmpBtnNamesArray.add(new Object[]{"Reset Dflt UI Vals","Reset Dflt UI Vals",resetMapDefsIDX});
		tmpBtnNamesArray.add(new Object[]{"Using ChiSq for Ftr Dist", "Not Using ChiSq Distance", mapUseChiSqDistIDX});       
		tmpBtnNamesArray.add(new Object[]{"Prdct Dist ignores 0-ftrs","Prdct Dist w/all ftrs", mapExclProdZeroFtrIDX});    
		tmpBtnNamesArray.add(new Object[]{"Hide Train Data", "Show Train Data", mapDrawTrainDatIDX});       
		tmpBtnNamesArray.add(new Object[]{"Hide Test Data", "Show Test Data", mapDrawTestDatIDX});        
		tmpBtnNamesArray.add(new Object[]{"Hide Nodes", "Show Nodes", mapDrawAllMapNodesIDX});    
		tmpBtnNamesArray.add(new Object[]{"Hide Lbls", "Show Lbls", mapDrawNodeLblIDX});        
		tmpBtnNamesArray.add(new Object[]{"Hide Nodes (by Pop)", "Show Nodes (by Pop)", mapDrawPopMapNodesIDX});    
		tmpBtnNamesArray.add(new Object[]{"Showing UMat (Bi-Cubic)", "Showing Ftr Map", mapDrawUMatrixIDX});        
		tmpBtnNamesArray.add(new Object[]{"Hide Hot Ftr Nodes (by Wt)", "Show Hot Ftr Nodes (by Wt)", mapDrawWtMapNodesIDX});     
		tmpBtnNamesArray.add(new Object[]{"Hide Ftr Wt Segments", "Show Ftr Wt Segments", mapDrawFtrWtSegMembersIDX});
		tmpBtnNamesArray.add(new Object[]{"Hide Clstr (U-Dist)", "Show Clstr (U-Dist)", mapDrawUMatSegMembersIDX}); 
		tmpBtnNamesArray.add(new Object[]{"Hide Clstr Image", "Show Clstr Image", mapDrawUMatSegImgIDX});  
		
		//these are only enabled if they have been defined to return values from instancing class
		String[] classBtnTFLabels = getClassBtnTFLabels();
		if((null != classBtnTFLabels) && (classBtnTFLabels.length == 2)) {tmpBtnNamesArray.add(new Object[]{classBtnTFLabels[0],classBtnTFLabels[1],mapDrawClassSegmentsIDX});}		
		String[] catBtnTFLabels = getCategoryBtnTFLabels();
		if((null != catBtnTFLabels) && (catBtnTFLabels.length == 2)) {tmpBtnNamesArray.add(new Object[]{catBtnTFLabels[0],catBtnTFLabels[1],mapDrawCategorySegmentsIDX});}				
		String[] saveSegmentTFLabels = getSegmentSaveBtnTFLabels();
		if((null != classBtnTFLabels) && (classBtnTFLabels.length == 2)) {tmpBtnNamesArray.add(new Object[]{saveSegmentTFLabels[0],saveSegmentTFLabels[1],saveAllSegmentMapsIDX});}		
		String[] catClassLockBtnTFLabels = getClassCatLockBtnTFLabels();
		if((null != catClassLockBtnTFLabels) && (catClassLockBtnTFLabels.length == 2)) {tmpBtnNamesArray.add(new Object[]{catClassLockBtnTFLabels[0],catClassLockBtnTFLabels[1],mapLockClassCatSegmentsIDX});}	
		//add instancing-class specific buttons - returns total # of private flags in instancing class
		numPrivFlags = initAllSOMPrivBtns_Indiv(tmpBtnNamesArray);
		//finalize setup for UI toggle buttons - convert to arrays
		truePrivFlagNames = new String[tmpBtnNamesArray.size()];
		falsePrivFlagNames = new String[truePrivFlagNames.length];
		privModFlgIdxs = new int[truePrivFlagNames.length];
		for(int i=0;i<truePrivFlagNames.length;++i) {
			Object[] tmpAra = tmpBtnNamesArray.get(i);
			truePrivFlagNames[i] = (String) tmpAra[0];
			falsePrivFlagNames[i] = (String) tmpAra[1];
			privModFlgIdxs[i] = (int) tmpAra[2];
		}		
		numClickBools = truePrivFlagNames.length;	
		initPrivBtnRects(0,numClickBools);		
		
	}//initAllPrivBtns

	/**
	 * Instancing class-specific (application driven) UI buttons to display are built 
	 * in this function.  Add an entry to tmpBtnNamesArray for each button, in the order 
	 * they are to be displayed
	 * @param tmpBtnNamesArray array list of Object arrays, where in each object array : 
	 * 			the first element is the true string label, 
	 * 			the 2nd elem is false string array, and 
	 * 			the 3rd element is integer flag idx 
	 * @return total number of privBtnFlags in instancing class (including those not displayed)
	 */
	protected abstract int initAllSOMPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);
	//these are used by instancing class to determine the names of the class and category data values used.  if these are empty then that means these features are not used
	/**
	 * Instance class determines the true and false labels the class buttons use - if empty then no classes used
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of class-based segment
	 */
	protected abstract String[] getClassBtnTFLabels();
	/**
	 * Instance class determines the true and false labels the category buttons use - if empty then no categories used
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of category-based segment
	 */
	protected abstract String[] getCategoryBtnTFLabels();
	
	/**
	 * Instance class determines the true and false labels the class-category locking should use
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of whether 
	 * category should be locked to allow selection through within-category classes
	 */
	protected abstract String[] getClassCatLockBtnTFLabels();

	/**
	 * This will return instance class-based true and false labels for save segment data.  if empty then no segment saving possible
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control saving of segment data
	 */
	protected abstract String[] getSegmentSaveBtnTFLabels();
	
	private boolean _catExistsAndIsShown = false;
	private boolean _classExistsAndIsShown = false;
	//initialize structure to hold modifiable menu regions
	@Override
	protected final void setupGUIObjsAras(){		
		//keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
		TreeMap<Integer, String[]> tmpListObjVals = new TreeMap<Integer, String[]>();
		tmpListObjVals.put(uiMapShapeIDX, new String[] {"rectangular","hexagonal"});
		tmpListObjVals.put(uiMapBndsIDX, new String[] {"planar","toroid"});
		tmpListObjVals.put(uiMapKTypIDX, new String[] {"Dense CPU", "Dense GPU", "Sparse CPU"});		
		tmpListObjVals.put(uiMapNHdFuncIDX, new String[]{"gaussian","bubble"});		
		tmpListObjVals.put(uiMapRadCoolIDX, new String[]{"linear","exponential"});
		tmpListObjVals.put(uiMapLrnCoolIDX, new String[]{"linear","exponential"});		
		tmpListObjVals.put(uiTrainDataFrmtIDX, SOM_FtrDataType.getListOfTypes());
		//tmpListObjVals.put(uiTestDataFrmtIDX, SOM_FtrDataType.getListOfTypes());		
		tmpListObjVals.put(uiMapPreBuiltDirIDX, new String[] {"None"});
		tmpListObjVals.put(uiMapNodeBMUTypeToDispIDX, SOM_MapManager.getNodeBMUMapTypes());
		tmpListObjVals.put(uiFtrSelectIDX, new String[] {"None"});		
		
		ArrayList<Object[]> tmpUIObjArray = new ArrayList<Object[]>();
		//tmpBtnNamesArray.add(new Object[]{"Building SOM","Build SOM ",buildSOMExe});
		//object array of elements of following format  : 
		//	the first element double array of min/max/mod values
		//	the 2nd element is starting value
		//	the 3rd elem is label for object
		//	the 4th element is boolean array of {treat as int, has list values, value is sent to owning window}
		
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiTrainDataFrmtIDX).length-1, 1.0}, 1.0*SOM_FtrDataType.Standardized.getVal(), "Train Data Frmt", new boolean[]{true, true, true}});   				//uiTrainDataFrmtIDX                                                                        
		//tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiTestDataFrmtIDX).length-1, 1.0}, 2.0, "Data Mapping Frmt", new boolean[]{true, true, true}});  				//uiTestDataFrmtIDX                                                                         
		tmpUIObjArray.add(new Object[] {new double[]{1.0, 100.0, 1.0}, 100.0,	"Data % To Train", new boolean[]{true, false, true}});   													//uiTrainDatPartIDX                                                                         
		tmpUIObjArray.add(new Object[] {new double[]{1.0, 120.0, 10}, 10.0, "# Map Rows", new boolean[]{true, false, true}});   															//uiMapRowsIDX 	 		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{1.0, 120.0, 10}, 10.0, "# Map Columns", new boolean[]{true, false, true}});   															//uiMapColsIDX	 		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{1.0, 200.0, 10}, 10.0, "# Training Epochs", new boolean[]{true, false, true}});  														//uiMapEpochsIDX		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapShapeIDX).length-1, 1},0.0, "Map Node Shape", new boolean[]{true, true, true}});   						//uiMapShapeIDX	 		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapBndsIDX).length-1, 1},1.0, "Map Boundaries",	new boolean[]{true, true, true}});  						//uiMapBndsIDX	 		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapKTypIDX).length-1, 1.01},2.0, "Dense/Sparse (C/G)PU",new boolean[]{true, true, true}});   				//uiMapKTypIDX	 		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapNHdFuncIDX).length-1, 1},0.0, "Neighborhood Func", new boolean[]{true, true, true}});   					//uiMapNHdFuncIDX		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapRadCoolIDX).length-1, 1},0.0, "Radius Cooling", new boolean[]{true, true, true}});   						//uiMapRadCoolIDX		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapLrnCoolIDX).length-1, 1},0.0, "Learn rate Cooling", new boolean[]{true, true, true}});   					//uiMapLrnCoolIDX		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.001, 10.0, 0.001}, 1.0, "Start Learn Rate", new boolean[]{false, false, true}});   													//uiMapLrnStIDX	 		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{0.001, 1.0, 0.001}, 0.1, "End Learn Rate", new boolean[]{false, false, true}});   														//uiMapLrnEndIDX		                                                                    
		tmpUIObjArray.add(new Object[] {new double[]{2.0, 300.0, 1.0},	 20.0, "Start Cool Radius", new boolean[]{true, false, true}});   													//uiMapRadStIDX	 	# nodes	                                                                
		tmpUIObjArray.add(new Object[] {new double[]{1.0, 10.0, 1.0},	 1.0, "End Cool Radius", new boolean[]{true, false, true}});   														//uiMapRadEndIDX		# nodes	  
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapPreBuiltDirIDX).length-1,1.0}, 0.0, "Pretrained Map Dirs", new boolean[] {true, true, true}});			//uiMapPreBuiltDirIDX
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapNodeBMUTypeToDispIDX).length-1, 1.0}, 0.0, "Ex Type For Node BMU", new boolean[]{true, true, true}}); 	//uiMapNodeBMUTypeToDispIDX                                                                 
		tmpUIObjArray.add(new Object[] {new double[]{0.0, 1.0, .01}, SOM_MapManager.initNodeInSegFtrWtDistThresh, "Map Node Disp Wt Thresh", new boolean[]{false, false, true}});   	//uiNodeWtDispThreshIDX                                                                     
		tmpUIObjArray.add(new Object[] {new double[]{0.0, 1.0, .001}, SOM_MapManager.initNodeInSegUMatrixDistThresh, "Segment UDist Thresh", new boolean[]{false, false, true}});   	//uiNodeInSegThreshIDX//threshold of u-matrix weight for nodes to belong to same segment    
		tmpUIObjArray.add(new Object[] {new double[]{0.0, 1.0, .1},	 0.0, "Mouse Over Sens",	new boolean[]{false, false, true}});   														//uiMseRegionSensIDX                                                                        
		tmpUIObjArray.add(new Object[] {new double[]{0.0, 20.0, .1},  0.0, "Pop Map Node Display Size Thresh",	new boolean[]{false, false, true}});														//uiPopMapNodeDispSizeIDX
		tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiFtrSelectIDX).length-1,1.0}, 0.0, "Feature IDX To Show", new boolean[] {true, true, true}});					//uiFtrSelectIDX
		
		String catUIDesc = getCategoryUIObjLabel();
		if((null!=catUIDesc) && (catUIDesc.length()>0)) {
			_catExistsAndIsShown = true;
			tmpListObjVals.put(uiCategorySelectIDX, new String[] {"None"});	
			tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiCategorySelectIDX).length-1,1.0}, 0.0, catUIDesc, new boolean[] {true, true, true}});			//uiMapPreBuiltDirIDX
		} else {			_catExistsAndIsShown = false;		}
		
		String classUIDesc = getClassUIObjLabel();
		if((null!=classUIDesc) && (classUIDesc.length()>0)) {
			_classExistsAndIsShown = true;
			tmpListObjVals.put(uiClassSelectIDX, new String[] {"None"});	
			tmpUIObjArray.add(new Object[] {new double[]{0.0, tmpListObjVals.get(uiClassSelectIDX).length-1,1.0}, 0.0, classUIDesc, new boolean[] {true, true, true}});			//uiMapPreBuiltDirIDX
		} else {			_classExistsAndIsShown = false;		}
		
		//populate instancing application objects
		setupGUIObjsArasIndiv(tmpUIObjArray,tmpListObjVals);
		
		int numGUIObjs = tmpUIObjArray.size();		
		guiMinMaxModVals = new double [numGUIObjs][3];
		guiStVals = new double[numGUIObjs];
		guiObjNames = new String[numGUIObjs];
		guiBoolVals = new boolean [numGUIObjs][4];
		uiVals = new double[numGUIObjs];//raw values
		for(int i =0;i<numGUIObjs; ++i) {
			guiMinMaxModVals[i] = (double[])tmpUIObjArray.get(i)[0];
			guiStVals[i] = (Double)tmpUIObjArray.get(i)[1];
			guiObjNames[i] = (String)tmpUIObjArray.get(i)[2];
			guiBoolVals[i] = (boolean[])tmpUIObjArray.get(i)[3];
			uiVals[i] = guiStVals[i];
		}
		
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects

		buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff},tmpListObjVals);			//builds a horizontal list of UI comps	
		
	}//setupGUIObjsAras
	/**
	 * instancing class description for category display UI object - if null or length==0 then not shown/used
	 */
	protected abstract String getCategoryUIObjLabel();
	/**
	 * instancing class description for class display UI object - if null or length==0 then not shown/used
	 */
	protected abstract String getClassUIObjLabel();
	/**
	 * pass the list of values for the feature map select display list box, in idx order
	 * @param ftrStrVals : list of values to display for each feature
	 */
	public final void setUI_FeatureListVals(String[] ftrStrVals) {	guiObjs[uiFtrSelectIDX].setListVals(ftrStrVals);	}
	/**
	 * pass the list of values for the category list box, in idx order
	 * @param categoryVals : list of values to display for category select list
	 */
	public final void setUI_CategoryListVals(String[] categoryVals) {	if(getPrivFlags(_categoryCanBeShownIDX)) {	guiObjs[uiCategorySelectIDX].setListVals(categoryVals);	}}
	/**
	 * pass the list of values for the class list box, in idx order
	 * @param classVals : list of values to display for class select list
	 */
	public final void setUI_ClassListVals(String[] classVals) {		if(getPrivFlags(_classCanBeShownIDX)) {		guiObjs[uiClassSelectIDX].setListVals(classVals);	}}
	
	/**
	 * Instancing class-specific (application driven) UI objects should be defined
	 * in this function.  Add an entry to tmpBtnNamesArray for each button, in the order 
	 * they are to be displayed
	 * @param tmpUIObjArray array list of Object arrays, where in each object array : 
	 * 			the first element double array of min/max/mod values
	 * 			the 2nd element is starting value
	 * 			the 3rd elem is label for object
	 * 			the 4th element is boolean array of {treat as int, has list values, value is sent to owning window}
	 * @param tmpListObjVals treemap keyed by object IDX and value is list of strings of values for all UI list select objects
	 */
	protected abstract void setupGUIObjsArasIndiv(ArrayList<Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals);
	
	public final void resetUIVals(){for(int i=0; i<guiStVals.length;++i){				guiObjs[i].setVal(guiStVals[i]);		}}	
		
	//set window-specific variables that are based on current visible screen dimensions
	protected final void setVisScreenDimsPriv() {
		float xStart = rectDim[0] + .5f*(curVisScrDims[0] - (curVisScrDims[1]-(2*xOff)));
		//start x and y and dimensions of full map visualization as function of visible window size;
		mapMgr.setSOM_mapLoc(new float[]{xStart, rectDim[1] + yOff});
		//now build calc analysis offset struct
		setVisScreenDimsPriv_Indiv();
	}//calcAndSetMapLoc
	protected abstract void setVisScreenDimsPriv_Indiv();
	
	@Override
	public final void setPrivFlags(int idx, boolean val){				
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case buildSOMExe 			: {break;}			//placeholder	
			case resetMapDefsIDX		: {if(val){resetUIVals(); setPrivFlags(resetMapDefsIDX,false);}}
			case mapDataLoadedIDX 		: {break;}			//placeholder				
			case mapUseChiSqDistIDX		: {//whether or not to use chi-squared (weighted) distance for features
				//turn off scaled ftrs if this is set
				mapMgr.setUseChiSqDist(val);
				break;}							
			case mapExclProdZeroFtrIDX		: {//whether or not distances between two datapoints assume that absent features in smaller-length datapoints are 0, or to ignore the values in the larger datapoints
				mapMgr.setMapExclZeroFtrs(val);
				break;}							
			case mapDrawTrainDatIDX		: {//draw training examples
				break;}							
			case mapDrawTestDatIDX		: {//draw testing examples
				break;}		
			case mapDrawWtMapNodesIDX		: {//draw map nodes
				if (val) {//turn off other node displays
					setPrivFlags(mapDrawPopMapNodesIDX, false);
					setPrivFlags(mapDrawAllMapNodesIDX, false);					
				}
				break;}							
			case mapDrawPopMapNodesIDX  : {				
				if (val) {//turn off other node displays
					setPrivFlags(mapDrawWtMapNodesIDX, false);
					setPrivFlags(mapDrawAllMapNodesIDX, false);					
				}
				break;}
			case mapDrawAllMapNodesIDX	: {//draw all map nodes, even empty
				if (val) {//turn off other node displays
					setPrivFlags(mapDrawPopMapNodesIDX, false);
					setPrivFlags(mapDrawWtMapNodesIDX, false);					
				}
				break;}	
			case mapDrawFtrWtSegMembersIDX :{
				if(val) {mapMgr.buildFtrWtSegmentsOnMap();}
				break;}
			case mapDrawClassSegmentsIDX		:{			break;}			
			case mapDrawCategorySegmentsIDX		:{			break;}		
			
			case saveAllSegmentMapsIDX : {
				if(val) {
					mapMgr.saveAllSegment_BMUReports();
					setPrivFlags(saveAllSegmentMapsIDX, false);
				}
				break;}			
			case mapDrawNodeLblIDX : {//whether or not to show labels of nodes being displayed				
				break;}
			case mapDrawUMatrixIDX :{//whether to show the UMatrix (distance between nodes) representation of the map - overrides per-ftr display
				break;}
			case mapDrawUMatSegMembersIDX : {//whether to show segment membership for zones of the map, using a color overlay
				if(val) {mapMgr.buildUMatrixSegmentsOnMap();}
				break;}
			case mapDrawUMatSegImgIDX : {
				if(val) {mapMgr.buildUMatrixSegmentsOnMap();}
				break;}
			case showSelRegionIDX		 : {//highlight a specific region of the map, either all nodes above a certain threshold for a chosen class or category
				break;}
			case saveLocClrImgIDX : {break;}		//save image
			case mapLockClassCatSegmentsIDX : {
				setPrivFlags_LockCatForClassSegs(val);
				break;}

			default			: {setPrivFlagsIndiv(idx,val);}
		}
	}//setFlag		
	protected abstract void setPrivFlagsIndiv(int idx, boolean val);
	/**
	 * Instance-specific code for managing locking of category segment selection to enable cycling through class within category
	 * @param val whether the lock button is being turned on or off
	 */
	protected abstract void setPrivFlags_LockCatForClassSegs(boolean val);
	//set flag values when finished building map, to speed up initial display
	public final void setFlagsDoneMapBuild(){
		setPrivFlags(mapDrawTrainDatIDX, false);
		setPrivFlags(mapDrawWtMapNodesIDX, false);
		setPrivFlags(mapDrawAllMapNodesIDX, false);
	}//setFlagsDoneMapBuild
	

	//first verify that new .lrn file exists, then
	//build new SOM_MAP map using UI-entered values, then load resultant data - map nodes, bmus to training data
	protected final void buildNewSOMMap(){
		msgObj.dispMessage("SOM_MapUIWin","buildNewSOMMap","Starting Map Build", MsgCodes.info5);
		setPrivFlags(buildSOMExe,false);
		//send current UI values to map manager, load appropriate data, build directory structure and execute map
		boolean returnCode = mapMgr.loadTrainDataMapConfigAndBuildMap(true);
		//returnCode is whether map was built and trained successfully
		setFlagsDoneMapBuild();
		msgObj.dispMessage("SOM_MapUIWin","buildNewSOMMap","Map Build " + (returnCode ? "Completed Successfully." : "Failed due to error."), MsgCodes.info5);
		
	}//buildNewSOMMap		
	
	//update UI values from passed SOM_MAPDat object's current state
	public final void setUIValues(SOM_MapDat mapDat) {
		HashMap<String, Integer> mapInts = mapDat.getMapInts();
		HashMap<String, Float> mapFloats = mapDat.getMapFloats();
		HashMap<String, String> mapStrings = mapDat.getMapStrings();

		guiObjs[uiMapColsIDX].setVal(mapInts.get("mapCols"));
		guiObjs[uiMapRowsIDX].setVal(mapInts.get("mapRows"));
		guiObjs[uiMapEpochsIDX].setVal(mapInts.get("mapEpochs"));
		guiObjs[uiMapKTypIDX].setVal(mapInts.get("mapKType"));
		guiObjs[uiMapRadStIDX].setVal(mapInts.get("mapStRad"));
		guiObjs[uiMapRadEndIDX].setVal(mapInts.get("mapEndRad"));
		
		guiObjs[uiMapLrnStIDX].setVal(mapFloats.get("mapStLrnRate"));
		guiObjs[uiMapLrnEndIDX].setVal(mapFloats.get("mapEndLrnRate"));
		
		guiObjs[uiMapShapeIDX].setValInList(mapStrings.get("mapGridShape"));	
		guiObjs[uiMapBndsIDX].setValInList(mapStrings.get("mapBounds"));	
		guiObjs[uiMapRadCoolIDX].setValInList(mapStrings.get("mapRadCool"));	
		guiObjs[uiMapNHdFuncIDX].setValInList(mapStrings.get("mapNHood"));	
		guiObjs[uiMapLrnCoolIDX].setValInList(mapStrings.get("mapLearnCool"));
				
	}//setUIValues
	
	/**
	 * set display of prebuilt map directories to use based on loaded info from project config file
	 * @param _pbltMapArray 
	 */
	public final void setPreBuiltMapArray(String[] _pbltMapArray) {
		msgObj.dispInfoMessage("SOM_MapUIWin","setPreBuiltMapArray","Attempting to set prebuilt map values list of size : " +_pbltMapArray.length);
		int curIDX = guiObjs[uiMapPreBuiltDirIDX].setListVals(_pbltMapArray);
		uiVals[uiMapPreBuiltDirIDX] = guiObjs[uiMapPreBuiltDirIDX].getVal();
		mapMgr.setCurPreBuiltMapIDX(curIDX);
	}//

	///////////////////////////////
	// map <--> ui sync functions
	//
	@Override
	public final void setMapDataVal_Integer(int UIidx, double val) {	mapMgr.updateMapDatFromUI_Integer(getMapKeyStringFromUIidx(UIidx), (int) val);}	
	@Override
	public final void setMapDataVal_Float(int UIidx, double val) {	mapMgr.updateMapDatFromUI_Float(getMapKeyStringFromUIidx(UIidx), (float) val);}
	@Override
	//public void setMapDataVal_String(int UIidx, double val) {	mapMgr.updateMapDatFromUI_String(getMapKeyStringFromUIidx(UIidx), getUIListValStr(UIidx, (int)val));}
	public final void setMapDataVal_String(int UIidx, double val) {	mapMgr.updateMapDatFromUI_String(getMapKeyStringFromUIidx(UIidx), guiObjs[UIidx].getListValStr((int)val));}
	
	// set UI vals from map mgr - these are changes resulting from non-UI made changes to map
	@Override
	public final void updateUIDataVal_Integer(String key, Integer val) {
		if(!isMapNameOfType(mapDatNames_Ints, key)) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_Integer","Attempting to set UI object with unknown Key : " +key + " using integer value " + val +". Aborting.",MsgCodes.warning1);	
			return;}
		Integer uiObjIDX = getUIidxFromMapKeyString(key);
		uiVals[uiObjIDX] = guiObjs[uiObjIDX].setVal(val);
		switch (uiObjIDX) {
			//integer values
			case uiMapRowsIDX 	    : {guiObjs[uiMapRadStIDX].setVal(.5*Math.min(val, guiObjs[uiMapColsIDX].getVal()));	break;}
			case uiMapColsIDX	    : {guiObjs[uiMapRadStIDX].setVal(.5*Math.min(guiObjs[uiMapRowsIDX].getVal(), val));break;}
			case uiMapEpochsIDX	    : {break;}
			case uiMapKTypIDX	    : {break;}
			case uiMapRadStIDX	    : {if(val <= guiObjs[uiMapRadEndIDX].getVal()+guiMinMaxModVals[uiObjIDX][2]) {uiVals[uiObjIDX] = guiObjs[uiObjIDX].setVal(guiObjs[uiMapRadEndIDX].getVal()+guiMinMaxModVals[uiObjIDX][2]);}break;}
			case uiMapRadEndIDX	    : {if(val >= guiObjs[uiMapRadStIDX].getVal()-guiMinMaxModVals[uiObjIDX][2]) { uiVals[uiObjIDX] = guiObjs[uiObjIDX].setVal(guiObjs[uiMapRadStIDX].getVal()-guiMinMaxModVals[uiObjIDX][2]);}break;}
		}
	}//setUIDataVal_Integer	

	@Override
	public final void updateUIDataVal_Float(String key, Float val) {
		if(!isMapNameOfType(mapDatNames_Floats, key)) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_Float","Attempting to set UI object with unknown Key : " +key + " using integer value " + val +". Aborting.",MsgCodes.warning1);	
			return;}
		Integer uiObjIDX = getUIidxFromMapKeyString(key);
		uiVals[uiObjIDX] = guiObjs[uiObjIDX].setVal(val);
		switch (uiObjIDX) {
		case uiMapLrnStIDX	    : {	if(val <= guiObjs[uiMapLrnEndIDX].getVal()+guiMinMaxModVals[uiObjIDX][2]) {	uiVals[uiObjIDX] = guiObjs[uiObjIDX].setVal(guiObjs[uiMapLrnEndIDX].getVal()+guiMinMaxModVals[uiObjIDX][2]);}break;}
		case uiMapLrnEndIDX	    : {	if(val >= guiObjs[uiMapLrnStIDX].getVal()-guiMinMaxModVals[uiObjIDX][2]) {	uiVals[uiObjIDX] = guiObjs[uiObjIDX].setVal(guiObjs[uiMapLrnStIDX].getVal()-guiMinMaxModVals[uiObjIDX][2]);}break;}
		}
	}//setUIDataVal_Float
	
	@Override
	public final void updateUIDataVal_String(String key, String val) {
		if(!isMapNameOfType(mapDatNames_Strings, key)) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_String","Attempting to set UI object with unknown Key : " +key + " using String value " + val +". Aborting.",MsgCodes.warning1);
			return;}
		Integer uiObjIDX = getUIidxFromMapKeyString(key);
		int[] retVals = guiObjs[uiObjIDX].setValInList(val);
		//if retVals[1] != 0 then not ok
		if(retVals[1] != 0) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_String","Attempting to set list object : " +key + " to unknown list value " + val +". Aborting.",MsgCodes.warning1);
		} else {
			uiVals[uiObjIDX] = retVals[0];
		}
	}//setUIDataVal_String
	
	///////////////////////////////
	// end map <--> ui sync functions
	//
	
	//this sets the window's uiVals array to match the ui object's internal state - if object is set but this is not called, subordinate code for object (in switch below) will not get run
	@Override
	protected final void setUIWinVals(int UIidx) {
		double val = guiObjs[UIidx].getVal(); 
		if(uiVals[UIidx] != val){uiVals[UIidx] = val;} else {return;}//set values in raw array and only proceed if values have changed
		//int intVal = (int)val;
		switch(UIidx){
			//integer values
			case uiMapRowsIDX 	    : {setMapDataVal_Integer(UIidx,val);guiObjs[uiMapRadStIDX].setVal(.5*MyMathUtils.min(val, guiObjs[uiMapColsIDX].getVal()));	break;}	//also set rad start to have a value == to 1/2 the max of rows or columns
			case uiMapColsIDX	    : {setMapDataVal_Integer(UIidx,val);guiObjs[uiMapRadStIDX].setVal(.5*MyMathUtils.min(guiObjs[uiMapRowsIDX].getVal(), val));break;}
			case uiMapEpochsIDX	    : {setMapDataVal_Integer(UIidx,val);break;}
			case uiMapKTypIDX	    : {
				setMapDataVal_Integer(UIidx,val);break;}
			case uiMapRadStIDX	    : {
				if(val <= guiObjs[uiMapRadEndIDX].getVal()+guiMinMaxModVals[UIidx][2]) {val = guiObjs[UIidx].setVal(guiObjs[uiMapRadEndIDX].getVal()+guiMinMaxModVals[UIidx][2]);uiVals[UIidx] = val;}
				setMapDataVal_Integer(UIidx,val);		break;}
			case uiMapRadEndIDX	    : {
				if(val >= guiObjs[uiMapRadStIDX].getVal()-guiMinMaxModVals[UIidx][2]) { val = guiObjs[UIidx].setVal(guiObjs[uiMapRadStIDX].getVal()-guiMinMaxModVals[UIidx][2]);uiVals[UIidx] = val;}
				setMapDataVal_Integer(UIidx,val);		break;}
			//end of integer values |start of float values
			case uiMapLrnStIDX	    : {
				if(val <= guiObjs[uiMapLrnEndIDX].getVal()+guiMinMaxModVals[UIidx][2]) {val = guiObjs[UIidx].setVal(guiObjs[uiMapLrnEndIDX].getVal()+guiMinMaxModVals[UIidx][2]);uiVals[UIidx] = val;}			
				setMapDataVal_Float(UIidx,val);			break;}
			case uiMapLrnEndIDX	    : {
				if(val >= guiObjs[uiMapLrnStIDX].getVal()-guiMinMaxModVals[UIidx][2]) {	val = guiObjs[UIidx].setVal(guiObjs[uiMapLrnStIDX].getVal()-guiMinMaxModVals[UIidx][2]);uiVals[UIidx] = val;}		
				setMapDataVal_Float(UIidx,val);			break;}
			//end of float values | start of string/list values
			case uiMapNHdFuncIDX	: {setMapDataVal_String(UIidx,val); break;}
			case uiMapRadCoolIDX	: {setMapDataVal_String(UIidx,val); break;}
			case uiMapLrnCoolIDX	: {setMapDataVal_String(UIidx,val); break;}
			case uiMapShapeIDX	    : {setMapDataVal_String(UIidx,val); break;}
			case uiMapBndsIDX	    : {setMapDataVal_String(UIidx,val); break;}
			//end map arg-related string/list values
			
			case uiTrainDataFrmtIDX 		: {//format of training data
				mapMgr.setCurrentTrainDataFormat(SOM_FtrDataType.getVal((int)(this.guiObjs[uiTrainDataFrmtIDX].getVal())));
				break;}
//			case uiTestDataFrmtIDX 			: {
//				mapMgr.setCurrentTestDataFormat(SOM_FtrDataType.getVal((int)(this.guiObjs[uiTestDataFrmtIDX].getVal())));
//				break;}
			case uiTrainDatPartIDX 			: {break;}
			case uiNodeWtDispThreshIDX : {
				float _mapNodeWtDispThresh = (float)(this.guiObjs[uiNodeWtDispThreshIDX].getVal());
				mapMgr.setMapNodeWtDispThresh(_mapNodeWtDispThresh);
				mapMgr.setNodeInFtrWtSegThresh(_mapNodeWtDispThresh);				
				break;}
			case  uiPopMapNodeDispSizeIDX:{
				float _mapNodePopDispThresh = (float)(this.guiObjs[uiPopMapNodeDispSizeIDX].getVal());
				mapMgr.setMapNodePopDispThresh(_mapNodePopDispThresh);
				break;}
			case uiNodeInSegThreshIDX 		:{		//used to determine threshold of value for setting membership in a segment/cluster
				mapMgr.setNodeInUMatrixSegThresh((float)(this.guiObjs[uiNodeInSegThreshIDX].getVal()));
				mapMgr.buildUMatrixSegmentsOnMap();
				break;}			
			case uiMapPreBuiltDirIDX 		: {//what prebuilt map of list of maps shown to right of screen to use, if any are defined in project config
				mapMgr.setCurPreBuiltMapIDX((int) (this.guiObjs[uiMapPreBuiltDirIDX].getVal()));
				break;}			
			case uiMapNodeBMUTypeToDispIDX 	: {//type of examples being mapped to each map node to display
				mapMgr.setMapNodeDispType(SOM_ExDataType.getVal((int)(this.guiObjs[uiMapNodeBMUTypeToDispIDX].getVal())));
				break;}			
			case uiMseRegionSensIDX 		: {			break;}
			case uiFtrSelectIDX				: {		//feature idx map to display
				mapMgr.setCurMapImgIDX((int)guiObjs[uiFtrSelectIDX].getVal());	
				break;}
			case uiCategorySelectIDX : {	//category select changed - managed by instancing app		
				setCategory_UIObj(settingCategoryFromClass); 
				break;}
			case uiClassSelectIDX : {			
				setClass_UIObj(settingClassFromCategory);  
				break;}
			default : {setUIWinValsIndiv(UIidx);}
		}
	}//setUIWinVals
	
	/**
	 * feature type used to train currently loaded map
	 * @param _ftrToTrain
	 */
	public void setFtrTrainTypeFromConfig(int _ftrTypeUsedToTrain) {
		this.guiObjs[uiTrainDataFrmtIDX].setVal(_ftrTypeUsedToTrain);
		uiVals[uiTrainDataFrmtIDX] = this.guiObjs[uiTrainDataFrmtIDX].getVal();
	}
	
	protected void setCategory_UIObj(boolean settingCategoryFromClass) {
		mapMgr.setCurCategoryIDX((int)(this.guiObjs[uiCategorySelectIDX].getVal()));
		mapMgr.setCurCategoryLabel(getCategoryLabelFromIDX(mapMgr.getCurCategoryIDX()));				
		setUIWinVals_HandleCategory(settingCategoryFromClass); 
	}

	
	protected void setClass_UIObj(boolean settingClassFromCategory) {
		mapMgr.setCurClassIDX((int)(this.guiObjs[uiClassSelectIDX].getVal()));
		mapMgr.setCurClassLabel(getClassLabelFromIDX(mapMgr.getCurClassIDX()));				
		setUIWinVals_HandleClass(settingClassFromCategory);  		
	}
	
	private boolean settingCategoryFromClass = false, settingClassFromCategory = false;
	
	/**
	 * Called when class display select value is changed in ui
	 */
	protected final void setUIWinVals_HandleClass(boolean settingClassFromCategory) {
		if(!settingClassFromCategory) {		//don't want to change category again if setting from category ui obj change - loop potential
			int curJPGIdxVal = (int)guiObjs[uiCategorySelectIDX].getVal();
			int jpgIdxToSet = getCategoryFromClass(curJPGIdxVal,(int)guiObjs[uiClassSelectIDX].getVal());
			if(curJPGIdxVal != jpgIdxToSet) {
				settingCategoryFromClass = true;
				guiObjs[uiCategorySelectIDX].setVal(jpgIdxToSet);
				setUIWinVals(uiCategorySelectIDX);
				uiVals[uiCategorySelectIDX] = guiObjs[uiCategorySelectIDX].getVal();
				settingCategoryFromClass = false;
			}
		}		
	}//setJPGroupIDXFromJp
	/**
	 * Called when category display select value is changed in ui
	 */
	protected final void setUIWinVals_HandleCategory(boolean settingCategoryFromClass) {
		//msgObj.dispInfoMessage("SOM WIN","setUIWinVals::uiJPGToDispIDX", "Click : settingJPGFromJp : " + settingJPGFromJp);
		if(!settingCategoryFromClass) {
			int curClassIdxVal = (int)guiObjs[uiClassSelectIDX].getVal();
			int classIdxToSet = getClassFromCategory((int)guiObjs[uiCategorySelectIDX].getVal(), curClassIdxVal);
			if(curClassIdxVal != classIdxToSet) {
				//msgObj.dispMessage("SOM WIN","setUIWinVals:uiJPGToDispIDX", "Attempt to modify uiJPToDispIDX : curJPIdxVal : "  +curJPIdxVal + " | jpToSet : " + jpIdxToSet, MsgCodes.info1);
				settingClassFromCategory = true;
				guiObjs[uiClassSelectIDX].setVal(classIdxToSet);	
				setUIWinVals(uiClassSelectIDX);
				uiVals[uiClassSelectIDX] =guiObjs[uiClassSelectIDX].getVal();
				settingClassFromCategory = false;
			}
		}
	}//setUIWinVals_HandleCategory

	/**
	 * return instance-specific catgory idx for passed class - should return current cat idx if appropriates
	 * called when class changes
	 * @param _curCatIDX
	 * @param _classIDX new class idx
	 * @return
	 */
	protected abstract int getCategoryFromClass(int _curCatIDX, int _classIDX);
	/**
	 * return instance-specific class idx for passed category - should return current class idx if appropriate
	 * called when category changes
	 * @param _catIDX new category idx
	 * @param _curClassIDX
	 * @return appropriate category idx for current class 
	 */
	protected abstract int getClassFromCategory(int _catIDX, int _curClassIDX) ;
	/**
	 * For instance-class specific ui values
	 * @param UIidx
	 */
	protected abstract void setUIWinValsIndiv(int UIidx);
	
	/**
	 * return class label from index - will be instance specific
	 * @param _idx idx from class list box to get class label (used as key in map holding class data in map manager)
	 * @return
	 */
	protected abstract int getClassLabelFromIDX(int _idx);
	
	
	/**
	 * return category label from index - will be instance specific
	 * @param _idx idx from category list box to get category label (used as key in map holding category data in map manager)
	 * @return
	 */
	protected abstract int getCategoryLabelFromIDX(int _idx);

	
	
	public final float getTrainTestDatPartition() {	return (float)(.01*this.guiObjs[uiTrainDatPartIDX].getVal());}	
	
	
	/**
	 * return map dims based on the size of this window
	 */
	public final float[] getWinUIMapDims() {
		float width = this.rectDim[3] * SOMDimScaleFact;
		return new float[] {width,width};
	}
	/**
	 * set the % of the height of the window that the som display should take up
	 * @param _sclFact
	 */
	public final void setSOMDimScaleFact(float _sclFact) {SOMDimScaleFact=_sclFact;}
	/////////////////////////////////////////
	// draw routines
	private boolean doBuildMap = false;
	@Override
	protected final void drawMe(float animTimeMod) {
		drawSetDispFlags();
		if(getPrivFlags(mapDataLoadedIDX) != mapMgr.isMapDrawable()){setPrivFlags(mapDataLoadedIDX,mapMgr.isMapDrawable());}
		boolean isMapDataLoaded = getPrivFlags(mapDataLoadedIDX);
		drawMap(isMapDataLoaded);		
		if(doBuildMap){buildNewSOMMap(); doBuildMap = false;} 
		else if(getPrivFlags(buildSOMExe)) {	doBuildMap = true;	}	
	}
	protected abstract void drawSetDispFlags();
	
	private void drawMap(boolean mapDataLoaded){		
		//draw map rectangle
		pa.pushMatrix();pa.pushStyle();
		//instance-specific drawing
		drawMapIndiv();
		if(mapDataLoaded){mapMgr.drawMapRectangle(pa);}	
		pa.popStyle();pa.popMatrix();
	}//drawMap()	
	protected abstract void drawMapIndiv();
	
	public final void drawMseOverData() {	mapMgr.drawMseOverData(pa);}

	/////////////////////////////////////////
	// end draw routines

	
	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {
		msgObj.dispMessage("Straff_SOMMapUIWin","handleSideMenuMseOvrDispSel","Click Mouse display in "+name+" : btn : " + btn, MsgCodes.info4);
		SOM_MseOvrDispTypeVals uiMseDispData = handleSideMenuMseOvrDisp_MapBtnToType(btn, val);
		//msgObj.dispInfoMessage("Straff_SOMMapUIWin","handleSideMenuMseOvrDispSel","Mouse Display Function : " + btn +" selected == "+ uiMseDispData.toString());
		if(uiMseDispData == SOM_MseOvrDispTypeVals.mseOvrOtherIDX) {	handleSideMenuMseOvrDispSel_Indiv(btn,val);	}
		mapMgr.setUiMseDispData(uiMseDispData);
		msgObj.dispMessage("Straff_SOMMapUIWin","handleSideMenuMseOvrDispSel","Done Click Mouse display in "+name+" : btn : " + btn + " | Dist Type : " + uiMseDispData.toString(), MsgCodes.info4);
	}
	
	/**
	 * determine how buttons map to mouse over display types
	 * @param btn 
	 * @return
	 */
	protected abstract SOM_MseOvrDispTypeVals handleSideMenuMseOvrDisp_MapBtnToType(int btn, boolean val);
	
	/**
	 * custom mouse button handling
	 * @param btn
	 * @param val
	 */
	protected final void handleSideMenuMseOvrDispSel_Indiv(int btn, boolean val) {
		if(val) {		mapMgr.setCustUIMseDispData(btn);} else {	mapMgr.setCustUIMseDispData(-1);}	
	}

	
	//given pixel location relative to upper left corner of map, return map node float - this measures actual distance in map node coords
	//so rounding to ints give map node tuple coords, while float gives interp between neighbors
	//protected final float[] getMapNodeLocFromPxlLoc(float mapPxlX, float mapPxlY, float sclVal){	return new float[]{(sclVal* mapPxlX * mapMgr.getNodePerPxlCol()) - .5f, (sclVal* mapPxlY * mapMgr.getNodePerPxlRow()) - .5f};}	
	/**
	 * check whether the mouse is over a legitimate map location
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	protected final boolean checkMouseOvr(int mouseX, int mouseY){	
		return mapMgr.checkMouseOvr(mouseX, mouseY, (float) guiObjs[uiMseRegionSensIDX].getVal());
	}//chkMouseOvr
	

	/**
	 * check mouse over/click in experiment; if btn == -1 then mouse over
	 * @param msx
	 * @param msy
	 * @param mseClckInWorld
	 * @param btn
	 * @return
	 */
	public final boolean checkMouseClick(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		return mapMgr.checkMouseClick(mouseX, mouseY, mseClckInWorld, mseBtn);
	};
	/**
	 * check mouse drag/move in experiment; if btn == -1 then mouse over
	 * @param msx
	 * @param msy
	 * @param mseClckInWorld
	 * @param btn
	 * @return  
	 */
	public final boolean checkMouseDragMove(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		return mapMgr.checkMouseDragMove(mouseX, mouseY,pmouseX, pmouseY, mouseClickIn3D, mseDragInWorld, mseBtn);
	};
	
	public final void checkMouseRelease() {mapMgr.checkMouseRelease();};
	
	//return strings for directory names and for individual file names that describe the data being saved.  used for screenshots, and potentially other file saving
	//first index is directory suffix - should have identifying tags based on major/archtypical component of sim run
	//2nd index is file name, should have parameters encoded
	@Override
	protected String[] getSaveFileDirNamesPriv() {
		String dirString="", fileString ="";
		//for(int i=0;i<uiAbbrevList.length;++i) {fileString += uiAbbrevList[i]+"_"+ (uiVals[i] > 1 ? ((int)uiVals[i]) : uiVals[i] < .0001 ? String.format("%6.3e", uiVals[i]) : String.format("%3.3f", uiVals[i]))+"_";}
		return new String[]{dirString,fileString};	
	}
	@Override
	public void hndlFileLoad(File file, String[] vals, int[] stIdx) {
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileSave 
		//hndlFileLoad_GUI(vals, stIdx);
	}
	@Override
	public ArrayList<String> hndlFileSave(File file) {
		ArrayList<String> res = new ArrayList<String>();
		//if wanting to load/save UI values, uncomment this call and similar in hndlFileLoad 
		//res = hndlFileSave_GUI();
		return res;
	}
	@Override
	protected final myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,0);}

	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc){}//not a snap-to window
	@Override
	protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){		}
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
	@Override
	protected void addSScrToWinIndiv(int newWinKey){}
	@Override
	protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWinIndiv(int idx) {}	
	@Override
	protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	//resize drawn all trajectories
	@Override
	protected void resizeMe(float scale) {}
	@Override
	protected void closeMe() {}
	@Override
	protected void showMe() {
		//pa.setMenuDbgBtnNames(menuDbgBtnNames);	
		setCustMenuBtnNames();
	}

}//SOMMapUIWin
