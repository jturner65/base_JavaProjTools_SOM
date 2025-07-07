package base_SOM_Objects.som_examples.base.visualization;

import java.util.Arrays;

import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_Utils_Objects.io.messaging.MessageObject;

/**
 * This class holds functionality for rendering on the mari.
 * Since this functionality is fundamentally different than the 
 * necessary functionality for feature calculation/manipulation
 * we have it separate from the base example class
 * @author john
 *
 */
public abstract class baseDataPtVis{
    /**
     * message object manages logging/printing to screen
     */
    protected static MessageObject msgObj;
    /**
     * location in mapspace most closely matching this node - actual map location (most likely between 4 map nodes), built from neighborhood
     */
    public myPointf mapLoc;        
    /**
     * bmu map node location - this is same as mapLoc(and ignored) for map nodes
     */
    protected myPointf mapNodeLoc;
    /**
     * radius to draw on map
     */
    protected float mapDrawRad;
    /**
     * sphere detail to draw on map
     */
    protected static int drawDet;    
    /**
     * for debugging purposes, gives min radius of spheres that will be displayed on map for each node proportional to # of samples - only display related
     */
    public static float minRad = 100000;
    /**
     * for debugging purposes, gives max radius of spheres that will be displayed on map for each node proportional to # of samples - only display related
     */
    public static float maxRad = -100000;
    /**
     * array of color IDXs for specific color roles : idx 0 ==fill, idx 1 == strk, idx 2 == txt
     */
    private int[] mapNodeClrs;
    /**
     * alternate state array of color IDXs for specific color roles : idx 0 ==fill, idx 1 == strk, idx 2 == txt
     */
    private int[] mapAltClrs;        
    
    public baseDataPtVis(int[] _mapNodeClrs, int[] _mapAltClrs) {
        msgObj = MessageObject.getInstance();
        mapLoc = new myPointf();    
        mapNodeLoc = new myPointf();
        mapDrawRad = 1.0f;
        drawDet = 2;
        setMapNodeClrs(_mapNodeClrs);
        setMapAltClrs(_mapAltClrs);
    }//ctor    
    
    //copy ctor
    public baseDataPtVis(baseDataPtVis _otr) { 
        this(_otr.getMapNodeClrs(), _otr.getMapAltClrs());    
        mapLoc = _otr.mapLoc;
        mapNodeLoc = _otr.mapNodeLoc;
    }//
    
    protected void setRad(float _rad){
        mapDrawRad = _rad;
        minRad = minRad > mapDrawRad ? mapDrawRad : minRad;
        maxRad = maxRad < mapDrawRad ? mapDrawRad : maxRad;
    }
    public float getRad(){return mapDrawRad;}

    public int[] getMapAltClrs() {    return Arrays.copyOf(mapAltClrs, mapAltClrs.length);}
    public void setMapAltClrs(int[] mapAltClrs) {    this.mapAltClrs = mapAltClrs;}
    public int[] getMapNodeClrs() {    return Arrays.copyOf(mapNodeClrs, mapNodeClrs.length);}
    public void setMapNodeClrs(int[] mapNodeClrs) {    this.mapNodeClrs = mapNodeClrs;}
    
    /**
     * set map location for this example
     * @param _pt
     */
    public final void setMapLoc(myPointf _pt){mapLoc.set(_pt.x,_pt.y,_pt.z);}

    /**
     * draw this example with a line linking it to its best matching unit
     * @param ri
     * @param _rad
     * @param ID
     */
    public final void drawMeLinkedToBMU(IRenderInterface ri, float _rad, String ID){
        ri.pushMatState();
        //draw point of radius rad at mapLoc - actual location on map
        //show(myPointf P, float rad, int det, int[] clrs, String[] txtAra)
        int[] clrAra = getMapNodeClrs();
        ri.showTextAra(mapLoc, _rad, drawDet, clrAra, new String[]{ID});
        //draw line to bmu location
        ri.setColorValStroke(clrAra[1],255);
        ri.setStrokeWt(1.0f);
        ri.drawLine(mapLoc, mapNodeLoc);
        ri.popMatState();        
    }//drawMeLinkedToBMU
    
    /**
     * 
     * @param ri
     */
    public void drawMeSmallNoLbl(IRenderInterface ri){
        ri.pushMatState();
        int[] clrAra = getMapNodeClrs();
        ri.showPtAsSphere(mapLoc, 2, 2, clrAra[0],clrAra[1]); 
        ri.popMatState();        
    }    
        
    /**
     * override drawing in map nodes
     * @param ri
     */
    public final void drawMeMap(IRenderInterface ri){
        ri.pushMatState();    
        int[] clrAra = getMapNodeClrs();
        ri.showPtAsSphere(mapLoc, getRad(), drawDet, clrAra[0],clrAra[1]);        
        ri.popMatState();        
    }//drawMeMap
    
    /**
     * override drawing in map nodes
     * @param AppMgr
     * @param ri
     * @param clr
     */
    public final void drawMeMapClr(GUI_AppManager AppMgr, IRenderInterface ri, int[] clr){
        ri.pushMatState();
        //draw point of radius rad at mapLoc
        AppMgr.show_ClrAra(mapLoc, mapDrawRad,drawDet, clr, clr);
        ri.popMatState();        
    }//drawMeMapClr
    
    /**
     * 
     * @param AppMgr
     * @param ri
     * @param lbl
     * @param clr
     * @param rad
     * @param rank
     */
    public final void drawMeRanked(GUI_AppManager AppMgr, IRenderInterface ri, String lbl, int[] clr, float rad, int rank){
        ri.pushMatState();
        //draw point of radius rad at maploc with label and no background box    
        AppMgr.showNoBox_ClrAra(mapLoc, rad, drawDet, clr, clr, IRenderInterface.gui_White, lbl);
        ri.popMatState();
    }

}//baseDataPtVis
