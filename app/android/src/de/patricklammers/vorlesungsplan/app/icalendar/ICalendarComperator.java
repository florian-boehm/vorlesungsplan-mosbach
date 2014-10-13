package de.patricklammers.vorlesungsplan.app.icalendar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import de.patricklammers.vorlesungsplan.app.MainActivity;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.settings.Settings;

/**
 * Diese Klasse vergleich zwei ICS-Kalenderdateien miteinander und findet heraus,
 * wie viele Termine gelöscht, modifiziert und hinzugefügt wurde. Bei modifizierten
 * Terminen kann sie feststellen, ob sich der Raum oder das Datum geändert haben.
 * 
 * @author Florian Schwab
 *
 */
public class ICalendarComperator extends Thread {
	
	public enum EventModifiedStatus { NEW, ROOM_CHANGED, DATE_CHANGED, REMOVED };
	private MainActivity mActivity = null;
	private File newFile = null;
	private File actFile = null;
	private File oldFile = null;
	private Settings settings;
	private NotificationManager mNotificationManager;
    public static final int NOTIFICATION_ID = 1;

	public ICalendarComperator(MainActivity mActivity) {
		// Bereite alle Variablen vor
		this.mActivity = mActivity;
		this.settings = Settings.getInstance(mActivity);
		this.newFile = new File(mActivity.getExternalFilesDir(null).getAbsolutePath() + "/updated_" + settings.getSetting("icsFile"));
		this.actFile = new File(mActivity.getExternalFilesDir(null).getAbsolutePath() + "/" + settings.getSetting("icsFile"));
		this.oldFile = new File(mActivity.getExternalFilesDir(null).getAbsolutePath() + "/old_" + settings.getSetting("icsFile"));
		
		// Wenn ein Update heruntergeladen wurde ...
		if(newFile.exists()) {
			// ... und aktuell eine Datei existiert ...
			if(actFile.exists()) {
				// ... dann mache die aktuelle Datei zur alten Datei 
				actFile.renameTo(oldFile);
			}
			
			// und die neue Datei zur aktuellen Datei
			newFile.renameTo(actFile);
		}
	}
	
	/**
	 * Der Vergleich der beiden Kalenderdateien erfolgt asynchron in dieser Methode.
	 * Zum Abschluss werden die Ergebnisse an die MainActivity übergeben, die diese
	 * dann darstellt.
	 */
	@Override
	public void run() {
		if (actFile.exists() && oldFile.exists()) {
			// Lösche die Notification, dass es ein Update gab
			mNotificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(NOTIFICATION_ID);
			
			// Vergleichen und Updateinformationen anzeigen
			Differences dif = compare(parse(oldFile), parse(actFile));
			
			if(dif != null && dif.addedCount + dif.modifiedCount + dif.removedCount > 0 && dif.events != null && dif.events.size() > 0) {
				mActivity.showUpdateNotificationBar(dif);
	    		showUpdateNotificationAndLight();
			}
			
			// Danach die alte Datei löschen
			this.oldFile.delete();
		}
	}

	/**
	 * Findet die Unterschiede zwischen beiden Kalenderdateien heraus.
	 * Die Methode erwartet die Events bereits in zwei ArrayListen,
	 * die eigentlichen Dateien müssen vorher also geparst werden.
	 * 
	 * @param oldEvents	Die ArrayList mit den alten Events
	 * @param newEvents	Die ArrayList mit den neuen Events
	 * @return Der Unterschied zwischen beiden Kalendern
	 */
	private Differences compare(ArrayList<Vevent> oldEvents, ArrayList<Vevent> newEvents) {
		// Initialisiere das Resultat-Objekt
		Differences result = new Differences();
		result.events = new HashMap<String, EventModifiedStatus>();
		
		ArrayList<Vevent> tempEvents = new ArrayList<Vevent>();
		final int size = newEvents.size();
		
		// Wenn beide Listen keine Events enthalten, dann kann es sein, dass Veränderungen
		// an vergangenen Events stattgefunden haben. Diese werden hier allerdings nicht
		// angzeigt.
		if(newEvents.size() + oldEvents.size() == 0) {
			Logbook.w("Both eventlists were empty!");
			return null;
		} else {
			// Vergleiche beide Arrays. Nehme dazu die Events aus dem größeren
			for(Vevent event : ((size > 0) ? newEvents : oldEvents)) {
				if(((size > 0) ? oldEvents : newEvents).contains(event)) {
					((size > 0) ? oldEvents : newEvents).remove(event);
					tempEvents.add(event);
				}
			}
			
			for(Vevent event : tempEvents) {
				((size > 0) ? newEvents : oldEvents).remove(event);
			}
		}
		
		// Die Anzahl der Events in der newEvents ArrayList wird vorerst als addedCount
		// und die Anzahl der Events in oldEvents als removedCount betrachtet.
		result.addedCount = newEvents.size();
		result.removedCount = oldEvents.size();
		result.modifiedCount = 0;
		
		Vevent innerEvent = null;
		tempEvents = new ArrayList<Vevent>();
		
		// Durch diese beiden Schleifen wird versucht, zusammengehörende Events zu finden,
		// bei denen sich entweder der Raum oder die Zeit geändert haben.
		// Das oldEvents Array wird in der äußeren Schleife verwendet, damit die Anzahl der
		// Schleifendurchläufe geringer ist, denn nur an Events aus oldEvents könnte eine
		// Veränderung vorgenommen worden sein.
		for(Vevent oldEvent : oldEvents) {
			innerEvent = null;
			
			for(Vevent newEvent : newEvents) {
				// Gleiche Events haben per Definition den gleichen Vorlesungsnamen und Dozent!
				if(newEvent.getEventDescription() != null && newEvent.getEventSummary() != null &&
						newEvent.getEventDescription().equals(oldEvent.getEventDescription()) &&
						newEvent.getEventSummary().equals(oldEvent.getEventSummary())) {
					// Vermindere addedCount und removedCount; erhöhe außerdem modifiedCount
					result.addedCount--;
					result.removedCount--;
					result.modifiedCount++;
					
					// Finde heraus, ob sich das Datum oder der Raumg geändert haben
					if(newEvent.getEventBegin().compareTo(oldEvent.getEventBegin()) == 0 && 
							newEvent.getEventEnd().compareTo(oldEvent.getEventEnd()) == 0 &&
							!newEvent.getEventLocation().equals(oldEvent.getEventLocation())) {
						result.events.put(newEvent.getUid(), EventModifiedStatus.ROOM_CHANGED);
					} else {
						result.events.put(newEvent.getUid(), EventModifiedStatus.DATE_CHANGED);
					}

					innerEvent = newEvent;
					break;
				}
			}
			
			// Wenn zwei zusammengehörige Events gefunden wurden, dann muss das Event aus newEvents
			// gelöscht werden, damit es nicht mehrmals verwendet wird.
			if(innerEvent != null) {
				// Füge es gleichzeitig in die temporäre ArrayListe um nachher die gelöschten Elemente zu erkennen
				tempEvents.add(oldEvent);
				newEvents.remove(innerEvent);
			}
		}
		
		// Die verbleibenden Events sind neu im Kalender
		for(Vevent newEvent : newEvents) {
			result.events.put(newEvent.getUid(), EventModifiedStatus.NEW);
		}
		
		// Die verbleibenden Events sind aus dem Kalender gelöscht worden im Kalender
		for(Vevent oldEvent : oldEvents) {
			if(!tempEvents.contains(oldEvent)) {
				result.events.put(oldEvent.getUid()+"|"+oldEvent.getEventSummary(), EventModifiedStatus.REMOVED);
			}
		}

		return result;
	}

	/**
	 * Ließt die ICS-Datei ein und speichert die Events in einer ArrayList. Diese
	 * Methode arbeitet der parse-Methode in ICalendar ähnlich, betrachtet allerdings
	 * von vornherein nur Events, die in der Zukunft liegen.
	 * 
	 * @param file Die ICS-Datei, die eingelesen werden soll
	 */
	private ArrayList<Vevent> parse(File icsFile) {
		ArrayList<Vevent> result = new ArrayList<Vevent>();
		Date now = Calendar.getInstance(Locale.GERMANY).getTime();
		Boolean is_calendar = false;
		Boolean is_event = false;

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(icsFile)));

			String line = "";
			Vevent event = null;

			while ((line = br.readLine()) != null) {
				String[] line_arr = line.split(":");
				if (!is_calendar && line_arr[0].equals("BEGIN") && line_arr[1].equals("VCALENDAR")) {
					is_calendar = true;
				}
				if (is_calendar && !is_event && line_arr[0].equals("BEGIN") && line_arr[1].equals("VEVENT")) {
					is_event = true;
					event = new Vevent();
				}

				if (is_event) {
					try {
						if (line_arr[0].equals("DTSTART;TZID=Europe/Berlin")) {
							event.setEventBegin(line_arr[1]);
							
							// Wenn das Event in der Vergangenheit liegt, wird es beim Vergleich nicht beachtet
							if(event.getEventBegin().compareTo(now) <= 0) {
								is_event = false;
								continue;
							}
						} else if (line_arr[0].equals("DTEND;TZID=Europe/Berlin")) {
							event.setEventEnd(line_arr[1]);
						} else if (line_arr[0].equals("LOCATION")) {
							event.setEventLocation(line_arr[1]);
						} else if (line_arr[0].equals("SUMMARY")) {
							event.setEventSummary(line_arr[1]);
						} else if (line_arr[0].equals("DESCRIPTION")) {
							event.setEventDescription(line_arr[1]);
						} else if (line_arr[0].equals("UID")) {
							if (line_arr.length == 3) {
								event.setUid(line_arr[1] + ":" + line_arr[2]);
							} else {
								event.setUid(line_arr[1]);
							}
						}
					} catch (ArrayIndexOutOfBoundsException aioobe) {
						// Kann passieren
						Logbook.w(aioobe);
					}
				}

				if (is_calendar && is_event && line_arr[0].equals("END") && line_arr[1].equals("VEVENT")) {
					result.add(event);
					is_event = false;
				}
				if (is_calendar && !is_event && line_arr[0].equals("END") && line_arr[1].equals("VCALENDAR")) {
					is_calendar = false;
				}
			}
			
			br.close();
		} catch (FileNotFoundException fnfe) {
			Logbook.e(fnfe);
		} catch (IOException ioe) {
			Logbook.e(ioe);
		} 
		
		return result;
	}
	
    private void showUpdateNotificationAndLight() {
        mNotificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);;

        Intent mainActivityIntent = new Intent(mActivity, MainActivity.class);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		} else {
			mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
        
        PendingIntent contentIntent = PendingIntent.getActivity(mActivity, 0, mainActivityIntent, 0);

        String colorCode = Settings.getInstance(mActivity.getApplicationContext()).getSetting("notificationColor");
		colorCode = (colorCode == null) ? "#e2001a" : colorCode;
		int colorValue = Color.parseColor(colorCode);		
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mActivity)
	        .setSmallIcon(R.drawable.ic_notification)
	        .setContentTitle(mActivity.getString(R.string.app_name))
	        .setStyle(new NotificationCompat.BigTextStyle().bigText(mActivity.getString(R.string.notification_update_msg)))
	        .setContentText(mActivity.getString(R.string.notification_update_msg))
	        .setAutoCancel(true)
	        .setContentIntent(contentIntent)
	        .setLights(0x00FFFFFF & colorValue, 1000, 1000);
        
        Notification notification = mBuilder.build();

        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }
	
	public class Differences {
		public int addedCount = 0;
		public int removedCount = 0;
		public int modifiedCount = 0;
		public HashMap<String, EventModifiedStatus> events = null;		
	}
}
