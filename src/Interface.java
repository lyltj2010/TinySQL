import java.io.Console;
public class Interface {
	// accept SQL statement at a line
	// read a file containing many statements and output to file
    public static void main(String[] args) {
    	Console cnsl = null;
    	String query = null;
    	
    	cnsl = System.console();
		while(cnsl != null) {
			query = cnsl.readLine("> ");
			System.out.println(query);
		} 	
    }
}

