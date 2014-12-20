package concolic;

import java.util.ArrayList;

public class ToDoPath {

	private ArrayList<String> pathChoices = new ArrayList<String>();
	private ArrayList<String> pastSwitchStmtsThatFlowedThrough = new ArrayList<String>();
	private String targetPathStmtInfo = "";
	private int newDirection = -1;
	private boolean shouldFlowThroughThisSwitchStmt = false;
	
	public ArrayList<String> getPathChoices() {
		return pathChoices;
	}
	
	public void setPathChoices(ArrayList<String> pathChoices) {
		this.pathChoices = new ArrayList<String>();
		this.pathChoices.addAll(pathChoices);
	}

	public String getTargetPathStmtInfo() {
		return targetPathStmtInfo;
	}

	public void setTargetPathStmtInfo(String targetPathStmtInfo) {
		this.targetPathStmtInfo = targetPathStmtInfo;
	}

	public int getNewDirection() {
		return newDirection;
	}

	public void setNewDirection(int newDirection) {
		this.newDirection = newDirection;
	}
	
	public String getPastChoice(String pathStmtInfo) {
		for (String pC : this.pathChoices) {
			String stmtInfo = pC.split(",")[0];
			String choice = pC.split(",")[1];
			if (stmtInfo.equals(pathStmtInfo))
				return choice;
		}
		return "";
	}

	public boolean shouldFlowThroughThisSwitchStmt() {
		return shouldFlowThroughThisSwitchStmt;
	}

	public void setShouldFlowThroughThisSwitchStmt(boolean shouldFlowThroughThisSwitchStmt) {
		this.shouldFlowThroughThisSwitchStmt = shouldFlowThroughThisSwitchStmt;
	}

	public ArrayList<String> getPastSwitchStmtsThatFlowedThrough() {
		return pastSwitchStmtsThatFlowedThrough;
	}

	public void setPastSwitchStmtsThatFlowedThrough(ArrayList<String> pastSwitchStmtsThatFlowedThrough) {
		this.pastSwitchStmtsThatFlowedThrough = pastSwitchStmtsThatFlowedThrough;
	}

	public void addPastSwitchStmtThatFlowedThrough(String switchStmtInfo) {
		this.pastSwitchStmtsThatFlowedThrough.add(switchStmtInfo);
	}
	
}
