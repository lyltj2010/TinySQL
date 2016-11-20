package tinySQL;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import storageManager.*;

public class PhysicalQuery {
	Parser parser;
	MainMemory mem;
	Disk disk;
	SchemaManager schema_manager;
	public PhysicalQuery() {
		parser = new Parser();
		mem = new MainMemory(); disk=new Disk();
		schema_manager=new SchemaManager(mem,disk);
		disk.resetDiskIOs(); disk.resetDiskTimer();
	}
	
	public void exec(String sql) {
		String stmt = sql.trim().toLowerCase().split("[\\s]+")[0];
		switch(stmt) {
		case "insert": this.insertQuery(sql); break;
		case "select": this.selectQuery(sql); break;
		case "create": this.createQuery(sql); break;
		case "drop": this.dropQuery(sql); break;
		case "delete": this.deleteQuery(sql); break;
		default:;
		}
	}
	
	private Schema createSchema(LinkedHashMap<String, FieldType> raw_schema) {
		// System.out.print("Creating schema " + "..." + "\n");
		ArrayList<String> field_names=new ArrayList<String>();
	    ArrayList<FieldType> field_types=new ArrayList<FieldType>();
	    for(String key : raw_schema.keySet()) {	
	    	field_names.add(key);
	    	field_types.add(raw_schema.get(key));
	    }
	    Schema schema=new Schema(field_names,field_types);
	    // System.out.print("Schema created successfully..." + "\n");
	    return schema;
	}
	
	private Tuple createTuple(Relation relation_reference, LinkedHashMap<String, String> tp) {
		Tuple tuple = relation_reference.createTuple();
		for(String key:tp.keySet()) {
			String field = key;
			String value = tp.get(key);
			if(isInt(value)) { tuple.setField(field, Integer.parseInt(value)); }
			else { tuple.setField(field, value); }
		}
		return tuple;
	}
	
	public void createQuery(String sql) {
		String relation_name = parser.parseCreate(sql).a;
		Schema schema = createSchema(parser.parseCreate(sql).b);
		schema_manager.createRelation(relation_name,schema);
	    // Relation relation_reference=schema_manager.createRelation(relation_name,schema);
	    // System.out.print(relation_reference.getSchema() + "\n");
	    // return relation_reference;
	}

	public void insertQuery(String sql) {
		String relation_name = parser.parseInsert(sql).a;
		LinkedHashMap<String, String> tp = parser.parseInsert(sql).b;
		
		Relation relation_reference = schema_manager.getRelation(relation_name);
		Tuple tuple = createTuple(relation_reference, tp);
	    // System.out.print(tuple.getSchema() + "\n");
		appendTupleToRelation(relation_reference, mem, 0, tuple);
	}
	
	public void dropQuery(String sql) {
		String relation_name = parser.parseDrop(sql);
		schema_manager.deleteRelation(relation_name);
	}
	
	public void deleteQuery(String sql) {
		ParserTree tree = parser.parseDelete(sql);
		String relation_name = tree.tables[0];
		Relation relation_reference = schema_manager.getRelation(relation_name);
		int num_men_blocks = mem.getMemorySize();
		int num_relation_blocks = relation_reference.getNumOfBlocks();
		int index_current_block = 0;
		do {
			int num_to_mem = Math.min(num_men_blocks,num_relation_blocks);
			relation_reference.getBlocks(index_current_block,0,num_to_mem);
			for(int i = 0; i < num_to_mem; i++) {
				Block block_reference = mem.getBlock(i);
				if(block_reference.getNumTuples() == 0) continue; // handle holes
				ArrayList<Tuple> tuples = block_reference.getTuples();
				if(tree.where) {
					for(int j = 0; j < tuples.size(); j++) {
						ExpressionTree cond_tree = tree.conditions;
						Tuple tp = tuples.get(j);
						if(cond_tree.check(tp, cond_tree.getRoot()))
							block_reference.invalidateTuple(j);
					}
				} else {
					block_reference.invalidateTuples();
				}
			}
			relation_reference.setBlocks(index_current_block,0,num_to_mem);
			num_relation_blocks -= num_to_mem;
			index_current_block += num_to_mem;
		} while(num_relation_blocks > 0);
	}
	
	public void selectQuery(String sql) {
		ParserTree tree = parser.parseSelect(schema_manager, sql);
		if(tree.tables.length == 1) { selectQuery1(tree); }
		else { selectQuery2(tree); }	
	}
	
	private void selectQuery1(ParserTree tree) {
		// One table case
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		String relation_name = tree.tables[0];
		Relation relation_reference = schema_manager.getRelation(relation_name);
		for(int i = 0; i < relation_reference.getNumOfBlocks(); i++) {
			relation_reference.getBlock(i,0); // get ith block of relation into memory block 0
			Block block_reference=mem.getBlock(0); 
			if (block_reference.getNumTuples() == 0) continue;
			for(Tuple tp:block_reference.getTuples()) {
				if(tree.where) {
					ExpressionTree cond_tree = tree.conditions; 
					if(cond_tree.check(tp, cond_tree.getRoot())) tuples.add(tp); 
				} else {
					tuples.add(tp); 
				}
			}
		}
		for(Tuple tp:tuples) { System.out.println(tp); }
	}
	
	private void selectQuery2(ParserTree tree){
		// Multiple tables case
		System.out.println(tree.conditions.getRoot());
		String[] tables = tree.tables;
		if(tree.where) {
			// TODO handle SELECT * FROM course, course2
		}
		ArrayList<String> temp_tables = Join.joinTables(this, tables, tree.conditions);
		String table = temp_tables.get(temp_tables.size()-1);
		tree.tables = new String[] {table}; // replace with the final joined table
		selectQuery1(tree);
		Join.DropTempTables(this, temp_tables);
	}
	
	public static void appendTupleToRelation(Relation relation_reference, MainMemory mem, int memory_block_index, Tuple tuple) {
	    Block block_reference;
	    if (relation_reference.getNumOfBlocks()==0) {
	      block_reference=mem.getBlock(memory_block_index);
	      block_reference.clear(); //clear the block
	      block_reference.appendTuple(tuple); // append the tuple
	      relation_reference.setBlock(relation_reference.getNumOfBlocks(), memory_block_index);
	    } else {
	      relation_reference.getBlock(relation_reference.getNumOfBlocks() - 1, memory_block_index);
	      block_reference=mem.getBlock(memory_block_index);
	      if (block_reference.isFull()) {
	        block_reference.clear(); //clear the block
	        block_reference.appendTuple(tuple); // append the tuple
	        relation_reference.setBlock(relation_reference.getNumOfBlocks(),memory_block_index); //write back to the relation
	      } else {
	        block_reference.appendTuple(tuple); // append the tuple
	        relation_reference.setBlock(relation_reference.getNumOfBlocks()-1,memory_block_index); //write back to the relation
	      }
	    }
	  }
	
	private boolean isInt(String str) { return Character.isDigit(str.charAt(0)); }
	
	public void parseFile(String file) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			while((line = br.readLine()) != null) {
				this.exec(line);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		PhysicalQuery query = new PhysicalQuery();
		
		long startTime = System.nanoTime();
		query.parseFile("test.txt");
		long endTime = System.nanoTime();
		System.out.println("Used: " + (endTime - startTime) / 1000000 + "ms");		
	}
}
