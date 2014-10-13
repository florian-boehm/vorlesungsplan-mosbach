package de.patricklammers.vorlesungsplan.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.expandablelist.LunchExpandableListAdapter;
import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;
import de.patricklammers.vorlesungsplan.app.interfaces.DialogAndToastActivity;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.lunch.LunchPlan;

/**
 * Die Activity, die hinter dem Speiseplan steht 
 * 
 * @author Patrick Lammers
 */
public class LunchActivity extends ActionBarActivity implements OnGestureListener, DialogAndToastActivity
{
	private GestureDetector 					gestureDetector 	= null;
	private GestureOverlayView 					gestureOverlayView 	= null;
	private ProgressDialog 						progressDialog		= null;
	private Handler 							handler 			= null;
	private ArrayList<String>					listDataHeader		= null;
	private HashMap<String, ArrayList<String>>	listDataChild		= null;
	private LunchPlan 							plan				= null;
	
	@Override
    public void onCreate(Bundle savedInstanceState)	{
        super.onCreate(savedInstanceState);  
		
        handler = new Handler();
        
        gestureOverlayView = new GestureOverlayView(this);
        View inflate = getLayoutInflater().inflate(R.layout.lunchnew, null);
        gestureOverlayView.addView(inflate);
        gestureOverlayView.setGestureVisible(false);
        
        setContentView(gestureOverlayView);
    	setTitle(R.string.speiseplan);
    	
    	plan = new LunchPlan(this);
    	
    	plan.reloadData();
    	
    	// Setze den Gestendetektor
    	gestureDetector = new GestureDetector(getBaseContext(),this);
	}

	/*
     * Preparing the list data
     */
    public void prepareListData() {
    	
    	final LunchActivity mActivity = this;
		
    	handler.post(new Runnable() {
			public void run() {
				try	{
					LunchActivity.this.setContentView(R.layout.lunchnew);

					listDataHeader = new ArrayList<String>();
					listDataChild = new HashMap<String, ArrayList<String>>();
			        ArrayList<String> mon = new ArrayList<String>();
			        ArrayList<String> tue = new ArrayList<String>();
			        ArrayList<String> wed = new ArrayList<String>();
			        ArrayList<String> thu = new ArrayList<String>();
			        ArrayList<String> fri = new ArrayList<String>();
			        
			        SimpleDateFormat ger = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY);
			        SimpleDateFormat def = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.getDefault());
			        
			        // Adding child data
			        listDataHeader.add(def.format(ger.parse(plan.getMonday(0))));
			        listDataHeader.add(def.format(ger.parse(plan.getTuesday(0))));
			        listDataHeader.add(def.format(ger.parse(plan.getWednesday(0))));
			        listDataHeader.add(def.format(ger.parse(plan.getThursday(0))));
			        listDataHeader.add(def.format(ger.parse(plan.getFriday(0))));
			 
			        // Adding child data
			        String[] dummy;
			        dummy = plan.getMonday(1).split("<br />");
			        for (int i = 0; i < dummy.length; i++) {
			        	mon.add(dummy[i]);
			        }
			        dummy = plan.getTuesday(1).split("<br />");
			        for (int i = 0; i < dummy.length; i++) {
			        	tue.add(dummy[i]);
			        }
			        dummy = plan.getWednesday(1).split("<br />");
			        for (int i = 0; i < dummy.length; i++) {
			        	wed.add(dummy[i]);
			        }
			        dummy = plan.getThursday(1).split("<br />");
			        for (int i = 0; i < dummy.length; i++) {
			        	thu.add(dummy[i]);
			        }
			        dummy = plan.getFriday(1).split("<br />");
			        for (int i = 0; i < dummy.length; i++) {
			        	fri.add(dummy[i]);
			        }
			 
			        listDataChild.put(listDataHeader.get(0), mon);
			        listDataChild.put(listDataHeader.get(1), tue);
			        listDataChild.put(listDataHeader.get(2), wed);
			        listDataChild.put(listDataHeader.get(3), thu);
			        listDataChild.put(listDataHeader.get(4), fri);
			        
			        ExpandableListView expListView = (ExpandableListView) mActivity.findViewById(R.id.infoList);
			        LunchExpandableListAdapter listAdapter = new LunchExpandableListAdapter(mActivity, listDataHeader, listDataChild);
			        expListView.setAdapter(listAdapter);
			        
			        SimpleDateFormat format = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY);
			        Calendar cal = Calendar.getInstance(Locale.GERMANY);
			        Date now = cal.getTime();
			        
			        Logbook.d(format.format(now));
			        if (format.format(now).equals(plan.getMonday(0))) {
			        	expListView.expandGroup(0);
			        }
			        if (format.format(now).equals(plan.getTuesday(0))) {
			        	expListView.expandGroup(1);
			        }
			        if (format.format(now).equals(plan.getWednesday(0))) {
			        	expListView.expandGroup(2);
			        }
			        if (format.format(now).equals(plan.getThursday(0))) {
			        	expListView.expandGroup(3);
			        }
			        if (format.format(now).equals(plan.getFriday(0))) {
			        	expListView.expandGroup(4);
			        }
					
				} catch(Exception e) {
					Logbook.e(e);
				}
		}});
    }
	
	/**
	 * Diese Methode wird erst nach der onCreate-Methode ausgeführt
	 */
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		// Der Zurück-Pfeil in der ActionBar wird manuell angefordert
        getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}

	/**
	 * Diese Funktion baut das lunchmenue der Anwendung auf. Die Menuepunkte
	 * sind in der Resource 'R.menu.lunchmenu' definiert.
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.lunchmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	/**
	 * Immer wenn ein MenuItem ausgewählt wird
	 * 
	 * @param item Das ausgewählte MenuItem
	 */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Logbook.d("ID of selected item: "+item.getItemId());
    	
    	int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			// Die standard Übergangsanimation mit einer eigenen Animation überschreiben
			overridePendingTransition(R.anim.move_in_left, R.anim.move_out_right);
		} else if (itemId == R.id.menu_refresh) {
			plan.update(true);
			ExpandableListView expListView = (ExpandableListView) findViewById(R.id.infoList);
			LunchExpandableListAdapter listAdapter = new LunchExpandableListAdapter(this, listDataHeader, listDataChild);
			// setting list adapter
			expListView.setAdapter(listAdapter);
		}
    	return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Wenn der zurück-Hardwarebutton gedrückt wird
	 */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Die standard Übergangsanimation mit einer eigenen Animation überschreiben
    	overridePendingTransition(R.anim.move_in_left, R.anim.move_out_right);
    }

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Logbook.d("Back to MainActivity");
		
		if(velocityX > 2000.0) {
			this.finish();        		
    		// Die standard Übergangsanimation mit einer eigenen Animation überschreiben
    		this.overridePendingTransition(R.anim.move_in_left, R.anim.move_out_right);
		}
		
		return true;
	}

	public void onLongPress(MotionEvent e) {
		
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return true;
	}

	public void onShowPress(MotionEvent e) {
		
	}

	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent e)
	{
		gestureDetector.onTouchEvent(e);

	    return super.dispatchTouchEvent(e);
	}
	
	/**
	 * Zeigt einen ProgressDialog mit Standard-Titel und -Text an, der
	 * nicht abgebrochen werden kann.
	 */
	public void showWaitDialog() {		
		showWaitDialog(this.getString(R.string.download_dialog_waittext),this.getString(R.string.download_dialog_pretitle));
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
				progressDialog = ProgressDialog.show(LunchActivity.this, title, text, true, false);
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
		final LunchActivity lActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {				
				progressDialog = new ProgressDialog(LunchActivity.this);
				progressDialog.setTitle(title);
				progressDialog.setMessage(text);
				progressDialog.setCancelable(false);
				progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, lActivity.getString(R.string.download_dialog_cancel), new DialogInterface.OnClickListener() {
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
				if(progressDialog.isShowing()) {
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
				if(progressDialog.isShowing()) {
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
		final LunchActivity thisActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {
				Toast.makeText(thisActivity, text, duration).show();	
			}			
		});
	}
}
