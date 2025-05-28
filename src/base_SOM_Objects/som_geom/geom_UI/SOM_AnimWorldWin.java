package base_SOM_Objects.som_geom.geom_UI;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;
import base_SOM_Objects.som_geom.geom_utils.SOMGeomUIDataUpdater;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjTypes;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.base.GUI_AppWinVals;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Utils_Objects.tools.flags.Base_BoolFlags;
/**
 * this class will instance a combined window to hold an animation world and a
 * map display window overlay
 * 
 * @author john
 *
 */

public abstract class SOM_AnimWorldWin extends Base_DispWindow {
	/**
	 * map manager corresponding to this animation world
	 */
	public SOM_GeomMapManager mapMgr;

	/**
	 * window for map UI for this object
	 */
	protected SOM_GeomMapUIWin somUIWin = null;

	//ui vars
	public static final int
		gIDX_NumUIObjs 			= 0,
		gIDX_NumUISamplesPerObj = 1,	//per object # of training examples
		gIDX_FractNumTrainEx	= 2,	//fraction of span from min # to max # of training examples to set as value - to counter how difficult it can be to change the value
		gIDX_NumTrainingEx		= 3,	//total # of training examples to synthesize for training - will be a combinatorial factor of # of objs and # of samples per obj 
		gIDX_SelDispUIObj		= 4;	//ID of a ui obj to be selected and highlighted					

	protected static final int numBaseAnimWinUIObjs = 5;

	// raw values from ui components
	/**
	 * # of priv flags from base class and instancing class
	 */

	public static final int 
		//idx 0 is debug in privFlags structure
		showFullSourceObjIDX	= 1,				//show/hide full source object(sources of sample points)
		showSamplePntsIDX 		= 2,				//show/hide sample points
		
		showFullTrainingObjIDX	= 3,				//show/hide training data full objects		
		
		showUIObjLabelIDX		= 4,				//display the ui obj's ID as a text tag
		showUIObjSmplsLabelIDX	= 5,				//display the ui object's samples' IDs as a text tag
		
		showObjByWireFrmIDX		= 6,				//show object as wireframe, or as filled-in
		
		showSelUIObjIDX			= 7,				//highlight the ui obj with the selected idx
		showMapBasedLocsIDX 	= 8,				//show map-derived locations (bmu) of training data instead of actual locations (or along with?)
		
		useUIObjLocAsClrIDX		= 9,				//use location derived color, or random color; should use ui obj's location as both its and its samples' color
		allTrainExUniqueIDX 	= 10, 				//all training examples should be unique, if requested # can be supported, otherwise duplicate examples allowed
		//useSmplsForTrainIDX 	= 10,				//use surface samples, or ui obj centers, for training data
		uiObjBMUsSetIDX			= 11,				//ui object's bmus have been set
		mapBuiltToCurUIObjsIDX 	= 12,				//the current ui obj configuration has an underlying map built to it
		regenUIObjsIDX 			= 13,				//regenerate ui objs with current specs
		drawMapNodeGeomObjsIDX	= 14,				//whether or not to draw the selected map nodes' geometric representations
		drawSOM_MapUIVis		= 15;

	protected static final int numBaseAnimWinPrivFlags = 16;

	// initial UI values
	public int numGeomObjs = 10, numSmplPointsPerObj = 200, numTrainingExamples = 40000, curSelGeomObjIDX = 0;
	// fraction of max count of binomial coefficient to set as # of training
	// examples to sample from objects + samples
	public double fractOfBinomialForBaseNumTrainEx = .001;

	// object type the instancing window manages
	protected final SOM_GeomObjTypes geomObjType;

	// dimensions of SOM Map - hard coded to override setting from SOM Map UI Window
	// - need to set in window
	protected float[] SOMMapDims = new float[]{834.8f, 834.8f};


	public SOM_AnimWorldWin(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx, SOM_GeomObjTypes _type) {
		super(_p, _AppMgr, _winIdx);
		initAndSetAnimWorldVals();
		geomObjType = _type;
	}
	
	/**
	 * Individual SOM map window for each anim world.
	 * @param ownerWin
	 * @param fIdx
	 * @return
	 */
	public final void buildAndSetSOM_MapDispUIWin(int fIdx) {
		float[] _dimOpen = AppMgr.getDefaultPopUpWinDimOpen();
		float[] _dimClosed  =  AppMgr.getDefaultPopUpWinDimClosed();
		//Keep popup window full width whether shown or hidden
		_dimClosed[2] = _dimOpen[2];
		//keep hidden window
		float[] _initCamVals = AppMgr.getInitCameraValues();
		String owner = this.getName();
		//(int _winIDX, float[] _dimOpen, float[] _dimClosed, boolean[] _dispFlags, int[] _fill, int[] _strk, int[] _trajFill, int[] _trajStrk)

		
		/**
		 * Creates a struct holding a display window's necessary initialization values
		 * @param _winIdx the window's idx
		 * @param _strVals an array holding the window title(idx 0) and the window description(idx 1)
		 * @param _flags an array holding boolean values for idxs : 
		 * 		0 : dispWinIs3d, 
		 * 		1 : canDrawInWin; 
		 * 		2 : canShow3dbox (only supported for 3D); 
		 * 		3 : canMoveView
		 * @param _floatVals an array holding float arrays for 
		 * 				rectDimOpen(idx 0),
		 * 				rectDimClosed(idx 1),
		 * 				initCameraVals(idx 2)
		 * @param _intVals and array holding int arrays for
		 * 				winFillClr (idx 0),
		 * 				winStrkClr (idx 1),
		 * 				winTrajFillClr(idx 2),
		 * 				winTrajStrkClr(idx 3),
		 * 				rtSideFillClr(idx 4),
		 * 				rtSideStrkClr(idx 5)
		 * @param _sceneCenterVal center of scene, for drawing objects
		 * @param _initSceneFocusVal initial focus target for camera
		 */
		
		GUI_AppWinVals GeomMapUIWinDef = AppMgr.buildGUI_AppWinVals(-1, "Map UI for " + owner, "Visualize SOM Node location for "+owner,
				new boolean[] {false, false, false, false},
				new float[][] {_dimOpen, _dimClosed, _initCamVals},
				new int [][] {new int[]{20,40,50,200}, ri.getClr(IRenderInterface.gui_White, 255),
					ri.getClr(IRenderInterface.gui_LightGray, 255),ri.getClr(IRenderInterface.gui_Gray, 255),
					ri.getClr(IRenderInterface.gui_Black, 200),ri.getClr(IRenderInterface.gui_White, 255)});
		
		somUIWin = new SOM_GeomMapUIWin(ri, AppMgr, GeomMapUIWinDef, AppMgr.getArgsMap(), this);	
		
		somUIWin.setUI_FeatureListVals(setUI_GeomObjFeatureListVals());
		somUIWin.setMapMgr(mapMgr);
		msgObj.dispInfoMessage(className+"(SOM_AnimWorldWin)", "setGeomMapUIWin", "Setting somUIWin in " + winInitVals.winName + " to be  : "
				+ somUIWin.getName() + " and somUIWin's mapMgr to one belonging to win : " + mapMgr.win.getName());		
	}//buildAndSetSOM_MapDispUIWin	
	
	/**
	 * Initialize any UI control flags appropriate for all SOM Animation window applications
	 */
	@Override
	protected final void initDispFlags() {
		initDispFlags_Indiv();
	}
	
	/**
	 * Initialize any UI control flags appropriate for specific instanced SOM Animation window
	 */
	protected abstract void initDispFlags_Indiv();
	
	@Override
	protected final void initMe() {
		// build map associated with this geometric experiment
		// perform in this window since SOM window is subordinate to this one
		mapMgr = buildGeom_SOMMapManager();
		//set offset to use for custom menu objects
		custMenuOffset = 0.0f;
		// instance-specific init
		initMe_Indiv();
		// build default objects in screen
		rebuildSourceGeomObjs();
	}

	/**
	 * This function would provide an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.
	 */
	@Override
	protected UIDataUpdater buildUIDataUpdateObject() {
		return new SOMGeomUIDataUpdater(this);
	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	protected final void updateCalcObjUIVals() {}

	@Override
	protected int[] getFlagIDXsToInitToTrue() {
		return new int[] {showFullSourceObjIDX,useUIObjLocAsClrIDX, allTrainExUniqueIDX};
	}

	protected abstract String[] setUI_GeomObjFeatureListVals();

	/**
	 * whenever som is shown or not
	 */
	protected void setSOM_MapUIWinState(boolean val) {
		if (null != somUIWin) {	somUIWin.setShowWin(val);}
	}

	/**
	 * called after base window ctor/before instancing class ctor functions and
	 * initMe any initialization that is relevant for this abstract class's objects
	 */
	private void initAndSetAnimWorldVals() {
	}

	/**
	 * instancing window will build the map manager that this anim world will use
	 * 
	 * @return newly built map manager
	 */
	protected abstract SOM_GeomMapManager buildGeom_SOMMapManager();

	/**
	 * return appropriate SOM Map Manager for this window
	 * 
	 * @return SOM Map manager
	 */
	public final SOM_MapManager getMapMgr() {
		return mapMgr;
	}

	@Override
	protected final int initAllUIButtons(ArrayList<Object[]> tmpBtnNamesArray) {

		// add an entry for each button, in the order they are wished to be displayed
		// true tag, false tag, btn IDX

		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Debugging", "Debug"}, Base_BoolFlags.debugIDX));
		// UI",drawSOM_MapUIVis});
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Regenerating " + geomObjType.getName()+ " Objs","Regenerate " + geomObjType.getName()+ " Objs"}, regenUIObjsIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing " + geomObjType.getName()+ " Objects", "Show " + geomObjType.getName()+ " Objects"},	showFullSourceObjIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing " + geomObjType.getName()+ " Sample Points","Show " + geomObjType.getName()+ " Sample Points"}, showSamplePntsIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing Labels", "Show Labels"}, showUIObjLabelIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing Sample Labels", "Show Sample Labels"}, showUIObjSmplsLabelIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing Loc-based Color", "Showing Random Color"}, useUIObjLocAsClrIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing " + geomObjType.getName()+ " Training Exs",	"Show " + geomObjType.getName()+ " Training Exs"}, showFullTrainingObjIDX));

		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Hi-Light Sel " + geomObjType.getName()+ " ", "Enable " + geomObjType.getName()+ " Hi-Light"},	showSelUIObjIDX));
		// tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Train From " +geomObjType.getName()+ " Samples",
		// "Train From " +geomObjType.getName()+ " Centers/Bases", useSmplsForTrainIDX});
		// tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Save Data", "Save Data",
		// saveUIObjDataIDX});
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Gen Unique " + geomObjType.getName()+ " Train Exs",	"Allow dupe " + geomObjType.getName()+ " Train Exs"}, allTrainExUniqueIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing Map Node Geometry", "Show Map Node Geometry"}, drawMapNodeGeomObjsIDX));
		tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"Showing BMU-derived Locs", "Showing Actual Locs"}, showMapBasedLocsIDX));

		String[] showWFObjsTFLabels = getShowWireFrameBtnTFLabels();
		if ((null != showWFObjsTFLabels) && (showWFObjsTFLabels.length == 2)) {
			tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {showWFObjsTFLabels[0], showWFObjsTFLabels[1]}, showObjByWireFrmIDX));
		}

		// add instancing-class specific buttons - returns total # of private flags in
		// instancing class
		return initAllAnimWorldPrivBtns_Indiv(tmpBtnNamesArray);
		
	}

	/**
	 * Instancing class-specific (application driven) UI buttons to display are
	 * built in this function. Add an entry to tmpBtnNamesArray for each button, in
	 * the order they are to be displayed
	 * 
	 * @param tmpBtnNamesArray array list of Object arrays, where in each object
	 *                         array : the first element is the true string label,
	 *                         the 2nd elem is false string array, and the 3rd
	 *                         element is integer flag idx
	 * @return total number of privBtnFlags in instancing class (including those not
	 *         displayed)
	 */
	protected abstract int initAllAnimWorldPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);

	/**
	 * Instance class determines the true and false labels for button to control
	 * showing full object, or just wire frame If empty no button is displayed
	 * 
	 * @return array holding true(idx0) and false(idx1) labels for button
	 */
	protected abstract String[] getShowWireFrameBtnTFLabels();

	/**
	 * call to build or rebuild source geometric objects
	 */
	public final void rebuildSourceGeomObjs() {
		msgObj.dispInfoMessage(className+"(SOM_AnimWorldWin)", "rebuildSourceGeomObjs",	"Start (re)building all objects of type " + this.geomObjType);
		setMapMgrGeomObjVals();
		((SOM_GeomMapManager) mapMgr).buildGeomExampleObjs();
		msgObj.dispInfoMessage(className+"(SOM_AnimWorldWin)", "rebuildSourceGeomObjs",	"Finished (re)building all objects of type " + this.geomObjType);
	}

	/**
	 * send all UI values to map manager before objrunner tasks are executed
	 */
	public final void setMapMgrGeomObjVals() {
		((SOM_GeomMapManager) mapMgr).setUIObjData(numGeomObjs, numSmplPointsPerObj, numTrainingExamples);
		setMapMgrGeomObjVals_Indiv();
	}

	/**
	 * send all instance-specific values from UI to map manager
	 */
	protected abstract void setMapMgrGeomObjVals_Indiv();

	/**
	 * regenerate samples for current set of base objects
	 */
	public final void regenBaseGeomObjSamples() {
		msgObj.dispInfoMessage(className+"(SOM_AnimWorldWin)", "regenBaseGeomObjSamples", "Start regenerating "	+ numSmplPointsPerObj + " samples for all base objects of type " + this.geomObjType);
		setMapMgrGeomObjVals();
		((SOM_GeomMapManager) mapMgr).rebuildGeomExObjSamples();
		msgObj.dispInfoMessage(className+"(SOM_AnimWorldWin)", "regenBaseGeomObjSamples", "Finished regenerating "	+ numSmplPointsPerObj + " samples for all base objects of type " + this.geomObjType);
	}
	/**
	 * UI code-level Debug mode functionality. Called only from flags structure
	 * @param val
	 */
	@Override
	protected final void handleDispFlagsDebugMode_Indiv(boolean val) {}
	
	/**
	 * Application-specific Debug mode functionality (application-specific). Called only from privflags structure
	 * @param val
	 */
	@Override
	protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {	}
	
	/**
	 * Handle application-specific flag setting
	 */
	@Override
	public void handlePrivFlags_Indiv(int idx, boolean val, boolean oldVal){
		switch (idx) {// special actions for each flag
		case showSamplePntsIDX: {		break;		} // show/hide object's sample points
		case showFullSourceObjIDX: {	break;		} // show/hide full object
		case showFullTrainingObjIDX: {	break;		}
		// case saveUIObjDataIDX : {
		// if(val){saveGeomObjInfo();addPrivBtnToClear(saveUIObjDataIDX);}break;} //save
		// all object data
		case showUIObjLabelIDX: {		break;		} // show labels for objects
		case showUIObjSmplsLabelIDX: {	break;		}
		case useUIObjLocAsClrIDX: {
			msgObj.dispInfoMessage(className+"(SOM_AnimWorldWin)", "setPrivFlags :: useUIObjLocAsClrIDX", "Val :  " + val);
			break;
		} // color of objects is location or is random
		case showSelUIObjIDX: {			break;		}
		// case useSmplsForTrainIDX : { break;} //use surface samples for train and
		// centers for test, or vice versa
		case allTrainExUniqueIDX: {
			if (mapMgr != null) {	mapMgr.setTrainExShouldBeUnqiue(val);}
			break;
		}
		case showMapBasedLocsIDX: {		break;		}
		case uiObjBMUsSetIDX: {			break;		}
		case mapBuiltToCurUIObjsIDX: {	break;		} // whether map has been built and loaded for current config of spheres
		case regenUIObjsIDX: {
			if (val) {
				rebuildSourceGeomObjs();
				addPrivBtnToClear(regenUIObjsIDX);
			}
			break;
		} // remake all objects, turn off flag
		case showObjByWireFrmIDX: {		break;		}
		case drawMapNodeGeomObjsIDX: {	break;		}
		case drawSOM_MapUIVis: {
			this.setSOM_MapUIWinState(val);
			break;
		}
		default: {			handleSOMAnimFlags_Indiv(idx, val);}
		}
	}// setPrivFlags

	/**
	 * set values for instancing class-specific boolean flags
	 * 
	 * @param idx
	 * @param val
	 */
	protected abstract void handleSOMAnimFlags_Indiv(int idx, boolean val);
	
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
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals) {
		
		int minNumObjs = getMinNumObjs(), maxNumObjs = getMaxNumObjs(),	diffNumObjs = (maxNumObjs - minNumObjs > 100 ? 10 : 1);
		numGeomObjs = minNumObjs;
		tmpUIObjArray.put(gIDX_NumUIObjs, uiMgr.uiObjInitAra_Int(new double[]{minNumObjs, maxNumObjs, diffNumObjs}, numGeomObjs,"# of " + geomObjType.getName()+ " Objects", new boolean[]{true, true})); // gIDX_NumUIObjs
		int minNumSmplsPerObj = getMinNumSmplsPerObj(), maxNumSmplsPerObj = getMaxNumSmplsPerObj(),	diffNumSmplsPerObj = (maxNumSmplsPerObj - minNumSmplsPerObj > 100 ? 10 : 1);
		numSmplPointsPerObj = minNumSmplsPerObj;
		tmpUIObjArray.put(gIDX_NumUISamplesPerObj, uiMgr.uiObjInitAra_Int(new double[]{minNumSmplsPerObj, maxNumSmplsPerObj, diffNumSmplsPerObj}, numSmplPointsPerObj, "# of samples per Object", new boolean[]{true, true})); // gIDX_NumUISamplesPerObj
		tmpUIObjArray.put(gIDX_FractNumTrainEx, uiMgr.uiObjInitAra_Float(new double[]{0.00001, 1.000, 0.00001}, fractOfBinomialForBaseNumTrainEx,"Fract of Binomial for Train Ex", new boolean[]{true, false})); // gIDX_FractNumTrainEx

		long minNumTrainingExamples = numGeomObjs,
				maxNumTrainingExamples = getNumTrainingExamples(numGeomObjs, numSmplPointsPerObj),
				diffNumTrainingEx = (maxNumTrainingExamples - minNumTrainingExamples) > 1000 ? 1000 : 10;
		numTrainingExamples = (int) minNumTrainingExamples;
		tmpUIObjArray.put(gIDX_NumTrainingEx, uiMgr.uiObjInitAra_Int(new double[]{minNumTrainingExamples, maxNumTrainingExamples, diffNumTrainingEx}, numTrainingExamples,	"Ttl # of Train Ex [" + minNumTrainingExamples + ", " + maxNumTrainingExamples + "]", new boolean[]{true, false})); // gIDX_NumUISamplesPerObj
		tmpUIObjArray.put(gIDX_SelDispUIObj, uiMgr.uiObjInitAra_Int(new double[]{0, numGeomObjs-1, 1}, curSelGeomObjIDX,"ID of Object to Select", new boolean[]{true, true})); // gIDX_SelDispUIObj

		// populate instancing application objects
		setupGUIObjsAras_Indiv(tmpUIObjArray, tmpListObjVals);

	}// setupGUIObjsAras

	/**
	 * calculate the max # of examples for this type object - clique of object
	 * description degree
	 */
	protected abstract long getNumTrainingExamples(int objs, int smplPerObj);

	protected abstract int getMinNumObjs();
	protected abstract int getMaxNumObjs();
	protected abstract int getMinNumSmplsPerObj();
	protected abstract int getMaxNumSmplsPerObj();
	protected abstract int getModNumSmplsPerObj();

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

	/**
	 * update # of max training examples based on updated number of desired objects
	 * or samples per object
	 */
	private void refreshNumTrainingExampleBounds() {
		//min # of training examples will at least be # of geometric objects 
		long newMinVal = numGeomObjs;
		uiMgr.setNewUIMinVal(gIDX_NumTrainingEx, newMinVal);
		// binomial coefficient - n (total # of samples across all objects) choose k
		// (dim of minimal defining set of each object)		
		long newMaxVal = getNumTrainingExamples(numGeomObjs, numSmplPointsPerObj);
		uiMgr.setNewUIMaxVal(gIDX_NumTrainingEx, newMaxVal);
		uiMgr.setNewUIDispText(gIDX_NumTrainingEx, true, "Ttl # of Train Ex [" + newMinVal + ", " + newMaxVal + "]");
		double curNum = uiMgr.getUIValue(gIDX_NumTrainingEx);
		if (curNum < newMinVal) {
			uiMgr.setNewUIValue(gIDX_NumTrainingEx,newMinVal);
		}
		if (curNum > newMaxVal) {
			uiMgr.setNewUIValue(gIDX_NumTrainingEx,newMaxVal);
		}
	}// refreshNumTrainingExampleBounds

	private void refreshNumTrainingExamples() {
		long TtlNumExamples = getNumTrainingExamples(numGeomObjs, numSmplPointsPerObj);
		double newVal = fractOfBinomialForBaseNumTrainEx * TtlNumExamples;
		uiMgr.setNewUIValue(gIDX_NumTrainingEx,newVal);
		uiMgr.setUIWinVals(gIDX_NumTrainingEx);
	}
	
	/**
	 * Called if int-handling guiObjs_Numeric[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
		switch(UIidx){	
			case gIDX_NumUIObjs: {
				numGeomObjs = ival;
				uiMgr.setNewUIMaxVal(gIDX_SelDispUIObj,ival - 1);
				refreshNumTrainingExampleBounds();
				rebuildSourceGeomObjs();			
				break;
			}
			case gIDX_NumUISamplesPerObj: {
				numSmplPointsPerObj = ival;
				refreshNumTrainingExampleBounds();
				regenBaseGeomObjSamples();
				break;
			}		
			case gIDX_NumTrainingEx: {
				numTrainingExamples = ival;		
				setMapMgrGeomObjVals();
				break;
			}
			case gIDX_SelDispUIObj: {				
				curSelGeomObjIDX = MyMathUtils.min(ival, numGeomObjs - 1);
				break;
			}			
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
	 * Called if float-handling guiObjs_Numeric[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx){		
			case gIDX_FractNumTrainEx: { // fraction of total # of possible samples in current configuration to use for
				// training examples
				fractOfBinomialForBaseNumTrainEx = val;
				refreshNumTrainingExamples();
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
	 * instancing class-specific functionality
	 */
	protected abstract void initMe_Indiv();

	/**
	 * get ui values used to build current geometry (for preproc save)
	 * 
	 * @return map holding current ui values, augmented by possible instance-specific values
	 */
	public TreeMap<String, String> getAllUIValsForPreProcSave() {
		TreeMap<String, String> res = new TreeMap<String, String>();
		res.put("gIDX_NumUIObjs", String.format("%4d", (int) uiMgr.getUIValue(gIDX_NumUIObjs)));
		res.put("gIDX_NumUISamplesPerObj", String.format("%4d", (int) uiMgr.getUIValue(gIDX_NumUISamplesPerObj)));
		res.put("gIDX_FractNumTrainEx", String.format("%.4f", uiMgr.getUIValue(gIDX_FractNumTrainEx)));
		res.put("gIDX_NumTrainingEx", String.format("%4d", (int) uiMgr.getUIValue(gIDX_NumTrainingEx)));

		getAllUIValsForPreProcSave_Indiv(res);
		return res;
	}

	/**
	 * get instance-class specific ui values used to build current geometry (for
	 * preproc save)
	 * 
	 * @return instance-specific map holding current ui values
	 */
	protected abstract void getAllUIValsForPreProcSave_Indiv(TreeMap<String, String> vals);

	/**
	 * set ui values used to build preproc data being loaded
	 */
	public void setAllUIValsFromPreProcLoad(TreeMap<String, String> uiVals) {
		uiMgr.setNewUIValue(gIDX_FractNumTrainEx,Double.parseDouble(uiVals.get("gIDX_FractNumTrainEx")));
		uiMgr.setNewUIValue(gIDX_NumUIObjs,Integer.parseInt(uiVals.get("gIDX_NumUIObjs")));
		uiMgr.setNewUIValue(gIDX_NumUISamplesPerObj,Integer.parseInt(uiVals.get("gIDX_NumUISamplesPerObj")));
		uiMgr.setNewUIValue(gIDX_NumTrainingEx,Integer.parseInt(uiVals.get("gIDX_NumTrainingEx")));
		
		setAllUIValsFromPreProcLoad_Indiv(uiVals);
		uiMgr.setAllUIWinVals(); 
	}

	/**
	 * set ui instance-specific values used to build preproc data being loaded
	 */
	protected abstract void setAllUIValsFromPreProcLoad_Indiv(TreeMap<String, String> uiVals);

/////////////////////////////
	// drawing routines
	@Override
	protected void setCamera_Indiv(float[] camVals) {
		// No custom camera handling
		setCameraBase(camVals);
	}//setCameraIndiv

	/**
	 * draw the SOM Window
	 * 
	 * @param modAmtMillis
	 */
	public void drawSOMWinUI(float modAmtMillis) {
		if (null != somUIWin) {
			somUIWin.draw2D(modAmtMillis);
			somUIWin.drawHeader(new String[0], false, AppMgr.isDebugMode(), modAmtMillis);
		}
	}

	@Override
	protected final void drawMe(float animTimeMod) {
		ri.pushMatState();
		// nested ifthen shenannigans to get rid of if checks in each individual draw
		drawMeFirst_Indiv();
		// check if geom objs are built in mapMgr
		boolean useUIObjLocAsClr = uiMgr.getPrivFlag(useUIObjLocAsClrIDX),
				showUIObjLabel = uiMgr.getPrivFlag(showUIObjLabelIDX);
		if (mapMgr.getGeomObjsBuilt()) {
			boolean wantDrawBMUs = uiMgr.getPrivFlag(showMapBasedLocsIDX);
			boolean shouldDrawBMUs = (wantDrawBMUs && uiMgr.getPrivFlag(mapBuiltToCurUIObjsIDX));
			if (!shouldDrawBMUs && wantDrawBMUs) {
				uiMgr.setPrivFlag(showMapBasedLocsIDX, false);
				wantDrawBMUs = false;
			}
			_drawObjs(mapMgr.sourceGeomObjects, mapMgr.sourceGeomObjects.length, curSelGeomObjIDX, animTimeMod, shouldDrawBMUs,
					uiMgr.getPrivFlag(showSamplePntsIDX), uiMgr.getPrivFlag(showFullSourceObjIDX),
					useUIObjLocAsClr, uiMgr.getPrivFlag(showSelUIObjIDX), uiMgr.getPrivFlag(showObjByWireFrmIDX),
					showUIObjLabel, uiMgr.getPrivFlag(showUIObjSmplsLabelIDX));
		}
		// check if train samples are built in map mgr
		if ((mapMgr.getTrainDataObjsBuilt()) && (uiMgr.getPrivFlag(showFullTrainingObjIDX))) {
			_drawObjs(mapMgr.trainDatGeomObjects, mapMgr.getNumTrainingExsToShow(), -1, animTimeMod, false, false, true,
					useUIObjLocAsClr, false, uiMgr.getPrivFlag(showObjByWireFrmIDX),
					showUIObjLabel, false);
		} else {
			uiMgr.setPrivFlag(showFullTrainingObjIDX, false);
		}
		// draw geom objects for selected map node objects
		if (uiMgr.getPrivFlag(drawMapNodeGeomObjsIDX)) {
			mapMgr.drawSelectedMapNodeGeomObjs(ri, animTimeMod, showUIObjLabel,	useUIObjLocAsClr);
		}
		drawMeLast_Indiv();
		ri.popMatState();

	}// drawMe

	/**
	 * Draw all the example objects
	 * @param objs the array holding the example objects to draw
	 * @param numToDraw the number of objects to draw - expected to be > curSelObjIDX
	 * @param curSelObjIDX the currently selected object idx.
	 * @param animTimeMod
	 * @param mapBuiltAndUseMapLoc
	 * @param showSmpls
	 * @param showObjs
	 * @param useLocClr
	 * @param showSel
	 * @param showWireFrame
	 * @param showLabel
	 * @param showSmplsLabel
	 */
	
	private void _drawObjs(SOM_GeomObj[] objs, int numToDraw, int curSelObjIDX, float animTimeMod, boolean mapBuiltAndUseMapLoc,
			boolean showSmpls, boolean showObjs, boolean useLocClr, boolean showSel, boolean showWireFrame,
			boolean showLabel, boolean showSmplsLabel) {
		// nested ifthen shenannigans to get rid of if checks in each individual draw
		if (mapBuiltAndUseMapLoc) { // show bmus for objs
			if (showSel) {
				// if selected, show object filled with chosen color and show all other objects wireframe
				if (useLocClr) {	objs[curSelObjIDX].drawMeSelected_ClrLoc_BMU(ri, animTimeMod, showSmpls);} 
				else {				objs[curSelObjIDX].drawMeSelected_ClrRnd_BMU(ri, animTimeMod, showSmpls);}
				if (showLabel) {	objs[curSelObjIDX].drawMyLabel_BMU(ri, this);}

				if (showObjs) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrLoc_WF_BMU(ri);}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeClrLoc_WF_BMU(ri);}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrRnd_WF_BMU(ri);}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeClrRnd_WF_BMU(ri);}
					} // rand color
				}
				if (showSmpls) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrLoc_BMU(ri);	}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeSmpls_ClrLoc_BMU(ri);	}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrRnd_BMU(ri);	}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeSmpls_ClrRnd_BMU(ri);	}
					} // rand color
				}
			} else {
				if (showObjs) {		_drawObjs_UseBMUs(objs, numToDraw, useLocClr, showWireFrame, showLabel);}
				if (showSmpls) {
					if (useLocClr) {	for (int i = 0; i < numToDraw; ++i) {					objs[i].drawMeSmpls_ClrLoc_BMU(ri);	}} // loc color
					else {				for (int i = 0; i < numToDraw; ++i) {					objs[i].drawMeSmpls_ClrRnd_BMU(ri);	}} // rand color
				}
			}
		} else {
			if (showSel) {
				// if selected, show selected object filled with chosen color and show all other objects wireframe with or without samples and no labels
				if (useLocClr) {			objs[curSelObjIDX].drawMeSelected_ClrLoc(ri, animTimeMod, showSmpls);} 
				else {						objs[curSelObjIDX].drawMeSelected_ClrRnd(ri, animTimeMod, showSmpls);}
				if (showLabel) {			objs[curSelObjIDX].drawMyLabel(ri, this);}
				// all other objects default to wireframe display
				if (showObjs) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrLoc_WF(ri);}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeClrLoc_WF(ri);}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrRnd_WF(ri);}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeClrRnd_WF(ri);}
					} // rand color
				}
				if (showSmpls) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrLoc(ri);	}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeSmpls_ClrLoc(ri);	}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrRnd(ri);	}
						for (int i = curSelObjIDX + 1; i < numToDraw; ++i) {	objs[i].drawMeSmpls_ClrRnd(ri);	}
					} // rand color
				}
			} else {
				if (showObjs) {				_drawObjs_UseActual(objs, numToDraw, animTimeMod, useLocClr, showSmpls, showWireFrame, showLabel);}
				if (showSmpls) {
					if (useLocClr) {		for (int i = 0; i < numToDraw; ++i) {					objs[i].drawMeSmpls_ClrLoc(ri);	}} // loc color
					else {					for (int i = 0; i < numToDraw; ++i) {					objs[i].drawMeSmpls_ClrRnd(ri);	}} // rand color
					if (showSmplsLabel) {	for (int i = 0; i < numToDraw; ++i) {					objs[i].drawMySmplsLabel(ri, this);}}
				}
			}
		}
	}// _drawObjs

	private void _drawObjs_UseActual(SOM_GeomObj[] objs, int numToDraw, float animTimeMod, boolean useLocClr, boolean showSmpls,
			boolean showWireFrame, boolean showLabel) {
		if (showWireFrame) { // draw objects with wire frames
			if (useLocClr) {	for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrLoc_WF(ri);}} // loc color
			else {				for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrRnd_WF(ri);}} // rand color
		} else {
			if (useLocClr) {	for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrLoc(ri);}} // loc color
			else {				for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrRnd(ri);}} // rand color
		}
		if (showLabel) {		for (int i = 0; i < numToDraw; ++i) {		objs[i].drawMyLabel(ri, this);}}
	}// _drawObjs_UseActual

	private void _drawObjs_UseBMUs(SOM_GeomObj[] objs, int numToDraw, boolean useLocClr, boolean showWireFrame, boolean showLabel) {
		if (showWireFrame) { // draw objects with wire frames
			if (useLocClr) {	for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrLoc_WF_BMU(ri);}} // loc color
			else {				for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrRnd_WF_BMU(ri);}} // rand color
		} else {
			if (useLocClr) {	for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrLoc_BMU(ri);}} // loc color
			else {				for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMeClrRnd_BMU(ri);}} // rand color
		}
		if (showLabel) {		for (int i = 0; i < numToDraw; ++i) {	objs[i].drawMyLabel_BMU(ri, this);}}
	}// _drawObjs_UseBMUs

	/**
	 * instance-specific drawing setup before objects are actually drawn
	 */
	protected abstract void drawMeFirst_Indiv();

	/**
	 * instance-specific drawing after objects are drawn but before info is saved
	 */
	protected abstract void drawMeLast_Indiv();

	/**
	 * draw som map window UI Objects
	 */
	@Override
	public final void drawCustMenuObjs(float modAmtMillis) {
		// ((SOM_GeometryMain) pa).drawSOMUIObjs();
		// if(this.uiMgr.getPrivFlag(drawSOM_MapUIVis)) {
		if (somUIWin != null) {
			ri.pushMatState();
			somUIWin.drawWindowGuiObjs(AppMgr.isDebugMode(), modAmtMillis);					//draw what user-modifiable fields are currently available
//			somUIWin.drawGUIObjs(); // draw what user-modifiable fields are currently available
//			somUIWin.drawClickableBooleans(); // draw what user-modifiable boolean buttons
			ri.popMatState();
		}
	}

	@Override
	// draw 2d constructs over 3d area on screen - draws behind left menu section
	// modAmtMillis is in milliseconds
	protected final void drawRightSideInfoBarPriv(float modAmtMillis) {
		ri.pushMatState();
		//instance-specific
		float newYOff = drawRightSideInfoBar_Indiv(modAmtMillis, AppMgr.getTextHeightOffset());
		// display current simulation variables - call sim world through sim exec
		mapMgr.drawResultBar(ri, newYOff);
		ri.popMatState();
	}// drawOnScreenStuff

	/**
	 * any instance-window specific display
	 * 
	 * @param modAmtMillis
	 */
	protected abstract float drawRightSideInfoBar_Indiv(float modAmtMillis, float yOff);

	/////////////////////////////
	// end drawing routines

	/////////////////////////////
	// sim routines
	@Override
	protected final boolean simMe(float modAmtSec) {return false;}

	@Override
	protected final void stopMe() {}
	/////////////////////////////
	// end sim routines
	/**
	 * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
	 * @param funcRow idx for button row
	 * @param btn idx for button within row (column)
	 * @param label label for this button (for display purposes)
	 */
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn, String label){
		switch (funcRow) {
			case 0: {// row 1 of menu side bar buttons
				// {"Gen Training Data", "Save Training data","Load Training Data"}, //row 1
				switch (btn) {
					case 0: {
						mapMgr.loadPreProcTrainData(mapMgr.projConfigData.getPreProcDataDesiredSubDirName(), true);
						resetButtonState();
						break;
					}
					case 1: {
						mapMgr.saveAllPreProcExamples();
						resetButtonState();
						break;
					}
					case 2: {
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className+"(SOM_AnimWorldWin)", "launchMenuBtnHndlr", "Unknown Functions 1 btn : " + btn,
								MsgCodes.warning2);
						break;
					}
				}
				break;
			} // row 1 of menu side bar buttons
	
			case 1: {// row 2 of menu side bar buttons
				switch (btn) {
					case 0: {//calc optimal # of training examples
						mapMgr.calcOptNumObjsForDesiredProb(5, .95f);
						resetButtonState();
						break;
					}
					case 1: {//generate training data
						mapMgr.generateTrainingData();
						resetButtonState();
						break;
					}
					case 2: {//save current training data to disk, for use by SOM
						mapMgr.saveCurrentTrainData();
						resetButtonState();
						break;
					}
					case 3: {
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className+"(SOM_AnimWorldWin)", "launchMenuBtnHndlr", "Unknown Functions 2 btn : " + btn,
								MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 2 of menu side bar buttons
			case 2: {// row 3 of menu side bar buttons
				switch (btn) {
					case 0: {// show/hide som Map UI
						uiMgr.setPrivFlag(drawSOM_MapUIVis, !uiMgr.getPrivFlag(drawSOM_MapUIVis));
						resetButtonState();
						break;
					}
					case 1: {
						mapMgr.loadSOMConfig();// pass fraction of data to use for training
						resetButtonState();
						break;
					}
					case 2: {// currently does not display map
						mapMgr.loadTrainDataMapConfigAndBuildMap(false);
						resetButtonState();
						break;
					}					
					case 3: {
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className+"(SOM_AnimWorldWin)", "launchMenuBtnHndlr", "Unknown Functions 3 btn : " + btn,
								MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 3 of menu side bar buttons
			case 3: {// row 3 of menu side bar buttons
				switch (btn) {
					case 0:
					case 1:
					case 2:
					case 3: {// load all training data, default map config, and build map
						mapMgr.loadPretrainedExistingMap(btn, true);// runs in thread, button state reset there
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage(className+"(SOM_AnimWorldWin)", "launchMenuBtnHndlr", "Unknown Functions 4 btn : " + btn, MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 3 of menu side bar buttons
			default : {
				msgObj.dispWarningMessage(className+"(SOM_AnimWorldWin)","launchMenuBtnHndlr","Clicked Unknown Btn row : " + funcRow +" | Btn : " + btn);
				break;
			}
		}
	}

	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {somUIWin.handleSideMenuMseOvrDispSel(btn, val);}

	@Override
	protected final void handleSideMenuDebugSelEnable(int btn) {
		switch (btn) {
			case 0: {
				break;
			}
			case 1: {
				break;
			}
			case 2: {
				break;
			}
			case 3: {
				break;
			}
			case 4: {
				break;
			}
			default: {
				msgObj.dispMessage(className+"(SOM_AnimWorldWin)", "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
				break;
			}
		}
	}
	
	@Override
	protected final void handleSideMenuDebugSelDisable(int btn) {
		switch (btn) {
		case 0: {
			break;
		}
		case 1: {
			break;
		}
		case 2: {
			break;
		}
		case 3: {
			break;
		}
		case 4: {
			break;
		}
		default: {
			msgObj.dispMessage(className+"(SOM_AnimWorldWin)", "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
			resetButtonState();
			break;
		}
		}
	}

	/**
	 * called after the dimensions of the visible window have changed
	 */
	@Override
	protected final void setVisScreenDimsPriv() {}

	@Override
	protected final boolean hndlMouseMove_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld) {

		boolean res = false;
		// if(res) {return res;}
		if ((this.somUIWin != null) && (uiMgr.getPrivFlag(drawSOM_MapUIVis))) {
			res = somUIWin.handleMouseMove(mouseX, mouseY);
			if (res) {		return true;	}
		}
		return hndlMseMove_Priv(mouseX, mouseY, mseClckInWorld);
	}

	/**
	 * instance-specific mouse move handling
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param mseClckInWorld
	 * @return
	 */
	protected abstract boolean hndlMseMove_Priv(int mouseX, int mouseY, myPoint mseClckInWorld);

	// alt key pressed handles trajectory
	// cntl key pressed handles unfocus of spherey
	@Override
	protected final boolean hndlMouseClick_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		if ((this.somUIWin != null) && (uiMgr.getPrivFlag(drawSOM_MapUIVis))) {
			boolean res = somUIWin.handleMouseClick(mouseX, mouseY, mseBtn);
			if (res) {			return true;	}
		}
		return hndlMseClick_Priv(mouseX, mouseY, mseClckInWorld, mseBtn);
	}// hndlMouseClickIndiv

	/**
	 * instance-specific mouse click handling
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param mseClckInWorld
	 * @return
	 */
	protected abstract boolean hndlMseClick_Priv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn);

	@Override
	protected final boolean hndlMouseDrag_Indiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		//msgObj.dispInfoMessage(className,"hndlMouseDragIndiv","sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " +mseDragInWorld.toStrBrf());
//		if((privFlags[sphereSelIDX]) && (curSelSphere!="")){//pass drag through to selected sphere
//			//msgObj.dispInfoMessage(className,"hndlMouseDragIndiv","sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
//			res = sphereCntls.get(curSelSphere).hndlMouseDrag_Indiv(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D,curMseLookVec, mseDragInWorld);
//		}
		if (res) {			return res;		}
		// handleMouseDrag(int mouseX, int mouseY,int pmouseX, int pmouseY, myVector
		// mseDragInWorld, int mseBtn)
		if ((this.somUIWin != null) && (uiMgr.getPrivFlag(drawSOM_MapUIVis))) {
			res = somUIWin.handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY, mseDragInWorld, mseBtn);
			if (res) {				return true;			}
		}
		
		return hndlMseDrag_Priv(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D, mseDragInWorld, mseBtn);
	}

	/**
	 * instance-specific mouse drag handling
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param pmouseX
	 * @param pmouseY
	 * @param mouseClickIn3D
	 * @param mseDragInWorld
	 * @param mseBtn
	 * @return
	 */
	protected abstract boolean hndlMseDrag_Priv(int mouseX, int mouseY, int pmouseX, int pmouseY,myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn);

	@Override
	protected final void hndlMouseRel_Indiv() {
		if ((this.somUIWin != null) && (uiMgr.getPrivFlag(drawSOM_MapUIVis))) {			somUIWin.handleMouseRelease();		}
		hndlMseRelease_Priv();
	}

	/**
	 * instance-specific functionality for mouse release
	 */
	protected abstract void hndlMseRelease_Priv();
	@Override
	public final void processTraj_Indiv(DrawnSimpleTraj drawnNoteTraj) {	}
	@Override
	protected final void addSScrToWin_Indiv(int newWinKey) {	}
	@Override
	protected final void addTrajToScr_Indiv(int subScrKey, String newTrajKey) {	}
	@Override
	protected final void delSScrToWin_Indiv(int idx) {	}
	@Override
	protected final void delTrajToScr_Indiv(int subScrKey, String newTrajKey) {	}
	// resize drawn all trajectories
	@Override
	protected final void resizeMe(float scale) {	}
	@Override
	protected final void closeMe() {	}
	@Override
	protected final void showMe() {			setCustMenuBtnLabels();	}
	@Override
	protected final String[] getSaveFileDirNamesPriv() {		return new String[0];	}
	@Override
	public final void hndlFileLoad(File file, String[] vals, int[] stIdx) {	}
	@Override
	public final ArrayList<String> hndlFileSave(File file) {	return new ArrayList<String>();	}

}// SOM_AnimWorldWin
