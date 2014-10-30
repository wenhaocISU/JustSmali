package analysis;

import java.util.ArrayList;

import staticFamily.StaticApp;
import staticFamily.StaticMethod;
import tools.Adb;
import tools.Jdb;
import tools.JdbListener;

public class ConcolicExecution {

	private StaticApp staticApp;
	private String pkgName;
	private StaticMethod targetM;
	private ArrayList<String> seq = new ArrayList<String>();
	private Adb adb;
	private Jdb jdb;
	private JdbListener jdbListener;
	
	public ConcolicExecution(StaticApp staticApp) {
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
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private void FirstIteration() {
		adb.click(seq.get(seq.size()-1));
	}
	
	private void preparation() throws Exception{
		
		adb.stopApp(pkgName);
		adb.startApp(pkgName, staticApp.getMainActivity().getJavaName());
		jdb.init(pkgName);
		jdbListener = new JdbListener(jdb.getProcess().getInputStream());
		
		for (int i = 0, len = seq.size()-1; i < len; i++) {
			adb.click(seq.get(i));
			Thread.sleep(300);
		}
		
		for (int i : targetM.getSourceLineNumbers())
			jdb.setBreakPointAtLine(targetM.getDeclaringClass(staticApp).getJavaName(), i);
		
		Thread t = new Thread(jdbListener);
		t.start();
	}

}
