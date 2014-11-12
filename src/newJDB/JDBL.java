package newJDB;

import java.io.BufferedReader;
import java.io.IOException;

import newJDB.Main;

public class JDBL implements Runnable{

	private BufferedReader in;
	private String newestLine = "";
	
	public JDBL(BufferedReader in) {
		this.in = in;
	}
	
	public void run() {
		//System.out.println("before readline");
		try {
			Main.newestLine = in.readLine();
			
			System.out.println("-Line- " + newestLine);
			if (Main.newestLine == null)
				System.out.println("THEODDONE");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("after readline");
	}

	public String getNewestLine() {
		if (newestLine == null)
			System.out.println("ANOTHER THEODDONE");
		return newestLine;
	}

}
