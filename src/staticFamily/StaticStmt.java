package staticFamily;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import smali.BlockLabel;
import concolic.Operation;

@SuppressWarnings("serial")
public class StaticStmt implements Serializable{

	private String theStmt = "";
	private int stmtID = -1;
	
	private int originalLineNumber = -1;
	private int newLineNumber = -1;
	
	private String vA = "", vB = "", vC = "";
	
	private boolean hasCatch;
	private boolean hasFinally;
	private String exceptionType = "";
	private String catchTgtLabel = "";
	private String finallyTgtLabel = "";
	
	private boolean flowsThrough = true;
	
	private boolean generatesSymbol = false; // iget, sget, invoke&result_moved, 
	
	private boolean hasOperation = false;
	private Operation operation = new Operation();
	
	private boolean updatesPathCondition = false;
	
	private boolean endsMethod = false;
	
	private BlockLabel blockLabel = new BlockLabel();
		
	private Map<String, String> vDebugInfo = new HashMap<String, String>();
	
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
	
	public boolean flowsThrough() {
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

	public boolean hasCatch() {
		return hasCatch;
	}

	public void setHasCatch(boolean hasCatch) {
		this.hasCatch = hasCatch;
	}

	public boolean hasFinally() {
		return hasFinally;
	}

	public void setHasFinally(boolean hasFinally) {
		this.hasFinally = hasFinally;
	}

	public String getCatchTgtLabel() {
		return catchTgtLabel;
	}

	public void setCatchTgtLabel(String catchTgtLabel) {
		this.catchTgtLabel = catchTgtLabel;
	}

	public String getFinallyTgtLabel() {
		return finallyTgtLabel;
	}

	public void setFinallyTgtLabel(String finallyTgtLabel) {
		this.finallyTgtLabel = finallyTgtLabel;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(String exceptionType) {
		this.exceptionType = exceptionType;
	}

	public String getvA() {
		return vA;
	}

	public void setvA(String vA) {
		this.vA = vA;
	}

	public String getvB() {
		return vB;
	}

	public void setvB(String vB) {
		this.vB = vB;
	}

	public String getvC() {
		return vC;
	}

	public void setvC(String vC) {
		this.vC = vC;
	}

	public boolean generatesSymbol() {
		return generatesSymbol;
	}

	public void setGeneratesSymbol(boolean generatesSymbol) {
		this.generatesSymbol = generatesSymbol;
	}

	public boolean hasOperation() {
		return hasOperation;
	}

	public void setHasOperation(boolean hasOperation) {
		this.hasOperation = hasOperation;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
		System.out.println("[New Operation]" + operation.toString());
	}
	
	public Map<String, String> getvDebugInfo() {
		return vDebugInfo;
	}

	public void addvDebugInfo(String localName, String debugName) {
		this.vDebugInfo.put(localName, debugName);
	}

	public boolean updatesPathCondition() {
		return updatesPathCondition;
	}

	public void setUpdatesPathCondition(boolean updatesPathCondition) {
		this.updatesPathCondition = updatesPathCondition;
	}

	public boolean endsMethod() {
		return endsMethod;
	}

	public void setEndsMethod(boolean endsMethod) {
		this.endsMethod = endsMethod;
	}

}
