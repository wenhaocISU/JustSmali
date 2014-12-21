package concolic;

import java.util.ArrayList;

public class ToDoPath {

	private ArrayList<String> pathChoices = new ArrayList<String>();
	private String targetPathStmtInfo = "";
	private String newDirection = "";
	
	
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

	public String getNewDirection() {
		return newDirection;
	}

	public void setNewDirection(String newDirection) {
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

}
