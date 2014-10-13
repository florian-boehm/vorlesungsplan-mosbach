package de.patricklammers.vorlesungsplan.app.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import de.patricklammers.vorlesungsplan.app.R;

public class TimePreference extends DialogPreference {
  private int lastHour=0;
  private int lastMinute=0;
  private View picker=null;
  
  public static int getHour(String time) {
    String[] pieces=time.split(":");
    
    return(Integer.parseInt(pieces[0]));
  }

  public static int getMinute(String time) {
    String[] pieces=time.split(":");
    
    return(Integer.parseInt(pieces[1]));
  }

  public TimePreference(Context ctxt, AttributeSet attrs) {
    super(ctxt, attrs);
    
    setPositiveButtonText("Setzen");
    setNegativeButtonText("Abbrechen");
  }

  @Override
  protected View onCreateDialogView() {
    //picker=new TimePicker(getContext());

	//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    LayoutInflater mInflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    picker= mInflater.inflate(R.layout.refresh_interval_picker, null, false);
	    
	    NumberPicker dayPicker = (NumberPicker) picker.findViewById(R.id.hour_picker);
	    dayPicker.setMinValue(0);
	    dayPicker.setMaxValue(0);
	    
	    dayPicker.setFormatter(new NumberPicker.Formatter() {
			public String format(int value) {
				if(value < 10)
					return "0"+value;
				else
					return ""+value;
			}
		});
	//}
    
    return(picker);
  }
  
  @Override
  protected void onBindDialogView(View v) {
    super.onBindDialogView(v);
    
    //picker.setCurrentHour(lastHour);
    //picker.setCurrentMinute(lastMinute);
  }
  
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);

    if (positiveResult) {
      //lastHour=picker.getCurrentHour();
      //lastMinute=picker.getCurrentMinute();
      
      String time=String.valueOf(lastHour)+":"+String.valueOf(lastMinute);
      
      if (callChangeListener(time)) {
        persistString(time);
      }
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return(a.getString(index));
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    String time=null;
    
    if (restoreValue) {
      if (defaultValue==null) {
        time=getPersistedString("00:00");
      }
      else {
        time=getPersistedString(defaultValue.toString());
      }
    }
    else {
      time=defaultValue.toString();
    }
    
    lastHour=getHour(time);
    lastMinute=getMinute(time);
  }
}