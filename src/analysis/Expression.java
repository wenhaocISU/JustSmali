package analysis;
 
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
 
public class Expression extends DefaultMutableTreeNode{ // 
	public static int availableId = 0;
	public final int creationId;
	
	private static final long serialVersionUID = 1L;
	public Expression(String operator){
		this.setUserObject(operator);
		creationId = availableId++;
	}
	
	public String getContent(){
		return this.getUserObject().toString();
	}
	
	public Expression clone(){
		Expression root = new Expression(this.getUserObject().toString());
		for(int i=0; i< this.getChildCount();i++){
			Expression subNode = (Expression) this.getChildAt(i);
			root.add(subNode.clone());
		}
		return root;
	}
	
	public String toYicesStatement(){
		String result = "("+this.getUserObject().toString();
		for(int index = 0; index < this.getChildCount(); index++){
			Expression expr = (Expression) this.getChildAt(index);
			result += " "+expr.toYicesStatement();
		}
		return result + " )";
	}
	
	//nextLeaf may not end !
	
//	public boolean contains(Variable var){
//		DefaultMutableTreeNode current = this.getFirstLeaf();
//		do{
//			if(current.equals(var)) return true;
//			current = current.getNextLeaf();
//		}while(current != null);
//		return false;
//	}
//	
//	public boolean contains(String partten){
//		Expression current = (Expression) this.getFirstLeaf();
//		do{
//			if(current.toYicesStatement().matches(partten)) return true;
//			current = (Expression) current.getNextLeaf();
//		}while(current != null);
//		return false;
//	}
	
	public Set<Variable> getUniqueVarSet(){
		return getUniqueVarSet(null);
	}
	
	public Set<Variable> getUniqueVarSet(String partten){
		Set<Variable> varSet = new HashSet<Variable>();
		if(this instanceof Variable ){ 
			if(partten == null){
				varSet.add((Variable) this); 
			}else if(this.toYicesStatement().matches(partten)){ 
				varSet.add((Variable) this); 
			}
		}else{
			for(int i=0;i<this.getChildCount();i++){
				Expression expre = (Expression)this.getChildAt(i);
				Set<Variable> quiry = expre.getUniqueVarSet(partten);
				varSet.addAll(quiry);
			} 
		}
		
		return varSet;
		
		
		
		
		
		
//		Set<Variable> varSet = new HashSet<Variable>();
//		
//		System.out.println("count: "+this.getChildCount());
//		Expression e1 = (Expression) this.getFirstLeaf();
//		System.out.println(e1.creationId);
//		Expression e2 = (Expression) e1.getNextLeaf();
//		System.out.println(e2.creationId);
//		Expression e3 = (Expression) e2.getNextLeaf();
//		if(e3 != null) System.out.println(e3.creationId);
//		System.out.println(e2==e3);
//		
//		if(this.isLeaf() ){
//			if(this instanceof Variable){
//				if(partten == null){
//					varSet.add((Variable)this);
//				}else if(this.toYicesStatement().matches(partten)){
//					varSet.add((Variable)this);
//				}
//				return varSet;
//			}else{
//				return varSet;
//			}
//		}
//		
//		DefaultMutableTreeNode leaf = this.getFirstLeaf();
//		do{
//			if(leaf instanceof Variable){
//				Variable var = (Variable)leaf;
//				if(partten == null){
//					varSet.add(var);
//				}else if(var.toYicesStatement().matches(partten)){
//					varSet.add(var);
//				}
//			}
//			leaf = leaf.getNextLeaf();
//			
//		}while(leaf != null);
//		return varSet;
	}
	
	public boolean replace(String toReplace, Expression replacement){
		if(replacement == null) return false;
		boolean anyChange = false;
		int count = this.getChildCount();
		for(int i=0; i < count; i++){
			Expression expre = (Expression) this.getChildAt(i);
			if(expre.toYicesStatement().equals(toReplace)){
				this.remove(i);
				this.insert(replacement, i);
				anyChange = true;
			}else{
				anyChange = expre.replace(toReplace, replacement) || anyChange;
			}
		}
		return anyChange;
	}
	
	public boolean replace(Expression toReplace, Expression replacement){
		if(replacement == null) return false;
		boolean anyChange = false;
		int count = this.getChildCount();
		for(int i=0; i < count; i++){
			Expression expre = (Expression) this.getChildAt(i);
			if(toReplace.equals(expre)){
				this.remove(i);
				this.insert(replacement, i);
				anyChange = true;
			}else{
				anyChange = expre.replace(toReplace, replacement) || anyChange;
			}
		}
		return anyChange;
	}
	
	@Override
	public int hashCode(){
		return this.getContent().hashCode();
	}
	
	@Override
	public boolean equals(Object object){
		if( object instanceof Expression){
			Expression input = (Expression)object;
			String state1 = this.toYicesStatement();
			String state2 = input.toYicesStatement();
			boolean result = state1.equals(state2);
			return result;
		}
		return false;
	}
	
	
	public static Set<Variable> getUnqiueVarSet(Collection<? extends Expression> inputs){
		Set<Variable> result = new HashSet<Variable>();
		for(Expression expre : inputs){
			result.addAll(expre.getUniqueVarSet());
		}
		return result;
	}
	
	public static Set<Variable> getUnqiueVarSet(Collection<? extends Expression> inputs, String partten){
		Set<Variable> result = new HashSet<Variable>();
		for(Expression expre : inputs){
			result.addAll(expre.getUniqueVarSet(partten));
		}
		return result;
	}
	
	public static String createAssertion(String yices){
		return "(assert "+yices+")";
	}
}