package de.patricklammers.vorlesungsplan.app.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;
import de.patricklammers.vorlesungsplan.app.interfaces.DialogAndToastActivity;
import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.HashBuilder;

/**
 * Mit Hilfe dieser Klasse können Dateien heruntergeladen werden
 * 
 * @author Patrick Lammers (Basis-Funktionalitäten) & Florian Schwab (Thread-Funktionalitäten)
 */
public class FileDownload extends Thread implements CancelableThread  {
	// Der Pfad, an der die Datei lokal gespeichert wird
	private String localPath;
	// Der Pfad (meistens eine URL) zur Datei, die heruntergeladen wird
	private String remotePath;
	// Die Activity, die den FileDownloader erstellt hat
	private DialogAndToastActivity activity;
	// Die Methode, die nach dem Download aufgerufen wird
	private Method method;
	// Das Object an dem die Methode aufgerufen wird
	private Object receiver;
	// Gibt an, ob der Thread gecancelt wurde
	private boolean canceled;
	// Die TimedAction, die anzeigt, wenn der Download lange dauert
	private TimedAction longDownload = null;
	// Der Status mit dem ein Download abschließen kann
	public enum DownloadStatus { SUCCESSFULL, FAILED_NO_LOCALPATH, FAILED_NO_REMOTEPATH, FAILED_NO_INTERNETCON, FAILED_OTHER_REASON, WITHOUT_DOWNLOAD}
	
	// Timeout Variablen
	public final static int READ_TIMEOUT = 60000;	
	public final static int CONNECT_TIMEOUT = 20000;
	
	/**
	 * Erzeugt einen neuen FileDownload mit folgenden Parametern
	 * 
	 * @param activity 
	 * @param method
	 * @param receiver
	 */
	public FileDownload(DialogAndToastActivity activity, Method method, Object receiver) {
		this.activity = activity;
		this.method = method;
		this.receiver = receiver;
		this.canceled = false;
	}
	
	/**
	 * Wenn der Download über FileDownloader.start() gestartet wird, 
	 * dann wird diese Methode einmal durchlaufen
	 */
	@Override
	public void run() {
		// Zeige einen Dialog an, damit der Benutzer weiß, dass gerade eine Datei heruntergeladen wird
		String fileName = new File(remotePath).getName();

		if(fileName.equals("calendars.list")) {
			fileName = ((Activity) activity).getString(R.string.download_dialog_courselist);
		} else if(fileName.endsWith(".ics")) {
			fileName = ((Activity) activity).getString(R.string.download_dialog_lectureschedule);
		} else if(fileName.equals("speiseplan.php")) {
			fileName = ((Activity) activity).getString(R.string.download_dialog_mensamenu);
		} else if(fileName.equals("info.php")) {
			fileName = ((Activity) activity).getString(R.string.download_dialog_infos);
		}
		
		activity.showWaitDialog(((Activity) activity).getString(R.string.download_dialog_waittext), 
								((Activity) activity).getString(R.string.download_dialog_pretitle)+" "+
								fileName+" "+
								((Activity) activity).getString(R.string.download_dialog_posttitle),
								this);
		
		// Den eigentlichen Download ausführen
		DownloadStatus result = download();		
		
		// Die TimedAction abbrechen, denn der Download könnte bereits vor Ablauf der Zeit fertig sein
		if(longDownload != null) {
			longDownload.cancel();
		}
		
		// Den Dialog wieder verstecken
		activity.hideWaitDialog();

		if(this.canceled) {
			return;
		}
		
		synchronized(this) {
			// Abschließend wird versucht die Methode aufzurufen
			try {
				// Dabei wird der Methode das Ergebnis des Downloads mitgeteilt, 
				// d.h. die Methode ist auch für die Auswertung zuständig
				method.invoke(receiver, new Object[] { result });
			} catch (final IllegalArgumentException iarge) {
				Logbook.e(iarge);
			} catch (final IllegalAccessException iacse) {
				Logbook.e(iacse);
			} catch (final InvocationTargetException ite) {
				Logbook.e(ite);
				ite.printStackTrace();
			}	
		}
	}

	/** 
	 * Führt den eigentlichen Download aus und liefert einen Statuswert zurück
	 * 
	 * @return 
	 */
	private DownloadStatus download() {
		// Wenn der lokale Pfad nicht angegeben wurde => abbrechen
		if (this.localPath == null || this.localPath.equals("")) {
            Logbook.w("LocalPath not set");
			return DownloadStatus.FAILED_NO_LOCALPATH;
		}
		
		// Wenn der remote Pfad (= URL) nicht angegeben wurde => abbrechen
		if (this.remotePath == null || this.remotePath.equals("")) {
            Logbook.w("RemotePath not set");
			return DownloadStatus.FAILED_NO_REMOTEPATH;
		}
		
		// Wenn keine Verbindung zum Internet besteht => abbrechen
		if (!this.chkConnectionStatus()) {
			return DownloadStatus.FAILED_NO_INTERNETCON;
		}
		
		// TimedAction vorbereiten und starten;
		try {
			Method m = activity.getClass().getDeclaredMethod("changeDialogText", new Class[] { String.class });
			longDownload = new TimedAction(15000,activity,m,((Activity) activity).getString(R.string.download_dialog_overrun));
			longDownload.start();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        try {
            URL url = new URL(this.remotePath);
            File file = new File(this.localPath);

            //long startTime = System.currentTimeMillis();
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
            	
            	// Wenn der Download gecancelt wurde
            	if(this.canceled) {
            		return DownloadStatus.FAILED_OTHER_REASON;
            	}
            }

            // Die gelesenen Bytes in einen String umwandeln
            try {
                FileOutputStream fos = new FileOutputStream(file);     
                fos.write(baf.toByteArray());
                fos.close();
                
                // Wenn ein Vorlesungsplan heruntergeladen wurde, dann wird der Hashwert gespeichert
                if(file.getName().endsWith(".ics")) {
                	Settings.getInstance((Activity) activity).setSetting("hash", HashBuilder.toMD5(new String(baf.toByteArray(), "UTF-8"),"UTF-8"));
                }       	
            } catch(FileNotFoundException fnfe) {
            }
            
            // Wenn die Datei existiert, aber 0 Byte groß ist sollte sie gelöscht werden
            if(file.exists() && file.length() == 0) {
            	file.delete();
            	return DownloadStatus.FAILED_NO_INTERNETCON;
            }
            
            //Logbook.d("download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
        	return DownloadStatus.SUCCESSFULL;
        } catch (FileNotFoundException fnfe) {
        	Logbook.e(fnfe);        
        } catch (SocketTimeoutException ste) {
        	Logbook.e(ste);
        } catch (IOException ioe) {
        	Logbook.e(ioe);
        }
        
    	return DownloadStatus.FAILED_OTHER_REASON;
	}

	/**
	 * Überprüft den Status einer Datenverbindung (WIFI oder Mobilfunk)
	 * 
	 * @return boolean 	true, wenn eine Verbindung besteht
	 * 					false, wenn keine Verbindung besteht
	 */
	private boolean chkConnectionStatus() {
		// Einen ConnectivityManager erzeugen
		ConnectivityManager connMgr = (ConnectivityManager)	((Activity) this.activity).getSystemService(Context.CONNECTIVITY_SERVICE);
		// Die Netzwerkinformationen für WIFI oder Mobilfunk abfragen
		final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	
		// Wenn eins der beiden einen CONNECTED Status aufweißt, dann besteht eine Datenverbindung
		if (mobile == null && wifi == null) {
			return false;
		}
		if (mobile == null) {
			if (wifi.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			} else {
				return false;
			}
		} else {
			if (wifi.getState() == NetworkInfo.State.CONNECTED || mobile.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
	
	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public boolean cancel() {
		// Setze canceled auf true
		this.canceled = true;
		
		// Cancel auch die TimedAction
		longDownload.cancel();
		
		// TODO: Auswirkung abschätzen
		synchronized(this) {
			// Abschließend wird versucht die Methode aufzurufen
			try {
				// Dabei wird der Methode das Ergebnis des Downloads mitgeteilt, 
				// d.h. die Methode ist auch für die Auswertung zuständig
				method.invoke(receiver, new Object[] { DownloadStatus.FAILED_OTHER_REASON });
			} catch (final IllegalArgumentException iarge) {
				Logbook.e(iarge);
			} catch (final IllegalAccessException iacse) {
				Logbook.e(iacse);
			} catch (final InvocationTargetException ite) {
				Logbook.e(ite);
			}
		}
		
		return true;		
	}
}
