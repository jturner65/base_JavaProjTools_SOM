package base_SOM_Objects.som_ui.win_disp_ui;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_ui.utils.SOMWinUIDataUpdater;
import base_SOM_Objects.som_utils.SOM_MapDat;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiObjs.GUIObj_List;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Type;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * base UI window functionality to be used for any SOM-based projects
 * @author john
 *
 */
public abstract class SOM_MapUIWin extends Base_DispWindow implements ISOM_UIWinMapDat{
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
		mapDrawNodesWith0MapExIDX	= 8,			//draw nodes that have no mapped examples
		mapDrawWtMapNodesIDX		= 9,			//draw map nodes with non-0 (present) ftr vals for currently selected ftr
		mapDrawPopMapNodesIDX	   	= 10,			//draw map nodes that are bmus for training examples, with size logarithmically proportional to pop size
		mapDrawAllMapNodesIDX		= 11,			//draw all map nodes, even empty
		drawMapNodePopGraphIDX		= 12,			//draw graph next to SOM Map display showing map node population curve, with x axis being proportional to population, and y axis holding each node
		
		//UMatrix 		
		mapDrawUMatrixIDX			= 13,			//draw visualization of u matrix - distance between nodes
		mapDrawUMatSegImgIDX		= 14,			//draw the image of the interpolated segments based on UMatrix Distance
		mapDrawUMatSegMembersIDX	= 15,			//draw umatrix-based segments around regions of maps - visualizes clusters with different colors
		
		//ftr and ftr-dist-based
		mapDrawDistImageIDX			= 16,			//draw umatrix-like rendering based on sq dist between adjacent node vectors
		mapDrawFtrWtSegMembersIDX	= 17,			//draw ftr-wt-based segments around regions of map - display only segment built from currently display ftr on ftr map
		
		//class and category-based segments
		mapDrawClassSegmentsIDX		= 18,			//show class segments
		mapDrawCategorySegmentsIDX	= 19,			//show category (collection of classes) segments
		_categoryCanBeShownIDX		= 20,			//whether category values are used and can be shown on UI/interracted with
		_classCanBeShownIDX			= 21,			//whether class values are used and can be shown on UI/interracted with
		mapLockClassCatSegmentsIDX  = 22,			//lock category to cycle through classes
		
		showSelRegionIDX			= 23,			//highlight a specific region of the map, i.e. all nodes above a certain threshold for a chosen ftr
		//train/test data management
		somTrainDataLoadedIDX		= 24,			//whether data used to build map has been loaded yet
		saveLocClrImgIDX			= 25,			//
		//save segment mappings
		saveAllSegmentMapsIDX		= 26;			//this will save all the segment mappings that have been defined
	
	public static final int numSOMBasePrivFlags = 27;
	//instancing class will determine numPrivFlags based on how many more flags are added
	
	/**
	 * # of priv flags from base class and instancing class
	 */
	//private int numPrivFlags;
	
	//	//GUI Objects	
	public final static int 
		uiTrainDataNormIDX			= 0,			//normalization that feature data should have: unnormalized, norm per feature across all data, normalize each example
		uiBMU_DispDataFrmtIDX		= 1,			//format of vectors to use when comparing examples to nodes on map
		uiTrainDatPartIDX			= 2,			//partition % of training data out of total data (rest is testing)
		
		uiMapRowsIDX 				= 3,            //map rows
		uiMapColsIDX				= 4,			//map cols
		uiMapEpochsIDX				= 5,			//# of training epochs
		uiMapShapeIDX				= 6,			//hexagonal or rectangular
		uiMapBndsIDX				= 7,			//planar or torroidal bounds
		uiMapKTypIDX				= 8,			//0 : dense cpu, 1 : dense gpu, 2 : sparse cpu.  dense needs appropriate lrn file format
		uiMapNHdFuncIDX				= 9,			//neighborhood : 0 : gaussian, 1 : bubble
		uiMapRadCoolIDX				= 10,			//radius cooling 0 : linear, 1 : exponential
		uiMapLrnCoolIDX				= 11,			//learning rate cooling 0 : linear 1 : exponential
		uiMapLrnStIDX				= 12,			//start learning rate
		uiMapLrnEndIDX				= 13,			//end learning rate
		uiMapRadStIDX				= 14,			//start radius
		uiMapRadEndIDX				= 15,			//end radius
		uiMapPreBuiltDirIDX			= 16,			//list of prebuilt maps as defined in config - this specifies which prebuilt map to use
		uiMapNodeBMUTypeToDispIDX 	= 17,			//type of examples mapping to a particular node to display in visualization
		
		uiNodeWtDispThreshIDX 		= 18,			//threshold for display of map nodes on individual weight maps
		uiNodePopDispThreshIDX		= 19,			//only display populated map nodes that are this size or larger (log of population determines size)		
		uiNodeInSegThreshIDX		= 20,			//threshold of u-matrix weight for nodes to belong to same segment
		
		uiMseRegionSensIDX			= 21,			//senstivity threshold for mouse-over
		uiFtrSelectIDX				= 22,			//pick the feature to display, if ftr-idx wt graphs are being displayed
		uiCategorySelectIDX			= 23,			//pick the category to display, if category mapping is available/enabled
		uiClassSelectIDX			= 24;			//pick the class to display, if class mapping is available/enabled
	
	public static final int numSOMBaseGUIObjs = 25;
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
	 */	
	public SOM_MapUIWin(IRenderInterface _p, GUI_AppManager _AppMgr, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt) {
		super(_p, _AppMgr, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
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
	 * @param mapKey
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
		//only set for visualization
		mapMgr.setDispWinData(this);
		
		//this window uses right side info window
		setFlags(drawRightSideMenu, true);		//may need some re-scaling to keep things in the middle and visible
		
		//init specific sim flags
		//initPrivFlags(numPrivFlags);
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
		mapMgr.initFromUIWinInitMe(
				(int)(guiObjs[uiTrainDataNormIDX].getVal()), 
				(int)(guiObjs[uiBMU_DispDataFrmtIDX].getVal()),
				(float)(guiObjs[uiNodeWtDispThreshIDX].getVal()),
				(float)(guiObjs[uiNodePopDispThreshIDX].getVal()), 
				(int)(guiObjs[uiMapNodeBMUTypeToDispIDX].getVal()));

		initMeIndiv();
	}
	
	@Override
	protected int[] getFlagIDXsToInitToTrue() {		return null;}

	/**
	 * This function would provide an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.
	 */
	@Override
	protected UIDataUpdater buildUIDataUpdateObject() {
		return new SOMWinUIDataUpdater(this);
	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	protected final void updateCalcObjUIVals() {}
	
	protected abstract void setInitValsForPrivFlags_Indiv();
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
	
	protected abstract void initMeIndiv();	
	
	@Override
	/**
	 * initialize all private-flag based UI buttons here - called by base class before initMe
	 */
	public final int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray){	
		//add an entry for each button, in the order they are wished to be displayed		
		tmpBtnNamesArray.add(new Object[]{"Building SOM","Build SOM ",buildSOMExe});
		tmpBtnNamesArray.add(new Object[]{"Reset Dflt UI Vals","Reset Dflt UI Vals",resetMapDefsIDX});
		tmpBtnNamesArray.add(new Object[]{"Using ChiSq for Ftr Dist", "Not Using ChiSq Distance", mapUseChiSqDistIDX});       
		tmpBtnNamesArray.add(new Object[]{"Prdct Dist ignores 0-ftrs","Prdct Dist w/all ftrs", mapExclProdZeroFtrIDX});    
		tmpBtnNamesArray.add(new Object[]{"Showing UMat (Bi-Cubic)", "Showing Ftr Map", mapDrawUMatrixIDX});        
		tmpBtnNamesArray.add(new Object[]{"Hide Nodes", "Show Nodes", mapDrawAllMapNodesIDX});    
		tmpBtnNamesArray.add(new Object[]{"Hide Node Lbls", "Show Node Lbls", mapDrawNodeLblIDX});      
		tmpBtnNamesArray.add(new Object[]{"Hide Nodes (by Pop)", "Show Nodes (by Pop)", mapDrawPopMapNodesIDX});    
		tmpBtnNamesArray.add(new Object[]{"Hide Nodes w/o mapped Ex","Show Nodes w/o mapped Ex",mapDrawNodesWith0MapExIDX});
		tmpBtnNamesArray.add(new Object[]{"Hide Hot Ftr Nodes (by Wt)", "Show Hot Ftr Nodes (by Wt)", mapDrawWtMapNodesIDX});     
		tmpBtnNamesArray.add(new Object[]{"Hide Node Pop Graph", "Show Node Pop Graph", drawMapNodePopGraphIDX});
		tmpBtnNamesArray.add(new Object[]{"Hide Train Data", "Show Train Data", mapDrawTrainDatIDX});       
		tmpBtnNamesArray.add(new Object[]{"Hide Test Data", "Show Test Data", mapDrawTestDatIDX});        
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
		return initAllSOMPrivBtns_Indiv(tmpBtnNamesArray);
		
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

	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	{value is sent to owning window, 
	 *           	value is sent on any modifications (while being modified, not just on release), 
	 *           	changes to value must be explicitly sent to consumer (are not automatically sent)}    
	 * @param tmpListObjVals : map of list object possible selection values
	 */
	@Override
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){		
		//keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
		tmpListObjVals.put(uiMapShapeIDX, new String[] {"rectangular","hexagonal"});
		tmpListObjVals.put(uiMapBndsIDX, new String[] {"planar","toroid"});
		tmpListObjVals.put(uiMapKTypIDX, new String[] {"Dense CPU", "Dense GPU", "Sparse CPU"});		
		tmpListObjVals.put(uiMapNHdFuncIDX, new String[]{"gaussian","bubble"});		
		tmpListObjVals.put(uiMapRadCoolIDX, new String[]{"linear","exponential"});
		tmpListObjVals.put(uiMapLrnCoolIDX, new String[]{"linear","exponential"});		
		tmpListObjVals.put(uiTrainDataNormIDX, SOM_FtrDataType.getListOfTypes());
		tmpListObjVals.put(uiBMU_DispDataFrmtIDX, SOM_FtrDataType.getListOfTypes());		
		tmpListObjVals.put(uiMapPreBuiltDirIDX, new String[] {"None"});
		tmpListObjVals.put(uiMapNodeBMUTypeToDispIDX, SOM_MapManager.getNodeBMUMapTypes());
		tmpListObjVals.put(uiFtrSelectIDX, new String[] {"None"});		
			
		tmpUIObjArray.put(uiTrainDataNormIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiTrainDataNormIDX).length-1, 1.0}, 1.0*SOM_FtrDataType.FTR_NORM.getVal(), "Data Normalizing", GUIObj_Type.ListVal, new boolean[]{true}});   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(uiBMU_DispDataFrmtIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiBMU_DispDataFrmtIDX).length-1, 1.0}, 1.0*SOM_FtrDataType.UNNORMALIZED.getVal(), "Map Node Ftr Disp Frmt", GUIObj_Type.ListVal, new boolean[]{true}});  				//uiBMU_DispDataFrmtIDX                                                                         
		tmpUIObjArray.put(uiTrainDatPartIDX, new Object[] {new double[]{1.0, 100.0, 1.0}, 100.0,	"Data % To Train", GUIObj_Type.IntVal, new boolean[]{true}});   													//uiTrainDatPartIDX                                                                         
		tmpUIObjArray.put(uiMapRowsIDX, new Object[] {new double[]{1.0, 120.0, 10}, 10.0, "# Map Rows", GUIObj_Type.IntVal, new boolean[]{true}});   															//uiMapRowsIDX 	 		                                                                    
		tmpUIObjArray.put(uiMapColsIDX, new Object[] {new double[]{1.0, 120.0, 10}, 10.0, "# Map Columns", GUIObj_Type.IntVal, new boolean[]{true}});   															//uiMapColsIDX	 		                                                                    
		tmpUIObjArray.put(uiMapEpochsIDX, new Object[] {new double[]{1.0, 200.0, 10}, 10.0, "# Training Epochs", GUIObj_Type.IntVal, new boolean[]{true}});  														//uiMapEpochsIDX		                                                                    
		tmpUIObjArray.put(uiMapShapeIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapShapeIDX).length-1, 1},0.0, "Map Node Shape", GUIObj_Type.ListVal, new boolean[]{true}});   						//uiMapShapeIDX	 		                                                                    
		tmpUIObjArray.put(uiMapBndsIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapBndsIDX).length-1, 1},1.0, "Map Boundaries", GUIObj_Type.ListVal, new boolean[]{true}});  						//uiMapBndsIDX	 		                                                                    
		tmpUIObjArray.put(uiMapKTypIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapKTypIDX).length-1, 1.01},2.0, "Dense/Sparse (C/G)PU", GUIObj_Type.ListVal, new boolean[]{true}});   				//uiMapKTypIDX	 		                                                                    
		tmpUIObjArray.put(uiMapNHdFuncIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapNHdFuncIDX).length-1, 1},0.0, "Neighborhood Func", GUIObj_Type.ListVal, new boolean[]{true}});   					//uiMapNHdFuncIDX		                                                                    
		tmpUIObjArray.put(uiMapRadCoolIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapRadCoolIDX).length-1, 1},0.0, "Radius Cooling", GUIObj_Type.ListVal, new boolean[]{true}});   						//uiMapRadCoolIDX		                                                                    
		tmpUIObjArray.put(uiMapLrnCoolIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapLrnCoolIDX).length-1, 1},0.0, "Learn rate Cooling", GUIObj_Type.ListVal, new boolean[]{true}});   					//uiMapLrnCoolIDX		                                                                    
		tmpUIObjArray.put(uiMapLrnStIDX, new Object[] {new double[]{0.001, 10.0, 0.001}, 1.0, "Start Learn Rate", GUIObj_Type.FloatVal, new boolean[]{true}});   													//uiMapLrnStIDX	 		                                                                    
		tmpUIObjArray.put(uiMapLrnEndIDX, new Object[] {new double[]{0.001, 1.0, 0.001}, 0.1, "End Learn Rate", GUIObj_Type.FloatVal, new boolean[]{true}});   														//uiMapLrnEndIDX		                                                                    
		tmpUIObjArray.put(uiMapRadStIDX, new Object[] {new double[]{2.0, 300.0, 1.0},	 20.0, "Start Cool Radius", GUIObj_Type.IntVal, new boolean[]{true}});   													//uiMapRadStIDX	 	# nodes	                                                                
		tmpUIObjArray.put(uiMapRadEndIDX, new Object[] {new double[]{1.0, 10.0, 1.0},	 1.0, "End Cool Radius", GUIObj_Type.IntVal, new boolean[]{true}});   														//uiMapRadEndIDX		# nodes	  
		tmpUIObjArray.put(uiMapPreBuiltDirIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapPreBuiltDirIDX).length-1,1.0}, 0.0, "Pretrained Map Dirs", GUIObj_Type.ListVal, new boolean[]{true}});			//uiMapPreBuiltDirIDX
		tmpUIObjArray.put(uiMapNodeBMUTypeToDispIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiMapNodeBMUTypeToDispIDX).length-1, 1.0}, 0.0, "Ex Type For Node BMU", GUIObj_Type.ListVal, new boolean[]{true}}); 	//uiMapNodeBMUTypeToDispIDX                                                                 
		tmpUIObjArray.put(uiNodeWtDispThreshIDX, new Object[] {new double[]{0.0, 1.0, .01}, SOM_MapManager.initNodeInSegFtrWtDistThresh, "Map Node Disp Wt Thresh", GUIObj_Type.FloatVal, new boolean[]{true}});   	//uiNodeWtDispThreshIDX                                                                     
		tmpUIObjArray.put(uiNodePopDispThreshIDX, new Object[] {new double[]{0.0, 1.0, .001},  0.0, "Node Disp Pop Size Thresh %", GUIObj_Type.FloatVal, new boolean[]{true}});														//uiPopMapNodeDispSizeIDX
		tmpUIObjArray.put(uiNodeInSegThreshIDX, new Object[] {new double[]{0.0, 1.0, .001}, SOM_MapManager.initNodeInSegUMatrixDistThresh, "Segment UDist Thresh", GUIObj_Type.FloatVal, new boolean[]{true}});   	//uiNodeInSegThreshIDX//threshold of u-matrix weight for nodes to belong to same segment    
		tmpUIObjArray.put(uiMseRegionSensIDX, new Object[] {new double[]{0.0, 1.0, .001}, 0.001, "Mouse Over Sens", GUIObj_Type.FloatVal, new boolean[]{true}});   														//uiMseRegionSensIDX                                                                        
		tmpUIObjArray.put(uiFtrSelectIDX, new Object[] {new double[]{0.0, tmpListObjVals.get(uiFtrSelectIDX).length-1,1.0}, 0.0, "Feature IDX To Show", GUIObj_Type.ListVal, new boolean[]{true}});					//uiFtrSelectIDX
		
		String catUIDesc = getCategoryUIObjLabel();
		if((null!=catUIDesc) && (catUIDesc.length()>0)) {
			_catExistsAndIsShown = true;
			tmpListObjVals.put(uiCategorySelectIDX, new String[] {"None"});	
			tmpUIObjArray.put(uiCategorySelectIDX,new Object[] {new double[]{0.0, tmpListObjVals.get(uiCategorySelectIDX).length-1,1.0}, 0.0, catUIDesc, GUIObj_Type.ListVal, new boolean[]{true}});			//uiMapPreBuiltDirIDX
		} else {			_catExistsAndIsShown = false;		}
		
		String classUIDesc = getClassUIObjLabel();
		if((null!=classUIDesc) && (classUIDesc.length()>0)) {
			_classExistsAndIsShown = true;
			tmpListObjVals.put(uiClassSelectIDX, new String[] {"None"});	
			tmpUIObjArray.put(uiClassSelectIDX,new Object[] {new double[]{0.0, tmpListObjVals.get(uiClassSelectIDX).length-1,1.0}, 0.0, classUIDesc, GUIObj_Type.ListVal, new boolean[]{true}});			//uiMapPreBuiltDirIDX
		} else {			_classExistsAndIsShown = false;		}
		
		//populate instancing application objects
		setupGUIObjsAras_Indiv(tmpUIObjArray,tmpListObjVals);
		
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
	public final void setUI_FeatureListVals(String[] ftrStrVals) {	((GUIObj_List) guiObjs[uiFtrSelectIDX]).setListVals(ftrStrVals);	}
	/**
	 * pass the list of values for the category list box, in idx order
	 * @param categoryVals : list of values to display for category select list
	 */
	public final void setUI_CategoryListVals(String[] categoryVals) {	if(getPrivFlags(_categoryCanBeShownIDX)) {	((GUIObj_List) guiObjs[uiCategorySelectIDX]).setListVals(categoryVals);	}}
	/**
	 * pass the list of values for the class list box, in idx order
	 * @param classVals : list of values to display for class select list
	 */
	public final void setUI_ClassListVals(String[] classVals) {		if(getPrivFlags(_classCanBeShownIDX)) {		((GUIObj_List) guiObjs[uiClassSelectIDX]).setListVals(classVals);	}}
	
	/**
	 * Instancing class-specific (application driven) UI objects should be defined
	 * in this function.  Add an entry to tmpBtnNamesArray for each button, in the order 
	 * they are to be displayed
	 * @param tmpUIObjArray map keyed by uiIDX of object, value is list of Object arrays, where in each object array : 
	 * 			the first element double array of min/max/mod values
	 * 			the 2nd element is starting value
	 * 			the 3rd elem is label for object
	 * 			the 4th element is boolean array of {treat as int, has list values, value is sent to owning window}
	 * @param tmpListObjVals treemap keyed by object IDX and value is list of strings of values for all UI list select objects
	 */
	protected abstract void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals);
		
	//set window-specific variables that are based on current visible screen dimensions
	protected final void setVisScreenDimsPriv() {
		float xStart = rectDim[0] + .5f*(curVisScrDims[0] - (curVisScrDims[1]-(2*xOff)));
		//start x and y and dimensions of full map visualization as function of visible window size;
		mapMgr.setSOM_mapLoc(new float[]{xStart, rectDim[1] + yOff});
		//handle application-specific functionality
		setVisScreenDimsPriv_Indiv();
	}//calcAndSetMapLoc
	protected abstract void setVisScreenDimsPriv_Indiv();
	
	@Override
	public final void setPrivFlags(int idx, boolean val){				
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case buildSOMExe 			: {break;}			//placeholder	
			case resetMapDefsIDX		: {if(val){resetUIVals(true); setPrivFlags(resetMapDefsIDX,false);}}
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
			case mapDrawNodesWith0MapExIDX : {//hide or draw map nodes that have no mapped examples
				
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
			case drawMapNodePopGraphIDX			:{			break;}
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
		
		((GUIObj_List) guiObjs[uiMapShapeIDX]).setValInList(mapStrings.get("mapGridShape"));	
		((GUIObj_List) guiObjs[uiMapBndsIDX]).setValInList(mapStrings.get("mapBounds"));	
		((GUIObj_List) guiObjs[uiMapRadCoolIDX]).setValInList(mapStrings.get("mapRadCool"));	
		((GUIObj_List) guiObjs[uiMapNHdFuncIDX]).setValInList(mapStrings.get("mapNHood"));	
		((GUIObj_List) guiObjs[uiMapLrnCoolIDX]).setValInList(mapStrings.get("mapLearnCool"));
				
	}//setUIValues
	
	/**
	 * set display of prebuilt map directories to use based on loaded info from project config file
	 * @param _pbltMapArray 
	 */
	public final void setPreBuiltMapArray(String[] _pbltMapArray) {
		msgObj.dispInfoMessage("SOM_MapUIWin","setPreBuiltMapArray","Attempting to set prebuilt map values list of size : " +_pbltMapArray.length);
		int curIDX = ((GUIObj_List) guiObjs[uiMapPreBuiltDirIDX]).setListVals(_pbltMapArray);
		uiUpdateData.setIntValue(uiMapPreBuiltDirIDX, (int) guiObjs[uiMapPreBuiltDirIDX].getVal());
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
	public final void setMapDataVal_String(int UIidx, double val) {	mapMgr.updateMapDatFromUI_String(getMapKeyStringFromUIidx(UIidx), ((GUIObj_List) guiObjs[UIidx]).getListValStr((int)val));}
	
	// set UI vals from map mgr - these are changes resulting from non-UI made changes to map
	@Override
	public final void updateUIDataVal_Integer(String key, Integer val) {
		if(!isMapNameOfType(mapDatNames_Ints, key)) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_Integer","Attempting to set UI object with unknown Key : " +key + " using integer value " + val +". Aborting.",MsgCodes.warning1);	
			return;}
		Integer uiObjIDX = getUIidxFromMapKeyString(key);
		uiUpdateData.setIntValue(uiObjIDX, (int) guiObjs[uiObjIDX].setVal(val));
		switch (uiObjIDX) {
			//integer values
			case uiMapRowsIDX 	    : {guiObjs[uiMapRadStIDX].setVal(.5*Math.min(val, guiObjs[uiMapColsIDX].getVal()));	break;}
			case uiMapColsIDX	    : {guiObjs[uiMapRadStIDX].setVal(.5*Math.min(guiObjs[uiMapRowsIDX].getVal(), val));break;}
			case uiMapEpochsIDX	    : {break;}
			case uiMapKTypIDX	    : {break;}
			case uiMapRadStIDX	    : {if(val <= guiObjs[uiMapRadEndIDX].getVal()+guiObjs[uiObjIDX].getModStep()) {uiUpdateData.setIntValue(uiObjIDX, (int) guiObjs[uiObjIDX].setVal(guiObjs[uiMapRadEndIDX].getVal()+guiObjs[uiObjIDX].getModStep()));}break;}
			case uiMapRadEndIDX	    : {if(val >= guiObjs[uiMapRadStIDX].getVal()-guiObjs[uiObjIDX].getModStep()) { uiUpdateData.setIntValue(uiObjIDX, (int) guiObjs[uiObjIDX].setVal(guiObjs[uiMapRadStIDX].getVal()-guiObjs[uiObjIDX].getModStep()));}break;}
		}
	}//setUIDataVal_Integer	

	@Override
	public final void updateUIDataVal_Float(String key, Float val) {
		if(!isMapNameOfType(mapDatNames_Floats, key)) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_Float","Attempting to set UI object with unknown Key : " +key + " using integer value " + val +". Aborting.",MsgCodes.warning1);	
			return;}
		Integer uiObjIDX = getUIidxFromMapKeyString(key);
		uiUpdateData.setFloatValue(uiObjIDX, (float) guiObjs[uiObjIDX].setVal(val));
		switch (uiObjIDX) {
		case uiMapLrnStIDX	    : {	if(val <= guiObjs[uiMapLrnEndIDX].getVal()+guiObjs[uiObjIDX].getModStep()) {uiUpdateData.setFloatValue(uiObjIDX, (float) guiObjs[uiObjIDX].setVal(guiObjs[uiMapLrnEndIDX].getVal()+guiObjs[uiObjIDX].getModStep()));}break;}
		case uiMapLrnEndIDX	    : {	if(val >= guiObjs[uiMapLrnStIDX].getVal()-guiObjs[uiObjIDX].getModStep()) {	uiUpdateData.setFloatValue(uiObjIDX, (float) guiObjs[uiObjIDX].setVal(guiObjs[uiMapLrnStIDX].getVal()-guiObjs[uiObjIDX].getModStep()));}break;}
		}
	}//setUIDataVal_Float
	
	@Override
	public final void updateUIDataVal_String(String key, String val) {
		if(!isMapNameOfType(mapDatNames_Strings, key)) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_String","Attempting to set UI object with unknown Key : " +key + " using String value " + val +". Aborting.",MsgCodes.warning1);
			return;}
		Integer uiObjIDX = getUIidxFromMapKeyString(key);
		int[] retVals = ((GUIObj_List) guiObjs[uiObjIDX]).setValInList(val);
		//if retVals[1] != 0 then not ok
		if(retVals[1] != 0) {
			msgObj.dispMessage("SOM_MapUIWin","updateUIDataVal_String","Attempting to set list object : " +key + " to unknown list value " + val +". Aborting.",MsgCodes.warning1);
		} else {
			uiUpdateData.setIntValue(uiObjIDX, retVals[0]);
		}
	}//setUIDataVal_String
	
	///////////////////////////////
	// end map <--> ui sync functions
	//
		
	/**
	 * Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
		
		switch(UIidx){
			//integer values
			case uiMapRowsIDX 	    : {setMapDataVal_Integer(UIidx,ival);guiObjs[uiMapRadStIDX].setVal(.5*MyMathUtils.min(ival, (int)guiObjs[uiMapColsIDX].getVal()));	break;}	//also set rad start to have a value == to 1/2 the max of rows or columns
			case uiMapColsIDX	    : {setMapDataVal_Integer(UIidx,ival);guiObjs[uiMapRadStIDX].setVal(.5*MyMathUtils.min((int)guiObjs[uiMapRowsIDX].getVal(), ival));break;}
			case uiMapEpochsIDX	    : {setMapDataVal_Integer(UIidx,ival);break;}
			case uiMapKTypIDX	    : {setMapDataVal_Integer(UIidx,ival);break;}
			case uiMapRadStIDX	    : {
				if(ival <= guiObjs[uiMapRadEndIDX].getVal()+guiObjs[UIidx].getModStep()) {uiUpdateData.setIntValue(UIidx, (int) guiObjs[UIidx].setVal(guiObjs[uiMapRadEndIDX].getVal()+guiObjs[UIidx].getModStep()));}
				setMapDataVal_Integer(UIidx,ival);		break;}
			case uiMapRadEndIDX	    : {
				if(ival >= guiObjs[uiMapRadStIDX].getVal()-guiObjs[UIidx].getModStep()) {uiUpdateData.setIntValue(UIidx, (int) guiObjs[UIidx].setVal(guiObjs[uiMapRadStIDX].getVal()-guiObjs[UIidx].getModStep()));}
				setMapDataVal_Integer(UIidx,ival);		break;}

			case uiMapNHdFuncIDX	: {setMapDataVal_String(UIidx,ival); break;}
			case uiMapRadCoolIDX	: {setMapDataVal_String(UIidx,ival); break;}
			case uiMapLrnCoolIDX	: {setMapDataVal_String(UIidx,ival); break;}
			case uiMapShapeIDX	    : {setMapDataVal_String(UIidx,ival); break;}
			case uiMapBndsIDX	    : {setMapDataVal_String(UIidx,ival); break;}
			//end map arg-related string/list values
			
			case uiTrainDataNormIDX 		: {//format of training data
				mapMgr.setCurrentTrainDataFormat(SOM_FtrDataType.getVal((int)(guiObjs[uiTrainDataNormIDX].getVal())));
				break;}
			case uiBMU_DispDataFrmtIDX 			: {
				mapMgr.setBMU_DispFtrTypeFormat(SOM_FtrDataType.getVal((int)(guiObjs[uiBMU_DispDataFrmtIDX].getVal())));
				break;}
			case uiTrainDatPartIDX 			: {break;}
			case uiNodeWtDispThreshIDX : {
				float _mapNodeWtDispThresh = (float)(guiObjs[uiNodeWtDispThreshIDX].getVal());
				mapMgr.setMapNodeWtDispThresh(_mapNodeWtDispThresh);
				mapMgr.setNodeInFtrWtSegThresh(_mapNodeWtDispThresh);				
				break;}
			case  uiNodePopDispThreshIDX:{
				float _mapNodePopDispThresh = (float)(guiObjs[uiNodePopDispThreshIDX].getVal());
				mapMgr.setMapNodePopDispThreshPct(_mapNodePopDispThresh);
				break;}
			case uiNodeInSegThreshIDX 		:{		//used to determine threshold of value for setting membership in a segment/cluster
				mapMgr.setNodeInUMatrixSegThresh((float)(guiObjs[uiNodeInSegThreshIDX].getVal()));
				mapMgr.buildUMatrixSegmentsOnMap();
				break;}			
			case uiMapPreBuiltDirIDX 		: {//what prebuilt map of list of maps shown to right of screen to use, if any are defined in project config
				mapMgr.setCurPreBuiltMapIDX((int) (guiObjs[uiMapPreBuiltDirIDX].getVal()));
				break;}			
			case uiMapNodeBMUTypeToDispIDX 	: {//type of examples being mapped to each map node to display
				mapMgr.setMapNodeDispType(SOM_ExDataType.getVal((int)(guiObjs[uiMapNodeBMUTypeToDispIDX].getVal())));
				break;}			
			case uiMseRegionSensIDX 		: {			break;}
			case uiFtrSelectIDX				: {		//feature idx map to display
				mapMgr.setCurFtrMapImgIDX((int)guiObjs[uiFtrSelectIDX].getVal());	
				break;}
			case uiCategorySelectIDX : {	//category select changed - managed by instancing app		
				setCategory_UIObj(settingCategoryFromClass); 
				break;}
			case uiClassSelectIDX : {			
				setClass_UIObj(settingClassFromCategory);  
				break;}				
			default : {
				boolean found = setUI_IntValsCustom_Indiv( UIidx,  ival,  oldVal);
				if (!found) {
					msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				}
				break;}
		}			
	}//setUI_IntValsCustom
	
	protected abstract boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal);
	
	/**
	 * Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx){
			case uiMapLrnStIDX	    : {
				if(val <= guiObjs[uiMapLrnEndIDX].getVal()+guiObjs[UIidx].getModStep()) {uiUpdateData.setFloatValue(UIidx, (float) guiObjs[UIidx].setVal(guiObjs[uiMapLrnEndIDX].getVal()+guiObjs[UIidx].getModStep()));}			
				setMapDataVal_Float(UIidx,val);			break;}
			case uiMapLrnEndIDX	    : {
				if(val >= guiObjs[uiMapLrnStIDX].getVal()-guiObjs[UIidx].getModStep()) {uiUpdateData.setFloatValue(UIidx, (float) guiObjs[UIidx].setVal(guiObjs[uiMapLrnStIDX].getVal()-guiObjs[UIidx].getModStep()));}		
				setMapDataVal_Float(UIidx,val);			break;}
			
			case uiTrainDataNormIDX 		: {//format of training data
				mapMgr.setCurrentTrainDataFormat(SOM_FtrDataType.getVal((int)(guiObjs[uiTrainDataNormIDX].getVal())));
				break;}
			case uiBMU_DispDataFrmtIDX 			: {
				mapMgr.setBMU_DispFtrTypeFormat(SOM_FtrDataType.getVal((int)(guiObjs[uiBMU_DispDataFrmtIDX].getVal())));
				break;}
			case uiTrainDatPartIDX 			: {break;}
			case uiNodeWtDispThreshIDX : {
				float _mapNodeWtDispThresh = (float)(guiObjs[uiNodeWtDispThreshIDX].getVal());
				mapMgr.setMapNodeWtDispThresh(_mapNodeWtDispThresh);
				mapMgr.setNodeInFtrWtSegThresh(_mapNodeWtDispThresh);				
				break;}
			case  uiNodePopDispThreshIDX:{
				float _mapNodePopDispThresh = (float)(guiObjs[uiNodePopDispThreshIDX].getVal());
				mapMgr.setMapNodePopDispThreshPct(_mapNodePopDispThresh);
				break;}
			case uiNodeInSegThreshIDX 		:{		//used to determine threshold of value for setting membership in a segment/cluster
				mapMgr.setNodeInUMatrixSegThresh((float)(guiObjs[uiNodeInSegThreshIDX].getVal()));
				mapMgr.buildUMatrixSegmentsOnMap();
				break;}			
			case uiMapPreBuiltDirIDX 		: {//what prebuilt map of list of maps shown to right of screen to use, if any are defined in project config
				mapMgr.setCurPreBuiltMapIDX((int) (guiObjs[uiMapPreBuiltDirIDX].getVal()));
				break;}			
			case uiMapNodeBMUTypeToDispIDX 	: {//type of examples being mapped to each map node to display
				mapMgr.setMapNodeDispType(SOM_ExDataType.getVal((int)(guiObjs[uiMapNodeBMUTypeToDispIDX].getVal())));
				break;}			
			case uiMseRegionSensIDX 		: {			break;}
			case uiFtrSelectIDX				: {		//feature idx map to display
				mapMgr.setCurFtrMapImgIDX((int)guiObjs[uiFtrSelectIDX].getVal());	
				break;}
			case uiCategorySelectIDX : {	//category select changed - managed by instancing app		
				setCategory_UIObj(settingCategoryFromClass); 
				break;}
			case uiClassSelectIDX : {			
				setClass_UIObj(settingClassFromCategory);  
				break;}
			default : {
				boolean found = setUI_FloatValsCustom_Indiv( UIidx, val, oldVal);
				if (!found) {
					msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No float-defined gui object mapped to idx :"+UIidx);
				}
				break;}
		}	
	}//setUI_FloatValsCustom
	
	protected abstract boolean setUI_FloatValsCustom_Indiv(int UIidx, float ival, float oldVal);
	
	
	
	/**
	 * feature type used to train currently loaded map
	 * @param _ftrTypeUsedToTrain
	 */
	public void setFtrTrainTypeFromConfig(int _ftrTypeUsedToTrain) {
		guiObjs[uiTrainDataNormIDX].setVal(_ftrTypeUsedToTrain);
		checkAndSetIntVal(uiTrainDataNormIDX,(int) guiObjs[uiTrainDataNormIDX].getVal());
	}
	
	protected void setCategory_UIObj(boolean settingCategoryFromClass) {
		mapMgr.setCurCategoryIDX((int)(guiObjs[uiCategorySelectIDX].getVal()));
		mapMgr.setCurCategoryLabel(getCategoryLabelFromIDX(mapMgr.getCurCategoryIDX()));				
		setUIWinVals_HandleCategory(settingCategoryFromClass); 
	}

	
	protected void setClass_UIObj(boolean settingClassFromCategory) {
		mapMgr.setCurClassIDX((int)(guiObjs[uiClassSelectIDX].getVal()));
		mapMgr.setCurClassLabel(getClassLabelFromIDX(mapMgr.getCurClassIDX()));				
		setUIWinVals_HandleClass(settingClassFromCategory);  		
	}
	
	private boolean settingCategoryFromClass = false, settingClassFromCategory = false;
	
	/**
	 * Called when class display select value is changed in ui
	 */
	protected final void setUIWinVals_HandleClass(boolean settingClassFromCategory) {
		if(!settingClassFromCategory) {		//don't want to change category again if setting from category ui obj change - loop potential
			int curCatIdxVal = (int)guiObjs[uiCategorySelectIDX].getVal();
			int catIdxToSet = getCategoryFromClass(curCatIdxVal,(int)guiObjs[uiClassSelectIDX].getVal());
			if(curCatIdxVal != catIdxToSet) {
				settingCategoryFromClass = true;
				guiObjs[uiCategorySelectIDX].setVal(catIdxToSet);
				setUIWinVals(uiCategorySelectIDX);
				checkAndSetIntVal(uiTrainDataNormIDX,(int) guiObjs[uiCategorySelectIDX].getVal());
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
				checkAndSetIntVal(uiClassSelectIDX,(int) guiObjs[uiClassSelectIDX].getVal());
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
//	/**
//	 * For instance-class specific ui values
//	 * @param UIidx
//	 */
//	protected abstract void setUIWinValsIndiv(int UIidx);
	
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

	
	
	public final float getTrainTestDatPartition() {	return (float)(.01*guiObjs[uiTrainDatPartIDX].getVal());}	
	
	
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
	
	@Override
	//draw 2d constructs over 3d area on screen - draws behind left menu section
	//modAmtMillis is in milliseconds
	protected final void drawRightSideInfoBarPriv(float modAmtMillis) {
		pa.pushMatState();
		//display current simulation variables - call sim world through sim exec
		mapMgr.drawResultBar(pa, yOff);
		pa.popMatState();					
	}//drawOnScreenStuff
	
	
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
		pa.pushMatState();
		//instance-specific drawing
		drawMapIndiv();
		if(mapDataLoaded){mapMgr.drawSOMMapData(pa);}	
		pa.popMatState();
	}//drawMap()	
	protected abstract void drawMapIndiv();
	
	public final void drawMseOverData() {	mapMgr.drawMseOverData(pa);}

	/////////////////////////////////////////
	// end draw routines

	
	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {
		msgObj.dispMessage("SOM_MapUIWin","handleSideMenuMseOvrDispSel","Click Mouse display in "+name+" : btn : " + btn, MsgCodes.info4);
		SOM_MseOvrDispTypeVals uiMseDispData = handleSideMenuMseOvrDisp_MapBtnToType(btn, val);
		//msgObj.dispInfoMessage("SOM_MapUIWin","handleSideMenuMseOvrDispSel","Mouse Display Function : " + btn +" selected == "+ uiMseDispData.toString());
		if(uiMseDispData == SOM_MseOvrDispTypeVals.mseOvrOtherIDX) {	handleSideMenuMseOvrDispSel_Indiv(btn,val);	}
		mapMgr.setUiMseDispData(uiMseDispData);
		msgObj.dispMessage("SOM_MapUIWin","handleSideMenuMseOvrDispSel","Done Click Mouse display in "+name+" : btn : " + btn + " | Dist Type : " + uiMseDispData.toString(), MsgCodes.info4);
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
	 * @param mouseX
	 * @param mouseY
	 * @param mseClckInWorld
	 * @param mseBtn
	 * @return
	 */
	public final boolean checkMouseClick(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		return mapMgr.checkMouseClick(mouseX, mouseY, mseClckInWorld, mseBtn);
	};
	/**
	 * check mouse drag/move in experiment; if btn == -1 then mouse over
	 * @param mouseX
	 * @param mouseY
	 * @param pmouseX
	 * @param pmouseY
	 * @param mouseClickIn3D
	 * @param mseDragInWorld
	 * @param mseBtn
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
	protected final String[] getSaveFileDirNamesPriv() {
		return new String[0];
	}

	@Override
	public final void hndlFileLoad(File file, String[] vals, int[] stIdx) {
	}

	@Override
	public final ArrayList<String> hndlFileSave(File file) {
		return new ArrayList<String>();
	}
	@Override
	protected final myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,0);}

	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc){}//not a snap-to window
	@Override
	public void processTrajIndiv(DrawnSimpleTraj drawnNoteTraj){		}
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
	protected void showMe() {		setCustMenuBtnLabels();	}

}//SOMMapUIWin
