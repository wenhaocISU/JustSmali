package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class InvokeStmt extends StaticStmt{

	private String targetSig;
	private String params;
	private String invokeType;
	
	private boolean resultsMoved;

	public String getTargetSig() {
		return targetSig;
	}

	public void setTargetSig(String targetSig) {
		this.targetSig = targetSig;
	}

	public boolean resultsMoved() {
		return resultsMoved;
	}

	public void setResultsMoved(boolean resultsMoved) {
		this.resultsMoved = resultsMoved;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getInvokeType() {
		return invokeType;
	}

	public void setInvokeType(String invokeType) {
		this.invokeType = invokeType;
	}
	
}
