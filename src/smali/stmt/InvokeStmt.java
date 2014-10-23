package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class InvokeStmt extends StaticStmt{

	private String targetSig;
	
	private boolean resultsMoved;

	public String getTargetSig() {
		return targetSig;
	}

	public void setTargetSig(String targetSig) {
		this.targetSig = targetSig;
	}

	public boolean isResultsMoved() {
		return resultsMoved;
	}

	public void setResultsMoved(boolean resultsMoved) {
		this.resultsMoved = resultsMoved;
	}
	
}
