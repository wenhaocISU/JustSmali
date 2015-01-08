package smali.stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import concolic.Expression;

@SuppressWarnings("serial")
public class SwitchStmt extends StaticStmt{

	private boolean isPswitch;
	private boolean isSswitch;
	private String switchMapLabel;
	private String pSwitchInitValue;
	private Map<Integer, String> switchMap = new HashMap<Integer, String>();
	
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
	
	public Map<Integer, Integer> getSwitchMap(StaticMethod m) {
		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (Map.Entry<Integer, String> entry : switchMap.entrySet()) {
			int realValue = entry.getKey();
			int targetLineNumber = m.getFirstLineNumberOfBlock(entry.getValue());
			result.put(realValue, targetLineNumber);
		}
		return result;
	}

	public void setSwitchMap(Map<Integer, String> switchMap) {
		this.switchMap = switchMap;
	}
		
	public int getFlowThroughLineNumber(StaticMethod m) {
		return m.getSmaliStmts().get(getStmtID()+1).getSourceLineNumber();
	}
	
	public Expression getSwitchCondition(int value) {
		if (!this.switchMap.containsKey(value))
			return null;
		Expression result = new Expression("==");
		result.add(new Expression(getSwitchV()));
		result.add(new Expression("#" + value));
		return result;
	}
	
	public ArrayList<Expression> getFlowThroughConditions() {
		ArrayList<Expression> result = new ArrayList<Expression>();
		for (int value : this.switchMap.keySet()) {
			Expression cond = new Expression("!=");
			cond.add(new Expression(getSwitchV()));
			cond.add(new Expression("#" + value));
			result.add(cond);
		}
		return result;
	}
	
}
