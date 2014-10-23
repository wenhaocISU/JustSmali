package staticFamily;

import java.io.Serializable;

import smali.BlockLabel;

@SuppressWarnings("serial")
public class StaticStmt implements Serializable{

	private String theStmt = "";
	private int stmtID = -1;
	private int originalLineNumber = -1;
	private int newLineNumber = -1;
	private boolean flowsThrough = true;
	private BlockLabel blockLabel = new BlockLabel();
	
	public String getTheStmt() {
		return theStmt;
	}
	
	public void setTheStmt(String theStmt) {
		this.theStmt = theStmt;
	}
	
	public int getStmtID() {
		return stmtID;
	}
	
	public void setStmtID(int stmtID) {
		this.stmtID = stmtID;
	}
	
	public boolean hasOriginalLineNumber() {
		if (originalLineNumber == -1)
			return false;
		return true;
	}
	
	public int getSourceLineNumber() {
		if (originalLineNumber == -1)
			return newLineNumber;
		return originalLineNumber;
	}
	
	public void setOriginalLineNumber(int originalLineNumber) {
		this.originalLineNumber = originalLineNumber;
	}
	
	
	public void setNewLineNumber(int newLineNumber) {
		this.newLineNumber = newLineNumber;
	}
	
	public boolean isFlowsThrough() {
		return flowsThrough;
	}
	
	public void setFlowsThrough(boolean flowsThrough) {
		this.flowsThrough = flowsThrough;
	}
	
	public BlockLabel getBlockLabel() {
		return blockLabel;
	}
	
	public void setBlockLabel(BlockLabel blockLabel) {
		BlockLabel l = new BlockLabel();
		l.setNormalLabels(blockLabel.getNormalLabels());
		l.setTryLabels(blockLabel.getTryLabels());
		l.setNormalLabelSection(blockLabel.getNormalLabelSection());
		this.blockLabel = l;
	}
	
}
