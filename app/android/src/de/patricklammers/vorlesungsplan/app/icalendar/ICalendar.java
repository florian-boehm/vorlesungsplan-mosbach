package de.patricklammers.vorlesungsplan.app.icalendar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.MainActivity;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.io.FileDownload;
import de.patricklammers.vorlesungsplan.app.io.FileDownload.DownloadStatus;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.io.RegisterServerOperation;
import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.StatusHelper.SH;

/**
 * Repräsentiert eine ICS-Kalenderdatei, indem die Events aus der Datei in einer
 * ArrayList gespeichert werden. Außerdem übernimmt diese Klasse das parsen der
 * ICS-Kalenderdatei
 * 
 * @author Patrick Lammers
 */
public class ICalendar {
	private final int defaultOffset = 0;
	private final String localPath;
	private String course = null;
	private String icsFile = null;
	private String icsFileUpdated = null;
	private String icsFileRemote = null;
	
	private ArrayList<Vevent> events = new ArrayList<Vevent>();
	private MainActivity mActivity;
	private Settings settings;
	
	private boolean refreshViewAfterReload = false;
	
	/**
	 * Erzeugt eine neue Instanz von ICalendar, die das Herunterladen der ICS-Datei anstößt
	 * und die Events des aktuell ausgewählten Kurses in einer ArrayList speichert
	 *  
	 * @param mActivity
	 * @param calendarList
	 */
	public ICalendar(MainActivity mActivity) {
		this.mActivity = mActivity;
		this.localPath = mActivity.getApplication().getExternalFilesDir(null).getAbsolutePath();
		this.settings = Settings.getInstance(this.mActivity);
		this.loadSettings();
		
		File d = new File(this.localPath);
		if (!d.exists()) {
			d.mkdir();
		}	
	}
	
	/**
	 * Läd die Werte aus den Settings neu ein
	 */
	private void loadSettings() {
		this.course = this.settings.getSetting("course");
		this.icsFile = this.localPath + "/" + this.settings.getSetting("icsFile");
		this.icsFileUpdated = this.localPath + "/updated_" + this.settings.getSetting("icsFile");
		this.icsFileRemote = this.settings.getSetting("icsFileRemote");		
	}
	
	/**
	 * 
	 * @param refreshViewAfterReload
	 * @param forceUpdate
	 */	
	public void reloadData(boolean refreshViewAfterReload, boolean forceUpdate) {
		this.refreshViewAfterReload = refreshViewAfterReload;
		this.loadSettings();
		
		if(SH.preconditionsForCalendarDownloadAreFulfilled(course,icsFile,icsFileUpdated,icsFileRemote)) {
			// Wenn die Kalenderdatei ...
			if (new File(this.icsFile).exists()) {
				// ... bereits existiert, dann wird geprüft, ob ein Update notwendig ist
				if(SH.calendarWasUpdatedOnce(settings)) {
					this.update(forceUpdate);
				} else {
					if(SH.calendarUpdateNecessary(settings, forceUpdate)) {
						this.update(forceUpdate);
					} else {
						initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
					}
				}
			} else {
				// ... nicht existiert, dann wird das Update erzwungen
				this.update(true);
			}
		}
	}
	
	/**
	 * Führt ein Update der ICS-Datei durch
	 */
	public void update(boolean forceUpdate) {
		if((!SH.wantsGCM(settings) && this.settings.getSetting("dontTryAgainCalendarDownload") == null) || forceUpdate) {
			try {
				// Initialisiere den Downloader
				FileDownload down = new FileDownload(this.mActivity, this.getClass().getMethod("initializeAfter", new Class[] { DownloadStatus.class }),this);
				down.setLocalPath(this.icsFileUpdated);
				down.setRemotePath(this.icsFileRemote);
				
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
	 * Initialisiert den ICalendar nach dem Download
	 * 
	 * @param statusCode
	 */
	public void initializeAfter(DownloadStatus statusCode) {
		// Text, der am Ende als Toast ausgegeben wird
		String toastMsg = "";
		
		// SimpleDateFormat vorbereiten
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY);
		Calendar cal = Calendar.getInstance(Locale.GERMANY);
		
		switch (statusCode) {
			case FAILED_NO_LOCALPATH:
			case FAILED_NO_REMOTEPATH:
				toastMsg = (toastMsg.equals("")) ? mActivity.getString(R.string.toast_app_problem) : toastMsg;
			case FAILED_OTHER_REASON:
			case FAILED_NO_INTERNETCON:				
				toastMsg = (toastMsg.equals("")) ? mActivity.getString(R.string.toast_lectureschedule_download_failed) : toastMsg;	
				// In allen vier Fällen, sollte bis zum Neustart der Anwendung nicht mehr versucht werden, den Kalender herunterzuladen
				settings.setSetting("dontTryAgainCalendarDownload", "true");	
			case SUCCESSFULL:
				toastMsg = (toastMsg.equals("")) ? mActivity.getString(R.string.toast_lectureschedule_download_succeeded) : toastMsg;	
			case WITHOUT_DOWNLOAD:
				// Dieser Teil wird IMMER ausgeführt, egal ob der Download erfolgreich war oder fehlgeschlagen ist
				// Schauen ob ein Update für den Kurskalender heruntergeladen wurde und dann das Update "einspielen"
				new ICalendarComperator(mActivity).start();
				
				// Wenn bisher noch keine toastMsg gesetzt wurde, dann wurde die ICS-Datei erfolgreich heruntergeladen
				if(toastMsg.equals(mActivity.getString(R.string.toast_lectureschedule_download_succeeded))) {
					this.settings.setSetting("lastCalRefresh", format.format(cal.getTime()));
				}
				
				// Wenn die Datei existiert ...
				if (new File(this.icsFile).exists()) {
					// ... dann wird sie eingelesen
					this.parse(this.icsFile);

					if(this.refreshViewAfterReload)	{
						this.mActivity.prepareCalendarLayout();
						this.refreshViewAfterReload = false;
					}

					if(!toastMsg.equals("")) {
						this.mActivity.showToast(toastMsg, Toast.LENGTH_LONG);
					}
				} else {
					toastMsg = (toastMsg.equals("")) ? mActivity.getString(R.string.toast_lectureschedule_download_failed) : toastMsg;					
					this.mActivity.showToast(toastMsg, Toast.LENGTH_LONG);
					
					// Wenn die Kurs-ICS-Datei nicht existiert, so wird der Kurs wieder aus den Einstellungen gelöscht
					RegisterServerOperation.unregister(settings.getSetting("regId")).start();
					this.settings.removeSetting("regId");
					this.settings.removeSetting("course");
					this.settings.removeSetting("icsFile");
					this.settings.removeSetting("icsFileRemote");
					
					this.mActivity.prepareCalendarLayout();
				}
		}
	}
	
	/**
	 * Ließt die ICS-Datei ein und speichert die Events in einer ArrayList
	 * 
	 * @param file Die ICS-Datei, die eingelesen werden soll
	 */
	private void parse(String file) {
		mActivity.showWaitDialog(mActivity.getString(R.string.download_dialog_waittext),
								mActivity.getString(R.string.parse_dialog_title));
		
		Boolean is_calendar = false;
		Boolean is_event = false;
		
		this.events.clear();
		
		try	{
			File f = new File(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

			String line = "";
			
			while((line = br.readLine()) != null) {
				String[] line_arr = line.split(":");
				if (!is_calendar && line_arr[0].equals("BEGIN") && line_arr[1].equals("VCALENDAR")) {
					is_calendar = true;
				}
				if (is_calendar && !is_event && line_arr[0].equals("BEGIN") && line_arr[1].equals("VEVENT")) {
					is_event = true;
					this.events.add(new Vevent());
				}
				
				if(is_event) {
					if (line_arr[0].equals("DTSTART;TZID=Europe/Berlin")) {
						this.events.get(this.getEventCount() - 1).setEventBegin(line_arr[1]);
					}
					else if (line_arr[0].equals("DTEND;TZID=Europe/Berlin")) {
						this.events.get(this.getEventCount() - 1).setEventEnd(line_arr[1]);
					}
					else if (line_arr[0].equals("LOCATION")) {
						this.events.get(this.getEventCount() - 1).setEventLocation(line_arr[1]);
					}
					else if (line_arr[0].equals("SUMMARY")) {
						this.events.get(this.getEventCount() - 1).setEventSummary(line_arr[1]);
					}
					else if (line_arr[0].equals("DESCRIPTION")) {
						this.events.get(this.getEventCount() - 1).setEventDescription(line_arr[1]);
					}
					else if (line_arr[0].equals("UID")) {
						if(line_arr.length == 3) {
							this.events.get(this.getEventCount() - 1).setUid(line_arr[1]+":"+line_arr[2]);
						} else {
							this.events.get(this.getEventCount() - 1).setUid(line_arr[1]);							
						}
					}
				}				
				
				if (is_calendar && is_event && line_arr[0].equals("END") && line_arr[1].equals("VEVENT")) {
					is_event = false;
				}
				if (is_calendar && !is_event && line_arr[0].equals("END") && line_arr[1].equals("VCALENDAR")) {
					is_calendar = false;
				}
			}
			br.close();

		} catch (FileNotFoundException fnfe) {
			Logbook.e(fnfe);
		} catch (IOException ioe)	{
			Logbook.e(ioe);
		}	
		
		mActivity.hideWaitDialog();
	}

	public int getEventCount() {
		return this.events.size();
	}
	
	public String getEventSummary(int id) {
		if (id < this.getEventCount() && id >= 0) {
			return this.events.get(id).getEventSummary();
		}
		return null;
	}
	
	public String getEventLocation(int id) {
		if (id < this.getEventCount() && id >= 0) {
			return this.events.get(id).getEventLocation();
		}
		return null;
	}
	
	public String getEventDescription(int id) {
		if (id < this.getEventCount() && id >= 0) {
			return this.events.get(id).getEventDescription();
		}
		return null;
	}
	
	public Date getEventBegin(int id) {
		if (id < this.getEventCount() && id >= 0) {
			return this.events.get(id).getEventBegin();
		}
		return null;
	}
	
	public Date getEventEnd(int id) {
		if (id < this.getEventCount() && id >= 0) {
			return this.events.get(id).getEventEnd();
		}
		return null;
	}
	
	public ArrayList<Vevent> getEventsBySummary(String summary) {
		ArrayList<Vevent> list = new ArrayList<Vevent>();
		
		int id = getNextEventIDBySummary(summary);
		if (id < 0) {
			return null;
		} else {
			list.add(this.events.get(id));
			id = getNextEventIDBySummary(summary, id + 1);
			while (id >= 0) {
				list.add(this.events.get(id));
				id = getNextEventIDBySummary(summary, id + 1);
			}
		}
		
		return list;
	}
	
	public int getNextEventIDBySummary(String summary) {
		return getNextEventIDBySummary(summary, defaultOffset);
	}
	
	public int getNextEventIDBySummary(String summary, int offset) {
		if (offset < this.getEventCount() && offset >= 0) {
			for (int i = offset; i < this.getEventCount(); i++) {
				if (this.events.get(i).getEventSummary().equals(summary)) {
					return i;
				}
			}
			return -1;
		}
		return -2;
	}
	
	public ArrayList<Vevent> getEvents(Date d1, Date d2) {
		ArrayList<Vevent> result = new ArrayList<Vevent>();
		
		for(Vevent v : events) {			
			if(d1 == null || (d1 != null && v.getEventEnd().compareTo(d1) >= 0)) {
				if(d2 == null || (d2 != null && v.getEventBegin().compareTo(d2) <= 0)) {
					result.add(v);						
				}
			}
		}
		
		return result;
	}
	
	public int getCountOfAllEvents() {
		return events.size()-1;
	}
}
