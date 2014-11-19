package tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class oldJdbListener implements Runnable{

	private InputStream stream;
	private ArrayList<String> hits = new ArrayList<String>();
	private String newestHit;
	private boolean bpMode;
	private boolean localMode;
	
	public oldJdbListener(InputStream stream) {
		this.stream = stream;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = in.readLine())!=null)
				System.out.println(line);
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

}
