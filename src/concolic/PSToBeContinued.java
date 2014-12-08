package concolic;

public class PSToBeContinued {

	private PathSummary pathSummary = new PathSummary();
	
	private int toBeFlipped = -1;
	
	private int newDirection = -1;

	public PathSummary getPathSummary() {
		return pathSummary;
	}

	public void setPathSummary(PathSummary pathSummary) {
		this.pathSummary = pathSummary;
	}

	public int getToBeFlipped() {
		return toBeFlipped;
	}

	public void setToBeFlipped(int toBeFlipped) {
		this.toBeFlipped = toBeFlipped;
	}

	public int getNewDirection() {
		return newDirection;
	}

	public void setNewDirection(int newDirection) {
		this.newDirection = newDirection;
	}
	
	
	
	
}
