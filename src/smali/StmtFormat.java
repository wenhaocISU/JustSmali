package smali;

import java.util.ArrayList;
import java.util.Arrays;

public class StmtFormat {
	
	private static ArrayList<String> ifFormat = new ArrayList<String>(Arrays.asList(
			"if-eq",
			"if-ne",
			"if-lt",
			"if-ge",
			"if-gt",
			"if-le",
			"if-eqz",
			"if-nez",
			"if-ltz",
			"if-gez",
			"if-gtz",
			"if-lez"
	));
	
	private static ArrayList<String> invokeFormat = new ArrayList<String>(Arrays.asList(
			"invoke-virtual",
			"invoke-super",
			"invoke-direct",
			"invoke-static",
			"invoke-interface",
			"invoke-virtual/range",
			"invoke-virtual/range",
			"invoke-super/range",
			"invoke-direct/range",
			"invoke-static/range",
			"invoke-interface/range"
	));
	
	private static ArrayList<String> getFieldFormat = new ArrayList<String>(Arrays.asList(
			"iget",
			"iget-wide",
			"iget-object",
			"iget-boolean",
			"iget-byte",
			"iget-char",
			"iget-short",
			"sget",
			"sget-wide",
			"sget-object",
			"sget-boolean",
			"sget-byte",
			"sget-char",
			"sget-short"
	));
	
	private static ArrayList<String> putFieldFormat = new ArrayList<String>(Arrays.asList(
			"iput",
			"iput-wide",
			"iput-object",
			"iput-boolean",
			"iput-byte",
			"iput-char",
			"iput-short",
			"sput",
			"sput-wide",
			"sput-object",
			"sput-boolean",
			"sput-byte",
			"sput-char",
			"sput-short"
	));
	
	private static ArrayList<String> moveFormat = new ArrayList<String>(Arrays.asList(
			"move",
			"move/from16",
			"move/16",
			"move-wide",
			"move-wide/from16",
			"move-wide/16",
			"move-object",
			"move-object/from16",
			"move-object/16"
	));
	
	private static ArrayList<String> moveResultFormat = new ArrayList<String>(Arrays.asList(
			"move-result",
			"move-result-wide",
			"move-result-object"
	));
	
	private static ArrayList<String> returnFormat = new ArrayList<String>(Arrays.asList(
			"return-void",
			"return",
			"return-wide",
			"return-object"
	));

	private static ArrayList<String> constFormat = new ArrayList<String>(Arrays.asList(
			"const/4",
			"const/16",
			"const",
			"const/high16",
			
			"const-wide/16",
			"const-wide/32",
			"const-wide",
			"const-wide/high16",
			
			"const-string",
			"const-string/jumbo",
			"const-class"
	));
	
	private static ArrayList<String> arrayPutFormat = new ArrayList<String>(Arrays.asList(
			"fill-array-data",
			"aput",
			"aput-wide",
			"aput-object",
			"aput-boolean",
			"aput-byte",
			"aput-char",
			"aput-short"
	));
	
	public static ArrayList<String> arrayGetFormat = new ArrayList<String>(Arrays.asList(
			"aget",
			"aget-wide",
			"aget-object",
			"aget-boolean",
			"aget-byte",
			"aget-char",
			"aget-short"
	));
	
	private static ArrayList<String> newFormat = new ArrayList<String>(Arrays.asList(
			"new-instance",
			"new-array",
			"filled-new-array",
			"filled-new-array/range"
	));
	
	private static ArrayList<String> gotoFormat = new ArrayList<String>(Arrays.asList(
			"goto",
			"goto/16",
			"goto/32"
	));
	
	private static String throwFormat = "throw";
	
	private static ArrayList<String> switchFormat = new ArrayList<String>(Arrays.asList(
			"packed-switch",
			"sparse-switch"
	));
	
	private static ArrayList<String> otherComputationFormat_2v = new ArrayList<String>(Arrays.asList(
			"instance-of",
			"array-length",
			
			"neg-int", "not-int",
			"neg-long", "not-long",
			"neg-float",
			"neg-double", "neg-double",
			
			"int-to-long", "int-to-float", "int-to-double",
			"long-to-int", "long-to-float", "long-to-double",
			"float-to-int", "float-to-long", "float-to-double",
			"double-to-int", "double-to-long", "double-to-float",
			"int-to-byte", "int-to-char", "int-to-short",
			
			"add-int/2addr", "sub-int/2addr", "mul-int/2addr", "dev-int/2addr", "rem-int/2addr",
			"and-int/2addr", "or-int/2addr", "xor-int/2addr", "shl-int/2addr", "shr-int/2addr", "ushr-int/2addr",
			"add-long/2addr", "sub-long/2addr", "mul-long/2addr", "dev-long/2addr", "rem-long/2addr",
			"and-long/2addr", "or-long/2addr", "xor-long/2addr", "shl-long/2addr", "shr-long/2addr", "ushr-long/2addr",
			"add-float/2addr", "sub-float/2addr", "mul-float/2addr", "dev-float/2addr", "rem-float/2addr",
			"add-double/2addr", "sub-double/2addr", "mul-double/2addr", "dev-double/2addr", "rem-double/2addr",
			"add-int/lit16", "sub-int/lit16", "mul-int/lit16", "dev-int/lit16", "rem-int/lit16",
			"and-int/lit16", "or-int/lit16", "xor-int/lit16",
			"add-int/lit8", "sub-int/lit8", "mul-int/lit8", "dev-int/lit8", "rem-int/lit8",
			"and-int/lit8", "or-int/lit8", "xor-int/lit8", "shl-int/lit8", "shr-int/lit8", "ushr-int/lit8"
	));
	
	private static ArrayList<String> otherComputationFormat_3v = new ArrayList<String>(Arrays.asList(
			"cmpl-float", "cmpg-float",
			"cmpl-double", "cmpg-double",
			"cmp-long",
			"add-int", "sub-int", "mul-int", "dev-int", "rem-int",
			"and-int", "or-int", "xor-int", "shl-int", "shr-int", "ushr-int",
			"add-long", "sub-long", "mul-long", "dev-long", "rem-long",
			"and-long", "or-long", "xor-long", "shl-long", "shr-long", "ushr-long",
			"add-float", "sub-float", "mul-float", "dev-float", "rem-float",
			"add-double", "sub-double", "mul-double", "dev-double", "rem-double"
	));
	
	private static String checkCastFormat = "check-cast";
	
	static ArrayList<String> otherFormat = new ArrayList<String>(Arrays.asList(
			"monitor-enter",
			"monitor-exit",
			"move-exception"
	));
	

	
	public static boolean isArrayPut(String l) {
		if (l.contains(" "))
			return arrayPutFormat.contains(l.split(" ")[0]);
		return false;
	}
	
	public static boolean isFillArray(String l) {
		if (l.contains(" "))
			return l.split(" ")[0].equals(arrayPutFormat.get(0));
		return false;
	}
	
	public static boolean isArrayGet(String l) {
		if (l.contains(" "))
			return arrayGetFormat.contains(l.split(" ")[0]);
		return false;
	}
	
	public static boolean isConst(String l) {
		for (String s : constFormat)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}
	
	public static boolean isMove(String l) {
		for (String s : moveFormat)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}
	
	public static boolean isNew(String l) {
		for (String s : newFormat)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}
	
	
	
	public static boolean isV2OP(String l) {
		for (String s : otherComputationFormat_2v)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}

	public static boolean isV3OP(String l) {
		for (String s : otherComputationFormat_3v)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}
	
	public static boolean isCheckCast(String l) {
		return (l.startsWith(checkCastFormat));
	}
	
	public static boolean isGoto(String l) {
		for (String s : gotoFormat)
			if (l.startsWith(s + " :"))
				return true;
		return false;
	}
	
	public static boolean isReturn(String l) {
		for (String s : returnFormat)
			if (l.startsWith(s))
				return true;
		return false;
	}
	
	public static boolean isInvoke(String l) {
		if (l.startsWith("invoke") && l.contains(" ")) {
			if (invokeFormat.contains(l.split(" ")[0]))
				return true;
		}
		return false;
	}
	
	public static boolean isMoveResult(String l) {
		for (String s : moveResultFormat)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}
	
	public static boolean isGetField(String l) {
		if (l.contains(" "))
			return getFieldFormat.contains(l.split(" ")[0]);
		return false;
	}
	

	public static boolean isPutField(String l) {
		if (l.contains(" "))
			return putFieldFormat.contains(l.split(" ")[0]);
		return false;
	}
	
	public static boolean isIfStmt(String l) {
		if (l.contains(" "))
			return ifFormat.contains(l.split(" ")[0]);
		return false;
	}
	
	public static boolean isSwitch(String l) {
		for (String s : switchFormat)
			if (l.startsWith(s + " "))
				return true;
		return false;
	}
	
	public static boolean isThrow(String l) {
		return (l.startsWith(throwFormat + " "));
	}
	

	
}
