package AgentArchitectures;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
	//Format of the date for the log
	static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");  
	
	public static void print_msg(String agent, String msg) {
		System.out.println(dtf.format(LocalDateTime.now()) + " - " + agent + " - " + msg);
	}
}
