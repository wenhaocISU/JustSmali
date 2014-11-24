package concolic;

import java.util.ArrayList;

import smali.stmt.FieldStmt;
import smali.stmt.IfStmt;
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
			firstIteration();
			//newFirstIteration();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private void firstIteration() throws Exception {
		adb.click(seq.get(seq.size()-1));
		Thread.sleep(100);
		String newLine = "";
		PathSummary pS = new PathSummary();
		final ArrayList<Operation> symbolicRelations = new ArrayList<Operation>();
		boolean newSymbol = false;
		Operation newSymbolO = new Operation();
		while (!newLine.equals("TIMEOUT")) {
			if (!newLine.equals(""))
				System.out.println("[J]" + newLine);
			if (newLine.startsWith("Breakpoint hit: ")) {
				String subLine = jdb.readLine();
				while (!subLine.equals("TIMEOUT"))
					subLine = jdb.readLine();
				ArrayList<String> jdbLocals = jdb.getLocals();
				String methodInfo = newLine.split(", ")[1];
				String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
				String mN = methodInfo.substring(methodInfo.lastIndexOf(".")+1).replace("(", "").replace(")", "");
				String lineInfo = newLine.split(", ")[2];
				int newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
				pS.addExecutionLog(newHitLine);
				StaticClass c = staticApp.findClassByJavaName(cN);
				if (c == null)	continue;
				StaticMethod m = c.getMethod(mN, newHitLine);
				if (m == null)	continue;
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)	continue;
				System.out.println("    *bytecode: " + s.getTheStmt());
				if (newSymbol) {
					symbolicRelations.add(newSymbolO);
					newSymbol = false;
					newSymbolO = new Operation();
				}
				if (s.hasOperation()) {
					System.out.println("    *operation: " + s.getOperation().toString());
					Operation newO = s.getOperation();
					updateSymbolicRelations(symbolicRelations, newO);
				}
				if (s.generatesSymbol()) {
					System.out.println("    *generates symbol");
					// 2 things: First, add to symbol table; Second, v = $
					newSymbol = true;
					newSymbolO = generateNewSymbolOperation(s);
				}
				if (s instanceof IfStmt) {
					System.out.println("    *condition: " + ((IfStmt)s).getCondition().toString());
					
				}
				
				System.out.println("    *locals: ");
				for (String l : jdbLocals)
					System.out.println("     " + l);
				jdb.cont();
			}
			newLine = jdb.readLine();
			Thread.sleep(100);
		}
		System.out.println("Finished");
		for (Operation o : symbolicRelations) {
			System.out.println(" " + o.toString());
		}
	}
	
	
	private Operation generateNewSymbolOperation(StaticStmt s) {
		Operation o = new Operation();
		if (!s.generatesSymbol())
			return o;
		o.setNoOp(true);
		if (s instanceof FieldStmt) {
			o.setLeft(s.getvA());
			o.setRightA(((FieldStmt) s).getObject());
		}
		else if (s instanceof MoveStmt) {
			//o.setLeft(s.getvA());
		}
		return o;
	}

	private void updateSymbolicRelations(ArrayList<Operation> symbolicRelations, Operation newO) {
		int index = -1;
		for (int i = 0, len = symbolicRelations.size(); i < len; i++) {
			if (symbolicRelations.get(i).getLeft().equals(newO.getLeft())) {
				index = i;
				break;
			}
		}
		String rightA = newO.getRightA();
		if (!rightA.startsWith("#")) {
			for (Operation oldO : symbolicRelations) {
				if (oldO.getLeft().equals(rightA)) {
					String relation = "(" + oldO.toString().split(" = ")[1] + ")";
					newO.setRightA(relation);
					break;
				}
			}
		}
		if (!newO.isNoOp() && !newO.getRightB().startsWith("#")) {
			for (Operation oldO : symbolicRelations) {
				if (oldO.getLeft().equals(newO.getRightB())) {
					String relation = "(" + oldO.toString().split(" = ")[1] + ")";
					newO.setRightB(relation);
					break;
				}
			}
		}
		System.out.println("[NEWO]" + newO.toString());
		if (index == -1)
			symbolicRelations.add(newO);
		else	symbolicRelations.set(index, newO);
	}

	private void preparation() throws Exception{
		
		adb.uninstallApp(staticApp.getPackageName());
		adb.installApp(staticApp.getSignedAppPath());
		adb.startApp(pkgName, staticApp.getMainActivity().getJavaName());
		
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
