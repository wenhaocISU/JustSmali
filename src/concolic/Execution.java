package concolic;

import java.util.ArrayList;
import java.util.Arrays;

import smali.stmt.FieldStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.MoveStmt;
import smali.stmt.ReturnStmt;
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
			pS_0.setSymbolicStates(initSymbolicStates(eventHandlerMethod));
			pS_0 = concreteExecution(pS_0, eventHandlerMethod, false);
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private ArrayList<Operation> initSymbolicStates(StaticMethod targetM) {
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
						addReturnIntoSymbolicStates(symbolicStates, ((ReturnStmt) s));
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
	
	private void addReturnIntoSymbolicStates(ArrayList<Operation> symbolicStates, ReturnStmt returnStmt) {
		int index = -1;
		for (int i = 0; i < symbolicStates.size(); i++) {
			Operation o  = symbolicStates.get(i);
			if (o.getLeft().equals(returnStmt.getvA())) {
				index = i;
				break;
			}
		}
		Operation oldO = symbolicStates.get(index++);
		oldO.setLeft("$newestInvokeResult");
		symbolicStates.set(index-1, oldO);
		String theStuff = oldO.getRight();
		while (index < symbolicStates.size()) {
			oldO = symbolicStates.get(index++);
			// change the first definition operation
			if (oldO.getLeft().contains(theStuff)) {
				String newLeft = oldO.getLeft().replace(theStuff, "$newestInvokeResult");
				oldO.setLeft(newLeft);
				symbolicStates.set(index-1, oldO);
			}
			// change the field opeations that comes from the return variable
		}
	}

	private PathSummary mergePSAfterMethodInvoke(PathSummary pS, PathSummary subPS, boolean resultsMoved) {
		// if the root field is static, then save it
		// if the root field is instance, then save it when it was returned.
		PathSummary result = subPS.clone();
		ArrayList<Operation> newSS = pS.getSymbolicStates();
		for (Operation o : subPS.getSymbolicStates()) {
			if (o.getLeft().contains("$newestInvokeResult"))
				newSS.add(o);
			else if (o.getLeft().contains("$Fstatic"))
				newSS.add(o);
		}
		result.setSymbolicStates(newSS);
		return result;
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
			/*	ArrayList<String> jdbLocals = jdb.getLocals();
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
		String rightA = newO.getRightA();
		if (!rightA.equals("$newestInvokeResult") && !rightA.startsWith("#")) {
			for (Operation oldO : symbolicStates) {
				if (oldO.getLeft().equals(rightA)) {
					String relation = oldO.getRight();
					newO.setRightA(relation);
					break;
				}
			}
		}
		if (!rightA.equals("$newestInvokeResult") && !newO.isNoOp() && !newO.getRightB().startsWith("#")) {
			for (Operation oldO : symbolicStates) {
				if (oldO.getLeft().equals(newO.getRightB())) {
					String relation = oldO.getRight();
					newO.setRightB(relation);
					break;
				}
			}
		}
		boolean needToAdd = true;
		if (newSymbol) {
			System.out.println("[CHECK]" + newO.toString());
			rightA = newO.getRightA();
			if (rightA.equals("$newestInvokeResult")) {
				needToAdd = false;
/*				Operation newliestAddedOperation = symbolicStates.get(symbolicStates.size()-1);
				if (newliestAddedOperation.getLeft().equals("$newestInvokeResult")) {
					newO.setRightA(newliestAddedOperation.getRightA());
					System.out.println(" -deleting temp " + newliestAddedOperation.toString());
					symbolicStates.remove(newliestAddedOperation);
					
				}*/
				ArrayList<Operation> nonRelated = new ArrayList<Operation>();
				ArrayList<Operation> related = new ArrayList<Operation>();
				for (Operation oldO : symbolicStates) {
					if (!oldO.getLeft().contains("$newestInvokeResult"))
						nonRelated.add(oldO);
					else {
						Operation relatedO = new Operation();
						relatedO.setLeft(oldO.getLeft().replace("$newestInvokeResult", newO.getLeft()));
						relatedO.setNoOp(oldO.isNoOp());
						relatedO.setOp(oldO.getOp());
						relatedO.setRightA(oldO.getRightA());
						relatedO.setRightB(oldO.getRightB());
						related.add(relatedO);
					}
				}
				for (Operation relatedO : related)
					nonRelated.add(relatedO);
				System.out.println(" -sorting symbolicStates, before:");
				for (Operation o : symbolicStates)	System.out.println(" " + o.toString());
				symbolicStates.clear();
				for (Operation o : nonRelated)
					symbolicStates.add(o);
				System.out.println(" -sorting symbolicStates, after:");
				for (Operation o : symbolicStates)	System.out.println(" " + o.toString());
			}
			else if (rightA.contains("$F")){
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
		
		if (index != -1) {
			System.out.println(" -deleting " + symbolicStates.get(index));
			symbolicStates.remove(index);
		}
		if (needToAdd) {
			System.out.println(" -adding " + newO.toString());
			symbolicStates.add(newO);
		}
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
