package ml.arseniy899.drone_control;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;

/**
 * \brief One class to combine all logs coming from whole application.
 * If log file path is right, data is written there from this class
 */
public class LogKeeper
{
	public File file;//!< Link to file object where data should be written
	public static FileWriter writer;//!< Stream-writer for outputting data to file
	public static Date datum;//!< Date object for formatting data for file names and logs date/time inside file
	public static  LogKeeper self;//!< Link to instance of object. Need because methods are public static
	MemoryWork memoryWork;//!< Instance of MemoryWork class (constant memory) for determine uif user has switched on debug output
	private static String mPackageName = BuildConfig.APPLICATION_ID;
	/**
	 * Constructor for start-up propses: loading logs path, opening file, etc.
	 * @param context Need to open file
	 */
	public LogKeeper(Context context)
	{
		LogKeeper.self = this;
		this.memoryWork = new MemoryWork (context);
		LogKeeper.datum = new Date ();
		
		SimpleDateFormat df = new SimpleDateFormat ("yyyy-MM-dd", Locale.ROOT);
		String fullName;// = df.format(LogKeeper.datum)+"_"+packageName+".log";
		fullName = mPackageName+"_"+df.format(LogKeeper.datum)+".log";
		
		Stack<File> dirlist = new Stack<File> ();
		dirlist.clear();
		File dir = new File (getHomePath()+"/logs/");
		dirlist.push(new File (dir+"/logs/"));
		
		while (!dirlist.isEmpty()) {
			File dirCurrent = dirlist.pop();
			
			File[] fileList = dirCurrent.listFiles();
			if(fileList != null)
				for (File aFileList : fileList) {
					if (!aFileList.isDirectory())
					{
						Calendar time = Calendar.getInstance();
						time.add(Calendar.DAY_OF_YEAR,-3);
						Date lastModified = new Date(aFileList.lastModified());
						if(lastModified.before(time.getTime()))
						{
							aFileList.delete();
						}
					}
				}
		}
		
		
		
		LogKeeper.i("//","reading file if");
		if(!dir.exists ())
			dir.mkdir ();
		this.file = new File (dir, fullName);
		if(!this.file.exists ())
			try {
				this.file.createNewFile ();
			} catch (IOException e) {
				e.printStackTrace ();
			}
   
		try {
			LogKeeper.writer = new FileWriter (this.file, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Outputs data as 'Information' stream to adb log, outputs data to file. Format: date-time + " | I/" + tag + ": " + value
	 * @param tag Tag name for find specific information
	 * @param value Information for logging
	 */
	public static void i(String tag, String value)
	{
		SimpleDateFormat time = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
		LogKeeper.datum = new Date ();
		String timeF = time.format (LogKeeper.datum) + "";
		tag = mPackageName+"/"+tag;
		if (LogKeeper.writer == null) {
			Log.i (tag, value + " | LOG FILE ERROR!!");
			return;
		}
		else Log.i (tag, value);
		
		try {
			String str = timeF + " | I/" + tag + ": " + value + "\n";
			LogKeeper.writer.write (str);
			LogKeeper.writer.flush ();
		} catch (IOException e) {
			e.printStackTrace ();
			
		}
	
	}
	/**
	 * Outputs data as 'Debug' stream to adb log, outputs data to file if user switched on debug mode. Format: date-time + " | I/" + tag + ": " + value
	 * @param tag Tag name for find specific information
	 * @param value Information for logging
	 */
	public static void d(String tag, String value)
	{
		tag = mPackageName+"/"+tag;
		if (LogKeeper.writer == null)
		{
			Log.d (tag, value + " | LOG FILE ERROR!!");
			return;
		}
		else
		{
			if(LogKeeper.self.memoryWork.loadB ("debug-on") || true)
			{
				LogKeeper.datum = new Date ();
				SimpleDateFormat time = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
				String timeF = time.format(LogKeeper.datum)+"";
				try {
					String str = timeF+" | D/"+tag+": "+value+"\n";
					LogKeeper.writer.write(str);
					LogKeeper.writer.flush();
				} catch (IOException e) {
					e.printStackTrace ();
					
				}
			}
			Log.d (tag, value);
		}
	}
	/**
	 * Outputs data as 'Error' stream to adb log, outputs data to file. Format: date-time + " | E/" + tag + ": " + value
	 * @param tag Tag name for find specific information
	 * @param value Information for logging
	 */
	public static void e(String tag, String value)
	{
		tag = mPackageName+"/"+tag;
		SimpleDateFormat time = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
		LogKeeper.datum = new Date ();
		String timeF = time.format(LogKeeper.datum)+"";
		if(LogKeeper.writer == null) {
			Log.e (tag, value+" | LOG FILE ERROR!!");
			return;
		}
		else
			Log.e(tag,value);
		try {
			String str = timeF+" | E/"+tag+": "+value+"\n";
			LogKeeper.writer.write(str);
			LogKeeper.writer.flush();
		} catch (IOException e) {
			e.printStackTrace ();
			
		}
	}
	
	/**
	 * Get's full path to home directory for the app. If doesn't exist - creates it.
	 * @return String absolute path to app directory
	 */
	public static String getHomePath()
	{
		String path = "";
		File cacheDir;
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
			cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"DRONE");
		else
			cacheDir=new File(android.os.Environment.getRootDirectory(),"DRONE");

		if(!cacheDir.exists())
			cacheDir.mkdirs();
		File logs = new File(cacheDir.getPath(), "logs");
		if(!logs.exists())
			logs.mkdirs();
		return path;
	}
	
	/**
	 * Convert general exception to formatted string with stachtrace
	 * @param e general exception
	 * @return resulting String
	 */
	public static String getExceptionInfo(Exception e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString(); // stack trace as a string
	}
}
