package tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class JdbListener implements Runnable{

	private InputStream stream;
	private boolean stop;
	private ArrayList<String> hits = new ArrayList<String>();
	private String newestHit;
	private boolean bpMode;
	private boolean localMode;
	
	public JdbListener(InputStream stream) {
		this.stream = stream;
		stop = false;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line;
			while (true) {
				if (stop)	break;
				if ((line = in.readLine())==null)
					continue;
				System.out.println(line);
				if (line.startsWith("Breakpoint hit: ")) {
					hits.add(line);
					newestHit = line;
				}
//				System.out.println(" [J] " + line);
			}
			in.close();
			System.out.println("\nlistener exiting...");
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public ArrayList<String> getHits() {
		return hits;
	}
	
	public String getNewestHit() {
		return newestHit;
	}

	public boolean isBpMode() {
		return bpMode;
	}

	public void setBpMode(boolean bpMode) {
		this.bpMode = bpMode;
	}

	public boolean isLocalMode() {
		return localMode;
	}

	public void setLocalMode(boolean localMode) {
		this.localMode = localMode;
	}
	
	public void stopListening() {
		this.stop = true;
	}

}
