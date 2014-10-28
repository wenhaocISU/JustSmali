package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class ConstStmt extends StaticStmt{

	public String getV() {
		return getvA();
	}
	
	public String getValue() {
		return getvB();
	}
	
}
