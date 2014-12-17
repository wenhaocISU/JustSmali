package concolic;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class PathSummary implements Serializable{

	private ArrayList<String> executionLog = new ArrayList<String>();
	private ArrayList<Condition> pathCondition = new ArrayList<Condition>();
	private ArrayList<Operation> symbolicStates = new ArrayList<Operation>();
	
	private ArrayList<String> pathChoices = new ArrayList<String>();
	
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

	public void addSymbolicState(Operation newO) {
		this.symbolicStates.add(newO);
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
		ArrayList<String> pathChoices = new ArrayList<String>();
		for (String pathChoice: this.pathChoices)
			pathChoices.add(pathChoice);
		result.setPathChoices(pathChoices);
		return result;
	}

	public ArrayList<String> getPathChoices() {
		return pathChoices;
	}

	public void setPathChoices(ArrayList<String> pathChoices) {
		this.pathChoices = pathChoices;
	}
	
	public void addPathChoice(String pathChoice) {
		if (!this.pathChoices.contains(pathChoice))
			this.pathChoices.add(pathChoice);
	}
	
	public void updatePathCondition(Condition newCond) {
		boolean leftDone = false, rightDone = false;
		if (newCond.getRight().startsWith("#"))
			rightDone = true;
		for (Operation o : this.symbolicStates) {
			if (!leftDone && o.getLeft().equals(newCond.getLeft())) {
				newCond.setLeft(o.getRight());
				leftDone = true;
			}
			if (!rightDone && o.getLeft().equals(newCond.getRight())) {
				newCond.setRight(o.getRight());
				rightDone = true;
			}
			if (leftDone && rightDone)
				break;
		}
		this.pathCondition.add(newCond);
	}
	
	public void updateReturnSymbol(String vName) throws Exception{
		int index = getIndexOfOperationWithLeft(vName);
		if (index < 0)
			throw (new Exception("Can't find the assignment of returned variable from symbolicStates"));
		Operation theAssignO = this.symbolicStates.get(index);
		theAssignO.setLeft("$newestInvokeResult");
		this.symbolicStates.set(index, theAssignO);
		if (theAssignO.isNoOp()) {
			for (int i = index+1; i < this.symbolicStates.size(); i++) {
				Operation o = this.symbolicStates.get(i);
				if (o.getLeft().endsWith(theAssignO.getRightA())) {
					String newLeft = o.getLeft().replace(theAssignO.getRightA(), "$newestInvokeResult");
					o.setLeft(newLeft);
					if (o.getLeft().equals(this.symbolicStates.get(i).getLeft()))
						System.out.println("Good News in updateReturnSymbol. Can delete the set(i, o)");
					this.symbolicStates.set(i, o);
				}
			}
		}
	}
	
	public void updateSymbolicStates(Operation newO, boolean newSymbol) {
		int index = getIndexOfOperationWithLeft(newO.getLeft());
		boolean ADone = false, BDone = false;
		String rightA = newO.getRightA(), rightB = newO.getRightB();
		if (rightA.equals("$newestInvokeResult") || rightA.startsWith("#"))
			ADone = true;
		if (rightA.equals("$newestInvokeResult") || newO.isNoOp() || rightB.startsWith("#"))
			BDone = true;
		for (Operation o : this.symbolicStates) {
			//if (!ADone)
		}
		//TODO if (newSymbol):
		//			if ($Fstatic), no action needed.
		//			if ($Finstance), replace right most object if can
		//			if ($newestInvokeResult), 
		
		//TODO if index >= 0, replace operation;
	}
	
	public void mergeWithInvokedPS(PathSummary subPS) {
		
	}
	
	private int getIndexOfOperationWithLeft(String vName) {
		for (int i = 0; i < this.symbolicStates.size(); i++) {
			Operation o = this.symbolicStates.get(i);
			if (o.getLeft().equals(vName))
				return i;
		}
		return -1;
	}
	
}
