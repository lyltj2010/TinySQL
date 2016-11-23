package tinySQL;

import java.util.ArrayList;

public class ParserTree {
	public String keyword;
	public boolean distinct;
	public String distinct_str;
	public ArrayList<String> attributes;
	public boolean from;
	public String[] tables;
	public boolean where;
	public ExpressionTree conditions;
	public ParserTree parent;
	public ParserTree child;
	public String order_by;

	public ParserTree(String keyword) {
		this.attributes = new ArrayList<String>();
		this.keyword = keyword;
	}
	
	public String toString() {
		String str = "";
		str += keyword + " ";
		if(distinct) {
			str += "distinct ";
			str += distinct_str;
		}
		if (attributes.size()!=0) str += String.join(",", attributes)+" ";
		if (from) str += "from ";
		str += String.join(",",tables) +" ";
		if (where) {
			str += "where ";
			str += conditions.getRoot() + " ";
		}
		if (order_by != null) str += "order by " + order_by;
		return str;
	}
}
