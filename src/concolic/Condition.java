package concolic;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Condition implements Serializable{

	private String left = "";
	private String op = "";
	private String right = "";
	
	public String getLeft() {
		return left;
	}
	
	public void setLeft(String left) {
		this.left = left;
	}

	public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}
	
	public String toString() {
		return left + " " + op + " " + right;
	}
	

	
}
