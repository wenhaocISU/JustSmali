package smali;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import analysis.Expression;
import smali.stmt.ArrayStmt;
import smali.stmt.CheckCastStmt;
import smali.stmt.ConstStmt;
import smali.stmt.FieldStmt;
import smali.stmt.GotoStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.MoveStmt;
import smali.stmt.NewStmt;
import smali.stmt.ReturnStmt;
import smali.stmt.SwitchStmt;
import smali.stmt.ThrowStmt;
import smali.stmt.V2OPStmt;
import smali.stmt.V3OPStmt;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticField;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;

public class Parser {

	private static StaticApp staticApp;
	private static File smaliFolder;
	private static BufferedReader in;
	private static String classSmali;
	private static BlockLabel label;
	private static boolean normalLabelAlreadyUsed;
	private static ArrayList<String> oldLines = new ArrayList<String>();
	private static Instrumentation instr = new Instrumentation();
	
	public static void parseSmali(StaticApp theApp){
		
		staticApp = theApp;
		smaliFolder = new File(staticApp.outPath + "/apktool/smali/");
		System.out.println("parsing smali files...");
		for (File f : smaliFolder.listFiles())
			initClasses(f);
		try {
			parseFiles();
			smaliFolder.renameTo(new File(staticApp.outPath + "/apktool/originalSmali/"));
			File newSmali = new File(staticApp.outPath + "/apktool/newSmali");
			newSmali.renameTo(new File(staticApp.outPath + "/apktool/smali"));
		} catch (Exception e) {e.printStackTrace();}
	}
	
	private static void initClasses(File f) {
		if (f.isDirectory())
			for (File subF : f.listFiles())
				initClasses(subF);
		else if (f.isFile() && f.getName().endsWith(".smali")) {
			String className = f.getAbsolutePath();
			className = className.substring(
					className.indexOf(smaliFolder.getAbsolutePath()) + smaliFolder.getAbsolutePath().length()+1,
					className.lastIndexOf(".smali"));
			className = className.replace(File.separator, ".");
			final StaticClass c = new StaticClass();
			c.setJavaName(className);
			c.setInDEX(true);
			getLargestLineNumberAndMightAsWellGetOldLinesAndInitMethodBodies(f, c);
			staticApp.addClass(c);
		}
	}

	private static void parseFiles() throws Exception{
		int counter = 1, total = staticApp.getClasses().size();
		for (StaticClass c : staticApp.getClasses()) {
			File f = new File(staticApp.outPath + "/apktool/smali/" + c.getJavaName().replace(".", "/") + ".smali");
			System.out.println("(" + counter++ + "/" + total + ") " + c.getJavaName() + " ...");
			c = parseSmaliCode(f, c);
		}
	}
	
	private static int index = 0;
	private static int paramIndex = 0;
	
	private static StaticClass parseSmaliCode(File f, final StaticClass c) throws Exception{
		int largestLineNumber = c.largestOriginalSourceLineNumber + 1;
		oldLines = c.getOldLines();
		classSmali = "";
		index = 0;
		int maxLine = oldLines.size();
		String line;
		// 1. first line
		if (maxLine > 0) {
			line = oldLines.get(index++);
			classSmali = line + "\n";
			if (line.contains(" public "))		c.setPublic(true);
			if (line.contains(" interface "))	c.setInterface(true);
			if (line.contains(" final "))		c.setFinal(true);
			if (line.contains(" abstract "))	c.setAbstract(true);
			if (line.contains(" private "))		c.setPrivate(true);
			if (line.contains(" protected "))	c.setProtected(true);
		}
		// 2. before arriving method section
		while (index < maxLine) {
			line = oldLines.get(index++);
			classSmali += line + "\n";
			if (line.equals("# direct methods") || line.equals("# virtual methods"))
				break;
			parsePreMethodSection(c, line);
		}
		// 3. arrived method section
		while (index < maxLine) {
			line = oldLines.get(index++);
			classSmali += line + "\n";
			if (line.startsWith(".method ")) {
				int originalLineNumber = -1, stmtID = 0;
				paramIndex = 0;
				final StaticMethod m = c.getMethodBySubSig(line.substring(line.lastIndexOf(" ")+1));
				label = new BlockLabel();
				label.setNormalLabels(new ArrayList<String>(Arrays.asList(":main")));
				normalLabelAlreadyUsed = false;
				m.setSmaliCode(line+ "\n");
				while (!line.equals(".end method")  && index < oldLines.size()) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					m.setSmaliCode(m.getSmaliCode() + line + "\n");
					if (line.equals(""))	continue;
					if (line.contains(" "))
						line = line.trim();
					if (line.startsWith("#"))	continue;
					if (line.startsWith(".")) {
						if (line.startsWith(".line")) {
							originalLineNumber = Integer.parseInt(line.split(" ")[1]);
							if (m.getSourceLineNumbers().contains(originalLineNumber)) {
								originalLineNumber = -1;
								classSmali = classSmali.substring(0, classSmali.length()-1);
								classSmali = classSmali.substring(0, classSmali.lastIndexOf("\n")+1);
								String mSmali = m.getSmaliCode();
								mSmali = mSmali.substring(0, mSmali.length()-1);
								m.setSmaliCode(mSmali.substring(0, mSmali.lastIndexOf("\n")+1));
							}
							else m.addSourceLineNumber(originalLineNumber);
						}
						else {
							parseDots(m, line);
						}
					}
					else if (line.startsWith(":")){
						parseColons(m, line);
					}
					else {
						StaticStmt s = parseStmt(m, line);
						s.setStmtID(stmtID++);
						s.setTheStmt(line);
						s.setBlockLabel(label);
						normalLabelAlreadyUsed = true;
						if (originalLineNumber == -1) {
							s.setNewLineNumber(largestLineNumber++);
							String left = classSmali.substring(0, classSmali.lastIndexOf("\n\n")+2);
							String right = classSmali.substring(classSmali.lastIndexOf("\n\n")+2);
							classSmali = left + "    .line " + s.getSourceLineNumber() + "\n" + right;
							String methodSmali = m.getSmaliCode();
							left = methodSmali.substring(0, methodSmali.lastIndexOf("\n\n")+2);
							right = methodSmali.substring(methodSmali.lastIndexOf("\n\n")+2);
							methodSmali = left + "    .line " + s.getSourceLineNumber() + "\n" + right;
							m.setSmaliCode(methodSmali);
						}
						else {
							s.setOriginalLineNumber(originalLineNumber);
							originalLineNumber = -1;
						}
						if (!instr.classInBlackList(c.getDexName())) {
							if (s.getStmtID() == 0)
								classSmali = instr.addMethodStarting(classSmali, m.getSmaliSignature());
							if (s instanceof ReturnStmt) {
								ReturnStmt rS = (ReturnStmt) s;
								classSmali = instr.addMethodReturn(classSmali, m.getSmaliSignature(), rS);
							}
						}
						if (s instanceof SwitchStmt) {
							String vName = ((SwitchStmt) s).getSwitchV();
							if (!m.getvDebugInfo().containsKey(vName))
							{
								classSmali = classSmali.substring(0, classSmali.length()-1);
								String methodSmali = m.getSmaliCode();
								methodSmali = methodSmali.substring(0, methodSmali.length()-1);
								classSmali = classSmali.substring(0, classSmali.lastIndexOf("\n")+1);
								methodSmali = methodSmali.substring(0, methodSmali.lastIndexOf("\n")+1);
								classSmali += "    .local " + vName + ", \"wenhao" + vName + "\":I";
								methodSmali += "    .local " + vName + ", \"wenhao" + vName + "\":I";
								classSmali += "\n    " + line + "\n";
								methodSmali += "\n    " + line + "\n";
								m.setSmaliCode(methodSmali);
							}
						}
						m.addSourceLineNumber(s.getSourceLineNumber());
						if (s instanceof IfStmt)
							label.setNormalLabelSection(label.getNormalLabelSection()+1);
						else if (s instanceof ReturnStmt) {
							label = new BlockLabel();
							m.setReturnLineNumber(s.getSourceLineNumber());
						}
						else if (s instanceof GotoStmt || s instanceof SwitchStmt || s instanceof ThrowStmt)
							label = new BlockLabel();
						m.addSmaliStmt(s);
					}
				}
			}
		}
		File newF = new File(staticApp.outPath + "/apktool/newSmali/" + c.getJavaName().replace(".", "/") + ".smali");
		newF.getParentFile().mkdirs();
		try {
			PrintWriter out = new PrintWriter(new FileWriter(newF));
			out.write(classSmali);
			out.close();
		}	catch (Exception e) {e.printStackTrace();}
		c.setOldLines(new ArrayList<String>());
		return c;
	}
	
	private static StaticStmt parseStmt(StaticMethod m, String line) throws Exception{
		
		if (StmtFormat.isArrayGet(line) || StmtFormat.isArrayPut(line)) {
			ArrayStmt s = new ArrayStmt();
			s.setIsGet(StmtFormat.isArrayGet(line));
			s.setIsPut(StmtFormat.isArrayPut(line));
			s.setIsFill(StmtFormat.isFillArray(line));
			String arguments[] = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			if (!s.isFill()) {
				s.setvB(arguments[1]);
				s.setvC(arguments[2]);
			}
			return s;
		}
		if (StmtFormat.isCheckCast(line)) {
			CheckCastStmt s = new CheckCastStmt();
			String arguments[] = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(Grammar.parseParameters(arguments[1]).get(0));
			return s;
		}
		if (StmtFormat.isConst(line)) {
			ConstStmt s = new ConstStmt();
			String arguments[] = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(arguments[1]);			
			String left = s.getV();
			// TODO make #... as the root, and put the value to its child
			Expression right = new Expression("#number");
			if (line.startsWith("const-string"))
				right = new Expression("#string");
			if (line.startsWith("const-class"))
				right = new Expression("#class");
			right.add(new Expression(s.getValue()));
			Expression ex = new Expression("=");
			ex.add(new Expression(left));
			ex.add(right);
			s.setExpression(ex);
			return s;
		}
		if (StmtFormat.isGetField(line) || StmtFormat.isPutField(line)) {
			FieldStmt s = new FieldStmt();
			s.setIsGet(StmtFormat.isGetField(line));
			s.setIsPut(StmtFormat.isPutField(line));
			s.setGeneratesSymbol(s.isGet());
			String arguments[] = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(arguments[1]);
			if (line.startsWith("s"))	s.setStatic(true);
			else s.setvC(arguments[2]);
			String fieldSig = s.getvC();
			if (s.isStatic())	fieldSig = s.getvB();
			s.setFieldSig(fieldSig);
			String tgtCN = fieldSig.split("->")[0];
			String fSubSig = fieldSig.split("->")[1];
			StaticClass tgtC = staticApp.findClassByDexName(tgtCN);
			StaticField tgtF = null;
			if (tgtC != null) {
				tgtF = tgtC.getField(fSubSig.split(":")[0]);
				if (tgtF == null){
					tgtF = new StaticField();
					tgtF.setDexSubSig(fSubSig);
					tgtC.addField(tgtF);
				}
				tgtF.addInCallSourceSig(m.getSmaliSignature());
				m.addFieldRefSigs(tgtF.getDexSignature());
			}
			
			String fieldOp = "$Finstance";
			if (s.isStatic()) fieldOp = "$Fstatic";
			Expression fieldExpr = new Expression(fieldOp);
			fieldExpr.add(new Expression(s.getFieldSig()));
			if (!s.isStatic())
				fieldExpr.add(new Expression(s.getObject()));
			Expression vExpr = new Expression(s.getvA());
			Expression ex = new Expression("=");
			if (s.isPut()) {  ex.add(fieldExpr); ex.add(vExpr);}
			else 			{ ex.add(vExpr);	 ex.add(fieldExpr);}
			
			if (s.isGet() && tgtF != null && tgtF.isSynthetic() && tgtF.getName().startsWith("this$")) {
				Expression newRight = new Expression("$this");
				newRight.add(new Expression(tgtF.getType()));
				ex.remove(1);
				ex.insert(newRight, 1);
			}
			
			s.setExpression(ex);
			return s;
		}
		if (StmtFormat.isGoto(line)) {
			GotoStmt s = new GotoStmt();
			String tgtLabel = line.substring(line.indexOf(" ")+1);
			s.setTargetLabel(tgtLabel);
			s.setFlowsThrough(false);
			return s;
		}
		if (StmtFormat.isIfStmt(line)) {
			IfStmt s = new IfStmt();
			s.setHas1V(StmtFormat.is1VIf(line));
			s.setHas2V(StmtFormat.is2VIf(line));
			String arguments[] = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(arguments[1]);
			if (s.has2V())
				s.setvC(arguments[2]);
			s.setFlowsThrough(false);
			s.setUpdatesPathCondition(true);
			if (m.getSmaliStmts().size() > 0) {
				StaticStmt lastS = m.getSmaliStmts().get(m.getSmaliStmts().size()-1);
				if (lastS instanceof V3OPStmt && lastS.getTheStmt().startsWith("cmp")) {
					s.setFollowingCMPStmt(true);
					s.setCMPStmtLeft(lastS.getvB());
					s.setCMPStmtRight(lastS.getvC());
				}
			}
			return s;
		}
		if (StmtFormat.isInvoke(line)) {
			InvokeStmt s = new InvokeStmt();
			String invokeType = line.substring(0, line.indexOf(" "));
			s.setInvokeType(invokeType);
			String arguments = line.substring(line.indexOf(" ")+1);
			String param = arguments.substring(0, arguments.lastIndexOf(", "));
			param = param.substring(1, param.length()-1);
			s.setParams(param);
			String methodSig = arguments.substring(arguments.lastIndexOf(", ")+2);
			s.setTargetSig(methodSig);
			String tgtCN = methodSig.split("->")[0];
			StaticClass tgtC = staticApp.findClassByDexName(tgtCN);
			StaticMethod tgtM = staticApp.findMethod(methodSig);
			if (tgtC != null && tgtM != null) {
				tgtM.addInCallSourceSig(m.getSmaliSignature());
				m.addOutCallTargetSigs(tgtM.getSmaliSignature());
			}
			return s;
		}
		if (StmtFormat.isMove(line) || StmtFormat.isMoveResult(line)) {
			MoveStmt s = new MoveStmt();
			if (StmtFormat.isMoveResult(line)) {
				s.setvA(line.substring(line.indexOf(" ")+1));
				StaticStmt lastS = m.getSmaliStmts().get(m.getSmaliStmts().size()-1);
				if (lastS instanceof InvokeStmt) {
					((InvokeStmt) lastS).setResultsMoved(true);
					s.setGeneratesSymbol(true);
					Expression ex = new Expression("=");
					ex.add(new Expression(s.getvA()));
					ex.add(new Expression("$return"));
					s.setExpression(ex);
					s.setResultMovedFrom(m.getSmaliStmts().size()-1);
				}
				else if (lastS instanceof NewStmt) {
					if (((NewStmt) lastS).isNewArray())
						((NewStmt) lastS).setNewArrayMoved(true);
				}
				else throw (new Exception("something's wrong when parsing this MoveResult stmt..\n\t" + m.getSmaliSignature() + "\n"));
			}
			else {
				String[] arguments = line.substring(line.indexOf(" ")+1).split(", ");
				s.setvA(arguments[0]);
				s.setvB(arguments[1]);
				Expression ex = new Expression("=");
				ex.add(new Expression(s.getDestV()));
				ex.add(new Expression(s.getSourceV()));
				s.setExpression(ex);
			}
			return s;
		}
		if (StmtFormat.isNew(line)) {
			NewStmt s = new NewStmt();
			if (line.startsWith("new-instance "))
				s.setNewInstance(true);
			else s.setNewArray(true);
			s.setArguments(line.substring(line.indexOf(" ")+1));
			if (s.isNewInstance()) {
				String[] arguments = line.substring(line.indexOf(" ")+1).split(", ");
				Expression ex = new Expression("=");
				ex.add(new Expression(arguments[0]));
				//TODO make $new as the root, and type as its single child
				Expression right = new Expression("$new");
				right.add(new Expression(arguments[1]));
				ex.add(right);
				s.setExpression(ex);
			}
			return s;
		}
		if (StmtFormat.isReturn(line)) {
			ReturnStmt s = new ReturnStmt();
			s.setFlowsThrough(false);
			s.setEndsMethod(true);
			if (!s.returnsVoid())
				s.setvA(line.substring(line.indexOf(" ")+1));
			return s;
		}
		if (StmtFormat.isSwitch(line)) {
			SwitchStmt s = new SwitchStmt();
			String[] arguments = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setSwitchMapLabel(arguments[1]);
			s.setIsPswitch(line.startsWith("packed-switch"));
			s.setISSswitch(line.startsWith("sparse-switch"));
			s.setFlowsThrough(false);
			s.setUpdatesPathCondition(true);
			return s;
		}
		if (StmtFormat.isThrow(line)) {
			ThrowStmt s = new ThrowStmt();
			String vA = line.substring(line.indexOf(" ")+1);
			s.setvA(vA);
			s.setFlowsThrough(false);
			s.setEndsMethod(true);
			return s;
		}
		if (StmtFormat.isV2OP(line)) {
			V2OPStmt s = new V2OPStmt();
			String[] arguments = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(arguments[1]);
			if (arguments.length > 2) {
				s.setHas3rdConstArg(true);
				s.setvC("#" + arguments[2]);
			}
			if (line.startsWith("instance-of") || line.startsWith("array-length"))
				return s;
			Expression ex = new Expression("=");
			if (line.startsWith("int-to-") || line.startsWith("long-to-") ||
				line.startsWith("float-to-") || line.startsWith("double-to-"))
			{
				ex.add(new Expression(s.getvA()));
				ex.add(new Expression(s.getvB()));
			}
			else
			{
				String op = line.substring(0, line.indexOf(" "));
				op = op.split("-")[0];
				Expression left = new Expression(s.getvA());
				Expression right = new Expression(op);
				if (s.has3rdConstArg()) {
					// vA = vB + #C
					right.add(new Expression(s.getvB()));
					right.add(new Expression(s.getvC()));
				}
				else {
					// vA = vA + vB
					right.add(new Expression(s.getvA()));
					right.add(new Expression(s.getvB()));
				}
				ex.add(left);
				ex.add(right);
			}
			s.setExpression(ex);
			return s;
		}
		if (StmtFormat.isV3OP(line)) {
			V3OPStmt s = new V3OPStmt();
			String[] arguments = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(arguments[1]);
			s.setvC(arguments[2]);
			if (line.startsWith("cmp"))
				return s;
			Expression ex = new Expression("=");
			String op = line.substring(0, line.indexOf(" "));
			op = op.split("-")[0];
			Expression left = new Expression(s.getvA());
			Expression right = new Expression(op);
			right.add(new Expression(s.getvB()));
			right.add(new Expression(s.getvC()));
			ex.add(left);
			ex.add(right);
			s.setExpression(ex);
			return s;
		}
		StaticStmt s = new StaticStmt();
		return s;
	}

	private static void parseColons(StaticMethod m, String line) {
		
		if (line.startsWith(":array_")) {
			String aLabel = line;
			String tableContent = "";
			while (!line.equals(".end array-data") && index < oldLines.size()) {
				line = oldLines.get(index++);
				classSmali += line + "\n";
				m.setSmaliCode(m.getSmaliCode() + line + "\n");
				tableContent += line + "\n";
				if (line.contains(" "))
					line = line.trim();
			}
			for (StaticStmt s : m.getSmaliStmts()) {
				if (s instanceof ArrayStmt) {
					ArrayStmt aS = (ArrayStmt) s;
					if (aS.isFill() && aS.getFillTabelLabel().equals(aLabel)) {
						aS.setFillTableContent(tableContent);
					}
				}
			}
		}
		else if (line.startsWith(":sswitch_data_") || line.startsWith(":pswtich_data_")) {
			String sLabel = line;
			SwitchStmt ss = new SwitchStmt();
			Map<Integer, String> switchMap = new HashMap<Integer, String>();
			for (StaticStmt s : m.getSmaliStmts()) {
				if (s instanceof SwitchStmt && ((SwitchStmt) s).getSwitchMapLabel().equals(sLabel)) {
					ss = (SwitchStmt) s;
				}
			}
			while (!line.equals(".end sparse-switch") && !line.equals(".end packed-switch")  && index < oldLines.size()) {
				line = oldLines.get(index++);
				classSmali += line + "\n";
				m.setSmaliCode(m.getSmaliCode() + line + "\n");
				if (line.contains(" "))
					line = line.trim();
				if (ss.isPswitch()) {
					int psindex = 0;
					if (line.startsWith(".packed-switch"))
						ss.setpSwitchInitValue(line.split(" ")[1].replace("0x", ""));
					else if (line.startsWith(":")) {
						int initValue = Integer.parseInt(ss.getpSwitchInitValue(), 16);
						switchMap.put(initValue + psindex++, line);
					}
				}
				else if (ss.isSswitch()){
					if (line.contains(" -> ")) {
						String value = line.split(" -> ")[0].replace("0x", "");
						String tgtLabel = line.split(" -> ")[1];
						switchMap.put(Integer.parseInt(value, 16), tgtLabel);
					}
				}
			}
			ss.setSwitchMap(switchMap);
		}
		else if (line.startsWith(":try_start_")){
			label.addTryLabel(line);
		}
		else if (line.startsWith(":try_end_")) {
			ArrayList<String> newTL = label.getTryLabels();
			newTL.remove(line.replace("_end_", "_start_"));
			label.setTryLabels(newTL);
		}
		else {
			if (normalLabelAlreadyUsed) {
				ArrayList<String> newNL = new ArrayList<String>();
				newNL.add(line);
				label.setNormalLabels(newNL);
				label.setNormalLabelSection(0);
				normalLabelAlreadyUsed = false;
			} else {
				label.addNormalLabel(line);
			}
		}
	}

	private static StaticMethod initMethod(String line, StaticClass c) {
		String subSig = line.substring(line.lastIndexOf(" ")+1);
		String fullSig = c.getDexName() + "->" + subSig;
		StaticMethod m = new StaticMethod();
		m = new StaticMethod();
		m.setSmaliSignature(fullSig);
		String returnType = subSig.substring(subSig.indexOf(")")+1);
		m.setReturnType(Grammar.dexToJavaTypeName(returnType));
		String parameters = subSig.substring(subSig.indexOf("(") + 1, subSig.indexOf(")"));
		ArrayList<String> params = Grammar.parseParameters(parameters);
		m.setParameterTypes(params);
		m.setDeclaringClass(c.getDexName());
		if (line.contains(" public "))		m.setPublic(true);
		if (line.contains(" private "))		m.setPrivate(true);
		if (line.contains(" protected "))	m.setProtected(true);
		if (line.contains(" static "))		m.setStatic(true);
		if (line.contains(" final "))		m.setFinal(true);
		if (line.contains(" constructor ")) m.setConstructor(true);
		if (!m.isStatic())
			paramIndex = 1;
		return m;
	}
	
	private static void parseDots(StaticMethod m, String line) {
		//	1.2 - '.catch' 
		if (line.startsWith(".catch ")) {
			String range = line.substring(line.indexOf("{")+1, line.indexOf("}"));
			range = range.split(" .. ")[0];
			String tgtLabel = line.substring(line.lastIndexOf(" :")+1);
			String exceptionType = line.substring(line.indexOf(".catch ")+7, line.indexOf("; {"));
			for (StaticStmt s : m.getSmaliStmts()) {
				if (!s.getBlockLabel().getTryLabels().contains(range))
					continue;
				s.setHasCatch(true);
				s.setCatchTgtLabel(tgtLabel);
				s.setExceptionType(exceptionType);
			}
		}
		// 1.3 - '.catchall'
		else if (line.startsWith(".catchall ")) {
			String range = line.substring(line.indexOf("{")+1, line.indexOf("}"));
			range = range.split(" .. ")[0];
			String tgtLabel = line.substring(line.lastIndexOf(" :")+1);
			for (StaticStmt s : m.getSmaliStmts()) {
				if (!s.getBlockLabel().getTryLabels().contains(range))
					continue;
				s.setHasFinally(true);
				s.setFinallyTgtLabel(tgtLabel);
			}
		}
		// 1.4 - '.locals'
		else if (line.startsWith(".locals ")) {
			int vCount = Integer.parseInt(line.split(" ")[1]);
			m.setLocalVariableCount(vCount);
		}
		// 1.5 - '.annotation'
		else if (line.startsWith(".annotation")) {
			while (!line.equals(".end annotation")  && index < oldLines.size()) {
				line = oldLines.get(index++);
				classSmali += line + "\n";
				m.setSmaliCode(m.getSmaliCode() + line + "\n");
				if (line.contains(" "))
					line = line.trim();
			}
		}
		else if (line.startsWith(".local ")) {
			// .local v0, a:I
			String names = line.substring(line.indexOf(".local ") + ".local ".length());
			String localName = names.substring(0, names.indexOf(", "));
			String debugName = names.substring(names.indexOf(", ")+2, names.indexOf(":"));
			m.addvDebugInfo(localName, debugName);
		}
		else if (line.startsWith(".parameter")) {
			//.parameter "v"
			String localName = "p" + paramIndex++;
			if (line.contains("\"")) {
				String debugName = line.substring(line.indexOf(".parameter ") + ".parameter ".length());
				debugName = debugName.replace("\"", "");
				m.addvDebugInfo(localName, debugName);
			}
		}
		else if (line.startsWith(".param ") && line.contains("\"")) {
			// .param p1, a:I
			String names = line.substring(line.indexOf(".param ") + ".param ".length());
			String localName = names.substring(0, names.indexOf(", "));
			String debugName = names.substring(names.indexOf(", ")+2, names.lastIndexOf("\""));
			m.addvDebugInfo(localName, debugName);
		}
	}
	
	
	private static void parsePreMethodSection(StaticClass c, String line){
		if (line.startsWith(".super ")) {
			String superClassName = line.substring(line.lastIndexOf(" ")+1);
			c.setSuperClass(Grammar.dexToJavaTypeName(superClassName));
		}
		else if (line.startsWith(".source \"")) {
			String sourceName = line.substring(line.lastIndexOf(".source ")+8).replace("\"", "");
			c.setSourceFileName(sourceName);
		}
		else if (line.startsWith(".implements ")) {
			String interfaceName = line.substring(line.indexOf(".implements ")+12);
			c.addInterface(interfaceName);
		}
		else if (line.startsWith(".annotation ")) {
			if (line.equals(".annotation system Ldalvik/annotation/MemberClasses;")) {
				while (!line.equals(".end annotation")  && index < oldLines.size()) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					if (line.startsWith("        ")) {
						String innerCN = line.trim();
						if (innerCN.endsWith(","))
							innerCN = innerCN.substring(0, innerCN.length()-1);
						c.addInnerClass(Grammar.dexToJavaTypeName(innerCN));
					}
				}
			}
			else if (line.equals(".annotation system Ldalvik/annotation/EnclosingMethod;")) {
				while (!line.equals(".end annotation")  && index < oldLines.size()) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					if (line.startsWith("    value = ")) {
						String mSig = line.substring(line.lastIndexOf(" = ")+3);
						String dexC = mSig.split("->")[0];
						StaticClass outerC = staticApp.findClassByDexName(dexC);
						if (outerC != null) {
							outerC.addInnerClass(c.getJavaName());
							c.setOuterClass(outerC.getJavaName());
						}
					}
				}
			}
			else if (line.equals(".annotation system Ldalvik/annotation/EnclosingClass;")) {
				while (!line.equals(".end annotation")  && index < oldLines.size()) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					if (line.startsWith("    value = ")) {
						String dexC = line.substring(line.lastIndexOf(" = ")+3);
						StaticClass outerC = staticApp.findClassByDexName(dexC);
						if (outerC != null) {
							outerC.addInnerClass(c.getJavaName());
							c.setOuterClass(outerC.getJavaName());
						}
					}
				}
			}
			else if (line.equals(".annotation system Ldalvik/annotation/InnerClass;")) {
				c.setInnerClass(true);
			}
		}
		else if (line.startsWith(".field ")) {
			String subSig = line.substring(line.lastIndexOf(" ")+1);
			String initValue = "";
			if (line.contains(" = ")) {
				subSig = line.split(" = ")[0];
				subSig = subSig.substring(subSig.lastIndexOf(" ")+1);
				initValue = line.split(" = ")[1];
			}
			String name = subSig.split(":")[0];
			String dexType = subSig.split(":")[1];
			StaticField f = c.getField(name);
			if (f == null) {
				f = new StaticField();
				f.setDexSubSig(subSig);
			}
			f.setDeclaringClassName(c.getDexName());
			if (line.contains(" public "))		f.setPublic(true);
			if (line.contains(" private "))		f.setPrivate(true);
			if (line.contains(" protected "))	f.setProtected(true);
			if (line.contains(" final ")) 		f.setFinal(true);
			if (line.contains(" static "))	 	f.setStatic(true);
			if (line.contains(" synthetic "))	f.setIsSynthetic(true);
			f.setInitValue(initValue);
			f.setType(Grammar.dexToJavaTypeName(dexType));
			f.setDeclaration(line);
			c.addField(f);
		}
	}
	
	private static void getLargestLineNumberAndMightAsWellGetOldLinesAndInitMethodBodies(File f, StaticClass c) {
		try {
			in = new BufferedReader(new FileReader(f));
			String line;
			oldLines = new ArrayList<String>();
			while ((line = in.readLine())!=null) {
				oldLines.add(line);
				if (line.startsWith("    .line ")) {
					int current = Integer.parseInt(line.substring(line.lastIndexOf(" ")+1));
					if (c.largestOriginalSourceLineNumber < current)
						c.largestOriginalSourceLineNumber = current;
				}
				else if (line.startsWith(".method ")) {
					StaticMethod m = initMethod(line, c);
					c.addMethod(m);
				}
			}
			c.setOldLines(oldLines);
		}	catch (Exception e) {e.printStackTrace();}
	}

	
}
