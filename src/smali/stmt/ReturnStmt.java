package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class ReturnStmt extends StaticStmt {

	public boolean isReturnVoid() {
		return getTheStmt().startsWith("return-void");
	}
	
}
