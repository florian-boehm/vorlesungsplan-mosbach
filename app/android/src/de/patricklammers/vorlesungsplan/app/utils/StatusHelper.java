package de.patricklammers.vorlesungsplan.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.settings.Settings;

public class StatusHelper {

	public static boolean registeredSuccessfully(String gcmOperationResult) {
		return (gcmOperationResult != null && (gcmOperationResult.equals("DEV_REGISTER_SUCCESSFULL") || gcmOperationResult.equals("DEV_UPDATE_SUCCESSFULL")));
	}

	public static boolean unregisteredSuccessfully(String gcmOperationResult) {
		return (gcmOperationResult != null && (gcmOperationResult.equals("DEV_UNREGISTER_SUCCESSFULL") || gcmOperationResult.equals("DEV_NOT_EXISTING")));
	}

	public static boolean noCourseSelected(Settings settings) {
		if(settings == null)
			return true;
		
		return (settings.getSetting("course") == null || settings.getSetting("course").equals(""));
	}

	public static boolean regIdAndCourseDontExist(Settings settings) {
		if(settings == null)
			return false;
		
		return !(settings.getSetting("regId") != null && settings.getSetting("course") != null);
	}
	
	public static boolean wantsGCM(Settings settings) {
		if(settings == null)
			return false;
		
		return (settings.getSetting("regId") != null && !settings.getSetting("regId").equals(""));
	}

	public static boolean preconditionsForCalendarDownloadAreFulfilled(String course, String icsFile, String icsFileUpdated, String icsFileRemote) {
		return (course != null && !course.equals("") && icsFile != null && !icsFile.equals("") && icsFileUpdated != null && !icsFileUpdated.equals("") && icsFileRemote != null && !icsFileRemote.equals(""));
	}
	
	public static boolean calendarWasUpdatedOnce(Settings settings) {
		if(settings == null)
			return false;
		
		return (settings.getSetting("lastCalRefresh") == null || settings.getSetting("lastCalRefresh").equals(""));
	}

	public static boolean calendarUpdateNecessary(Settings settings, boolean forceUpdate) {
		// SimpleDateFormat vorbereiten
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY);
		Calendar cal = Calendar.getInstance(Locale.GERMANY);
		
		try	{
			Date lastCalRefreshDate = format.parse(settings.getSetting("lastCalRefresh"));
			Date now = cal.getTime();
			cal.setTime(lastCalRefreshDate);
			
			if(settings.getSetting("calRefreshInterval") == null || settings.getSetting("calRefreshInterval").equals(""))	{
				cal.add(Calendar.MINUTE, 10);							
			} else {
				String refreshInterval = settings.getSetting("calRefreshInterval");

				String days = refreshInterval.substring(0, refreshInterval.indexOf('-'));
				String time = refreshInterval.substring(refreshInterval.indexOf('-')+1);
				String hours = time.substring(0,2);
				String minutes = time.substring(2);
				
				cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(days));
				cal.add(Calendar.HOUR, Integer.parseInt(hours));
				cal.add(Calendar.MINUTE, Integer.parseInt(minutes));
			}
			
			Date dateToUpdate = cal.getTime();

			return (dateToUpdate.compareTo(now) <= 0 || forceUpdate);
		}
		catch (ParseException e) {
			Logbook.e("Error while parsing 'lastCalRefresh'");
			return true;
		}
	}
	
	public static boolean preconditionsForCourseListDownloadAreFulfilled(Settings settings) {
		return (settings.getSetting("lastListRefresh") != null && !settings.getSetting("lastListRefresh").equals(""));
	}
	
	public static boolean courseListUpdateNecessary(Settings settings) {
		try	{
			// SimpleDateFormat und Calendar vorbereiten
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.GERMANY);
			Calendar cal = Calendar.getInstance(Locale.GERMANY);
			
			// hier wird überprüft, ob ein Update notwendig ist (Aktualisierungsintervall: 1 Monat)
			Date lastListRefreshDate = format.parse(settings.getSetting("lastListRefresh"));
			Date now = cal.getTime();
			
			cal.setTime(lastListRefreshDate);
			cal.add(Calendar.MONTH, 1);
			Date dayToUpdate = cal.getTime();
			
			return (dayToUpdate.compareTo(now) <= 0);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Logbook.e("Error while parsing 'lastListRefresh'");
			return true;
		}
	}

	public static boolean lastUpdate14DaysInPast(Settings settings) {
		// SimpleDateFormat vorbereiten
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY);
		Calendar cal = Calendar.getInstance(Locale.GERMANY);
		
		if(settings.getSetting("lastlastCalRefresh") == null) {
			return true;
		}
		
		try	{
			Date lastCalRefreshDate = format.parse(settings.getSetting("lastCalRefresh"));
			Date now = cal.getTime();
			cal.setTime(lastCalRefreshDate);
			cal.add(Calendar.DATE, 14);
			Date dateToUpdate = cal.getTime();

			return (dateToUpdate.compareTo(now) <= 0);
		}
		catch (ParseException e) {
			Logbook.e("Error while parsing 'lastCalRefresh'");
			return true;
		}
	}
	
	public static boolean regIdOnServerMismatchesTheRegIdOnTheDevice(Settings settings, String regIdOnServer) {
		return regIdOnServer != null && settings.getSetting("regId") != null && !settings.getSetting("regId").equals("") && !regIdOnServer.equals(settings.getSetting("regId"));
	}
	
	public class SH extends StatusHelper {
		
	}
}
