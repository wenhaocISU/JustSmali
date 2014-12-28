package analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import main.Paths;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import smali.Parser;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import tools.Apktool;
import tools.Others;
import tools.Soot;

public class StaticInfo {

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
		
		File instrumentedAPK_smali = new File(staticApp.outPath + "/" + apkFile.getName().substring(0, apkFile.getName().lastIndexOf(".apk")) + "_smali.apk");
		if (forceAllSteps || !instrumentedAPK_smali.exists()) {
			Apktool.recompileAPK(staticApp);
		}
		staticApp.setSignedAppPath(instrumentedAPK_smali.getAbsolutePath());
		
		File instrumentedAPK_soot = new File(staticApp.outPath + "/" + apkFile.getName().substring(0, apkFile.getName().lastIndexOf(".apk")) + "_soot.apk");
		if (forceAllSteps || !instrumentedAPK_soot.exists()) {
			Soot.InstrumentEveryMethod(staticApp);
		}
		Others.signAPK(staticApp);
		
		System.out.println("\nAnalysis Initialization Complete.\n");
		return staticApp;
	}

	private static void parseManifest() {
		try {
			File manifestFile = new File(staticApp.outPath + "/apktool/AndroidManifest.xml");
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile);
			doc.getDocumentElement().normalize();
			Node manifestNode = doc.getFirstChild();
			String pkgName = manifestNode.getAttributes().getNamedItem("package").getNodeValue();
			staticApp.setPackageName(pkgName);
			NodeList aList = doc.getElementsByTagName("activity");
			boolean mainActFound = false;
			for (int i = 0, len = aList.getLength(); i < len; i++) {
				Node a = aList.item(i);
				String aName = a.getAttributes().getNamedItem("android:name").getNodeValue();
				if (aName.startsWith("."))
					aName = aName.substring(1, aName.length());
				if (!aName.contains("."))
					aName = pkgName + "." + aName;
				StaticClass c = staticApp.findClassByJavaName(aName);
				c.setActivity(true);
				Element e = (Element) a;
				NodeList actions = e.getElementsByTagName("action");
				for (int j = 0, len2 = actions.getLength(); j < len2; j++) {
					Node action = actions.item(j);
					if (action.getAttributes().getNamedItem("android:name").getNodeValue().equals("android.intent.action.MAIN")) {
						c.setMainActivity(true);
						mainActFound = true;
						break;
					}
				}
			}
			if (!mainActFound) {
				NodeList aaList = doc.getElementsByTagName("activity-alias");
				for (int i = 0, len = aaList.getLength(); i < len; i++) {
					if (mainActFound)
						break;
					Node aa = aaList.item(i);
					String aName = aa.getAttributes().getNamedItem("android:targetActivity").getNodeValue();
					if (aName.startsWith("."))
						aName = aName.substring(1, aName.length());
					if (!aName.contains("."))
						aName = pkgName + "." + aName;
					Element e = (Element) aa;
					NodeList actions = e.getElementsByTagName("action");
					for (int j = 0, len2 = actions.getLength(); j < len2; j++) {
						Node action = actions.item(j);
						if (action.getAttributes().getNamedItem("android:name").getNodeValue().equals("android.intent.action.MAIN")) {
							StaticClass c = staticApp.findClassByJavaName(aName);
							c.setMainActivity(true);
							mainActFound = true;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void saveInfoFile() {
		System.out.print("\nSaving StaticApp into file...  ");
		File infoFile = new File(staticApp.outPath + "/static.info");
		if (infoFile.exists())
			infoFile.delete();
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(infoFile));
			out.writeObject(staticApp);
			out.close();
			System.out.print("Done.\n");
		}	catch (Exception e) {e.printStackTrace();}
	}

	private static void loadInfoFile() {
		System.out.print("\nLoading StaticApp...  ");
		File infoFile = new File(staticApp.outPath + "/static.info");
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(infoFile));
				staticApp = (StaticApp) in.readObject();
				in.close();
				System.out.print("Done.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				if (e.getMessage().contains("local class incompatible")) {
					System.out.print("Class version incompatible... rebuilding StaticApp...\n");
					Apktool.extractAPK(staticApp);
					Parser.parseSmali(staticApp);
					parseManifest();
					saveInfoFile();
				}
				else e.printStackTrace();
			}
	}

	private static boolean infoFileExists() {
		File infoFile = new File(staticApp.outPath + "/static.info");
		return infoFile.exists();
	}
	
	
}
