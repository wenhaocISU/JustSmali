package staticFamily;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.SwitchStmt;
import concolic.PathSummary;

@SuppressWarnings("serial")
public class StaticMethod  implements Serializable{

	private String smaliSignature;
	private String declaringClass;
	private String returnType;
	
	private int localVariableCount = 0;
	
	private boolean isPublic, isPrivate, isProtected;
	private boolean isStatic, isFinal;
	private boolean isConstructor;
	
	private String smaliCode;
	private ArrayList<StaticStmt> smaliStmts = new ArrayList<StaticStmt>();
	private ArrayList<String> parameterTypes = new ArrayList<String>();
	private ArrayList<String> parameterNames = new ArrayList<String>();
	
	private ArrayList<Integer> sourceLineNumbers = new ArrayList<Integer>();
	private int returnLineNumber;
	
	private ArrayList<String> inCallSourceSigs = new ArrayList<String>();
	private ArrayList<String> outCallTargetSigs = new ArrayList<String>();
	private ArrayList<String> fieldRefSigs = new ArrayList<String>();
	
	private Map<String, String> vDebugInfo = new HashMap<String, String>();
	
	private ArrayList<PathSummary> pathSummaries = new ArrayList<PathSummary>();
	
	// Getters and Setters
	public String getSmaliSignature() {
		return smaliSignature;
	}
	
	public String getSmaliSubSignature() {
		return this.smaliSignature.split("->")[1];
	}
	
	public String getName() {
		String subSig = this.getSmaliSubSignature();
		return subSig.substring(0, subSig.indexOf("("));
	}
	
	public void setSmaliSignature(String smaliSignature) {
		this.smaliSignature = smaliSignature;
	}
	
	public StaticClass getDeclaringClass(StaticApp testApp) {
		return testApp.findClassByDexName(declaringClass);
	}
	
	public void setDeclaringClass(String declaringClass) {
		this.declaringClass = declaringClass;
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

	public boolean isInstance() {
		return !isStatic;
	}


	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public String getSmaliCode() {
		return smaliCode;
	}

	public void setSmaliCode(String smaliCode) {
		this.smaliCode = smaliCode;
	}

	public ArrayList<StaticStmt> getSmaliStmts() {
		return smaliStmts;
	}

	public void addSmaliStmt(StaticStmt s) {
		this.smaliStmts.add(s);
	}

	public ArrayList<Integer> getSourceLineNumbers() {
		return sourceLineNumbers;
	}

	public void setSourceLineNumbers(ArrayList<Integer> sourceLineNumbers) {
		this.sourceLineNumbers = sourceLineNumbers;
	}


	public void addSourceLineNumber(int srcLineNumber) {
		if (!this.sourceLineNumbers.contains(srcLineNumber))
			this.sourceLineNumbers.add(srcLineNumber);
	}

	public ArrayList<String> getInCallSourceSigs() {
		return inCallSourceSigs;
	}

	public void addInCallSourceSig(String srcSig) {
		if (!this.inCallSourceSigs.contains(srcSig))
			this.inCallSourceSigs.add(srcSig);
	}

	public ArrayList<String> getOutCallTargetSigs() {
		return outCallTargetSigs;
	}

	public void addOutCallTargetSigs(String tgtSig) {
		if (!this.outCallTargetSigs.contains(tgtSig))
			this.outCallTargetSigs.add(tgtSig);
	}

	public ArrayList<String> getFieldRefSigs() {
		return fieldRefSigs;
	}

	public void addFieldRefSigs(String fieldRefSig) {
		if (!this.fieldRefSigs.contains(fieldRefSig))
			this.fieldRefSigs.add(fieldRefSig);
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public void setConstructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public ArrayList<String> getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(ArrayList<String> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public ArrayList<String> getParameterNames() {
		return parameterNames;
	}

	public void addParameterName(String pN) {
		this.parameterNames.add(pN);
	}

	public int getLocalVariableCount() {
		return localVariableCount;
	}

	public void setLocalVariableCount(int localVariableCount) {
		this.localVariableCount = localVariableCount;

	}

	public int getReturnLineNumber() {
		return returnLineNumber;
	}

	public void setReturnLineNumber(int returnLineNumber) {
		this.returnLineNumber = returnLineNumber;
	}

	public StaticStmt getStmtByLineNumber(int line) {
		for (StaticStmt s : smaliStmts)
			if (s.getSourceLineNumber() == line)
				return s;
		return null;
	} 
	
	public Map<String, String> getvDebugInfo() {
		return vDebugInfo;
	}

	public void addvDebugInfo(String localName, String debugName) {
		this.vDebugInfo.put(localName, debugName);
	}

	public int getFirstLineNumberOfBlock(String label) {
		for (StaticStmt s : smaliStmts) {
			if (s.getBlockLabel().getNormalLabels().contains(label))
				return s.getSourceLineNumber();
		}
		return -1;
	}
	
	public StaticStmt getFirstStmtOfBlock(String label) {
		for (StaticStmt s : smaliStmts) {
			if (s.getBlockLabel().getNormalLabels().contains(label))
				return s;
		}
		return null;
	}

	public ArrayList<PathSummary> getPathSummaries() {
		return pathSummaries;
	}

	public void setPathSummaries(ArrayList<PathSummary> pathSummaries) {
		this.pathSummaries = pathSummaries;
	}

	
	
}
