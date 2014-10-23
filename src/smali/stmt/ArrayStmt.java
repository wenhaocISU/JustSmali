package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class ArrayStmt extends StaticStmt{

	private boolean isPut = false;
	private boolean isGet = false;
	private boolean isFill = false;
	private String fillTabelLabel = "";
	
	public boolean isPut() {
		return isPut;
	}
	
	public void setIsPut(boolean isPut) {
		this.isPut = isPut;
	}
	
	public boolean isFill() {
		return isFill;
	}
	
	public void setIsFill(boolean isFill) {
		this.isFill = isFill;
	}
	
	public boolean isGet() {
		return isGet;
	}
	
	public void setIsGet(boolean isGet) {
		this.isGet = isGet;
	}
	
	public String getFillTabelLabel() {
		return fillTabelLabel;
	}
	
	public void setFillTabelLabel(String fillTabelLabel) {
		this.fillTabelLabel = fillTabelLabel;
	}
	
	
}
