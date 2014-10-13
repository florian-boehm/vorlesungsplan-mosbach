package de.patricklammers.vorlesungsplan.app;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import de.patricklammers.vorlesungsplan.app.expandablelist.InfoExpandableListAdapter;
import de.patricklammers.vorlesungsplan.app.info.HelpFeature;
import de.patricklammers.vorlesungsplan.app.info.Info;
import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;
import de.patricklammers.vorlesungsplan.app.interfaces.DialogAndToastActivity;
import de.patricklammers.vorlesungsplan.app.io.Logbook;

/**
 * Die Activity, die hinter der Info & Hilfe steht 
 * 
 * @author Florian Schwab
 */
public class InfoHelpActivity extends ActionBarActivity implements DialogAndToastActivity
{
	private Handler 								handler 			= null;
	private ProgressDialog 							progressDialog		= null;
	private ArrayList<String>						listDataHeader		= null;
	private HashMap<String, ArrayList<HelpFeature>>	listDataChild		= null;
	private Info 									info				= null;
	
	@Override
    public void onCreate(Bundle savedInstanceState)	{
        super.onCreate(savedInstanceState);  
		
		setContentView(R.layout.info_help_new);
    	setTitle(R.string.menu_info_help);

    	handler = new Handler();
    	
    	info = new Info(this);
    	info.update(true);

    	String versionName = "";
    	
    	// Die Versionsnummer auslesen
        try	{
			versionName = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException nnfe) {
			Logbook.e(nnfe);
		}
        
        // Die Versionsnummer wird dynamisch aus dem Android-Manifest angehängt
        TextView versionInfo = (TextView) findViewById(R.id.version_info);
        versionInfo.append(" "+versionName);
	}
	
	public void afterSending(String message) {
		if (message.equals("success")) {
			this.showToast(this.getString(R.string.data_sended_successfully), 2000);
		} else {
			this.showToast(this.getString(R.string.error_occurred), 3000);
		}
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
	 * Immer wenn ein MenuItem ausgewählt wird
	 * 
	 * @param item Das ausgewählte MenuItem
	 */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Logbook.d("ID of selected item: "+item.getItemId());
    	
    	switch (item.getItemId()) {        	
    		case android.R.id.home:           
	    		finish();        		
	    		// Die standard Übergangsanimation mit einer eigenen Animation überschreiben
	    		overridePendingTransition(R.anim.move_in_left, R.anim.move_out_right);
	    		break;
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
    
    /*
     * Preparing the list data
     */
    public void prepareListData() {

		final InfoHelpActivity ioActivity = this;
		
    	handler.post(new Runnable() {
			public void run() {
				try	{
					InfoHelpActivity.this.setContentView(R.layout.info_help_new);
					
			    	String versionName = "";
			    	Integer versionCode = 0;
			    	
			    	// Die Versionsnummer auslesen
			        try	{
						versionName = getPackageManager().getPackageInfo(ioActivity.getPackageName(), 0).versionName;
						versionCode = getPackageManager().getPackageInfo(ioActivity.getPackageName(), 0).versionCode;
					} catch (NameNotFoundException nnfe) {
						Logbook.e(nnfe);
					}
			        
			        // Die Versionsnummer wird dynamisch aus dem Android-Manifest angehängt
			        TextView versionInfo = (TextView) findViewById(R.id.version_info);
			        versionInfo.append(" "+versionName);
					
			        Button update = (Button) findViewById(R.id.update);
			        if (ioActivity.info.getActualVersionCode() > versionCode) {
			        	update.setText(ioActivity.getString(R.string.update_available));
			        	update.setVisibility(View.VISIBLE);
			        	update.setOnClickListener(new OnClickListener() {
			                public void onClick(View v) {
			                	final String url = "https://play.google.com/store/apps/details?id=de.patricklammers.vorlesungsplan.app";
			                	 Uri uri = Uri.parse(url);
			                	 Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			                	 startActivity(intent);
			                } 
			            });
			        }
			        
					listDataHeader = new ArrayList<String>();
					listDataChild = new HashMap<String, ArrayList<HelpFeature>>();
			        ArrayList<HelpFeature> help = new ArrayList<HelpFeature>();
			        ArrayList<HelpFeature> feature = new ArrayList<HelpFeature>();
			        ArrayList<HelpFeature> bug = new ArrayList<HelpFeature>();
			        
			        // Adding child data
			        listDataHeader.add(ioActivity.getString(R.string.help_title));
			        listDataHeader.add(ioActivity.getString(R.string.bug_report_title));
			        listDataHeader.add(ioActivity.getString(R.string.feature_request_title));
			 
			        // Adding child data0
			        for (int i = 0; i < info.getSize(); i++) {
			        	HelpFeature dummy = info.getItem(i);
			        	if (dummy.getField() == 0) {
			        		help.add(dummy);
			        	} else {
			        		feature.add(dummy);
			        	}
			        }
			        bug.add(new HelpFeature());
			        feature.add(new HelpFeature());
			 
			        listDataChild.put(listDataHeader.get(0), help);
			        listDataChild.put(listDataHeader.get(1), bug);
			        listDataChild.put(listDataHeader.get(2), feature);
			        
			        ExpandableListView expListView = (ExpandableListView) ioActivity.findViewById(R.id.infoList);
			        InfoExpandableListAdapter listAdapter = new InfoExpandableListAdapter(ioActivity, listDataHeader, listDataChild);
			        listAdapter.setActivity(ioActivity);
			        expListView.setAdapter(listAdapter);
				} catch(Exception e) {
					Logbook.e(e);
				}
		}});
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
				progressDialog = ProgressDialog.show(InfoHelpActivity.this, title, text, true, false);
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
		final InfoHelpActivity ioActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {				
				progressDialog = new ProgressDialog(InfoHelpActivity.this);
				progressDialog.setTitle(title);
				progressDialog.setMessage(text);
				progressDialog.setCancelable(false);
				progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ioActivity.getString(R.string.download_dialog_cancel), new DialogInterface.OnClickListener() {
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
		final InfoHelpActivity thisActivity = this;
		
		this.handler.post(new Runnable() {
			public void run() {
				Toast.makeText(thisActivity, text, duration).show();	
			}			
		});
	}
}
