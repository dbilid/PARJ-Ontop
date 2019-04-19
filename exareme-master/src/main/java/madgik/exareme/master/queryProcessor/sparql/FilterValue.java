package madgik.exareme.master.queryProcessor.sparql;

public class FilterValue implements Comparable {
	
	private Integer value;
	private int table;
	private int inverse;
	
	public FilterValue(int value, int table, int inverse) {
		super();
		this.value = value;
		this.table = table;
		this.inverse = inverse;
	}

	@Override
	public int compareTo(Object o) {
		if(o == null) {
			throw new NullPointerException();
		}
		if(! (o instanceof FilterValue)) {
			throw new ClassCastException();
		}
		FilterValue other=(FilterValue)o;
		return value.compareTo(other.value);
	}

	public int getTable() {
		return table;
	}

	public int getInverse() {
		return inverse;
	}

	public int getValue() {
		return value;
	}
	
	

}
