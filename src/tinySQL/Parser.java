package tinySQL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import storageManager.FieldType;
import storageManager.Relation;
import storageManager.SchemaManager;

public class Parser {

	private ParserHelper helper = new ParserHelper();
	
	public boolean checkSyntax(String sql) {
		String stmt = sql.trim().toLowerCase().split(" ")[0];
		boolean isValid;
		switch(stmt) {
		case "create": isValid = helper.checkCreate(sql);
			break;
		case "drop": isValid = helper.checkDrop(sql);
			break;
		case "select": isValid = helper.checkSelect(sql);
			break;
		case "delete": isValid = helper.checkDelete(sql);
			break;
		case "insert": isValid = helper.checkInsert(sql);
			break;
		default: isValid = false;
			break;
		}
		return isValid;		
	}
	
	public Pair<String, LinkedHashMap<String, FieldType>> parseCreate(String sql) {
		// CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)
		sql = sql.trim().toLowerCase();
		String table_name = sql.split("[\\s]+")[2];
		// Extract content in brackets
		Pattern pattern = Pattern.compile("\\((.+)\\)"); 
		Matcher matcher = pattern.matcher(sql);
		matcher.find();
		String[] pairs = matcher.group(1).trim().split("[\\s]*,[\\s]*");
		LinkedHashMap<String, FieldType> schema = new LinkedHashMap<String, FieldType>();
		for(String pair:pairs) {
			String attr = pair.split("[\\s]+")[0];
		 	String raw_type = pair.split("[\\s]+")[1];
		 	FieldType type;
		 	if(raw_type.equals("int")) { type = FieldType.INT; }
		 	else { type = FieldType.STR20; }
		 	schema.put(attr, type);
		}
		return new Pair<String, LinkedHashMap<String, FieldType>>(table_name, schema);
	}
	
	public String parseDrop(String sql) {
		// DROP TABLE course
		String table_name = sql.trim().toLowerCase().split("[\\s]")[2];
		return table_name;
	}
	
	public ParserTree parseSelect(SchemaManager schema_manager, String sql) {
		ParserTree tree;
		sql = sql.toLowerCase();
		String[] tables;
		String[] words = sql.trim().replaceAll("[,\\s]+", ",").split(",");
		int fromid = Arrays.asList(words).indexOf("from");
		if(sql.contains("where")) {
			int whereid = Arrays.asList(words).indexOf("where");
			tables = Arrays.copyOfRange(words, fromid + 1, whereid);
		} else {
			tables = Arrays.copyOfRange(words, fromid + 1, words.length);
		}
		
		if(tables.length == 1) { tree = parseSelect1(sql);} // single table
		else { tree = parseSelect2(schema_manager, sql); } // multiple tables
		return tree;
	}

	private ParserTree parseSelect1(String sql) {
		// SELECT * FROM course WHERE exam = 100 AND project = 100
		sql = sql.toLowerCase();
		ParserTree tree = new ParserTree("select");
		tree.from = true; // already checked in syntax checker

		String[] words = sql.trim().replaceAll("[,\\s]+", ",").split(",");
		int fromid = Arrays.asList(words).indexOf("from");
		// attributes
		if(sql.contains("distinct")) {
			tree.distinct = true;
			int distinctid = Arrays.asList(words).indexOf("distinct");
			tree.distinct_str = words[distinctid + 1];
			tree.attributes = new ArrayList<String>(Arrays.asList(Arrays.copyOfRange(words, 2, fromid)));
		} else {
			tree.distinct = false;
			tree.attributes = new ArrayList<String>(Arrays.asList(Arrays.copyOfRange(words, 1, fromid)));
		}
		// tables and conditions
		if(sql.contains("where")) {
			tree.where = true;
			int whereid = Arrays.asList(words).indexOf("where");
			tree.tables = Arrays.copyOfRange(words, fromid + 1, whereid);
			// conditions like: exam = 100 AND project = 100 order by exam
			if(sql.contains("order") && sql.contains("by")) {
				tree.order_by = words[words.length - 1];
				String conditions = sql.substring(sql.indexOf("where"), sql.indexOf("order")).trim();
				tree.conditions = new ExpressionTree(conditions);
			} else {
				String conditions = sql.substring(sql.indexOf("where") + 5).trim();
				tree.conditions = new ExpressionTree(conditions);
			}
		} else {
			tree.where = false;
			if(sql.contains("order") && sql.contains("by")) {
				int orderid = Arrays.asList(words).indexOf("order");
				tree.tables = Arrays.copyOfRange(words, fromid + 1, orderid);
			} else {
				tree.tables = Arrays.copyOfRange(words, fromid + 1, words.length);
			}
		}
		return tree;
	}
	
	private ParserTree parseSelect2(SchemaManager schema_manager, String sql) {
		// Multiple tables case, prefix attributes with table name
		// SELECT * FROM r, s, t WHERE r.a=t.a AND r.b=s.b AND s.c=t.c
		ParserTree tree = parseSelect1(sql);
		
		// prefix select list with table name, like grade2 -> course2.grade2
		ArrayList<String> attrs = tree.attributes;
		String[] tables = tree.tables;
		if(!(attrs.size() == 1 && attrs.get(0).equals("*"))) {
			int index = 0;
			for(String attr:attrs) {
				if(attr.contains(".")) {
					index += 1;
					continue;
				} else {
					String tmp = inWhichTable(schema_manager, tables, attr);
					attrs.set(index, tmp + "." + attr);
					index += 1;
				}
			}
			tree.attributes = attrs; // update tree
		}
		// prefix attrs in conditions with table name
		if(tree.where) {
			Node node = tree.conditions.getRoot();
			node = updateExpressionTree(node, schema_manager, tables);
			tree.conditions.setRoot(node); // update conditions
		}
		return tree;
	}
	
	private Node updateExpressionTree(Node node, SchemaManager schema_manager, String[] tables) {
		if(node.op.contains(".")) return node;
		String in_what_table = inWhichTable(schema_manager, tables, node.op);
		if(in_what_table.equals("NotInTable")) {
			if(node.left == null || node.right == null) return node;
			node.left = updateExpressionTree(node.left, schema_manager, tables);
			node.right = updateExpressionTree(node.right, schema_manager, tables);
		} else {
			node.op = in_what_table + "." + node.op;
		}
		return node;
	}
	
	private String inWhichTable(SchemaManager schema_manager, String[] tables, String str) {
		Relation relation_reference;
		for(String table:tables) {
			relation_reference = schema_manager.getRelation(table);
			if(relation_reference.getSchema().fieldNameExists(str)) return table;
		}
		return "NotInTable"; // Exception handling?
	}
	

	public ParserTree parseDelete(String sql) {
		// DELETE FROM course
		// DELETE FROM course WHERE grade = "E"
		sql = sql.toLowerCase();
		ParserTree tree = new ParserTree("delete");
		tree.from = true; // already checked in syntax checker
		String[] words = sql.trim().split("[\\s]+");
		int fromid = Arrays.asList(words).indexOf("from");
		int whereid = Arrays.asList(words).indexOf("where");
		// tables and conditions
		if(sql.contains("where")) {
			tree.where = true;
			String conditions = sql.substring(sql.indexOf("where") + 5).trim();
			tree.conditions = new ExpressionTree(conditions);
			tree.tables = Arrays.copyOfRange(words, fromid + 1, whereid);
		} else {
			tree.where = false;
			tree.tables = Arrays.copyOfRange(words, fromid + 1, words.length);
		}
		return tree;
	}

	public Pair<String, LinkedHashMap<String, String>> parseInsert(String sql) {
		// INSERT INTO course (sid, homework, project, exam, grade) VALUES (1, 99, 100, 100, "A")
		String table_name = sql.trim().toLowerCase().split("[\\s]+")[2];

		Pattern pattern = Pattern.compile("\\((.+)\\).*\\((.+)\\)"); 
		Matcher matcher = pattern.matcher(sql);
		matcher.find();
		String[] attrs = matcher.group(1).trim().split("[\\s]*,[\\s]*");
		String[] values = matcher.group(2).trim().split("[\\s]*,[\\s]*");

		LinkedHashMap<String, String> record = new LinkedHashMap<String, String>();
		for(int i=0; i<attrs.length; i++) {
			String value = values[i].replaceAll("^[\\'\"]","").replaceAll("[\\'\"]$", "");
			record.put(attrs[i], value);
		}
		return new Pair<String, LinkedHashMap<String, String>>(table_name, record);
	}
	
	public static void error(String message) {
		System.out.println(message); 
		System.exit(0);
	}
	
	public static void main(String[] args) {
		Parser parser = new Parser();
		// String sql = "CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)";
		// Select 
//		 String sql1 = "SELECT course1.sid, homework FROM course1, course2 where course1.sid = course2.id";
//		 ParserTree tree1 = parser.parseSelect(sql1);
//		 System.out.println(tree1);
		// Delete
		String sql2 = "DELETE FROM course WHERE grade = \"E\"";
		ParserTree tree2 = parser.parseDelete(sql2);
		System.out.println(tree2);
	}
}
