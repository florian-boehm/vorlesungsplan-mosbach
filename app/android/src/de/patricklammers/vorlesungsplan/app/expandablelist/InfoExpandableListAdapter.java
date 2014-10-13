package de.patricklammers.vorlesungsplan.app.expandablelist;

import java.util.ArrayList;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.patricklammers.vorlesungsplan.app.InfoHelpActivity;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.info.HelpFeature;
import de.patricklammers.vorlesungsplan.app.io.FeatureBugSubmit;
import de.patricklammers.vorlesungsplan.app.io.FeatureBugSubmit.SendingType;

public class InfoExpandableListAdapter extends BaseExpandableListAdapter {
	private Context _context;
    private ArrayList<String> _listDataHeader;
    private HashMap<String, ArrayList<HelpFeature>> _listDataChild;
    private InfoHelpActivity activity = null;
 
    public InfoExpandableListAdapter(Context context, ArrayList<String> listDataHeader, HashMap<String, ArrayList<HelpFeature>> listDataChild) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listDataChild;
    }
    
    public void setActivity(InfoHelpActivity a) {
    	this.activity = a;
    }
    
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).get(childPosititon);
    }
 
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
 
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
    	
    	final HelpFeature child = (HelpFeature) getChild(groupPosition, childPosition);
    	final Context c = this._context;
    	final int grp = groupPosition;
    	final InfoHelpActivity a = this.activity; 
    	
    	if (child.getField() == 0) {
    		LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.help_list_item, null);
            
            TextView head = (TextView) convertView.findViewById(R.id.lblListItemTitle);
            head.setText(child.getTitle());
            
            TextView desc = (TextView) convertView.findViewById(R.id.lblListItemText);
            desc.setText(child.getDescription());
    	} else if (child.getField() == 1){
    		LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.feature_list_item, null);
            
            TextView title = (TextView) convertView.findViewById(R.id.lblListItemTitle);
            title.setText(child.getTitle());
            
            TextView version = (TextView) convertView.findViewById(R.id.lblListItemVersion);
            if (child.getStatus().equals("3")) {
            	version.setText(_context.getString(R.string.implemented_in) + " " + child.getVersion());
            }
            if (child.getStatus().equals("2")) {
            	version.setText(_context.getString(R.string.will_not_be_implemented));
            }
            if (child.getStatus().equals("1")) {
            	version.setText(_context.getString(R.string.will_be_implemented));
            }
            
            TextView desc = (TextView) convertView.findViewById(R.id.lblListItemDescription);
            desc.setText(child.getDescription());
    	} else {
    		LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.input_list_item, null);
            
            TextView head = (TextView) convertView.findViewById(R.id.lblListItemTitle);
            TextView text = (TextView) convertView.findViewById(R.id.lblListItemDescription);
            if (grp == 1) {
                head.setText(_context.getString(R.string.bug_report));
                text.setText(_context.getString(R.string.bug_report_info));
            }
            if (grp == 2){
                head.setText(_context.getString(R.string.feature_request));
                text.setText(_context.getString(R.string.feature_request_info));
            }
            
            Button send = (Button) convertView.findViewById(R.id.update);
            send.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	String gmail = null;
                	AccountManager am = AccountManager.get(c);
                	Account[] ac = am.getAccountsByType("com.google");
                	if (ac.length == 0) {
                		gmail = null;
                	} else {
                		gmail = ac[0].name;
                	}
                	
                	String title;
                	EditText ttl = (EditText) a.findViewById(R.id.feature_input_title);
                	if (ttl.getText() == null) {
                		title = "";
                	} else {
                		title = ttl.getText().toString();
                	}
                	if (title.equals("")) {
                		a.showToast(_context.getString(R.string.please_insert_a_title), 5000);
                	} else {
	                	EditText desc = (EditText) a.findViewById(R.id.feature_input_description);
	                	String description;
	                	if (desc.getText() == null) {
	                		description = "";
	                	} else {
	                		description = desc.getText().toString();
	                	}
	                	if (description.equals("")) {
		                	a.showToast(_context.getString(R.string.please_insert_a_desc), 5000);
		                } else {
		                	if (grp == 1) {
		                		FeatureBugSubmit fbs = new FeatureBugSubmit(gmail, title, description, SendingType.BUG, a);
		                		fbs.send();
		                	}
		                	if (grp == 2) {
		                		FeatureBugSubmit fbs = new FeatureBugSubmit(gmail, title, description, SendingType.FEATURE, a);
		                		fbs.send();
		                	}
                		}
                	}
                } 
            });
    	}
    	
        return convertView;
    }
 
    public int getChildrenCount(int groupPosition) {
    	if (this._listDataChild == null) {
    		return 0;
    	}
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
    }
 
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }
 
    public int getGroupCount() {
    	if (this._listDataHeader == null) {
    		return 0;
    	}
        return this._listDataHeader.size();
    }
 
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }
 
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }
 
        TextView lblListHeader = (TextView) convertView.findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);
 
        return convertView;
    }
 
    public boolean hasStableIds() {
        return false;
    }
 
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
