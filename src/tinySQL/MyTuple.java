package tinySQL;
import java.util.ArrayList;
import storageManager.Field;
import storageManager.FieldType;
import storageManager.Tuple;

public  class MyTuple implements Comparable<MyTuple> {
	private Field key;
	private Integer index;
	private ArrayList<Field> fields;
	public MyTuple() {}
	public MyTuple(Tuple tuple) {
		// Distinct for *
		this.fields = new ArrayList<Field>();
		for (int i = 0; i < tuple.getNumOfFields(); i++) {
			fields.add(tuple.getField(i));
		}
	}
	
	public MyTuple(Field key) {
		// Distinct for specified field
		this.key = key;
		this.fields = new ArrayList<Field>();
		fields.add(key);
	}
	
	public MyTuple(Field key, Integer index, Tuple tuple) {
		// Order by specified field
		this.key = key;
		this.index = index;
		this.fields = new ArrayList<Field>();
		for (int i = 0; i < tuple.getNumOfFields(); i++) {
			fields.add(tuple.getField(i));
		}
	}

	@Override
	public int compareTo(MyTuple key2) {
		if(key.type == FieldType.INT) {
			return ((Integer)key.integer).compareTo(key2.key.integer);
		}
		if(key.type == FieldType.STR20) {
			return  key.str.compareTo(key2.key.str);
		}
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MyTuple)) return false;
		return this.hashCode() == obj.hashCode();
	}
	
	public int hashCode() {
		String str = "";
		for(Field field:fields) {
			str += field;
		}
		return str.hashCode();
	}
	
	 public String toString()  {
	  String str = "";
	  for (int i=0; i<fields.size(); i++)
	      str+=fields.get(i)+"\t";
	  return str;
	}
	public Integer getIndex() {
		return index;
	}
}
