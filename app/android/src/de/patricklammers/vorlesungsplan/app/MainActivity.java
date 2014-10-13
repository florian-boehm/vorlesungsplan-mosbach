package de.patricklammers.vorlesungsplan.app;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.icalendar.CustomListView;
import de.patricklammers.vorlesungsplan.app.icalendar.ICalendar;
import de.patricklammers.vorlesungsplan.app.icalendar.ICalendarComperator.Differences;
import de.patricklammers.vorlesungsplan.app.icalendar.ICalendarComperator.EventModifiedStatus;
import de.patricklammers.vorlesungsplan.app.icalendar.Vevent;
import de.patricklammers.vorlesungsplan.app.icalendar.VeventAdapter;
import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;
import de.patricklammers.vorlesungsplan.app.interfaces.DialogAndToastActivity;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.io.RegisterServerOperation;
import de.patricklammers.vorlesungsplan.app.settings.Settings;
import de.patricklammers.vorlesungsplan.app.utils.StatusHelper.SH;

/**
 * Diese Activity ist die MainActivity und bildet den Einstiegspunkt der App
 * 
 * @author Florian Schwab
 */
public class MainActivity extends ActionBarActivity implements DialogAndToastActivity {
	// Nimmt Aufgaben von anderen Threads entgegen und leitet sie an die MainActivity weiter
	private Handler 		handler 		= null;
	private ICalendar		courseCalendar	= null; 
	private ProgressDialog 	progressDialog	= null;
	private Settings		settings 		= null;
	private Date			customBegin		= null;
	private CustomListView	listView 		= null;
	private LinearLayout	updateNotificationBar = null;
	private VeventAdapter   vAdapter		= null;
	private Differences		differences		= null;
	private boolean			showUpdateNotification = false;
	private InternalUpdateReceiver receiver = null;
	private boolean isPaused = true;
	
	/**
	 * Die onCreate Methode wird nur beim erstmaligen Öffnen der App
	 * aufgerufen. Deshalb finden alle tatsächlichen Vorbereitungen nun
	 * in der onResume Methode statt.
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)	{
        super.onCreate(savedInstanceState);  
        
        // Bevor die Einstellungen geladen werden, müssen sie vom Conformator überprüft werden
        // Aktuell nicht notwendig!
        // while(!SettingsConformator.confirmFile(this)) {};
        settings = Settings.getInstance(this);
		
		setLanguage();
		setContentView(R.layout.calendar);
        setTitle(R.string.app_title);
        handler = new Handler();
		
		// Wenn die Sprache geändert wurde, dann wird automatisch wieder zu den Einstellungen gewechselt
		if(getIntent().getBooleanExtra("backToSettings", false)) {
			Intent settingsIntent = new Intent(this.getApplicationContext(), SettingsActivity.class);
			settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(settingsIntent);
		}
        
        receiver = new InternalUpdateReceiver(handler);
        registerReceiver(receiver, new IntentFilter("GCMIntentService.UPDATED_CALENDAR"));
        
        // TODO prüfen, ob diese Funktion gut ist!
        if(SH.lastUpdate14DaysInPast(settings) && SH.wantsGCM(settings)) {
        	RegisterServerOperation.statusAS(settings);
        }
        
        // Säubere "DontTryAgain" Settings
        settings.removeSetting("dontTryAgainCourseListDownload");
        settings.removeSetting("dontTryAgainCalendarDownload");
    }

	/**
	 * Die onResume Methode ist der letzte Schritt, der aufgerufen wird,
	 * bevor die App tatsaechlich angezeigt wird. Sie wird IMMER aufgerufen!
	 * Wenn die App komplett neu erzeugt wird, erneut gestartet wird oder
	 * aus dem Hintergrund in den Vordergrund geholt wird.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		isPaused = false;
		
		// customBegin auf null zurücksetzen und Pull for History wieder aktivieren
		customBegin = null;
		courseCalendar = new ICalendar(this);
		
		// Setze die Markups zurück, falls diese noch existieren, die UpdateNotificationBar
		// aber nicht mehr angezeigt werden soll
		// TODO Prüfen, ob dieser Fall jemals eintritt
		if(!showUpdateNotification) {
			differences = null;
		}
		
		// Wenn noch kein Kurs ausgewählt wurde
		if(SH.noCourseSelected(settings)) {
			prepareCalendarLayout();
		} else {		
			courseCalendar.reloadData(true, false); 
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		isPaused = true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onNewIntent(Intent i) {
		super.onNewIntent(i);
		setIntent(i);
		overridePendingTransition(R.anim.move_in_left, R.anim.move_out_right);
	}

	/**
	 * Diese Funktion baut das Hauptmenue der Anwendung auf. Die Menuepunkte
	 * sind in der Resource 'R.menu.mainmenu' definiert.
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    /**
     * Wenn ein Menuepunkt angeklickt wurde, wird diese Methode aufgerufen.
     * Sie switcht die ID des Menuepunktes und fuert dann unterschiedliche
     * Methoden aus.
     */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			courseCalendar.reloadData(true, false);
		} else if (itemId == R.id.menu_settings) {
			Intent settingsIntent = new Intent(this.getApplicationContext(), SettingsActivity.class);
			settingsIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(settingsIntent);
			overridePendingTransition(R.anim.move_in_right, R.anim.move_out_left);
		} else if (itemId == R.id.menu_search) {
			Toast.makeText(this, "Diese Funktion ist leider noch nicht implementiert.\nVielen Dank für dein Verständnis.", Toast.LENGTH_LONG).show();
		} else if (itemId == R.id.menu_info_help) {
			Intent infoHelpIntent = new Intent(this.getApplicationContext(), InfoHelpActivity.class);
			infoHelpIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(infoHelpIntent);
			overridePendingTransition(R.anim.move_in_right, R.anim.move_out_left);
		} else if (itemId == R.id.menu_speiseplan) {
			Intent lunchMenuIntent = new Intent(this.getApplicationContext(), LunchActivity.class);
			lunchMenuIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(lunchMenuIntent);
			overridePendingTransition(R.anim.move_in_right, R.anim.move_out_left);
		} else if (itemId == R.id.menu_refresh) {
			// Hier wird der Kurskalender neu geladen und ein Update erzwungen
			this.setCustomBegin(null);
			courseCalendar.reloadData(true,true);
		}
        
        return super.onOptionsItemSelected(item);
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
				progressDialog = ProgressDialog.show(MainActivity.this, title, text, true, false);
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
		final MainActivity mActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {				
				progressDialog = new ProgressDialog(MainActivity.this);
				progressDialog.setTitle(title);
				progressDialog.setMessage(text);
				progressDialog.setCancelable(false);
				progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mActivity.getString(R.string.download_dialog_cancel), new DialogInterface.OnClickListener() {
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
	 * Diese Funktion wird IMMER von einer Instanz von ICalendar ausgeloest.
	 * Grund: Der ICalender muss zuerst die Daten verarbeitet haben, damit
	 * das Kalenderlayout aufgebaut werden kann.
	 */
	public void prepareCalendarLayout() {    	  
		final MainActivity mActivity = this;
		
		// Wenn die Sprache geändert wurde, dann wird automatisch wieder zu den Einstellungen gewechselt
		// und das am besten bevor der Kalender gerendert wird. Deshalb befindet sich das hier an dieser Stelle!
		if(getIntent().getBooleanExtra("backToSettings", false)) {
			getIntent().removeExtra("backToSettings");
			Intent settingsIntent = new Intent(this.getApplicationContext(), SettingsActivity.class);
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			} else {
				settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_CLEAR_TOP);
			}
			
            startActivity(settingsIntent);
		}
		
        // Prüfe, ob die App geupdated wurde
        checkIfAppGotUpdated();
		
    	handler.post(new Runnable() {
			public void run() {
				try	{
					String appTitle = MainActivity.this.getResources().getString(R.string.app_title);
					
					if(settings.getSetting("course") != null) {
						// Actionbar vorbereiten
						MainActivity.this.getActionBarHelper().setDisplayHomeAsUpEnabled(false);
						MainActivity.this.setTitle(appTitle + " ("+Settings.getInstance(mActivity).getSetting("course")+")");
					} else {
						// Actionbar vorbereiten
						MainActivity.this.getActionBarHelper().setDisplayHomeAsUpEnabled(false);
						MainActivity.this.setTitle(appTitle);			
					}
					
					// Aktuelles Datum abrufen
					Calendar cal = Calendar.getInstance(Locale.GERMANY);
					Date begin = (Date) cal.getTime();
					
					if(customBegin != null) {
						begin = customBegin;
					}
					
					// ListView suchen und den Adapter auf null setzen
					if(listView == null) {
						listView = (CustomListView) MainActivity.this.findViewById(R.id.calendar_list_view);
						listView.setMainActivity(mActivity);
					}

					// Wenn der Kurs gewechselt wurde, dann sollte zum Anfang der Liste gesprungen werden
					if(mActivity.getIntent().getBooleanExtra("courseChanged", false)) {
						getIntent().removeCategory("courseChanged");
						listView.smoothScrollToPosition(0);
						// Außerdem muss die Update-Notification-Bar ausgeblendet werden
						if(showUpdateNotification) {
							hideUpdateNotificationBar();
						}
					}
					
					listView.resetOldTitle();
					listView.setFastScrollEnabled(true);
					// listView.setAdapter(null);
					setPullForHistoryEnabled(true);
					
					// Neuen VeventAdapter erstellen und in die ListView einsetzen
					ArrayList<Vevent> events = (Settings.getInstance(mActivity).getSetting("course") == null) ? null : courseCalendar.getEvents(begin,null);
					
					if(vAdapter == null) {
						vAdapter = new VeventAdapter(MainActivity.this, events);
						listView.setAdapter((ListAdapter) vAdapter);
					} else {
						vAdapter.replaceAllEventsWith(events);
					}
					
			    	listView.setClickable(false);
			    	
			    	// Wenn die Methode aufgerufen wurde, aber noch die UpdateNotificationBar angezeigt werden soll,
			    	// dann muss die Dummyrow am Ende manuell hinzugefügt werden, da diese durch den Reset des
			    	// Adapters verloren gegangen ist!
			    	if(showUpdateNotification) {
						mActivity.addNeccessaryEvents(updateNotificationBar.getMeasuredHeight());
			    	}
				} catch(Exception e) {
					Logbook.e(e);
					e.printStackTrace();
				}
		}});
	}
	
	/**
	 * Diese Methode erzeugt einen Toast mit dem gewünschten Text und der gewünschten Anzeigedauer  
	 * 
	 * @param text
	 * @param duration
	 */
	public void showToast(final String text, final int duration) {
		final MainActivity mActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {
				Toast.makeText(mActivity, text, duration).show();	
			}			
		});
	}

	public Date getCustomBegin() {
		return customBegin;
	}

	public void setCustomBegin(Date customBegin) {
		this.customBegin = customBegin;
	}
	
	public ICalendar getCourseCalendar() {
		return this.courseCalendar;
	}
	
	public void setPullForHistoryEnabled(boolean isEnabled) {
		if(isEnabled) {
			listView.enable();
		} else {
			listView.disable();
		}
	}
	
	private void setLanguage() {
		String language = settings.getSetting("language");
		
		if(language != null) {
			if(language.equals("en")) {
				Locale.setDefault(Locale.ENGLISH);
				Configuration config = new Configuration();
				config.locale = Locale.ENGLISH;
				this.getResources().updateConfiguration(config, null);
			} else {
				Locale.setDefault(Locale.GERMANY);
				Configuration config = new Configuration();
				config.locale = Locale.GERMANY;
				this.getResources().updateConfiguration(config, null);        	
			}
		}
	}

	public void showUpdateNotificationBar(final Differences dif) {
		final MainActivity mActivity = this;
		this.differences = dif;
		
		if(differences == null) {
			return;
		}
		
		this.handler.post(new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				// Wenn bereits die UpdateNotificationBar angezeigt wird, dann ist ein Update
				// eingetroffen, bevor die UpdateNotificationBar geschlossen wurde (sehr selten)!
				if(showUpdateNotification) {
					clearUnneccessaryEvents();
				}
				
				if(updateNotificationBar == null) {
					// Suche die updateNotificationBar und bereite sie vor
					updateNotificationBar = (LinearLayout) MainActivity.this.findViewById(R.id.update_notification_bar);
					((ImageView) updateNotificationBar.findViewById(R.id.update_notification_close)).setOnTouchListener(new OnTouchListener() {
						public boolean onTouch(View v, MotionEvent event) {
							mActivity.hideUpdateNotificationBar();
							return true;
						}
					});
				}

				if(updateNotificationBar != null) {
					// Strings vorbereiten
					String addedEvents = "- ";
					String removedEvents = "- ";
					String modifiedEvents = "- ";
					addedEvents += dif.addedCount + " " + mActivity.getString((dif.addedCount == 1) ? R.string.update_info_added_singular : R.string.update_info_added_plural);
					removedEvents += dif.removedCount + " " + mActivity.getString((dif.removedCount == 1) ? R.string.update_info_removed_singular : R.string.update_info_removed_plural);
					modifiedEvents += dif.modifiedCount + " " + mActivity.getString((dif.modifiedCount == 1) ? R.string.update_info_modified_singular : R.string.update_info_modified_plural);
					((TextView) updateNotificationBar.findViewById(R.id.update_notification_added)).setText(addedEvents);
					((TextView) updateNotificationBar.findViewById(R.id.update_notification_removed)).setText(removedEvents);
					((TextView) updateNotificationBar.findViewById(R.id.update_notification_modified)).setText(modifiedEvents);
					
					// Messe die View bevor sie gerendert wurde
					updateNotificationBar.setVisibility(View.INVISIBLE);
					
					if(updateNotificationBar.getMeasuredHeight() == 0) {
						Point p = new Point();
						
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
							p.set(mActivity.getWindowManager().getDefaultDisplay().getWidth(), mActivity.getWindowManager().getDefaultDisplay().getHeight());
						} else {				
							mActivity.getWindowManager().getDefaultDisplay().getSize(p);
						}
						
						updateNotificationBar.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
					}
					
					final int measuredHeight = updateNotificationBar.getMeasuredHeight();

					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						ObjectAnimator barOutAnimator = new ObjectAnimator();
						barOutAnimator.setTarget(updateNotificationBar);
						barOutAnimator.setFloatValues(measuredHeight,0.0f);
						barOutAnimator.setPropertyName("translationY");
						barOutAnimator.setDuration(1000);
						barOutAnimator.setStartDelay(1000);
						barOutAnimator.setInterpolator(new DecelerateInterpolator());
						
						updateNotificationBar.setTranslationY(updateNotificationBar.getY()+updateNotificationBar.getMeasuredHeight());
						updateNotificationBar.setVisibility(View.VISIBLE);
	
						// Starte die Ausfahr-Animation zum Schluss
						barOutAnimator.start();
					} else {
						// updateNotificationBar.setTranslationY(0.0f);
						updateNotificationBar.setVisibility(View.VISIBLE);
					}
					
					// Füge einen OnGestureListener hinzu
					updateNotificationBar.setOnTouchListener(new OnTouchListener() {
						float startY = 0.0f;
						
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							switch(event.getAction()) {
								case MotionEvent.ACTION_DOWN:
									startY = event.getY();
									break;
								case MotionEvent.ACTION_UP:
									float dif = event.getY() - startY;
									float height = updateNotificationBar.getHeight();
									
									if(dif > (height*0.3)) {
										hideUpdateNotificationBar();
									}
									break;
							}

							return true;
						}
					});
					
					mActivity.addNeccessaryEvents(measuredHeight);
					mActivity.showUpdateNotification = true;
				}
			}
		});
	}

	protected void addNeccessaryEvents(int measuredHeight) {
		// Füge gelöschte Events ein
		for(Entry<String, EventModifiedStatus> entry : differences.events.entrySet()) {
			if(entry.getValue().equals(EventModifiedStatus.REMOVED)) {
				vAdapter.insertRemovedEvent(entry.getKey(),false);
			}
		}
		
		vAdapter.sortEvents();
		vAdapter.addDummyRowAtEnd(measuredHeight-10);
		
		// Informiere alle Observer darüber, dass sich die Eventliste geändert hat
		vAdapter.notifyDataSetChanged();
	}

	public void hideUpdateNotificationBar() {
		final MainActivity mActivity = this;
		
		if(!showUpdateNotification) {
			return;
		}
		
		this.handler.post(new Runnable() {
			public void run() {
				if(updateNotificationBar != null) {
					mActivity.differences = null;
					mActivity.clearUnneccessaryEvents();
					
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						ObjectAnimator barInAnimator = new ObjectAnimator();
						barInAnimator.setTarget(updateNotificationBar);
						barInAnimator.setFloatValues(0.0f,updateNotificationBar.getY());
						barInAnimator.setPropertyName("translationY");
						barInAnimator.setDuration(1000);
						barInAnimator.setInterpolator(new AccelerateInterpolator());
						barInAnimator.addListener(new AnimatorListener() {
							
							public void onAnimationStart(Animator animation) {
							}
							
							public void onAnimationRepeat(Animator animation) {
							}
							
							public void onAnimationEnd(Animator animation) {
								updateNotificationBar.setVisibility(View.GONE);
							}
							
							public void onAnimationCancel(Animator animation) {
							}
						});
	
						// Starte die Einfahr-Animation zum Schluss
						barInAnimator.start();
					} else {
						// updateNotificationBar.setTranslationY(updateNotificationBar.getY());
						updateNotificationBar.setVisibility(View.GONE);
					}
					
					mActivity.showUpdateNotification = false;
				}
			}
		});
	}
	
	private void clearUnneccessaryEvents() {
		// Die Dummyrow muss vor den RemovedEvents gelöscht werden, weil sie sich am Ende befindet
		// und removeRemovedEvents ein Collection.sort durchführt!
		vAdapter.removeDummyRowAtEnd();
		vAdapter.removeRemovedEvents();
		
		// Informiere alle Observer darüber, dass sich die Eventliste geändert hat
		vAdapter.notifyDataSetChanged();
	}
	
	public int getDisplayedEventsCount() {
		if(vAdapter != null) {
			return vAdapter.getCount();
		} else {
			return 0;
		}
	}
	
	public Differences getDifferencesFromLastUpdate() {
		return differences;
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
	private void checkIfAppGotUpdated() {
		int actualVersionCode = -1;
		
		try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            actualVersionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            Logbook.e(e);
        }
		
		if(settings.getSetting("versionCode") != null) {
			int lastVersionCode = Integer.parseInt(settings.getSetting("versionCode"));
			
			if(lastVersionCode != actualVersionCode) {
				settings.setSetting("versionCode", Integer.toString(actualVersionCode));
				
				if(settings.getSetting("regId") != null) {
					settings.removeSetting("regId");
					
					try {
						RegisterServerOperation registerOp = RegisterServerOperation.register(this)
							.setInvokeMethod(this.getClass().getDeclaredMethod("afterRegister", new Class[] {String.class}))
							.setInvokeObject(this);

						this.showWaitDialog(this.getString(R.string.download_dialog_waittext), this.getString(R.string.dialog_smartphone_gets_registerd),registerOp);
	                	
	                	registerOp.start();
					} catch (NoSuchMethodException e) {
						Logbook.e(e);
					}
				}
			}
		} else {
			if(actualVersionCode != -1) {
				settings.setSetting("versionCode", Integer.toString(actualVersionCode));
			}
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
			this.showToast(this.getString(R.string.toast_smartphone_got_registered), Toast.LENGTH_LONG);
		} else {
			this.showToast(this.getString(R.string.toast_smartphone_got_not_registered), Toast.LENGTH_LONG);
			// Wenn die Registrierung fehlgeschlagen ist, müssen die Einstellungen zurückgesetzt werden
			settings.removeSetting("regId");
		}
	}
	
	public class InternalUpdateReceiver extends BroadcastReceiver {
		private Handler handler = null;
		
		public InternalUpdateReceiver(Handler handler) {
			this.handler = handler;
		}
		
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        handler.post(new Runnable() {
				public void run() {
					if(!isPaused()) {
						listView.smoothScrollToPosition(0);
						courseCalendar.reloadData(true, false);
					}
				}
			});
	    }
	}
}