package smali.stmt;

import java.util.HashMap;
import java.util.Map;

import concolic.Condition;
import concolic.Operation;
import staticFamily.StaticMethod;
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
	
	public Map<Integer, Condition> getSwitchMap(StaticMethod m) {
		Map<Integer, Condition> result = new HashMap<Integer, Condition>();
		for (Map.Entry<String, String> entry : switchMap.entrySet()) {
			String value = entry.getKey();
			if (!value.startsWith("#"))
				value = "#" + value;
			String targetLabel = entry.getValue();
			Condition cond = new Condition();
			cond.setLeft(this.getvA());
			cond.setOp("=");
			cond.setRight(value);
			if (this.isPswitch) {
				Operation o = new Operation();
				o.setLeft(this.getvA());
				o.setOp("add");
				o.setRightA(this.pSwitchInitValue);
				o.setRightB(value);
				cond.setRight(o.getRight());
			}
			int targetLineNumber = m.getFirstLineNumberOfBlock(targetLabel);
			result.put(targetLineNumber, cond);
		}
		return result;
	}
	
	public void setSwitchMap(Map<String, String> switchMap) {
		this.switchMap = switchMap;
	}
		

}
