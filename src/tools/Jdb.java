package tools;

import java.io.OutputStream;

import main.Paths;

public class Jdb {

	private int localPort =7772;
	private String srcPath = "src";
	private Process pc;
	private OutputStream out;
	
	public void init(String packageName) {
		String pID = new Adb().getPID(packageName);
		try {
			Runtime.getRuntime().exec(Paths.adbPath + " forward tcp:" + localPort + " jdwp:" + pID).waitFor();
			pc = Runtime.getRuntime().exec("jdb -sourcepath src -attach localhost:" + localPort);
			out = pc.getOutputStream();
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public void setBreakPointAtLine(String className, int line) {
		try {
			out.write(("stop at " + className + ":" + line + "\n").getBytes());
			out.flush();
		}	catch (Exception e) { e.printStackTrace(); }
	}
	
	public void cont() {
		try {
			out.write("cont\n".getBytes());
			out.flush();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public void setMonitorCont(boolean flag) {
		try {
			if (flag) 	out.write("monitor cont\n".getBytes());
			else 		out.write("unmonitor 1\n".getBytes());
			out.flush();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public void exit() {
		try {
			out.write("exit\n".getBytes());
			out.flush();
		}	catch (Exception e) { e.printStackTrace(); }
	}
	
	public Process getProcess() {
		return pc;
	}

	public int getLocalPort() {
		return localPort;
	}
	
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	} 
	
	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}
	
}
