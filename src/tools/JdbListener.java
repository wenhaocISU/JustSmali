package tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class JdbListener implements Runnable{

	private InputStream stream;
	public static boolean stopThread;
	private ArrayList<String> hits = new ArrayList<String>();
	private String newestHit;
	
	public JdbListener(InputStream stream) {
		this.stream = stream;
		stopThread = false;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = in.readLine())!=null) {
				if (stopThread)	return;
				if (line.startsWith("Breakpoint hit: ")) {
					hits.add(line);
					newestHit = line;
				}
				//System.out.println(" [J] " + line);
			}
			in.close();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public ArrayList<String> getHits() {
		return hits;
	}
	
	public String getNewestHit() {
		return newestHit;
}

}
