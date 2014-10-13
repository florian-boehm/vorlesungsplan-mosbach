package de.patricklammers.vorlesungsplan.app.icalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Diese Klasse repräsentiert einen Vorlesungstermin und seine verschiednen Eigenschaften
 * 
 * @author Patrick Lammers
 */
public class Vevent implements Comparable<Vevent> {
	private Date eventBegin;
	private Date eventEnd;
	private String eventSummary = "";
	private String eventLocation = "";
	private String eventDescription = "";
	private String uid = "";

	/**
	 * Setzt das Anfangsdatum des Events. 
	 * 
	 * @param begin Das Anfangsdatum als String in der Form: yyyyMMdd'T'HHmmss
	 */
	public void setEventBegin(String begin) {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.GERMANY);
		try {
			this.eventBegin = format.parse(begin);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public void setEventBegin(Date begin) {
		this.eventBegin = begin;
	}
	
	/**
	 * Setzt das Enddatum des Events.
	 *
	 * @param end Das Enddatum als String in der Form: yyyyMMdd'T'HHmmss
	 */
	public void setEventEnd(String end) {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.GERMANY);
		try {
			this.eventEnd = format.parse(end);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public void setEventEnd(Date end) {
		this.eventEnd = end;
	}
	
	/**
	 * Setzt den Ort des Events
	 * 
	 * @param location Der Ort des Events als String
	 */
	public void setEventLocation(String location) {
		this.eventLocation = location;
	}
	
	/**
	 * Setzt die Zusammenfassung des Events (hier der Vorlesungsname)
	 * 
	 * @param summary
	 */
	public void setEventSummary(String summary){
		this.eventSummary = summary;
	}

	/**
	 * Setzt die Beschreibung des Events (hier der Dozentname)
	 * 
	 * @param eventDescription
	 */
	public void setEventDescription(String eventDescription) {
		this.eventDescription = eventDescription;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		
		if(obj instanceof Vevent) {
			Vevent event = (Vevent) obj;
			boolean result = true;
			
			if(event.getUid() != null && this.getUid() != null) {
				if(result && event.getEventBegin().compareTo(this.getEventBegin()) != 0) {
					result = false;
				}
			} else {
				if(event.getUid() != this.getUid()) {
					result = false;
				}
			}
			
			if(event.getEventEnd() != null && this.getEventEnd() != null) {
				if(result && event.getEventEnd().compareTo(this.getEventEnd()) != 0) {
					result = false;
				}
			} else {
				if(event.getEventEnd() != this.getEventEnd()) {
					result = false;
				}
			}
			
			if(event.getEventDescription() != null && this.getEventDescription() != null) {
				if(result && !event.getEventDescription().equals(this.getEventDescription())) {
					result = false;
				}
			} else {
				if(event.getEventDescription() != this.getEventDescription()) {
					result = false;
				}
			}
			
			if(event.getEventLocation() != null && this.getEventLocation() != null) {
				if(result && !event.getEventLocation().equals(this.getEventLocation())) {
					result = false;
				}
			} else {
				if(event.getEventLocation() != this.getEventLocation()) {
					result = false;
				}
			}
			
			if(event.getEventSummary() != null && this.getEventSummary() != null) {
				if(result && !event.getEventSummary().equals(this.getEventSummary())) {
					result = false;
				}
			} else {
				if(event.getEventSummary() != this.getEventSummary()) {
					result = false;
				}
			}
				
			return result;
		}

		return false;
	}

	/**
	 * Setzt die UID des Events
	 * 
	 * @param uid
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}
	
	public Date getEventBegin() {
		return this.eventBegin;
	}
	
	public Date getEventEnd() {
		return this.eventEnd;
	}
	
	public String getEventLocation() {
		return this.eventLocation;
	}
	
	public String getEventSummary() {
		return this.eventSummary;
	}
	
	public String getEventDescription() {
		return eventDescription;
	}

	public String getUid() {
		return uid;
	}

	public int compareTo(Vevent another) {
		if(another == null || this.getEventBegin() == null || another.getEventBegin() == null) {
			return 0;
		}
		
		return this.getEventBegin().compareTo(another.getEventBegin());
	}
}
