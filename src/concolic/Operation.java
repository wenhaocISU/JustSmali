package concolic;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Operation implements Serializable{

	// Ignoring: aput, aget, instance-of, array-length
	// Addition: iput/sput won't have operation in StaticInfo, because it's better to be done at runtime, with Object id info, etc. Easier to describe the symbol.
	// Ignoring: cmpl-float, cmpg-float, cmpl-double, cmpg-double. Because this is not really operation, this is a condition. Since they are not int, they have to
	// do operation and return a result. Unlike int where you can just do comparison in the if statement.
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
	
	public String toString() {
		return left + " = " + rightA + " " + op + " " + rightB;
	}

}
