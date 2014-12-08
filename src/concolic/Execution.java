package concolic;

import java.util.ArrayList;

import smali.stmt.FieldStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.MoveStmt;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import tools.Adb;
import tools.Jdb;

public class Execution {

	private StaticApp staticApp;
	private String pkgName;
	private StaticMethod targetM;
	private ArrayList<String> seq = new ArrayList<String>();
	private Adb adb;
	private Jdb jdb;
	
	public Execution(StaticApp staticApp) {
		this.staticApp = staticApp;
		this.pkgName = staticApp.getPackageName();
		this.adb = new Adb();
		this.jdb = new Jdb();
	}
	
	public void setTargetMethod(String methodSig) {
		this.targetM = staticApp.findMethod(methodSig);
	}
	
	public void setSequence(ArrayList<String> seq) {
		this.seq = seq;
	}
	
	public void doIt() {
		try {
			preparation();
			PathSummary firstPS = firstIteration();
			
			//newFirstIteration();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private PathSummary newFirstIteration() throws Exception {
		
		PathSummary pS = new PathSummary();
		final ArrayList<Operation> symbolicStates = new ArrayList<Operation>();
		final ArrayList<Condition> pathConditions = new ArrayList<Condition>();
		ArrayList<Integer> executionLog = new ArrayList<Integer>();
		
		String jdbNewLine = "";
		while (!jdbNewLine.equals("TIMEOUT")) {
			if (!jdbNewLine.equals(""))
				System.out.println("[JDBOut]" + jdbNewLine);
			if (jdbNewLine.startsWith("Breakpoint hit: ")) {
				String trimming = jdb.readLine();
				while (!trimming.equals("TIMEOUT"))
					trimming = jdb.readLine();
				String methodInfo = jdbNewLine.split(", ")[1];
				String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
				String mN = methodInfo.substring(methodInfo.lastIndexOf(".")+1).replace("(", "").replace(")", "");
				String lineInfo = jdbNewLine.split(", ")[2];
				int newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
				pS.addExecutionLog(cN + ":" + newHitLine);
				StaticClass c = staticApp.findClassByJavaName(cN);
				if (c == null)	continue;
				StaticMethod m = c.getMethod(mN, newHitLine);
				if (m == null)	continue;
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)	continue;
				// 1. Generates Symbol?
				if (s.generatesSymbol()) {
					
				}
				// 2. Has Operation?
				else if (s.hasOperation()) {
					
				}
				// 3. Updates Path Condition?
				else if (s instanceof IfStmt) {
					
				}
				// 4. Invokes Method?
				else if (s instanceof InvokeStmt) {
					
				}
			}
		}
		
		pS.setSymbolicStates(symbolicStates);
		pS.setPathCondition(pathConditions);
		return pS;
	}
	
	private PathSummary firstIteration() throws Exception {
		adb.click(seq.get(seq.size()-1));
		Thread.sleep(100);
		String newLine = "";
		PathSummary pS = new PathSummary();
		final ArrayList<Operation> symbolicRelations = new ArrayList<Operation>();
		final ArrayList<Condition> pathCondition = new ArrayList<Condition>();
		ArrayList<Integer> executionLog = new ArrayList<Integer>();
		boolean newSymbol = false;
		boolean newPathCondition = false;
		int nextPossibleLine = -1;
		Operation newSymbolO = new Operation();
		while (!newLine.equals("TIMEOUT")) {
			if (!newLine.equals(""))
				System.out.println("\n[J]" + newLine);
			if (newLine.startsWith("Breakpoint hit: ")) {
				String subLine = jdb.readLine();
				while (!subLine.equals("TIMEOUT"))
					subLine = jdb.readLine();
				//ArrayList<String> jdbLocals = jdb.getLocals();
				String methodInfo = newLine.split(", ")[1];
				String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
				String mN = methodInfo.substring(methodInfo.lastIndexOf(".")+1).replace("(", "").replace(")", "");
				String lineInfo = newLine.split(", ")[2];
				int newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
				pS.addExecutionLog(cN + ":" + newHitLine);
				StaticClass c = staticApp.findClassByJavaName(cN);
				if (c == null)	continue;
				StaticMethod m = c.getMethod(mN, newHitLine);
				if (m == null)	continue;
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)	continue;
				executionLog.add(newHitLine);
				System.out.println("    *bytecode: " + s.getTheStmt());
				if (newSymbol) {
					newSymbolO = updateConcreteSymbol(newSymbolO);
					updateSymbolicRelations(symbolicRelations, newSymbolO, true);
					newSymbol = false;
					newSymbolO = new Operation();
				}
				if (newPathCondition) {
					if (newHitLine != nextPossibleLine) {
						Condition lastCond = pathCondition.get(pathCondition.size()-1);
						lastCond.reverseCondition();
						pathCondition.set(pathCondition.size()-1, lastCond);
					}
					newPathCondition = false;
				}
				// 1/4 Operation
				if (s.hasOperation()) {
					System.out.println("    *operation: " + s.getOperation().toString());
					Operation newO = s.getOperation();
					updateSymbolicRelations(symbolicRelations, newO, false);
				}
				// 2/4 Generates Symbol
				if (s.generatesSymbol()) {
					System.out.print("    *generates symbol:  ");
					// 2 things: First, add to symbol table; Second, v = $
					newSymbol = true;
					newSymbolO = generateNewSymbolOperation(s);
					System.out.println(newSymbolO.toString());
				}
				// 3/4 Path Condition
				if (s instanceof IfStmt) {
					System.out.println("    *condition: " + ((IfStmt)s).getCondition().toString());
					updatePathCondition(pathCondition, ((IfStmt)s).getCondition(), symbolicRelations);
					newPathCondition = true;
					nextPossibleLine = m.getFirstLineNumberOfBlock(((IfStmt) s).getTargetLabel());
				}
				// 4/4 Method Invocation
				if (s instanceof InvokeStmt) {
					InvokeStmt iS = (InvokeStmt) s;
					System.out.println("    *invokes: " + iS.getTargetSig());
					System.out.println("    *with param: " + iS.getParams());
					StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
					StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
					if (targetM != null && targetC != null) {
						ArrayList<Integer> targetLines = targetM.getSourceLineNumbers();
						for (int tL : targetLines) {
							jdb.setBreakPointAtLine(targetC.getJavaName(), tL);
							//System.out.println("---- Setting BP " + targetC.getJavaName() + ":" + tL);
							//System.out.println("---- " + jdb.readLine());
						}
						
					}
				}
				System.out.println("    *locals: ");
				ArrayList<String> jdbLocals = jdb.getLocals();
				for (String lL : jdbLocals) {
					System.out.println("     " + lL);
				}
				System.out.println("Symbolic States:");
				for (Operation o : symbolicRelations) {
					System.out.println(" " + o.toString());
				}
				System.out.println("Path Condition:");
				for (Condition cond : pathCondition) {
					System.out.println(" " + cond.toString());
				}
				jdb.cont();
			}
			newLine = jdb.readLine();
			if (newLine == null)
				System.out.println("Jdb crashed.");
			Thread.sleep(100);
		}
		System.out.println("Finished");
		pS.setSymbolicStates(symbolicRelations);
		pS.setPathCondition(pathCondition);
		System.out.println("Execution Log:");
		for (int i : executionLog)
			System.out.print(i + "  ");
		System.out.print("\n");
		System.out.println("Symbolic States:");
		for (Operation o : symbolicRelations) {
			System.out.println(" " + o.toString());
		}
		System.out.println("Path Condition:");
		for (Condition cond : pathCondition) {
			System.out.println(" " + cond.toString());
		}
		return pS;
	}
	
	
	private Operation updateConcreteSymbol(Operation newSymbolO) {
		
		Operation result = newSymbolO;
		String[] parts = result.getRightA().split(">>");
		if (parts.length == 3) {
			ArrayList<String> jdbLocals = jdb.getLocals();
			for (String jL : jdbLocals) {
				String left = jL.split(" = ")[0];
				String right = jL.split(" = ")[1];
				if (left.equals("wenhao" + parts[2])) {
					parts[2] = right.replace("(", "<").replace(")", ">").replace("instance of ", "");
					break;
				}
			}
			result.setRightA(parts[0] + ">>" + parts[1] + ">>" + parts[2]);
		}
		return result;
	}

	private void updatePathCondition(ArrayList<Condition> pathCondition,
			Condition condition, ArrayList<Operation> symbolicRelations) {
		boolean leftDone = false, rightDone = false;
		if (condition.getRight().equals("0"))
			rightDone = true;
		for (Operation o : symbolicRelations) {
			if (o.getLeft().equals(condition.getLeft()) && !leftDone) {
				String newExpr = o.getRight();
				condition.setLeft(newExpr);
				leftDone = true;
			}
			else if (o.getLeft().equals(condition.getRight()) && !rightDone) {
				String newExpr = o.getRight();
				condition.setRight(newExpr);
				rightDone = true;
			}
			if (leftDone && rightDone)
				break;
		}
		pathCondition.add(condition);
	}

	private Operation generateNewSymbolOperation(StaticStmt s) {
		Operation o = new Operation();
		if (!s.generatesSymbol())
			return o;
		o.setNoOp(true);
		if (s instanceof FieldStmt) {
			o.setLeft(s.getvA());
			o.setRightA("$instanceF>>" + ((FieldStmt) s).getFieldSig() + ">>" + ((FieldStmt) s).getObject());
			if (((FieldStmt) s).isStatic()) {
				o.setRightA("$staticF>>" + ((FieldStmt) s).getFieldSig());
			}
		}
		else if (s instanceof MoveStmt) {
			//o.setLeft(s.getvA());
		}
		return o;
	}

	private void updateSymbolicRelations(ArrayList<Operation> symbolicRelations, Operation newO, boolean newSymbol) {
		int index = -1;
		for (int i = 0, len = symbolicRelations.size(); i < len; i++) {
			if (symbolicRelations.get(i).getLeft().equals(newO.getLeft())) {
				index = i;
				break;
			}
		}
		if (!newSymbol) {
			String rightA = newO.getRightA();
			String oldOS = "";
			if (!rightA.startsWith("#")) {
				for (Operation oldO : symbolicRelations) {
					if (oldO.getLeft().equals(rightA)) {
						String relation = oldO.getRight();
						oldOS = oldO.toString();
						newO.setRightA(relation);
						break;
					}
				}
			}
			if (!newO.isNoOp() && !newO.getRightB().startsWith("#")) {
				for (Operation oldO : symbolicRelations) {
					if (oldO.getLeft().equals(newO.getRightB())) {
						String relation = oldO.getRight();
						newO.setRightB(relation);
						break;
					}
				}
			}
			System.out.println("[OLDO]" + oldOS);
		}
		System.out.println("[NEWO]" + newO.toString());
		if (index != -1) {
			symbolicRelations.remove(index);
		}
		symbolicRelations.add(newO);
	}

	private void preparation() throws Exception{
		
		//adb.rebootDevice();
		//System.out.print("Waiting for device to reboot...  ");
		//Thread.sleep(30000);
		//System.out.println("OK.");
		adb.uninstallApp(staticApp.getPackageName());
		adb.installApp(staticApp.getSignedAppPath());
		//adb.unlockScreen();
		adb.startApp(pkgName, staticApp.getMainActivity().getJavaName());
		
		System.out.println("\nInitiating jdb..");
		jdb.init(pkgName);
		
		for (int i = 0, len = seq.size()-1; i < len; i++) {
			adb.click(seq.get(i));
			Thread.sleep(300);
		}
		
		for (int i : targetM.getSourceLineNumbers()) {
			jdb.setBreakPointAtLine(targetM.getDeclaringClass(staticApp).getJavaName(), i);
		}
		
	}

}
