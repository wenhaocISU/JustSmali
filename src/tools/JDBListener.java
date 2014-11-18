package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.Callable;

public class JDBListener implements Callable<String>{

	private BufferedReader in;
	
	public JDBListener(BufferedReader in) {
		this.in = in;
	}
	
	public String call() {
		String result = "";
		try {
			//System.out.println("(before readLine)");
			//Thread.sleep(2000);
			result = in.readLine();
			//System.out.println("(after readLine)");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
}
