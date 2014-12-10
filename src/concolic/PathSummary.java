package concolic;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class PathSummary implements Serializable{

	private ArrayList<String> executionLog = new ArrayList<String>();
	private ArrayList<Condition> pathCondition = new ArrayList<Condition>();
	private ArrayList<Operation> symbolicStates = new ArrayList<Operation>();
	
	public ArrayList<String> getExecutionLog() {
		return executionLog;
	}
	
	public void addExecutionLog(String newLine) {
		this.executionLog.add(newLine);
	}

	public ArrayList<Condition> getPathCondition() {
		return pathCondition;
	}

	public void setPathCondition(ArrayList<Condition> pathConditions) {
		this.pathCondition = pathConditions;
	}

	public ArrayList<Operation> getSymbolicStates() {
		return symbolicStates;
	}

	public void setSymbolicStates(ArrayList<Operation> symbolicStates) {
		this.symbolicStates = symbolicStates;
	}
	
	public PathSummary clone() {
		PathSummary result = new PathSummary();
		for (String s : executionLog)
			result.addExecutionLog(s);
		ArrayList<Operation> sStates = new ArrayList<Operation>();
		for (Operation o : symbolicStates)
			sStates.add(o);
		result.setSymbolicStates(sStates);
		ArrayList<Condition> pCond = new ArrayList<Condition>();
		for (Condition cond : pathCondition)
			pCond.add(cond);
		result.setPathCondition(pCond);
		return result;
	}
	
}
