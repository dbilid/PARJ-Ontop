package inf.unibz.it.obda.model.impl;


import java.net.URI;

public class GTEPredicate extends ComparisonOperatorPredicate {

	private URI name = URI.create("http://www.obda.org/ucq/predicate/operator/comparison#GTE");
	private int identifier = -2;
	
	public int getArity() {
		// TODO Auto-generated method stub
		return 0;
	}

	public URI getName() {
		// TODO Auto-generated method stub
		return name;
	}
	
	public boolean equals(Object obj){
		if(obj == null|| !(obj instanceof PredicateImp)){
			return false;
		}else{
			
			return this.hashCode() == obj.hashCode();
		}	
	}
	
	public int hashCode(){
		return identifier;
	}

	public GTEPredicate copy() {
		return new GTEPredicate();
	}

}