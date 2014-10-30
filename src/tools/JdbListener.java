package tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JdbListener implements Runnable{

	private InputStream stream;
	public static boolean stopThread;
	
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
				System.out.println(" [J] " + line);
			}
			in.close();
		}	catch (Exception e) {e.printStackTrace();}
	}

}
