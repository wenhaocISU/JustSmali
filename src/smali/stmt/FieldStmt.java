package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class FieldStmt extends StaticStmt {

	private String fieldSig;
	private boolean isGet = false;
	private boolean isPut = false;

	public String getFieldSig() {
		return fieldSig;
	}

	public void setFieldSig(String fieldSig) {
		this.fieldSig = fieldSig;
	}

	public boolean isGet() {
		return isGet;
	}

	public void setIsGet(boolean isGet) {
		this.isGet = isGet;
	}

	public boolean isPut() {
		return isPut;
	}

	public void setIsPut(boolean isPut) {
		this.isPut = isPut;
	}
	
}
