package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class MoveStmt extends StaticStmt{

	private int resultMovedFrom = -1;
	
	public String getDestV() {
		return getvA();
	}
	
	public String getSourceV() {
		return getvB();
	}
	
	public boolean isMoveResult() {
		return (this.getTheStmt().startsWith("move-result"));
	}

	public int getResultMovedFrom() {
		return resultMovedFrom;
	}

	public void setResultMovedFrom(int resultMovedFrom) {
		this.resultMovedFrom = resultMovedFrom;
	}
	
}
