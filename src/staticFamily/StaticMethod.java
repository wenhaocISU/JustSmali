package staticFamily;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class StaticMethod  implements Serializable{

	private String smaliSignature;
	private String declaringClass;
	
	private boolean isPublic, isPrivate, isProtected;
	private boolean isInstance, isStatic;
	
	private String smaliCode;
	private ArrayList<StaticStmt> smaliStmts = new ArrayList<StaticStmt>();
	
	private ArrayList<Integer> sourceLineNumbers = new ArrayList<Integer>();
	private int returnLineNumber;
	
	private ArrayList<String> inCallSourceSigs = new ArrayList<String>();
	private ArrayList<String> outCallTargetSigs = new ArrayList<String>();
	private ArrayList<String> fieldRefSigs = new ArrayList<String>();
	
	// Getters and Setters
	public String getSmaliSignature() {
		return smaliSignature;
	}
	
	public void setSmaliSignature(String smaliSignature) {
		this.smaliSignature = smaliSignature;
	}
	
	public String getDeclaringClass() {
		return declaringClass;
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
		return isInstance;
	}

	public void setInstance(boolean isInstance) {
		this.isInstance = isInstance;
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

	public int getReturnLineNumber() {
		return returnLineNumber;
	}

	public void addSourceLineNumber(int srcLineNumber) {
		this.sourceLineNumbers.add(srcLineNumber);
	}

	public ArrayList<String> getInCallSourceSigs() {
		return inCallSourceSigs;
	}

	public void addInCallSourceSig(String srcSig) {
		this.inCallSourceSigs.add(srcSig);
	}

	public ArrayList<String> getOutCallTargetSigs() {
		return outCallTargetSigs;
	}

	public void addOutCallTargetSigs(String tgtSig) {
		this.outCallTargetSigs.add(tgtSig);
	}

	public ArrayList<String> getFieldRefSigs() {
		return fieldRefSigs;
	}

	public void addFieldRefSigs(String fieldRefSig) {
		this.fieldRefSigs.add(fieldRefSig);
	}
	
	
	
	
}
