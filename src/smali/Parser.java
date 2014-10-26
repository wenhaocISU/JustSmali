package smali;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
	
	public static void parseSmali(StaticApp theApp) {
		
		staticApp = theApp;
		smaliFolder = new File(staticApp.outPath + "/apktool/smali/");
		System.out.println("parsing smali files...");
		for (File f : smaliFolder.listFiles())
			parseFile(f);
		File original = new File(staticApp.outPath + "/apktool/smali/");
		File instrumented = new File(staticApp.outPath + "/apktool/newSmali/");
		System.out.println("\nmoving original smali files into /apktool/oldSmali/...");
		original.renameTo(new File(staticApp.outPath + "/apktool/oldSmali/"));
		System.out.println("\nmoving instrumented smali files into /apktool/smali/...");
		instrumented.renameTo(new File(staticApp.outPath + "/apktool/smali/"));
	}
	
	private static void parseFile(File f) {
		if (f.isDirectory())
			for (File subF : f.listFiles())
				parseFile(subF);
		else if (f.isFile() && f.getName().endsWith(".smali")) {
			String className = f.getAbsolutePath();
			className = className.substring(
					className.indexOf(smaliFolder.getAbsolutePath()) + smaliFolder.getAbsolutePath().length()+1,
					className.lastIndexOf(".smali"));
			className = className.replace(File.separator, ".");
			StaticClass c = staticApp.findClassByJavaName(className);
			if (c == null)
				c = new StaticClass();
			c.setInDEX(true);
			c.setJavaName(className);
			c = parseSmaliCode(f, c);
			staticApp.addClass(c);
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
			int originalLineNumber = -1;
			
			line = oldLines.get(index++);
			classSmali += line + "\n";
			if (line.startsWith(".method ")) {
				List<LocalVariable> vList = new ArrayList<LocalVariable>();
				List<LocalVariable> pList = new ArrayList<LocalVariable>();

				String methodSmali = line + "\n";
				final StaticMethod m = initMethod(line, c);
				int pIndex = 0;
				if (!m.isStatic()) {
					pIndex = 1;
					LocalVariable pV = new LocalVariable();
					pV.setDexName("p0");
					pV.setType(c.getJavaName());
					pList.add(pV);
				}
				for (int i = 0, len = m.getParameterTypes().size(); i < len; i++) {
					LocalVariable pV = new LocalVariable();
					pV.setDexName("p" + i);
				}
				while (!line.equals(".end method")) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					methodSmali += line + "\n";
					if (line.equals(""))	continue;
					if (line.contains(" "))	line = line.trim();
					if (line.startsWith("#"))	continue;
					/**
						.locals
						.end local
						.parameter
						.end parameter
						.prologue
						.line
						.end method
						.annotation
						.end annotation
						.local
						.catchall
						.restart local
						.sparse-switch
						.end sparse-switch
						.array-data
						.end array-data
						.catch
						.packed-switch
						.end packed-switch
					* */
					// Section 1 - dots
					if (line.startsWith(".")) {
						//	1.1 - '.line'
						if (line.startsWith(".line")) {
							originalLineNumber = Integer.parseInt(line.split(" ")[1]);
							m.addSourceLineNumber(originalLineNumber);
						}
						//	1.2 - '.catch' 
						else if (line.startsWith(".catch ")) {
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
							for (int i = 0; i < vCount; i++) {
								LocalVariable lV = new LocalVariable();
								lV.setDexName("v" + i);
								vList.add(lV);
							}
						}
						// 1.5 - '.local'
						else if (line.startsWith(".local ")) {
							//String 
							
						}
					}
					// Section 2 - colon
					else if (line.startsWith(":")){
						
					}
					// Section 3 - smali code
					else {
						
					}
				}
				m.setSmaliCode(methodSmali);
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
				while (!line.equals(".end annotation")) {
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
				while (!line.equals(".end annotation")) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					if (line.startsWith("    value = ")) {
						String mSig = line.substring(line.lastIndexOf(" = ")+3);
						String dexC = mSig.split("->")[0];
						StaticClass outerC = staticApp.findClassByDexName(dexC);
						if (outerC == null) {
							outerC = new StaticClass();
							outerC.setJavaName(Grammar.dexToJavaTypeName(dexC));
						}
						outerC.addInnerClass(c.getJavaName());
						c.setOuterClass(outerC.getJavaName());
					}
				}
			}
			else if (line.equals(".annotation system Ldalvik/annotation/EnclosingClass;")) {
				while (!line.equals(".end annotation")) {
					line = oldLines.get(index++);
					classSmali += line + "\n";
					if (line.startsWith("    value = ")) {
						String dexC = line.substring(line.lastIndexOf(" = ")+3);
						StaticClass outerC = staticApp.findClassByDexName(dexC);
						if (outerC == null) {
							outerC = new StaticClass();
							outerC.setJavaName(Grammar.dexToJavaTypeName(dexC));
						}
						outerC.addInnerClass(c.getJavaName());
						c.setOuterClass(outerC.getJavaName());
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
			if (line.contains(" public "))		f.setPublic(true);
			if (line.contains(" private "))		f.setPrivate(true);
			if (line.contains(" protected "))	f.setProtected(true);
			if (line.contains(" final ")) 		f.setFinal(true);
			if (line.contains(" static "))	 	f.setStatic(true);
			f.setInitValue(initValue);
			f.setType(Grammar.dexToJavaTypeName(dexType));
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
