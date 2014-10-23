package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class GotoStmt extends StaticStmt{

	private String targetLabel;

	public String getTargetLabel() {
		return targetLabel;
	}

	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}
	
	
}
