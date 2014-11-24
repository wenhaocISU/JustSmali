package smali.stmt;

import staticFamily.StaticStmt;
import concolic.Condition;

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
	
	public Condition getCondition() {
		Condition result = new Condition();
		String op = getTheStmt().split(" ")[0].split("-")[1];
		result.setLeft(getvA());
		result.setRight(getvB());
		if (op.equals("eq"))			result.setOp("=");
		else if (op.equals("ne"))		result.setOp("!=");
		else if (op.equals("lt"))		result.setOp("<");
		else if (op.equals("ge"))		result.setOp(">=");
		else if (op.equals("gt"))		result.setOp(">");
		else if (op.equals("le"))		result.setOp("<=");
		else if (op.equals("eqz"))		{ result.setOp("="); result.setRight("0"); }
		else if (op.equals("nez"))		{ result.setOp("!="); result.setRight("0"); }
		else if (op.equals("ltz"))		{ result.setOp("<"); result.setRight("0"); }
		else if (op.equals("gez"))		{ result.setOp(">="); result.setRight("0"); }
		else if (op.equals("gtz"))		{ result.setOp(">"); result.setRight("0"); }
		else if (op.equals("lez"))		{ result.setOp("<="); result.setRight("0"); }
		return result;
	}
	
}
