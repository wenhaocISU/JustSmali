package smali;

import java.util.ArrayList;

import smali.stmt.ReturnStmt;

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
	
	public String addMethodReturn(String classSmali, String methodSig, ReturnStmt s) {
		
		// if method return v0 type is double, then v0 will occupy v0 and v1. therefore, can't just simply use v1 and v2 here.
		int outVNo = 0, stringVNo = 0;
		// 1.get returnVNo, find out what variable shouldn't be touched
		String returnVName = s.getvA();
		int returnVNo = 0;
		if (returnVName.startsWith("v"))
			returnVNo = Integer.parseInt(returnVName.replace("v", ""));
		ArrayList<Integer> toAvoid = new ArrayList<Integer>();
		toAvoid.add(returnVNo);
		if (s.getTheStmt().startsWith("return-wide"))
			toAvoid.add(returnVNo+1);
		
		// 2.see if there are 2 slots that can be used
		boolean found = false;
		String localCount = classSmali.substring(classSmali.lastIndexOf(".locals ") + ".locals ".length());
		localCount = localCount.substring(0, localCount.indexOf("\n"));
		int theCount = Integer.parseInt(localCount);
		for (int i = 0; i < theCount-1; i++) {
			if (!toAvoid.contains(i) && !toAvoid.contains(i+1)) {
				found = true;
				outVNo = i;
				stringVNo = i+1;
			}
		}

		// 3. if not found, then start from the 2nd slot of returned object
		if (!found) {
			outVNo = toAvoid.get(toAvoid.size()-1) + 1;
			stringVNo = outVNo + 1;
		}

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
	    
		if (stringVNo >= theCount) {
			left = classSmali.substring(0, classSmali.lastIndexOf(".locals "));
			right = classSmali.substring(classSmali.lastIndexOf(left) + left.length());
			right = right.replace(".locals " + theCount, ".locals " + stringVNo + 1);
			classSmali = left + right;
		}
		
		return classSmali;
	}
	
}
