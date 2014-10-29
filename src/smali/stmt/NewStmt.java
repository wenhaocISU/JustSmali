package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class NewStmt extends StaticStmt{

	private boolean isNewArray;
	
	private boolean isNewInstance;
	
	private boolean newArrayMoved;

	private String arguments;
	
	public boolean newArrayMoved() {
		return newArrayMoved;
	}

	public void setNewArrayMoved(boolean newArrayMoved) {
		this.newArrayMoved = newArrayMoved;
	}

	public boolean isNewArray() {
		return isNewArray;
	}

	public void setNewArray(boolean isNewArray) {
		this.isNewArray = isNewArray;
	}

	public boolean isNewInstance() {
		return isNewInstance;
	}

	public void setNewInstance(boolean isNewInstance) {
		this.isNewInstance = isNewInstance;
	}

	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}
	
}
