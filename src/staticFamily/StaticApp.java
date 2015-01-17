package staticFamily;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class StaticApp implements Serializable{

	private File apkFile;
	public String outPath;
	private String packageName;
	private List<StaticClass> classes = new ArrayList<StaticClass>();
	
	// Getters and Setters

	public String getPackageName() {
		return packageName;
	}
	
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	public List<StaticClass> getClasses() {
		return classes;
	}
	
	public void addClass(StaticClass c) {
		this.classes.add(c);
	}

	public File getApkFile() {
		return apkFile;
	}

	public void setApkFile(File apkFile) {
		this.apkFile = apkFile;
	}
	
	// Utilities
	
	public StaticClass findClassByJavaName(String cN) {
		for (StaticClass c : classes)
			if (c.getJavaName().equals(cN))
				return c;
		return null;
	}
	
	public StaticClass findClassByDexName(String cN) {
		for (StaticClass c : classes)
			if (c.getDexName().equals(cN))
				return c;
		return null;
	}
	
	public StaticMethod findMethod(String sig) {
		if (!sig.contains("->"))
			return null;
		String cN = sig.split("->")[0];
		StaticClass c = findClassByDexName(cN);
		if (c == null)
			return null;
		return c.getMethodByFullSig(sig);
	}
	
	public StaticField findField(String sig) {
		if (!sig.contains("->"))
			return null;
		String cN = sig.split("->")[0];
		String fN = sig.split("->")[1].split(":")[0];
		StaticClass c = findClassByDexName(cN);
		if (c == null)
			return null;
		return c.getField(fN);
	}
	
	public StaticClass getMainActivity() {
		for (StaticClass c : classes)
			if (c.isMainActivity())
				return c;
		return null;
	}
	
	public ArrayList<StaticClass> getActivities() {
		ArrayList<StaticClass> result = new ArrayList<StaticClass>();
		for (StaticClass c : classes)
			if (c.isActivity())
				result.add(c);
		return result;
	}


	public String getSmaliAppPath() {
		return outPath + "/" + apkFile.getName().substring(0, apkFile.getName().lastIndexOf(".apk")) + "_smali.apk";
	}


	public String getSootAppPath() {
		//return outPath + "/" + apkFile.getName().substring(0, apkFile.getName().lastIndexOf(".apk")) + "_soot.apk";
		return getSmaliAppPath();
	}

	
}
