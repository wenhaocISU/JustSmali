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
				Thread.sleep(500);
				String keystoreName = Paths.keystorePath.substring(Paths.keystorePath.lastIndexOf("/")+1);
				Process pc = Runtime.getRuntime().exec(
						"jarsigner -keystore " + Paths.keystorePath + 
						" -signedjar " + newF.getAbsolutePath() + 
						" " + f.getAbsolutePath() +
						" " + keystoreName);
				BufferedReader in_err = new BufferedReader(new InputStreamReader(pc.getErrorStream()));
				BufferedReader in = new BufferedReader(new InputStreamReader(pc.getInputStream()));
				String line;
//				while ((line = in.readLine())!=null)
//					System.out.println("[jarsignerIN]" + line);
//				while ((line = in_err.readLine())!=null)
//					System.out.println("[jarsignerERR]" + line);
				OutputStream out = pc.getOutputStream();
				out.write((Paths.keystoreKey + "\n").getBytes());
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
