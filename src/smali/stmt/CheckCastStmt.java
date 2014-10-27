package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class CheckCastStmt extends StaticStmt{

	public String getCheckingType() {
		return getvB();
	}

	
}
