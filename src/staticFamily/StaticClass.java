package staticFamily;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class StaticClass  implements Serializable{

	private String javaName;
	private String sourceFileName;
	
	private List<StaticField> fields = new ArrayList<StaticField>();
	private List<StaticMethod> methods = new ArrayList<StaticMethod>();
	
	private List<String> interfaces = new ArrayList<String>();
	private String superClass;
	private String outerClass;
	private List<String> innerClasses = new ArrayList<String>();
	
	private boolean isPublic, isPrivate, isProtected, isFinal;
	private boolean isAbstract, isInterface;
	private boolean isInDEX;
	private boolean isInnerClass;
	private boolean isActivity, isMainActivity;
	
	public int largestOriginalSourceLineNumber = -1;
	private ArrayList<String> oldLines = new ArrayList<String>();
	
	// Getters and Setters
	
	public String getJavaName() {
		return javaName;
	}
	public void setJavaName(String javaName) {
		this.javaName = javaName;
	}
	public String getDexName() {
		return "L" + this.javaName.replace(".", "/") + ";";
	}
	public List<StaticField> getFields() {
		return fields;
	}
	public void addField(StaticField field) {
		this.fields.add(field);
	}
	public List<StaticMethod> getMethods() {
		return methods;
	}
	public void addMethod(StaticMethod m) {
		this.methods.add(m);
	}
	public List<String> getInterfaces() {
		return interfaces;
	}
	public void addInterface(String interfaceName) {
		if (!this.interfaces.contains(interfaceName))
			this.interfaces.add(interfaceName);
	} 
	public String getOuterClass() {
		return outerClass;
	}
	public void setOuterClass(String outerClass) {
		this.outerClass = outerClass;
	}
	public String getSuperClass() {
		return superClass;
	}
	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}
	public List<String> getInnerClasses() {
		return innerClasses;
	}
	public void addInnerClass(String innerClass) {
		if (!this.innerClasses.contains(innerClass))
			this.innerClasses.add(innerClass);
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public boolean isPrivate() {
		return isPrivate;
	}
	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}
	public boolean isProtected() {
		return isProtected;
	}
	public void setProtected(boolean isProtected) {
		this.isProtected = isProtected;
	}
	public boolean isFinal() {
		return isFinal;
	}
	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}
	public boolean isAbstract() {
		return isAbstract;
	}
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}
	public boolean isInterface() {
		return isInterface;
	}
	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}
	public boolean isConcrete() {
		return (!isAbstract && !isInterface);
	}

	public boolean isInDEX() {
		return isInDEX;
	}
	public void setInDEX(boolean isInDEX) {
		this.isInDEX = isInDEX;
	}
	public boolean hasInnerClass() {
		if (innerClasses.size() > 0)
			return true;
		return false;
	}
	public boolean isInnerClass() {
		return isInnerClass;
	}
	public void setInnerClass(boolean isInnerClass) {
		this.isInnerClass = isInnerClass;
	}
	public boolean isActivity() {
		return isActivity;
	}
	public void setActivity(boolean isActivity) {
		this.isActivity = isActivity;
	}
	public boolean isMainActivity() {
		return isMainActivity;
	}
	public void setMainActivity(boolean isMainActivity) {
		this.isMainActivity = isMainActivity;
	}
	public String getSourceFileName() {
		return sourceFileName;
	}
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
	
	
	// utilities
	
	public StaticField getField(String fieldName) {
		for (StaticField f : fields)
			if (f.getName().equals(fieldName))
				return f;
		return null;
	}
	
	public StaticMethod getMethodByFullSig(String mSig) {
		for (StaticMethod m : methods)
			if (m.getSmaliSignature().equals(mSig))
				return m;
		return null;
	}
	
	public StaticMethod getMethodBySubSig(String mSubSig) {
		for (StaticMethod m : methods)
			if (m.getSmaliSubSignature().equals(mSubSig))
				return m;
		return null;
	}
	
	public StaticMethod getMethod(String mName, int lineNo) {
		for (StaticMethod m : methods)
			if (m.getName().equals(mName) && m.getSourceLineNumbers().contains(lineNo))
				return m;
		return null;
	}
	
	public ArrayList<String> getOldLines() {
		return oldLines;
	}
	
	public void setOldLines(ArrayList<String> oldLines) {
		this.oldLines = oldLines;
	}
	
	public StaticStmt getStmtByLineNumber(int line) {
		for (StaticMethod m : this.methods)
			for (StaticStmt s : m.getSmaliStmts())
				if (s.getSourceLineNumber() == line)
					return s;
		return null;
	}
	
	public int getMethodCount() {
		return this.methods.size();
	}

	public int getFieldCount() {
		return this.fields.size();
	}
	
}
