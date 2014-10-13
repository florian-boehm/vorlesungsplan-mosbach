package de.patricklammers.vorlesungsplan.app.io;

import java.util.Arrays;

import android.util.Log;

/**
 * Diese Klasse ist eigentlich nur ein Mantel für android.util.Log 
 * allerdings bietet sie das setzten einer Loglevel-Grenze. Dadurch 
 * werden nur Nachrichten ausgegeben, die gleich oder über dieser
 * Grenze liegen. Außerdem vereinheitlicht Logbook die Ausgabe des
 * Objektnamens und der Methode, die den Logeintrag erzeugt hat. 
 * 
 * @author Florian Schwab
 */
public class Logbook {
	// Die Loglevel in aufsteigender Reihenfolge
	public static enum Loglevel { DEBUG, WARNING, ERROR, NONE };
	public static final Loglevel MIN_LOG_LEVEL = Loglevel.DEBUG;
	
	private static void makeEntry(String message, Loglevel loglvl) {
		if(loglvl.ordinal() < MIN_LOG_LEVEL.ordinal()) {
			return;
		}
		
		// Wir bedienen uns des StackTraces um das Objekt und die Methode ausfindig
		// zu machen, welches die Message loggen wollte
		Throwable t = new Throwable();		
		StackTraceElement caller = null;

		if(t.getStackTrace().length >= 3) {
			caller = t.getStackTrace()[2];			
		}
		
		// Alternative Methode
		/*for(StackTraceElement ste : t.getStackTrace()) {
			if(!ste.getClassName().equals("Logbook") || 
					(ste.getClassName().equals("Logbook") && 
							!(ste.getMethodName().equals("makeEntry") || 
							  ste.getMethodName().equals("d") || 
							  ste.getMethodName().equals("e") || 
							  ste.getMethodName().equals("w")))) {
				caller = ste;
				break;
			}
		}*/
		
		String callerString = "";
		
		// Wenn der Caller nicht gefunden wurde, so können wir diesen leider nicht anzeigen
		if(caller != null) {
			String className = caller.getClassName().substring(caller.getClassName().lastIndexOf(".")+1);
			callerString = className+"."+caller.getMethodName()+"("+caller.getFileName()+":"+caller.getLineNumber()+")";
		}
		
		switch(loglvl) {
			case DEBUG:
				Log.d(callerString,message);
				break;
			case WARNING:
				Log.w(callerString,message);
				break;
			default:
			case ERROR:
				Log.e(callerString,message);
				break;
		}
	}
	
	public static void d(String message) {
		Logbook.makeEntry(message, Loglevel.DEBUG);
	}
	
	public static void w(String message) {
		Logbook.makeEntry(message, Loglevel.WARNING);		
	}
	
	public static void e(String message) {
		Logbook.makeEntry(message, Loglevel.ERROR);
	}
	
	public static void d(Exception e) {
		if(e == null) {
			return;
		}
		
		String message = "";
		
		message += (e.getClass() != null && e.getClass().getName() != null) ? e.getClass().getName() : "no classname";
		message += ":";	
		message += (e.getMessage() != null) ? e.getMessage() : Arrays.toString(e.getStackTrace());		
		
		Logbook.makeEntry(message, Loglevel.DEBUG);
	}
	
	public static void w(Exception e) {
		if(e == null) {
			return;
		}
		
		String message = "";
		
		message += (e.getClass() != null && e.getClass().getName() != null) ? e.getClass().getName() : "no classname";
		message += ":";	
		message += (e.getMessage() != null) ? e.getMessage() : Arrays.toString(e.getStackTrace());		
		
		Logbook.makeEntry(message, Loglevel.WARNING);
	}
	
	public static void e(Exception e) {
		if(e == null) {
			return;
		}
		
		String message = "";
		
		message += (e.getClass() != null && e.getClass().getName() != null) ? e.getClass().getName() : "no classname";
		message += ":";	
		message += (e.getMessage() != null) ? e.getMessage() : Arrays.toString(e.getStackTrace());		
		
		Logbook.makeEntry(message, Loglevel.ERROR);
	}
	
	public static String calledFrom() {
		// Wir bedienen uns des StackTraces um das Objekt und die Methode ausfindig
		// zu machen, welches die Message loggen wollte
		Throwable t = new Throwable();		
		StackTraceElement caller = null;

		if(t.getStackTrace().length >= 3) {
			caller = t.getStackTrace()[2];
		}
		
		String callerString = "";
		
		// Wenn der Caller nicht gefunden wurde, so können wir diesen leider nicht anzeigen
		if(caller != null) {
			String className = caller.getClassName().substring(caller.getClassName().lastIndexOf(".")+1);
			callerString = className+"."+caller.getMethodName()+"("+caller.getFileName()+":"+caller.getLineNumber()+")";
		}
		
		return callerString;
	}
}