package base_SOM_Objects.som_ui.utils;

import java.util.Map;

import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;

public class SOMWinUIDataUpdater extends UIDataUpdater {

	public SOMWinUIDataUpdater(myDispWindow _win) {
		super(_win);
		// TODO Auto-generated constructor stub
	}

	public SOMWinUIDataUpdater(UIDataUpdater _otr) {
		super(_otr);
		// TODO Auto-generated constructor stub
	}

	public SOMWinUIDataUpdater(myDispWindow _win, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_win, _iVals, _fVals, _bVals);
		// TODO Auto-generated constructor stub
	}

}
