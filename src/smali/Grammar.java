package smali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Grammar {

	@SuppressWarnings("serial")
	public static Map<String, String> primitiveTypes = new HashMap<String, String>(){{
		put("V", "void");
		put("Z", "boolean");
		put("B", "byte");
		put("S", "short");
		put("C", "char");
		put("I", "int");
		put("J", "long");
		put("F", "float");
		put("D", "double");
	}};
	

	public static final String[] poundHeads = {
		"# interfaces",
		"# annotations",
		"# instance fields",
		"# static fields",
		"# direct methods",
		"# virtual methods",
		// these 3 probably doesn't matter
		"#calls:",
		"#getter for:",
		"#setter for:",
	};
	
	public static final String[] instructions_that_jumps = {
		"if-",				// if-eqz v0, :cond_0
		
		"goto",				// goto :goto_0
		
		".catch",			// .catch EXCEPTION/TYPE {:try_start_0 .. :try_end_0} :catch_0
		".catchall",		// .catchall {:try_start_0 .. :try_end_0} :catchall_0
		
		"sparse-switch",	// sparse-switch p1, :sswitch_data_0
							// ......
							//  :sswitch_data_0
							//	.sparse-switch
							//	0x1389 -> :sswitch_1
							//	0x138a -> :sswitch_2
							//  .end sparse-switch
		
		"packed-switch",	// packed-switch v2, :pswitch_data_0
							// ......
							//  :pswitch_data_0
							//  .packed-switch 0x1
							//  :pswitch_0
							//  :pswitch_1
							//  .end packed-switch
		
		"fill-array-data",	// this one is special, in the code there's no jumping back
							// and the :array_0 block is usually put at the end, even after return stmt
							// so they must returned implicitly
							// fill-array-data v0, :array_0
							// ......
							//	:array_0
							//	.array-data 0x4
							//	    0x7t 0x0t 0x0t 0x0t
							//	    0x4t 0x0t 0x0t 0x0t
							//	    0x2t 0x0t 0x0t 0x0t
							//	    0x1t 0x0t 0x0t 0x0t
							//	    0xbt 0x0t 0x0t 0x0t
							//	.end array-data
	};
	
	public static final String[] labelHeads = {
		":cond",
		":goto",
		":catch", ":catchall", ":try_start", ":try_end", // can ignore
		":array",										 // can ignore
		":sswitch_data", ":sswitch",
		":pswitch_data", ":pswitch",
	};
	
	public static final String[] dotHeads = {
		".class", ".super", ".source", ".implements", ".enum", 
		".annotation", ".end annotation", ".subannotation", ".end subannotation", 
		".field", ".end field",
		".method", ".end method", ".prologue", ".line", 
		".locals", ".local", ".end local", 
		".parameter", ".end parameter", 
		".catchall", ".catch", 
		".restart local",
		".sparse-switch", ".end sparse-switch", 
		".packed-switch", ".end packed-switch", 
		".array-data", ".end array-data", 
	};
	
	public static String dexToJavaTypeName(String dex) {
		if (dex.startsWith("L") && dex.endsWith(";"))
			return dex.substring(1, dex.length()-1).replace("/", ".");
		else if (primitiveTypes.containsKey(dex))
			return primitiveTypes.get(dex);
		return "";
	}

	public static String javaToDexClassName(String java) {
		switch (java) {
			case "V":	return "void";
			case "Z":	return "boolean";
			case "B":	return "byte";
			case "S":	return "short";
			case "C":	return "char";
			case "I":	return "int";
			case "J":	return "long";
			case "F":	return "float";
			case "D":	return "double";
			default:	return "L" + java.replace(".", "/") + ";";
		}
	}
	
	public static ArrayList<String> parseParameters(String p) {
		ArrayList<String> result = new ArrayList<String>();
		int index = 0;
		while (index < p.length()) {
			char c = p.charAt(index++);
			if (c == 'L') {
				String l = "" + c;
				if (c == 'L') {
					while (c != ';') {
						c = p.charAt(index++);
						l += c;
					}
				}
				result.add(dexToJavaTypeName(l));
			}
			else if (c == '[') {
				int dimension = 0;
				while (c == '[') {
					c = p.charAt(index++);
					dimension++;
				}
				String a = "" + c;
				if (c == 'L') {
					while (c != ';') {
						c = p.charAt(index++);
						a += c;
					}
					a = dexToJavaTypeName(a);
				}
				else switch (c) {
					case 'V':	a = "void";		break;
					case 'Z':	a = "boolean";	break;
					case 'B':	a = "byte";		break;
					case 'S':	a = "short";	break;
					case 'C':	a = "char";		break;
					case 'I':	a = "int";		break;
					case 'J':	a = "long";		break;
					case 'F':	a = "float";	break;
					case 'D':	a = "double";	break;
				}
				for (int i = 0; i < dimension; i++)
					a += "[]";
				result.add(a);
			}
			else {
				switch (c) {
					case 'V':	result.add("void");		break;
					case 'Z':	result.add("boolean");	break;
					case 'B':	result.add("byte");		break;
					case 'S':	result.add("short");	break;
					case 'C':	result.add("char");		break;
					case 'I':	result.add("int");		break;
					case 'J':	result.add("long");		break;
					case 'F':	result.add("float");	break;
					case 'D':	result.add("double");	break;
				}
			}
		}
		return result;
	}
}
