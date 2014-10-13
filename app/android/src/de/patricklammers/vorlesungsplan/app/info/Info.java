package de.patricklammers.vorlesungsplan.app.info;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import android.app.Activity;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.InfoHelpActivity;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.io.FileDownload;
import de.patricklammers.vorlesungsplan.app.io.FileDownload.DownloadStatus;
import de.patricklammers.vorlesungsplan.app.io.Logbook;

public class Info {
	private ArrayList<HelpFeature> helpFeature = new ArrayList<HelpFeature>();
	
	private final String src = "http://vorlesungsplan.patricklammers.de/info.php";
	private final String localPath;
	private InfoHelpActivity ihActivity;
	private Integer actualVersionCode;
	
	public Info(InfoHelpActivity activity) {
		this.ihActivity = activity;
		this.localPath = ((Activity) this.ihActivity).getApplication().getExternalFilesDir(null).getAbsolutePath();
		File d = new File(this.localPath);
		
		if (!d.exists()) {
			d.mkdir();
		}
	}
		
	/**
	 * Führt ein Update der Infoseite durch
	 */
	public void update(boolean forceUpdate) {
		if(forceUpdate) {
			try {
				// Initialisiere den Downloader
				FileDownload down = new FileDownload(this.ihActivity, this.getClass().getMethod("initializeAfter", new Class[] { DownloadStatus.class }),this);
				down.setLocalPath(this.localPath + "/info.txt");
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
				toastMsg = (toastMsg.equals("")) ? ihActivity.getString(R.string.toast_app_problem) : toastMsg;
			case FAILED_OTHER_REASON:
			case FAILED_NO_INTERNETCON:
				toastMsg = (toastMsg.equals("")) ? ihActivity.getString(R.string.toast_infos_download_failed) : toastMsg;
			case SUCCESSFULL:
				toastMsg = (toastMsg.equals("")) ? ihActivity.getString(R.string.toast_infos_download_succeeded) : toastMsg;	
			case WITHOUT_DOWNLOAD:
				File f = new File(this.localPath + "/info.txt");
				
				try {
					sc = new Scanner(f, "UTF-8");
				
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						String[] lineArr = line.split("###");
						if (!lineArr[0].equals("")) {
							if (lineArr[0].equals("version")) {
								this.actualVersionCode = Integer.parseInt(lineArr[1]);
							}
							if (lineArr[0].equals("help")) {
								this.helpFeature.add(new HelpFeature(lineArr[1], lineArr[2]));
							}
							if (lineArr[0].equals("feature")) {
								this.helpFeature.add(new HelpFeature(lineArr[1], lineArr[2], lineArr[3], lineArr[4]));
							}
						}
					}
				
					if(!toastMsg.equals(""))	{			
						this.ihActivity.showToast(toastMsg, Toast.LENGTH_LONG);
					}
					this.ihActivity.prepareListData();
				} catch(FileNotFoundException fnfe) {
					toastMsg = (toastMsg.equals("")) ? ihActivity.getString(R.string.toast_infos_download_failed) : toastMsg;					
					this.ihActivity.showToast(toastMsg, Toast.LENGTH_LONG);
				
					Logbook.e(fnfe);
				}
		}
	}
	
	public Integer getActualVersionCode() {
		return actualVersionCode;
	}

	public Integer getSize() {
		return this.helpFeature.size();
	}
	
	public HelpFeature getItem(Integer position) {
		return this.helpFeature.get(position);
	}
	
}
