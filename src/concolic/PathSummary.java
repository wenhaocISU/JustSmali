package concolic;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class PathSummary implements Serializable{

	private ArrayList<Integer> executionLog = new ArrayList<Integer>();
	private ArrayList<Condition> pathCondition = new ArrayList<Condition>();
	private ArrayList<Operation> symbolicStates = new ArrayList<Operation>();
	
	public ArrayList<Integer> getExecutionLog() {
		return executionLog;
	}
	
	public void addExecutionLog(int lineNumber) {
		this.executionLog.add(lineNumber);
	}

	public ArrayList<Condition> getPathCondition() {
		return pathCondition;
	}

	public void addPathCondition(Condition pathCondition) {
		this.pathCondition.add(pathCondition);
	}

	public ArrayList<Operation> getSymbolicStates() {
		return symbolicStates;
	}

	public void setSymbolicStates(ArrayList<Operation> symbolicStates) {
		this.symbolicStates = symbolicStates;
	}
	
}
