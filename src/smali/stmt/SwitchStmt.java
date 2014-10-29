package smali.stmt;

import java.util.HashMap;
import java.util.Map;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class SwitchStmt extends StaticStmt{

	private boolean isPswitch;
	private boolean isSswitch;
	private String switchMapLabel;
	private String pSwitchInitValue;
	private Map<String, String> switchMap = new HashMap<String, String>();
	
	public String getSwitchV() {
		return getvA();
	}
	
	public boolean isPswitch() {
		return isPswitch;
	}
	public void setIsPswitch(boolean isPswitch) {
		this.isPswitch = isPswitch;
	}
	public boolean isSswitch() {
		return isSswitch;
	}
	public void setISSswitch(boolean isSswitch) {
		this.isSswitch = isSswitch;
	}
	public String getSwitchMapLabel() {
		return switchMapLabel;
	}
	public void setSwitchMapLabel(String switchMapLabel) {
		this.switchMapLabel = switchMapLabel;
	}
	public String getpSwitchInitValue() {
		return pSwitchInitValue;
	}
	public void setpSwitchInitValue(String pSwitchInitValue) {
		this.pSwitchInitValue = pSwitchInitValue;
	}
	public Map<String, String> getSwitchMap() {
		return switchMap;
	}
	public void setSwitchMap(Map<String, String> switchMap) {
		this.switchMap = switchMap;
	}
	
}
