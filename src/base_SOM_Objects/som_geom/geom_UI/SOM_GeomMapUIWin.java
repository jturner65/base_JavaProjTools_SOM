package base_SOM_Objects.som_geom.geom_UI;

import java.util.HashMap;
import java.util.TreeMap;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Render_Interface.IRenderInterface;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MapUIWin;
import base_SOM_Objects.som_ui.win_disp_ui.SOM_MseOvrDispTypeVals;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.GUI_AppWinVals;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;
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
	protected HashMap<String, Object> argsMap;
	
	public SOM_GeomMapUIWin(IRenderInterface _p, GUI_AppManager _AppMgr, GUI_AppWinVals _winInitVals, HashMap<String, Object> _argsMap, SOM_AnimWorldWin _animWin) {
		super(_p, _AppMgr,_winInitVals);
		argsMap = _argsMap;
		animWin = _animWin;
		super.initThisWin(false);
	}
	
	/**
	 * Get the click coordinates formed by the parent
	 * @return
	 */
	@Override
	protected final float[] getParentWindowUIClkCoords() {
		float [] menuUIClkCoords = animWin.getUIClkCoords();
		msgObj.dispInfoMessage(className, "initUIBox", "Using animWin.uiClkCoords : y == "+ menuUIClkCoords[3] + " | height ==  "+menuUIClkCoords[3]);
		return new float[] {menuUIClkCoords[0],menuUIClkCoords[3],menuUIClkCoords[2],menuUIClkCoords[3]};
	}
	
	/**
	 * Initialize any UI control flags appropriate for specific instanced SOM mapUI window
	 */
	@Override
	protected final void initDispFlags_Indiv() {
		//disable right-side menu since the main animation window will handle showing/hiding that.
		dispFlags.setHasRtSideMenu(false);
	}
	
	/**
	 * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
	 */
	@Override
	public int getTotalNumOfPrivBools() {
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
		return ((SOM_AnimWorldWin)AppMgr.getCurFocusDispWindow()).getMapMgr();
	}

	/**
	 * instance-specific window initialization
	 */
	@Override
	protected void initMe_Indiv() {
		//default to showing right side bar menu
		//setFlags(showRightSideMenu, true);	
		//moved from mapMgr ctor, to remove dependence on papplet in that object
		//set offset to use for custom menu objects
		custMenuOffset = 0.0f;
		mapMgr.initMapAras(1, 1);
	}

	@Override
	protected void setInitValsForPrivFlags_Indiv() {}

	/**
	 * Instance class determines the true and false labels the class-category locking should use
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of whether 
	 * category should be locked to allow selection through within-category classes
	 */
	@Override
	protected String[] getClassCatLockBtnTFLabels() {return new String[]{"Cat Changes with Class","Lock Cat; Class only in Cat"};}
	
	/**
	 * Instance class determines the true and false labels the class buttons use - if empty then no classes used
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of class-based segment
	 */
	@Override
	protected final String[] getClassBtnTFLabels() {	return new String[]{"Hide Classes ","Show Classes "};}
	
	/**
	 * Instance class determines the true and false labels the category buttons use - if empty then no categories used
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control display of category-based segment
	 */
	@Override
	protected final String[] getCategoryBtnTFLabels() {	return new String[]{"Hide Categories", "Show Categories"};}	
	
	/**
	 * This will return instance class-based true and false labels for save segment data.  if empty then no segment saving possible
	 * @return array holding true(idx0) and false(idx1) labels for buttons to control saving of segment data
	 */
	@Override
	protected final String[] getSegmentSaveBtnTFLabels() {return new String[]{"Saving Cls, Cat, Ftr seg BMUs", "Save Cls, Cat, Ftr seg BMUs" };}	
	
	/**
	 * Build all UI objects to be shown in left side bar menu for this window. This is the first child class function called by initThisWin
	 * @param tmpUIObjMap : map of GUIObj_Params, keyed by unique string, with values describing the UI object
	 * 			- The object IDX                   
	 *          - A double array of min/max/mod values                                                   
	 *          - The starting value                                                                      
	 *          - The label for object                                                                       
	 *          - The object type (GUIObj_Type enum)
	 *          - A boolean array of behavior configuration values : (unspecified values default to false)
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *          - A boolean array of renderer format values :(unspecified values default to false) - Behavior Boolean array must also be provided!
	 * 				idx 0 : Should be multiline
	 * 				idx 1 : One object per row in UI space (i.e. default for multi-line and btn objects is false, single line non-buttons is true)
	 * 				idx 2 : Text should be centered (default is false)
	 * 				idx 3 : Object should be rendered with outline (default for btns is true, for non-buttons is false)
	 * 				idx 4 : Should have ornament
	 * 				idx 5 : Ornament color should match label color 
	 */
	protected final void setupGUIObjsAras_Indiv(TreeMap<String, GUIObj_Params> tmpUIObjMap) {}

	/**
	 * Build all UI buttons to be shown in left side bar menu for this window. This is for instancing windows to add to button region
	 * @param firstIdx : the first index to use in the map/as the objIdx
	 * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
	 * 				the first element is the object index
	 * 				the second element is true label
	 * 				the third element is false label
	 * 				the final element is integer flag idx 
	 */
	protected final void setupGUIBoolSwitchAras_Indiv(int firstIdx, TreeMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {
		int idx=firstIdx;
		tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Showing Feature[0:2] Clr","Not Showing Feature[0:2] Clr", mapShowLocClrIDX)); 
	}

	@Override
	protected final void setVisScreenDimsPriv_Indiv() {}
	
	@Override
	protected void setPrivFlags_Indiv(int idx, boolean val) {
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
	protected void drawMap_Indiv() {		}
	@Override
	protected boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean setUI_FloatValsCustom_Indiv(int UIidx, float ival, float oldVal) {
		// TODO Auto-generated method stub
		return false;
	}	
	@Override
	public void initDrwnTraj_Indiv(){}
	@Override
	public void drawCustMenuObjs(float animTimeMod){}
	@Override
	protected boolean simMe(float modAmtSec) {	return true;}
	//set camera to custom location - only used if dispFlag set
	@Override
	protected void setCamera_Indiv(float[] camVals){	}
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
				msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
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
			msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
			resetButtonState();
			break;
		}
		}
	}

	//handle mouseover 
	@Override
	protected boolean hndlMouseMove_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		boolean res = false;
		if(uiMgr.getPrivFlag(mapDataLoadedIDX)){ res = checkMouseOvr(mouseX, mouseY);	}
		return res;
	}	
	@Override
	protected boolean hndlMouseClick_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		boolean mod = false;	
		if(uiMgr.getPrivFlag(mapDataLoadedIDX)){ 
			//msgObj.dispInfoMessage(className, "hndlMouseClickIndiv", "In Mouse Click mx : " +mouseX+ " | my : " + mouseY+" | mseClckInWorld : " + mseClckInWorld.toStrBrf() + " | mseBtn : " +mseBtn + " | uiMgr.getPrivFlag(mapDataLoadedIDX) : "+uiMgr.getPrivFlag(mapDataLoadedIDX));
			mod = this.checkMouseClick(mouseX, mouseY, mseClckInWorld, mseBtn);
		}
//		if(mod) {return mod;}
//		else {return checkUIButtons(mouseX, mouseY);}
		return mod;
	}
	@Override
	protected boolean hndlMouseDrag_Indiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean mod = false;
		if(uiMgr.getPrivFlag(mapDataLoadedIDX)){ 
			//msgObj.dispInfoMessage(className, "hndlMouseDragIndiv", "In Mouse Drag mx : " +mouseX+ " | my : " + mouseY+" | pmx : " +pmouseX+ " | pmy : " + pmouseY+" | mouseClickIn3D : " + mouseClickIn3D.toStrBrf()+" | mseDragInWorld : " + mseDragInWorld.toStrBrf()+ " | mseBtn : " +mseBtn + " | uiMgr.getPrivFlag(mapDataLoadedIDX) : "+uiMgr.getPrivFlag(mapDataLoadedIDX));
			mod = this.checkMouseDragMove(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D, mseDragInWorld, mseBtn);
		}		
		return mod;
	}
	@Override
	protected void hndlMouseRel_Indiv() {			
		if(uiMgr.getPrivFlag(mapDataLoadedIDX)){ 
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

	/**
	 * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
	 * @param funcRow idx for button row
	 * @param btn idx for button within row (column)
	 * @param label label for this button (for display purposes)
	 */
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn, String label) {}

	@Override
	protected void setCustMenuBtnLabels() {}

	@Override
	public final void processTraj_Indiv(DrawnSimpleTraj drawnTraj) {}


}//myTrajEditWin
