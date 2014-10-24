package smali;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticField;
import staticFamily.StaticMethod;

public class Parser {

	private static StaticApp staticApp;
	private static File smaliFolder;
	private static BufferedReader in;
	private static String classSmali;
	
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
	
	private static StaticClass parseSmaliCode(File f, final StaticClass c) {
		int largestLineNumber = getLargestLineNumber(f);
		classSmali = "";
		try {
			in = new BufferedReader(new FileReader(f));
			String line;
			// first line
			if ((line = in.readLine())!=null) {
				classSmali = line + "\n";
				if (line.contains(" public "))		c.setPublic(true);
				if (line.contains(" interface "))	c.setInterface(true);
				if (line.contains(" final "))		c.setFinal(true);
				if (line.contains(" abstract "))	c.setAbstract(true);
			}
			// before arriving method section
			while ((line = in.readLine())!=null) {
				classSmali += line + "\n";
				if (line.equals("# direct methods") || line.equals("# virtual methods"))
					break;
				parsePreMethodSection(c, line);
			}
			// arrived method section
			while ((line = in.readLine())!=null) {
				classSmali += line + "\n";
				//TODO when met constructor method, if class is not public
				// check if it's private or protected.
				if (line.startsWith(".method ")) {
					StaticMethod m = new StaticMethod();
					m.setDeclaringClass(c.getJavaName());
					
					while (!line.equals(".end method")) {
						line = in.readLine();
						classSmali += line + "\n";
						
					}
					c.addMethod(m);
				}
			}
			in.close();
			File newF = new File(staticApp.outPath + "/apktool/newSmali/" + c.getJavaName().replace(".", "/") + ".smali");
			newF.getParentFile().mkdirs();
			PrintWriter out = new PrintWriter(new FileWriter(newF));
			out.write(classSmali);
			out.close();
		}	catch (Exception e) {e.printStackTrace();}
		return c;
	}
	
	private static void parsePreMethodSection(StaticClass c, String line) throws Exception{
		if (line.startsWith(".super ")) {
			String superClassName = line.substring(line.lastIndexOf(" ")+1);
			c.setSuperClass(Grammar.dexToJavaClassName(superClassName));
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
					line = in.readLine();
					classSmali += line + "\n";
					if (line.startsWith("        ")) {
						String innerCN = line.trim();
						if (innerCN.endsWith(","))
							innerCN = innerCN.substring(0, innerCN.length()-1);
						c.addInnerClass(Grammar.dexToJavaClassName(innerCN));
					}
				}
			}
			else if (line.equals(".annotation system Ldalvik/annotation/EnclosingMethod;")) {
				while (!line.equals(".end annotation")) {
					line = in.readLine();
					classSmali += line + "\n";
					if (line.startsWith("    value = ")) {
						String mSig = line.substring(line.lastIndexOf(" = ")+3);
						String dexC = mSig.split("->")[0];
						StaticClass outerC = staticApp.findClassByDexName(dexC);
						if (outerC == null) {
							outerC = new StaticClass();
							outerC.setJavaName(Grammar.dexToJavaClassName(dexC));
						}
						outerC.addInnerClass(c.getJavaName());
						c.setOuterClass(outerC.getJavaName());
					}
				}
			}
			else if (line.equals(".annotation system Ldalvik/annotation/EnclosingClass;")) {
				while (!line.equals(".end annotation")) {
					line = in.readLine();
					classSmali += line + "\n";
					if (line.startsWith("    value = ")) {
						String dexC = line.substring(line.lastIndexOf(" = ")+3);
						StaticClass outerC = staticApp.findClassByDexName(dexC);
						if (outerC == null) {
							outerC = new StaticClass();
							outerC.setJavaName(Grammar.dexToJavaClassName(dexC));
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
			StaticField f = new StaticField();
			if (line.contains(" public "))		f.setPublic(true);
			if (line.contains(" private "))		f.setPrivate(true);
			if (line.contains(" protected "))	f.setProtected(true);
			if (line.contains(" final "))		f.setFinal(true);
			if (line.contains(" static "))		f.setStatic(true);
										else	f.setInstance(true);
			String nameType = line.substring(line.lastIndexOf(" ")+1);
			if (f.isFinal() && line.contains(" = ")) {
				System.out.println("-C- " + c.getJavaName() + ": " + line);
				nameType = line.split(" = ")[0];
				nameType = nameType.substring(nameType.lastIndexOf(" ")+1);
				String finalValue = line.split(" = ")[1];
				f.setFinalValue(finalValue);
			}
			String name = nameType.split(":")[0];
			String dexType = nameType.split(":")[1];
			f.setName(name);
			f.setType(Grammar.dexToJavaClassName(dexType));
		}
	}
	
	private static int getLargestLineNumber(File f) {
		int result = 0;
		try {
			in = new BufferedReader(new FileReader(f));
			String line;
			while ((line = in.readLine())!=null) {
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
