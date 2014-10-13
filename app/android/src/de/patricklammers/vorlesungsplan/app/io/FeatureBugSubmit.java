package de.patricklammers.vorlesungsplan.app.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import de.patricklammers.vorlesungsplan.app.InfoHelpActivity;
import de.patricklammers.vorlesungsplan.app.io.FileDownload.DownloadStatus;

public class FeatureBugSubmit {
	
	public enum SendingType {FEATURE, BUG}
	
	private static final String key = "690e04a53f89daf4f288fec36d83610d"; // 4eda0df47c2064b53185537ccfb533bf
//	private static final String key = "Hallo";
	private String mail = null;
	private String title = null;
	private String description = null;
	private SendingType type = null;
	private Integer versioncode = 0;
	private InfoHelpActivity activity = null;
	private String localPath;
	
	
	public FeatureBugSubmit(String mail, String title, String description, SendingType type, InfoHelpActivity activity) {
		this.mail = mail;
		this.title = title;
		this.description = description;
		this.type = type;
		this.activity = activity;
		try {
			this.versioncode = this.activity.getPackageManager().getPackageInfo(this.activity.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
		}
		this.localPath = ((Activity) this.activity).getApplication().getExternalFilesDir(null).getAbsolutePath();
	}
	
	@SuppressWarnings("unused")
	private String encodeString(final String message) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		try {
			Cipher ecipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			Encryption k = new Encryption(FeatureBugSubmit.key);
			ecipher.init(Cipher.ENCRYPT_MODE, k);
			byte[] b = ecipher.doFinal(message.getBytes());
			Charset c = Charset.forName("UTF-8");
			return new String(b, c);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void download() {
		try {
			// Initialisiere den Downloader
			FileDownload down = new FileDownload(this.activity, this.getClass().getMethod("initializeAfter", new Class[] { DownloadStatus.class }),this);
			down.setLocalPath(this.localPath + "/send.txt");
			down.setRemotePath(this.getUrl());
			// Starte den Download
			down.start();			
		} catch (NoSuchMethodException nsme) {
        	Logbook.e(nsme);
		}
	}
	
	/**
	 * Initialisiert die CalendarList nach dem Download
	 * 
	 * @param statusCode
	 */
	public void initializeAfter(DownloadStatus statusCode) {
		Scanner sc = null;
		String toastMsg = "";
		
		switch (statusCode) {
			case FAILED_NO_LOCALPATH:
			case FAILED_NO_REMOTEPATH:
				toastMsg = (toastMsg.equals("")) ? "Problem mit der Anwendung. Bitte die Entwickler kontaktieren!" : toastMsg;
			case FAILED_OTHER_REASON:
			case FAILED_NO_INTERNETCON:
				toastMsg = (toastMsg.equals("")) ? "Infos konnten nicht heruntergeladen werden!" : toastMsg;
			case SUCCESSFULL:
				toastMsg = (toastMsg.equals("")) ? "Infos wurden erfolgreich heruntergeladen!" : toastMsg;	
			case WITHOUT_DOWNLOAD:
				File f = new File(this.localPath + "/send.txt");
				
				try {
					sc = new Scanner(f, "UTF-8");
					String line = "";
					while (sc.hasNextLine()) {
						if (line.equals("")) {
							line += sc.nextLine();
						} else {
							line += "\n" + sc.nextLine();
						}
					}
				
					if(!toastMsg.equals(""))	{			
//						this.activity.showToast(toastMsg, Toast.LENGTH_LONG);
					}
					this.activity.afterSending(line);
				} catch(FileNotFoundException fnfe) {
					toastMsg = (toastMsg.equals("")) ? "Infos konnten nicht heruntergeladen werden!" : toastMsg;					
//					this.activity.showToast(toastMsg, Toast.LENGTH_LONG);
				
					Logbook.e(fnfe);
				}
		}
	}
	
	public String getUrl() {
		if (this.type == SendingType.BUG) {
			return "http://vorlesungsplan.patricklammers.de/bug.php?m=" + this.getMail() + "&t=" + this.getTitle() + "&d=" + this.getDescription() + "&v=" + this.versioncode.toString();
		}
		if (this.type == SendingType.FEATURE) {
			return "http://vorlesungsplan.patricklammers.de/feature.php?m=" + this.getMail() + "&t=" + this.getTitle() + "&d=" + this.getDescription() + "&v=" + this.versioncode.toString();
		}
		return null;
	}
	
	public void send() {
		this.download();
	}

	public String getMail() {
		return mail;
	}

	public String getTitle() {
		return title.replace(" ", "_").replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace("&", "_and_").replace("=", "_equal_");
	}

	public String getDescription() {
		return description.replace(" ", "_").replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace("&", "_and_").replace("=", "_equal_");
	}

}
