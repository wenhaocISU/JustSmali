package concolic;

import analysis.Expression;


public class Variable extends Expression{
	private static final long serialVersionUID = 1L;
	
	public String name;
	public String type;
	
	public Variable(Variable other){
		this(other.name,other.type);
	}
	
	public Variable(String name, String type) {
		super(name+"::"+type);
		this.name = name;
		this.type = type;
		this.setAllowsChildren(false);
	}
	
	@Override
	public Variable clone(){
		return new Variable(this);
	}
	
	public void rename(String name){
		this.name = name;
	}
	
	public String toYicesStatement(){
		return name;
	}
	
	public String toVariableDefStatement(){
		return "(define "+name+"::"+type+")";
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}
}