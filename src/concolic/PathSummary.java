package concolic;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class PathSummary implements Serializable{

	private String methodSignature = "";
	
	private ArrayList<String> executionLog = new ArrayList<String>();
	private ArrayList<Expression> symbolicStates = new ArrayList<Expression>();
	private ArrayList<Expression> pathCondition = new ArrayList<Expression>();
	
	private ArrayList<String> pathChoices = new ArrayList<String>();
	
	public ArrayList<String> getExecutionLog() {
		return executionLog;
	}
	
	public void addExecutionLog(String newLine) {
		this.executionLog.add(newLine);
	}

	public ArrayList<Expression> getPathCondition() {
		return pathCondition;
	}

	public void setPathCondition(ArrayList<Expression> pathConditions) {
		this.pathCondition = pathConditions;
	}

	public ArrayList<Expression> getSymbolicStates() {
		return symbolicStates;
	}

	public void addSymbolicState(Expression newO) {
		this.symbolicStates.add(newO);
	}
	
	public void setSymbolicStates(ArrayList<Expression> symbolicStates) {
		this.symbolicStates = symbolicStates;
	}
	
	public PathSummary clone() {
		PathSummary result = new PathSummary();
		for (String s : executionLog)
			result.addExecutionLog(s);
		ArrayList<Expression> sStates = new ArrayList<Expression>();
		for (Expression o : symbolicStates)
			sStates.add(o);
		result.setSymbolicStates(sStates);
		ArrayList<Expression> pCond = new ArrayList<Expression>();
		for (Expression cond : pathCondition)
			pCond.add(cond);
		result.setPathCondition(pCond);
		ArrayList<String> pathChoices = new ArrayList<String>();
		for (String pathChoice: this.pathChoices)
			pathChoices.add(pathChoice);
		result.setPathChoices(pathChoices);
		result.setMethodSignature(this.methodSignature);
		return result;
	}

	public ArrayList<String> getPathChoices() {
		return pathChoices;
	}

	public void setPathChoices(ArrayList<String> pathChoices) {
		this.pathChoices = pathChoices;
	}
	
	public void addPathChoice(String pathChoice) {
		if (!this.pathChoices.contains(pathChoice))
			this.pathChoices.add(pathChoice);
	}
	
	public void updatePathCondition(Expression newCond) {
		Expression left = (Expression) newCond.getChildAt(0);
		Expression right = (Expression) newCond.getChildAt(1);
		Expression updatedLeft = this.findExistingExpression(left);
		newCond.remove(0);
		newCond.insert(updatedLeft, 0);
		if (!right.getUserObject().toString().startsWith("#")) {
			Expression updatedRight = this.findExistingExpression(right);
			newCond.remove(1);
			newCond.insert(updatedRight, 1);
		}
		this.pathCondition.add(newCond);
	}
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
	public void updateReturnSymbol(String vName) throws Exception{
		int index = getIndexOfOperationWithLeft(vName);
		if (index < 0)
			throw (new Exception("Can't find the assignment of returned variable '" + vName + "'from symbolicStates"));
		Expression theAssignExpr = this.symbolicStates.get(index);
		theAssignExpr.remove(0);
		theAssignExpr.insert(new Expression("$return"), 0);
		Expression assignRight = (Expression) theAssignExpr.getChildAt(1);
		for (int i = index+1; i < this.symbolicStates.size(); i++) {
			Expression ex = this.symbolicStates.get(i);
			Expression left = (Expression) ex.getChildAt(0);
			if (ExpressionContains(left, assignRight))
				left.replace(assignRight, new Expression("$return"));
		}
	}
	

	public void updateSymbolicStates(Expression newEx) {
		//1  v0 = v1
		//2  v0 = v1 add v2
		//3  v0 = sig $Finstance v1
		//4  v0 = sig $Fstatic v1
		//5  v0 = $return
		Expression left = (Expression) newEx.getChildAt(0);
		Expression right = (Expression) newEx.getChildAt(1);
		// if left is '$Finstance>>a>>v0', replace v0 first
		if (left.getUserObject().toString().equals("$Finstance")) {
			Expression obj = (Expression) left.getChildAt(1);
			Expression updatedObj = this.findExistingExpression(obj);
			if (updatedObj != null) {
				left.remove(1);
				left.insert(updatedObj, 1);
			}
		}
		int index = getIndexOfOperationWithLeft(left);
		String op = right.getUserObject().toString();
		if (op.equals("$return")) {
			//5 v0 = $return
			int assignIndex = getIndexOfOperationWithLeft("$return");
			Expression assignEx = this.symbolicStates.get(assignIndex);
			assignEx.remove(0);
			assignEx.insert(left, 0);
			Expression assignRight = (Expression) assignEx.getChildAt(1);
			ArrayList<Expression> toRemove = new ArrayList<Expression>();
			for (int i = assignIndex+1; i < this.symbolicStates.size(); i++) {
				Expression ex = this.symbolicStates.get(i);
				Expression thisLeft = (Expression) ex.getChildAt(0);
				if (ExpressionContains(thisLeft, "$return"))
					thisLeft.replace(new Expression("$return"), assignRight);
				int thisIndex = getIndexOfOperationWithLeft(thisLeft);
				if (thisIndex != i && thisIndex > -1)
					toRemove.add(ex.clone());
			}
			if (index > -1) {
				this.symbolicStates.remove(index);
			}
			for (Expression ex : toRemove) {
				Expression thisLeft = (Expression) ex.getChildAt(0);
				int i = getIndexOfOperationWithLeft(thisLeft);
				this.symbolicStates.remove(i);
			}
		}
		else if (right.getChildCount() == 0 || op.equals("$Fstatic")) {
			//1&4 v0 = v1 or $Fstatic sig
			// left = v0
			// right = v1 or $Fstatic sig
			if (!op.startsWith("#")) {
				Expression updatedRight = this.findExistingExpression(right);
				if (updatedRight != null) {
					newEx.remove(1);
					newEx.insert(updatedRight, 1);
				}
			}
			if (index > -1)
				this.symbolicStates.remove(index);
			this.symbolicStates.add(newEx);
		}
		else if (op.equals("$Finstance")) {
			//3 v0 = sig $Finstance v1
			Expression obj = (Expression) right.getChildAt(1);
			Expression updatedObj = this.findExistingExpression(obj);
			right.remove(1);
			right.insert(updatedObj, 1);
			if (index > -1)
				this.symbolicStates.remove(index);
			this.symbolicStates.add(newEx);
		}
		else {
			//2 v0 = v1 add v2
			// left = v0
			// right = add v1 v2
			for (int i = 0; i < right.getChildCount(); i++) {
				Expression childOfRight = (Expression) right.getChildAt(i);
				Expression updatedChild = this.findExistingExpression(childOfRight);
				if (updatedChild != null) {
					right.remove(i);
					right.insert(updatedChild, i);
				}
			}
			if (index > -1)
				this.symbolicStates.remove(index);
			this.symbolicStates.add(newEx);
		}
	}
	
	
	public void mergeWithInvokedPS(PathSummary subPS) {
		this.executionLog = subPS.getExecutionLog();
		this.pathChoices = subPS.getPathChoices();
		this.pathCondition = subPS.getPathCondition();
		for (Expression ex : subPS.getSymbolicStates()) {
			// to add:
			// 1 $return = ... 
			// 2 $Fstatic = ...
			Expression left = (Expression) ex.getChildAt(0);
			if (ExpressionContains(left, "$return")) {
				this.symbolicStates.add(ex);
			}
			else if (ExpressionContains(left, "$Fstatic")) {
				int index = this.getIndexOfOperationWithLeft(left);
				if (index > -1)
					this.symbolicStates.remove(index);
				this.symbolicStates.add(ex);
			}
		}
	}
	
	private int getIndexOfOperationWithLeft(String vName) {
		for (int i = 0; i < this.symbolicStates.size(); i++) {
			Expression ex = this.symbolicStates.get(i);
			Expression left = (Expression) ex.getChildAt(0);
			if (left.getUserObject().toString().equals(vName))
				return i;
		}
		return -1;
	}
	
	private int getIndexOfOperationWithLeft(Expression left) {
		for (int i = 0; i < this.symbolicStates.size(); i++) {
			Expression ex = this.symbolicStates.get(i);
			Expression thisLeft = (Expression) ex.getChildAt(0);
			if (thisLeft.equals(left))
				return i;
		}
		return -1;
	}
	

	public String getMethodSignature() {
		return methodSignature;
	}

	public void setMethodSignature(String methodSignature) {
		this.methodSignature = methodSignature;
	}


	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	

	public Expression findExistingExpression(Expression leftToMatch) {
		Expression result = null;
		for (Expression ex : this.symbolicStates) {
			if (!ex.getUserObject().toString().equals("="))
				continue;
			Expression left = (Expression) ex.getChildAt(0);
			if (left.equals(leftToMatch)) {
				result = ((Expression) ex.getChildAt(1)).clone();
				break;
			}
		}
		return result;
	}
	
	private boolean ExpressionContains(Expression ex, String s) {
		if (ex.getUserObject().toString().equals(s))
			return true;
		else {
			for (int i = 0; i < ex.getChildCount(); i++) {
				Expression child = (Expression) ex.getChildAt(i);
				if (ExpressionContains(child, s))
					return true;
			}
			return false;
		}
	}
	
	private boolean ExpressionContains(Expression ex, Expression containee) {
		if (ex.equals(containee))
			return true;
		else {
			for (int i = 0; i < ex.getChildCount(); i++) {
				Expression child  = (Expression) ex.getChildAt(i);
				if (ExpressionContains(child, containee))
					return true;
			}
			return false;
		}
	}
}
