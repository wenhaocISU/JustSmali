package main;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import analysis.Basic;

public class Main {

	static StaticApp testApp;
	
	public static void main(String[] args) {
		
		String[] apps = {
				"C:/Users/Wenhao/Documents/juno_workspace/AndroidTest/bin/AndroidTest.apk",
				"testApps/Fast.apk",
				"testApps/backupHelper.apk",
				"testApps/Butane.apk",
				"testApps/CalcA.apk",
				"testApps/KitteyKittey.apk",
		};
		
		String apkPath = apps[0];
		
		testApp = Basic.initAnalysis(apkPath, true);
		
		test();
	}
	
	static void test() {
		for (StaticClass c : testApp.getClasses()) {
			if (c.isInterface())
				System.out.println("-I- " + c.getDexName());
		}
	}
	
}
