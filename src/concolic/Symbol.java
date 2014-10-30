package concolic;

public class Symbol {

	private String symbolName;
	private String source;
	
	private boolean isField;
	private boolean isParameter;
	private boolean isMethodReturn;
	
	public String getSymbolName() {
		return symbolName;
	}
	
	public void setSymbolName(String symbolName) {
		this.symbolName = symbolName;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public boolean isField() {
		return isField;
	}

	public void setField(boolean isField) {
		this.isField = isField;
	}

	public boolean isParameter() {
		return isParameter;
	}

	public void setParameter(boolean isParameter) {
		this.isParameter = isParameter;
	}

	public boolean isMethodReturn() {
		return isMethodReturn;
	}

	public void setMethodReturn(boolean isMethodReturn) {
		this.isMethodReturn = isMethodReturn;
	}
	
	
}
