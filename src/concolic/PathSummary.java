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
			throw (new Exception("Can't find the assignment of returned variable '" + vName + "'from symbolicStates"));
		Operation theAssignO = this.symbolicStates.get(index);
		theAssignO.setLeft("$return");
		if (theAssignO.isNoOp()) {
			for (int i = index+1; i < this.symbolicStates.size(); i++) {
				Operation o = this.symbolicStates.get(i);
				if (o.getLeft().endsWith(theAssignO.getRightA())) {
					String newLeft = o.getLeft().replace(theAssignO.getRightA(), "$return");
					o.setLeft(newLeft);
				}
			}
		}
	}
	
	public void updateSymbolicStates(Operation oToAdd, boolean newSymbol) {
		// only 4 possible scenarios:
		// 1. vi = vm op vn
		// 2. vi = $return
		// 3. vi = $Finstance>>..>>vm
		// 4. vi = $Fstatic>>... (this one no need to replace anything)
		Operation newO = oToAdd.clone();
		int index = getIndexOfOperationWithLeft(newO.getLeft());
		// scenario 1
		if (!newSymbol) {
			boolean ADone = false, BDone = false;
			if (newO.getRightA().startsWith("#"))	ADone = true;
			if (newO.isNoOp() || newO.getRightB().startsWith("#"))	BDone = true;
			for (Operation o : this.symbolicStates) {
				if (!ADone && o.getLeft().equals(newO.getRightA())) {
					newO.setRightA(o.getRight());
					ADone = true;
				}
				if (!BDone && o.getLeft().equals(newO.getRightB())) {
					newO.setRightB(o.getRight());
					BDone = true;
				}
				if (ADone && BDone)
					break;
			}
			System.out.println("[Wanna Add]" + newO.toString());
			if (index > -1) {
				System.out.println("[Deleting]" + this.symbolicStates.get(index));
				this.symbolicStates.remove(index);
			}
			this.symbolicStates.add(newO);
			System.out.println("[Added]" + newO.toString());
		}
		// scenario 2
		else if (newO.getRightA().equals("$return")){
			int assignOIndex = getIndexOfOperationWithLeft("$return");
			Operation assignO = this.symbolicStates.get(assignOIndex);
			assignO.setLeft(newO.getLeft());
			if (!assignO.isNoOp())
				for (int i = assignOIndex+1; i < this.symbolicStates.size(); i++) {
					Operation o = this.symbolicStates.get(i);
					if (o.getLeft().contains("$return")) {
						o.setLeft(o.getLeft().replace("$return", assignO.getRightA()));
					}
				}
		}
		// scenario 3 & 4
		else{
			if (newO.getRightA().startsWith("$Finstance")) {
				String prefix = newO.getRightA().substring(0, newO.getRightA().lastIndexOf(">>")+2);
				String objectName = newO.getRightA().substring(newO.getRightA().lastIndexOf(">>")+2);
				for (Operation o : this.symbolicStates) {
					if (o.getLeft().equals(objectName)) {
						newO.setRightA(prefix + o.getRight());
						break;
					}
				}
			}
			if (index > -1)
				this.symbolicStates.remove(index);
			this.symbolicStates.add(newO);
		}
	}
	
	public void mergeWithInvokedPS(PathSummary subPS) {
		this.executionLog = subPS.getExecutionLog();
		this.pathChoices = subPS.getPathChoices();
		this.pathCondition = subPS.getPathCondition();
		for (Operation o : subPS.getSymbolicStates()) {
			if (o.getLeft().contains("$return"))
				this.symbolicStates.add(o);
			else if (o.getLeft().contains("$Fstatic")){
				int index = getIndexOfOperationWithLeft(o.getLeft());
				if (index > -1)
					this.symbolicStates.remove(index);
				this.symbolicStates.add(o);
			}
		}
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
