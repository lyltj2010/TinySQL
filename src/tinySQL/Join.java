package tinySQL;
import java.util.ArrayList;
import java.util.HashSet;
import storageManager.*;

public class Join {
	public static ArrayList<String> joinTables(PhysicalQuery Phi, String[] tables, ExpressionTree exp_tree) {
		// Join multiple tables and return temporary tables
		ArrayList<String> temp_tables = new ArrayList<String>();
		Node node = exp_tree.getRoot();
		ArrayList<Node> nodes = splitNode(node); // sub conditions to be pushed down
		
		String t1; String t2;
		int index = 0;
		for(; index < tables.length; index++) {
			if(index == 0) {
				t1 = tables[index]; t2 = tables[index+1];
				Join.joinTwoTables(Phi, t1, t2, nodes, exp_tree);
				temp_tables.add(t1 + "_cross_" + t2);
				index += 1;
			} else {
				t1 = temp_tables.get(temp_tables.size()-1);
				t2 = tables[index];
				Join.joinTwoTables(Phi, t1, t2, nodes, exp_tree);
				temp_tables.add(t1 + "_cross_" + t2);
			}
		}
		return temp_tables;
	}
	
	public static ArrayList<String> joinTables(PhysicalQuery Phi, String[] tables) {
		// Handle query without where clause
		ArrayList<String> temp_tables = new ArrayList<String>();
		String t1; String t2;
		int index = 0;
		for(; index < tables.length; index++) {
			if(index == 0) {
				t1 = tables[index]; t2 = tables[index+1];
				Join.joinTwoTables(Phi, t1, t2);
				temp_tables.add(t1 + "_cross_" + t2);
				index += 1;
			} else {
				t1 = temp_tables.get(temp_tables.size()-1);
				t2 = tables[index];
				Join.joinTwoTables(Phi, t1, t2);
				temp_tables.add(t1 + "_cross_" + t2);
			}
		}
		return temp_tables;
	}

	private static ArrayList<Node> splitNode(Node node) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		if(!"&|".contains(node.op)) {
			nodes.add(node);
		} else {
			nodes.addAll(splitNode(node.left));
			nodes.addAll(splitNode(node.right));
		}
		return nodes;
	}

	private static HashSet<String> pushToWhich(Node node) {
		HashSet<String> tableSet = new HashSet<String>();
		if(node.left == null || node.right == null) {
			if(node.op.contains(".")) tableSet.add(node.op.split("\\.")[0]);
			return tableSet;
		}
		tableSet.addAll(pushToWhich(node.left));
		tableSet.addAll(pushToWhich(node.right));
		return tableSet;
	}
	
	private static String joinTwoTables(PhysicalQuery Phi, String t1, String t2) {
		SchemaManager schema_manager = Phi.schema_manager;
		Relation relation1 = schema_manager.getRelation(t1);
		Relation relation2 = schema_manager.getRelation(t2);
		Schema schema3 = joinTwoSchema(t1, t2, relation1.getSchema(), relation2.getSchema());
		String t3 = t1 + "_cross_" + t2; // joined table name like course_cross_course2
		schema_manager.createRelation(t3, schema3);
		Relation relation3 = schema_manager.getRelation(t3);
		Tuple tp3 = relation3.createTuple();
		
		// Read in tuples from t1 and t2, join them and insert into t3
		for(int i = 0; i < relation1.getNumOfBlocks(); i++) {
			relation1.getBlock(i, 0);
			Block block_reference1 = Phi.mem.getBlock(0);
			if (block_reference1.getNumTuples() == 0) continue;
			for(Tuple tp1:block_reference1.getTuples()) {
				// inner loop for relation 2
				for(int j = 0; j < relation2.getNumOfBlocks(); j++) {
					relation2.getBlock(j, 2); // TODO read in multiple blocks
					Block block_reference2 = Phi.mem.getBlock(2);
					if (block_reference2.getNumTuples() == 0) continue;
					for(Tuple tp2:block_reference2.getTuples()) {
						tp3 = joinTwoTuples(tp1, tp2, tp3);
						PhysicalQuery.appendTupleToRelation(relation3, Phi.mem, 9, tp3);
					}
				}
			}
		}
		return t3;
	}
	
	private static String joinTwoTables(PhysicalQuery Phi, String t1, String t2, ArrayList<Node> nodes, ExpressionTree exp_tree) {
		SchemaManager schema_manager = Phi.schema_manager;
		Relation relation1 = schema_manager.getRelation(t1);
		Relation relation2 = schema_manager.getRelation(t2);
		Schema schema3 = joinTwoSchema(t1, t2, relation1.getSchema(), relation2.getSchema());
		String t3 = t1 + "_cross_" + t2; // joined table name like course_cross_course2
		schema_manager.createRelation(t3, schema3);
		Relation relation3 = schema_manager.getRelation(t3);
		Tuple tp3 = relation3.createTuple();
		
		// Read in tuples from t1 and t2, join them and insert into t3
		for(int i = 0; i < relation1.getNumOfBlocks(); i++) {
			relation1.getBlock(i, 0);
			Block block_reference1 = Phi.mem.getBlock(0);
			if (block_reference1.getNumTuples() == 0) continue;
			for(Tuple tp1:block_reference1.getTuples()) {
				if(!check(t1, tp1, nodes, exp_tree)) continue; // selection pushed down
				// inner loop for relation 2
				for(int j = 0; j < relation2.getNumOfBlocks(); j++) {
					relation2.getBlock(j, 2); // TODO read in multiple blocks
					Block block_reference2 = Phi.mem.getBlock(2);
					if (block_reference2.getNumTuples() == 0) continue;
					for(Tuple tp2:block_reference2.getTuples()) {
						if(!check(t2, tp2, nodes, exp_tree)) continue; // selection pushed down
						tp3 = joinTwoTuples(tp1, tp2, tp3);
						PhysicalQuery.appendTupleToRelation(relation3, Phi.mem, 9, tp3);
					}
				}
			}
		}
		return t3;
	}
	
	private static boolean check(String t, Tuple tp, ArrayList<Node> nodes, ExpressionTree exp_tree) {
		if(t.contains("_cross_")) return true; // joined table, already checked
		HashSet<String> tableSet = new HashSet<String>();
		for(Node node:nodes) {
			tableSet = pushToWhich(node);
			if(tableSet.size() > 1) continue;
			if(tableSet.size() == 1) {
				if(!tableSet.iterator().next().equals(t)) continue;
				return exp_tree.check(tp, stripTableName(node));
			}
		}
		return true;
	}
	
	private static Node stripTableName(Node node) {
		// in subnode, attr may contain course.sid, but in single table, just sid in fields
		if(node.left == null || node.right == null) {
			if(node.op.contains(".")) node.op = node.op.split("\\.")[1];
			return node;
		}
		node.left = stripTableName(node.left);
		node.right = stripTableName(node.right);
		return node;
	}
	
	private static Schema joinTwoSchema(String t1, String t2, Schema schema1, Schema schema2) {
		ArrayList<String> field_names=new ArrayList<String>();
		ArrayList<FieldType> field_types=new ArrayList<FieldType>();
		
		for(int i=0; i < schema1.getNumOfFields(); i++) {
			String field_name = schema1.getFieldNames().get(i);
			if(!field_name.contains(".")) field_name = t1 + "." + field_name;
			field_names.add(field_name);
			field_types.add(schema1.getFieldTypes().get(i));
		}
		
		for(int i=0; i < schema2.getNumOfFields(); i++) {
			String field_name = schema2.getFieldNames().get(i);
			if(!field_name.contains(".")) field_name = t2 + "." + field_name;
			field_names.add(field_name);
			field_types.add(schema2.getFieldTypes().get(i));
		}
		
		Schema schema3 = new Schema(field_names, field_types);
		return schema3;
	}
	
	private static Tuple joinTwoTuples(Tuple tp1, Tuple tp2, Tuple tp3) {
		// join tuple tp1 tp2 into tp3
		int index = 0; // access by offset
		for(; index < tp1.getNumOfFields(); index++) {
			String value = tp1.getField(index).toString();
			if(isInt(value)) { tp3.setField(index, Integer.parseInt(value)); }
			else { tp3.setField(index, value); }
		}
		// loop tp2, fetch its values
		index -= tp1.getNumOfFields();
		for(; index < tp2.getNumOfFields(); index++) {
			String value = tp2.getField(index).toString();
			if(isInt(value)) { tp3.setField(index + tp1.getNumOfFields(), Integer.parseInt(value)); }
			else { tp3.setField(index + tp1.getNumOfFields(), value); }
		}
		return tp3;
	}
	
	public static void DropTempTables(PhysicalQuery Phi, ArrayList<String> temp_tables) {
		for(int i=0; i<temp_tables.size(); i++) {
			SchemaManager schema_manager = Phi.schema_manager;
			schema_manager.deleteRelation(temp_tables.get(i));
		}
	}
	
	private static boolean isInt(String str) { return Character.isDigit(str.charAt(0)); }
}
