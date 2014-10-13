package de.patricklammers.vorlesungsplan.app.lunch;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import android.app.Activity;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.LunchActivity;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.io.FileDownload;
import de.patricklammers.vorlesungsplan.app.io.FileDownload.DownloadStatus;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.settings.Settings;

public class LunchPlan {
	private String[]		monday 		= new String[2];
	private String[] 		tuesday 	= new String[2];
	private String[] 		wednesday 	= new String[2];
	private String[] 		thursday 	= new String[2];
	private String[] 		friday 		= new String[2];
	
	private final String 	src 		= "http://vorlesungsplan.patricklammers.de/speiseplan/speiseplan.php";
	private final String 	localPath;
	private LunchActivity 	lActivity	= null;
	private Settings 		config 		= null;
	
	public LunchPlan(LunchActivity activity) {
		this.lActivity = activity;
		
		this.config = Settings.getInstance(activity);
		
		this.localPath = ((Activity) this.lActivity).getApplication().getExternalFilesDir(null).getAbsolutePath();
		File d = new File(this.localPath);
		
		if (!d.exists()) {
			d.mkdir();
		}
	}
	
	public void reloadData() {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY);
		Calendar nextRefresh = Calendar.getInstance(Locale.GERMANY);
		Calendar refreshBegin = Calendar.getInstance(Locale.GERMANY);
		Calendar refreshEnd = Calendar.getInstance(Locale.GERMANY);
		
		if (new File(this.localPath + "/speiseplan.txt").exists()) {
			if(this.config.getSetting("lastLunchRefresh") == null || this.config.getSetting("lastLunchRefresh").equals("")) {
				this.update(true);
			} else {
				try	{
					Date lastLunchRefresh = format.parse(this.config.getSetting("lastLunchRefresh"));
					Date now = nextRefresh.getTime();
					nextRefresh.setTime(lastLunchRefresh);
					
					if(this.config.getSetting("lastLunchRefresh") == null || this.config.getSetting("lastLunchRefresh").equals(""))	{
						this.update(true);
					} else {
						SimpleDateFormat format2 = new SimpleDateFormat("EEEE", Locale.GERMANY);
						Logbook.d(format2.format(now));
						
						if (format2.format(now).equals("Montag")) { // zw. 6 und 14:30 alle halbe stunde
							refreshBegin.set(Calendar.HOUR, 6);
							refreshBegin.set(Calendar.MINUTE, 0);
							refreshBegin.set(Calendar.SECOND, 0);
							refreshEnd.set(Calendar.HOUR, 14);
							refreshEnd.set(Calendar.MINUTE, 30);
							refreshEnd.set(Calendar.SECOND, 0);
							
							if(refreshBegin.getTime().compareTo(now) <= 0 && now.compareTo(refreshEnd.getTime()) <= 0) {
								nextRefresh.add(Calendar.MINUTE, 30);

								if(nextRefresh.getTime().compareTo(now) <= 0) {
									this.update(true);
								} else {
									initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
								}
							} else {
								if(refreshBegin.getTime().compareTo(now) <= 0 && lastLunchRefresh.compareTo(refreshBegin.getTime()) <= 0) {
									this.update(true);
								} else {
									initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
								}
							}
						} else if (!format2.format(now).equals("Samstag") && !format2.format(now).equals("Sonntag")) { // um 9
							refreshBegin.set(Calendar.HOUR, 9);
							refreshBegin.set(Calendar.MINUTE, 0);
							refreshBegin.set(Calendar.SECOND, 0);

							if(refreshBegin.getTime().compareTo(now) <= 0 && lastLunchRefresh.compareTo(refreshBegin.getTime()) <= 0) {
								this.update(true);
							} else {
								initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
							}
						} else { // am WE garnicht
							initializeAfter(DownloadStatus.WITHOUT_DOWNLOAD);
							// NICHTS!!
						}
					}
				} catch (ParseException e) {
					Logbook.e("Error while parsing 'lastLunchRefresh'");
				} catch (Exception e) {
					Logbook.e(e);
				}
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
		if(forceUpdate) {
			try {
				// Initialisiere den Downloader
				FileDownload down = new FileDownload(this.lActivity, this.getClass().getMethod("initializeAfter", new Class[] { DownloadStatus.class }),this);
				down.setLocalPath(this.localPath + "/speiseplan.txt");
				down.setRemotePath(src);
				// Starte den Download
				down.start();			
			} catch (NoSuchMethodException nsme) {
	        	Logbook.e(nsme);
			}	
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
		String toastMsg = "";
		
		switch (statusCode) {
			case FAILED_NO_LOCALPATH:
			case FAILED_NO_REMOTEPATH:
				toastMsg = (toastMsg.equals("")) ? lActivity.getString(R.string.toast_app_problem) : toastMsg;
			case FAILED_OTHER_REASON:
			case FAILED_NO_INTERNETCON:
				toastMsg = (toastMsg.equals("")) ? lActivity.getString(R.string.toast_lunchplan_download_failed) : toastMsg;
			case SUCCESSFULL:
				toastMsg = (toastMsg.equals("")) ? lActivity.getString(R.string.toast_lunchplan_download_succeeded) : toastMsg;	
			case WITHOUT_DOWNLOAD:
				File f = new File(this.localPath + "/speiseplan.txt");
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY);
				Calendar cal = Calendar.getInstance(Locale.GERMANY);
				
				if(toastMsg.equals(lActivity.getString(R.string.toast_lunchplan_download_succeeded))) {
					if (this.config.setSetting("lastLunchRefresh", sdf.format(cal.getTime()))) {
						Logbook.d("Setting saved");
					} else {
						Logbook.d("Setting not saved");
					}
				}
				
				try {
					sc = new Scanner(f, "UTF-8");
				
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						String[] lineArr = line.split(":");
						if (!lineArr[0].equals("")) {
							if (lineArr[0].contains("Montag")) {
								this.monday[0] = lineArr[0];
								this.monday[1] = lineArr[1];
							}
							if (lineArr[0].contains("Dienstag")) {
								this.tuesday[0] = lineArr[0];
								this.tuesday[1] = lineArr[1];
							}
							if (lineArr[0].contains("Mittwoch")) {
								this.wednesday[0] = lineArr[0];
								this.wednesday[1] = lineArr[1];
							}
							if (lineArr[0].contains("Donnerstag")) {
								this.thursday[0] = lineArr[0];
								this.thursday[1] = lineArr[1];
							}
							if (lineArr[0].contains("Freitag")) {
								this.friday[0] = lineArr[0];
								this.friday[1] = lineArr[1];
							}
						}
					}
				
					if(!toastMsg.equals(""))	{			
						this.lActivity.showToast(toastMsg, Toast.LENGTH_LONG);
					}
					this.lActivity.prepareListData();
				} catch(FileNotFoundException fnfe) {
					toastMsg = (toastMsg.equals("")) ? lActivity.getString(R.string.toast_lunchplan_download_failed) : toastMsg;					
					this.lActivity.showToast(toastMsg, Toast.LENGTH_LONG);
				
					Logbook.e(fnfe);
				}
		}
	}

	
	public String getMonday(int field) {
		return monday[field];
	}

	
	public String getTuesday(int field) {
		return tuesday[field];
	}

	
	public String getWednesday(int field) {
		return wednesday[field];
	}

	
	public String getThursday(int field) {
		return thursday[field];
	}

	
	public String getFriday(int field) {
		return friday[field];
	}
	
}
