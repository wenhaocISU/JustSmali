package main;

public class Paths {

	/**
	 * Readme:
	 * appDataDir and androidSDKPath are the two directories that you need change for 
	 * your local run.
	 * 
	 * androidSDKPath is the place where sdk locates on your computer
	 * appDataDir is the place where the generated files stored.
	 */
	
	public static String androidSDKPath = "/home/wenhaoc/bin/adt-bundle-linux-x86_64-20140702/";
	public static String appDataDir = "/home/wenhaoc/AppData/";
	public static String keystorePath = "/home/wenhaoc/bin/keys/wenhaoc.keystore";
	public static String keystoreKey = "isu_obad";
	
	public static String androidToolPath = androidSDKPath+"sdk/tools/";
	public static String androidPlatformToolPath = androidSDKPath+"sdk/platform-tools/";
	public static String adbPath = androidPlatformToolPath+"adb";
	public static String androidJarPath = "libs/android.jar";
	public static String apktoolPath = "libs/apktool.jar";
	public static String aaptPath = androidSDKPath + "/sdk/build-tools/android-4.4W/aapt";
}