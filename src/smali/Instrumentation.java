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
		
		return classSmali;
	}
	
	public String addMethodReturn(String classSmali, String methodSig, String returnVName) {
		
		return classSmali;
	}
	
}
