package de.patricklammers.vorlesungsplan.app.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.http.util.ByteArrayBuffer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.StatusHelper.SH;

public class GCMIntentService extends IntentService {

    public GCMIntentService() {
        super("GCMIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    	
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        Settings settings = Settings.getInstance(this.getApplicationContext());
        
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            	// Extras holen ...            	
            	String courseToUpdate = extras.getString("UPDATE");
            	String hashValue = extras.getString("HASH");
            	String regIdOnServer = extras.getString("REGID");
            	
            	// Wenn eine Notification ankommt, bei der die regId auf dem Server nicht mit der auf dem Gerät übereinstimmt,
            	// dann sollte die regId auf dem Server gelöscht werden. Dies tritt auf, wenn das Gerät eine neue regId bekommen
            	// hat, aber der Server diese nicht aktualisiert hat. Es verhindert außerdem das mehrmalige Empfangen der gleichen
            	// Nachricht!
            	if(SH.regIdOnServerMismatchesTheRegIdOnTheDevice(settings, regIdOnServer)) {
            		RegisterServerOperation.unregister(regIdOnServer).start();
            	} else {
            		// Prüfe Hashsumme vor dem Update
                	if(settings.getSetting("hash") == null || !settings.getSetting("hash").equals(hashValue)) {            		
                		if(SH.wantsGCM(settings)) {
                			if(courseToUpdate.equals(settings.getSetting("course"))) {
                				downloadCalendar(settings.getSetting("regId"));
                			} else {
                				Logbook.w("Received update notification for wrong course");
                				// TODO prüfen ob sinnvoll!
                	        	RegisterServerOperation.statusAS(settings);
                			}
                		}
                	} else {
                		if(SH.wantsGCM(settings)) {
                			// Wenn kein Update notwendig ist wird trotzdem das lastCalRefresh auf die aktuelle Zeit gesetzt
                			// Das ist wichtig, damit die automatische Wiederanmeldung nur alle 14 Tage nach dem letzten ACK durchgeführt wird
                			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY);
                			Calendar cal = Calendar.getInstance(Locale.GERMANY);
                			settings.setSetting("lastCalRefresh", format.format(cal.getTime()));
                			
                			RegisterServerOperation.ackAS(settings.getSetting("regId"));
                		}
                	}
            	}
            } else if (extras.getString("registration_id") != null) {
            	// Eine regId war vorhanden, aber die neue stimmt nicht mit der aktuell gespeicherten überein
            	if(settings.getSetting("regId") != null && !settings.getSetting("regId").equals("") && !settings.getSetting("regId").equals(extras.getString("registration_id"))) {
            		settings.setSetting("regId", extras.getString("registration_id"));
            	}
            }
        }
        
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void downloadCalendar(String regId) {
    	try {
    		Settings settings = Settings.getInstance(this.getApplicationContext());
            URL url = new URL(settings.getSetting("icsFileRemote"));
            File file = new File(this.getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+"/updated_"+settings.getSetting("icsFile"));

            // Eine neue Verbindung zur URL öffnen
            URLConnection ucon = url.openConnection();
            ucon.setConnectTimeout(FileDownload.CONNECT_TIMEOUT);
            ucon.setReadTimeout(FileDownload.READ_TIMEOUT);

            // Einen InputStream definieren, um von der Verbindung zu lesen
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            // Ließt Bytes in einen Buffer, bis es nicht mehr zu lesen gibt (-1)
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
            	baf.append((byte) current);
            }

            // Die gelesenen Bytes in einen String umwandeln
            FileOutputStream fos = new FileOutputStream(file);     
            fos.write(baf.toByteArray());
            fos.close();
            
            // Wenn die Datei existiert, aber 0 Byte groß ist sollte sie gelöscht werden
            if(file.exists() && file.length() == 0) {
            	file.delete();
            } else {
                try {
					RegisterServerOperation.hash(settings, baf, regId, this.getApplicationContext())
						.setInvokeObject(this)
						.setInvokeMethod(this.getClass().getDeclaredMethod("postHash", new Class[] {String.class}))
						.start();
				} catch (NoSuchMethodException e) {
					Logbook.e(e);
				}
            }
        } catch (FileNotFoundException fnfe) {
        	Logbook.e(fnfe);        
        } catch (SocketTimeoutException ste) {
        	Logbook.e(ste);
        } catch (IOException ioe) {
        	Logbook.e(ioe);
        }
	}
    
    public void postHash(String result) {
    	if(result.equals("TRUE")) {
    		try {
	    		sendBroadcast(new Intent("GCMIntentService.UPDATED_CALENDAR"));
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
}