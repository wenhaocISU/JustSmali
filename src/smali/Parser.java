package smali;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;

public class Parser {

	private static StaticApp staticApp;
	private static File smaliFolder;
	private static BufferedReader in;
	private static String classSmali;
	
	public static void parseSmali(StaticApp theApp) {
		staticApp = theApp;
		smaliFolder = new File(staticApp.outPath + "/apktool/smali/");
		for (File f : smaliFolder.listFiles())
			parseFile(f);
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
			final StaticClass c = new StaticClass();
			c.setInDEX(true);
			c.setJavaName(className);
			parseSmaliCode(f, c);
			staticApp.addClass(c);
			//System.out.println(" max line number for class " + c.getJavaName() + "\tis " + getLargestLineNumber(f));
		}
	}
	
	private static void parseSmaliCode(File f, final StaticClass c) {
		int largestLineNumber = getLargestLineNumber(f);
		classSmali = "";
		try {
			in = new BufferedReader(new FileReader(f));
			String line;
			// first line
			if ((line = in.readLine())!=null) {
				classSmali = line;
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
					
				}
			}
			in.close();
		}	catch (Exception e) {e.printStackTrace();}
		
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
			else if (line.equals(".annotation system Ldalvik/annotation/InnerClass;")) {
				c.setInnerClass(true);
			}
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
