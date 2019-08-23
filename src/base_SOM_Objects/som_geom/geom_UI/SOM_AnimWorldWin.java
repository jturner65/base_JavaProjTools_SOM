package base_SOM_Objects.som_geom.geom_UI;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_geom.SOM_GeomMapManager;
import base_SOM_Objects.som_geom.geom_examples.SOM_GeomObj;
import base_SOM_Objects.som_geom.geom_utils.geom_objs.SOM_GeomObjTypes;
import base_UI_Objects.my_procApplet;
import base_UI_Objects.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.myDispWindow;
import base_Utils_Objects.MyMathUtils;
import base_Utils_Objects.io.MsgCodes;
import base_Utils_Objects.vectorObjs.myPoint;
import base_Utils_Objects.vectorObjs.myVector;

/**
 * this class will instance a combined window to hold an animation world and a
 * map display window overlay
 * 
 * @author john
 *
 */

public abstract class SOM_AnimWorldWin extends myDispWindow {
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
	private int numPrivFlags;

	public static final int 
		debugAnimIDX 			= 0,				//debug
		
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

	// initial values
	public int numGeomObjs = 10, numSmplPointsPerObj = 200, numTrainingExamples = 40000, curSelGeomObjIDX = 0;
	// fraction of max count of binomial coefficient to set as # of training
	// examples to sample from objects + samples
	public double fractOfBinomialForBaseNumTrainEx = .001;

	// object type the instancing window manages
	public final SOM_GeomObjTypes geomObjType;

	// dimensions of SOM Map - hard coded to override setting from SOM Map UI Window
	// - need to set in window
	protected float[] SOMMapDims = new float[] { 834.8f, 834.8f };

	public String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar menu, or ignored
			{ "Load Geometry Data", "Save Geometry Data", "Build Training Data" }, // row 1
			{ "Save Train Data", "---", "---", "Show SOM Win" }, // row 3
			{ "Build Map", "LD SOM Config", "---", "---" }, // row 2
			{ "---", "---", "---", "---" }, { "---", "---", "---", "---", "---" } };

	public static final String[] MseOvrLblsAra = new String[] { "Loc", "Dist", "Pop", "Ftr", "Class", "Cat", "None" };

	public SOM_AnimWorldWin(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, SOM_GeomObjTypes _type) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
		initAndSetAnimWorldVals();
		geomObjType = _type;
	}
	

	@Override
	protected final void initMe() {
		// build map associated with this geometric experiment
		// perform in this window since SOM window is subordinate to this one

		mapMgr = buildGeom_SOMMapManager();

		// capable of using right side menu
		setFlags(drawRightSideMenu, true);
		// init specific sim flags
		initPrivFlags(numPrivFlags);
		// default setting is to show the geometric objects
		setPrivFlags(showFullSourceObjIDX, true);
		// default to show location as color
		setPrivFlags(useUIObjLocAsClrIDX, true);
		// set default to use unique training examples
		setPrivFlags(allTrainExUniqueIDX, true);

		pa.setAllMenuBtnNames(menuBtnNames);

		// instance-specific init
		initMe_Indiv();
		// build default objects in screen
		rebuildSourceGeomObjs();
	}

	

	public void setGeomMapUIWin(SOM_GeomMapUIWin _somUIWin) {
		somUIWin = _somUIWin;
		somUIWin.setUI_FeatureListVals(setUI_GeomObjFeatureListVals());
		somUIWin.setMapMgr(mapMgr);
		msgObj.dispInfoMessage("SOM_AnimWorldWin", "setGeomMapUIWin", "Setting somUIWin in " + name + " to be  : "
				+ somUIWin.name + " and somUIWin's mapMgr to one belonging to win : " + mapMgr.win.name);
	}

	protected abstract String[] setUI_GeomObjFeatureListVals();

	/**
	 * whenever som is shown or not
	 */
	protected void setSOM_MapUIWinState(boolean val) {
		if (null != somUIWin) {
			somUIWin.setFlags(myDispWindow.showIDX, val);
//			/this.setRectDimsY( somUIWin.getRectDim(1));
		}
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
	 * @return
	 */
	public abstract SOM_GeomMapManager buildGeom_SOMMapManager();

	/**
	 * return appropriate SOM Map Manager for this window
	 * 
	 * @return
	 */
	public final SOM_MapManager getMapMgr() {
		return mapMgr;
	}

	@Override
	public final void initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {

		// add an entry for each button, in the order they are wished to be displayed
		// true tag, false tag, btn IDX

		tmpBtnNamesArray.add(new Object[] { "Debugging", "Debug", debugAnimIDX });
		// UI",drawSOM_MapUIVis});
		tmpBtnNamesArray.add(new Object[] { "Regenerating " + geomObjType + " Objs","Regenerate " + geomObjType + " Objs", regenUIObjsIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing " + geomObjType + " Objects", "Show " + geomObjType + " Objects",	showFullSourceObjIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing " + geomObjType + " Sample Points","Show " + geomObjType + " Sample Points", showSamplePntsIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing Labels", "Show Labels", showUIObjLabelIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing Sample Labels", "Show Sample Labels", showUIObjSmplsLabelIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing Loc-based Color", "Showing Random Color", useUIObjLocAsClrIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing " + geomObjType + " Training Exs",	"Show " + geomObjType + " Training Exs", showFullTrainingObjIDX });

		tmpBtnNamesArray.add(new Object[] { "Hi-Light Sel " + geomObjType + " ", "Enable " + geomObjType + " Hi-Light",	showSelUIObjIDX });
		// tmpBtnNamesArray.add(new Object[]{"Train From " +geomObjType + " Samples",
		// "Train From " +geomObjType + " Centers/Bases", useSmplsForTrainIDX});
		// tmpBtnNamesArray.add(new Object[]{"Save Data", "Save Data",
		// saveUIObjDataIDX});
		tmpBtnNamesArray.add(new Object[] { "Gen Unique " + geomObjType + " Train Exs",	"Allow dupe " + geomObjType + " Train Exs", allTrainExUniqueIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing Map Node Geometry", "Show Map Node Geometry", drawMapNodeGeomObjsIDX });
		tmpBtnNamesArray.add(new Object[] { "Showing BMU-derived Locs", "Showing Actual Locs", showMapBasedLocsIDX });

		String[] showWFObjsTFLabels = getShowWireFrameBtnTFLabels();
		if ((null != showWFObjsTFLabels) && (showWFObjsTFLabels.length == 2)) {
			tmpBtnNamesArray.add(new Object[] { showWFObjsTFLabels[0], showWFObjsTFLabels[1], showObjByWireFrmIDX });
		}

		// add instancing-class specific buttons - returns total # of private flags in
		// instancing class
		numPrivFlags = initAllAnimWorldPrivBtns_Indiv(tmpBtnNamesArray);
		
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
		msgObj.dispInfoMessage("SOM_AnimWorldWin", "rebuildSourceGeomObjs",	"Start (re)building all objects of type " + this.geomObjType);
		setMapMgrGeomObjVals();
		((SOM_GeomMapManager) mapMgr).buildGeomExampleObjs();
		msgObj.dispInfoMessage("SOM_AnimWorldWin", "rebuildSourceGeomObjs",	"Finished (re)building all objects of type " + this.geomObjType);
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
		msgObj.dispInfoMessage("SOM_AnimWorldWin", "regenBaseGeomObjSamples", "Start regenerating "	+ numSmplPointsPerObj + " samples for all base objects of type " + this.geomObjType);
		setMapMgrGeomObjVals();
		((SOM_GeomMapManager) mapMgr).rebuildGeomExObjSamples();
		msgObj.dispInfoMessage("SOM_AnimWorldWin", "regenBaseGeomObjSamples", "Finished regenerating "	+ numSmplPointsPerObj + " samples for all base objects of type " + this.geomObjType);
	}

	@Override
	public final void setPrivFlags(int idx, boolean val) {
		int flIDX = idx / 32, mask = 1 << (idx % 32);
		privFlags[flIDX] = (val ? privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch (idx) {// special actions for each flag
		case debugAnimIDX: {			break;		}
		case showSamplePntsIDX: {		break;		} // show/hide object's sample points
		case showFullSourceObjIDX: {	break;		} // show/hide full object
		case showFullTrainingObjIDX: {	break;		}
		// case saveUIObjDataIDX : {
		// if(val){saveGeomObjInfo();addPrivBtnToClear(saveUIObjDataIDX);}break;} //save
		// all object data
		case showUIObjLabelIDX: {		break;		} // show labels for objects
		case showUIObjSmplsLabelIDX: {	break;		}
		case useUIObjLocAsClrIDX: {
			msgObj.dispInfoMessage("SOM_AnimWorldWin", "setPrivFlags :: useUIObjLocAsClrIDX", "Val :  " + val);
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
		default: {			setPrivFlags_Indiv(idx, val);}
		}
	}// setPrivFlags

	/**
	 * set values for instancing class-specific boolean flags
	 * 
	 * @param idx
	 * @param val
	 */
	protected abstract void setPrivFlags_Indiv(int idx, boolean val);

	@Override
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals) {
		
		// object array of elements of following format :
		// the first element double array of min/max/mod values
		// the 2nd element is starting value
		// the 3rd elem is label for object
		// the 4th element is boolean array of {treat as int, has list values, value is sent to owning window}
		int minNumObjs = getMinNumObjs(), maxNumObjs = getMaxNumObjs(),	diffNumObjs = (maxNumObjs - minNumObjs > 100 ? 10 : 1);
		numGeomObjs = minNumObjs;
		tmpUIObjArray.put(gIDX_NumUIObjs,new Object[] { new double[] { minNumObjs, maxNumObjs, diffNumObjs }, (double) (numGeomObjs * 1.0),"# of " + geomObjType + " Objects", new boolean[] { true, false, true } }); // gIDX_NumUIObjs
		int minNumSmplsPerObj = getMinNumSmplsPerObj(), maxNumSmplsPerObj = getMaxNumSmplsPerObj(),	diffNumSmplsPerObj = (maxNumSmplsPerObj - minNumSmplsPerObj > 100 ? 10 : 1);
		numSmplPointsPerObj = minNumSmplsPerObj;
		tmpUIObjArray.put(gIDX_NumUISamplesPerObj, new Object[] { new double[] { minNumSmplsPerObj, maxNumSmplsPerObj, diffNumSmplsPerObj },(double) (numSmplPointsPerObj), "# of samples per Object", new boolean[] { true, false, true } }); // gIDX_NumUISamplesPerObj
		// gIDX_FractNumTrainEx fractOfBinomialForBaseNumTrainEx
		tmpUIObjArray.put(gIDX_FractNumTrainEx, new Object[] { new double[] { 0.00001, 1.000, 0.00001 }, fractOfBinomialForBaseNumTrainEx,"Fract of Binomial for Train Ex", new boolean[] { false, false, true } }); // gIDX_FractNumTrainEx

		// gIDX_NumTraingEx
		long minNumTrainingExamples = getNumTrainingExamples(minNumObjs, minNumSmplsPerObj),maxNumTrainingExamples = 10 * minNumTrainingExamples,diffNumTrainingEx = (maxNumTrainingExamples - minNumTrainingExamples) > 1000 ? 1000 : 10;
		numTrainingExamples = (int) minNumTrainingExamples;
		tmpUIObjArray.put(gIDX_NumTrainingEx, new Object[] { new double[] { minNumTrainingExamples, maxNumTrainingExamples, diffNumTrainingEx },(double) (numTrainingExamples),	"Ttl # of Train Ex [" + minNumTrainingExamples + ", " + maxNumTrainingExamples + "]",new boolean[] { true, false, true } }); // gIDX_NumUISamplesPerObj
		tmpUIObjArray.put(gIDX_SelDispUIObj, new Object[] { new double[] { 0, numGeomObjs - 1, 1 }, (double) (curSelGeomObjIDX),"ID of Object to Select", new boolean[] { true, false, true } }); // gIDX_SelDispUIObj

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
		// binomial coefficient - n (total # of samples across all objects) choose k
		// (dim of minimal defining set of each object)
		long newMaxVal = getNumTrainingExamples(numGeomObjs, numSmplPointsPerObj);
		guiObjs[gIDX_NumTrainingEx].setNewMax(newMaxVal);
		int minVal = (int) guiObjs[gIDX_NumTrainingEx].getMinVal();
		guiObjs[gIDX_NumTrainingEx].setNewDispText("Ttl # of Train Ex [" + minVal + ", " + newMaxVal + "]");
		double curNum = guiObjs[gIDX_NumTrainingEx].getVal();
		if (curNum < minVal) {
			guiObjs[gIDX_NumTrainingEx].setVal(minVal);
		}
		if (curNum > newMaxVal) {
			guiObjs[gIDX_NumTrainingEx].setVal(newMaxVal);
		}
	}// refreshNumTrainingExampleBounds

	private void refreshNumTrainingExamples() {
		long TtlNumExamples = getNumTrainingExamples(numGeomObjs, numSmplPointsPerObj);
		double newVal = fractOfBinomialForBaseNumTrainEx * TtlNumExamples;
		guiObjs[gIDX_NumTrainingEx].setVal(newVal);
		setUIWinVals(gIDX_NumTrainingEx);
	}

	@Override
	protected final void setUIWinVals(int UIidx) {
		float val = (float) guiObjs[UIidx].getVal();
		int ival = (int) val;

		switch (UIidx) {
		case gIDX_NumUIObjs: {
			if (ival != numGeomObjs) {
				numGeomObjs = ival;
				guiObjs[gIDX_SelDispUIObj].setNewMax(ival - 1);
				refreshNumTrainingExampleBounds();
				rebuildSourceGeomObjs();
			}
			break;
		}
		case gIDX_NumUISamplesPerObj: {
			if (ival != numSmplPointsPerObj) {
				numSmplPointsPerObj = ival;
				refreshNumTrainingExampleBounds();
				regenBaseGeomObjSamples();
			}
			break;
		}
		case gIDX_FractNumTrainEx: { // fraction of total # of possible samples in current configuration to use for
										// training examples
			if (val != fractOfBinomialForBaseNumTrainEx) {
				fractOfBinomialForBaseNumTrainEx = val;
				refreshNumTrainingExamples();
			}
			break;
		}
		case gIDX_NumTrainingEx: {
			if (ival != numTrainingExamples) {		numTrainingExamples = ival;		}
			setMapMgrGeomObjVals();
			break;
		}
		case gIDX_SelDispUIObj: {
			if (ival != curSelGeomObjIDX) {
				curSelGeomObjIDX = MyMathUtils.min(ival, numGeomObjs - 1);
			} // don't select a sphere Higher than the # of spheres
			break;
		}
		default: {
			setUIWinVals_Indiv(UIidx, val);
		}
		}
	}

	/**
	 * For instance-class specific ui values
	 * 
	 * @param UIidx
	 */
	protected abstract void setUIWinVals_Indiv(int UIidx, float val);

	/**
	 * override this since no close box support
	 */
	@Override
	protected boolean checkClsBox(int mouseX, int mouseY) {
		return false;
	}

	/**
	 * instancing class-specific functionality
	 */
	protected abstract void initMe_Indiv();

	/**
	 * get ui values used to build current geometry (for preproc save)
	 * 
	 * @return
	 */
	public TreeMap<String, String> getAllUIValsForPreProcSave() {
		TreeMap<String, String> res = new TreeMap<String, String>();
		res.put("gIDX_NumUIObjs", String.format("%4d", (int) guiObjs[gIDX_NumUIObjs].getVal()));
		res.put("gIDX_NumUISamplesPerObj", String.format("%4d", (int) guiObjs[gIDX_NumUISamplesPerObj].getVal()));
		res.put("gIDX_FractNumTrainEx", String.format("%.4f", guiObjs[gIDX_FractNumTrainEx].getVal()));
		res.put("gIDX_NumTrainingEx", String.format("%4d", (int) guiObjs[gIDX_NumTrainingEx].getVal()));

		getAllUIValsForPreProcSave_Indiv(res);
		return res;

	}

	/**
	 * get instance-class specific ui values used to build current geometry (for
	 * preproc save)
	 * 
	 * @return
	 */
	protected abstract void getAllUIValsForPreProcSave_Indiv(TreeMap<String, String> vals);

	/**
	 * set ui values used to build preproc data being loaded
	 * 
	 * @return
	 */
	public void setAllUIValsFromPreProcLoad(TreeMap<String, String> uiVals) {
		guiObjs[gIDX_FractNumTrainEx].setVal(Double.parseDouble(uiVals.get("gIDX_FractNumTrainEx")));
		guiObjs[gIDX_NumUIObjs].setVal(Integer.parseInt(uiVals.get("gIDX_NumUIObjs")));
		guiObjs[gIDX_NumUISamplesPerObj].setVal(Integer.parseInt(uiVals.get("gIDX_NumUISamplesPerObj")));
		guiObjs[gIDX_NumTrainingEx].setVal(Integer.parseInt(uiVals.get("gIDX_NumTrainingEx")));

		setAllUIValsFromPreProcLoad_Indiv(uiVals);
		setAllUIWinVals();
	}

	/**
	 * set ui instance-specific values used to build preproc data being loaded
	 * 
	 * @return
	 */
	protected abstract void setAllUIValsFromPreProcLoad_Indiv(TreeMap<String, String> uiVals);

/////////////////////////////
	// drawing routines
	@Override
	protected final void setCameraIndiv(float[] camVals) {
		// , float rx, float ry, float dz are now member variables of every window
		pa.camera(camVals[0], camVals[1], camVals[2], camVals[3], camVals[4], camVals[5], camVals[6], camVals[7], camVals[8]);
		// puts origin of all drawn objects at screen center and moves forward/away by dz
		pa.translate(camVals[0], camVals[1], (float) dz);
		setCamOrient();
	}

	/**
	 * draw the SOM Window
	 * 
	 * @param modAmtMillis
	 */
	public void drawSOMWinUI(float modAmtMillis) {
		if (null != somUIWin) {
			somUIWin.draw2D(modAmtMillis);
			somUIWin.drawHeader(modAmtMillis);
		}
	}

	@Override
	protected final void drawMe(float animTimeMod) {
		pa.pushMatrix();
		pa.pushStyle();// nested ifthen shenannigans to get rid of if checks in each individual draw
		drawMeFirst_Indiv();
		// check if geom objs are built in mapMgr
		// mapMgr.drawSrcObjsInUIWindow(pa, animTimeMod, curSelGeomObjIDX,
		// getPrivFlags(showMapBasedLocsIDX));
		if (mapMgr.getGeomObjsBuilt()) {
			boolean wantDrawBMUs = getPrivFlags(showMapBasedLocsIDX);
			boolean shouldDrawBMUs = (wantDrawBMUs && getPrivFlags(mapBuiltToCurUIObjsIDX));
			if (!shouldDrawBMUs && wantDrawBMUs) {
				setPrivFlags(showMapBasedLocsIDX, false);
				wantDrawBMUs = false;
			}

			_drawObjs(mapMgr.sourceGeomObjects, curSelGeomObjIDX, animTimeMod, shouldDrawBMUs,
					getPrivFlags(showSamplePntsIDX), getPrivFlags(showFullSourceObjIDX),
					getPrivFlags(useUIObjLocAsClrIDX), getPrivFlags(showSelUIObjIDX), getPrivFlags(showObjByWireFrmIDX),
					getPrivFlags(showUIObjLabelIDX), getPrivFlags(showUIObjSmplsLabelIDX));
		}
		// check if train samples are built in map mgr
		if ((mapMgr.getTrainDataObjsBuilt()) && (getPrivFlags(showFullTrainingObjIDX))) {
			// mapMgr.drawSynthObjsInUIWindow(pa, animTimeMod,
			// getPrivFlags(showMapBasedLocsIDX));
			_drawObjs(mapMgr.trainDatGeomObjects, -1, animTimeMod, false, false, true,
					getPrivFlags(useUIObjLocAsClrIDX), false, getPrivFlags(showObjByWireFrmIDX),
					getPrivFlags(showUIObjLabelIDX), false);
		} else {
			setPrivFlags(showFullTrainingObjIDX, false);
		}
		// else { msgObj.dispInfoMessage("SOM_AnimWorldWin", "drawMe", "ui obj data
		// loaded is false");}
		// draw geom objects for selected map node objects
		if (getPrivFlags(drawMapNodeGeomObjsIDX)) {
			mapMgr.drawSelectedMapNodeGeomObjs(pa, animTimeMod, getPrivFlags(showUIObjLabelIDX),
					getPrivFlags(useUIObjLocAsClrIDX));
		}
		drawMeLast_Indiv();
		pa.popStyle();
		pa.popMatrix();

	}// drawMe

	private void _drawObjs(SOM_GeomObj[] objs, int curSelObjIDX, float animTimeMod, boolean mapBuiltAndUseMapLoc,
			boolean showSmpls, boolean showObjs, boolean useLocClr, boolean showSel, boolean showWireFrame,
			boolean showLabel, boolean showSmplsLabel) {
		if (mapBuiltAndUseMapLoc) { // show bmus for objs
			if (showSel) {// if selected, show object filled with chosen color and show all other objects wireframe
				if (useLocClr) {	objs[curSelObjIDX].drawMeSelected_ClrLoc_BMU(pa, animTimeMod, showSmpls);} 
				else {				objs[curSelObjIDX].drawMeSelected_ClrRnd_BMU(pa, animTimeMod, showSmpls);}
				if (showLabel) {	objs[curSelObjIDX].drawMyLabel_BMU(pa, this);}

				if (showObjs) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrLoc_WF_BMU(pa);}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeClrLoc_WF_BMU(pa);}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrRnd_WF_BMU(pa);}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeClrRnd_WF_BMU(pa);}
					} // rand color
				}
				if (showSmpls) {
					if (useLocClr) {for (int i = 0; i < curSelObjIDX; ++i) {	objs[i].drawMeSmpls_ClrLoc_BMU(pa);	}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeSmpls_ClrLoc_BMU(pa);	}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrRnd_BMU(pa);	}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeSmpls_ClrRnd_BMU(pa);	}
					} // rand color
				}
			} else {
				if (showObjs) {		_drawObjs_UseBMUs(objs, useLocClr, showWireFrame, showLabel);}
				if (showSmpls) {
					if (useLocClr) {	for (int i = 0; i < objs.length; ++i) {					objs[i].drawMeSmpls_ClrLoc_BMU(pa);	}} // loc color
					else {				for (int i = 0; i < objs.length; ++i) {					objs[i].drawMeSmpls_ClrRnd_BMU(pa);	}} // rand color
				}
			}
		} else {
			if (showSel) {// if selected, show selected object filled with chosen color and show all other objects wireframe with or without samples and no labels
				if (useLocClr) {			objs[curSelObjIDX].drawMeSelected_ClrLoc(pa, animTimeMod, showSmpls);} 
				else {						objs[curSelObjIDX].drawMeSelected_ClrRnd(pa, animTimeMod, showSmpls);}
				if (showLabel) {			objs[curSelObjIDX].drawMyLabel(pa, this);}
				// all other objects default to wireframe display
				if (showObjs) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrLoc_WF(pa);}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeClrLoc_WF(pa);}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeClrRnd_WF(pa);}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeClrRnd_WF(pa);}
					} // rand color
				}
				if (showSmpls) {
					if (useLocClr) {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrLoc(pa);	}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeSmpls_ClrLoc(pa);	}
					} // loc color
					else {
						for (int i = 0; i < curSelObjIDX; ++i) {				objs[i].drawMeSmpls_ClrRnd(pa);	}
						for (int i = curSelObjIDX + 1; i < objs.length; ++i) {	objs[i].drawMeSmpls_ClrRnd(pa);	}
					} // rand color
				}

			} else {
				if (showObjs) {				_drawObjs_UseActual(objs, animTimeMod, useLocClr, showSmpls, showWireFrame, showLabel);}
				if (showSmpls) {
					if (useLocClr) {		for (int i = 0; i < objs.length; ++i) {					objs[i].drawMeSmpls_ClrLoc(pa);	}} // loc color
					else {					for (int i = 0; i < objs.length; ++i) {					objs[i].drawMeSmpls_ClrRnd(pa);	}} // rand color
					if (showSmplsLabel) {	for (int i = 0; i < objs.length; ++i) {					objs[i].drawMySmplsLabel(pa, this);}}
				}
			}
		}
	}// _drawObjs

	private void _drawObjs_UseActual(SOM_GeomObj[] objs, float animTimeMod, boolean useLocClr, boolean showSmpls,
			boolean showWireFrame, boolean showLabel) {
		if (showWireFrame) { // draw objects with wire frames
			if (useLocClr) {	for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrLoc_WF(pa);}} // loc color
			else {				for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrRnd_WF(pa);}} // rand color
		} else {
			if (useLocClr) {	for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrLoc(pa);}} // loc color
			else {				for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrRnd(pa);}} // rand color
		}
		if (showLabel) {		for (int i = 0; i < objs.length; ++i) {		objs[i].drawMyLabel(pa, this);}}
	}// _drawObjs_UseActual

	private void _drawObjs_UseBMUs(SOM_GeomObj[] objs, boolean useLocClr, boolean showWireFrame, boolean showLabel) {
		if (showWireFrame) { // draw objects with wire frames
			if (useLocClr) {	for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrLoc_WF_BMU(pa);}} // loc color
			else {				for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrRnd_WF_BMU(pa);}} // rand color
		} else {
			if (useLocClr) {	for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrLoc_BMU(pa);}} // loc color
			else {				for (int i = 0; i < objs.length; ++i) {	objs[i].drawMeClrRnd_BMU(pa);}} // rand color
		}
		if (showLabel) {		for (int i = 0; i < objs.length; ++i) {	objs[i].drawMyLabel_BMU(pa, this);}}
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
	public final void drawCustMenuObjs() {
		// ((SOM_GeometryMain) pa).drawSOMUIObjs();
		// if(this.getPrivFlags(drawSOM_MapUIVis)) {
		if (somUIWin != null) {
			pa.pushMatrix();
			pa.pushStyle();
			somUIWin.drawGUIObjs(); // draw what user-modifiable fields are currently available
			somUIWin.drawClickableBooleans(); // draw what user-modifiable boolean buttons
			pa.popStyle();
			pa.popMatrix();
		}
	}

	@Override
	// draw 2d constructs over 3d area on screen - draws behind left menu section
	// modAmtMillis is in milliseconds
	protected final void drawRightSideInfoBarPriv(float modAmtMillis) {
		pa.pushMatrix();		pa.pushStyle();
		//instance-specific
		float newYOff = drawRightSideInfoBar_Indiv(modAmtMillis, yOff);
		// display current simulation variables - call sim world through sim exec
		mapMgr.drawResultBar(pa, newYOff);
		pa.popStyle();		pa.popMatrix();
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

	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn) {
		msgObj.dispMessage("SOM_AnimWorldWin", "launchMenuBtnHndlr", "Begin requested action : Click Functions "+(funcRow+1)+" in " + name + " : btn : " + btn, MsgCodes.info4);
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
						mapMgr.generateTrainingData();
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage("SOM_AnimWorldWin", "launchMenuBtnHndlr", "Unknown Functions 1 btn : " + btn,
								MsgCodes.warning2);
						break;
					}
				}
				break;
			} // row 1 of menu side bar buttons
	
			case 1: {// row 2 of menu side bar buttons
				switch (btn) {
					case 0: {
						mapMgr.saveCurrentTrainData();
						resetButtonState();
						break;
					}
					case 1: {
						mapMgr.calcOptNumObjsForDesiredProb(10, .95f);
						resetButtonState();
						break;
					}
					case 2: {
						resetButtonState();
						break;
					}
					case 3: {// show/hide som Map UI
						setPrivFlags(drawSOM_MapUIVis, !getPrivFlags(drawSOM_MapUIVis));
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage("SOM_AnimWorldWin", "launchMenuBtnHndlr", "Unknown Functions 2 btn : " + btn,
								MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 2 of menu side bar buttons
			case 2: {// row 3 of menu side bar buttons
				switch (btn) {
					case 0: {
						mapMgr.loadTrainDataMapConfigAndBuildMap(false);
						resetButtonState();
						break;
					}
					case 1: {
						mapMgr.loadSOMConfig();// pass fraction of data to use for training
						resetButtonState();
						break;
					}
					case 2: {
						resetButtonState();
						break;
					}
					case 3: {
						resetButtonState();
						break;
					}
					default: {
						msgObj.dispMessage("SOM_AnimWorldWin", "launchMenuBtnHndlr", "Unknown Functions 3 btn : " + btn,
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
						msgObj.dispMessage("SOM_AnimWorldWin", "launchMenuBtnHndlr", "Unknown Functions 4 btn : " + btn, MsgCodes.warning2);
						resetButtonState();
						break;
					}
				}
				break;
			} // row 3 of menu side bar buttons
		}
		msgObj.dispMessage("SOM_AnimWorldWin", "launchMenuBtnHndlr", "End requested action (multithreaded actions may still be working) : Click Functions "+(funcRow+1)+" in " + name + " : btn : " + btn, MsgCodes.info4);
	}

	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {somUIWin.handleSideMenuMseOvrDispSel(btn, val);}

	@Override
	public final void handleSideMenuDebugSel(int btn, int val) {
		msgObj.dispMessage("SOM_AnimWorldWin", "handleSideMenuDebugSel","Click Debug functionality in " + name + " : btn : " + btn, MsgCodes.info4);
		switch (btn) {
			case 0: {
				resetButtonState();
				break;
			}
			case 1: {
				resetButtonState();
				break;
			}
			case 2: {
				resetButtonState();
				break;
			}
			case 3: {// show current mapdat status
				resetButtonState();
				break;
			}
			case 4: {
				resetButtonState();
				break;
			}
			default: {
				msgObj.dispMessage("SOM_AnimWorldWin", "handleSideMenuDebugSel", "Unknown Debug btn : " + btn,MsgCodes.warning2);
				resetButtonState();
				break;
			}
		}
		msgObj.dispMessage("SOM_AnimWorldWin", "handleSideMenuDebugSel", "End Debug functionality selection.",MsgCodes.info4);
	}

	/**
	 * called after the dimensions of the visible window have changed
	 */
	@Override
	protected final void setVisScreenDimsPriv() {
		// float xStart = rectDim[0] + .5f*(curVisScrDims[0] -
		// (curVisScrDims[1]-(2*xOff)));

	}

	@Override
	protected final boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld) {

		boolean res = false;
		// if(res) {return res;}
		if ((this.somUIWin != null) && (getPrivFlags(drawSOM_MapUIVis))) {
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
	protected final boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		if ((this.somUIWin != null) && (getPrivFlags(drawSOM_MapUIVis))) {
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
	protected final boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		// pa.outStr2Scr("hndlMouseDragIndiv sphere ui drag in world mouseClickIn3D : "
		// + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " +
		// mseDragInWorld.toStrBrf());
//		if((privFlags[sphereSelIDX]) && (curSelSphere!="")){//pass drag through to selected sphere
//			//pa.outStr2Scr("sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
//			res = sphereCntls.get(curSelSphere).hndlMouseDragIndiv(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D,curMseLookVec, mseDragInWorld);
//		}
		if (res) {			return res;		}
		// handleMouseDrag(int mouseX, int mouseY,int pmouseX, int pmouseY, myVector
		// mseDragInWorld, int mseBtn)
		if ((this.somUIWin != null) && (getPrivFlags(drawSOM_MapUIVis))) {
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
	protected final void hndlMouseRelIndiv() {
		if ((this.somUIWin != null) && (getPrivFlags(drawSOM_MapUIVis))) {			somUIWin.handleMouseRelease();		}
		hndlMseRelease_Priv();
	}

	/**
	 * instance-specific functionality for mouse release
	 */
	protected abstract void hndlMseRelease_Priv();
	@Override
	protected final void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj) {	}
	@Override
	protected final void addSScrToWinIndiv(int newWinKey) {	}
	@Override
	protected final void addTrajToScrIndiv(int subScrKey, String newTrajKey) {	}
	@Override
	protected final void delSScrToWinIndiv(int idx) {	}
	@Override
	protected final void delTrajToScrIndiv(int subScrKey, String newTrajKey) {	}
	// resize drawn all trajectories
	@Override
	protected final void resizeMe(float scale) {	}
	@Override
	protected final void closeMe() {	}
	@Override
	protected final void showMe() {		setCustMenuBtnNames();	}
	@Override
	protected final String[] getSaveFileDirNamesPriv() {		return new String[0];	}
	@Override
	public final void hndlFileLoad(File file, String[] vals, int[] stIdx) {	}
	@Override
	public final ArrayList<String> hndlFileSave(File file) {	return new ArrayList<String>();	}

}// SOM_AnimWorldWin
