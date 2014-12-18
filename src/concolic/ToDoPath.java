package concolic;

import java.util.ArrayList;

public class ToDoPath {

	private ArrayList<String> pathChoices = new ArrayList<String>();
	private String targetPathStmtInfo = "";
	private int newDirection = -1;
	
	public ArrayList<String> getPathChoices() {
		return pathChoices;
	}
	
	public void setPathChoices(ArrayList<String> pathChoices) {
		this.pathChoices = pathChoices;
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
	
	
}
