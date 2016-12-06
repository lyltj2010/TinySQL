import java.io.Console;

import tinySQL.PhysicalQuery;
public class Interface {
	// accept SQL statement at a line
	// read a file containing many statements and output to file
    public static void main(String[] args) {
    	PhysicalQuery physical_query = new PhysicalQuery();
    	if(args.length == 1) {
    		physical_query.parseFile(args[0]);
    		
    	} else {
    		Console cnsl = null;
        	String query = null;
        	cnsl = System.console();
    		while(cnsl != null) {
    			query = cnsl.readLine("> ");
    			physical_query.exec(query);
    		} 
    	}
    		
    }
}

