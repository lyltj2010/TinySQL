package tinySQL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserHelper {
	
	public boolean isInteger(String str) {
		try{
			int num = Integer.parseInt(str);
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	public boolean isValidName(String str) {
		// letter[digit | letter]*
		return str.matches("^[a-zA-Z]+[\\w\\d]*");
	}
	public boolean isValidColName(String str) {
		// [table-name.]attribute-name
		if(str.contains(".")){
			if(str.split("\\.").length > 2) {
				return false;
			} else {
				String table = str.split("\\.")[0];
				String attr = str.split("\\.")[1];
				return this.isValidName(table) && this.isValidColName(attr);
			}	
		} else {
			return this.isValidName(str);
		}
	}
	
	// Syntax Checkers
	public boolean checkCreate(String sql) {
		// CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)
		String[] words = sql.trim().toLowerCase().split("[\\s]");
		if(words.length <= 3) return false;
		if(!words[1].equals("table")) return false;
		if(!isValidName(words[2])) return false;
		if(!words[3].startsWith("(")) return false;
		if(!words[words.length-1].endsWith(")")) return false;
		Pattern p = Pattern.compile("\\((.+)\\)"); 
		Matcher matcher = p.matcher(sql);
		if(!matcher.find()) return false;
		// String schema = matcher.group(1); extract string in ()
		return true;
	}
	public boolean checkDrop(String sql){
		// DROP TABLE course
		String[] words = sql.toLowerCase().trim().split("[\\s]");
		if(words.length == 3 && words[1].equals("table") && this.isValidName(words[2])) {
			return true;
		} else {
			return false;
		}		
	}
	
	public boolean checkSelect(String sql) {
		// SELECT * FROM course
		// SELECT sid, course.grade FROM course
		String[] words = sql.trim().toLowerCase().replaceAll("[,\\s]+", ",").split(",");
		int fromid = Arrays.asList(words).indexOf("from");
		if(fromid == -1) return false; // make sure from appear
		for(String word:Arrays.copyOfRange(words, fromid + 1, words.length)){
			if(!this.isValidName(word)) {
				return false; // check words after from
			}
		}
		if(words[1].equals("*") && fromid==2) return true;
		for(String word:Arrays.copyOfRange(words, 1, fromid)){
			if(!(this.isValidName(word) || this.isValidColName(word))) {
				return false; // check words between select and from
			}
		}

		return true;
	}
	
	public boolean checkDelete(String sql) {
		// DELETE FROM course
		// DELETE FROM course WHERE grade = "E"
		String[] words = sql.toLowerCase().trim().split("[\\s]+");
		if(words.length < 3) return false;
		if(!words[1].equals("from")) return false;
		if(!isValidName(words[2])) return false;
		if(Arrays.asList(words).contains("where")) {
			if(!words[3].equals("where")) {
				return false;
			}
		}
		return true;
	}

	public boolean checkInsert(String sql) {
		// INSERT INTO course (sid, homework, project, exam, grade) VALUES (1, 99, 100, 100, "A")
		String[] words = sql.trim().toLowerCase().split("[\\s]+");
		if(words.length <= 3) return false;
		if(!words[1].equals("into")) return false;
		if(!isValidName(words[2])) return false;
		if(!words[3].startsWith("(")) return false;
		if(!words[words.length-1].endsWith(")")) return false;
		
		int valueid = Arrays.asList(words).indexOf("values");
		if(valueid == -1) return false;
		if(!words[valueid-1].endsWith(")")) return false;
		if(!words[valueid+1].startsWith("(")) return false;
		Pattern p = Pattern.compile("\\((.+)\\).*\\((.+)\\)"); 
		Matcher matcher = p.matcher(sql);
		if(!matcher.find()) return false;
		return true;
	}
	
	public static void main(String[] args) {
		String sql = "DELETE FROM course WHERE grade = 'E'";
		ParserHelper helper = new ParserHelper();
		//helper.checkSelect(sql);
		System.out.println(helper.checkDelete(sql));
	}

}
