package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;

import main.Paths;
import staticFamily.StaticApp;

public class Others {

	public static void signAPK(StaticApp testApp) {
		for (File f : (new File(testApp.outPath)).listFiles()) {
			if (!f.getName().endsWith("_unsigned.apk"))	continue;
			System.out.println("\nSigning '" + f.getAbsolutePath() + "'...  ");
			String newFName = f.getName().substring(0, f.getName().lastIndexOf("_unsigned.apk")) + ".apk";
			File newF = new File(testApp.outPath + "/" + newFName);
			if (newF.exists())	newF.delete();
			try {
				Thread.sleep(2000);
				String keystoreName = Paths.keystorePath.substring(Paths.keystorePath.lastIndexOf("/")+1);
				Process pc = Runtime.getRuntime().exec(
						"jarsigner -keystore " + Paths.keystorePath + 
						" -signedjar " + newF.getAbsolutePath() + 
						" " + f.getAbsolutePath() +
						" " + keystoreName);
				OutputStream out = pc.getOutputStream();
				out.write("isu_obad\n".getBytes());
				out.flush();
				pc.waitFor();
			}	catch (Exception e) { e.printStackTrace(); }
			newF = new File(testApp.outPath + "/" + newFName);
			if (newF.exists()) {
				System.out.println("\nDone. Signed file: '" + newF.getAbsolutePath() + "'");
			}
			f.delete();
		}
	}
	
}
