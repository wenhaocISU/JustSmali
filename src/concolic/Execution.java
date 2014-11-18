package concolic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import tools.Adb;
import tools.Jdb;
import tools.oldJdbListener;

public class Execution {

	private StaticApp staticApp;
	private String pkgName;
	private StaticMethod targetM;
	private ArrayList<String> seq = new ArrayList<String>();
	private Adb adb;
	private Jdb jdb;
	private oldJdbListener jdbListener;
	private BufferedReader in;
	
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
			//FirstIteration();
			newFirstIteration();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private void newFirstIteration() throws Exception {
		adb.click(seq.get(seq.size()-1));
		Thread.sleep(100);
		String newLine = "";
		while (!newLine.equals("TIMEOUT")) {
			newLine = jdb.readLine();
			System.out.println("[J]" + newLine);
			if (newLine.startsWith("Breakpoint hit: "))
				jdb.cont();
			Thread.sleep(100);
		}
		System.out.println("Finished");
	}
	
	private void FirstIteration() throws Exception{
		adb.click(seq.get(seq.size()-1));
		Thread.sleep(100);
		int newHitLine = -1;
		while (newHitLine != targetM.getReturnLineNumber()) {
			String newestHit = jdbListener.getNewestHit();
			jdb.cont();
			Thread.sleep(100);
			if (!newestHit.contains(", "))
				continue;
			String methodInfo = newestHit.split(", ")[1];
			String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
			String mN = methodInfo.substring(methodInfo.lastIndexOf(".")+1).replace("(", "").replace(")", "");
			String lineInfo = newestHit.split(", ")[2];
			newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
			StaticClass c = staticApp.findClassByJavaName(cN);
			if (c == null)	continue;
			StaticMethod m = c.getMethod(mN, newHitLine);
			if (m == null)	continue;
			StaticStmt s = m.getStmtByLineNumber(newHitLine);
			if (s == null)	continue;
			System.out.println("[hit] " + cN + "->" + mN + ":" + newHitLine + " '" + s.getTheStmt() + "'  ");
			System.out.println(" *operation: " + s.getOperation().toString());
		}
		jdbListener.stopListening();
		System.out.println("\n trying to stop jdbListener..");
	}
	
	private void preparation() throws Exception{
		
		adb.uninstallApp(staticApp.getPackageName());
		adb.installApp(staticApp.getSignedAppPath());
		adb.startApp(pkgName, staticApp.getMainActivity().getJavaName());
		
		jdb.init(pkgName);
		
		in = new BufferedReader(new InputStreamReader(jdb.getProcess().getInputStream()));
		
		//jdbListener = new JdbListener(jdb.getProcess().getInputStream());
		
		for (int i = 0, len = seq.size()-1; i < len; i++) {
			adb.click(seq.get(i));
			Thread.sleep(300);
		}
		
		for (int i : targetM.getSourceLineNumbers()) {
			jdb.setBreakPointAtLine(targetM.getDeclaringClass(staticApp).getJavaName(), i);
		}
		
		//Thread t = new Thread(jdbListener);
		//t.start();
	}

}
