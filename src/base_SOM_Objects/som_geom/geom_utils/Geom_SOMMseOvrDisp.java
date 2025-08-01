package base_SOM_Objects.som_geom.geom_utils;

import java.util.ArrayList;
import java.util.TreeMap;

import base_Render_Interface.IRenderInterface;
import base_SOM_Objects.som_managers.SOM_MapManager;
import base_SOM_Objects.som_mapnodes.base.SOM_MapNode;
import base_SOM_Objects.som_ui.SOM_MseOvrDisplay;

public class Geom_SOMMseOvrDisp extends SOM_MseOvrDisplay {

    public Geom_SOMMseOvrDisp(SOM_MapManager _mapMgr, float _dispThesh) {
        super(_mapMgr, _dispThesh);
    }

    @Override
    protected int[] setNodeColors() {            
        return new int[] {IRenderInterface.gui_Black,IRenderInterface.gui_White,IRenderInterface.gui_Black};
    }
    @Override
    protected String getFtrDispTitleString(int count) {return "Ftrs :  count : "+count;}
    /**
     * construct per feature display value 
     * @param ftrIDX : the index in the feature vector
     * @param ftr : the value in the ftr vector
     * @param strongestFtrs : the map being populated with the string arrays at each ftr value
     */
    @Override
    protected void buildPerFtrData(Integer ftrIDX, Float ftr, TreeMap<Float, ArrayList<String>> strongestFtrs) {
        ArrayList<String> vals = strongestFtrs.get(ftr);
        if(null==vals) {vals = new ArrayList<String>();}
        vals.add(""+ftrIDX);
        strongestFtrs.put(ftr, vals);
    }

    @Override
    protected String getClassProbTitleString(SOM_MapNode nearestMapNode, int ttlNumClasses) { 
        if(nearestMapNode == null) {        return "Null Nearest Map Node : No Classes present.";    }
        return nearestMapNode.mapNodeCoord.toString() + " Class Probs : ("+ttlNumClasses+" classes mapped) ";}
    @Override
    protected String getCategoryProbTitleString(SOM_MapNode nearestMapNode, int ttlNumCategories) { 
        if(nearestMapNode == null) {        return "Null Nearest Map Node : No Categories present.";    }

        return nearestMapNode.mapNodeCoord.toString() + " Category Probs : ("+ttlNumCategories+" categories mapped) ";
    }

    /**
     * instancing-specific initialization called for every data change for mouse object
     */
    @Override
    protected void initAll_Indiv() {}
    /**
     * instancing-specific finalizing called after every mouse-over data process
     */
    @Override
    protected void finalizeMseLblDatCtor_Indiv(ArrayList<String> _mseLblDat, int _longestLine) {    }
    /**
     * instancing-specific clearing called when mouse over display is cleared
     */
    @Override
    protected void clearMseDat_Indiv() {    }
}//Sphere_SOMMseOvrDisp
