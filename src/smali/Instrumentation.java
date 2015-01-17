package smali;

public class Instrumentation {

	
	public String addMethodStarting(String classSmali, String methodSig) {
		
		String line1 = "    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;";
		String line2head = "    const-string v2, ";
		String line3 = "    invoke-virtual {v1, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V";
		String line2 = line2head + "\"METHOD_STARTING," + methodSig + "\"";
	    String all3Lines = line1 + "\n" + line2 + "\n" + line3;
	    
		String left = classSmali.substring(0, classSmali.lastIndexOf("\n\n")+2);
		String right = classSmali.substring(classSmali.lastIndexOf("\n\n")+2);
		
		if (right.contains("    .prologue\n")) {
			right = right.replace("    .prologue\n", "");
			all3Lines = "    .prologue\n" + all3Lines;
		}
		
		classSmali = left + all3Lines + "\n\n" + right;
		
		String localCount = classSmali.substring(classSmali.lastIndexOf(".locals ") + ".locals ".length());
		localCount = localCount.substring(0, localCount.indexOf("\n"));
		int theCount = Integer.parseInt(localCount);
		if (theCount < 3) {
			left = classSmali.substring(0, classSmali.lastIndexOf(".locals "));
			right = classSmali.substring(classSmali.lastIndexOf(left) + left.length());
			right = right.replace(".locals " + theCount, ".locals 3");
			classSmali = left + right;
		}
		
		return classSmali;
	}
	
	public String addMethodReturn(String classSmali, String methodSig, String returnVName) {
		
		int outVNo = 1;
		int stringVNo = 2;
		int returnVNo = 0;
		
		if (returnVName.startsWith("v"))
			returnVNo = Integer.parseInt(returnVName.replace("v", ""));
		if (returnVNo == 1)
			outVNo = 0;
		else if (returnVNo == 2)
			stringVNo = 0;
		
		String outPrintV = "v" + outVNo;
		String stringV = "v" + stringVNo;
		
		String line1 = "    sget-object " + outPrintV + ", Ljava/lang/System;->out:Ljava/io/PrintStream;";
		String line2head = "    const-string " + stringV + ", ";
		String line3 = "    invoke-virtual {" + outPrintV + ", " + stringV + "}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V";
		
		String line2 = line2head + "\"METHOD_RETURNING," + methodSig + "\"";
		
	    String all3Lines = line1 + "\n" + line2 + "\n" + line3;
		
		String left = classSmali.substring(0, classSmali.lastIndexOf("\n\n")+2);
		String right = classSmali.substring(classSmali.lastIndexOf("\n\n")+2);
		
		classSmali = left + all3Lines + "\n\n" + right;
	    
		return classSmali;
	}
	
}
