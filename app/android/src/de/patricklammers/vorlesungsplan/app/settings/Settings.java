package de.patricklammers.vorlesungsplan.app.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import android.content.Context;
import de.patricklammers.vorlesungsplan.app.io.Logbook;

/**
 * Diese Klasse stellt eine Schnittstelle auf unser Settings-File bereit.
 * 
 * @author Patrick Lammers (Basis-Implementierung) & Florian Schwab (Singelton-Implementierung)
 */
public class Settings {
	// Die Singelton Instanz
	private static Settings settingsInstance = null;
	public static final String DELIMITER = "\t";
	
	private String path = "";
	private final String file = "settings.conf";
	private HashMap<String, String> conf = new HashMap<String, String>();
		
	// Privater Konstruktor
	private Settings(Context context) {
		// Den Pfad zum App-spezifischen Ordner auf der SD-Karte abrufen
		this.path = context.getExternalFilesDir(null).getAbsolutePath();
		File d = new File(this.path);
		if (!d.exists()) {
			d.mkdir();
		}
		
		// Überprüfen, ob bereits ein Settings-File existiert. Ansonsten wird ein neues angelegt
		d = new File(this.path + "/" + this.file);
		if (!d.exists()) {
			try {
				d.createNewFile();
			} catch (IOException ioe) {
				Logbook.e(ioe);
			}
		}
		
		// Das Settings-File wird eingelesen
		Scanner sc = null;
		try {
			sc = new Scanner(d);
			
			// Solange zeilenweise lesen, wie es noch ungelesene Zeilen gibt
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				String[] lineArr = line.split(DELIMITER);
				this.conf.put(lineArr[0], lineArr[1]);
			}
			
			sc.close();
		} catch(FileNotFoundException fnfe) {
			// Die Exception sollte eigentlich nicht auftreten
			Logbook.e(fnfe);
		}		
	}
	
	/**
	 * Liefer die Settings-Instanz, mit der auf das Settings-File
	 * lesend und schreibend zugegriffen werden kann
	 * 
	 * @param context Die Activity, die die Settings-Instanz beinhaltet, muss eigentlich nur beim ersten Mal angegeben werden
	 */
	public synchronized static Settings getInstance(Context context) {
		// Wenn diese Methode das erste Mal aufgerufen wird
		if(settingsInstance == null){
			// Es wird eine Activity benötigt, ansonsten kann nicht fortgefahren werden
			if(context == null) {
				return null;
			}
			
			settingsInstance = new Settings(context);
		}
		
		return settingsInstance;	
	}
	
	/**
	 * Ließt den Wert zu einem gegebenen Schlüssel aus dem Settings-File
	 * 
	 * @param key Der Schlüssel nachdem gesucht werden soll
	 * @return Der zum Schlüssel passende Wert als String, oder null, wenn
	 * 			der Schlüssel nicht gefunden wurde
	 */
	public synchronized String getSetting(String key) {
		if (this.conf.containsKey(key)) {
			return (String)this.conf.get(key);
		}
		return null;
	}
	
	/**
	 * Speichert einen Wert mit einem Schlüssel im Settings-File
	 * 
	 * @param key
	 * @param value 
	 * @return true wenn das speichern erfolgreich war; false wenn nicht
	 */
	public synchronized boolean setSetting(String key, String value) {
		if (key != null && !key.equals("") && value != null && !value.equals("")) {
			this.conf.put(key, value);
			return this.save();
		}
		
		return false;
	}
	
	/**
	 * Speichert die Settings-Map in das Settings-File
	 * 
	 * @return true wenn das Speichern erfolgreich war; false wenn nicht
	 */
	private boolean save() {
		File f = new File(this.path + "/" + this.file);
		FileWriter writer = null;
		try {
			writer = new FileWriter(f, false);
			for (Map.Entry<String, String> e : this.conf.entrySet()) {
				writer.write(e.getKey() + DELIMITER + e.getValue());
				writer.write(System.getProperty("line.separator"));
			}
			writer.flush();			   
			writer.close();
			
			return true;
		} catch (IOException ioe) {
	    	Logbook.e(ioe);
	    	return false;
	    }
	}
	
	/**
	 * Löscht den Wert aus der Konfigurationsdatei
	 * 
	 * @param key Der Schlüssel des zu löschenden Wertes
	 */	
	public synchronized boolean removeSetting(String key) {
		if(key != null && !key.equals("")) {
			this.conf.remove(key);
			return this.save();
		} else {
			return false;
		}
	}
}
