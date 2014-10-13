package de.patricklammers.vorlesungsplan.app.icalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.patricklammers.vorlesungsplan.app.MainActivity;
import de.patricklammers.vorlesungsplan.app.R;
import de.patricklammers.vorlesungsplan.app.icalendar.ICalendarComperator.EventModifiedStatus;
import de.patricklammers.vorlesungsplan.app.io.Logbook;
import de.patricklammers.vorlesungsplan.app.settings.Settings;

/**
 * Der VeventAdapter managed die Liste aller anzuzeigenden Termine und das
 * Zusammenspiel mit der ListView, die als tatsächliches Anzeigeelement
 * diesen Adapter übergeben bekommt. Der Adapter kann auch ein zwei Arten
 * von Informationsfeldern anzeigen:
 * 
 * - Wenn kein Kurs ausgewählt wurde
 * - Wenn keine Termine in der Zukunft liegen
 * 
 * @author Florian Schwab
 */
public class VeventAdapter extends BaseAdapter
{
	// Die Events, die dieser Adapter managed
	private ArrayList<Vevent> events;
	// Diese ArrayList dient dazu festzustellen, welcher Termin, der erste an 
	// einem Tag ist und deshalb auch das Datum und die Kalenderwoche anzeigt
	private ArrayList<AdditionalEventInfo> additionalEventInfo;
	// 2 Flags, wenn die ArrayList der Events leer ist oder eine Info angezeigt werden soll
	private boolean isEmpty = false;
	private boolean showInfo = false;
	private MainActivity mActivity;

	/**
	 * Erzeugt einen neuen VeventAdapter
	 * 
	 * @param context Der Context <=> die Activity, in der die ListView angezeigt wird
	 * @param events Eine Liste mit allen Terminen, die angezeigt werden sollen
	 */
	public VeventAdapter(MainActivity mActivity, ArrayList<Vevent> events) {
		this.mActivity = mActivity;
		this.events = events;
		
		// Sonderfall: items == null, dann wurde noch kein Kurs ausgewählt
		if(this.events == null) {
			showInfo = true;
			mActivity.setPullForHistoryEnabled(false);
			return;
		}			
		
		// Wenn die Liste leer ist, dann existieren keine Termine in der Zukunft
		if(this.events.size() == 0) {
			this.events.add(new Vevent());
			isEmpty = true;
		} else {
			// Wenn bereits alle Termine angezeigt werden, dann wird PullForHistory disabled
			if(this.events.size() == mActivity.getCourseCalendar().getCountOfAllEvents()) {
				mActivity.setPullForHistoryEnabled(false);
			}
			
			prepareFirstAtDayList();
		}
	}

	private void prepareFirstAtDayList() {
		// In dieser Liste wird eingetragen, ob der Termin der erste Termin am Tag ist
		additionalEventInfo = new ArrayList<AdditionalEventInfo>();
		 
		Calendar cal = Calendar.getInstance(Locale.GERMAN);
		Date lastDate = null;
		Date actualDate = null;
		int lastFirstEventAtDayPos = 0;
		int i = 0;
		
		// Dazu werden alle Termine durchlaufen
		for(i = 0; i < this.events.size(); i++) {	
			// Beim Datum des aktuellen Termins werden die Zeiten auf 0 gesetzt
			if(events.get(i).getEventBegin() != null) {
				actualDate = getZeroTimeDate(events.get(i).getEventBegin());
 			} else {
 				additionalEventInfo.add(i, new AdditionalEventInfo(false));
 				continue;
 			}
			
			AdditionalEventInfo aei = new AdditionalEventInfo();
			
			// Wenn es der erste Termin in der gesamten Liste ist oder das letzte Datum vor
			// dem aktuellen Datum liegt, dann ist der aktuelle Termin der erste Termin am Tag
			if(lastDate == null || lastDate.compareTo(actualDate) < 0) {
				aei.isFirstAtDay = true;
				lastFirstEventAtDayPos = i;
			} else {
				aei.isFirstAtDay = false;
			}
			
			additionalEventInfo.add(i, aei);
			
			// Finde die Farbe der Leiste heraus
			String eventDesc = events.get(i).getEventSummary().toLowerCase();
			int startColor = Color.parseColor("#e2001a");
			int colorPrio = 10;
			
			if(eventDesc.contains("klausur") || eventDesc.contains("test") || eventDesc.contains("prüfung")) {
				String colorCode = Settings.getInstance(mActivity).getSetting("examsColor");
				colorCode = (colorCode == null) ? "#e2001a" : colorCode;
				startColor = Color.parseColor(colorCode);
				colorPrio = 20;
			} else if(eventDesc.contains("studientag")) {
				String colorCode = Settings.getInstance(mActivity).getSetting("freeDaysColor");
				colorCode = (colorCode == null) ? "#e2001a" : colorCode;
				startColor = Color.parseColor(colorCode);
				colorPrio = 0;
			}
			
			cal.setTime(actualDate);
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			
			if(dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
				String colorCode = Settings.getInstance(mActivity).getSetting("weekendLectureColor");
				colorCode = (colorCode == null) ? "#e2001a" : colorCode;
				startColor = Color.parseColor(colorCode);
				colorPrio = 15;
			}
			
			if(events.get(i).getEventEnd().compareTo(Calendar.getInstance(Locale.GERMANY).getTime()) < 0) {
				startColor = Color.parseColor("#bfc5c8");
				colorPrio = 30;
			}
			
			additionalEventInfo.get(lastFirstEventAtDayPos).setBarStartColor(startColor, colorPrio);
			
			// Nach der Überprüfung wird das aktuelle Datum zum letzten Datum
			lastDate = actualDate;
		}
	}

	/**
	 * Setzt die Zeiten des Date Objects auf 0 um einen Vegleich zwischen zwei
	 * Date-Objekten auf Tagesbasis zu ermöglichen
	 * 
	 * @param date Das Datum, dessen Zeiten auf 0 gesetzt werden sollen
	 * @return Das veränderte Datum
	 */
	private Date getZeroTimeDate(Date date) {
		Calendar cal = Calendar.getInstance(Locale.GERMANY);

		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTime();
	}

	/**
	 * Erzeugt eine View, die dann in der ListView angezeigt wird
	 * 
	 * @param int Die Position der View (= entspricht dem Index im events-Array)
	 * @param View Wenn schon einmal eine View für diese Position erzeugt wurde, dann wird sie hier übergeben
	 * @param ViewGroup Die ViewGroup, der die View untergeordnet ist
	 * @return Die View, die in der ListView angezeigt wird
	 */
	@SuppressWarnings("deprecation")
	public View getView(int position, View convertView, ViewGroup parent) {
		// Logbook.d("VeventAdapter.getView","position: "+position);
		//Logbook.entry()		
		ViewHolder holder = null;
		
		// Wenn nur eine Info Zeile angezeigt werden soll, die darauf hinweist, dass ein Kurs gewählt werden muss
		if(showInfo) {
			if(convertView != null && (convertView.findViewById(R.id.infotext) == null && convertView.findViewById(R.id.infotext_v11) == null)) {
				convertView = null;
			}
			
			if(convertView == null)	{
				LayoutInflater inflater = (LayoutInflater) this.mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				// Ab Geräten mit Honeycomb is der Menübutton in der ActionBar (das ist nicht immer richtig!)
				// Wenn ein Gerät einen Hardware-Menübutton hat, dann wird die "inforow" angezeigt
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH || ViewConfiguration.get(mActivity).hasPermanentMenuKey()) {
					convertView = inflater.inflate(R.layout.inforow, null);
				} else {
					convertView = inflater.inflate(R.layout.inforow_v11, null);		
				}
			}
			
			return convertView;
		}
		
		// Wenn es keine Termine gibt, die in der Zukunft liegen und somit nichts angezeigt wird
		if(isEmpty)	{
			if(convertView != null && convertView.findViewById(R.id.update_notification_added) == null) {
				convertView = null;
			}
			
			// dann wird eine View aus der emptyrow.xml erzeugt, die den entsprechenden Hinweis enthält
			if(convertView == null)	{
				LayoutInflater inflater = (LayoutInflater) this.mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.emptyrow, null);
			}
			
			return convertView;
		}
		
		Vevent event = null;
		
		// Das passende Element wird aus der Liste geholt
		if(this.getItem(position) != null) {
			event = (Vevent) this.getItem(position);
		} else {
			Logbook.w("Needed an Vevent but found none!");
			return convertView;
		}
		
		// Wenn die Zeile eine Dummyrow sein soll
		if(event.getUid().equals("DUMMY_ROW")) {
			LayoutInflater inflater = (LayoutInflater) this.mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.dummyrow, null);
			View tempView = convertView.findViewById(R.id.dummyrow);
			
			if(tempView != null) {
				final int height = Integer.valueOf(event.getEventSummary());
				
				if(tempView.getLayoutParams() != null) {
					tempView.getLayoutParams().height = height;
					tempView.setLayoutParams(tempView.getLayoutParams());
				} else {
					AbsListView.LayoutParams params = new AbsListView.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, height);
					tempView.setLayoutParams(params);
				}
			}
			
			return convertView;
		}
		
		// Wenn die Zeile "Delted Event Row" sein soll
		if(event.getEventDescription().equals("REMOVED_EVENT")) {
			LayoutInflater inflater = (LayoutInflater) this.mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.deleted_event_row, null);
			TextView tempView = (TextView) convertView.findViewById(R.id.event_removed_text);
			
			if(tempView != null) {
				tempView.setText(tempView.getText()+" "+event.getEventSummary());
				
				// Wenn der Termin der erste Termin am Tag ist ...
				if(additionalEventInfo.get(position).isFirstAtDay) {
					SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE - dd.MM.yyyy", Locale.getDefault());
					Calendar cal = Calendar.getInstance(Locale.GERMANY);
					cal.setFirstDayOfWeek(Calendar.MONDAY);
					cal.setTime(event.getEventBegin());

					// ... dann sollen auch das Datum und die Kalenderwoche sichtbar sein
					((TextView) convertView.findViewById(R.id.event_date)).setText(dateFormatter.format(event.getEventBegin()));
					if(Locale.getDefault().toString().startsWith("en")) {
						((TextView) convertView.findViewById(R.id.event_cw)).setText("CW: "+cal.get(Calendar.WEEK_OF_YEAR));			
					} else {
						((TextView) convertView.findViewById(R.id.event_cw)).setText("KW: "+cal.get(Calendar.WEEK_OF_YEAR));					
					}
					
					convertView.findViewById(R.id.event_date).setVisibility(View.VISIBLE);
					convertView.findViewById(R.id.event_cw).setVisibility(View.VISIBLE);
				} else {
					// ansonsten wird beides ausgeblendet (da es sonst redundante Informationen dargestellt werden)
					convertView.findViewById(R.id.event_date).setVisibility(View.GONE);
					convertView.findViewById(R.id.event_cw).setVisibility(View.GONE);
				}
			}
			
			return convertView;
		}
		
		// Wenn die View schon einmal als DummyRow missbraucht wurde, dann muss sie neu erzeugt werden!
		if(convertView != null && (convertView.findViewById(R.id.dummyview) != null || 
									convertView.findViewById(R.id.event_removed_text) != null || 
									convertView.findViewById(R.id.update_notification_added) != null || 
									convertView.findViewById(R.id.infotext) != null ||
									convertView.findViewById(R.id.infotext_v11) != null)) {
			convertView = null;
		}

		// Wenn ein Termin angezeigt werden soll und bisher keine View erzeugt wurde ...
		if(convertView == null)	{
			// ... dann wird eine entsprechende View angelegt
			LayoutInflater inflater = (LayoutInflater) this.mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.row, null);
			holder = new ViewHolder();
			// und deren Elemente (TextViews) in einem Holder gespeichert
			holder.event_begin = (TextView) convertView.findViewById(R.id.event_begin);
			holder.event_end = (TextView) convertView.findViewById(R.id.event_end);
			holder.event_date = (TextView) convertView.findViewById(R.id.event_date);
			holder.event_cw = (TextView) convertView.findViewById(R.id.event_cw);
			holder.event_description = (TextView) convertView.findViewById(R.id.event_description);
			holder.event_location = (TextView) convertView.findViewById(R.id.event_location);
			holder.event_summary = (TextView) convertView.findViewById(R.id.event_summary);
			holder.horizontal_line = (View) convertView.findViewById(R.id.horizontal_line);
			holder.horizontal_line_progress = (View) convertView.findViewById(R.id.horizontal_line_progress);
			
			// Mache den Hintergrund von event_date und event_cw mutable, weil sie den gleichen State teilen, wenn sie von der gleichen Resource stammen
			holder.event_date.getBackground().mutate();
			holder.event_cw.getBackground().mutate();
			
			convertView.setTag(holder);
		} else {
			// Wenn bereits eine View angelegt war, dann wird dessen Holder ausgelesen
			holder = (ViewHolder) convertView.getTag();
		}
		
		// Hier werden die Formatter und die Calendar-Instanzen vorbereitet
		SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE - dd.MM.yyyy", Locale.getDefault());
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.GERMAN);
		Calendar cal = Calendar.getInstance(Locale.GERMANY);
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(event.getEventBegin());
		
		// Hier wird die View mit den tatsächlichen Inhalten gefüllt
		holder.event_date.setText(dateFormatter.format(event.getEventBegin()));
		holder.event_begin.setText(timeFormatter.format(event.getEventBegin()));
		holder.event_end.setText(timeFormatter.format(event.getEventEnd()));
		if(Locale.getDefault().toString().startsWith("en")) {
			holder.event_cw.setText("CW: "+cal.get(Calendar.WEEK_OF_YEAR));			
		} else {
			holder.event_cw.setText("KW: "+cal.get(Calendar.WEEK_OF_YEAR));					
		}
		holder.event_description.setText(event.getEventDescription());
		holder.event_location.setText(event.getEventLocation());
		holder.event_summary.setText(event.getEventSummary());
		
		// Wenn der Termin der erste Termin am Tag ist ...
		if(additionalEventInfo.get(position).isFirstAtDay) {
			// ... dann sollen auch das Datum und die Kalenderwoche sichtbar sein
			holder.event_date.setVisibility(View.VISIBLE);
			holder.event_cw.setVisibility(View.VISIBLE);
			
			int[] padding = { 	holder.event_date.getPaddingLeft(),
					holder.event_date.getPaddingTop(), 
					holder.event_date.getPaddingRight(), 
					holder.event_date.getPaddingBottom(),
					holder.event_cw.getPaddingLeft(),
					holder.event_cw.getPaddingTop(), 
					holder.event_cw.getPaddingRight(), 
					holder.event_cw.getPaddingBottom() };
			
			holder.event_date.setPadding(padding[0], padding[1], padding[2], padding[3]);
			holder.event_cw.setPadding(padding[4], padding[5], padding[6], padding[7]);
			
			setColorOfBar(holder, additionalEventInfo.get(position).barStartColor);
		} else {			
			holder.event_date.setVisibility(View.GONE);
			holder.event_cw.setVisibility(View.GONE);
		}

		// Wenn der Termin der aktuell stattfindende Termin ist, dann wird die horizontale Linie prozentual eingefärbt
		if(event.getEventBegin().compareTo(Calendar.getInstance(Locale.GERMANY).getTime()) < 0 && event.getEventEnd().compareTo(Calendar.getInstance(Locale.GERMANY).getTime()) > 0) {
			LayoutParams layoutParams = holder.horizontal_line_progress.getLayoutParams();
			
			// Zeitanteilberechnung
			Calendar c = Calendar.getInstance(Locale.GERMANY);			
			int actualHours = c.get(Calendar.HOUR_OF_DAY);
			int actualMinutes = c.get(Calendar.MINUTE);
			
			c.setTime(event.getEventEnd());
			int endHours = c.get(Calendar.HOUR_OF_DAY);
			int endMinutes = c.get(Calendar.MINUTE);
			
			c.setTime(event.getEventBegin());
			int eventHours = endHours - c.get(Calendar.HOUR_OF_DAY);
			int eventMinutes = endMinutes - c.get(Calendar.MINUTE);
			actualHours -= c.get(Calendar.HOUR_OF_DAY);
			actualMinutes -= c.get(Calendar.MINUTE);
			
			double eventDuration = eventHours+(eventMinutes/60.0);
			double actualEventDuration = actualHours+(actualMinutes/60.0);
					
			// Messe die View bevor sie gerendert wurde
			if(holder.horizontal_line.getMeasuredWidth() == 0) {
				Point p = new Point();
				
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
					p.set(mActivity.getWindowManager().getDefaultDisplay().getWidth(), mActivity.getWindowManager().getDefaultDisplay().getHeight());
				} else {				
					mActivity.getWindowManager().getDefaultDisplay().getSize(p);
				}
				
				int widthMeasureSpec = MeasureSpec.makeMeasureSpec(p.x, MeasureSpec.EXACTLY);
				int heightMeasureSpec = MeasureSpec.makeMeasureSpec(p.y, MeasureSpec.EXACTLY);
				convertView.measure(widthMeasureSpec, heightMeasureSpec);
			}
			
			layoutParams.width = (int) Math.ceil(holder.horizontal_line.getMeasuredWidth() * (actualEventDuration/eventDuration));
			holder.horizontal_line_progress.setLayoutParams(layoutParams);
			holder.horizontal_line_progress.setVisibility(View.VISIBLE);
		} else {
			holder.horizontal_line_progress.setVisibility(View.GONE);
		}
		
		// Prüfe ob ein Markup für den Termin besteht
		if(mActivity.getDifferencesFromLastUpdate() != null && mActivity.getDifferencesFromLastUpdate().events.containsKey(event.getUid())) {
			holder.event_summary.setTextColor(mActivity.getResources().getColor(R.color.orange));
			
			if(mActivity.getDifferencesFromLastUpdate().events.get(event.getUid()).equals(EventModifiedStatus.ROOM_CHANGED)) {
				holder.event_summary.setText(holder.event_summary.getText() + " (" + mActivity.getString(R.string.room_changed) + ")");
			} else if (mActivity.getDifferencesFromLastUpdate().events.get(event.getUid()).equals(EventModifiedStatus.DATE_CHANGED)) {
				holder.event_summary.setText(holder.event_summary.getText() + " (" + mActivity.getString(R.string.date_changed) + ")");
			} else if (mActivity.getDifferencesFromLastUpdate().events.get(event.getUid()).equals(EventModifiedStatus.NEW)) {
				holder.event_summary.setText(holder.event_summary.getText() + " (" + mActivity.getString(R.string.new_event) + ")");
			}
		} else {
			holder.event_summary.setTextColor(mActivity.getResources().getColor(R.color.dhbw_grau));
		}

		return convertView;
	}

	@SuppressWarnings("deprecation")
	private void setColorOfBar(ViewHolder holder, int startColor) {
		int endColor = Color.parseColor("#8f1016");
		
		// Berechne Endfarbe (etwas dunkler)
		if(startColor == Color.parseColor("#e2001a")) {
			endColor = Color.parseColor("#8f1016");
		} else if(startColor == Color.parseColor("#bfc5c8")) {
			endColor = Color.parseColor("#7d8990");
		} else {
			float[] hsv = new float[3];
			Color.colorToHSV(startColor, hsv);
			hsv[2] -= 0.1;
			hsv[2] = (hsv[2] < 0) ? 0 : hsv[2];
			
			endColor = Color.HSVToColor(hsv);
		}
		
		if(android.os.Build.VERSION.SDK_INT >= 16) {
			((GradientDrawable) ((LayerDrawable) holder.event_date.getBackground()).getDrawable(0)).setColors(new int[] {endColor,startColor});
			((GradientDrawable) ((LayerDrawable) holder.event_cw.getBackground()).getDrawable(0)).setColors(new int[] {endColor,startColor});
		} else {
			// Fallback für APIs unter 16
			GradientDrawable ngd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {endColor,startColor});
			ngd.mutate();
			holder.event_date.setBackgroundDrawable(ngd);
			holder.event_cw.setBackgroundDrawable(ngd);
		}
	}

	/**
	 * Liefert die Anzahl der Elemente, die aktuell in der ListView angezeigt werden sollen
	 * 
	 * @return Die Anzahl der Elemente in der ListView
	 */
	public int getCount() {
		if(events == null) {
			return 1;
		} else {		
			return events.size();
		}
	}

	/**
	 * Liefert- das Element an einer gewünschten Position
	 * 
	 * @param int Die Position, des Elements
	 * @return Das Element an der spezifizierten Position
	 */
	public Object getItem(int position) {
		if(events == null) {
			return null;
		} else {
			return events.get(position);
		}
	}

	/** 
	 * Die ItemId ist hier einfach die position selber
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Der ViewHolder dient dazu, die Views zwischenzuspeichern, damit sie nicht immer wieder neu
	 * per findViewById herausgesucht werden müssen. Dies soll Performance bringen
	 *  
	 * @author Florian Schwab
	 */
	static class ViewHolder {
		TextView event_summary;
		TextView event_begin;
		TextView event_end;
		TextView event_cw;
		TextView event_location;
		TextView event_date;
		TextView event_description;
		View horizontal_line;
		View horizontal_line_progress;
	}

	public void addDummyRowAtEnd(int heightInPixels) {
		Vevent tempEvent = new Vevent();
		tempEvent.setUid("DUMMY_ROW");
		tempEvent.setEventSummary(Integer.toString((heightInPixels < 0) ? 0 : heightInPixels));
		events.add(tempEvent);
		additionalEventInfo.add(new AdditionalEventInfo(false));
	}
	
	public void removeDummyRowAtEnd() {
		final int lastElement = events.size() -1;
		
		if(events.get(lastElement).getUid().equals("DUMMY_ROW")) {
			events.remove(lastElement);
			additionalEventInfo.remove(lastElement);
		}
	}

	public void insertRemovedEvent(final String key, boolean autoSort) {
		final String[] split = key.split("\\|");
		
		if(split.length == 2) {
			final String uid = split[0];
			final String summary = split[1];
			final String date = uid.substring(uid.length()-13);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH:mm", Locale.GERMANY);
			
			Vevent tempEvent = new Vevent();
			tempEvent.setUid(uid);
			tempEvent.setEventSummary(summary);
			tempEvent.setEventDescription("REMOVED_EVENT");
			
			try {
				tempEvent.setEventBegin(sdf.parse(date));
				tempEvent.setEventEnd(sdf.parse(date));
				events.add(tempEvent);
				
				if(autoSort) {
					Collections.sort(events);
					prepareFirstAtDayList();
				}
			} catch (ParseException e) {
				Logbook.e(e);
				return;
			}
		}
	}

	public void sortEvents() {
		Collections.sort(events);
		prepareFirstAtDayList();
	}
	
	public void removeRemovedEvents() {
		ArrayList<Vevent> eventsToRemove = new ArrayList<Vevent>();
		
		for(int i = 0; i < events.size(); i++) {
			if(events.get(i).getEventDescription().equals("REMOVED_EVENT")) {
				eventsToRemove.add(events.get(i));
			}
		}
		
		for(Vevent e : eventsToRemove) {
			events.remove(e);
		}
		
		prepareFirstAtDayList();
	}

	public void replaceAllEventsWith(ArrayList<Vevent> events) {
		this.events = events;
		this.isEmpty = false;
		this.showInfo = false;
		
		// Sonderfall: items == null, dann wurde noch kein Kurs ausgewählt
		if(this.events == null) {
			showInfo = true;
			mActivity.setPullForHistoryEnabled(false);
			return;
		}			
		
		// Wenn die Liste leer ist, dann existieren keine Termine in der Zukunft
		if(this.events.size() == 0) {
			this.events.add(new Vevent());
			isEmpty = true;
		} else {
			// Wenn bereits alle Termine angezeigt werden, dann wird PullForHistory disabled
			if(this.events.size() == mActivity.getCourseCalendar().getCountOfAllEvents()) {
				mActivity.setPullForHistoryEnabled(false);
			}
			
			prepareFirstAtDayList();
		}
		
		this.notifyDataSetChanged();
	}
	
	private class AdditionalEventInfo {
		private boolean isFirstAtDay = false;
		private int	barStartColor = Color.parseColor("#e2001a");
		private int colorPrio = -1;
		
		public AdditionalEventInfo(boolean isFirstAtDay) {
			this.isFirstAtDay = isFirstAtDay;
		}

		public void setBarStartColor(int color, int prio) {
			if(prio > colorPrio) {
				barStartColor = color;
				colorPrio = prio;
			}
		}

		public AdditionalEventInfo() {
		}
	}
}
