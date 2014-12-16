package concolic;

import staticFamily.StaticMethod;

public class PSToBeContinued {

	private PathSummary pathSummary = new PathSummary();
	
	private StaticMethod currentMethod = new StaticMethod();
	
	private int nextStepLineNumber; 

	public PathSummary getPathSummary() {
		return pathSummary;
	}

	public void setPathSummary(PathSummary pathSummary) {
		this.pathSummary = pathSummary;
	}

	public int getNextStepLineNumber() {
		return nextStepLineNumber;
	}

	public void setNextStepLineNumber(int nextStepLineNumber) {
		this.nextStepLineNumber = nextStepLineNumber;
	}

	public StaticMethod getCurrentMethod() {
		return currentMethod;
	}

	public void setCurrentMethod(StaticMethod currentMethod) {
		this.currentMethod = currentMethod;
	}

}
