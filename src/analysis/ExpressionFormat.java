package analysis;

import java.util.ArrayList;
import java.util.Arrays;

public class ExpressionFormat {

	public static ArrayList<String> conditionOPs = new ArrayList<String>(Arrays.asList(
			"==", "!=",
			">=", ">",
			"<=", "<"
	));
	
	public static ArrayList<String> calculationOPs = new ArrayList<String>(Arrays.asList(
			"add", "sub", "mul", "dev", "rem", "and", "or", "xor", "shl", "shr", "ushr"
	));
	
}
