package smali.stmt;

import analysis.Expression;
import staticFamily.StaticMethod;
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
	
	public Expression getJumpCondition() {
		String op = getTheStmt().split(" ")[0].split("-")[1];
		String newOp = "";
		String left = getvA();
		String right = getvB();
		if (op.equals("eq"))			newOp = "==";
		else if (op.equals("ne"))		newOp = "!=";
		else if (op.equals("lt"))		newOp = "<";
		else if (op.equals("ge"))		newOp = ">=";
		else if (op.equals("gt"))		newOp = ">";
		else if (op.equals("le"))		newOp = "<=";
		else if (op.equals("eqz"))		{ newOp = "==";  right = "#0"; }
		else if (op.equals("nez"))		{ newOp = "!="; right = "#0"; }
		else if (op.equals("ltz"))		{ newOp = "<";  right = "#0"; }
		else if (op.equals("gez"))		{ newOp = ">="; right = "#0"; }
		else if (op.equals("gtz"))		{ newOp = ">";  right = "#0"; }
		else if (op.equals("lez"))		{ newOp = "<="; right = "#0"; }
		Expression result = new Expression(newOp);
		result.add(new Expression(left));
		result.add(new Expression(right));
		return result;
	}
	
	public int getJumpTargetLineNumber(StaticMethod m) {
		return m.getFirstLineNumberOfBlock(getTargetLabel());
	}
	
	public int getFlowThroughTargetLineNumber(StaticMethod m) {
		int stmtID = m.getSmaliStmts().indexOf(this);
		if (stmtID == -1)
			return -1;
		return m.getSmaliStmts().get(stmtID+1).getSourceLineNumber();
	}
	
}
