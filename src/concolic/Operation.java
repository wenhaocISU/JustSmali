package concolic;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Operation implements Serializable{

	private String left = "";
	private boolean noOp;
	private String rightA = "";
	private String op = "";
	private String rightB = "";
	
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
	
	public String toOldString() {
		if (noOp)
			return left + " = " + rightA;
		return left + " = " + rightA + " " + op + " " + rightB;
	}
	
	public String toString() {
		if (noOp)
			return "(= " + left + " " + "(" + rightA + " ))";
		return "(= " + left + " " + "(" + op + " " + rightA + " " + rightB + " " + "))";
	}
	
	public String getRight() {
		if (noOp) {
			if (rightA.startsWith("(") && rightA.endsWith(")"))
				return rightA;
			return "(" + rightA + " )";
		}
		return "(" + op + " " + rightA + " " + rightB + " )";
	}

	public Operation clone() {
		Operation result = new Operation();
		result.setLeft(this.left);
		result.setNoOp(this.noOp);
		result.setRightA(this.rightA);
		result.setRightB(this.rightB);
		return result;
	}
	
}
