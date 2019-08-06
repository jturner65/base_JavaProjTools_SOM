package base_SOM_Objects.som_geom.geom_UI;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.BaseBarMenu;

/**
 * class to manage buttons used by sidebar window - overrides base setup, allows for custom config
 * @author john
 *
 */
public class SOM_GeomSideBarMenu extends BaseBarMenu {

	public SOM_GeomSideBarMenu(my_procApplet _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
	}
	
	/**
	 * initialize application-specific windows and titles in structs :
	 *  guiBtnRowNames, guiBtnNames, defaultUIBtnNames, guiBtnInst, guiBtnWaitForProc;
	 */
	@Override
	protected void initSideBarMenuBtns_Priv() {
		/**
		 * set row names for each row of ui action buttons getMouseOverSelBtnNames()
		 * @param _funcRowNames array of names for each row of functional buttons 
		 * @param _numBtnsPerFuncRow array of # of buttons per row of functional buttons - size must match # of entries in _funcRowNames array
		 * @param _numDbgBtns # of debug buttons
		 * @param _inclWinNames include the names of all the instanced windows
		 * @param _inclMseOvValues include a row for possible mouse over values
		 */
		//protected void setBtnData(String[] _funcRowNames, int[] _numBtnsPerFuncRow, int _numDbgBtns, boolean _inclWinNames, boolean _inclMseOvValues) {

		setBtnData(new String[]{"Load/Save Geometry Data","Functions 2","Functions 3","Functions 4"}, new int[] {3,4,4,4}, 5, true, true);

	}


}//mySideBarMenu
