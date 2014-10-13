package de.patricklammers.vorlesungsplan.app.icalendar;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import android.app.Activity;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.SettingsActivity;
import de.patricklammers.vorlesungsplan.app.io.FileDownload;
import de.patricklammers.vorlesungsplan.app.io.FileDownload.DownloadStatus;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.StatusHelper.SH;

/**
 * 
 * @author Patrick Lammers
 */
public class CourseList {
	// Die URL zur Kalenderliste
	private final String src = "http://pollux.dhbw-mosbach.de/ics/calendars.list";
	private final String localPath;
	private Settings settings;
	private HashMap<String, String> list = new HashMap<String, String>();
	private SettingsActivity sActivity;
	
	/**
	 * Erzeugt eine neue Instanz von CourseList, die das Herunterladen der Kalenderliste
	 * anstößt und alle zur Verfügung stehenden Kurse in einer HashMap speichert
	 * 
	 * @param mActivity
	 * @param forceUpdate 
	 */
	public CourseList(SettingsActivity sActivity) {
		this.sActivity = sActivity;
		this.localPath = ((Activity) this.sActivity).getApplication().getExternalFilesDir(null).getAbsolutePath();
		this.settings = Settings.getInstance((Activity) this.sActivity);
		File d = new File(this.localPath);
		
		if (!d.exists()) {
			d.mkdir();
		}
	}
	
	public void reloadData() {		
		// Wenn bereits eine Kalenderliste existiert ...
		if (new File(this.localPath + "/calendars.list").exists()) {
			// wenn der lastListRefresh Wert noch nicht in den Einstellungen existiert,
			if(SH.preconditionsForCourseListDownloadAreFulfilled(settings)) {
				if(SH.courseListUpdateNecessary(settings)) {
					// Wenn ein Update notwendig ist
					this.update();
				} else {
					// Wenn kein Update notwendig ist
					initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
				}
			} else {
				this.update();
			}
		} else {
			this.update(true);
		}
	}
	
	/**
	 * Führt ein Update der Kursliste durch (nicht erzwungen)
	 */
	public void update() {
		this.update(false);
	}
		
	/**
	 * Führt ein Update der Kursliste durch
	 */
	public void update(boolean forceUpdate) {
		if(this.settings.getSetting("dontTryAgainCourseListDownload") == null || forceUpdate) {
			try {
				// Initialisiere den Downloader
				FileDownload down = new FileDownload(this.sActivity, this.getClass().getMethod("initializeAfter", new Class[] { DownloadStatus.class }),this);
				down.setLocalPath(this.localPath + "/calendars.list");
				down.setRemotePath(src);
				// Starte den Download
				down.start();			
			} catch (NoSuchMethodException nsme) {
	        	Logbook.e(nsme);
			}	
		} else {
			// Den Vorlesungplan immer parsen!
			initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
		}
	}
	
	/**
	 * Initialisiert die CalendarList nach dem Download
	 * 
	 * @param statusCode
	 */
	public void initializeAfter(DownloadStatus statusCode)
	{
		Scanner sc = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.GERMANY);
		Calendar kalender = Calendar.getInstance(Locale.GERMANY);
		String toastMsg = "";
		
		switch (statusCode) {
			case FAILED_NO_LOCALPATH:
			case FAILED_NO_REMOTEPATH:
				toastMsg = (toastMsg.equals("")) ? sActivity.getString(R.string.toast_app_problem) : toastMsg;
			case FAILED_OTHER_REASON:
			case FAILED_NO_INTERNETCON:
				toastMsg = (toastMsg.equals("")) ? sActivity.getString(R.string.toast_courselist_download_failed) : toastMsg;
				// In allen vier Fällen, sollte bis zum Neustart der Anwendung nicht mehr versucht werden, die Kursliste herunterzuladen
				settings.setSetting("dontTryAgainCourseListDownload", "true");
			case SUCCESSFULL:
				toastMsg = (toastMsg.equals("")) ? sActivity.getString(R.string.toast_courselist_download_succeeded) : toastMsg;	
			case WITHOUT_DOWNLOAD:
				// Dieser Teil wird IMMER ausgeführt, egal ob der Download erfolgreich war oder fehlgeschlagen ist
				File f = new File(this.localPath + "/calendars.list");
				
				// Wenn bisher noch keine toastMsg gesetzt wurde, dann wurde die Kalenderliste erfolgreich heruntergeladen
				if(toastMsg.equals(sActivity.getString(R.string.toast_courselist_download_succeeded))) {
					this.settings.setSetting("lastListRefresh", format.format(kalender.getTime()));
				}
			
				try {
					sc = new Scanner(f);
				
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						String[] lineArr = line.split(";");
						if (!lineArr[0].equals("") && !lineArr[0].equals("CALENDARS")) {
							list.put(lineArr[0], lineArr[1]);
						}
					}
					
					// TODO Unbedingt vor der Veröffentlichung löschen!
					list.put("AAA_DEBUG","http://vorlesungsplan.patricklammers.de/beta/aaa_debug.ics");
				
					if(!toastMsg.equals(""))	{			
						this.sActivity.showToast(toastMsg, Toast.LENGTH_LONG);
					}
				} catch(FileNotFoundException fnfe) {
					toastMsg = (toastMsg.equals("")) ? sActivity.getString(R.string.toast_courselist_download_failed) : toastMsg;					
					this.sActivity.showToast(toastMsg, Toast.LENGTH_LONG);
				
					Logbook.e(fnfe);
				}
		}

		// Nach dem erfolgreichen Einlesen werden die Preferences der SettingsActivity vorbereitet
		sActivity.preparePreferences();
	}
	
	/**
	 * Liefert die Kursbezeichnungen als ArrayList zurück
	 * 
	 * @return
	 */
	public ArrayList<String> getCalendarList() {
		ArrayList<String> calendars = new ArrayList<String>();

		// Es wird aus der HashMap nur der Schlüssel benötigt
		for (Map.Entry<String, String> e : this.list.entrySet()) {
			calendars.add((String)e.getKey());
		}
		
		// Die Liste wird alphabetisch sortiert
		Collections.sort(calendars);
		
		return calendars;
	}

	/**
	 * Liefert die URL zur ICS-Datei des angegebenen Kurses
	 * 
	 * @param course Der Kurs, dessen URL zur ICS-Datei gesucht wird
	 * @return Die URL zur ICS-Datei
	 */
	public String getUrlFromCalendar(String course) {
		return this.list.get(course);
	}
	
	/**
	 * Liefert den Namen der ICS-Datei des angegebenen Kurses
	 * 
	 * @param course Der Kurs, dessen ICS-Dateiname gesucht wird
	 * @return Der ICS-Dateiname
	 */
	public String getCalendarFileName(String course) {
		try{
			String url = this.list.get(course);
			String[] lineArr = url.split("/");
			
			return lineArr[lineArr.length - 1];	
		} catch (NullPointerException npe) {
			Logbook.e(npe);
			return "";
		}
	}
}
