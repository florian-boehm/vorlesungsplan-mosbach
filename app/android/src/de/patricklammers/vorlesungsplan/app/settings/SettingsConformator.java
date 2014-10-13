package de.patricklammers.vorlesungsplan.app.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.interfaces.DialogAndToastActivity;
import de.patricklammers.vorlesungsplan.app.io.Logbook;

public class SettingsConformator {
	private static final String toastMsg = "Die Einstellungen mussten leider aufgrund eines Fehlers zurückgesetzt werden!";
	
	/**
	 * Überprüft, ob das Settings-File in einem konsistenten Zustand für diese Version der 
	 * DHBW-Mosbach App ist. Inkonsistente Settings-Files werden versucht zu migrieren. Ist
	 * dies nicht möglich, so muss das Settings-File komplett gelöscht werden.
	 * 
	 * @param activity
	 * @return Wenn das Settingsfile konsistent ist oder nicht existiert, dann wird true zurückgeliefert
	 */
	@SuppressLint("DefaultLocale")
	public static boolean confirmFile(DialogAndToastActivity activity) {		
		// Den Pfad zum App-spezifischen Ordner auf der SD-Karte abrufen
		String appFolderPath = ((Activity) activity).getApplication().getExternalFilesDir(null).getAbsolutePath();
		
		// Wenn der App-Folder nicht besteht, dann wird er angelegt
		File appFolder = new File(appFolderPath);
		if (!appFolder.exists()) {
			appFolder.mkdir();
		}
				
		// Überprüfen, ob bereits ein Settings-File existiert
		File settingsFile = new File(appFolderPath + "/settings.conf");
		if (!settingsFile.exists()) {
			// Wenn kein Settings-File existiert, dann kann der Conformator beendet werden
			return true;
		}
				
		// aktuelle Version herausfinden
		String actVersionCode = null;
		
		try {
			actVersionCode = String.format("%07d", ((Activity) activity).getPackageManager().getPackageInfo(((Activity) activity).getPackageName(), 0).versionCode);
		} catch (NameNotFoundException nnfe) {
			Logbook.e(nnfe);
		}
		
		if(actVersionCode == null) {
			return false;
		}
		
		// Suche nativ nach einer Zeile, die das Schlüsselwort "versionCode" enthält		
		String oldVersionCode = "";
		
		try {
			Scanner reader = new Scanner(settingsFile);
			int lineCount = 0;
			
			while (reader.hasNextLine()) {
				lineCount++;
				String line = reader.nextLine();

				if(line.contains("versionCode")) {
					// Versuche nun den Wert des versionCodes auszuschneiden; unabhängig von Trennzeichen
					String cut = "";
					
					int i = 0;
					
					for(i = 0; i <= line.length()-7; i++) {
						cut = line.substring(i, i+7);

						if(cut.matches("\\d{7}")) {
							// Wenn 7 Digits gefunden wurde, interpetieren wir diese als versionCode
							oldVersionCode = cut;
						}
					}
					
					if(i + 7 == line.length() && oldVersionCode.isEmpty()) {
						// Wenn im Settings-File zwar "versionCode" aber keine Zahlen stehen,
						// dann ist das Settings-File inkonsistent
						activity.showToast(toastMsg, Toast.LENGTH_LONG);
						reader.close();
						return settingsFile.delete();
					}
				}
			}
			
			reader.close();
			
			if(lineCount == 0) {
				return true;
			}
		} catch(FileNotFoundException fnfe) {
			// Die Exception sollte eigentlich nie auftreten
			Logbook.e(fnfe);
		}
		
		// Wenn keine Version gefunden wurde, dann ist die App älter als 0000404
		// Vor Version 0000403 war ":" das Trennzeichen im Settings-File
		// Seit Version 0000403 ist "\t" das Trennzeichen im Settings-File
		if(oldVersionCode.isEmpty()) {
			activity.showWaitDialog("Einstellungen werden angepasst", "Bitte warten ...");
			
			// Versuche die Version am Trennzeichen zu erkennen
			try {
				Scanner reader = new Scanner(settingsFile);
				
				while (reader.hasNextLine()) {
					
					String line = reader.nextLine();

					// Wenn ein Tabulatorzeichen enthalten ist, dann gehen wir davon aus, dass es Version 0000403 ist
					if(line.contains("\t")) {
						oldVersionCode = "0000403";
						break;
					} else {
						oldVersionCode = "0000402";
						break;
					}
				}
				
				reader.close();
			} catch(FileNotFoundException fnfe) {
				// Die Exception sollte eigentlich nie auftreten
				Logbook.e(fnfe);
			}
			
			// Wenn immer noch keine Version feststeht, dann ist das Settings-File inkonsistent
			if(oldVersionCode.isEmpty()) {
				activity.showToast(toastMsg, Toast.LENGTH_LONG);
				return settingsFile.delete();
			} 
			
			// Migration von 0000402 zu 0000403
			if(oldVersionCode.equals("0000402") && Integer.parseInt(actVersionCode) > Integer.parseInt(oldVersionCode)) {
				Logbook.d("Migrate from 0000402 to 0000403");
				// Ersetze den jeweils ersten Doppelpunkt in einer Zeile durch einen Tabulator
				File settingsFileBackup = new File(appFolderPath + "/settings.conf.bak");
				boolean status = settingsFile.renameTo(settingsFileBackup);
				
				if(status) {
					settingsFile = new File(appFolderPath + "/settings.conf");
					
					// Kopiere die alten Zeile, ersetze den Delimiter und speichere die Zeile		
					try {
						FileWriter writer = new FileWriter(settingsFile, false);	
						Scanner reader = new Scanner(settingsFileBackup);
						
						while(reader.hasNextLine()) {
							String line = reader.nextLine();
							line = line.replaceFirst(":", "\t");
							writer.write(line);
							writer.write(System.getProperty("line.separator"));
						}
						
						writer.flush();
						writer.close();
						reader.close();
						oldVersionCode = "0000403";
						settingsFileBackup.delete();
					}  catch(FileNotFoundException fnfe) {
						Logbook.e(fnfe);
					} catch (IOException ioe) {
						Logbook.e(ioe);
					}
				} else {
					activity.showToast(toastMsg, Toast.LENGTH_LONG);
					return settingsFile.delete();
				}				
			}

			// Migration von 0000403 zu 0000404
			if(oldVersionCode.equals("0000403") && Integer.parseInt(actVersionCode) > Integer.parseInt(oldVersionCode)) {
				Logbook.d("Migrate from 0000403 to 0000404");
				Settings s = Settings.getInstance((Activity) activity);
				s.setSetting("versionCode", "0000404");
				oldVersionCode = "0000404";
			}
		} else {
			// VersionsCode wurde im Settings-File gefunden ...
			// Noch nichts zu tun
			/* TEMPLATE:
			if(oldVersionCode.equals("0000404") && Integer.parseInt(actVersionCode) > Integer.parseInt(oldVersionCode)) {
				Settings s = Settings.getInstance((Activity) activity);
				s.setSetting("versionCode", "0000405");
				oldVersionCode = "0000405";
			}*/
			// Migration von 0000404 zu 0000500
			if(oldVersionCode.equals("0000404") && Integer.parseInt(actVersionCode) > Integer.parseInt(oldVersionCode)) {
				Settings s = Settings.getInstance((Activity) activity);
				s.setSetting("versionCode", "0000500");
				oldVersionCode = "0000500";
			}
		}
		
		activity.hideWaitDialog();		
		return true;
	}
}
