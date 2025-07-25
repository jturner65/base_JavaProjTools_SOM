package base_SOM_Objects.som_ui.win_disp_ui;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Render_Interface.IRenderInterface;
import base_SOM_Objects.som_examples.enums.SOM_ExDataType;
import base_SOM_Objects.som_examples.enums.SOM_FtrDataType;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_ui.uiData.SOMWinUIDataUpdater;
import base_SOM_Objects.som_utils.SOM_MapDat;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.base.GUI_AppWinVals;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;
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
        //idx 0 is debug in privFlags structure
        buildSOMExe                     = 1, //command to initiate SOM-building
        resetMapDefsIDX                = 2, //reset default UI values for map
        mapDataLoadedIDX                = 3, //whether map has been loaded or not    
        mapUseChiSqDistIDX            = 4, //whether to use chi-squared (weighted by variance) distance for features or regular euclidean dist
        mapExclProdZeroFtrIDX        = 5, //whether or not distances between two datapoints assume that absent features in source data point should be zero or ignored when comparing to map node ftrs

        //display/interaction
        mapDrawTrainDatIDX            = 6, //draw training examples
        mapDrawTestDatIDX             = 7, //draw testing examples - data held out and not used to train the map 
        mapDrawNodeLblIDX            = 8, //draw labels for nodes
        mapDrawNodesWith0MapExIDX    = 9, //draw nodes that have no mapped examples
        mapDrawWtMapNodesIDX            = 10, //draw map nodes with non-0 (present) ftr vals for currently selected ftr
        mapDrawPopMapNodesIDX           = 11, //draw map nodes that are bmus for training examples, with size logarithmically proportional to pop size
        mapDrawAllMapNodesIDX        = 12, //draw all map nodes, even empty
        drawMapNodePopGraphIDX        = 13, //draw graph next to SOM Map display showing map node population curve, with x axis being proportional to population, and y axis holding each node
        
        //UMatrix         
        mapDrawUMatrixIDX            = 14, //draw visualization of u matrix - distance between nodes
        mapDrawUMatSegImgIDX            = 15, //draw the image of the interpolated segments based on UMatrix Distance
        mapDrawUMatSegMembersIDX        = 16, //draw umatrix-based segments around regions of maps - visualizes clusters with different colors
        
        //ftr and ftr-dist-based
        mapDrawDistImageIDX            = 17, //draw umatrix-like rendering based on sq dist between adjacent node vectors
        mapDrawFtrWtSegMembersIDX    = 18, //draw ftr-wt-based segments around regions of map - display only segment built from currently display ftr on ftr map
        
        //class and category-based segments
        mapDrawClassSegmentsIDX        = 19, //show class segments
        mapDrawCategorySegmentsIDX    = 20, //show category (collection of classes) segments
        _categoryCanBeShownIDX        = 21, //whether category values are used and can be shown on UI/interracted with
        _classCanBeShownIDX            = 22, //whether class values are used and can be shown on UI/interracted with
        mapLockClassCatSegmentsIDX      = 23, //lock category to cycle through classes
        
        showSelRegionIDX                = 24, //highlight a specific region of the map, i.e. all nodes above a certain threshold for a chosen ftr
        //train/test data management
        somTrainDataLoadedIDX        = 25, //whether data used to build map has been loaded yet
        saveLocClrImgIDX                = 26, //
        //save segment mappings
        saveAllSegmentMapsIDX        = 27;            //this will save all the segment mappings that have been defined
    
    public static final int numSOMBasePrivFlags = 28;
    //instancing class will determine numPrivFlags based on how many more flags are added
    
    /**
     * # of priv flags from base class and instancing class
     */
    //private int numPrivFlags;
    
    //    //GUI Objects    
    public final static int 
        gIDX_TrainDataNormIDX        = 0, //normalization that feature data should have: unnormalized, norm per feature across all data, normalize each example
        gIDX_BMU_DispDataFrmtIDX         = 1, //format of vectors to use when comparing examples to nodes on map
        gIDX_TrainDatPartIDX             = 2, //partition % of training data out of total data (rest is testing)
                                     
        gIDX_MapRowsIDX                  = 3,            //map rows
        gIDX_MapColsIDX                = 4, //map cols
        gIDX_MapEpochsIDX            = 5, //# of training epochs
        gIDX_MapShapeIDX                 = 6, //hexagonal or rectangular
        gIDX_MapBndsIDX                = 7, //planar or torroidal bounds
        gIDX_MapKTypIDX                = 8, //0 : dense cpu, 1 : dense gpu, 2 : sparse cpu.  dense needs appropriate lrn file format
        gIDX_MapNHdFuncIDX            = 9, //neighborhood : 0 : gaussian, 1 : bubble
        gIDX_MapRadCoolIDX            = 10, //radius cooling 0 : linear, 1 : exponential
        gIDX_MapLrnCoolIDX            = 11, //learning rate cooling 0 : linear 1 : exponential
        gIDX_MapLrnStIDX                 = 12, //start learning rate
        gIDX_MapLrnEndIDX            = 13, //end learning rate
        gIDX_MapRadStIDX                 = 14, //start radius
        gIDX_MapRadEndIDX            = 15, //end radius
        gIDX_MapPreBuiltDirIDX        = 16, //list of prebuilt maps as defined in config - this specifies which prebuilt map to use
        gIDX_MapNodeBMUTypeToDispIDX = 17, //type of examples mapping to a particular node to display in visualization
                                     
        gIDX_NodeWtDispThreshIDX     = 18, //threshold for display of map nodes on individual weight maps
        gIDX_NodePopDispThreshIDX    = 19, //only display populated map nodes that are this size or larger (log of population determines size)        
        gIDX_NodeInSegThreshIDX        = 20, //threshold of u-matrix weight for nodes to belong to same segment
                                     
        gIDX_MseRegionSensIDX        = 21, //sensitivity threshold for mouse-over
        gIDX_FtrSelectIDX            = 22, //pick the feature to display, if ftr-idx wt graphs are being displayed
        gIDX_CategorySelectIDX        = 23, //pick the category to display, if category mapping is available/enabled
        gIDX_ClassSelectIDX            = 24;            //pick the class to display, if class mapping is available/enabled
    
    public static final int numSOMBaseGUIObjs = 25;
    //
    //match descriptor string to index and index to string, to facilitate access
    public LinkedHashMap<String, Integer> mapDatDescrToUIIdx;
    public LinkedHashMap<Integer,String> mapUIIdxToMapDatDescr;
    //array of gui object idxs corresponding positionally with map dat names specified above
    public static final int[] mapObjUIIdxs = new int[] {
        gIDX_MapColsIDX, gIDX_MapRowsIDX, gIDX_MapEpochsIDX, gIDX_MapKTypIDX,gIDX_MapRadStIDX, gIDX_MapRadEndIDX,gIDX_MapLrnStIDX,gIDX_MapLrnEndIDX,
        gIDX_MapShapeIDX ,gIDX_MapBndsIDX,gIDX_MapRadCoolIDX,gIDX_MapNHdFuncIDX, gIDX_MapLrnCoolIDX
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
    public SOM_MapUIWin(IRenderInterface _p, GUI_AppManager _AppMgr, GUI_AppWinVals _winInitVals) {
        super(_p, _AppMgr,_winInitVals);
        initAndSetSOMDatUIMaps();
    }//ctor
    
    //set convenience maps to more easily access UI objects related to map data/args
    private void initAndSetSOMDatUIMaps() {
        //build map descriptor index to mapdat descriptor string map. 
        mapDatDescrToUIIdx = new LinkedHashMap<String, Integer>();
        mapUIIdxToMapDatDescr = new LinkedHashMap<Integer,String> ();
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
    public String getMapKeyStringFromUIidx(int UIidx) {        return mapUIIdxToMapDatDescr.get(UIidx);    }

    /**
     * Given MapDat key, return an int corresponding to the appropriate ui object in the instancing window
     * @param mapKey
     * @return
     */
    @Override
    public int getUIidxFromMapKeyString(String mapKey){        return mapDatDescrToUIIdx.get(mapKey);    }

    @Override
    protected final void initMe() {
        //initUIBox();                //set up ui click region to be in sidebar menu below menu's entries    
        mapMgr = buildMapMgr();
        initAfterMapMgrSet(new boolean[] {true,true});
    }//initMe() 
    
    /**
     * This function implements the instantiation of a child window owned by this window, if such exists.
     * The implementation should be similar to how the main windows are implemented in GUI_AppManager::initAllDispWindows.
     * If no child window exists, this implementation of this function can be empty
     * 
     * The SOM Map window should probably never have a child window, since it will, itself, usually be a child window
     * @param GUI_AppWinVals the window control values for the child window.
     */
    @Override
    protected final void buildAndSetChildWindow_Indiv(GUI_AppWinVals _appVals) {}     
    /**
     * Initialize any UI control flags appropriate for all SOM mapUI window applications
     */
    @Override
    protected final void initDispFlags() {
        
        // capable of using right side menu
        //dispFlags.setHasRtSideMenu(true);
        initDispFlags_Indiv();
    }
    /**
     * Initialize any UI control flags appropriate for specific instanced SOM mapUI window
     */
    protected abstract void initDispFlags_Indiv();
    
    /**
     * initialize the map manager this window uses after it is built or set
     * @param flagsToSet : idx 0 is mapDrawUMatrixIDX, idx 1 is mapExclProdZeroFtrIDX; either set these to initial values or copy current values
     */
    private void initAfterMapMgrSet(boolean[] flagsToSet) {
        mapUIAPI = mapMgr.mapUIAPI;
        setVisScreenWidth(winInitVals.rectDim[2]);
        //only set for visualization
        mapMgr.setDispWinData(this);        
        //init specific sim flags
        //initPrivFlags(numPrivFlags);
        /**
         * set these values from when UI was created 
         */
        uiMgr.setPrivFlag(_categoryCanBeShownIDX, _catExistsAndIsShown);
        uiMgr.setPrivFlag(_classCanBeShownIDX, _classExistsAndIsShown);        
        uiMgr.setPrivFlag(mapDrawTrainDatIDX,uiMgr.getPrivFlag(mapDrawTrainDatIDX));
        uiMgr.setPrivFlag(mapDrawWtMapNodesIDX,uiMgr.getPrivFlag(mapDrawWtMapNodesIDX));
        uiMgr.setPrivFlag(mapUseChiSqDistIDX,uiMgr.getPrivFlag(mapUseChiSqDistIDX));
        uiMgr.setPrivFlag(mapDrawUMatrixIDX, flagsToSet[0]);
        uiMgr.setPrivFlag(mapExclProdZeroFtrIDX, flagsToSet[1]);
        //set initial values for UI
        mapMgr.initFromUIWinInitMe(
                (int)(uiMgr.getUIValue(gIDX_TrainDataNormIDX)), 
                (int)(uiMgr.getUIValue(gIDX_BMU_DispDataFrmtIDX)), 
                (float)(uiMgr.getUIValue(gIDX_NodeWtDispThreshIDX)), 
                (float)(uiMgr.getUIValue(gIDX_NodePopDispThreshIDX)), 
                (int)(uiMgr.getUIValue(gIDX_MapNodeBMUTypeToDispIDX)));

        initMe_Indiv();
    }
    
    @Override
    protected int[] getFlagIDXsToInitToTrue() {        return null;}

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
        msgObj.dispInfoMessage("SOM_MapUIWin", "setMapMgr", "Start setting map manager from " +  mapMgr.getName() + " to " + _mapMgr.getName());        
        //set passed map manager as current
        mapMgr = _mapMgr;
        //re-init window to use this map manager
        initAfterMapMgrSet(new boolean[] {uiMgr.getPrivFlag(mapDrawUMatrixIDX), uiMgr.getPrivFlag(mapExclProdZeroFtrIDX)});
        //send new mapMgr's config data
        mapMgr.setUIValsFromProjConfig();
    }
    
    protected abstract void initMe_Indiv();    

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
     * Build all UI objects to be shown in left side bar menu for this window. This is the first child class function called by initThisWin
     * @param tmpUIObjMap : map of GUIObj_Params, keyed by unique string, with values describing the UI object
     *             - The object IDX                   
     *          - A double array of min/max/mod values                                                   
     *          - The starting value                                                                      
     *          - The label for object                                                                       
     *          - The object type (GUIObj_Type enum)
     *          - A boolean array of behavior configuration values : (unspecified values default to false)
     *               idx 0: value is sent to owning window,  
     *               idx 1: value is sent on any modifications (while being modified, not just on release), 
     *               idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
     *          - A boolean array of renderer format values :(unspecified values default to false) - Behavior Boolean array must also be provided!
     *                 - Should be multiline
     *                 - One object per row in UI space (i.e. default for multi-line and btn objects is false, single line non-buttons is true)
     *                 - Force this object to be on a new row/line (For side-by-side layouts)
     *                 - Text should be centered (default is false)
     *                 - Object should be rendered with outline (default for btns is true, for non-buttons is false)
     *                 - Should have ornament
     *                 - Ornament color should match label color 
     */
    @Override
    protected final void setupGUIObjsAras(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap){        //keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects            
        //keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
        String[] listOfTypes = SOM_FtrDataType.getListOfTypes();
        String[] bmuMapTypes = SOM_MapManager.getNodeBMUMapTypes();    
        
        tmpUIObjMap.put("gIDX_TrainDataNormIDX", uiMgr.uiObjInitAra_List(gIDX_TrainDataNormIDX, SOM_FtrDataType.FTR_NORM.getVal(), "Data Normalizing", listOfTypes));                   //uiTrainDataFrmtIDX                                                                        
        tmpUIObjMap.put("gIDX_BMU_DispDataFrmtIDX", uiMgr.uiObjInitAra_List(gIDX_BMU_DispDataFrmtIDX, SOM_FtrDataType.UNNORMALIZED.getVal(), "Map Node Ftr Disp Frmt", listOfTypes));      //gIDX_BMU_DispDataFrmtIDX                                                                         
        tmpUIObjMap.put("gIDX_TrainDatPartIDX", uiMgr.uiObjInitAra_Int(gIDX_TrainDatPartIDX, new double[]{1.0, 100.0, 1.0}, 100.0, "Data % To Train"));                                               //gIDX_TrainDatPartIDX                                                                         
        tmpUIObjMap.put("gIDX_MapRowsIDX", uiMgr.uiObjInitAra_Int(gIDX_MapRowsIDX, new double[]{1.0, 120.0, 10}, 10.0, "# Map Rows"));                                                           //gIDX_MapRowsIDX                                                                                  
        tmpUIObjMap.put("gIDX_MapColsIDX", uiMgr.uiObjInitAra_Int(gIDX_MapColsIDX, new double[]{1.0, 120.0, 10}, 10.0, "# Map Columns"));                                                       //gIDX_MapColsIDX                                                                                 
        tmpUIObjMap.put("gIDX_MapEpochsIDX", uiMgr.uiObjInitAra_Int(gIDX_MapEpochsIDX, new double[]{1.0, 200.0, 10}, 10.0, "# Training Epochs"));                                                  //gIDX_MapEpochsIDX                                                                            
        tmpUIObjMap.put("gIDX_MapShapeIDX", uiMgr.uiObjInitAra_List(gIDX_MapShapeIDX, 0.0, "Map Node Shape", new String[]{"rectangular","hexagonal"}));                           //gIDX_MapShapeIDX                                                                                 
        tmpUIObjMap.put("gIDX_MapBndsIDX", uiMgr.uiObjInitAra_List(gIDX_MapBndsIDX, 1.0, "Map Boundaries", new String[]{"planar","toroid"}));                          //gIDX_MapBndsIDX                                                                                 
        tmpUIObjMap.put("gIDX_MapKTypIDX", uiMgr.uiObjInitAra_List(gIDX_MapKTypIDX, 2.0, "Dense/Sparse (C/G)PU", new String[]{"Dense CPU", "Dense GPU", "Sparse CPU"}));                   //gIDX_MapKTypIDX                                                                                 
        tmpUIObjMap.put("gIDX_MapNHdFuncIDX", uiMgr.uiObjInitAra_List(gIDX_MapNHdFuncIDX, 0.0, "Neighborhood Func", new String[]{"gaussian","bubble"}));                       //gIDX_MapNHdFuncIDX                                                                            
        tmpUIObjMap.put("gIDX_MapRadCoolIDX", uiMgr.uiObjInitAra_List(gIDX_MapRadCoolIDX, 0.0, "Radius Cooling", new String[]{"linear","exponential"}));                           //gIDX_MapRadCoolIDX                                                                            
        tmpUIObjMap.put("gIDX_MapLrnCoolIDX", uiMgr.uiObjInitAra_List(gIDX_MapLrnCoolIDX, 0.0, "Learn rate Cooling", new String[]{"linear","exponential"}));                       //gIDX_MapLrnCoolIDX                                                                            
        tmpUIObjMap.put("gIDX_MapLrnStIDX", uiMgr.uiObjInitAra_Float(gIDX_MapLrnStIDX, new double[]{0.001, 10.0, 0.001}, 1.0, "Start Learn Rate"));                                                       //gIDX_MapLrnStIDX                                                                                 
        tmpUIObjMap.put("gIDX_MapLrnEndIDX", uiMgr.uiObjInitAra_Float(gIDX_MapLrnEndIDX, new double[]{0.001, 1.0, 0.001}, 0.1, "End Learn Rate"));                                                           //gIDX_MapLrnEndIDX                                                                            
        tmpUIObjMap.put("gIDX_MapRadStIDX", uiMgr.uiObjInitAra_Int(gIDX_MapRadStIDX, new double[]{2.0, 300.0, 1.0}, 20.0, "Start Cool Radius"));                                                       //gIDX_MapRadStIDX         # nodes                                                                    
        tmpUIObjMap.put("gIDX_MapRadEndIDX", uiMgr.uiObjInitAra_Int(gIDX_MapRadEndIDX, new double[]{1.0, 10.0, 1.0}, 1.0, "End Cool Radius"));                                                           //gIDX_MapRadEndIDX        # nodes      
        tmpUIObjMap.put("gIDX_MapPreBuiltDirIDX", uiMgr.uiObjInitAra_List(gIDX_MapPreBuiltDirIDX, 0.0, "Pretrained Map Dirs",new String[]{"None"}));            //gIDX_MapPreBuiltDirIDX
        tmpUIObjMap.put("gIDX_MapNodeBMUTypeToDispIDX", uiMgr.uiObjInitAra_List(gIDX_MapNodeBMUTypeToDispIDX, 0.0, "Ex Type For Node BMU", bmuMapTypes));     //gIDX_MapNodeBMUTypeToDispIDX                                                                 
        tmpUIObjMap.put("gIDX_NodeWtDispThreshIDX", uiMgr.uiObjInitAra_Float(gIDX_NodeWtDispThreshIDX, new double[]{0.0, 1.0, .01}, SOM_MapManager.initNodeInSegFtrWtDistThresh, "Map Node Disp Wt Thresh"));       //gIDX_NodeWtDispThreshIDX                                                                     
        tmpUIObjMap.put("gIDX_NodePopDispThreshIDX", uiMgr.uiObjInitAra_Float(gIDX_NodePopDispThreshIDX, new double[]{0.0, 1.0, .001}, 0.0, "Node Disp Pop Size Thresh %"));                                                        //uiPopMapNodeDispSizeIDX
        tmpUIObjMap.put("gIDX_NodeInSegThreshIDX", uiMgr.uiObjInitAra_Float(gIDX_NodeInSegThreshIDX, new double[]{0.0, 1.0, .001}, SOM_MapManager.initNodeInSegUMatrixDistThresh, "Segment UDist Thresh"));       //gIDX_NodeInSegThreshIDX//threshold of u-matrix weight for nodes to belong to same segment    
        tmpUIObjMap.put("gIDX_MseRegionSensIDX", uiMgr.uiObjInitAra_Float(gIDX_MseRegionSensIDX, new double[]{0.0, 1.0, .001}, 0.001, "Mouse Over Sens"));                                                           //gIDX_MseRegionSensIDX                                                                        
        tmpUIObjMap.put("gIDX_FtrSelectIDX", uiMgr.uiObjInitAra_List(gIDX_FtrSelectIDX, 0.0, "Feature IDX To Show", new String[]{"None"}));                    //gIDX_FtrSelectIDX
        
        String catUIDesc = getCategoryUIObjLabel();
        if((null!=catUIDesc) && (catUIDesc.length()>0)) {
            _catExistsAndIsShown = true;
            tmpUIObjMap.put("gIDX_CategorySelectIDX", uiMgr.uiObjInitAra_List(gIDX_CategorySelectIDX, 0.0, catUIDesc, new String[]{"None"}));            //gIDX_MapPreBuiltDirIDX
        } else {            _catExistsAndIsShown = false;        }
        
        String classUIDesc = getClassUIObjLabel();
        if((null!=classUIDesc) && (classUIDesc.length()>0)) {
            _classExistsAndIsShown = true;
            tmpUIObjMap.put("gIDX_ClassSelectIDX", uiMgr.uiObjInitAra_List(gIDX_ClassSelectIDX, 0.0, classUIDesc, new String[]{"None"}));            //gIDX_MapPreBuiltDirIDX
        } else {            _classExistsAndIsShown = false;        }
        setupGUIObjsAras_Indiv(tmpUIObjMap);
    }//setupGUIObjsAras
    
    /**
     * Build UI button objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    protected final void setupGUIBoolSwitchAras(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {        
        //add an entry for each button, in the order they are wished to be displayed
        int idx=firstIdx;
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Building SOM","Build SOM ",buildSOMExe));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Reset Dflt UI Vals","Reset Dflt UI Vals",resetMapDefsIDX));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Using ChiSq for Ftr Dist", "Not Using ChiSq Distance", mapUseChiSqDistIDX));       
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Prdct Dist ignores 0-ftrs","Prdct Dist w/all ftrs", mapExclProdZeroFtrIDX));    
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Showing UMat (Bi-Cubic)", "Showing Ftr Map", mapDrawUMatrixIDX));        
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Nodes", "Show Nodes", mapDrawAllMapNodesIDX));    
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Node Lbls", "Show Node Lbls", mapDrawNodeLblIDX));      
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Nodes (by Pop)", "Show Nodes (by Pop)", mapDrawPopMapNodesIDX));    
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Nodes w/o mapped Ex","Show Nodes w/o mapped Ex",mapDrawNodesWith0MapExIDX));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Hot Ftr Nodes (by Wt)", "Show Hot Ftr Nodes (by Wt)", mapDrawWtMapNodesIDX));     
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Node Pop Graph", "Show Node Pop Graph", drawMapNodePopGraphIDX));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Train Data", "Show Train Data", mapDrawTrainDatIDX));       
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Test Data", "Show Test Data", mapDrawTestDatIDX));        
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Ftr Wt Segments", "Show Ftr Wt Segments", mapDrawFtrWtSegMembersIDX));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Clstr (U-Dist)", "Show Clstr (U-Dist)", mapDrawUMatSegMembersIDX)); 
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, "Hide Clstr Image", "Show Clstr Image", mapDrawUMatSegImgIDX));  
        
        //these are only enabled if they have been defined to return values from instancing class
        String[] classBtnTFLabels = getClassBtnTFLabels();
        if((null != classBtnTFLabels) && (classBtnTFLabels.length == 2)) {tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, classBtnTFLabels[0],classBtnTFLabels[1],mapDrawClassSegmentsIDX));}        
        String[] catBtnTFLabels = getCategoryBtnTFLabels();
        if((null != catBtnTFLabels) && (catBtnTFLabels.length == 2)) {tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, catBtnTFLabels[0],catBtnTFLabels[1],mapDrawCategorySegmentsIDX));}                
        String[] saveSegmentTFLabels = getSegmentSaveBtnTFLabels();
        if((null != classBtnTFLabels) && (classBtnTFLabels.length == 2)) {tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, saveSegmentTFLabels[0],saveSegmentTFLabels[1],saveAllSegmentMapsIDX));}        
        String[] catClassLockBtnTFLabels = getClassCatLockBtnTFLabels();
        if((null != catClassLockBtnTFLabels) && (catClassLockBtnTFLabels.length == 2)) {tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx, "button_"+idx++, catClassLockBtnTFLabels[0],catClassLockBtnTFLabels[1],mapLockClassCatSegmentsIDX));}    
        //populate instancing application objects, including instancing-class specific buttons
        setupGUIBoolSwitchAras_Indiv(idx, tmpUIBoolSwitchObjMap);        
    }//setupGUIBoolSwitchAras

    /**
     * Build all UI objects to be shown in left side bar menu for this window. This is the first child class function called by initThisWin
     * @param tmpUIObjMap : map of GUIObj_Params, keyed by unique string, with values describing the UI object
     *             - The object IDX                   
     *          - A double array of min/max/mod values                                                   
     *          - The starting value                                                                      
     *          - The label for object                                                                       
     *          - The object type (GUIObj_Type enum)
     *          - A boolean array of behavior configuration values : (unspecified values default to false)
     *               idx 0: value is sent to owning window,  
     *               idx 1: value is sent on any modifications (while being modified, not just on release), 
     *               idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
     *          - A boolean array of renderer format values :(unspecified values default to false) - Behavior Boolean array must also be provided!
     *                 - Should be multiline
     *                 - One object per row in UI space (i.e. default for multi-line and btn objects is false, single line non-buttons is true)
     *                 - Force this object to be on a new row/line (For side-by-side layouts)
     *                 - Text should be centered (default is false)
     *                 - Object should be rendered with outline (default for btns is true, for non-buttons is false)
     *                 - Should have ornament
     *                 - Ornament color should match label color 
     */
    protected abstract void setupGUIObjsAras_Indiv(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap);

    /**
     * Build all UI buttons to be shown in left side bar menu for this window. This is for instancing windows to add to button region
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    protected abstract void setupGUIBoolSwitchAras_Indiv(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap);
    
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
    public final void setUI_FeatureListVals(String[] ftrStrVals) {    uiMgr.setAllUIListValues(gIDX_FtrSelectIDX, ftrStrVals, true);    }
    /**
     * pass the list of values for the category list box, in idx order
     * @param categoryVals : list of values to display for category select list
     */
    public final void setUI_CategoryListVals(String[] categoryVals) {    if(uiMgr.getPrivFlag(_categoryCanBeShownIDX)) {    uiMgr.setAllUIListValues(gIDX_CategorySelectIDX, categoryVals, true);    }}
    /**
     * pass the list of values for the class list box, in idx order
     * @param classVals : list of values to display for class select list
     */
    public final void setUI_ClassListVals(String[] classVals) {        if(uiMgr.getPrivFlag(_classCanBeShownIDX)) {        uiMgr.setAllUIListValues(gIDX_ClassSelectIDX, classVals, true);    }}

    /**
     * set window-specific variables that are based on current visible screen dimensions
     */
    protected final void setVisScreenDimsPriv() {
        float xStart = winInitVals.rectDim[0] + .5f*(curVisScrDims[0] - (curVisScrDims[1]-(2*AppMgr.getXOffset())));
        //start x and y and dimensions of full map visualization as function of visible window size;
        mapMgr.setSOM_mapLoc(new float[]{xStart, winInitVals.rectDim[1] + AppMgr.getTextHeightOffset()});
        //handle application-specific functionality
        setVisScreenDimsPriv_Indiv();
    }//calcAndSetMapLoc
    protected abstract void setVisScreenDimsPriv_Indiv();
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
    protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {    }
    
    /**
     * Handle application-specific flag setting
     */
    @Override
    public void handlePrivFlags_Indiv(int idx, boolean val, boolean oldVal){
        switch (idx) {//special actions for each flag
            case buildSOMExe             : {break;}            //placeholder    
            case resetMapDefsIDX        : {if(val){uiMgr.resetUIVals(true); uiMgr.setPrivFlag(resetMapDefsIDX,false);}}
            case mapDataLoadedIDX         : {break;}            //placeholder                
            case mapUseChiSqDistIDX        : {//whether or not to use chi-squared (weighted) distance for features
                //turn off scaled ftrs if this is set
                mapMgr.setUseChiSqDist(val);
                break;}                            
            case mapExclProdZeroFtrIDX        : {//whether or not distances between two datapoints assume that absent features in smaller-length datapoints are 0, or to ignore the values in the larger datapoints
                mapMgr.setMapExclZeroFtrs(val);
                break;}                            
            case mapDrawTrainDatIDX        : {//draw training examples
                break;}                            
            case mapDrawTestDatIDX        : {//draw testing examples
                break;}        
            case mapDrawNodesWith0MapExIDX : {//hide or draw map nodes that have no mapped examples
                
                break;}
            case mapDrawWtMapNodesIDX        : {//draw map nodes
                if (val) {//turn off other node displays
                    uiMgr.setPrivFlag(mapDrawPopMapNodesIDX, false);
                    uiMgr.setPrivFlag(mapDrawAllMapNodesIDX, false);                    
                }
                break;}                            
            case mapDrawPopMapNodesIDX  : {                
                if (val) {//turn off other node displays
                    uiMgr.setPrivFlag(mapDrawWtMapNodesIDX, false);
                    uiMgr.setPrivFlag(mapDrawAllMapNodesIDX, false);                    
                }
                break;}
            case mapDrawAllMapNodesIDX    : {//draw all map nodes, even empty
                if (val) {//turn off other node displays
                    uiMgr.setPrivFlag(mapDrawPopMapNodesIDX, false);
                    uiMgr.setPrivFlag(mapDrawWtMapNodesIDX, false);                    
                }
                break;}    
            case mapDrawFtrWtSegMembersIDX :{
                if(val) {mapMgr.buildFtrWtSegmentsOnMap();}
                break;}
            case mapDrawClassSegmentsIDX        :{            break;}            
            case mapDrawCategorySegmentsIDX        :{            break;}        
            case drawMapNodePopGraphIDX            :{            break;}
            case saveAllSegmentMapsIDX : {
                if(val) {
                    mapMgr.saveAllSegment_BMUReports();
                    uiMgr.setPrivFlag(saveAllSegmentMapsIDX, false);
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
            case showSelRegionIDX         : {//highlight a specific region of the map, either all nodes above a certain threshold for a chosen class or category
                break;}
            case saveLocClrImgIDX : {break;}        //save image
            case mapLockClassCatSegmentsIDX : {
                setPrivFlags_LockCatForClassSegs(val);
                break;}

            default            : {setPrivFlags_Indiv(idx,val);}
        }
    }//setFlag        
    protected abstract void setPrivFlags_Indiv(int idx, boolean val);
    /**
     * Instance-specific code for managing locking of category segment selection to enable cycling through class within category
     * @param val whether the lock button is being turned on or off
     */
    protected abstract void setPrivFlags_LockCatForClassSegs(boolean val);
    //set flag values when finished building map, to speed up initial display
    public final void setFlagsDoneMapBuild(){
        uiMgr.setPrivFlag(mapDrawTrainDatIDX, false);
        uiMgr.setPrivFlag(mapDrawWtMapNodesIDX, false);
        uiMgr.setPrivFlag(mapDrawAllMapNodesIDX, false);
    }//setFlagsDoneMapBuild
    

    //first verify that new .lrn file exists, then
    //build new SOM_MAP map using UI-entered values, then load resultant data - map nodes, bmus to training data
    protected final void buildNewSOMMap(){
        msgObj.dispMessage("SOM_MapUIWin","buildNewSOMMap","Starting Map Build", MsgCodes.info5);
        uiMgr.setPrivFlag(buildSOMExe,false);
        //send current UI values to map manager, load appropriate data, build directory structure and execute map
        boolean returnCode = mapMgr.loadTrainDataMapConfigAndBuildMap(true);
        //returnCode is whether map was built and trained successfully
        setFlagsDoneMapBuild();
        msgObj.dispMessage("SOM_MapUIWin","buildNewSOMMap","Map Build " + (returnCode ? "Completed Successfully." : "Failed due to error."), MsgCodes.info5);
        
    }//buildNewSOMMap        
    
    /**
     * update UI values from passed SOM_MapDat object's current state
     * @param mapDat 
     */
    public final void setUIValues(SOM_MapDat mapDat) {
        HashMap<String, Integer> mapInts = mapDat.getMapInts();
        HashMap<String, Float> mapFloats = mapDat.getMapFloats();
        HashMap<String, String> mapStrings = mapDat.getMapStrings();

        uiMgr.setNewUIValue(gIDX_MapColsIDX, mapInts.get("mapCols"));
        uiMgr.setNewUIValue(gIDX_MapRowsIDX, mapInts.get("mapRows"));
        uiMgr.setNewUIValue(gIDX_MapEpochsIDX, mapInts.get("mapEpochs"));
        uiMgr.setNewUIValue(gIDX_MapKTypIDX, mapInts.get("mapKType"));
        uiMgr.setNewUIValue(gIDX_MapRadStIDX, mapInts.get("mapStRad"));
        uiMgr.setNewUIValue(gIDX_MapRadEndIDX, mapInts.get("mapEndRad"));
        
        uiMgr.setNewUIValue(gIDX_MapLrnStIDX, mapFloats.get("mapStLrnRate"));
        uiMgr.setNewUIValue(gIDX_MapLrnEndIDX, mapFloats.get("mapEndLrnRate"));
        
        uiMgr.setDispUIListVal(gIDX_MapShapeIDX, mapStrings.get("mapGridShape"));    
        uiMgr.setDispUIListVal(gIDX_MapBndsIDX, mapStrings.get("mapBounds"));    
        uiMgr.setDispUIListVal(gIDX_MapRadCoolIDX, mapStrings.get("mapRadCool"));    
        uiMgr.setDispUIListVal(gIDX_MapNHdFuncIDX, mapStrings.get("mapNHood"));    
        uiMgr.setDispUIListVal(gIDX_MapLrnCoolIDX, mapStrings.get("mapLearnCool"));
                
    }//setUIValues
    
    /**
     * set display of prebuilt map directories to use based on loaded info from project config file
     * @param _pbltMapArray 
     */
    public final void setPreBuiltMapArray(String[] _pbltMapArray) {
        msgObj.dispInfoMessage("SOM_MapUIWin","setPreBuiltMapArray","Attempting to set prebuilt map values list of size : " +_pbltMapArray.length);
        int curIDX = uiMgr.setAllUIListValues(gIDX_MapPreBuiltDirIDX,_pbltMapArray, true);
        getUIDataUpdater().setIntValue(gIDX_MapPreBuiltDirIDX, (int) uiMgr.getUIValue(gIDX_MapPreBuiltDirIDX));
        mapMgr.setCurPreBuiltMapIDX(curIDX);
    }//

    ///////////////////////////////
    // map <--> ui sync functions
    //
    @Override
    public final void setMapDataVal_Integer(int UIidx, double val) {    mapMgr.updateMapDatFromUI_Integer(getMapKeyStringFromUIidx(UIidx), (int) val);}    
    @Override
    public final void setMapDataVal_Float(int UIidx, double val) {    mapMgr.updateMapDatFromUI_Float(getMapKeyStringFromUIidx(UIidx), (float) val);}
    @Override
    //public void setMapDataVal_String(int UIidx, double val) {    mapMgr.updateMapDatFromUI_String(getMapKeyStringFromUIidx(UIidx), getUIListValStr(UIidx, (int)val));}
    public final void setMapDataVal_String(int UIidx, double val) {    mapMgr.updateMapDatFromUI_String(getMapKeyStringFromUIidx(UIidx), uiMgr.getListValStr(UIidx,(int)val));}
    
    // set UI vals from map mgr - these are changes resulting from non-UI made changes to map
    @Override
    public final void updateUIDataVal_Integer(String key, Integer val) {
        if(!isMapNameOfType(mapDatNames_Ints, key)) {
            msgObj.dispWarningMessage("SOM_MapUIWin","updateUIDataVal_Integer","Attempting to set UI object with unknown Key : " +key + " using integer value " + val +". Aborting.");    
            return;}
        Integer uiObjIDX = getUIidxFromMapKeyString(key);
        getUIDataUpdater().setIntValue(uiObjIDX, (int) uiMgr.setNewUIValue(uiObjIDX, val));
        switch (uiObjIDX) {
            //integer values
            case gIDX_MapRowsIDX         : {uiMgr.setNewUIValue(gIDX_MapRadStIDX, .5*Math.min(val, uiMgr.getUIValue(gIDX_MapColsIDX)));    break;}
            case gIDX_MapColsIDX        : {uiMgr.setNewUIValue(gIDX_MapRadStIDX, .5*Math.min(uiMgr.getUIValue(gIDX_MapRowsIDX), val));break;}
            case gIDX_MapEpochsIDX        : {break;}
            case gIDX_MapKTypIDX        : {break;}
            case gIDX_MapRadStIDX        : {
                if(val <= uiMgr.getUIValue(gIDX_MapRadEndIDX)+uiMgr.getModStep(uiObjIDX)) {
                    getUIDataUpdater().setIntValue(uiObjIDX, (int) uiMgr.setNewUIValue(uiObjIDX, uiMgr.getUIValue(gIDX_MapRadEndIDX)+uiMgr.getModStep(uiObjIDX)));
                }
                break;}
            case gIDX_MapRadEndIDX        : {
                if(val >= uiMgr.getUIValue(gIDX_MapRadStIDX)-uiMgr.getModStep(uiObjIDX)) { 
                    getUIDataUpdater().setIntValue(uiObjIDX, (int) uiMgr.setNewUIValue(uiObjIDX, uiMgr.getUIValue(gIDX_MapRadStIDX)-uiMgr.getModStep(uiObjIDX)));
                }
                break;}
        }
    }//setUIDataVal_Integer    

    @Override
    public final void updateUIDataVal_Float(String key, Float val) {
        if(!isMapNameOfType(mapDatNames_Floats, key)) {
            msgObj.dispWarningMessage("SOM_MapUIWin","updateUIDataVal_Float","Attempting to set UI object with unknown Key : " +key + " using integer value " + val +". Aborting.");    
            return;}
        Integer uiObjIDX = getUIidxFromMapKeyString(key);
        getUIDataUpdater().setFloatValue(uiObjIDX, (float)uiMgr.setNewUIValue(uiObjIDX, val));
        switch (uiObjIDX) {
        case gIDX_MapLrnStIDX        : {    
            if(val <= uiMgr.getUIValue(gIDX_MapLrnEndIDX)+uiMgr.getModStep(uiObjIDX)) {
                getUIDataUpdater().setFloatValue(uiObjIDX, (float) uiMgr.setNewUIValue(uiObjIDX, uiMgr.getUIValue(gIDX_MapLrnEndIDX)+uiMgr.getModStep(uiObjIDX)));
            }
            break;}
        case gIDX_MapLrnEndIDX        : {    
            if(val >= uiMgr.getUIValue(gIDX_MapLrnStIDX)-uiMgr.getModStep(uiObjIDX)) {    
                getUIDataUpdater().setFloatValue(uiObjIDX, (float) uiMgr.setNewUIValue(uiObjIDX, uiMgr.getUIValue(gIDX_MapLrnStIDX)-uiMgr.getModStep(uiObjIDX)));
            }
            break;}
        }
    }//setUIDataVal_Float
    
    @Override
    public final void updateUIDataVal_String(String key, String val) {
        if(!isMapNameOfType(mapDatNames_Strings, key)) {
            msgObj.dispWarningMessage("SOM_MapUIWin","updateUIDataVal_String","Attempting to set UI object with unknown Key : " +key + " using String value " + val +". Aborting.");
            return;}
        Integer uiObjIDX = getUIidxFromMapKeyString(key);
        int[] retVals = uiMgr.setDispUIListVal(uiObjIDX, val);
        //if retVals[1] != 0 then not ok
        if(retVals[1] != 0) {
            msgObj.dispWarningMessage("SOM_MapUIWin","updateUIDataVal_String","Attempting to set list object : " +key + " to unknown list value " + val +". Aborting.");
        } else {
            getUIDataUpdater().setIntValue(uiObjIDX, retVals[0]);
        }
    }//setUIDataVal_String
    
    ///////////////////////////////
    // end map <--> ui sync functions
    //
        
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
            //integer values
            case gIDX_MapRowsIDX         : {setMapDataVal_Integer(UIidx,ival);uiMgr.setNewUIValue(gIDX_MapRadStIDX, .5*MyMathUtils.min(ival, (int)uiMgr.getUIValue(gIDX_MapColsIDX)));    break;}    //also set rad start to have a value == to 1/2 the max of rows or columns
            case gIDX_MapColsIDX        : {setMapDataVal_Integer(UIidx,ival);uiMgr.setNewUIValue(gIDX_MapRadStIDX, .5*MyMathUtils.min((int)uiMgr.getUIValue(gIDX_MapRowsIDX), ival));break;}
            case gIDX_MapEpochsIDX        : {setMapDataVal_Integer(UIidx,ival);break;}
            case gIDX_MapKTypIDX        : {setMapDataVal_Integer(UIidx,ival);break;}
            case gIDX_MapRadStIDX        : {
                if(ival <= uiMgr.getUIValue(gIDX_MapRadEndIDX)+uiMgr.getModStep(UIidx)) {
                    getUIDataUpdater().setIntValue(UIidx, (int) uiMgr.setNewUIValue(UIidx, uiMgr.getUIValue(gIDX_MapRadEndIDX)+uiMgr.getModStep(UIidx)));
                }
                setMapDataVal_Integer(UIidx,ival);        break;}
            case gIDX_MapRadEndIDX        : {
                if(ival >= uiMgr.getUIValue(gIDX_MapRadStIDX)-uiMgr.getModStep(UIidx)) {
                    getUIDataUpdater().setIntValue(UIidx, (int) uiMgr.setNewUIValue(UIidx, uiMgr.getUIValue(gIDX_MapRadStIDX)-uiMgr.getModStep(UIidx)));
                }
                setMapDataVal_Integer(UIidx,ival);        break;}
            case gIDX_MapNHdFuncIDX    : {setMapDataVal_String(UIidx,ival); break;}
            case gIDX_MapRadCoolIDX    : {setMapDataVal_String(UIidx,ival); break;}
            case gIDX_MapLrnCoolIDX    : {setMapDataVal_String(UIidx,ival); break;}
            case gIDX_MapShapeIDX        : {setMapDataVal_String(UIidx,ival); break;}
            case gIDX_MapBndsIDX        : {setMapDataVal_String(UIidx,ival); break;}
            //end map arg-related string/list values
            
            case gIDX_TrainDataNormIDX         : {//format of training data
                mapMgr.setCurrentTrainDataFormat(SOM_FtrDataType.getEnumByIndex((int)(uiMgr.getUIValue(gIDX_TrainDataNormIDX))));
                break;}
            case gIDX_BMU_DispDataFrmtIDX             : {
                mapMgr.setBMU_DispFtrTypeFormat(SOM_FtrDataType.getEnumByIndex((int)(uiMgr.getUIValue(gIDX_BMU_DispDataFrmtIDX))));
                break;}
            case gIDX_TrainDatPartIDX             : {break;}
            case gIDX_NodeWtDispThreshIDX : {
                float _mapNodeWtDispThresh = (float)(uiMgr.getUIValue(gIDX_NodeWtDispThreshIDX));
                mapMgr.setMapNodeWtDispThresh(_mapNodeWtDispThresh);
                mapMgr.setNodeInFtrWtSegThresh(_mapNodeWtDispThresh);                
                break;}
            case  gIDX_NodePopDispThreshIDX:{
                float _mapNodePopDispThresh = (float)(uiMgr.getUIValue(gIDX_NodePopDispThreshIDX));
                mapMgr.setMapNodePopDispThreshPct(_mapNodePopDispThresh);
                break;}
            case gIDX_NodeInSegThreshIDX         :{        //used to determine threshold of value for setting membership in a segment/cluster
                mapMgr.setNodeInUMatrixSegThresh((float)(uiMgr.getUIValue(gIDX_NodeInSegThreshIDX)));
                mapMgr.buildUMatrixSegmentsOnMap();
                break;}            
            case gIDX_MapPreBuiltDirIDX         : {//what prebuilt map of list of maps shown to right of screen to use, if any are defined in project config
                mapMgr.setCurPreBuiltMapIDX((int) (uiMgr.getUIValue(gIDX_MapPreBuiltDirIDX)));
                break;}            
            case gIDX_MapNodeBMUTypeToDispIDX     : {//type of examples being mapped to each map node to display
                mapMgr.setMapNodeDispType(SOM_ExDataType.getEnumByIndex((int)(uiMgr.getUIValue(gIDX_MapNodeBMUTypeToDispIDX))));
                break;}            
            case gIDX_MseRegionSensIDX         : {            break;}
            case gIDX_FtrSelectIDX                : {        //feature idx map to display
                mapMgr.setCurFtrMapImgIDX((int)uiMgr.getUIValue(gIDX_FtrSelectIDX));    
                break;}
            case gIDX_CategorySelectIDX : {    //category select changed - managed by instancing app        
                setCategory_UIObj(settingCategoryFromClass); 
                break;}
            case gIDX_ClassSelectIDX : {            
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
            case gIDX_MapLrnStIDX        : {
                if(val <= uiMgr.getUIValue(gIDX_MapLrnEndIDX)+uiMgr.getModStep(UIidx)) {
                    getUIDataUpdater().setFloatValue(UIidx, (float) uiMgr.setNewUIValue(UIidx, uiMgr.getUIValue(gIDX_MapLrnEndIDX)+uiMgr.getModStep(UIidx)));
                }            
                setMapDataVal_Float(UIidx,val);            break;}
            case gIDX_MapLrnEndIDX        : {
                if(val >= uiMgr.getUIValue(gIDX_MapLrnStIDX)-uiMgr.getModStep(UIidx)) {
                    getUIDataUpdater().setFloatValue(UIidx, (float) uiMgr.setNewUIValue(UIidx, uiMgr.getUIValue(gIDX_MapLrnStIDX)-uiMgr.getModStep(UIidx)));
                }        
                setMapDataVal_Float(UIidx,val);            break;}
            
            case gIDX_TrainDataNormIDX         : {//format of training data
                mapMgr.setCurrentTrainDataFormat(SOM_FtrDataType.getEnumByIndex((int)(uiMgr.getUIValue(gIDX_TrainDataNormIDX))));
                break;}
            case gIDX_BMU_DispDataFrmtIDX             : {
                mapMgr.setBMU_DispFtrTypeFormat(SOM_FtrDataType.getEnumByIndex((int)(uiMgr.getUIValue(gIDX_BMU_DispDataFrmtIDX))));
                break;}
            case gIDX_TrainDatPartIDX             : {break;}
            case gIDX_NodeWtDispThreshIDX : {
                float _mapNodeWtDispThresh = (float)(uiMgr.getUIValue(gIDX_NodeWtDispThreshIDX));
                msgObj.dispInfoMessage(className, "setUI_FloatValsCustom", "Setting node weight disp thresh to be :"+_mapNodeWtDispThresh);
                mapMgr.setMapNodeWtDispThresh(_mapNodeWtDispThresh);
                mapMgr.setNodeInFtrWtSegThresh(_mapNodeWtDispThresh);                
                break;}
            case  gIDX_NodePopDispThreshIDX:{
                float _mapNodePopDispThresh = (float)(uiMgr.getUIValue(gIDX_NodePopDispThreshIDX));
                msgObj.dispInfoMessage(className, "setUI_FloatValsCustom", "Setting node pop disp thresh to be :"+_mapNodePopDispThresh);
                mapMgr.setMapNodePopDispThreshPct(_mapNodePopDispThresh);
                break;}
            case gIDX_NodeInSegThreshIDX         :{        //used to determine threshold of value for setting membership in a segment/cluster
                mapMgr.setNodeInUMatrixSegThresh((float)(uiMgr.getUIValue(gIDX_NodeInSegThreshIDX)));
                mapMgr.buildUMatrixSegmentsOnMap();
                break;}            
            case gIDX_MapPreBuiltDirIDX         : {//what prebuilt map of list of maps shown to right of screen to use, if any are defined in project config
                mapMgr.setCurPreBuiltMapIDX((int) (uiMgr.getUIValue(gIDX_MapPreBuiltDirIDX)));
                break;}            
            case gIDX_MapNodeBMUTypeToDispIDX     : {//type of examples being mapped to each map node to display
                mapMgr.setMapNodeDispType(SOM_ExDataType.getEnumByIndex((int)(uiMgr.getUIValue(gIDX_MapNodeBMUTypeToDispIDX))));
                break;}            
            case gIDX_MseRegionSensIDX         : {            break;}
            case gIDX_FtrSelectIDX                : {        //feature idx map to display
                mapMgr.setCurFtrMapImgIDX((int)uiMgr.getUIValue(gIDX_FtrSelectIDX));    
                break;}
            case gIDX_CategorySelectIDX : {    //category select changed - managed by instancing app        
                setCategory_UIObj(settingCategoryFromClass); 
                break;}
            case gIDX_ClassSelectIDX : {            
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
        int newVal = (int)uiMgr.setNewUIValue(gIDX_TrainDataNormIDX, _ftrTypeUsedToTrain);
        checkAndSetIntVal(gIDX_TrainDataNormIDX, newVal);
    }
    
    protected void setCategory_UIObj(boolean settingCategoryFromClass) {
        mapMgr.setCurCategoryIDX((int)(uiMgr.getUIValue(gIDX_CategorySelectIDX)));
        mapMgr.setCurCategoryLabel(getCategoryLabelFromIDX(mapMgr.getCurCategoryIDX()));                
        setUIWinVals_HandleCategory(settingCategoryFromClass); 
    }

    
    protected void setClass_UIObj(boolean settingClassFromCategory) {
        mapMgr.setCurClassIDX((int)(uiMgr.getUIValue(gIDX_ClassSelectIDX)));
        mapMgr.setCurClassLabel(getClassLabelFromIDX(mapMgr.getCurClassIDX()));                
        setUIWinVals_HandleClass(settingClassFromCategory);          
    }
    
    private boolean settingCategoryFromClass = false, settingClassFromCategory = false;
    
    /**
     * Called when class display select value is changed in ui
     */
    protected final void setUIWinVals_HandleClass(boolean settingClassFromCategory) {
        if(!settingClassFromCategory) {        //don't want to change category again if setting from category ui obj change - loop potential
            int curCatIdxVal = (int)uiMgr.getUIValue(gIDX_CategorySelectIDX);
            int catIdxToSet = getCategoryFromClass(curCatIdxVal,(int)uiMgr.getUIValue(gIDX_ClassSelectIDX));
            if(curCatIdxVal != catIdxToSet) {
                settingCategoryFromClass = true;
                uiMgr.setNewUIValue(gIDX_CategorySelectIDX, catIdxToSet);
                uiMgr.setUIWinVals(gIDX_CategorySelectIDX);
                checkAndSetIntVal(gIDX_TrainDataNormIDX,(int)uiMgr.getUIValue(gIDX_CategorySelectIDX));
                settingCategoryFromClass = false;
            }
        }        
    }//setUIWinVals_HandleClass
    /**
     * Called when category display select value is changed in ui
     */
    protected final void setUIWinVals_HandleCategory(boolean settingCategoryFromClass) {
        if(!settingCategoryFromClass) {
            int curClassIdxVal = (int)uiMgr.getUIValue(gIDX_ClassSelectIDX);
            int classIdxToSet = getClassFromCategory((int)uiMgr.getUIValue(gIDX_CategorySelectIDX), curClassIdxVal);
            if(curClassIdxVal != classIdxToSet) {
                settingClassFromCategory = true;
                uiMgr.setNewUIValue(gIDX_ClassSelectIDX, classIdxToSet);    
                uiMgr.setUIWinVals(gIDX_ClassSelectIDX);
                checkAndSetIntVal(gIDX_ClassSelectIDX,(int)uiMgr.getUIValue(gIDX_ClassSelectIDX));
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

    
    
    public final float getTrainTestDatPartition() {    return (float)(.01*uiMgr.getUIValue(gIDX_TrainDatPartIDX));}    
    
    
    /**
     * return map dims based on the size of this window
     */
    public final float[] getWinUIMapDims() {
        float width = winInitVals.rectDim[3] * SOMDimScaleFact;
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
        ri.pushMatState();
        //display current simulation variables - call sim world through sim exec
        mapMgr.drawResultBar(ri, AppMgr.getTextHeightOffset());
        ri.popMatState();                    
    }//drawOnScreenStuff
    
    
    private boolean doBuildMap = false;
    @Override
    protected final void drawMe(float animTimeMod) {
        drawSetDispFlags();
        if(uiMgr.getPrivFlag(mapDataLoadedIDX) != mapMgr.isMapDrawable()){uiMgr.setPrivFlag(mapDataLoadedIDX,mapMgr.isMapDrawable());}
        boolean isMapDataLoaded = uiMgr.getPrivFlag(mapDataLoadedIDX);
        drawMap(isMapDataLoaded);        
        if(doBuildMap){buildNewSOMMap(); doBuildMap = false;} 
        else if(uiMgr.getPrivFlag(buildSOMExe)) {    doBuildMap = true;    }    
    }
    protected abstract void drawSetDispFlags();
    
    private void drawMap(boolean mapDataLoaded){        
        //draw map rectangle
        ri.pushMatState();
        //instance-specific drawing
        drawMap_Indiv();
        if(mapDataLoaded){mapMgr.drawSOMMapData(ri);}    
        ri.popMatState();
    }//drawMap()    
    protected abstract void drawMap_Indiv();
    
    public final void drawMseOverData() {    mapMgr.drawMseOverData(ri);}

    /////////////////////////////////////////
    // end draw routines

    
    @Override
    public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {
        msgObj.dispMessage("SOM_MapUIWin","handleSideMenuMseOvrDispSel","Click Mouse display in "+getName()+" : btn : " + btn, MsgCodes.info4);
        SOM_MseOvrDispTypeVals uiMseDispData = handleSideMenuMseOvrDisp_MapBtnToType(btn, val);
        //msgObj.dispInfoMessage("SOM_MapUIWin","handleSideMenuMseOvrDispSel","Mouse Display Function : " + btn +" selected == "+ uiMseDispData.toString());
        if(uiMseDispData == SOM_MseOvrDispTypeVals.mseOvrOtherIDX) {    handleSideMenuMseOvrDispSel_Indiv(btn,val);    }
        mapMgr.setUiMseDispData(uiMseDispData);
        msgObj.dispMessage("SOM_MapUIWin","handleSideMenuMseOvrDispSel","Done Click Mouse display in "+getName()+" : btn : " + btn + " | Dist Type : " + uiMseDispData.toString(), MsgCodes.info4);
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
        if(val) {        mapMgr.setCustUIMseDispData(btn);} else {    mapMgr.setCustUIMseDispData(-1);}    
    }

    //given pixel location relative to upper left corner of map, return map node float - this measures actual distance in map node coords
    //so rounding to ints give map node tuple coords, while float gives interp between neighbors
    //protected final float[] getMapNodeLocFromPxlLoc(float mapPxlX, float mapPxlY, float sclVal){    return new float[]{(sclVal* mapPxlX * mapMgr.getNodePerPxlCol()) - .5f, (sclVal* mapPxlY * mapMgr.getNodePerPxlRow()) - .5f};}    
    /**
     * check whether the mouse is over a legitimate map location
     * @param mouseX
     * @param mouseY
     * @return
     */
    protected final boolean checkMouseOvr(int mouseX, int mouseY){    
        return mapMgr.checkMouseOvr(mouseX, mouseY, (float)uiMgr.getUIValue(gIDX_MseRegionSensIDX));
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
    }
    
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
    }
    
    public final void checkMouseRelease() {mapMgr.checkMouseRelease();}
    
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
    public void processTraj_Indiv(DrawnSimpleTraj drawnNoteTraj){        }
    @Override
    protected void endShiftKey_Indiv() {}
    @Override
    protected void endAltKey_Indiv() {}
    @Override
    protected void endCntlKey_Indiv() {}
    @Override
    protected void addSScrToWin_Indiv(int newWinKey){}
    @Override
    protected void addTrajToScr_Indiv(int subScrKey, String newTrajKey){}
    @Override
    protected void delSScrToWin_Indiv(int idx) {}    
    @Override
    protected void delTrajToScr_Indiv(int subScrKey, String newTrajKey) {}
    //resize drawn all trajectories
    @Override
    protected void resizeMe(float scale) {}
    @Override
    protected void closeMe() {}
    @Override
    protected void showMe() {        setCustMenuBtnLabels();    }

}//SOMMapUIWin
