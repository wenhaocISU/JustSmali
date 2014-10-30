package concolic;

import java.util.ArrayList;

public class PathSummary {

	private ArrayList<String> executionLog = new ArrayList<String>();
	
	private Constraint pathConstraint;
	
	private VariableSymbolMap vsMap;

	public ArrayList<String> getExecutionLog() {
		return executionLog;
	}

	public void addExecutionLog(String executionLog) {
		this.executionLog.add(executionLog);
	}

	public Constraint getPathConstraint() {
		return pathConstraint;
	}

	public void setPathConstraint(Constraint pathConstraint) {
		this.pathConstraint = pathConstraint;
	}

	public VariableSymbolMap getVSMap() {
		return vsMap;
	}

	public void setVSMap(VariableSymbolMap vsMap) {
		this.vsMap = vsMap;
	}
	
	
}
