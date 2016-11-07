package tinySQL;
import storageManager.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class QueryCreate {
	MainMemory mem=new MainMemory();
	Disk disk=new Disk();
	SchemaManager schema_manager=new SchemaManager(mem,disk);
	
	private FieldType getFieldType(String type) {
		type = type.toLowerCase();
		if(type.equals("int")) {
			return FieldType.INT;
		} else {
			return FieldType.STR20;
		}
	}
	
	public void createRelation(String name, LinkedHashMap<String, String> raw_schema) {
		// Create a schema
	    System.out.print("Creating schema " + name + "..." + "\n");
	    ArrayList<String> field_names=new ArrayList<String>();
	    ArrayList<FieldType> field_types=new ArrayList<FieldType>();
	    
	    for(String key : raw_schema.keySet()) {	
	    	field_names.add(key);
	    	field_types.add(getFieldType(raw_schema.get(key)));
	    }
	    
	    Schema schema=new Schema(field_names,field_types);
	    System.out.print("Schema created successfully..." + "\n");
    
	    // Create a relation with created schema by schema manager
	    String relation_name= name;
	    System.out.print("Creating table " + relation_name + "..." + "\n");
	    Relation relation_reference=schema_manager.createRelation(relation_name,schema);
	    System.out.print("The schema details of table:" + "\n");
	    System.out.print(relation_reference.getSchema() + "\n");
	}
	

	public static void main(String[] args) {
		QueryCreate query = new QueryCreate();
		Parser parser = new Parser();
		String sql = "CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)";
		
		String name = parser.parseCreate(sql).a;	
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, String> raw_schema = parser.parseCreate(sql).b;
		
		query.createRelation(name, raw_schema);
	}

}
