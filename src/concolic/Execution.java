package concolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
	private boolean printOutPS = false;
	
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
	
	public void doFullSymbolic() throws Exception{
		ToDoPath toDoPath = new ToDoPath();
		PathSummary initPS = new PathSummary();
		initPS.setSymbolicStates(initSymbolicStates(eventHandlerMethod));
		PathSummary newPS = symbolicExecution(initPS, eventHandlerMethod, toDoPath, true);
		pathSummaries.add(newPS);
		symbolicallyFinishingUp();
		System.out.println("\nTotal number of PS: " + pathSummaries.size());
	}
	
	public void doIt() {
		try {
			
			preparation();
			
			adb.click(seq.get(seq.size()-1));
			Thread.sleep(100);

			PathSummary pS_0 = new PathSummary();
			pS_0.setSymbolicStates(initSymbolicStates(eventHandlerMethod));
			
			pS_0 = concreteExecution(pS_0, eventHandlerMethod, true);
			pathSummaries.add(pS_0);
			
			symbolicallyFinishingUp();
			
			jdb.exit();
			
			System.out.println("\nTotal number of PS: " + pathSummaries.size());
			
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
	
	private PathSummary concreteExecution(PathSummary pS, StaticMethod m, boolean inMainMethod) throws Exception {

		boolean newPathCondition = false; StaticStmt lastPathStmt = new StaticStmt();
		
		String jdbNewLine = "";
		while (!jdbNewLine.equals("TIMEOUT")) {
			//Processing A Breakpoint Hit
			if (jdbNewLine.contains("Breakpoint hit: ")) {
				// 1. Recognize the newly hit StaticStmt, and check for errors
				String trimming = jdb.readLine();
				while (!trimming.equals("TIMEOUT"))
					trimming = jdb.readLine();
				String bpInfo = jdbNewLine.substring(jdbNewLine.indexOf("Breakpoint hit: "));
				System.out.println("[J]" + bpInfo);
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
						PathSummary subPS = concreteExecution(trimmedPS, targetM, false);
						pS.mergeWithInvokedPS(subPS);
					}
					else if (iS.resultsMoved()) {
						Operation symbolOFromJavaAPI = generateJavaAPIReturnOperation(iS, pS.getSymbolicStates());
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
		}
		if (inMainMethod && printOutPS) {
			printOutPathSummary(pS);
		}
		return pS;
	}
	

	private void symbolicallyFinishingUp() throws Exception{
		int counter = 1;
		while (toDoPathList.size()>0) {
			System.out.println("[Symbolic Execution No." + counter++ + "]");
			ToDoPath toDoPath = toDoPathList.get(toDoPathList.size()-1);
			toDoPathList.remove(toDoPathList.size()-1);
			printOutToDoPath(toDoPath);
			PathSummary initPS = new PathSummary();
			initPS.setSymbolicStates(initSymbolicStates(eventHandlerMethod));
			PathSummary newPS = symbolicExecution(initPS, eventHandlerMethod, toDoPath, true);
			pathSummaries.add(newPS);
		}
	}
	
	private PathSummary symbolicExecution(PathSummary pS, StaticMethod m, ToDoPath toDoPath, boolean inMainMethod) throws Exception{
		
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
				String pastChoice = toDoPath.getAPastChoice();
				String choice = "";
				ArrayList<Condition> pathConditions = new ArrayList<Condition>();
				ArrayList<String> remainingDirections = new ArrayList<String>();
				if (!pastChoice.equals("")) {
					if (!pastChoice.startsWith(stmtInfo + ","))
						throw (new Exception("current PathStmt not synced with toDoPath.pastChoice. " + stmtInfo));
					// haven't arrived target path stmt yet. So follow past choice, do not make new ToDoPath
					choice = pastChoice;
				}
				else if (toDoPath.getTargetPathStmtInfo().equals(stmtInfo)){
					// this is the target path stmt
					choice = stmtInfo + "," + toDoPath.getNewDirection();
				}
				else {
					// already passed target path stmt
					choice = makeAPathChoice(s, stmtInfo, m);
					remainingDirections = getRemainingDirections(s, choice, m);
					for (String remainingDirection : remainingDirections) {
						pushNewToDoPath(pS.getPathChoices(), stmtInfo, remainingDirection);
					}
				}
				pS.addPathChoice(choice);
				pathConditions = retrievePathConditions(s, choice, m);
				for (Condition cond : pathConditions)
					pS.updatePathCondition(cond);
				s = m.getStmtByLineNumber(readNextLineNumber(s, choice, m));
				continue;
			}
			else if (s instanceof InvokeStmt) {
				InvokeStmt iS = (InvokeStmt) s;
				StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
				StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
				if (targetC != null && targetM != null) {
					PathSummary trimmedPS = trimPSForInvoke(pS, iS.getParams());
					PathSummary subPS = symbolicExecution(trimmedPS, targetM, toDoPath, false);
					pS.mergeWithInvokedPS(subPS);
				}
				else if (iS.resultsMoved()) {
					Operation symbolOFromJavaAPI = generateJavaAPIReturnOperation(iS, pS.getSymbolicStates());
					pS.addSymbolicState(symbolOFromJavaAPI);
				}
			}
			else if (s instanceof GotoStmt) {
				GotoStmt gS = (GotoStmt) s;
				s = m.getFirstStmtOfBlock(gS.getTargetLabel());
				continue;
			}
			int nextStmtID = s.getStmtID()+1;
			s = allStmts.get(nextStmtID);
		}
		if (inMainMethod && printOutPS) {
			printOutPathSummary(pS);
		}
		return pS;
	}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//															Utility Methods
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private ArrayList<String> getRemainingDirections(StaticStmt theS, String choice, StaticMethod m) {
		ArrayList<String> remainingDirections = new ArrayList<String>();
		if (theS instanceof IfStmt) {
			IfStmt s = (IfStmt) theS;
			int chosenLine = Integer.parseInt(choice.split(",")[1]);
			if (chosenLine == s.getFlowThroughTargetLineNumber(m))
				remainingDirections.add(s.getJumpTargetLineNumber(m) + "");
			else remainingDirections.add(s.getFlowThroughTargetLineNumber(m) + "");
		}
		else if (theS instanceof SwitchStmt) {
			SwitchStmt s = (SwitchStmt) theS;
			String chosenValue = choice.split(",")[1];
			if (chosenValue.equals("FlowThrough"))
				for (int i : s.getSwitchMap(m).keySet())
					remainingDirections.add(i + "");
			else {
				for (int i : s.getSwitchMap(m).keySet())
					if (i != Integer.parseInt(chosenValue))
						remainingDirections.add(i + "");
				remainingDirections.add("FlowThrough");
			}
			Collections.reverse(remainingDirections);
		}
		return remainingDirections;
	}

	private ArrayList<Condition> retrievePathConditions(StaticStmt s,	String choice, StaticMethod m) {
		ArrayList<Condition> result = new ArrayList<Condition>();
		if (s instanceof IfStmt) {
			IfStmt ifS = (IfStmt) s;
			int chosenLine = Integer.parseInt(choice.split(",")[1]);
			Condition cond = ifS.getJumpCondition();
			if (chosenLine != ifS.getJumpTargetLineNumber(m))
				cond.reverseCondition();
			result.add(cond);
		}
		else if (s instanceof SwitchStmt) {
			SwitchStmt sws = (SwitchStmt) s;
			String chosenValue = choice.split(",")[1];
			if (chosenValue.equals("FlowThrough"))
				for (Condition cond : sws.getFlowThroughConditions())
					result.add(cond);
			else if (sws.getSwitchMap(m).containsKey(Integer.parseInt(chosenValue)))
				result.add(sws.getSwitchCondition(Integer.parseInt(chosenValue)));
			else
				for (Condition cond : sws.getFlowThroughConditions())
					result.add(cond);
		}
		return result;
	}
	
	private int readNextLineNumber(StaticStmt s, String choice, StaticMethod m) {
		int nextLineNumber = -1;
		if (s instanceof IfStmt)
			nextLineNumber = Integer.parseInt(choice.split(",")[1]);
		else if (s instanceof SwitchStmt) {
			SwitchStmt swS = (SwitchStmt) s;
			String valueChoice = choice.split(",")[1];
			if (valueChoice.equals("FlowThrough"))
				nextLineNumber = swS.getFlowThroughLineNumber(m);
			else if (swS.getSwitchMap(m).containsKey(Integer.parseInt(valueChoice)))
				nextLineNumber = swS.getSwitchMap(m).get(Integer.parseInt(valueChoice));
			else 
				nextLineNumber = swS.getFlowThroughLineNumber(m);
		}
		return nextLineNumber;
	}
	
	private String makeAPathChoice(StaticStmt s, String stmtInfo, StaticMethod m) {
		String choice = "";
		if (s instanceof IfStmt) {
			choice = stmtInfo + "," + ((IfStmt) s).getJumpTargetLineNumber(m);
		}
		else if (s instanceof SwitchStmt) {
			choice = stmtInfo + ",FlowThrough";
		}
		return choice;
	}

	private Operation generateJavaAPIReturnOperation(InvokeStmt iS, ArrayList<Operation> symbolicStates) {
		Operation resultO = new Operation();
		resultO.setLeft("$return");
		resultO.setNoOp(true);

		String rawParams = iS.getParams();
		ArrayList<String> oldParams = new ArrayList<String>();
		ArrayList<String> newParams = new ArrayList<String>();
		if (!rawParams.contains(", "))	oldParams.add(rawParams);
		else	oldParams = new ArrayList<String>(Arrays.asList(rawParams.split(", ")));
		for (String oldp : oldParams)
			for (Operation o : symbolicStates)
				if (o.getLeft().equals(oldp)) {
					newParams.add(o.getRight());
					break;
				}
		String newParam = "{" + newParams.get(0);
		for (int i = 1; i < newParams.size(); i++) {
			newParam += ", " + newParams.get(i);
		}
		newParam += "}";
		resultO.setRightA("#" + iS.getInvokeType() + ">>" + iS.getTargetSig() + ">>" + newParam);
		return resultO;
	}
	
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
			System.out.print("   " + s);
		System.out.println("\n[Turning Point]");
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
