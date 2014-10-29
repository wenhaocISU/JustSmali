package smali;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import smali.stmt.ArrayStmt;
import smali.stmt.CheckCastStmt;
import smali.stmt.ConstStmt;
import smali.stmt.FieldStmt;
import smali.stmt.GotoStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.SwitchStmt;
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
	private static ArrayList<String> oldLines = new ArrayList<String>();
	private static BlockLabel label;
	private static boolean normalLabelAlreadyUsed;
	
	public static void parseSmali(StaticApp theApp) {
		
		staticApp = theApp;
		smaliFolder = new File(staticApp.outPath + "/apktool/smali/");
		System.out.println("parsing smali files...");
		for (File f : smaliFolder.listFiles())
			initClasses(f);
		parseFiles();
		File original = new File(staticApp.outPath + "/apktool/smali/");
		File instrumented = new File(staticApp.outPath + "/apktool/newSmali/");
		System.out.println("\nmoving original smali files into /apktool/oldSmali/...");
		original.renameTo(new File(staticApp.outPath + "/apktool/oldSmali/"));
		System.out.println("\nmoving instrumented smali files into /apktool/smali/...");
		instrumented.renameTo(new File(staticApp.outPath + "/apktool/smali/"));
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
			StaticClass c = new StaticClass();
			c.setJavaName(className);
			c.setInDEX(true);
			staticApp.addClass(c);
		}
	}

	private static void parseFiles() {
		for (StaticClass c : staticApp.getClasses()) {
			File f = new File(staticApp.outPath + "/apktool/smali/" + c.getJavaName().replace(".", "/") + ".smali");
			c = parseSmaliCode(f, c);
		}
	}
	
	private static int index = 0;
	
	private static StaticClass parseSmaliCode(File f, final StaticClass c) {
		int largestLineNumber = getLargestLineNumberAndMightAsWellGetOldLines(f);
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
			int originalLineNumber = -1;
			classSmali += line + "\n";
			if (line.startsWith(".method ")) {
				final StaticMethod m = initMethod(line, c);
				label = new BlockLabel();
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
							m.addSourceLineNumber(originalLineNumber);
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
						s.setTheStmt(line);
						s.setBlockLabel(label);
						m.addSmaliStmt(s);
					}
				}
				c.addMethod(m);
			}
		}
		File newF = new File(staticApp.outPath + "/apktool/newSmali/" + c.getJavaName().replace(".", "/") + ".smali");
		newF.getParentFile().mkdirs();
		try {
			PrintWriter out = new PrintWriter(new FileWriter(newF));
			out.write(classSmali);
			out.close();
		}	catch (Exception e) {e.printStackTrace();}
		return c;
	}
	
	private static StaticStmt parseStmt(StaticMethod m, String line) {
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
			return s;
		}
		if (StmtFormat.isGetField(line) || StmtFormat.isPutField(line)) {
			FieldStmt s = new FieldStmt();
			s.setIsGet(StmtFormat.isGetField(line));
			s.setIsPut(StmtFormat.isPutField(line));
			String arguments[] = line.substring(line.indexOf(" ")+1).split(", ");
			s.setvA(arguments[0]);
			s.setvB(arguments[1]);
			if (line.startsWith("s"))	s.setStatic(true);
			else s.setvC(arguments[2]);
			String fieldSig = s.getvC();
			if (s.isStatic())	fieldSig = s.getvB();
			String tgtCN = fieldSig.split("->")[0];
			String fSubSig = fieldSig.split("->")[1];
			StaticClass tgtC = staticApp.findClassByDexName(tgtCN);
			if (tgtC != null) {
				StaticField tgtF = tgtC.getField(fSubSig.split(":")[0]);
				if (tgtF == null){
					tgtF = new StaticField();
					tgtF.setDexSubSig(fSubSig);
					tgtC.addField(tgtF);
				}
				tgtF.addInCallSourceSig(m.getSmaliSignature());
				m.addFieldRefSigs(tgtF.getDexSignature());
			}
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
			return s;
		}
		if (StmtFormat.isInvoke(line)) {
			InvokeStmt s = new InvokeStmt();
			String arguments = line.substring(line.indexOf(" ")+1);
			String param = arguments.substring(0, arguments.lastIndexOf(", "));
			param = param.substring(1, param.length()-1);
			String methodSig = arguments.substring(arguments.lastIndexOf(", ")+2);
			String tgtCN = methodSig.split("->")[0];
			StaticClass tgtC = staticApp.findClassByDexName(tgtCN);
			if (tgtC != null) {
				StaticMethod tgtM = tgtC.getMethod(methodSig);
				if (tgtM == null) {
					tgtM = new StaticMethod();
					tgtM.setSmaliSignature(methodSig);
					tgtC.addMethod(tgtM);
				}
				tgtM.addInCallSourceSig(m.getSmaliSignature());
				m.addOutCallTargetSigs(tgtM.getSmaliSignature());
			}
			return s;
		}
		if (StmtFormat.isMove(line)) {
			
		}
		if (StmtFormat.isMoveResult(line)) {
			
		}
		if (StmtFormat.isNew(line)) {
			
		}
		if (StmtFormat.isReturn(line)) {
			
		}
		if (StmtFormat.isSwitch(line)) {
			
		}
		if (StmtFormat.isThrow(line)) {
			
		}
		if (StmtFormat.isV2OP(line)) {
			
		}
		if (StmtFormat.isV3OP(line)) {
			
		}
		StaticStmt s = new StaticStmt();
		return s;
	}

	private static void parseColons(StaticMethod m, String line) {
		
		if (line.startsWith(":array_")) {
			String aLabel = line;
			String tableContent = "";
			while (!line.equals(".end array_data") && index < oldLines.size()) {
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
			Map<String, String> switchMap = new HashMap<String, String>();
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
						ss.setpSwitchInitValue(line.split(" ")[1]);
					else if (line.startsWith(":")) {
						switchMap.put(""+psindex++, line);
					}
				}
				else if (ss.isSswitch()){
					if (line.contains(" -> ")) {
						String value = line.split(" -> ")[0];
						String tgtLabel = line.split(" -> ")[1];
						switchMap.put(value, tgtLabel);
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
		StaticMethod m = c.getMethod(fullSig);
		if (m == null) {
			m = new StaticMethod();
			m.setSmaliSignature(fullSig);
		}
		String returnType = subSig.substring(subSig.indexOf(")")+1);
		m.setReturnType(Grammar.dexToJavaTypeName(returnType));
		String parameters = subSig.substring(subSig.indexOf("(") + 1, subSig.indexOf(")"));
		ArrayList<String> params = Grammar.parseParameters(parameters);
		m.setParameterTypes(params);
		m.setDeclaringClass(c.getJavaName());
		if (line.contains(" public "))		m.setPublic(true);
		if (line.contains(" private "))		m.setPrivate(true);
		if (line.contains(" protected "))	m.setProtected(true);
		if (line.contains(" static "))		m.setStatic(true);
		if (line.contains(" final "))		m.setFinal(true);
		if (line.contains(" constructor ")) m.setConstructor(true);
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
			f.setInitValue(initValue);
			f.setType(Grammar.dexToJavaTypeName(dexType));
			c.addField(f);
		}
	}
	
	private static int getLargestLineNumberAndMightAsWellGetOldLines(File f) {
		int result = 0;
		try {
			in = new BufferedReader(new FileReader(f));
			String line;
			oldLines = new ArrayList<String>();
			while ((line = in.readLine())!=null) {
				oldLines.add(line);
				if (!line.startsWith("    .line "))
					continue;
				int current = Integer.parseInt(line.substring(line.lastIndexOf(" ")+1));
				if (result < current)
					result = current;
			}
		}	catch (Exception e) {e.printStackTrace();}
		return result;
	}

	
}
