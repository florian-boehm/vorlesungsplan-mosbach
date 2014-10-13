package de.patricklammers.vorlesungsplan.app.io;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import de.patricklammers.vorlesungsplan.app.SettingsActivity;
import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;
import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.HashBuilder;
import de.patricklammers.vorlesungsplan.app.utils.StatusHelper.SH;

public class RegisterServerOperation extends Thread implements CancelableThread {
	
	private enum OperationType {
		REGISTER, UNREGISTER, STATUS, ACKNOWLEDGE, HASH
	}
	
    private static final String		SENDER_ID							= "744552673696";
	private final static int 		READ_TIMEOUT 						= 60000;	
	private final static int 		CONNECT_TIMEOUT 					= 20000;
	private final String 			baseURL 							= "http://patricklammers.de/vorlesungsplan-app-notifier/";
	private String 					servletName 						= null;
	private Activity				activity 							= null;
	private HashMap<String,String> 	params 								= null;
	private OperationType			operationType						= null;
	private Context					context 							= null;
	private Object					invokeObject						= null;
	private Method					invokeMethod						= null;
	private boolean					canceled							= false;
	
	private RegisterServerOperation() {
		
	}
	
	public static RegisterServerOperation register(final Activity activity) {
		// Bereite Parameter für die RegisterServerOperation vor
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("regId", "dummy");
		params.put("course", Settings.getInstance(activity).getSetting("course"));
		
		// Baue eine RegisterServerOperation für den OperationType "REGISTER" auf
        final RegisterServerOperation operation = new RegisterServerOperation()
        		.setServletName("register")
        		.setOperationType(OperationType.REGISTER)
        		.setParams(params)
        		.setActivity(activity);

        return operation;
	}
	
	public static void registerAS(final String regId, final String course) {
		// Bereite Parameter für die RegisterServerOperation vor
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("regId", regId);
		params.put("course", course);
		
		// Baue eine RegisterServerOperation für den OperationType "REGISTER" auf und starte diese direkt
        new RegisterServerOperation()
        		.setServletName("register")
        		.setOperationType(OperationType.REGISTER)
        		.setParams(params)
        		.start();
	}
	
	public static void ackAS(String regId) {
		// Bereite Parameter für die RegisterServerOperation vor
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("regId", regId);
		
		// Baue eine RegisterServerOperation für den OperationType "ACKNOWLEDGE" auf
		new RegisterServerOperation()
        		.setServletName("ack")
        		.setOperationType(OperationType.ACKNOWLEDGE)
        		.setParams(params)
        		.start();
	}
	
	public static RegisterServerOperation ack(String regId) {
		// Bereite Parameter für die RegisterServerOperation vor
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("regId", regId);
		
		// Baue eine RegisterServerOperation für den OperationType "ACKNOWLEDGE" auf
        RegisterServerOperation operation = new RegisterServerOperation()
        		.setServletName("ack")
        		.setOperationType(OperationType.ACKNOWLEDGE)
        		.setParams(params);
        
        return operation;
	}
	
	public static RegisterServerOperation unregister(String regId) {
		// Bereite Parameter für die RegisterServerOperation vor
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("regId", regId);
		
		// Baue eine RegisterServerOperation für den OperationType "UNREGISTER" auf
        RegisterServerOperation operation = new RegisterServerOperation()
        		.setServletName("unregister")
        		.setOperationType(OperationType.UNREGISTER)
        		.setParams(params);

        return operation;
	}
	
	public static RegisterServerOperation hash(final Settings settings, final ByteArrayBuffer baf, final String regId, final Context context) {
		final String hashValue;
		String tempValue = "";
		
		try {
			tempValue = HashBuilder.toMD5(new String(baf.toByteArray(), "UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			tempValue = "";
			Logbook.e(uee);
		} finally {
			hashValue = tempValue;
		}
		
		// Bereite Parameter für die RegisterServerOperation vor
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("course", settings.getSetting("course"));
		params.put("hashValue", hashValue);
		params.put("regId", regId);
		
		// Baue eine RegisterServerOperation für den OperationType "HASH" auf
        RegisterServerOperation operation = new RegisterServerOperation()
        		.setServletName("hash")
        		.setOperationType(OperationType.HASH)
        		.setContext(context)
        		.setParams(params);
        
        return operation;
	}
	
	/**
	 * Diese Funktion bereitet eine RegisterServerOperation für die Statusprüfung vor.
	 * AS (= Autostart) bedeutet, dass die Operation automatisch gestartet wird.
	 * 
	 * @param settings
	 */
	public static void statusAS(final Settings settings) {
		// Prüfe, ob die benötigten Parameter vorhanden sind
		if(SH.regIdAndCourseDontExist(settings)) {
			Logbook.w("Status-Operation will not be executed because of missing parameters!");
			return;
		}
		
		// Bereite Parameter für die RegisterServerOperation vor
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("regId", settings.getSetting("regId"));
		params.put("course", settings.getSetting("course"));
		
		// Baue eine RegisterServerOperation für den OperationType "STATUS" auf und starte diese direkt
        new RegisterServerOperation()
        		.setServletName("status")
        		.setOperationType(OperationType.STATUS)
        		.setParams(params)
        		.start();
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		
	    if (resultCode != ConnectionResult.SUCCESS) {
	    	Logbook.w("resultCode: "+ resultCode);
	    	Logbook.w("is recoverable: "+ GooglePlayServicesUtil.isUserRecoverableError(resultCode));
	    	Logbook.w("error string: "+GooglePlayServicesUtil.getErrorString(resultCode));
	    	Logbook.w("activity != null ? "+(activity != null));
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            if(activity instanceof SettingsActivity) {
	            	((SettingsActivity) activity).showGCMErrorDialog(resultCode);
	            }
	        } else {
	            Logbook.e("This device is not supported.");
	            //context.finish();
	        }
	        return false;
		}
		
		return true;		
	}

	public void run() {
		try {
			// Wenn in den Parametern die regId ist, dann muss diese hier gefüllt werden
			if(params.containsKey("regId") && params.get("regId").equals("dummy")) {
				if(checkPlayServices()) {
					//Logbook.d("Device is GCM capable");
					
					// Hole die regId des Geräts oder erzeuge eine neue
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity);
			        String regId = Settings.getInstance(activity).getSetting("regId");
	
			        if (regId == null || regId.isEmpty()) {
			            regId = gcm.register(SENDER_ID);
			            Settings.getInstance(activity).setSetting("regId", regId);
			        }
			        
			        params.put("regId", regId);
		            //Logbook.d("Registration id for this device is: "+regId);
				} else {
					throw new Exception("REGISTRATION_FAILED");
				}
			}
			
			URL url = new URL(prepareURL());			
			long startTime = System.currentTimeMillis();
	        
	        if(canceled) {
	        	throw new Exception("THREAD_CANECELED");
	        }
		       
	        // Eine neue Verbindung zur URL öffnen
	        URLConnection ucon = url.openConnection();
	        ucon.setConnectTimeout(RegisterServerOperation.CONNECT_TIMEOUT);
	        ucon.setReadTimeout(RegisterServerOperation.READ_TIMEOUT);

	        // Einen InputStream definieren, um von der Verbindung zu lesen
	        InputStream is = ucon.getInputStream();
	        BufferedInputStream bis = new BufferedInputStream(is);

	        // Ließt Bytes in einen Buffer, bis es nicht mehr zu lesen gibt (-1)
	        ByteArrayBuffer baf = new ByteArrayBuffer(50);
	        int current = 0;
	        while ((current = bis.read()) != -1) {
	        	baf.append((byte) current);
	        }
	        
	        if(canceled) {
	        	throw new Exception("THREAD_CANECELED");
	        }
	        
	        String result = new String(baf.toByteArray(),"UTF-8");
	        
	        Logbook.d("Result from server: "+result);
	        Logbook.d("RegisterServerOperation finished in "+(System.currentTimeMillis()-startTime)+"ms");
			
			// Rufe die Post-Methoden zum entsprechenden OperationType auf
			switch(operationType) {
				case HASH:
					postHash(result);
					break;
				case STATUS:
					postStatus(result);
					break;
				case ACKNOWLEDGE:
					postAck(result);
					break;
				default:	
			}
			
			// Rufe eine spezifizierte Methode auf, wenn angegeben
			if(invokeMethod != null && invokeObject != null) {
				invokeMethod.invoke(invokeObject, new Object[] { result });
			}
		} catch (Exception outerE) {
			if(outerE.getMessage() != null && outerE.getMessage().equals("THREAD_CANECELED")) {
				// Tue nichts, die invokeMethod wird von der cancel-Methode ausgeführt
			} else if(outerE.getMessage() != null && (outerE.getMessage().equals("REGISTRATION_FAILED") || outerE.getMessage().equals("SERVICE_NOT_AVAILABLE"))) {
				// Rufe eine spezifizierte Methode auf, wenn angegeben
				if(invokeMethod != null && invokeObject != null) {
					try {
						invokeMethod.invoke(invokeObject, new Object[] { null });
					} catch (Exception innerE) {
						Logbook.e(innerE);
					}
				}
			} else {
				Logbook.e(outerE);
			}
		}
	}

	/**
	 * Baue die URL zum Servlet mit den entsprechenden Parametern zusammen
	 * 
	 * @return
	 */
	private String prepareURL() {
		// Wenn der servletName leer ist oder keine Parameter definiert wurden
		if(servletName == null || servletName.equals("") || params == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder(baseURL);		
		sb.append(servletName+"?");
		
		// Hänge die Parameter an die bisherige URL
		for(Entry<String,String> param : params.entrySet()) {
			String val = (param.getValue() == null) ? "" : param.getValue();
			
			sb.append(param.getKey()+"="+val+"&");
		}
		
		return new String(sb);
	}
	
	private void postAck(String result) {
		if(result.equals("DEV_NOT_REGISTERED") && params.get("course") != null) {
			RegisterServerOperation.registerAS(params.get("regId"), params.get("course"));
		}
	}
	
	private void postHash(String result) {
		if(result.equals("TRUE")) {
			if(params.get("course") != null) {
				RegisterServerOperation.ack(params.get("regId")).addParam("course", params.get("course")).start();
			} else {
				RegisterServerOperation.ackAS(params.get("regId"));
			}
			
			if(context != null) {
				Settings.getInstance(context).setSetting("hash", params.get("hashValue"));
			}
		}
	}

	private void postStatus(String result) {
		if(result.equals("DEV_NOT_REGISTERED") 
				|| (result.startsWith("DEV_REGISTERED") 
				&& !result.substring(result.indexOf("|")+1).equals(params.get("course")))) {
			RegisterServerOperation.registerAS(params.get("regId"),params.get("course"));
		}
	}

	/**
	 * Setzt den Namen des Servlets, das bei der RegisterServerOperation angesprochen wird
	 * 
	 * @param servletName
	 * @return
	 */
	public RegisterServerOperation setServletName(String servletName) {
		this.servletName = servletName;
		return this;
	}

	public RegisterServerOperation setParams(HashMap<String, String> params) {
		this.params = params;
		return this;
	}
	
	public RegisterServerOperation setOperationType(OperationType operationType) {
		this.operationType = operationType;
		return this;
	}
	
	public RegisterServerOperation setActivity(Activity activity) {
		this.activity = activity;
		return this;
	}

	public RegisterServerOperation setContext(Context context) {
		this.context = context;
		return this;
	}

	public RegisterServerOperation setInvokeObject(Object invokeObject) {
		this.invokeObject = invokeObject;
		return this;
	}

	public RegisterServerOperation setInvokeMethod(Method invokeMethod) {
		this.invokeMethod = invokeMethod;
		return this;
	}

	public RegisterServerOperation addParam(String key, String value) {
		this.params.put(key, value);
		return this;
	}

	public boolean cancel() {
		canceled = true;
		
		// Rufe eine spezifizierte Methode auf, wenn angegeben
		if(invokeMethod != null && invokeObject != null) {
			try {
				invokeMethod.invoke(invokeObject, new Object[] { null });
			} catch (Exception e) {
				Logbook.e(e);
			}
		}
		
		return true;
	}
}
