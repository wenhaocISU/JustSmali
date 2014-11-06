package concolic;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Operation implements Serializable{

	private String left;
	private boolean noOp;
	private String rightA;
	private String op;
	private String rightB;
	
	public String getLeft() {
		return left;
	}
	
	public void setLeft(String left) {
		this.left = left;
	}
	
	public boolean isNoOp() {
		return noOp;
	}
	
	public void setNoOp(boolean noOp) {
		this.noOp = noOp;
	}
	
	public String getRightA() {
		return rightA;
	}
	
	public void setRightA(String rightA) {
		this.rightA = rightA;
	}
	
	public String getRightB() {
		return rightB;
	}
	
	public void setRightB(String rightB) {
		this.rightB = rightB;
	}

	public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}
	
	public String toString() {
		return left + " = " + rightA + " " + op + " " + rightB;
	}

}
