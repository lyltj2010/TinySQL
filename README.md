# TinySQL

### Software Architecture

This tiny sql consists of several parts, parser, logical query plan, physical query plan and some optimization. Each part may consists of some helper classes which will be demonstrated in each corresponding part.

#### Parser

The parser parse sql input into fragments which will be restructured for the purpose of logical or physical query plan. For CREATE, DROP, INSERT statements, the parser just return a table name or a pair contains the table name and linked hash map. For SELECT and DELETE statements, a parser tree will generated because they are more complicated.  

**Create**:  

A pair `Pair<String, LinkedHashMap<String, FieldType>>` will be returned. The first element is the schema name, and the second element is a linked hash map, in which the key is the attribute name and the value is the field type of the attribute. This parserCreate assume the syntax is correct because the input sql will be checked using the parser helper. 

```java
public Pair<String, LinkedHashMap<String, FieldType>> parseCreate(String sql) {
	// CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)
	sql = sql.trim().toLowerCase();
	// more code ...
	return new Pair<String, LinkedHashMap<String, FieldType>>(table_name, schema);
}
```

**Drop**:  

The parseDrop function is pretty simple, just extract the table name and pass it down to query plan and drop the corresponding relation.  

```java
public String parseDrop(String sql) {
	// DROP TABLE course
	String table_name = sql.trim().toLowerCase().split("[\\s]")[2];
	return table_name;
}
```

**Insert**:  

The parseInsert function is pretty similar to the parseCrete function. A pair will be returned. The first element is the relation name, and the second is a linked hash map in which the key is field name and the value is what will be inserted. And then the query plan can use the table name to find corresponding relation name and insert the record to it.

```java
public Pair<String, LinkedHashMap<String, String>> parseInsert(String sql) {
	// INSERT INTO course (sid, homework, project, exam, grade) VALUES (1, 99, 100, 100, "A")
	String table_name = sql.trim().toLowerCase().split("[\\s]+")[2];
	// more code ...
	return new Pair<String, LinkedHashMap<String, String>>(table_name, record);
}
```

**Select**:  

The select statement is the most complex part of this tiny sql. There are two parserSelect functions. The first deals with one table case and the second for multiple table case. For flow for the multiple tables case is that tables will be joined first and the call the one table case function.  

For each select statement, a parse tree will be generated. Each part has a place in the tree, like the attributes list, the expression tree etc.  

```java
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
	// more code ....;
}
```

The most simple case is `select * from course` which will output every tuple in relation course. Tiny sql knows that asteroid indicates that all fields should be output and not all fields will be output when otherwise. The `outputTuples` method in `PhysicalQuery` class handle this.  

For select clause with condition like `SELECT * FROM course WHERE exam = 100 AND project = 100`, an expression tree will be generated which represents the condition with tree and provides evaluation functionality. A tuple retrieved from a relation and evaluate against the condition, a boolean value will be returned. When `true` returned, output the tuple; skip the tuple otherwise. Here is a short view of the expression tree. First tree will be built and then evaluation can be performed.  

```java
public Node buildTree(String bonds) {
	root = new Node();
	String[] words = splitWords(bonds);
	for(String word:words) {
	// some long code ......
	}
	while (!operator.isEmpty()) { operation(operator.pop()); }
	root = operand.pop();
	return root;
}
```

Evaluate it:  

```java
public boolean check(Tuple tuple, Node node) {
	boolean result = toBoolean(evaluate(tuple, node));
	return result;
}

private String evaluate(Tuple tuple, Node node) {
	String op = node.op;
	String left_op, right_op;
	boolean tmp;
	if(op.equals("&")) {
		// ...
		return String.valueOf(tmp);
	}
	// some long code ......
	} else {
		return fetchTuple(tuple, op);
	}
}
```

For multiple case, table will joined first, joined table returned and  then it's almost the same as the single table case. The `Join` class handle this operation. Base on two relations, create a new schema, cross join tuples from the two relations and insert into the new relation.   

The new schema is handled by this method:

```java
private static Schema joinTwoSchema(String t1, String t2, Schema schema1, Schema schema2) {
	ArrayList<String> field_names=new ArrayList<String>();
	ArrayList<FieldType> field_types=new ArrayList<FieldType>();
	for(int i=0; i < schema1.getNumOfFields(); i++) {
		String field_name = schema1.getFieldNames().get(i);
		// more code ...
	}
	Schema schema3 = new Schema(field_names, field_types);
	return schema3;
}
```

Tuples will be joined with this method:

```java
private static Tuple joinTwoTuples(Tuple tp1, Tuple tp2, Tuple tp3) {
	// join tuple tp1 tp2 into tp3
	int index = 0; // access by offset
	for(; index < tp1.getNumOfFields(); index++) {
		// more code ...
	}
	// loop tp2, fetch its values
}
```

Also, optimization can be done here because selection can be pushed down. Optimization will be explained in the optimization section.  

The `ParserHelper` class check the syntax of input sql commands and will be integrated into this tiny sql.

#### Physical Query

`PhysicalQuery` class  use the information retrieved by the parser and do physical query which interact with storage manager. Most of the API provided by storage manager is demonstrated by the `TestStorageManager` class.   

The CREATE, INSERT, DROP and DELETE query are pretty straightforward and will not be explained in details here.  

For select statement, find the relation, read its block into main memory and retrieve tuples. Here is short view of select physical query. 

```java
private void selectQuery1(ParserTree tree) {
	// One table case
	ArrayList<Tuple> tuples = new ArrayList<Tuple>();
	String relation_name = tree.tables[0];
	Relation relation_reference = schema_manager.getRelation(relation_name);
	for(int i = 0; i < relation_reference.getNumOfBlocks(); i++) {
		relation_reference.getBlock(i,0); // get its block of relation into memory block 0
		// more code ....
	}
	outputTuples(tree.attributes, tuples);
}
```

Also multiple tables case is handled. As discussed earlier, tables are joined first, and the joined table returned for query purpose. After query complete, the temporary tables will be deleted. Here is a short view of the multiple tables select query.

```java
private void selectQuery2(ParserTree tree){
	// Multiple tables case, cross join tables and use one table case on joined table
	String[] tables = tree.tables;
	ArrayList<String> temp_tables  = new ArrayList<String>();
	if(tree.where) {
		temp_tables = Join.joinTables(this, tables, tree.conditions);
	} else {
		temp_tables = Join.joinTables(this, tables);
	}
	String table = temp_tables.get(temp_tables.size()-1);
	tree.tables = new String[] {table}; // replace with the final joined table
	selectQuery1(tree);
	Join.DropTempTables(this, temp_tables);
}
```

Apart from the select and select where case, distinct and order by functionalities are also handled in physical query plan.   

For distinct functionality, we need a new tuple with hash code defined. With hash code, we spot which tuple is duplicated and thus remove it from the result. Here is how the hash code defined.   

```java
public int hashCode() {
	String str = "";
	for(Field field:fields) {
		str += field;
	}
	return str.hashCode();
}
```

Details can be found in the `MyTuple` class. After `distinctTuples`, tuples will turned into set from bag.  

Another feature is order by. In the new tuple, the comparedTo method is redefined. Here is a short view of how to make tuple comparable.

```java
@Override
public int compareTo(MyTuple key2) {
	if(key.type == FieldType.INT) {
		return ((Integer)key.integer).compareTo(key2.key.integer);
	}
	if(key.type == FieldType.STR20) {
		return  key.str.compareTo(key2.key.str);
	}
	return 0;
}
```

Once tuples can be compared, they can be compared. A binary heap data structure is used. Push tuple into heap one by one, pop in out one by one. The popped tuples are sorted and are what we want.

### Experiments 

Here are some demos to show what this tiny sql can do.  

Create and Insert:

```bash
CREATE TABLE course (sid INT, homework INT, project INT, exam INT, grade STR20)
INSERT INTO course (sid, homework, project, exam, grade) VALUES (1, 99, 100, 100, "A")
INSERT INTO course (sid, homework, project, exam, grade) VALUES (2, 90, 98, 99, "E")
INSERT INTO course (sid, homework, project, exam, grade) VALUES (4, 99, 90, 100, "B")
```

Simple select statement like `Select * from course` will output:

```bash
1	99	100	100	A	
2	90	98	99	E	
4	99	90	100	B
```

Select with condition like `SELECT * FROM course WHERE exam = 100 AND project > 90` will output:

```bash
1	99	100	100	A
```

Select with distinct like `SELECT DISTINCT homework FROM course` will output:

```bash
homework
99	
90
```

Select with order by like `SELECT * FROM course ORDER BY project` will output:

```bash
1	99	100	100	A	
2	90	98	99	E	
4	99	90	100	B
```

Select with projection like `SELECT sid, homework, project FROM course ORDER BY project` will output:

```bash
sid	 homework	project
1	  99		100	
2	  90		98	
4	  99		90	
```

Delete statement like `DELETE FROM course WHERE grade = "E"` and then `SELECT * FROM course` will output:

```bash
1	99	100	100	A	
4	99	90	100	B
```

Drop statement like `DROP TABLE course` and then `SELECT * FROM course` will output:

```bash
getRelation ERROR: relation course does not exist
```

Multiple tables case:

```bash
CREATE TABLE course2 (sid INT, exam INT, grade STR20)
INSERT INTO course2 (sid, exam, grade) VALUES (1, 100, "A")
INSERT INTO course2 (sid, exam, grade) VALUES (2, 25, "E")
INSERT INTO course2 (sid, exam, grade) VALUES (17, 0, "A")
```

Then `SELECT course.sid, course.grade, course2.grade FROM course, course2 WHERE course.sid = course2.sid` will output:

```bash
course.sid	course.grade	course2.grade
1				A					A	
2				E					E
```

Then `SELECT * FROM course, course2 WHERE course.sid = course2.sid ORDER BY course.exam` will output:

```bash
2	90	98	99	E	2	25	E	
1	99	100	100	A	1	100	A
```

More test case can be found in the `test.txt` file.



### Optimization

One optimization implemented in this tiny sql is the selection during join. In multiple table selection case, if we push the selection down and evaluate the condition as early as possible, then we can reduce temporary relations to a great extend.  

Condition may have several sub conditions and we can split it and assign each sub condition to it's corresponding relation. This is done by the `splitNode` method.

```java
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
```

Once split, we can use the `pushToWhich` method to find it's corresponding relation. Then evaluate this condition against every tuple in the relation. If return true, use it in cross join operation; else skip it.  

Here is a short view how the selection pushed down. The code `if(!check(t1, tp1, nodes, exp_tree))  continue` do the push select down job.

```java
private static String joinTwoTables(PhysicalQuery Phi, ...) {
	SchemaManager schema_manager = Phi.schema_manager;
	// more code ...
	for(int i = 0; i < relation1.getNumOfBlocks(); i++) {
		relation1.getBlock(i, 0);
		Block block_reference1 = Phi.mem.getBlock(0);
		if (block_reference1.getNumTuples() == 0) continue;
		for(Tuple tp1:block_reference1.getTuples()) 
			if(!check(t1, tp1, nodes, exp_tree))  continue; // Selection pushed down
			for(int j = 0; j < relation2.getNumOfBlocks(); j++) {
				relation2.getBlock(j, 2); 
				Block block_reference2 = Phi.mem.getBlock(2);
				if (block_reference2.getNumTuples() == 0) continue;
				for(Tuple tp2:block_reference2.getTuples()) {
					if(!check(t2, tp2, nodes, exp_tree)) continue; // Selection pushed down
					tp3 = joinTwoTuples(tp1, tp2, tp3);
					PhysicalQuery.appendTupleToRelation(relation3, Phi.mem, 9, tp3);
				}
			}
		}
	}
	return t3;
}
```
