package tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import main.Paths;

public class Adb {

	
	public void click(int x, int y) {
		try {
			Runtime.getRuntime().exec(Paths.adbPath + " shell input tap " + x + " " + y).waitFor();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public void click(String xy) {
		try {
			Runtime.getRuntime().exec(Paths.adbPath + " shell input tap " + xy).waitFor();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public String getPID(String packageName) {
		try {
			Process pc = Runtime.getRuntime().exec(Paths.adbPath + " shell ps |grep " + packageName);
			BufferedReader in = new BufferedReader(new InputStreamReader(pc.getInputStream()));
			String line;
			while ((line = in.readLine())!=null) {
				if (!line.endsWith(packageName)) continue;
				String[] parts = line.split(" ");
				for (int i = 1; i < parts.length; i++) {
					if (parts[i].equals(""))	continue;
					return parts[i].trim();
				}
			}
		}	catch (Exception e) {e.printStackTrace();}
		return "";
	}
		
	public void startApp(String packageName, String mainActName) {
		try {
			Runtime.getRuntime().exec(Paths.adbPath + " shell am start -n " + packageName + "/" + mainActName).waitFor();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public void stopApp(String packageName) {
		try {
			Runtime.getRuntime().exec(Paths.adbPath + " shell kill " + getPID(packageName)).waitFor();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
}
