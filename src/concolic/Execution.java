package concolic;

import java.util.ArrayList;
import java.util.Arrays;

import smali.stmt.FieldStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.MoveStmt;
import smali.stmt.ReturnStmt;
import smali.stmt.ThrowStmt;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import tools.Adb;
import tools.Jdb;

public class Execution {

	private StaticApp staticApp;
	private String pkgName;
	private StaticMethod eventHandlerMethod;
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
		this.eventHandlerMethod = staticApp.findMethod(methodSig);
	}
	
	public void setSequence(ArrayList<String> seq) {
		this.seq = seq;
	}
	
	public void doIt() {
		try {
			preparation();
			adb.click(seq.get(seq.size()-1));
			Thread.sleep(100);

			PathSummary pS_0 = new PathSummary();
			pS_0.setSymbolicStates(initMethodSymbolicStates(eventHandlerMethod));
			pS_0 = concreteExecution(pS_0, eventHandlerMethod, false);
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private ArrayList<Operation> initMethodSymbolicStates(StaticMethod targetM) {
		ArrayList<Operation> symbolicStates = new ArrayList<Operation>();
		int paramCount = eventHandlerMethod.getParameterTypes().size();
		if (!eventHandlerMethod.isStatic())
			paramCount++;
		for (int i = 0; i < paramCount; i++) {
			Operation o = new Operation();
			o.setLeft("p" + i);
			o.setNoOp(true);
			o.setRightA("$parameter" + i);
			symbolicStates.add(o);
		}
		return symbolicStates;
	}

	private PathSummary concreteExecution(PathSummary givenPS, StaticMethod m, boolean inAnInvokedMethod) throws Exception{
		PathSummary pS = givenPS.clone();
		final ArrayList<Operation> symbolicStates = pS.getSymbolicStates();
		final ArrayList<Condition> pathCondition = pS.getPathCondition();
		
		boolean newPathCondition = false; int nextPossibleLineFromIfStmt = -1; StaticStmt lastPathStmt = new StaticStmt();
		
		String jdbNewLine = "";
		while (!jdbNewLine.equals("TIMEOUT")) {
			if (!jdbNewLine.equals(""))
				System.out.println("\n[J]" + jdbNewLine);
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
				if (!m.getName().equals(mN))	 {System.out.print(m.getName() + " " + mN + " ");System.out.println("NOPE");continue;}
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)	continue;
				System.out.println("[Smali]" + s.getTheStmt());
				// 0. If last stmt creates path, update path conditions here
				if (newPathCondition) {
					if (lastPathStmt instanceof IfStmt && newHitLine!= nextPossibleLineFromIfStmt) {
						Condition lastCond = pathCondition.get(pathCondition.size()-1);
						lastCond.reverseCondition();
						pathCondition.set(pathCondition.size()-1, lastCond);
					}
					newPathCondition = false;
					lastPathStmt = new StaticStmt();
				}
				// 1. Method Ending? (return, throw)
				if (s.endsMethod()) {
					if (s instanceof ReturnStmt && !((ReturnStmt) s).returnsVoid()) {
						System.out.println("[Action]RETURNING " + s.getvA());
						Operation o = new Operation();
						o.setLeft("$return");
						o.setNoOp(true);
						o.setRightA(s.getvA());
						updateSymbolicStates(symbolicStates, o, false);
					}
					else {
						System.out.println("[Action]RETURN VOID OR THROWING");
					}
					break;
				}
				// 1. Generates Symbol? (GetField, MoveResultFromInvoke)
				if (s.generatesSymbol()) {
					System.out.println("[Action]Generates Symbol");
/*					System.out.println("-old symbolic states");
					for (Operation ss : symbolicStates)
						System.out.println(" " + ss.toString());*/
					Operation o = generateNewSymbolOperation(m, s);
					updateSymbolicStates(symbolicStates, o, true);
/*					System.out.println("-new symbolic states");
					for (Operation ss : symbolicStates)
						System.out.println(" " + ss.toString());*/
				}
				// 2. Has Operation?
				else if (s.hasOperation()) {
					System.out.println("[Action]Has Operation");
/*					System.out.println("-old symbolic states");
					for (Operation ss : symbolicStates)
						System.out.println(" " + ss.toString());*/
					updateSymbolicStates(symbolicStates, s.getOperation(), false);
/*					System.out.println("-new symbolic states");
					for (Operation ss : symbolicStates)
						System.out.println(" " + ss.toString());*/
				}
				// 3. Updates Path Condition?
				else if (s.updatesPathCondition()) {
					lastPathStmt = s;
					if (s instanceof IfStmt) {
						System.out.println("[Action]Updates PathCondition");
						updatePathCondition(pathCondition, ((IfStmt) s).getCondition(), symbolicStates);
						newPathCondition = true;
						nextPossibleLineFromIfStmt = m.getFirstLineNumberOfBlock(((IfStmt) s).getTargetLabel());
					}
				}
				// 4. Invokes Method?
				else if (s instanceof InvokeStmt) {
					InvokeStmt iS = (InvokeStmt) s;
					StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
					StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
					if (targetM != null && targetC != null) {
						for (int i : targetM.getSourceLineNumbers())
							jdb.setBreakPointAtLine(targetC.getJavaName(), i);
						jdb.cont();
						System.out.println("[Action]Invokes Method " + targetM.getSmaliSignature());
						PathSummary subPS = concreteExecution(trimPSBeforeMethodInvoke(pS, iS.getParams()), targetM, true);
						// merge new PS into old one
						// just replace exec log and path condition
						// only add in the useful operations
						pS = mergePSAfterMethodInvoke(pS, subPS, iS.resultsMoved());
						
						if (iS.resultsMoved()) {
							Operation temp = new Operation();
							temp.setLeft("$newestInvokeResult");
							temp.setNoOp(true);
							temp.setRightA("$" + targetM.getName());
							symbolicStates.add(temp);
						}
						System.out.println("[TEMP]Finished Executing " + targetM.getName());
					}
					else if (iS.resultsMoved()){
						Operation temp = new Operation();
						temp.setLeft("$newestInvokeResult");
						temp.setNoOp(true);
						temp.setRightA("$" + s.getTheStmt());
						symbolicStates.add(temp);
					}
				}
				jdb.cont();
			}
			jdbNewLine = jdb.readLine();
			if (jdbNewLine == null)
				System.out.println("Jdb crashed.");
			Thread.sleep(100);
		}
		System.out.println("\n================Finished executing " + m.getSmaliSignature());
		pS.setPathCondition(pathCondition);
		pS.setSymbolicStates(symbolicStates);
		System.out.println("\nExecution Log: ");
		for (String s : pS.getExecutionLog())
			System.out.println("  " + s);
		System.out.println("\nSymbolic States: ");
		for (Operation o : pS.getSymbolicStates())
			System.out.println("  " + o.toString());
		System.out.println("\nPathCondition: ");
		for (Condition cond : pS.getPathCondition())
			System.out.println("  " + cond.toString());
		System.out.println("===================================================");
		return pS;
	}
	
	private PathSummary mergePSAfterMethodInvoke(PathSummary pS, PathSummary subPS, boolean resultsMoved) {
		
		return null;
	}

	private PathSummary trimPSBeforeMethodInvoke(PathSummary pS, String params) {
		PathSummary result = pS.clone();
		ArrayList<String> parameters = new ArrayList<String>();
		if (!params.contains(", "))
			parameters.add(params);
		else parameters = (ArrayList<String>) Arrays.asList(params.split(", "));
/*		System.out.print("[PARAMS " + parameters.size() + "] " );
		for (String s : parameters)	System.out.print(s + " ");
		System.out.print("\n");*/
		int paramIndex = 0;
/*		System.out.println("-Symbolic States Before Trimming");
		for (Operation ss : pS.getSymbolicStates())
			System.out.println(" " + ss.toString());*/
		ArrayList<Operation> trimmedStates = new ArrayList<Operation>();
		for (String pi : parameters) {
			for (Operation o : result.getSymbolicStates()) {
				if (o.getLeft().equals(pi)) {
					Operation newO = new Operation();
					newO.setLeft("p" + paramIndex++);
					newO.setNoOp(true);
					newO.setRightA(o.getRight());
					trimmedStates.add(newO);
					break;
				}
			}
		}
		result.setSymbolicStates(trimmedStates);
/*		System.out.println("-Symbolic States After Trimming");
		for (Operation ss : result.getSymbolicStates())
			System.out.println(" " + ss.toString());*/
		//try { Thread.sleep(100000); } catch (InterruptedException e) { e.printStackTrace(); }
		return result;
	}

	private PathSummary firstIteration() throws Exception {
		PathSummary pS = new PathSummary();
		final ArrayList<Operation> symbolicStates = new ArrayList<Operation>();
		final ArrayList<Condition> pathConditions = new ArrayList<Condition>();
		
		boolean newPathCondition = false; int nextPossibleLine = -1; StaticStmt lastPathStmt = new StaticStmt();
		
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
				// 0. If last stmt creates path, update path conditions here
				if (newPathCondition) {
					if (lastPathStmt instanceof IfStmt && newHitLine!= nextPossibleLine) {
						Condition lastCond = pathConditions.get(pathConditions.size()-1);
						lastCond.reverseCondition();
						pathConditions.set(pathConditions.size()-1, lastCond);
					}
					newPathCondition = false;
					lastPathStmt = new StaticStmt();
				}
				// 1. Generates Symbol? (GetField, MoveResultFromInvoke)
				if (s.generatesSymbol()) {
					Operation o = generateNewSymbolOperation(m, s);
					updateSymbolicStates(symbolicStates, o, true);
					System.out.println("    [Generates Symbol]");
				}
				// 2. Has Operation?
				else if (s.hasOperation()) {
					updateSymbolicStates(symbolicStates, s.getOperation(), false);
					System.out.println("    [Has Operation]");
				}
				// 3. Updates Path Condition?
				else if (s.updatesPathCondition()) {
					lastPathStmt = s;
					if (s instanceof IfStmt) {
						updatePathCondition(pathConditions, ((IfStmt) s).getCondition(), symbolicStates);
						newPathCondition = true;
						nextPossibleLine = m.getFirstLineNumberOfBlock(((IfStmt) s).getTargetLabel());
						System.out.println("    [Path Cond will be updated next stmt]");
					}
				}
				// 4. Invokes Method?
				else if (s instanceof InvokeStmt) {
					InvokeStmt iS = (InvokeStmt) s;
					StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
					StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
					if (targetM != null && targetC != null) {
						ArrayList<Integer> invokedLines = targetM.getSourceLineNumbers();
						int finalLine = -1;
						for (int tL : invokedLines) {
							jdb.setBreakPointAtLine(targetC.getJavaName(), tL);
							System.out.println("setting breakpoint " + targetC.getJavaName() + ":" + tL);
							finalLine = tL;
						}
						System.out.println("    [Paused for 5 seconds]...");
						Thread.sleep(3000);
						String test = "";
						while (test != null && !test.equals("TIMEOUT")) {
							System.out.println(test);
							test = jdb.readLine();
						}
						Thread.sleep(3000);
						System.out.println("Trying to run it now");
						jdb.cont();
						String newInvokedLine = "";
						while (!newInvokedLine.equals("TIMEOUT")) {
							if (newInvokedLine.startsWith("Breakpoint hit")) {
								System.out.println(newInvokedLine);
								String subMethodInfo = newInvokedLine.split(",")[1].replace("(", "").replace(")", "").trim();
								String subLineInfo = newInvokedLine.split(",")[2].trim();
								String subClassName = subMethodInfo.substring(0, subMethodInfo.lastIndexOf("."));
								String subMethodName = subMethodInfo.substring(subMethodInfo.lastIndexOf(".")+1);
								String subLineNumber = subLineInfo.substring(subLineInfo.indexOf("line=")+5);
								subLineNumber = subLineNumber.substring(0, subLineNumber.indexOf(" "));
								System.out.println("  " + subClassName + " " + subMethodName + " " + subLineNumber);
								System.out.println("  " + targetC.getJavaName() + " " + targetM.getName() + " " + finalLine);
								//process the stmt here
								if (subClassName.equals(targetC.getJavaName()) && subMethodName.equals(targetM.getName())
										&& subLineNumber.equals(finalLine + "")) {
									break;
								}
								jdb.cont();
							}
							newInvokedLine = jdb.readLine();
							if (newInvokedLine == null)
								System.out.println("Jdb crashed.");
							Thread.sleep(100);
						}
					}
				}
				System.out.println("jdb Locals: ");
				ArrayList<String> jdbLocals = jdb.getLocals();
				for (String lL : jdbLocals) {
					System.out.println("     " + lL);
				}
				System.out.println("Symbolic States:");
				for (Operation o : symbolicStates) {
					System.out.println(" " + o.toString());
				}
				System.out.println("Path Condition:");
				for (Condition cond : pathConditions) {
					System.out.println(" " + cond.toString());
				}
				System.out.println("Current Execution Log:");
				for (String exeLine : pS.getExecutionLog())
					System.out.println(" " + exeLine);
				System.out.println("\n");
				jdb.cont();
			}
			jdbNewLine = jdb.readLine();
			if (jdbNewLine == null)
				System.out.println("Jdb crashed.");
			Thread.sleep(100);
		}
		System.out.println("Finished.");
		pS.setSymbolicStates(symbolicStates);
		pS.setPathCondition(pathConditions);
		return pS;
	}
	
	private PathSummary oldFirstIteration() throws Exception {
		adb.click(seq.get(seq.size()-1));
		Thread.sleep(100);
		String newLine = "";
		PathSummary pS = new PathSummary();
		final ArrayList<Operation> symbolicRelations = new ArrayList<Operation>();
		final ArrayList<Condition> pathCondition = new ArrayList<Condition>();
		ArrayList<Integer> executionLog = new ArrayList<Integer>();
		
		boolean newPathCondition = false;
		int nextPossibleLine = -1;
		StaticStmt lastPathStmt = new StaticStmt();
		
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
				if (newPathCondition) {
					if (lastPathStmt instanceof IfStmt && newHitLine != nextPossibleLine) {
						Condition lastCond = pathCondition.get(pathCondition.size()-1);
						lastCond.reverseCondition();
						pathCondition.set(pathCondition.size()-1, lastCond);
					}
					newPathCondition = false;
				}
				if (s.hasOperation()) {
					System.out.println("    *operation: " + s.getOperation().toString());
					Operation newO = s.getOperation();
					updateSymbolicStates(symbolicRelations, newO, false);
				}
				if (s.generatesSymbol()) {
					System.out.print("    *generates symbol:  ");
					newSymbolO = generateNewSymbolOperation(m, s);
					newSymbolO = updateConcreteSymbol(newSymbolO);
					updateSymbolicStates(symbolicRelations, newSymbolO, true);
					System.out.println(newSymbolO.toString());
				}
				// 3/4 Path Condition
				if (s.updatesPathCondition()) {
					if (s instanceof IfStmt) {
						System.out.println("    *condition: " + ((IfStmt)s).getCondition().toString());
						updatePathCondition(pathCondition, ((IfStmt)s).getCondition(), symbolicRelations);
						newPathCondition = true;
						nextPossibleLine = m.getFirstLineNumberOfBlock(((IfStmt) s).getTargetLabel());
						lastPathStmt = s;
					}
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

	private Operation generateNewSymbolOperation(StaticMethod m, StaticStmt s) {
		Operation o = new Operation();
		if (!s.generatesSymbol())
			return o;
		o.setNoOp(true);
		o.setLeft(s.getvA());
		if (s instanceof FieldStmt) {
			if (((FieldStmt) s).isStatic())
				o.setRightA("$Fstatic>>" + ((FieldStmt) s).getFieldSig());
			else {
				String objectName = ((FieldStmt) s).getObject();
// trying not using jdb locals
/*				ArrayList<String> jdbLocals = jdb.getLocals();
				for (String jL : jdbLocals) {
					String left = jL.split(" = ")[0];
					String right = jL.split(" = ")[1];
					if (left.equals("wenhao" + objectName)) {
						objectName = right.replace("(", "<").replace(")", ">").replace("instance of ", "");
						break;
					}
				}*/
				o.setRightA("$Finstance>>" + ((FieldStmt) s).getFieldSig() + ">>" + objectName);
			}
		}
		else if (s instanceof MoveStmt) {
			o.setRightA("$newestInvokeResult");
		}
		return o;
	}

	private void updateSymbolicStates(ArrayList<Operation> symbolicStates, Operation newO, boolean newSymbol) {
		System.out.println(" -raw " + newO.toString());
		int index = -1;
		for (int i = 0, len = symbolicStates.size(); i < len; i++) {
			if (symbolicStates.get(i).getLeft().equals(newO.getLeft())) {
				index = i;
				break;
			}
		}
		if (!newSymbol) {
			String rightA = newO.getRightA();
			if (!rightA.startsWith("#")) {
				for (Operation oldO : symbolicStates) {
					if (oldO.getLeft().equals(rightA)) {
						String relation = oldO.getRight();
						newO.setRightA(relation);
						break;
					}
				}
			}
			if (!newO.isNoOp() && !newO.getRightB().startsWith("#")) {
				for (Operation oldO : symbolicStates) {
					if (oldO.getLeft().equals(newO.getRightB())) {
						String relation = oldO.getRight();
						newO.setRightB(relation);
						break;
					}
				}
			}
			//System.out.println("[OLDO]" + oldOS);
		}
		else {
			String rightA = newO.getRightA();
			if (rightA.equals("$newestInvokeResult")) {
				Operation newliestAddedOperation = symbolicStates.get(symbolicStates.size()-1);
				if (newliestAddedOperation.getLeft().equals("$newestInvokeResult")) {
					newO.setRightA(newliestAddedOperation.getRightA());
					System.out.println(" -deleting temp " + newliestAddedOperation.toString());
					symbolicStates.remove(newliestAddedOperation);
				}
			}
			else {
				String thelastPart = rightA.substring(rightA.lastIndexOf("$F"));
				if (thelastPart.startsWith("$Finstance")) {
					String prefix = thelastPart.substring(0, thelastPart.lastIndexOf(">>")+2);
					String objectName = thelastPart.substring(thelastPart.lastIndexOf(">>")+2);
					for (Operation oldO : symbolicStates) {
						if (oldO.getLeft().equals(objectName)) {
							String relation = oldO.getRight();
							newO.setRightA(prefix + relation);
							break;
						}
					}
				}
			}
		}
		System.out.println(" -adding " + newO.toString());
		if (index != -1) {
			System.out.println(" -deleting " + symbolicStates.get(index));
			symbolicStates.remove(index);
		}
		symbolicStates.add(newO);
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
		
		for (int i : eventHandlerMethod.getSourceLineNumbers()) {
			jdb.setBreakPointAtLine(eventHandlerMethod.getDeclaringClass(staticApp).getJavaName(), i);
		}
		
		//while (!jdb.readLine().equals("TIMEOUT"));
		
	}

}
