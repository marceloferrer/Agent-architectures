package AgentArchitectures;

//Imports for date manipulation
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/// Class in charge of generic tasks 
public class Utils {
	//Format of the date for the log
	static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");  
	
	// Path of all the files. Change in case you want the txt read and write in a different specific folder
	//public static String Path = "C:\\Master\\Multi-Agent System\\book_seller\\src\\Files\\";
	public static String Path = "src\\Files\\";
	
	// Constants to add colors to the console
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
	public static final String BLUE = "\033[0;34m"; // BLUE
	public static final String RESET = "\033[0m";  // Text Reset
	
	// Print a generic message with the date and the agent information
	public static void print_msg(String agent, String msg) {
		System.out.println(dtf.format(LocalDateTime.now()) + " - " + agent + " - " + msg);
	}
}
