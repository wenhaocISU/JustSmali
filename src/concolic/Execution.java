package concolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

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
	private ArrayList<PSToBeContinued> PSTBCList = new ArrayList<PSToBeContinued>();
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
			
			pS_0 = concreteExecution(pS_0, eventHandlerMethod);
			
			System.out.println("\nNumber of PSTBC: " + PSTBCList.size());
			System.out.println("Number of path choices: " + pS_0.getPathChoices().size());
			for (String pC : pS_0.getPathChoices())
				System.out.println("  " + pC);
			//symbolicallyFinishingUp();
			jdb.exit();
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



	private void preparation() throws Exception{
		System.out.print("\nReinstalling App...  ");
		adb.uninstallApp(staticApp.getPackageName());
		adb.installApp(staticApp.getSignedAppPath());
		System.out.println("Done. Starting App.");
		adb.startApp(pkgName, staticApp.getMainActivity().getJavaName());
		
		System.out.print("\nInitiating jdb...  ");
		jdb.init(pkgName);
		System.out.println("Done.");
		
		System.out.print("\nGoing to target Layout...  ");
		for (int i = 0, len = seq.size()-1; i < len; i++) {
			adb.click(seq.get(i));
			Thread.sleep(300);
		}
		System.out.println("Done.");
		
		for (int i : eventHandlerMethod.getSourceLineNumbers()) {
			jdb.setBreakPointAtLine(eventHandlerMethod.getDeclaringClass(staticApp).getJavaName(), i);
		}
		
		System.out.println("\nStarting Concrete Execution.");
	}
	
	public PathSummary concreteExecution(PathSummary pS, StaticMethod m) throws Exception {
		
		System.out.println("\nStarting to Execute " + m.getSmaliSignature());
		boolean newPathCondition = false; StaticStmt lastPathStmt = new StaticStmt();
		
		String jdbNewLine = "";
		while (!jdbNewLine.equals("TIMEOUT")) {
			if (!jdbNewLine.equals(""))
				System.out.println("\n[J]" + jdbNewLine);
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
					throw (new Exception("Can't find StaticClass object of class " + cN));
				if (!m.getName().equals(mN))
					throw (new Exception("Mismatch between current StaticMethod and new Breakpoint method"));
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)
					throw (new Exception("Can't find StaticStmt object of " + cN + ":" + newHitLine));
				// 2. Process each StaticStmt
				// 2-1. Last StaticStmt is IfStmt or SwitchStmt, need to update PathCondition
				if (newPathCondition) {
					Condition cond = new Condition();
					ArrayList<Integer> remainingPaths = new ArrayList<Integer>();
					if (lastPathStmt instanceof IfStmt) {
						IfStmt ifS = (IfStmt) lastPathStmt;
						cond = ifS.getCondition();
						int jumpLine = ifS.getJumpTargetLineNumber(m);
						int flowThroughLine = ifS.getFlowThroughTargetLineNumber(m);
						if (newHitLine == jumpLine)
							remainingPaths.add(ifS.getFlowThroughTargetLineNumber(m));
						else if (newHitLine == flowThroughLine){
							cond.reverseCondition();
							remainingPaths.add(ifS.getJumpTargetLineNumber(m));
						}
						else throw (new Exception("IfStmt followed by unexpected Line..."));
					}
					else if (lastPathStmt instanceof SwitchStmt) {
						SwitchStmt swS = (SwitchStmt) lastPathStmt;
						Map<Integer, Condition> switchMap = swS.getSwitchMap(m);
						if (!switchMap.containsKey(newHitLine))
							throw (new Exception("SwitchStmt followd by unexpected Line..."));
						cond = switchMap.get(newHitLine);
						for (int line : switchMap.keySet()) {
							if (line != newHitLine)
								remainingPaths.add(line);
						}
					}
					String lastPathStmtInfo = pS.getExecutionLog().get(pS.getExecutionLog().size()-1);
					pS.addPathChoice(lastPathStmtInfo + "," + newHitLine);
					pS.updatePathCondition(cond);
					//TODO build new PSTBC from RemainingPaths
					//TODO update PSTBCList
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
	

}
