/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * NOTICE: 
 * 
 * This file was modified by Florian Schwab for Vorlesungsplan-App Mosbach and the
 * modified code sequences have been marked.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package afzkl.development.colorpickerview.preference;

import afzkl.development.colorpickerview.view.ColorPanelView;
import afzkl.development.colorpickerview.view.ColorPickerView;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import de.patricklammers.vorlesungsplan.app.R;

public class ColorPickerPreference extends DialogPreference implements ColorPickerView.OnColorChangedListener {


	private ColorPickerView				mColorPickerView;
	private ColorPanelView				mOldColorView;
	private ColorPanelView				mNewColorView;
	// MODIFIED: Added some hex RGB values as default colors
	private int[] 						mDefaultColors = new int[] { 0xE2001A , 0xFFA000, 0x26808F, 0x7C000E, 0x498500 , 0xCCCCCC};

	private int							mColor;
	
	private boolean						alphaChannelVisible = false;
	private String						alphaChannelText = null;
	private boolean						showDialogTitle = false;
	private boolean						showPreviewSelectedColorInList = true;
	private int							colorPickerSliderColor = -1;
	private int							colorPickerBorderColor = -1;
	// MODIFIED: Added a default value to the color picker preference
	private int							defaultValue = -1;
	
	
	public ColorPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);		
		init(attrs); 
	}
	
	private void init(AttributeSet attrs) {
		// BEGIN MODIFIED: Check all attributes of the preference 
		for(int i = 0; i < attrs.getAttributeCount(); i++) {
			// If a default value has been set it will be saved in our variable
			if(attrs.getAttributeName(i).equals("defaultValue")) {
				defaultValue = attrs.getAttributeIntValue(i, 0xFFFFFFFF);
			}
			
			// If the default value is not persisted it will be persistet now
			if(getPersistedInt(-1) == -1) {
				mColor = defaultValue;
				persistInt(defaultValue);
			}
		}
		// END MODIFIED
		
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPickerPreference);
		
		showDialogTitle = a.getBoolean(R.styleable.ColorPickerPreference_showDialogTitle, false);
		showPreviewSelectedColorInList = a.getBoolean(R.styleable.ColorPickerPreference_showSelectedColorInList, true);
		
		a.recycle();	
		a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPickerView);
		
		alphaChannelVisible = a.getBoolean(R.styleable.ColorPickerView_alphaChannelVisible, false);
		alphaChannelText = a.getString(R.styleable.ColorPickerView_alphaChannelText);		
		colorPickerSliderColor = a.getColor(R.styleable.ColorPickerView_colorPickerSliderColor, -1);
		colorPickerBorderColor = a.getColor(R.styleable.ColorPickerView_colorPickerBorderColor, -1);
		
		a.recycle();
		
		if(showPreviewSelectedColorInList) {
			setWidgetLayoutResource(R.layout.preference_preview_layout);
		}
		
		if(!showDialogTitle) {
			setDialogTitle(null);
		}
				
		setDialogLayoutResource(R.layout.dialog_color_picker);
		
		// MODIFIED: Changed the string from 'android.R.string.ok' to 'R.string.btn_ok' and so on
		setPositiveButtonText(R.string.btn_ok);
		setNegativeButtonText(R.string.btn_cancel);
		
		setPersistent(true);		
	}
		
	@Override
	protected Parcelable onSaveInstanceState() {
		 final Parcelable superState = super.onSaveInstanceState();

		 // Create instance of custom BaseSavedState
		 final SavedState myState = new SavedState(superState);
		 // Set the state's value with the class member that holds current setting value
		 
		 
		 if(getDialog() != null && mColorPickerView != null) {
			 myState.currentColor = mColorPickerView.getColor();
		 }
		 else {
			 myState.currentColor = 0;
		 }

		 return myState;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
	    // Check whether we saved the state in onSaveInstanceState
	    if (state == null || !state.getClass().equals(SavedState.class)) {
	        // Didn't save the state, so call superclass
	        super.onRestoreInstanceState(state);
	        return;
	    }

	    // Cast state to custom BaseSavedState and pass to superclass
	    SavedState myState = (SavedState) state;
	    super.onRestoreInstanceState(myState.getSuperState());
	    // Set this Preference's widget to reflect the restored state
	    if(getDialog() != null && mColorPickerView != null) {
	    	Log.d("mColorPicker", "Restoring color!");	    	
	    	mColorPickerView.setColor(myState.currentColor, true);
	    }
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		ColorPanelView preview = (ColorPanelView) view.findViewById(R.id.preference_preview_color_panel);
		
		if(preview != null) {
			preview.setColor(mColor);
		}
	}
	
	@Override
	protected void onBindDialogView(View layout) {
		super.onBindDialogView(layout);
		
		mColorPickerView = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
		
		mColorPickerView = (ColorPickerView) layout
				.findViewById(R.id.color_picker_view);
		mOldColorView = (ColorPanelView) layout.findViewById(R.id.color_panel_old);
		mNewColorView = (ColorPanelView) layout.findViewById(R.id.color_panel_new);

		/* BEGIN MODIFIED:
		* Removed if-else case that tested for landscape mode because this mode
		* is disabled in this App 
		*/
		((LinearLayout) mOldColorView.getParent()).setPadding(Math
				.round(mColorPickerView.getDrawingOffset()), 0, Math
				.round(mColorPickerView.getDrawingOffset()), 0);
		
		LinearLayout defaultColors = ((LinearLayout) layout.findViewById(R.id.default_colors));
		
		// Setze Farben und Listener der Default Colors
		for(int i = 0; i < defaultColors.getChildCount(); i++) {
			View v = defaultColors.getChildAt(i);
			
			if(v instanceof ColorPanelView) {
				ColorPanelView cpv = (ColorPanelView) v;
				cpv.setColor(mDefaultColors[i]);
				final int c = mDefaultColors[i];
				final ColorPickerPreference thiz = this;
				
				cpv.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						thiz.mColorPickerView.setColor(c);
						thiz.mNewColorView.setColor(c);
					}
				});
			}
		}
		
		defaultColors.setPadding(Math
				.round(mColorPickerView.getDrawingOffset()), 0, Math
				.round(mColorPickerView.getDrawingOffset()), 0);
		
		LinearLayout.LayoutParams params = (LayoutParams) defaultColors.getLayoutParams();
		params.setMargins(0, (int) (params.topMargin - mColorPickerView.getDrawingOffset()), 0, 0);
		defaultColors.setLayoutParams(params);
		// END MODIFIED

		mColorPickerView.setAlphaSliderVisible(alphaChannelVisible);
		mColorPickerView.setAlphaSliderText(alphaChannelText);		
		mColorPickerView.setSliderTrackerColor(colorPickerSliderColor);
		
		if(colorPickerSliderColor != -1) {
			mColorPickerView.setSliderTrackerColor(colorPickerSliderColor);
		}
		
		if(colorPickerBorderColor != -1) {
			mColorPickerView.setBorderColor(colorPickerBorderColor);
		}
		
		mColorPickerView.setOnColorChangedListener(this);
		
		mOldColorView.setColor(mColor);
		mNewColorView.setColor(mColor);
		mColorPickerView.setColor(mColor, true);
	}
	
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if(positiveResult) {
			mColor = mColorPickerView.getColor();
			persistInt(mColor);
			
			notifyChanged();
		} 
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {		
		if(restorePersistedValue) {
			mColor = getPersistedInt(0xFF000000);
		} else {
			mColor = (Integer) defaultValue;
			persistInt(mColor);
		}
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, 0xFF000000);
	}
	
	
	@Override
	public void onColorChanged(int newColor) {
		mNewColorView.setColor(newColor);
	}

	private static class SavedState extends BaseSavedState {
	    // Member that holds the setting's value
	    int currentColor;

	    public SavedState(Parcelable superState) {
	        super(superState);
	    }

	    public SavedState(Parcel source) {
	        super(source);
	        // Get the current preference's value
	        currentColor = source.readInt(); 
	    }

	    @Override
	    public void writeToParcel(Parcel dest, int flags) {
	        super.writeToParcel(dest, flags);
	        // Write the preference's value
	        dest.writeInt(currentColor);
	    }

	    // Standard creator object using an instance of this class
	    public static final Parcelable.Creator<SavedState> CREATOR =
	            new Parcelable.Creator<SavedState>() {

	        public SavedState createFromParcel(Parcel in) {
	            return new SavedState(in);
	        }

	        public SavedState[] newArray(int size) {
	            return new SavedState[size];
	        }
	    };
	}
}
