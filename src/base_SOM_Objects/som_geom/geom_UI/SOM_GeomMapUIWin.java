package base_SOM_Objects.som_geom.geom_UI;

import java.util.ArrayList;
import java.util.TreeMap;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MapUIWin;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MseOvrDispTypeVals;
import base_UI_Objects.GUI_AppManager;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * window to manage mapMgr interaction - acts like a pop-up window, so is subordinate to a SOM_AnimWorldWin instance
 * @author john
 *
 */
public class SOM_GeomMapUIWin extends SOM_MapUIWin {
	/**
	 * geom window that references/manages this map window
	 */
	protected final SOM_AnimWorldWin animWin;
	
	public static final int 
		mapShowLocClrIDX 			= numSOMBasePrivFlags + 0;			//show img built of map with each pxl clr built from the 1st 3 features of the interpolated point at that pxl between the map nodes
	
	public final int _numPrivFlags = numSOMBasePrivFlags + 1;

	/**
	 * default args for building map manager
	 */
	protected TreeMap<String, Object> argsMap;
	
	public SOM_GeomMapUIWin(IRenderInterface _p, GUI_AppManager _AppMgr, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, TreeMap<String, Object> _argsMap, SOM_AnimWorldWin _animWin) {
		super(_p, _AppMgr, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);
		argsMap = _argsMap;
		animWin = _animWin;
		super.initThisWin(false);
	}

	/**
	 * set up child class button rectangles - override this so that we 
	 */
	@Override
	protected final void initUIBox(){		
		float [] menuUIClkCoords = animWin.uiClkCoords;
		msgObj.dispInfoMessage(className, "initUIBox", "Using animWin.uiClkCoords : y == "+ menuUIClkCoords[3] + " | height ==  "+menuUIClkCoords[3]);
		initUIClickCoords(menuUIClkCoords[0],menuUIClkCoords[3],menuUIClkCoords[2],menuUIClkCoords[3]);			
	}

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
	protected final int initAllSOMPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray) {
		tmpBtnNamesArray.add(new Object[] {"Showing Feature[0:2] Clr","Not Showing Feature[0:2] Clr", mapShowLocClrIDX});          
		return _numPrivFlags;
	}
	
	/**
	 * build instance-specific map manager
	 */
	@Override
	protected SOM_MapManager buildMapMgr() { 
		msgObj.dispInfoMessage(className, "buildMapMgr", "Entering buildMapMgr : magMgr is currently :"+ (null==mapMgr ? " null" : " not null"));
		if(this.mapMgr != null) {return mapMgr;}
		//no need to set win here - this is set in SOM Win UI Base class
		//this is just a place holder - windows will set proper map manager when this window is selected to be active
		return ((SOM_AnimWorldWin)AppMgr.getCurrentWindow()).getMapMgr();
	}

	/**
	 * instance-specific window initialization
	 */
	@Override
	protected void initMeIndiv() {
		//default to showing right side bar menu
		//setFlags(showRightSideMenu, true);	
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		//pa.setAllMenuBtnNames(menuBtnNames);	
		mapMgr.initMapAras(1, 1);
	}

	@Override
	protected void setInitValsForPrivFlags_Indiv() {		
	}

	/**
	 * Instance class determines the true and false labels the class-category locking should use
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of whether 
	 * category should be locked to allow selection through within-category classes
	 */
	@Override
	protected String[] getClassCatLockBtnTFLabels() {return new String[] {"Cat Changes with Class","Lock Cat; Class only in Cat"};}
	/**
	 * Instance class determines the true and false labels the class buttons use - if empty then no classes used
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of class-based segment
	 */
	@Override
	protected final String[] getClassBtnTFLabels() {	return new String[] {"Hide Classes ","Show Classes "};}
	/**
	 * Instance class determines the true and false labels the category buttons use - if empty then no categories used
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of category-based segment
	 */
	@Override
	protected final String[] getCategoryBtnTFLabels() {	return new String[] {"Hide Categories", "Show Categories"};}	
	
	/**
	 * This will return instance class-based true and false labels for save segment data.  if empty then no segment saving possible
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control saving of segment data
	 */
	@Override
	protected final String[] getSegmentSaveBtnTFLabels() {return new String[] {"Saving Cls, Cat, Ftr seg BMUs", "Save Cls, Cat, Ftr seg BMUs" };}
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
	@Override
	protected final void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals) {}
	@Override
	protected final void setVisScreenDimsPriv_Indiv() {}
	
	@Override
	protected void setPrivFlagsIndiv(int idx, boolean val) {
		switch (idx) {//special actions for each flag
			case mapShowLocClrIDX		: {//draw all map nodes, even empty
				break;}						
		}
	}
	
	
	@Override
	protected void drawOnScreenStuffPriv(float modAmtMillis) {}
	@Override
	//set flags that should be set on each frame - these are set at beginning of frame draw
	protected void drawSetDispFlags() {	}
	@Override
	protected void drawMapIndiv() {		}

	@Override
	protected void setUIWinValsIndiv(int UIidx) {}	
	
	@Override
	public void initDrwnTrajIndiv(){}
	@Override
	public void drawCustMenuObjs(){}
	@Override
	protected boolean simMe(float modAmtSec) {	return true;}
	//set camera to custom location - only used if dispFlag set
	@Override
	protected void setCameraIndiv(float[] camVals){	}
	@Override
	protected void stopMe() {}	

	/**
	 * build SOM_MseOvrDispTypeVals value based on which button was chosen
	 */
	@Override
	protected SOM_MseOvrDispTypeVals handleSideMenuMseOvrDisp_MapBtnToType(int btn, boolean val) {
		//based on : 
		//SOM_AnimWorldWin.MseOvrLblsAra = new String[]{"Loc","Dist","Pop","Ftr","Class","Cat","None"};
		switch(btn){
			case 0 : { return val ? SOM_MseOvrDispTypeVals.mseOvrMapNodeLocIDX : SOM_MseOvrDispTypeVals.mseOvrNoneIDX;} 	//"loc"
			case 1 : { return val ? SOM_MseOvrDispTypeVals.mseOvrUMatDistIDX : SOM_MseOvrDispTypeVals.mseOvrNoneIDX;} 	//u mat dist
			case 2 : { return val ? SOM_MseOvrDispTypeVals.mseOvrMapNodePopIDX : SOM_MseOvrDispTypeVals.mseOvrNoneIDX;}  //pop
			case 3 : { return val ? SOM_MseOvrDispTypeVals.mseOvrFtrIDX : SOM_MseOvrDispTypeVals.mseOvrNoneIDX;}         //ftr
			case 4 : { return val ? SOM_MseOvrDispTypeVals.mseOvrClassIDX : SOM_MseOvrDispTypeVals.mseOvrNoneIDX;}       //class
			case 5 : { return val ? SOM_MseOvrDispTypeVals.mseOvrCatIDX : SOM_MseOvrDispTypeVals.mseOvrNoneIDX;}         //category
			case 6 : { return SOM_MseOvrDispTypeVals.mseOvrNoneIDX;}        //none
			default : { return SOM_MseOvrDispTypeVals.mseOvrOtherIDX;}      //other/custom
		}
	}

	@Override
	public final void handleSideMenuDebugSelEnable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable","Click Debug functionality on in " + name + " : btn : " + btn, MsgCodes.info4);
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
				msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
				break;
			}
		}
		msgObj.dispMessage(className, "handleSideMenuDebugSel", "End Debug functionality on selection.",MsgCodes.info4);
	}
	
	@Override
	public final void handleSideMenuDebugSelDisable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable","Click Debug functionality off in " + name + " : btn : " + btn, MsgCodes.info4);
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
			msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
			resetButtonState();
			break;
		}
		}
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "End Debug functionality off selection.",MsgCodes.info4);
	}

	//handle mouseover 
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		boolean res = false;
		if(getPrivFlags(mapDataLoadedIDX)){ res = checkMouseOvr(mouseX, mouseY);	}
		return res;
	}	
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		boolean mod = false;	
		if(getPrivFlags(mapDataLoadedIDX)){ 
			//msgObj.dispInfoMessage(className, "hndlMouseClickIndiv", "In Mouse Click mx : " +mouseX+ " | my : " + mouseY+" | mseClckInWorld : " + mseClckInWorld.toStrBrf() + " | mseBtn : " +mseBtn + " | getPrivFlags(mapDataLoadedIDX) : "+getPrivFlags(mapDataLoadedIDX));
			mod = this.checkMouseClick(mouseX, mouseY, mseClckInWorld, mseBtn);
		}
//		if(mod) {return mod;}
//		else {return checkUIButtons(mouseX, mouseY);}
		return mod;
	}
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean mod = false;
		if(getPrivFlags(mapDataLoadedIDX)){ 
			//msgObj.dispInfoMessage(className, "hndlMouseDragIndiv", "In Mouse Drag mx : " +mouseX+ " | my : " + mouseY+" | pmx : " +pmouseX+ " | pmy : " + pmouseY+" | mouseClickIn3D : " + mouseClickIn3D.toStrBrf()+" | mseDragInWorld : " + mseDragInWorld.toStrBrf()+ " | mseBtn : " +mseBtn + " | getPrivFlags(mapDataLoadedIDX) : "+getPrivFlags(mapDataLoadedIDX));
			mod = this.checkMouseDragMove(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D, mseDragInWorld, mseBtn);
		}		
		return mod;
	}
	@Override
	protected void hndlMouseRelIndiv() {			
		if(getPrivFlags(mapDataLoadedIDX)){ 
			//msgObj.dispInfoMessage(className, "hndlMouseRelIndiv", "In Mouse Release");
			this.checkMouseRelease();
		}		
	}	
	
	@Override
	public String toString(){
		String res = super.toString();
		return res;
	}

	@Override
	protected String getCategoryUIObjLabel() {
		return null;
	}

	@Override
	protected String getClassUIObjLabel() {
		return null;
	}

	@Override
	protected void setPrivFlags_LockCatForClassSegs(boolean val) {}

	@Override
	protected int getCategoryFromClass(int _curCatIDX, int _classIDX) {
		return 0;
	}

	@Override
	protected int getClassFromCategory(int _catIDX, int _curClassIDX) {
		return 0;
	}

	@Override
	protected int getClassLabelFromIDX(int _idx) {
		return 0;
	}

	@Override
	protected int getCategoryLabelFromIDX(int _idx) {
		return 0;
	}

	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn) {	}

	@Override
	protected void setCustMenuBtnNames() {}

}//myTrajEditWin
