package tinySQL;
import java.util.ArrayList;

public class ParserTree {
	public String keyword;
	public boolean distinct;
	public String[] attributes;
	public boolean from;
	public String[] tables;
	public boolean where;
	public ExpressionTree conditions;
	public ParserTree parent;
	public ParserTree child;
	public String order_by;

	public ParserTree(String keyword) {
		this.keyword = keyword;
	}
}
