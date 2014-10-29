package staticFamily;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class StaticField  implements Serializable{

	private boolean isStatic;
	
	private String dexSubSig;
	private String type;
	private String initValue = "";
	
	private String declaringClassName;
	
	private boolean isPublic, isPrivate, isProtected, isFinal;
	private boolean isInherited;
	
	private ArrayList<String> inCallSourceSigs = new ArrayList<String>();

	
	// Getters and Setters
	
	public boolean isInstance() {
		return !isStatic;
	}


	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public String getName() {
		return this.getDexSubSig().split(":")[0];
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDeclaringClassName() {
		return declaringClassName;
	}

	public void setDeclaringClassName(String declaringClassName) {
		this.declaringClassName = declaringClassName;
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

	public boolean isInherited() {
		return isInherited;
	}

	public void setInherited(boolean isInherited) {
		this.isInherited = isInherited;
	}

	public ArrayList<String> getInCallSourceSigs() {
		return inCallSourceSigs;
	}

	public void addInCallSourceSig(String srcSig) {
		if (!this.inCallSourceSigs.contains(srcSig))
			this.inCallSourceSigs.add(srcSig);
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public String getInitValue() {
		return initValue;
	}

	public void setInitValue(String finalValue) {
		this.initValue = finalValue;
	}


	public String getDexSubSig() {
		return dexSubSig;
	}


	public void setDexSubSig(String dexSubSig) {
		this.dexSubSig = dexSubSig;
	}


	public String getDexSignature() {
		return declaringClassName + "->" + dexSubSig;
	}


	
}
