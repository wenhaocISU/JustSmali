package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class FieldStmt extends StaticStmt {

	private String fieldSig;
	private boolean isGet = false;
	private boolean isPut = false;
	private boolean isStatic = false;
	

	public String getFieldSig() {
		return fieldSig;
	}

	public void setFieldSig(String fieldSig) {
		this.fieldSig = fieldSig;
	}

	public String getSrcV() {
		return getvA();
	}
	
	public String getDestV() {
		return getvA();
	}
	
	public String getObject() {
		if (!isStatic)
			return getvB();
		return "";
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

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}
	
}
