package analysis;

import java.util.ArrayList;

import staticFamily.StaticApp;
import staticFamily.StaticMethod;
import tools.Adb;
import tools.Jdb;
import tools.JdbListener;

public class ConcolicExecution {

	private StaticApp staticApp;
	private StaticMethod targetM;
	private ArrayList<String> seq = new ArrayList<String>();
	private Adb adb;
	private Jdb jdb;
	private JdbListener jdbListener;
	
	public ConcolicExecution(StaticApp staticApp) {
		this.staticApp = staticApp;
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
		System.out.println(staticApp.getPackageName());
		adb.startApp(staticApp.getPackageName(), staticApp.getMainActivity().getJavaName());
		jdb.init(staticApp.getPackageName());
		jdbListener = new JdbListener(jdb.getProcess().getInputStream());
		for (int i : targetM.getSourceLineNumbers())
			jdb.setBreakPointAtLine(targetM.getDeclaringClass(staticApp).getJavaName(), i);
		Thread t = new Thread(jdbListener);
		t.start();
	}
	

}
