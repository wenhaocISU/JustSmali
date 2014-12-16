package concolic;

import staticFamily.StaticMethod;

public class PSToBeContinued {

	private PathSummary pathSummary = new PathSummary();
	
	private StaticMethod currentMethod = new StaticMethod();
	
	private int nextStepLineNumber; 

	public PSToBeContinued(PathSummary pS, StaticMethod m, int i) {
		this.pathSummary = pS;
		this.currentMethod = m;
		this.nextStepLineNumber = i;
	}
	
	public PathSummary getPathSummary() {
		return pathSummary;
	}

	public int getNextStepLineNumber() {
		return nextStepLineNumber;
	}

	public StaticMethod getCurrentMethod() {
		return currentMethod;
	}


}
