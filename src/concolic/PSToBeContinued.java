package concolic;

import java.util.ArrayList;

public class PSToBeContinued {

	private PathSummary pathSummary = new PathSummary();
	
	private String condStmtInfo = "";
	
	private ArrayList<Integer> remainingPaths = new ArrayList<Integer>(); 

	public PathSummary getPathSummary() {
		return pathSummary;
	}

	public void setPathSummary(PathSummary pathSummary) {
		this.pathSummary = pathSummary;
	}

	public String getCondStmtInfo() {
		return condStmtInfo;
	}

	public void setCondStmtInfo(String condStmtInfo) {
		this.condStmtInfo = condStmtInfo;
	}

	public ArrayList<Integer> getRemainingPaths() {
		return remainingPaths;
	}

	public void setRemainingPaths(ArrayList<Integer> remainingPaths) {
		this.remainingPaths = remainingPaths;
	}



	
}
