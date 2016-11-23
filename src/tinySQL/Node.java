package tinySQL;
public class Node {
	String op;
	Node left, right;
	public Node() {};
	public Node(String op) { this.op = op; }
	public Node(String op, Node left, Node right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}
	
    Node copy() {
        Node left = null;
        Node right = null;
        if (this.left != null) {
            left = this.left.copy();
        }
        if (this.right != null) {
            right = this.right.copy();
        }
        return new Node(op, left, right);
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
