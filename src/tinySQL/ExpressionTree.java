package tinySQL;
import java.util.*;
/*
SELECT * FROM course WHERE exam = 100
SELECT * FROM course WHERE grade = "A"
SELECT * FROM course WHERE exam = 100 AND project = 100
SELECT * FROM course WHERE exam = 100 OR exam = 99
SELECT * FROM course WHERE exam > 70
SELECT * FROM course WHERE exam = 100 OR homework = 100 AND project = 100
SELECT * FROM course WHERE (exam + homework) = 200 
SELECT course.sid, course.grade, course2.grade FROM course, course2 WHERE course.sid = course2.sid
SELECT DISTINCT course.grade, course2.grade FROM course, course2 WHERE course.sid = course2.sid
SELECT * FROM course, course2 WHERE course.sid = course2.sid ORDER BY course.exam
SELECT * FROM course, course2 WHERE 
	  course.sid = course2.sid AND course.exam = 100 AND course2.exam = 100
SELECT * FROM course, course2 WHERE course.sid = course2.sid AND course.exam > course2.exam
SELECT * FROM course, course2 WHERE 
	  course.sid = course2.sid AND course.exam > course2.exam AND course.homework = 100
SELECT DISTINCT course.grade, course2.grade FROM course, course2 WHERE 
	  course.sid = course2.sid AND course.grade = "A" AND course2.grade = "A" ORDER BY course.exam
SELECT * FROM r, s, t WHERE r.a = t.a AND r.b = s.b AND s.c = t.c
*/

public class ExpressionTree {	
    /** class Node **/
    private class Node {    
        String op;
        Node left, right;
        public Node() {};
        public Node(String op) { this.op = op; }
        public Node(String op, Node left, Node right) {
        	this.op = op;
        	this.left = left;
        	this.right = right;
        }
        private String infix() {
        	String str = "";
        	if (left != null) 
    			str += "("+left.infix();
    		str += op;
    		if (right != null)
    			str += right.infix()+")";
    		return str;
        }
        public String toString(){
        	return infix();
        }
    }
    
    private Stack<String> operator;
    private Stack<Node> operand;
    private Node root;
    
    public ExpressionTree(String conds) {
    	 this.operator = new  Stack<String>();
    	 this.operand = new Stack<Node>();	
    }
    
    public Node buildTree(String conds) {
    	root = new Node();
    	String[] words = splitWords(conds);
    	for(String word:words) {
    		String kind = isWhat(word);
    		switch(kind) {
	    		case "operator": processOperator(word); break;
	    		case "leftParenthesis": processLeftParenthesis(); break;
	    		case "rightParenthesis": processRightParenthesis(); break;
	    		default: processOperand(word);
    		}
    	}
    	while (!operator.isEmpty()) { operation(operator.pop()); }
    	root = operand.pop();
    	return root;
    }
    
    // Some helper functions
    private void processOperator(String op) {
    	// "+","-","*","/","=",">","<", "&", "|"
    	int precedence = getPrecedence(op);
    	while((!operator.isEmpty()) && precedence <= getPrecedence(operator.peek())) {
    		operation(operator.pop());
    	}
    	operator.push(op); // don't forget to push in
    }
    
    private void processLeftParenthesis() { operator.push("("); }
    
    private void processRightParenthesis() {
    	while(!operator.empty() && !operator.peek().equals("(")) {
    		operation(operator.pop());
    	}
    	operator.pop(); // pop "(" out	
    }
    
    private void processOperand(String op) { operand.push(new Node(op)); }
    
    private void operation(String op) {
    	Node right = operand.pop();
    	Node left = operand.pop();
    	Node tmp = new Node(op, left, right);
    	operand.push(tmp);
    }

	private String[] splitWords(String conds) {
		conds = conds.toLowerCase().replaceAll(" and "," & ").replaceAll(" or "," | ");
		final String[] ops = new String[] {"\\+","-","\\*","/","=",">","<"};
		for(String op:ops) {
			if(conds.contains(op)) conds = conds.replaceAll(op, " "+ op +" ");
			if(conds.contains("(")) conds = conds.replaceAll("\\(", " ( ");
			if(conds.contains(")")) conds = conds.replaceAll("\\)", " ) ");
		}
		return conds.trim().split("[\\s]+");
	}
	
	private String isWhat(String word) {
		final String[] ops = new String[] {"+","-","*","/","&","|","=",">","<"};
		if(Arrays.asList(ops).contains(word)) {
			return "operator";
		} else if(word.equals("(")) {
			return "leftParenthesis";
		} else if(word.equals(")")) {
			return "rightParenthesis";
		} else {
			return "operand";
		}
	}
	
	private int getPrecedence(String op) {
		final String[] p1 = new String[] {"*","/"};
		final String[] p2 = new String[] {"+","-",">","<"};
		if(Arrays.asList(p1).contains(op)) { return 3; }
		else if(Arrays.asList(p2).contains(op)) { return 2; } 
		else if(op.equals("=")) {return 1;}
		else if(op.equals("|")) {return -1;}
		else { return 0; } // like & ( )
	}
	
	public static void main(String[] args) {
		String conds = "course.sid = course2.sid AND course.exam > course2.exam";
		ExpressionTree exp_tree = new ExpressionTree(conds);
		Node root = exp_tree.buildTree(conds);
		System.out.println(root);
	}
}
