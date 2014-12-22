package concolic;

import java.util.ArrayList;

public class ToDoPath {

	private ArrayList<String> pastChoices = new ArrayList<String>();
	private String targetPathStmtInfo = "";
	private String newDirection = "";
	private int choiceCounter = 0;
	
	
	public ArrayList<String> getPathChoices() {
		return pastChoices;
	}
	
	public void setPathChoices(ArrayList<String> pathChoices) {
		this.pastChoices = new ArrayList<String>();
		this.pastChoices.addAll(pathChoices);
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
		for (String pC : this.pastChoices) {
			String stmtInfo = pC.split(",")[0];
			String choice = pC.split(",")[1];
			if (stmtInfo.equals(pathStmtInfo))
				return choice;
		}
		return "";
	}
	
	public String getAPastChoice() {
		if (this.pastChoices.size() < 1 || choiceCounter > this.pastChoices.size()-1)
			return "";
		return this.pastChoices.get(choiceCounter++);
	}
	
}
