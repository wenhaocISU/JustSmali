package newJDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tools.Adb;
import tools.Jdb;

public class Main{

	public static String newestLine = "";

	public static void main(String[] args) {
		for (int i = 0; i < 100000; i++) {
			System.out.println("Iteration: (" + i + "/100000)");
			it();
		}
	}
	
	private static void it() {
		
		Adb adb = new Adb();
		adb.uninstallApp("the.app");
		adb.installApp("C:/Users/Wenhao/Desktop/TheApp_smali.apk");
		adb.startApp("the.app", "the.app.MainActivity");
		Jdb jdb = new Jdb();
		jdb.init("the.app");
		int[] bp = {85,71,86,72,87,83,75,76,77,78,79,88,80,89,90,91};
		for (int i : bp)
			jdb.setBreakPointAtLine("the.app.Irwin$3", i);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ArrayList<String> seq = 
			new ArrayList<String>(
			Arrays.asList(
			"275 1215", "575 1255", "532 789"
			));
		for (int i = 0, len = seq.size(); i < len; i++) {
			adb.click(seq.get(i));
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(jdb.getProcess().getInputStream()));
		ExecutorService executor = Executors.newFixedThreadPool(1);
		JDBCaller c = new JDBCaller(in);
		while (true) {
			Future<String> newestLine = executor.submit(c);
			try {
				String n = newestLine.get(1, TimeUnit.SECONDS);
				System.out.println(n);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				break;
			}
		}
		executor.shutdownNow();
		if (executor.isShutdown())
			System.out.println("executor down");
		
	}

		
	
	
}
