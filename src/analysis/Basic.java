package analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import main.Paths;
import smali.Parser;
import staticFamily.StaticApp;
import tools.Apktool;

public class Basic {

	private static StaticApp staticApp;
	
	public static StaticApp initAnalysis(String apkPath, boolean forceAllSteps) {
		
		staticApp = new StaticApp();
		File apkFile = new File(apkPath);
		staticApp.setApkFile(apkFile);
		staticApp.outPath = Paths.appDataDir + staticApp.getApkFile().getName();
		
		
		if (forceAllSteps || !infoFileExists()) {
			
			Apktool.extractAPK(staticApp);
			Parser.parseSmali(staticApp);
			parseManifest();
			saveInfoFile();
		}
		else {
			loadInfoFile();
		}
		
		return staticApp;
	}

	private static void parseManifest() {
		
	}
	
	private static void saveInfoFile() {
		File infoFile = new File(staticApp.outPath + "/static.info");
		if (infoFile.exists())
			infoFile.delete();
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(infoFile));
			out.writeObject(staticApp);
			out.close();
		}	catch (Exception e) {e.printStackTrace();}
	}

	private static void loadInfoFile() {
		File infoFile = new File(staticApp.outPath + "/static.info");
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(infoFile));
			staticApp = (StaticApp) in.readObject();
			in.close();
		}	catch (Exception e) {e.printStackTrace();}
	}

	private static boolean infoFileExists() {
		File infoFile = new File(staticApp.outPath + "/static.info");
		return infoFile.exists();
	}
	
	
}
