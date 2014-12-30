package tools;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import main.Paths;
import smali.Grammar;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StringConstant;
import staticFamily.StaticApp;


public class Soot {

	public static void Template(File file) {

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTransform", new SceneTransformer() {
					protected void internalTransform(String phaseName, Map<String, String> options) {
						// this method will be called only once
						System.out.println("WJTP");
					}
				}));

		PackManager.v().getPack("jtp").add(new Transform("jtp.myTransform", new BodyTransformer() {
					protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
						// this method will be called on each method in the DEX
						System.out.println("JTP " + b.getMethod().getSignature());
					}
				}));

		String[] args = {
				"-d", Paths.appDataDir + file.getName() + "/soot/Jimples",
				"-f", "J",
				"-src-prec", "apk",
				"-ire", "-allow-phantom-refs", "-w",
				"-force-android-jar", Paths.androidJarPath,
				"-process-path", file.getAbsolutePath() };
		soot.Main.main(args);
	}

	private static int returnCounter = 0;
	
	
	public static void InstrumentEveryMethod(StaticApp testApp){
		
		System.out.println("\nInstrumenting method information with Soot...\n");
		
		PackManager.v().getPack("jtp").add(new Transform("jtp.myTransform", new BodyTransformer() {
			protected void internalTransform(final Body b, String phaseName,Map<String, String> options) {

				final Local l_outPrint = Jimple.v().newLocal("outPrint", RefType.v("java.io.PrintStream"));
				b.getLocals().add(l_outPrint);
				final SootMethod out_println = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
				PatchingChain<Unit> units = b.getUnits();
				String mSig = generateBytecodeSignature(b.getMethod().getBytecodeSignature());
				System.out.println("[beginning]" + mSig);
				// first instrument the first line
				Iterator<Unit> unitsIT = b.getUnits().snapshotIterator();
				int hasThis = 1;
				if (b.getMethod().isStatic())
					hasThis = 0;
				for (int i = 0; i < b.getMethod().getParameterCount()+hasThis; i++)
					unitsIT.next();
				Unit firstUnit = unitsIT.next();
				units.insertBefore(Jimple.v().newAssignStmt(l_outPrint,
						Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())
						), firstUnit);
				units.insertBefore(Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(l_outPrint, out_println.makeRef(), StringConstant.v("METHOD_STARTING," + mSig))
						), firstUnit);
				b.validate();
				// then instrument the return
				returnCounter = 1;
				unitsIT = b.getUnits().snapshotIterator();
				while (unitsIT.hasNext()) {
					final Unit u = unitsIT.next();
					u.apply(new AbstractStmtSwitch() {
						public void caseReturnStmt(ReturnStmt rS) {
							PatchingChain<Unit> units = b.getUnits();
							String mSig = generateBytecodeSignature(b.getMethod().getBytecodeSignature());
							System.out.println("[Return]" + mSig);
							units.insertBefore(Jimple.v().newAssignStmt(l_outPrint,
									Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())
									), u);
							units.insertBefore(Jimple.v().newInvokeStmt(
									Jimple.v().newVirtualInvokeExpr(l_outPrint, out_println.makeRef(), StringConstant.v("METHOD_RETURNING," + mSig + "," + returnCounter))
									), u);
							returnCounter++;
						}
						public void caseReturnVoidStmt(ReturnVoidStmt rS) {
							PatchingChain<Unit> units = b.getUnits();
							String mSig = generateBytecodeSignature(b.getMethod().getBytecodeSignature());
							System.out.println("[Return-void]" + mSig);
							units.insertBefore(Jimple.v().newAssignStmt(l_outPrint,
									Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())
									), u);
							units.insertBefore(Jimple.v().newInvokeStmt(
									Jimple.v().newVirtualInvokeExpr(l_outPrint, out_println.makeRef(), StringConstant.v("METHOD_RETURNING," + mSig + "," + returnCounter))
									), u);
							returnCounter++;
						}
					});
				}
				b.validate();
			}
		}));
		try {
		File outFile = new File(testApp.outPath + "/" + testApp.getApkFile().getName());
		if (outFile.exists())
			outFile.delete();
		String[] args = {
				"-d", testApp.outPath,
				"-f", "dex",
				"-src-prec", "apk",
				"-ire", "-allow-phantom-refs", "-w",
				"-force-android-jar", Paths.androidJarPath,
				"-process-path", testApp.getApkFile().getAbsolutePath() };
		Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
		Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
		soot.Main.main(args);
		soot.G.reset();
		outFile = new File(testApp.outPath + "/" + testApp.getApkFile().getName());
		String outAppName = testApp.getApkFile().getName();
		outAppName = outAppName.substring(0, outAppName.lastIndexOf(".apk"));
		outAppName = outAppName + "_soot_unsigned.apk";
		File newOutFile = new File(testApp.outPath + "/" + outAppName);
		if (!outFile.renameTo(newOutFile))
			throw (new Exception("Failed to rename soot instrumented app"));
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	private static String generateBytecodeSignature(String methodInfo) {
		String dexClassName = Grammar.javaToDexClassName(methodInfo.substring(1, methodInfo.indexOf(": ")));
		String dexMethodSubsig = methodInfo.substring(methodInfo.indexOf(": ")+2, methodInfo.lastIndexOf(">"));
		return dexClassName + "->" + dexMethodSubsig;
	}
	
}
