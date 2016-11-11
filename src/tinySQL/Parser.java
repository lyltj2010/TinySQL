package tinySQL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
	// CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)
	// DROP TABLE course
	// SELECT * FROM course
	// DELETE FROM course
	// INSERT INTO course (sid, homework, project, exam, grade) VALUES (1, 99, 100, 100, "A")
	
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

	public void parseSQL(String sql) {
		if(checkSyntax(sql)){
			//String stmt = sql.trim().toLowerCase().split("[\\s]+")[0];
		} else {
			System.out.println("Syntax Error!");
		}	
	}
	
	public Pair<String, LinkedHashMap<String, String>> parseCreate(String sql) {
		// CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)
		String table_name = sql.trim().toLowerCase().split("[\\s]+")[2];
        	Pattern pattern = Pattern.compile("\\((.+)\\)"); 
		Matcher matcher = pattern.matcher(sql);
		matcher.find();
		String[] pairs = matcher.group(1).trim().split("[\\s]*,[\\s]*");
		
		LinkedHashMap<String, String> schema = new LinkedHashMap<String, String>();
		for(String pair:pairs) {
			String attr = pair.split("[\\s]+")[0];
			String type = pair.split("[\\s]+")[1];
			schema.put(attr, type);
		}
		return new Pair<String, LinkedHashMap<String, String>>(table_name, schema);
	}
	
	public String parseDrop(String sql) {
		// DROP TABLE course
		String table_name = sql.trim().toLowerCase().split("[\\s]")[2];
		return table_name;
	}

	public ParserTree parseSelect(String sql) {
		// SELECT distinct sid, course.grade FROM course
		// SELECT * FROM course WHERE exam = 100 AND project = 100
		sql = sql.toLowerCase();
		ParserTree tree = new ParserTree("select");
		tree.from = true; // already checked in syntax checker

		String[] words = sql.trim().replaceAll("[,\\s]+", ",").split(",");
		int fromid = Arrays.asList(words).indexOf("from");
		// attributes
		if(sql.contains("distinct")) {
			tree.distinct = true;
			tree.attributes = Arrays.copyOfRange(words, 2, fromid);
		} else {
			tree.distinct = false;
			tree.attributes = Arrays.copyOfRange(words, 1, fromid);
		}
		// tables and conditions
		if(sql.contains("where")) {
			tree.where = true;
			int whereid = Arrays.asList(words).indexOf("where");
			tree.tables = Arrays.copyOfRange(words, fromid + 1, whereid);
			// conditions like: exam = 100 AND project = 100
			String conditions = sql.substring(sql.indexOf("where") + 5).trim();
			tree.conditions = new ExpTree(conditions);
		} else {
			tree.where = false;
			tree.tables = Arrays.copyOfRange(words, fromid + 1, words.length);
		}
		// order by
		if(sql.contains("order") && sql.contains("by")) tree.order_by = words[words.length - 1];
		return tree;
	}

	public ParserTree parseDelete(String sql) {
		// DELETE FROM course
		// DELETE FROM course WHERE grade = "E"
		sql = sql.toLowerCase();
		ParserTree tree = new ParserTree("delete");
		tree.from = true; // already checked in syntax checker
		String[] words = sql.trim().split("[\\s]+");
		int fromid = Arrays.asList(words).indexOf("from");
		// tables and conditions
		if(sql.contains("where")) {
			tree.where = true;
			String conditions = sql.substring(sql.indexOf("where") + 5).trim();
			tree.conditions = new ExpTree(conditions);
		} else {
			tree.where = false;
			tree.tables = Arrays.copyOfRange(words, fromid + 1, words.length);
		}
		return tree;
	}

	public Pair<String, HashMap<String, String>> parseInsert(String sql) {
		// INSERT INTO course (sid, homework, project, exam, grade) VALUES (1, 99, 100, 100, "A")
		String table_name = sql.trim().toLowerCase().split("[\\s]+")[2];

		Pattern pattern = Pattern.compile("\\((.+)\\).*\\((.+)\\)"); 
		Matcher matcher = pattern.matcher(sql);
		matcher.find();
		String[] attrs = matcher.group(1).trim().split("[\\s]*,[\\s]*");
		String[] values = matcher.group(2).trim().split("[\\s]*,[\\s]*");

		HashMap<String, String> record = new HashMap<String, String>();
		for(int i=0; i<attrs.length; i++) {
			String value = values[i].replaceAll("^[\\'\"]","").replaceAll("[\\'\"]$", "");
			record.put(attrs[i], value);
		}
		return new Pair<String, HashMap<String, String>>(table_name, record);
	}

	public static void main(String[] args) {
		Parser parser = new Parser();
		// String sql = "CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)";
		// Select 
		String sql1 = "SELECT distinct name FROM course WHERE exam = 100 AND project = 100 order by name";
		ParserTree tree1 = parser.parseSelect(sql1);
		System.out.println(tree1.order_by);
		// Delete
		String sql2 = "DELETE FROM course WHERE grade = \"E\"";
		ParserTree tree2 = parser.parseSelect(sql2);
		System.out.println(tree2.tables[0]);
	}
}
