package smali;

public class Instrumentation {

	private static String line1 = "    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;";
	private static String line3 = "    invoke-virtual {v1, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V";
	private static String line2head = "    const-string v2, ";
	public String addMethodStarting(String classSmali, String methodSig) {
		
		String line2 = line2head + "\"METHOD_STARTING," + methodSig + "\"";
	    String all3Lines = line1 + "\n" + line2 + "\n" + line3;
	    
		String left = classSmali.substring(0, classSmali.lastIndexOf("\n\n")+2);
		String right = classSmali.substring(classSmali.lastIndexOf("\n\n")+2);
		
		if (right.contains("    .prologue\n")) {
			right = right.replace("    .prologue\n", "");
			all3Lines = "    .prologue\n" + all3Lines;
		}
		
		classSmali = left + all3Lines + "\n\n" + right;
		
		String localCount = classSmali.substring(classSmali.lastIndexOf(".locals "));
		localCount = localCount.substring(0, localCount.indexOf("\n"));
		int theCount = Integer.parseInt(localCount);
		if (theCount < 3) {
			left = classSmali.substring(classSmali.lastIndexOf(".locals " + localCount));
			right = classSmali.substring(classSmali.lastIndexOf(left) + left.length());
			right = right.replace(".locals " + theCount, ".locals 3");
			classSmali = left + right;
		}
		
		return classSmali;
	}
	
	public String addMethodReturn(String classSmali, String methodSig, String returnVName) {
		
		return classSmali;
	}
	
}
