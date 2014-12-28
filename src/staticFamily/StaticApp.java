package staticFamily;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import concolic.PathSummary;

@SuppressWarnings("serial")
public class StaticApp implements Serializable{

	private File apkFile;
	public String outPath;
	private String signedAppPath;
	private String packageName;
	private List<StaticClass> classes = new ArrayList<StaticClass>();	
	private ArrayList<PathSummary> pathSummaries = new ArrayList<PathSummary>();
	
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
		return c.getMethod(sig);
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

	public String getSignedAppPath() {
		return signedAppPath;
	}

	public void setSignedAppPath(String signedAppPath) {
		this.signedAppPath = signedAppPath;
	}

	public ArrayList<PathSummary> getAllPathSummaries() {
		ArrayList<PathSummary> result = new ArrayList<PathSummary>();
		for (StaticClass c : this.classes)
			for (StaticMethod m : c.getMethods())
				result.addAll(m.getPathSummaries());
		return result;
	}
	
	public ArrayList<PathSummary> getPathSummariesOfMethod(String methodSig) {
		return this.findMethod(methodSig).getPathSummaries();
	}
	
	public ArrayList<PathSummary> getPathSummariesOfMethod(StaticMethod m) {
		return m.getPathSummaries();
	}
	
}
