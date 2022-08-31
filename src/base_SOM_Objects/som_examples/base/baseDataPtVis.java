package base_SOM_Objects.som_examples.base;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_SOM_Objects.SOM_MapManager;
import base_SOM_Objects.som_examples.SOM_ExDataType;
import base_UI_Objects.GUI_AppManager;
import base_Utils_Objects.io.messaging.MessageObject;

/**
 * This class holds functionality for rendering on the map.
 * Since this functionality is fundamentally different than the 
 * necessary functionality for feature calculation/manipulation
 * we have it separate from the base example class
 * @author john
 *
 */
public abstract class baseDataPtVis{
	public SOM_MapManager mapMgr;
	//message object manages logging/printing to screen
	protected MessageObject msgObj;
	//type of example data this is
	protected SOM_ExDataType exampleDataType;
	//location in mapspace most closely matching this node - actual map location (most likely between 4 map nodes), built from neighborhood
	public myPointf mapLoc;		
	//bmu map node location - this is same as mapLoc(and ignored) for map nodes
	protected myPointf mapNodeLoc;
	//draw-based vars
	protected float mapDrawRad;
	protected static int drawDet;	
	//for debugging purposes, gives min and max radii of spheres that will be displayed on map for each node proportional to # of samples - only display related
	public static float minRad = 100000, maxRad = -100000;
	//array of color IDXs for specific color roles : idx 0 ==fill, idx 1 == strk, idx 2 == txt
	//alt is for displaying alternate state
	private int[] mapNodeClrs;
	private int[] mapAltClrs;		
	
	public baseDataPtVis(SOM_MapManager _map, SOM_ExDataType _type) {
		mapMgr = _map;exampleDataType=_type;
		if(msgObj==null) {msgObj=mapMgr.buildMsgObj();}//only set 1 msg object for all examples
		mapLoc = new myPointf();	
		mapNodeLoc = new myPointf();
		mapDrawRad = 1.0f;
		drawDet = 2;
		setMapNodeClrs(mapMgr.getClrFillStrkTxtAra(exampleDataType));
		setMapAltClrs(mapMgr.getAltClrFillStrkTxtAra());
	}//ctor	
	
	//copy ctor
	public baseDataPtVis(baseDataPtVis _otr) { 
		this(_otr.mapMgr,_otr.exampleDataType);	
		mapLoc = _otr.mapLoc;
		mapNodeLoc = _otr.mapNodeLoc;
		setMapNodeClrs(_otr.getMapNodeClrs());
		setMapAltClrs(_otr.getMapAltClrs());
	}//
	
	protected void setRad(float _rad){
		mapDrawRad = _rad;//((float)(Math.log(2.0f*(_rad+1))));
		minRad = minRad > mapDrawRad ? mapDrawRad : minRad;
		maxRad = maxRad < mapDrawRad ? mapDrawRad : maxRad;
		//drawDet = ((int)(Math.log(2.0f*(rad+1)))+1);
	}
	public float getRad(){return mapDrawRad;}
	
	public int getTypeVal() {return exampleDataType.getVal();}
	public SOM_ExDataType getType() {return exampleDataType;}
	public int[] getMapAltClrs() {	return mapAltClrs;}
	public void setMapAltClrs(int[] mapAltClrs) {	this.mapAltClrs = mapAltClrs;}
	public int[] getMapNodeClrs() {	return mapNodeClrs;}
	public void setMapNodeClrs(int[] mapNodeClrs) {	this.mapNodeClrs = mapNodeClrs;}

	
	//set map location for this example
	public final void setMapLoc(myPointf _pt){mapLoc.set(_pt.x,_pt.y,_pt.z);}

	//draw this example with a line linking it to its best matching unit
	public final void drawMeLinkedToBMU(IRenderInterface p, float _rad, String ID){
		p.pushMatState();
		//draw point of radius rad at mapLoc - actual location on map
		//show(myPointf P, float rad, int det, int[] clrs, String[] txtAra)
		int[] clrAra = getMapNodeClrs();
		p.showTxtAra(mapLoc, _rad, drawDet, clrAra, new String[] {ID});
		//draw line to bmu location
		p.setColorValStroke(clrAra[1],255);
		p.setStrokeWt(1.0f);
		p.drawLine(mapLoc, mapNodeLoc);
		p.popMatState();		
	}//drawMeLinkedToBMU
	
	public void drawMeSmallNoLbl(IRenderInterface p){
		p.pushMatState();
		int[] clrAra = getMapNodeClrs();
		p.showPtAsSphere(mapLoc, 2, 2, clrAra[0],clrAra[1]); 
		p.popMatState();		
	}	
		
	//override drawing in map nodes
	public final void drawMeMap(IRenderInterface p){
		p.pushMatState();	
		int[] clrAra = getMapNodeClrs();
		p.showPtAsSphere(mapLoc, getRad(), drawDet, clrAra[0],clrAra[1]);		
		p.popMatState();		
	}//drawMeMap
	
	//override drawing in map nodes
	public final void drawMeMapClr(GUI_AppManager AppMgr, IRenderInterface p, int[] clr){
		p.pushMatState();
		//draw point of radius rad at mapLoc
		AppMgr.show_ClrAra(mapLoc, mapDrawRad,drawDet, clr, clr);
		p.popMatState();		
	}//drawMeMapClr
	
	public final void drawMeRanked(GUI_AppManager AppMgr, IRenderInterface p, String lbl, int[] clr, float rad, int rank){
		p.pushMatState();
		//draw point of radius rad at maploc with label and no background box	
		AppMgr.showNoBox_ClrAra(mapLoc, rad, drawDet, clr, clr, IRenderInterface.gui_White, lbl);
		p.popMatState();
	}

}//baseDataPtVis
