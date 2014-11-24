package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import tools.Adb;
import tools.Apktool;
import tools.Others;
import analysis.StaticInfo;
import concolic.Execution;

public class Main {

	static StaticApp testApp;
	static Adb adb = new Adb();
	
	public static void main(String[] args) {
		
		String[] apps = {
/* 0 */			"/home/wenhaoc/adt_workspace/AndroidTest/bin/AndroidTest.apk",
				"/home/wenhaoc/AppStorage/Fast.apk",
				"/home/wenhaoc/AppStorage/APAC_engagement/backupHelper/backupHelper.apk",
				"/home/wenhaoc/AppStorage/APAC_engagement/Butane/Butane.apk",
				"/home/wenhaoc/AppStorage/APAC_engagement/CalcA/CalcA.apk",
/* 5 */			"/home/wenhaoc/AppStorage/APAC_engagement/KitteyKittey/KitteyKittey.apk",
				"/home/wenhaoc/AppStorage/net.mandaria.tippytipper.apk",
				"/home/wenhaoc/adt_workspace/TheApp/bin/TheApp.apk",
		
		};
		
		String apkPath = apps[7];
		
		testApp = StaticInfo.initAnalysis(apkPath, true);
		
		Execution cE = new Execution(testApp);
		
		cE.setTargetMethod("Lthe/app/Irwin$3;->onClick(Landroid/view/View;)V");
		//cE.setTargetMethod("Lthe/app/MainActivity;->onCreate(Landroid/os/Bundle;)V");

		//cE.setSequence(new ArrayList<String>(Arrays.asList("185 1138", "378 841", "345 546")));	// N7 2013
		//cE.setSequence(new ArrayList<String>(Arrays.asList("277 1211", "570 1233", "540 789")));	// N5
		cE.setSequence(new ArrayList<String>(Arrays.asList("127 751", "253 561", "238 359")));		// N7 old
		cE.doIt();
		
		//testStaticApp();
		
		//printAMethod();
		
		//patternSearching();
		
	}
	
	static void patternSearching() {
		File[] fs = new File(Paths.appDataDir).listFiles();
		for (File appFolder : fs) {
			File smaliFolders = new File(appFolder.getAbsoluteFile() + "/apktool/smali/");
			if (smaliFolders.exists())
			for (File f : smaliFolders.listFiles()) {
				searchFile(f);
			}
		}
	}
	
	static ArrayList<String> list = new ArrayList<String>();
	
	static void searchFile(File f) {
		if (f.isDirectory()) {
			for (File subF : f.listFiles())
				searchFile(subF);
		}
		if (f.isFile() && f.getName().endsWith(".smali")) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(f));
				String line;
				while ((line = in.readLine())!= null) {
					if (line.equals("# direct methods") || line.equals("# virtual methods"))
						break;
				}
				while ((line = in.readLine())!=null) {
					if (line.contains(".annotation")) {
						System.out.println(f.getAbsolutePath());
					}
				}
				in.close();
			}	catch (Exception e) {e.printStackTrace();}
		}
	}
	
	static void testStaticApp() {
		System.out.println("-class count- " + testApp.getClasses().size());
		for (StaticClass c : testApp.getClasses()) {
			if (!c.isActivity())	continue;
			if (c.isMainActivity())
				System.out.println("-Main Activity-");
			System.out.println("-C- " + c.getDexName());
			
		}
	}
	
	static void printAMethod() {
		Random rng = new Random();
		int i = rng.nextInt(10) + 1;
		if (i > testApp.getClasses().size())
			i = testApp.getClasses().size()-1;
		StaticClass c = testApp.getClasses().get(i);
		int j = rng.nextInt(3) + 1;
		if (j > c.getMethods().size())
			j = c.getMethods().size()-1;
		StaticMethod m = c.getMethods().get(j);
		System.out.println("-M- " + m.getSmaliSignature() + "\n");
		System.out.println(" *local count: " + m.getLocalVariableCount());
		System.out.println("\n" + m.getSmaliCode());
	}
	
}
