package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class V2OPStmt extends StaticStmt{

	private boolean has3rdConstArg;

	public boolean has3rdConstArg() {
		return has3rdConstArg;
	}

	public void setHas3rdConstArg(boolean has3rdConstArg) {
		this.has3rdConstArg = has3rdConstArg;
	}
	
	
}
