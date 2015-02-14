package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import main.Paths;
import staticFamily.StaticApp;

public class Apktool {
	
	public static void extractAPK(StaticApp testApp) {
		try {
			File app = testApp.getApkFile();
			System.out.println("\n-- apktool starting, target file: '"+ app.getAbsolutePath() + "'");
			String outDir = testApp.outPath + "/apktool";
			String command = "java -jar " + Paths.apktoolPath + " d -f " + app.getAbsolutePath() + " " + outDir;
			if (Paths.apktoolPath.contains("2.0.0rc"))
				command = "java -jar " + Paths.apktoolPath + " d -f -o " + outDir + " " + app.getAbsolutePath();
			Process pc = Runtime.getRuntime().exec(command);
			BufferedReader in = new BufferedReader(new InputStreamReader(pc.getInputStream()));
			BufferedReader in_err = new BufferedReader(new InputStreamReader(pc.getErrorStream()));
			String line;
			while ((line = in.readLine()) != null)
				System.out.println("   " + line);
			while ((line = in_err.readLine()) != null)
				System.out.println("   " + line);
			in.close();
			in_err.close();
			System.out.println("-- apktool finished extracting App '" + app.getAbsolutePath() + "'\n");
		} catch (Exception e) {	e.printStackTrace();}
	}
	
	public static void recompileAPK(StaticApp testApp) {
		try {
			System.out.print("\nRecompiling smali into APK...  ");
			String outAppName = testApp.getApkFile().getName();
			outAppName = outAppName.substring(0, outAppName.lastIndexOf(".apk"));
			outAppName = outAppName + "_smali_unsigned.apk";
			String command = "java -jar " + Paths.apktoolPath + " b -f"
					+ " -a " + Paths.aaptPath
					+ " " + testApp.outPath + "/apktool/"
					+ " " + testApp.outPath + "/" + outAppName;
			if (Paths.apktoolPath.contains("2.0.0rc"))
				command = "java -jar " + Paths.apktoolPath + " b -f"
						+ " -a " + Paths.aaptPath
						+ " -o " + testApp.outPath + "/" + outAppName
						+ " " + testApp.outPath + "/apktool/";
			Process pc = Runtime.getRuntime().exec(command);
			String line;
			boolean inputStreamGood = false;
			BufferedReader in_err = new BufferedReader(new InputStreamReader(pc.getErrorStream()));
			BufferedReader in = new BufferedReader(new InputStreamReader(pc.getInputStream()));
			ArrayList<String> missingNames = new ArrayList<String>();
			while ((line = in_err.readLine())!=null) {
				//System.out.println("[apktoolERR]" + line);
				if (line.contains("Building apk file...")) {
					inputStreamGood = true;
					System.out.print("Done.\n");
				}
				if (line.contains("Error retrieving parent for item: No resource found that matches the given name")) {
					String missingName = line.substring(line.indexOf("'")+1, line.lastIndexOf("'"));
					if (!missingNames.contains(missingName))
						missingNames.add(missingName);
				}
			}
			while ((line = in.readLine())!=null)
				//System.out.println("[apktoolIN]" + line);
			if (!inputStreamGood && missingNames.size() > 0) {
				System.out.print("Need to fix the styles.xml and redo..\n");
				String toWrite = "";
				BufferedReader inF = new BufferedReader(new FileReader(testApp.outPath + "/apktool/res/values/styles.xml"));
				while ((line = inF.readLine())!=null) {
					toWrite += line + "\n";
					if (line.equals("<resources>")) {
						for (String missingName : missingNames) {
							System.out.println("  adding    <style name=\"" + missingName + "\"/>");
							toWrite += "    <style name=\"" + missingName + "\"/>\n";
						}
					}
				}
				inF.close();
				PrintWriter outF = new PrintWriter(new FileWriter(testApp.outPath + "/apktool/res/values/styles.xml"));
				outF.write(toWrite);
				outF.close();
				recompileAPK(testApp);
			}
		}	catch (Exception e) {e.printStackTrace();}
	}
	
}
