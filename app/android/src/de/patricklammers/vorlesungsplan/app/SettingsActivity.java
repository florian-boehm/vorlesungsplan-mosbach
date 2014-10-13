package de.patricklammers.vorlesungsplan.app;

import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;

import de.patricklammers.vorlesungsplan.app.icalendar.CourseList;
import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;
import de.patricklammers.vorlesungsplan.app.interfaces.DialogAndToastActivity;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.io.RegisterServerOperation;
import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.StatusHelper.SH;

/**
 * Die SettingsActivity managed die Auswahl der verschiedenen Einstellungen
 * und das Abspeichern der gewählten Einstellungen im Settings-File
 * 
 * @author Florian Schwab
 */
public class SettingsActivity extends ActionBarPreferenceActivity implements OnSharedPreferenceChangeListener, DialogAndToastActivity
{
	// Die Preferences, die der Benutzer verändern kann	
	private ListPreference courseListPref = null;
	private ListPreference refreshIntervalPref = null;
	private ListPreference languagePref = null;
	private CheckBoxPreference useNotifierPref = null;
	
	// Werte für das Aktualisierungsinterval
	// Dieses Feld wird im Konstruktor überschrieben!
	private CharSequence[] intervalEntries = {	"5 Minuten", "15 Minuten", "30 Minuten",
												"1 Stunde", "2 Stunden", "6 Stunden", "12 Stunden"};
	private CharSequence[] intervalPref = {	"00-0005", "00-0015", "00-0030",
											"00-0100", "00-0200", "00-0600", "00-1200"};
	private CharSequence[] intervalValues = {"0","1","2","3","4","5","6"};
	private ProgressDialog progressDialog = null;
	private Handler handler = null;
	private CourseList courseList = null;
	private Settings settings = null;
	private SharedPreferences config = null;
	private String oldCourse = "";
	private String courseOnLoad = null;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// Bei älteren Android-Versionen vor Honeycomb muss die Actionbar manuell angefordert werden
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		}
        
		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);		
		
		// Setze die Werte der Sprache entsprechend ein
		if(intervalEntries.length == 7) {
			this.intervalEntries[0] = this.getString(R.string.settings_5_min);
			this.intervalEntries[1] = this.getString(R.string.settings_15_min);
			this.intervalEntries[2] = this.getString(R.string.settings_30_min);
			this.intervalEntries[3] = this.getString(R.string.settings_1_h);
			this.intervalEntries[4] = this.getString(R.string.settings_2_h);
			this.intervalEntries[5] = this.getString(R.string.settings_6_h);
			this.intervalEntries[6] = this.getString(R.string.settings_12_h);			
		}
		
		// Den Handler festlegen
        handler = new Handler();
        
        settings = Settings.getInstance(this);
        
		// Event-Handler registrieren, der beim Ändern von Einstellungen aktiv wird
        config = PreferenceManager.getDefaultSharedPreferences(this);
		config.registerOnSharedPreferenceChangeListener(this);
		
		preparePreferences();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

        courseOnLoad = settings.getSetting("course");
		courseList = new CourseList(this);
		courseList.reloadData();
	}
	
	/**
	 * Verarbeite den Intent und führe die entsprechende Animation aus, denn
	 * durch das REORDER_TO_FRONT Flag werden laufende Activites nicht neugestartet
	 */
	@Override
	protected void onNewIntent(Intent i) {
		super.onNewIntent(i);
		overridePendingTransition(R.anim.move_in_right, R.anim.move_out_left);
	}

	/**
	 * Diese Methode wird erst nach der onCreate-Methode ausgeführt, da sonst
	 * in Android-Versionen < Honeycomb Abstürze auftreten
	 */
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		// Der Zurück-Pfeil in der ActionBar wird manuell angefordert
        getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}

	/**
	 * Füllt die Preferences der SettingsActivity mit Inhalt
	 * 
	 * @param calendarList
	 */
	public void preparePreferences() {
		final SettingsActivity sActivity = this;
		
    	handler.post(new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				ArrayList<String> courseNames = null;
				
				useNotifierPref = (CheckBoxPreference) findPreference("use_notifier");
				
				if(useNotifierPref != null) {
					if((settings.getSetting("regId") == null || settings.getSetting("regId").equals("")) && useNotifierPref.isChecked()) {
						useNotifierPref.setChecked(false);
					}
					
					if(settings.getSetting("course") == null || settings.getSetting("course").equals("")) {
						useNotifierPref.setEnabled(false);
 					} else {
						useNotifierPref.setEnabled(true);
 					}
				}
				
				if(courseList != null) {
					courseNames = courseList.getCalendarList();
				}	
				
				courseListPref = (ListPreference) findPreference("course_list");
		    	
		    	// Wenn die Kursliste keine Einträge enthält, dann wird diese Einstellung disabled
		    	if(courseNames == null || courseNames.size() == 0) {
					if (courseListPref != null) {		
			    		courseListPref.setEnabled(false);
			    		courseListPref.setSummary(sActivity.getString(R.string.settings_courselist_not_downloaded));
					}
		    	} else {		
					if (courseListPref != null) {			
						// Befülle die Kursliste mit Einträgen
					    CharSequence[] courses = courseNames.toArray(new CharSequence[courseNames.size()]);	
			    		courseListPref.setEnabled(true);		    
						courseListPref.setEntries(courses);
						courseListPref.setEntryValues(courses);
			
						// Hier wird die Einstellung zurückgesetzt, wenn der Kurs in unserem Settings-File gelöscht wurde
						String course = Settings.getInstance(sActivity).getSetting("course");
			
						if(course == null || course.equals("")) {
							courseListPref.setValue(null);
						}
						
						// Die Zusammenfassung manuell setzen, da ältere Android-Versionen
						// dies nicht automatisch machen und sie sonst nichts anzeigen
				        courseListPref.setSummary(sActivity.getString(R.string.settings_courselist_actual_selected)
				        	+" "+PreferenceManager.getDefaultSharedPreferences(sActivity).getString("course_list", 
				        	sActivity.getString(R.string.settings_courselist_nothing)));
					}
		    	}
		
				refreshIntervalPref = (ListPreference) findPreference("refresh_interval");
				
				if (refreshIntervalPref != null) {
					refreshIntervalPref.setEntries(intervalEntries);
					refreshIntervalPref.setEntryValues(intervalValues);
		
					// Die Zusammenfassung manuell setzen, da ältere Android-Versionen
					// dies nicht automatisch machen und sie sonst nichts anzeigen
					if(refreshIntervalPref.getEntry() == null) {
						refreshIntervalPref.setSummary(sActivity.getString(R.string.settings_updateinterval_not_selected));			
					} else {
						refreshIntervalPref.setSummary(sActivity.getString(R.string.settings_updateinterval_actual_selected)+" "+refreshIntervalPref.getEntry());
					}
					
					if(useNotifierPref != null && useNotifierPref.isChecked()) {
						refreshIntervalPref.setEnabled(false);
					}
				}

				languagePref = (ListPreference) findPreference("language");
				String language = Settings.getInstance(sActivity).getSetting("language");
				
				if(language == null) {
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(sActivity);
					SharedPreferences.Editor editor = settings.edit();
					
					if(Locale.getDefault().toString().startsWith("en")) {
						editor.putString("language", "en");
					} else {
						editor.putString("language", "de");
					}
					
					editor.commit();
				} else {
					if(languagePref.getEntry() != null) {
						languagePref.setSummary(sActivity.getString(R.string.settings_language_actual_selected)+" "+languagePref.getEntry());						
					}			
				}
    		}
    	});
	}
	
	/**
     * Wenn ein Menuepunkt angeklickt wurde, wird diese Methode aufgerufen.
     * Sie switcht die ID des Menuepunktes und fuert dann unterschiedliche
     * Methoden aus.
     */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int itemId = item.getItemId();
		if (itemId == R.id.menu_refresh) {
			// Hier wird die Kursliste neu geladen und ein Update erzwungen
			courseList.update(true);
		} else if (itemId == android.R.id.home) {
	        backToMainActivity();
		}
    	
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Diese Funktion baut das Menü der SettingsActivity auf. Die Menuepunkte
	 * sind in der Resource 'R.menu.settingsmenu' definiert.
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.settingsmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    /**
     * Wenn der Zurück-Button gedrückt wird
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backToMainActivity();
    }
    
    private void backToMainActivity() {
    	Intent homeIntent = new Intent(this.getApplicationContext(), MainActivity.class);
		homeIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		
		if(settings.getSetting("course") != null && courseOnLoad != null) {
			homeIntent.putExtra("courseChanged", !courseOnLoad.equals(settings.getSetting("course")));
//		} else if (settings.getSetting("course") != null && courseOnLoad == null) {
//			homeIntent.putExtra("courseChanged", true)
		} else {
			homeIntent.putExtra("courseChanged", true);
		}
		
        startActivity(homeIntent);
    	overridePendingTransition(R.anim.move_in_left, R.anim.move_out_right);
	}

	/**
     * Wenn eine Einstellung geändert wird, dann müssen noch einige Vorkehrungen
     * getroffen werden
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if(!this.isFinishing()) {
    		if (key.equals("course_list")) {
    			saveCourseList(sharedPreferences, true);
    		} else if(key.equals("refresh_interval")) {
    			saveRefreshInterval(sharedPreferences, true);        	
    		} else if(key.equals("language")) {
    			saveLanguage(sharedPreferences, true);
    		} else if(key.equals("use_notifier")) {
    			saveUseNotifier(sharedPreferences, true);
    		} else if(key.equals("exams_color") || key.equals("free_days_color") || key.equals("notification_color") || key.equals("weekend_lecture_color")) {
    			int colorCode = sharedPreferences.getInt(key, Color.parseColor("#8f1016"));
    			String varName = key;
    			varName = varName.replace("_c", "C"); // bsp.: exams_color -> examsColor
    			varName = varName.replace("_l", "L"); // bsp.: weekend_lecture -> weekendLecture
    			varName = varName.replace("_d", "D"); // bsp.: free_days -> freeDays
    			settings.setSetting(varName, "#FF"+String.format("%06X", colorCode & 0xFFFFFF));
    		}
    	}
    } 
    
	/**
     * Setzt den Beschreibungstext der CourseList-Preference auf den richtigen Wert und 
     * speichert den in der Kursliste gewählten Eintrag auch im Settings-File, wenn der
     * zweite Paramter gleich 'true' ist
     *     
     * @param sharedPreferences
     * @param writeToSettingsFile
     */
    public void saveCourseList(SharedPreferences sharedPreferences, boolean writeToSettingsFile)
    {
    	String course = sharedPreferences.getString("course_list", this.getString(R.string.settings_courselist_nothing));
        
        // Die Zusammenfassung erneut manuell setzen (für ältere Android-Versionen)
        courseListPref.setSummary(this.getString(R.string.settings_courselist_actual_selected)+" "+course);
        
        // Lösche diese Eigenschaft, damit beim Kurswechsel noch einmal ein Download versucht wird
		settings.removeSetting("dontTryAgainCalendarDownload");
        
        // Die ausgwählte Einstellung wird zwar von der PreferencesActiviy
        // automatisch gespeichert, allerdings möchten wir die Einstellung
        // vorerst auch in unserer eigenen Settingsdatei parat haben
        if(writeToSettingsFile) {
        	// durch das zurücksetzen der CourseList-Preference erhält sie keinen leeren Wert,
        	// sondern den Wert 'keinen'. Dieser darf allerdings nicht in das Settings-File
        	// aufgenommen werden, da sonst beim Neustart der Anwendung versucht wird einen
        	// ICS-Kalender mit dem Namen 'keinen' zu öffnen und die Anwendung abstürzt
        	if(!course.equals("keinen")) {
        		// TODO Den Sinn hiervon herausfinden
        		if(useNotifierPref != null && !useNotifierPref.isEnabled()) {
        			useNotifierPref.setEnabled(true);
        		}
        		
    			final String tempOldCourse = settings.getSetting("course");

    			settings.setSetting("course", course);
        		settings.setSetting("icsFile", courseList.getCalendarFileName(course));
        		settings.setSetting("icsFileRemote", courseList.getUrlFromCalendar(course));
    			
        		// Wenn es ein anderer Kurs als vorher ist und das Gerät GCM möchte wird es neu registriert
        		if(SH.wantsGCM(settings) && !oldCourse.equals(course)) {
            		oldCourse = tempOldCourse;
        			Logbook.d("oldCourse: "+oldCourse);
            		
					try {
						RegisterServerOperation registerOp = RegisterServerOperation.register(this)
							.addParam("regId", settings.getSetting("regId"))
							.setInvokeMethod(SettingsActivity.class.getDeclaredMethod("afterRegisterCourseChanged", new Class[] {String.class}))
							.setInvokeObject(this);

	                	this.showWaitDialog(this.getString(R.string.download_dialog_waittext), this.getString(R.string.dialog_update_the_server),registerOp);
						
						registerOp.start();
					} catch (NoSuchMethodException nsme) {
						Logbook.e(nsme);
					}
        		} else {
            		oldCourse = tempOldCourse;
        		}
        	}
        }
    }
    
    /**
     * Setzt den Beschreibungstext der RefreshInterval-Preference auf den richtigen Wert und 
     * speichert das ICS-Kalenderdatei-Refreshintervall auch in dem Settings-File, wenn der
     * zweite Parameter gleich 'true' ist
     * 
     * @param sharedPreferences
     * @param writeToSettingsFile
     */
    public void saveRefreshInterval(SharedPreferences sharedPreferences, boolean writeToSettingsFile)
    {
    	String refreshValue = sharedPreferences.getString("refresh_interval","2");
    	Logbook.d("refresh interval set to: "+intervalPref[Integer.parseInt(refreshValue)].toString());
    	String refreshEntry = intervalEntries[Integer.parseInt(refreshValue)].toString();
    	
        // Die Zusammenfassung erneut manuell setzen (für ältere Android-Versionen)
        refreshIntervalPref.setSummary(this.getString(R.string.settings_updateinterval_actual_selected)+" "+refreshEntry);
       
        // Die ausgwählte Einstellung wird zwar von der PreferencesActiviy
        // automatisch gespeichert, allerdings möchten wir die Einstellung
        // vorerst auch in unserer eigenen Settingsdatei parat haben
        if(writeToSettingsFile) {
        	settings.setSetting("calRefreshInterval", intervalPref[Integer.parseInt(refreshValue)].toString());
        }
    }
    
    /**
     * Setzt den Beschreibungstext der Language-Preference auf den richtigen Wert und 
     * speichert die Sprache auch in dem Settings-File, wenn der zweite Parameter gleich 'true' ist
     * 
     * @param sharedPreferences
     * @param writeToSettingsFile
     */
    public void saveLanguage(SharedPreferences sharedPreferences, boolean writeToSettingsFile)
    {
    	String languageValue = sharedPreferences.getString("language", "de");
        String languageEntry = (languageValue.equals("en")) ? "English" : "Deutsch";
        boolean needRestart = false;
        
        // Die Zusammenfassung erneut manuell setzen (für ältere Android-Versionen)
        languagePref.setSummary(this.getString(R.string.settings_language_actual_selected)+" "+languageEntry);
        languagePref.setValue(languageValue);
        
        if(settings.getSetting("language") != null && !settings.getSetting("language").equals(languageValue)) {
        	needRestart = true;
        }
        
        // Die ausgwählte Einstellung wird zwar von der PreferencesActiviy
        // automatisch gespeichert, allerdings möchten wir die Einstellung
        // vorerst auch in unserer eigenen Settingsdatei parat haben
        if(writeToSettingsFile) {
        	settings.setSetting("language", languageValue);
        }
        
        if(needRestart) {
        	//finish();
        	Intent restartIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        	restartIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	restartIntent.putExtra("backToSettings", true);
        	startActivity(restartIntent);
        }
    }
    
    private void saveUseNotifier(final SharedPreferences sharedPreferences, boolean writeToSettingsFile) {
    	boolean useNotifier = sharedPreferences.getBoolean("use_notifier", false);
    	final SettingsActivity sActivity = this;
    	
    	if(useNotifier) {
    		final TextView message = new TextView(this);
			// i.e.: R.string.dialog_message =>
			            // "Test this dialog following the link to dtmilano.blogspot.com"
			final SpannableString s = new SpannableString(this.getString(R.string.dialog_use_notifier_warn_msg));
			Linkify.addLinks(s, Linkify.WEB_URLS);
			message.setText(s);
			message.setMovementMethod(LinkMovementMethod.getInstance());
			message.setPadding(20, 20, 20, 20);
			message.setTextSize(18);
			message.setLineSpacing(0.0f, 1.0f);
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setCancelable(false);
        	builder.setView(message).setTitle(this.getString(R.string.dialog_use_notifier_warn_title));
        	builder.setPositiveButton(this.getString(R.string.dialog_use_notifier_warn_accept), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                	try {
						RegisterServerOperation registerOp = RegisterServerOperation.register(sActivity)
							.setInvokeMethod(SettingsActivity.class.getDeclaredMethod("afterRegister", new Class[] {String.class}))
							.setInvokeObject(sActivity);

	                	sActivity.showWaitDialog(sActivity.getString(R.string.download_dialog_waittext), sActivity.getString(R.string.dialog_smartphone_gets_registerd),registerOp);
	                	
	                	registerOp.start();
					} catch (NoSuchMethodException e) {
						Logbook.e(e);
					}
                }
            });
        	builder.setNegativeButton(this.getString(R.string.dialog_use_notifier_warn_deny), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                	settings.removeSetting("regId");
        			SharedPreferences.Editor e = sharedPreferences.edit();
        			e.putBoolean("use_notifier", false);
        			e.commit();
        			
        			if(useNotifierPref != null && useNotifierPref.isChecked()) {
        				useNotifierPref.setChecked(false);
        			}
                }
            });
        	
        	AlertDialog dialog = builder.create();
        	dialog.show();
    	} else {
    		String regId = settings.getSetting("regId");
    		
    		if(regId != null) {
    			try {
	        		RegisterServerOperation unregisterOp = RegisterServerOperation.unregister(regId)
	        		.setInvokeMethod(SettingsActivity.class.getDeclaredMethod("afterUnregister", new Class[] {String.class}))
					.setInvokeObject(this);
	        		
	            	sActivity.showWaitDialog(sActivity.getString(R.string.download_dialog_waittext), sActivity.getString(R.string.dialog_smartphone_gets_unregistered), unregisterOp);
	            	
	            	unregisterOp.start();
				} catch (NoSuchMethodException e) {
					Logbook.e(e);
				} 			
    		}
    	}
	}

	public void afterUnregister(String result) {
		this.hideWaitDialog();
		
    	if(SH.unregisteredSuccessfully(result)) {
    		handler.post(new Runnable() {
    			public void run() {
    				refreshIntervalPref.setEnabled(true);
    			}
    		});
    		
    		settings.removeSetting("regId");
			this.showToast(this.getString(R.string.toast_smartphone_got_unsubscribed), Toast.LENGTH_LONG);
    	} else {
			this.showToast(this.getString(R.string.toast_smartphone_got_not_unsubscribed), Toast.LENGTH_LONG);
			
			handler.post(new Runnable() {
    			public void run() {
    				if(useNotifierPref != null && !useNotifierPref.isChecked()) {
						useNotifierPref.setChecked(true);
					}	
    			}
    		});
    	}
    }
	
	/** 
	 * Nachdem die Registrierung durch die GCMOperation durchgeführt wurde,
	 * wird diese Methode aufgerufen.
	 * 
	 * @param result
	 */
	public void afterRegister(String result) {
		this.hideWaitDialog();
		
		if(SH.registeredSuccessfully(result)) {
			handler.post(new Runnable() {
    			public void run() {
    				refreshIntervalPref.setEnabled(false);
    				
    				if(!useNotifierPref.isChecked()) {
    					useNotifierPref.setChecked(true);
    				}
    			}
    		});
			
			this.showToast(this.getString(R.string.toast_smartphone_got_registered), Toast.LENGTH_LONG);
		} else {
			this.showToast(this.getString(R.string.toast_smartphone_got_not_registered), Toast.LENGTH_LONG);
			// Wenn die Registrierung fehlgeschlagen ist, müssen die Einstellungen zurückgesetzt werden
			settings.removeSetting("regId");
			SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
			e.putBoolean("use_notifier", false);
			e.commit();
			
			handler.post(new Runnable() {
    			public void run() {
					if(useNotifierPref != null && useNotifierPref.isChecked()) {
						useNotifierPref.setChecked(false);
					}	
    			}
    		});
		}
	}

	
	/** 
	 * Nachdem die Registrierung durch die GCMOperation durchgeführt wurde,
	 * wird diese Methode aufgerufen.
	 * 
	 * @param result
	 */
	public void afterRegisterCourseChanged(String result) {
		this.hideWaitDialog();
		final String course = oldCourse;
		
		if(result == null || !result.equals("DEV_UPDATE_SUCCESSFULL")) {
			SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
			e.putString("course_list", course);
			e.commit();
			
			handler.post(new Runnable() {
				public void run() {
					Logbook.d("index of "+course+": "+courseListPref.findIndexOfValue(course));
					courseListPref.setValueIndex(courseListPref.findIndexOfValue(course));
				}
			});
		}
	}
    
    
    /**
	 * Zeigt einen ProgressDialog mit Standard-Titel und -Text an, der
	 * nicht abgebrochen werden kann.
	 */
	public void showWaitDialog() {		
		showWaitDialog(this.getString(R.string.download_dialog_pretitle),this.getString(R.string.download_dialog_waittext));
	}
	
	/**
	 * Diese Methode erzeugt einen ProgressDialog, bei dem der Titel
	 * und der Text als parameter uebergeben werden. Es kann imemr nur
	 * ein ProgressDialog gleichzeitig angezeigt werden. Der ProgressDialog
	 * kann nicht abgebrochen werden.
	 *  
	 * @param title
	 * @param text
	 */
	public void showWaitDialog(final String title, final String text) {
		this.handler.post(new Runnable() {
			public void run() {
				progressDialog = ProgressDialog.show(SettingsActivity.this, title, text, true, false);
			}
		});
	}

	/**
	 * Diese Methode erzeugt einen ProgressDialog, bei dem der Titel
	 * und der Text als parameter uebergeben werden. Es kann imemr nur
	 * ein ProgressDialog gleichzeitig angezeigt werden. Dieser
	 * ProgressDialog kann vom Benutzer abgebrochen werden.
	 *  
	 * @param title
	 * @param text
	 * @param thread Ein Thread, der abgebrochen wird, wenn der Benutzer
	 * 				 den ProgressDialog schließt
	 */
	public void showWaitDialog(final String title, final String text, final CancelableThread thread) {
		final SettingsActivity sActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {				
				progressDialog = new ProgressDialog(SettingsActivity.this);
				progressDialog.setTitle(title);
				progressDialog.setMessage(text);
				progressDialog.setCancelable(false);
				progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, sActivity.getString(R.string.download_dialog_cancel), new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
				    	// Thread abbrechen
				    	thread.cancel();
				    	// Dialog verstecken
				    	progressDialog.dismiss();
				    }
				});
				progressDialog.show();
			}			
		});
	}
	
	/**
	 * Diese Methode ändert den Text des ProgressDialogs
	 *  
	 * @param newText
	 */
	public void changeDialogText(final String newText){
		this.handler.post(new Runnable() {
			public void run() {	
				if(progressDialog != null && progressDialog.isShowing()) {
					progressDialog.setMessage(newText);
				}
			}
		});
	}
	
	/**
	 * Diese Methode versteckt den ProgressDialog wieder
	 */
	public void hideWaitDialog() {
		this.handler.post(new Runnable() {
			public void run() {	
				if(progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
			}	
		});
	}

	/**
	 * Diese Methode erzeugt einen Toast mit dem gewünschten Text und der gewünschten Anzeigedauer  
	 * 
	 * @param text
	 * @param duration
	 */
	public void showToast(final String text, final int duration) {
		final SettingsActivity thisActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {
				Toast.makeText(thisActivity, text, duration).show();	
			}			
		});
	}
	
	public void showGCMErrorDialog(final int recoverableResultCode) {
		final SettingsActivity thisActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {
				GooglePlayServicesUtil.getErrorDialog(recoverableResultCode, thisActivity,
		        		9000).show();
			}			
		});
	}
}
