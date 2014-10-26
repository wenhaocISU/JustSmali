package smali;

public class LocalVariable {

	private String dexName;
	private String javaSourceName;
	private String type;
	
	public String getDexName() {
		return dexName;
	}
	
	public void setDexName(String dexName) {
		this.dexName = dexName;
	}

	public String getJavaSourceName() {
		return javaSourceName;
	}

	public void setJavaSourceName(String javaSourceName) {
		this.javaSourceName = javaSourceName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public boolean equalsTo(LocalVariable v) {
		return this.getDexName().equals(v.getDexName()) &&
				this.getJavaSourceName().equals(v.getJavaSourceName()) &&
				this.getType().equals(v.getJavaSourceName());
	}
	
	
}
