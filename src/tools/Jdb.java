package tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import main.Paths;

public class Jdb {

	private int localPort =7772;
	private String srcPath = "src";
	private Process pc;
	private OutputStream out;
	
	public void init(String packageName) {
		String osName = System.getProperty("os.name");
		String pID = new Adb().getPID(packageName);
		try {
			Runtime.getRuntime().exec(Paths.adbPath + " forward tcp:" + localPort + " jdwp:" + pID).waitFor();
			if (osName.startsWith("Windows"))
				pc = Runtime.getRuntime().exec("jdb -sourcepath " + srcPath + "-connect com.sun.jdi.SocketAttach:hostname=localhost,port=" + localPort);
			else pc = Runtime.getRuntime().exec("jdb -sourcepath " + srcPath + " -attach localhost:" + localPort);
			out = pc.getOutputStream();
			//forTest();
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
	
	private void forTest() {
		try {
		String line, line2;
		BufferedReader in = new BufferedReader(new InputStreamReader(pc.getInputStream()));
		BufferedReader in_err = new BufferedReader(new InputStreamReader(pc.getErrorStream()));
		while (true) {
			if ((line = in.readLine())!=null) 
				System.out.println(line);
			if ((line2 = in_err.readLine())!=null)
				System.out.println(line2);
		}
		}	catch (Exception e) {e.printStackTrace();}
	}
}
