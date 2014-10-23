package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class IfStmt extends StaticStmt{

	private String targetLabel;

	public String getTargetLabel() {
		return targetLabel;
	}

	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}
	
	public boolean isIfZ() {
		return (this.getTheStmt().split(" ")[0].endsWith("z"));
	}
	
}
