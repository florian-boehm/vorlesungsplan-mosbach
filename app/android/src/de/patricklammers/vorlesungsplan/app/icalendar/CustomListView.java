package de.patricklammers.vorlesungsplan.app.icalendar;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import de.patricklammers.vorlesungsplan.app.LunchActivity;
import de.patricklammers.vorlesungsplan.app.MainActivity;
import de.patricklammers.vorlesungsplan.app.R;

/**
 * 
 * @author Florian
 *
 */
@SuppressLint("InlinedApi")
public class CustomListView extends ListView implements OnGestureListener {
	public enum CLWState { NOT_PULLING, READY_TO_PULL, UNTRIGGERED, READY_TO_TRIGGER, TRIGGERED, LOADING };

	private float threshold = 200;
	private float startY = 0;
	private CLWState state = CLWState.NOT_PULLING;
	private MainActivity mActivity = null;
	private View indicator = null;
	private Point screenSize = new Point();
	private CharSequence oldTitle = null;
	private boolean isDisabled = false;
	private GestureDetector gestureDetector = null;
	private int prevEventCount = 0;

	public CustomListView(Context context) {
		super(context);
	}
	
	public CustomListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float y = event.getY();
		
		if(isDisabled) {
			return super.onTouchEvent(event);	
		}
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				if(state == CLWState.READY_TO_PULL || state == CLWState.UNTRIGGERED || state == CLWState.READY_TO_TRIGGER) {
					if((y - startY) > threshold) {
						state = CLWState.READY_TO_TRIGGER;
					} else if((y - startY) < 0) {
						state = CLWState.NOT_PULLING;
					} else {
						state = CLWState.UNTRIGGERED;
					}
	            }
				break;
	        case MotionEvent.ACTION_DOWN:
	            if(getFirstVisiblePosition() == 0 && state == CLWState.NOT_PULLING) {
		            startY = y;
	            	state = CLWState.READY_TO_PULL;
	            }
	            break;
	        case MotionEvent.ACTION_UP:
	        	if(state == CLWState.READY_TO_TRIGGER) {
	        		if(mActivity != null) {
		        		state = CLWState.LOADING;

		        		Date customBegin = mActivity.getCustomBegin();
		        		Calendar cal = Calendar.getInstance(Locale.GERMANY);
		        		
		        		if(customBegin != null) {
		            		cal.setTime(customBegin);		        			
		        		} else {
		        			customBegin = cal.getTime();
		        		}     		
		        		
		        		prevEventCount = mActivity.getCourseCalendar().getEvents(customBegin,null).size();
		        		
		        		do {
			        		cal.add(Calendar.MONTH, -3);
			        		customBegin = cal.getTime();
		        		} while(mActivity.getCourseCalendar().getEvents(customBegin,null).size() <= prevEventCount && mActivity.getCourseCalendar().getEventCount() > 0);
		        		
		    	        mActivity.setCustomBegin(cal.getTime());
		    	        mActivity.prepareCalendarLayout();
		    	        state = CLWState.NOT_PULLING;
	        		}
	        	} else if(state == CLWState.UNTRIGGERED || state == CLWState.READY_TO_PULL) {
	        		state = CLWState.NOT_PULLING;
	        	}
	    }
		
		if(this.mActivity != null) {
			this.indicator = mActivity.findViewById(R.id.pull_for_history);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				this.mActivity.getWindowManager().getDefaultDisplay().getSize(screenSize);
			} else {
				this.screenSize.x = this.mActivity.getWindowManager().getDefaultDisplay().getWidth();
				this.screenSize.y = this.mActivity.getWindowManager().getDefaultDisplay().getHeight();
			}
			this.threshold = screenSize.y/3;
			
			if(this.oldTitle == null) {
				this.oldTitle = this.mActivity.getTitle();
			}
		}
		
		if(indicator != null) {
			switch(state) {
				case READY_TO_PULL:
					if(y - startY > 5) {
						this.mActivity.setTitle(mActivity.getString(R.string.pull_down));
						this.indicator.setVisibility(VISIBLE);		
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
							this.mActivity.getActionBar().setSubtitle(mActivity.getString(R.string.to_show_past_events));
							this.mActivity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
						}
					}
					break;
				case LOADING:
				case NOT_PULLING:
					this.indicator.setVisibility(GONE);
					this.mActivity.setTitle(oldTitle);
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						this.mActivity.getActionBar().setSubtitle(null);
						this.mActivity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE);
					}
					break;
				case UNTRIGGERED:
					float actualPosition = y - startY;
					if(actualPosition < 0) {
						actualPosition = 0;
					}
					
					android.view.ViewGroup.LayoutParams lp1 = indicator.getLayoutParams();
					lp1.width = (int) Math.ceil((screenSize.x) * ((Math.exp(actualPosition/threshold)-1)/(Math.E-1)));
					indicator.setLayoutParams(lp1);
					
					if(y - startY > 5) {
						this.indicator.setVisibility(VISIBLE);		
						this.mActivity.setTitle(mActivity.getString(R.string.pull_down));	
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
							this.mActivity.getActionBar().setSubtitle(mActivity.getString(R.string.to_show_past_events));
							this.mActivity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
						}
					}
					break;
				case READY_TO_TRIGGER:
					android.view.ViewGroup.LayoutParams lp2 = indicator.getLayoutParams();
					lp2.width = screenSize.x;
					indicator.setLayoutParams(lp2);

					mActivity.setTitle(mActivity.getString(R.string.release_now));
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						this.mActivity.getActionBar().setSubtitle(null);
					}
					break;
			default:
				break;
			}
		}
		
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event){
	    super.dispatchTouchEvent(event);
	    
	    if(gestureDetector == null) {
	    	return false;
	    } else {
	    	return gestureDetector.onTouchEvent(event); 
	    }
	}

	public void setMainActivity(MainActivity mActivity) {
		this.mActivity = mActivity;
		this.gestureDetector = new GestureDetector(mActivity.getBaseContext(),this);
	}
	
	public void enable() {
		this.isDisabled = false;
	}
	
	public void disable() {
		this.isDisabled = true;
	}

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if(velocityX < -2000.0 && Math.abs(velocityY) < 4000) {
			Intent lunchMenuIntent = new Intent(mActivity.getApplicationContext(), LunchActivity.class);
	    	lunchMenuIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	        mActivity.startActivity(lunchMenuIntent);
	    	mActivity.overridePendingTransition(R.anim.move_in_right, R.anim.move_out_left);
			return true;
		} else {
			return false;
		}
	}

	public void onLongPress(MotionEvent e) {
		
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	public void onShowPress(MotionEvent e) {
		
	}

	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	public void resetOldTitle() {
		this.oldTitle = null;
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
	}
}
