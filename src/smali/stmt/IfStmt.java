package smali.stmt;

import staticFamily.StaticStmt;

@SuppressWarnings("serial")
public class IfStmt extends StaticStmt{

	private boolean has1V, has2V;

	public String getTargetLabel() {
		if (has1V)
			return getvB();
		else return getvC();
	}

	
	public boolean isIfZ() {
		return (this.getTheStmt().split(" ")[0].endsWith("z"));
	}

	public boolean has1V() {
		return has1V;
	}

	public void setHas1V(boolean has1v) {
		has1V = has1v;
	}

	public boolean has2V() {
		return has2V;
	}

	public void setHas2V(boolean has2v) {
		has2V = has2v;
	}
	
}
