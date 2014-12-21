package concolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import smali.stmt.GotoStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.ReturnStmt;
import smali.stmt.SwitchStmt;
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
	private ArrayList<PathSummary> pathSummaries = new ArrayList<PathSummary>();
	private ArrayList<ToDoPath> toDoPathList = new ArrayList<ToDoPath>();
	
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
			
			pS_0 = concreteExecution(pS_0, eventHandlerMethod);
			pathSummaries.add(pS_0);
			System.out.println("============== ToDoPathList size : " + toDoPathList.size());
			
			int index = 1;
			for (ToDoPath t : toDoPathList) {
				System.out.println("\nToDoPath " + index++);
				System.out.println("[TargetStmtInfo] " + t.getTargetPathStmtInfo());
				System.out.println("[NewDirection]   " + t.getNewDirection());
			}
			
			//symbolicallyFinishingUp();
			
			jdb.exit();
			
		}	catch (Exception e) {e.printStackTrace();}
	}

	private void preparation() throws Exception{
		System.out.print("\nReinstalling and Restarting App...  ");
		adb.uninstallApp(staticApp.getPackageName());
		adb.installApp(staticApp.getSignedAppPath());
		System.out.println("Done.");
		adb.startApp(pkgName, staticApp.getMainActivity().getJavaName());
		
		System.out.print("\nInitiating jdb...  ");
		jdb.init(pkgName);
		System.out.println("Done.");
		
		System.out.print("\nGoing to Target Layout...  ");
		for (int i = 0, len = seq.size()-1; i < len; i++) {
			adb.click(seq.get(i));
			Thread.sleep(300);
		}
		System.out.println("Done.");
		
		for (int i : eventHandlerMethod.getSourceLineNumbers()) {
			jdb.setBreakPointAtLine(eventHandlerMethod.getDeclaringClass(staticApp).getJavaName(), i);
		}
	}
	
	private PathSummary concreteExecution(PathSummary pS, StaticMethod m) throws Exception {
		
		System.out.println("\nStarting to Execute " + m.getSmaliSignature());
		boolean newPathCondition = false; StaticStmt lastPathStmt = new StaticStmt();
		
		String jdbNewLine = "";
		while (!jdbNewLine.equals("TIMEOUT")) {
			if (!jdbNewLine.equals(""))
				System.out.println("[J]" + jdbNewLine);
			//Processing A Breakpoint Hit
			if (jdbNewLine.contains("Breakpoint hit: ")) {
				// 1. Recognize the newly hit StaticStmt, and check for errors
				String trimming = jdb.readLine();
				while (!trimming.equals("TIMEOUT"))
					trimming = jdb.readLine();
				String bpInfo = jdbNewLine.substring(jdbNewLine.indexOf("Breakpoint hit: "));
				String methodInfo = bpInfo.split(", ")[1];
				String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
				String mN = methodInfo.substring(methodInfo.lastIndexOf(".")+1).replace("(", "").replace(")", "");
				String lineInfo = bpInfo.split(", ")[2];
				int newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
				StaticClass c = staticApp.findClassByJavaName(cN);
				if (c == null)
					throw (new Exception("Can't find StaticClass object of class " + cN + ". In " + bpInfo));
				if (!m.getName().equals(mN))
					throw (new Exception("Mismatch between current StaticMethod and new Breakpoint method. In " + bpInfo));
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)
					throw (new Exception("Can't find StaticStmt object of " + cN + ":" + newHitLine));
				// 2. Process each StaticStmt
				// 2-1. Last StaticStmt is IfStmt or SwitchStmt, need to update PathCondition
				if (newPathCondition) {
					String lastPathStmtInfo = cN + ":" + lastPathStmt.getSourceLineNumber();
					if (lastPathStmt instanceof IfStmt) {
						IfStmt ifS = (IfStmt) lastPathStmt;
						int remainingLine = -1;
						Condition cond = ifS.getJumpCondition();
						if (newHitLine == ifS.getJumpTargetLineNumber(m))
							remainingLine = ifS.getFlowThroughTargetLineNumber(m);
						else if (newHitLine == ifS.getFlowThroughTargetLineNumber(m)) {
							remainingLine = ifS.getJumpTargetLineNumber(m);
							cond.reverseCondition();
						}
						else throw (new Exception("Unexpected Line Number Following IfStmt" + bpInfo));
						pushNewToDoPath(pS.getPathChoices(), lastPathStmtInfo, "" + remainingLine);
						pS.addPathChoice(lastPathStmtInfo + "," + newHitLine);
						pS.updatePathCondition(cond);
					}
					else if (lastPathStmt instanceof SwitchStmt) {
						SwitchStmt swS = (SwitchStmt) lastPathStmt;
						ArrayList<String> remainingCases = new ArrayList<String>();
						int switchVValue = Integer.parseInt(getConcreteValue(swS.getSwitchV()));
						for (int anyCase : swS.getSwitchMap(m).keySet())
							if (anyCase != newHitLine)
								remainingCases.add(anyCase+"");
						if (newHitLine == swS.getFlowThroughLineNumber(m)) {
							for (Condition cond : swS.getFlowThroughConditions())
								pS.updatePathCondition(cond);
						}
						else {
							pS.updatePathCondition(swS.getSwitchCondition(switchVValue));
							remainingCases.add("FlowThrough");
						}
						for (String remainingCase : remainingCases)
							pushNewToDoPath(pS.getPathChoices(), lastPathStmtInfo, remainingCase);
						pS.addPathChoice(lastPathStmtInfo + "," + switchVValue);
					}
					newPathCondition = false;
					lastPathStmt = new StaticStmt();
				}
				pS.addExecutionLog(cN + ":" + newHitLine);
				// 2-2. Current StaticStmt is Return or Throw
				if (s.endsMethod()) {
					if (s instanceof ReturnStmt && !((ReturnStmt) s).returnsVoid())
						pS.updateReturnSymbol(s.getvA());
					break;
				}
				// 2-3. Current StaticStmt generates New Symbol
				else if (s.generatesSymbol()) {
					pS.updateSymbolicStates(s.getOperation(), true);
				}
				// 2-4. Current StaticStmt contains Operation
				else if (s.hasOperation()) {
					pS.updateSymbolicStates(s.getOperation(), false);
				}
				// 2-5. Current StaticStmt is IfStmt or SwitchStmt, prepare for PathCondition update at next hit
				else if (s.updatesPathCondition()) {
					lastPathStmt = s;
					newPathCondition = true;
				}
				// 2-6. Current StaticStmt is InvokeStmt
				else if (s instanceof InvokeStmt) {
					InvokeStmt iS = (InvokeStmt) s;
					StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
					StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
					if (targetC != null && targetM != null) {
						for (int i : targetM.getSourceLineNumbers())
							jdb.setBreakPointAtLine(targetC.getJavaName(), i);
						jdb.cont();
						PathSummary trimmedPS = trimPSForInvoke(pS, iS.getParams());
						PathSummary subPS = concreteExecution(trimmedPS, targetM);
						pS.mergeWithInvokedPS(subPS);
					}
					else if (iS.resultsMoved()) {
						Operation symbolOFromJavaAPI = new Operation();
						symbolOFromJavaAPI.setLeft("$newestInvokeResult");
						symbolOFromJavaAPI.setNoOp(true);
						symbolOFromJavaAPI.setRightA("$" + s.getTheStmt());
						pS.addSymbolicState(symbolOFromJavaAPI);
					}
				}
				// 3. Finished Processing StaticStmt, let jdb continue
				jdb.cont();
			}
			// Finished Processing new JDB Line, Read Next Line
			jdbNewLine = jdb.readLine();
			if (jdbNewLine == null)
				throw (new Exception("Jdb might have crashed."));
			Thread.sleep(100);
		}
		System.out.println("\n==== Finished Executing " + m.getSmaliSignature());
		printOutPathSummary(pS);
		return pS;
	}
	


	private void symbolicallyFinishingUp() throws Exception{
		int counter = 1;
		while (toDoPathList.size()>0) {
			System.out.println("---- toDoPathList before the " + counter + " Symbolic Run ----");
			for (ToDoPath t : toDoPathList)
				this.printOutToDoPath(t);
			System.out.println("\n\n");
			ToDoPath toDoPath = toDoPathList.get(toDoPathList.size()-1);
			toDoPathList.remove(toDoPathList.size()-1);
			PathSummary initPS = new PathSummary();
			initPS.setSymbolicStates(initSymbolicStates(eventHandlerMethod));
			System.out.println("[Symbolic Execution No." + counter++ + "]");
			symbolicExecution(initPS, eventHandlerMethod, toDoPath);
		}
	}
	
	private PathSummary symbolicExecution(PathSummary pS, StaticMethod m, ToDoPath toDoPath) throws Exception{
		
		ArrayList<StaticStmt> allStmts = m.getSmaliStmts();
		String className = m.getDeclaringClass(staticApp).getJavaName();
		StaticStmt s = allStmts.get(0);
		while (true) {
			pS.addExecutionLog(className + ":" + s.getSourceLineNumber());
			if (s.endsMethod()) {
				if (s instanceof ReturnStmt && !((ReturnStmt) s).returnsVoid())
					pS.updateReturnSymbol(s.getvA());
				break;
			}
			else if (s.generatesSymbol()) {
				pS.updateSymbolicStates(s.getOperation(), true);
			}
			else if (s.hasOperation()) {
				pS.updateSymbolicStates(s.getOperation(), false);
			}
			else if (s.updatesPathCondition()) {
				String stmtInfo = className + ":" + s.getSourceLineNumber();
				String pastChoice = toDoPath.getPastChoice(stmtInfo);
				int nextStmtLineNumber = -1;
				if (s instanceof IfStmt) {
					//here pastChoice is the line number
					IfStmt ifS = (IfStmt) s;
					Condition cond = ifS.getJumpCondition();
					// need to follow past choice
					if (!pastChoice.equals("")) {
						nextStmtLineNumber = Integer.parseInt(pastChoice);
						if (nextStmtLineNumber != ifS.getJumpTargetLineNumber(m))
							cond.reverseCondition();
					}
					// need to follow new direction
					else if (toDoPath.getTargetPathStmtInfo().equals(stmtInfo)) {
						nextStmtLineNumber = Integer.parseInt(toDoPath.getNewDirection());
						if (nextStmtLineNumber != ifS.getJumpTargetLineNumber(m))
							cond.reverseCondition();
					}
					// need to chooose jump target, then build ToDoPath from flowthrough target
					else {
						nextStmtLineNumber = ifS.getJumpTargetLineNumber(m);
						int remainingPath = ifS.getFlowThroughTargetLineNumber(m);
						ToDoPath toDo = new ToDoPath();
						toDo.setNewDirection("" + remainingPath);
						toDo.setPathChoices(pS.getPathChoices());
						toDo.setTargetPathStmtInfo(stmtInfo);
						toDoPathList.add(toDo);
						printOutToDoPath(toDo);
					}
					pS.addPathChoice(stmtInfo + "," + nextStmtLineNumber);
					pS.updatePathCondition(cond);
				}
				else if (s instanceof SwitchStmt) {
					SwitchStmt swS = (SwitchStmt) s;
					Map<Integer, Integer> switchMap = swS.getSwitchMap(m);
					String thisChoice = pastChoice;
					if (pastChoice.equals("")) {
						thisChoice = "FlowThrough";
						nextStmtLineNumber = swS.getFlowThroughLineNumber(m);
						for (Condition cnd : swS.getFlowThroughConditions())
							pS.updatePathCondition(cnd);
						for (int key : switchMap.keySet()) {
							ToDoPath toDo = new ToDoPath();
							toDo.setNewDirection("" + key);
							toDo.setPathChoices(pS.getPathChoices());
							toDo.setTargetPathStmtInfo(stmtInfo);
							toDoPathList.add(toDo);
							printOutToDoPath(toDo);
						}
					}
					else if (pastChoice.equals("FlowThrough")){
						nextStmtLineNumber = swS.getFlowThroughLineNumber(m);
						for (Condition cnd : swS.getFlowThroughConditions())
							pS.updatePathCondition(cnd);
					}
					else {
						int value = Integer.parseInt(pastChoice);
						if (switchMap.containsKey(value)) {
							nextStmtLineNumber = switchMap.get(value);
						}
						else {
							nextStmtLineNumber = swS.getFlowThroughLineNumber(m);
						}
						Condition cnd = new Condition();
						cnd.setLeft(swS.getSwitchV());
						cnd.setOp("=");
						cnd.setRight(value+"");
						pS.updatePathCondition(cnd);
					}
					pS.addPathChoice(stmtInfo + "," + thisChoice);
				}
				s = m.getStmtByLineNumber(nextStmtLineNumber);
				continue;
			}
			else if (s instanceof InvokeStmt) {
				InvokeStmt iS = (InvokeStmt) s;
				StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
				StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
				if (targetC != null && targetM != null) {
					PathSummary trimmedPS = trimPSForInvoke(pS, iS.getParams());
					PathSummary subPS = symbolicExecution(trimmedPS, targetM, toDoPath);
					pS.mergeWithInvokedPS(subPS);
				}
				else if (iS.resultsMoved()) {
					Operation symbolOFromJavaAPI = new Operation();
					symbolOFromJavaAPI.setLeft("$newestInvokeResult");
					symbolOFromJavaAPI.setNoOp(true);
					symbolOFromJavaAPI.setRightA("$" + s.getTheStmt());
					pS.addSymbolicState(symbolOFromJavaAPI);
				}

			}
			else if (s instanceof GotoStmt) {
				//System.out.println("[GOTO] " + s.getTheStmt());
				GotoStmt gS = (GotoStmt) s;
				s = m.getFirstStmtOfBlock(gS.getTargetLabel());
				//System.out.println("[NEXT STMT] " + s.getTheStmt());
				continue;
			}
			//System.out.println("[Stmt]" + s.getTheStmt());
			int nextStmtID = s.getStmtID()+1;
			s = allStmts.get(nextStmtID);
		}
		//printOutPathSummary(pS);
		return pS;
	}
	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//										Utility Methods
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private PathSummary trimPSForInvoke(PathSummary pS, String unparsedParams) {
		PathSummary trimmedPS = pS.clone();
		ArrayList<String> params = new ArrayList<String>();
		if (!unparsedParams.contains(", "))
			params.add(unparsedParams);
		else params = (ArrayList<String>) Arrays.asList(unparsedParams.split(", "));
		int paramIndex = 0;
		ArrayList<Operation> trimmedSymbolicStates = new ArrayList<Operation>();
		for (Operation o : pS.getSymbolicStates()) {
			// 1. left.endwith $Fstatic
			// 2. left = parameter
			if (params.contains(o.getLeft())) {
				Operation newO = o.clone();
				newO.setLeft("p" + paramIndex++);
				params.remove(o.getLeft());
				trimmedSymbolicStates.add(newO);
			}
			else if (o.getLeft().contains("$Fstatic")) {
				trimmedSymbolicStates.add(o);
			}
		}
		trimmedPS.setSymbolicStates(trimmedSymbolicStates);
		return trimmedPS;
	}

	private void pushNewToDoPath(ArrayList<String> pathChoices, String pathStmtInfo, String newDirection) {
		ToDoPath toDo = new ToDoPath();
		toDo.setPathChoices(pathChoices);
		toDo.setTargetPathStmtInfo(pathStmtInfo);
		toDo.setNewDirection(newDirection);
		this.toDoPathList.add(toDo);
	}
	
	
	private void printOutPathSummary(PathSummary pS) {
		System.out.println("\n Execution Log: ");
		for (String s : pS.getExecutionLog())
			System.out.println("  " + s);
		System.out.println("\n Symbolic States: ");
		for (Operation o : pS.getSymbolicStates())
			System.out.println("  " + o.toString());
		System.out.println("\n PathCondition: ");
		for (Condition cond : pS.getPathCondition())
			System.out.println("  " + cond.toString());
		System.out.println("\n PathChoices: ");
		for (String pC : pS.getPathChoices())
			System.out.println("  " + pC);
		System.out.println("========================");
	}
	
	private void printOutToDoPath(ToDoPath toDoPath) {
		System.out.println("[PastChoice]");
		for (String s : toDoPath.getPathChoices())
			System.out.println(" " + s);
		System.out.println("[Turning Point]");
		System.out.println(" " + toDoPath.getTargetPathStmtInfo() + "," + toDoPath.getNewDirection() + "\n");
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
	
/*	private Operation updateConcreteSymbol(Operation newSymbolO) {
		
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
	}*/
	private String getConcreteValue(String vName) {
		ArrayList<String> jdbLocals = jdb.getLocals();
		for (String jL : jdbLocals) {
			String left = jL.split(" = ")[0];
			String right = jL.split(" = ")[1];
			if (left.equals("wenhao" + vName))
				return right;
		}
		return "";
	}

}
