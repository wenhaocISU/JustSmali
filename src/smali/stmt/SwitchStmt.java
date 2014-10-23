package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class SwitchStmt extends StaticStmt{

	private boolean isPswitch;
	private boolean isSswitch;
	private String switchMapLabel;
	private String pSwitchInitValue;
	public boolean isPswitch() {
		return isPswitch;
	}
	public void setIsPswitch(boolean isPswitch) {
		this.isPswitch = isPswitch;
	}
	public boolean isSswitch() {
		return isSswitch;
	}
	public void setSswitch(boolean isSswitch) {
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
	
}
