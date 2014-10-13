package de.patricklammers.vorlesungsplan.app.info;

public class HelpFeature {
	private Integer def;
	private String status = null;
	private String version = null;
	private String title = null;
	private String description = null;
	
	public HelpFeature(String title, String description) {
		this.def = 0;
		this.description = description;
		this.title = title;
	}
	
	public HelpFeature(String status, String version, String title, String description) {
		this.def = 1;
		this.status = status;
		this.description = description;
		this.title = title;
		this.version = version;
	}
	
	public HelpFeature() {
		this.def = 2;
	}
	
	public Integer getField() {
		return this.def;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public String toString() {
		if (this.def == 0) {
			return "Title: " + this.getTitle() + "\nDescription: " + this.getDescription();
		} else if (this.def == 1){
			return "Status: " + this.getStatus() + "\nVersion: " + this.getVersion() + "\nTitle: " + this.getTitle() + "\nDescription: " + this.getDescription();
		} else {
			return "inputmask";
		}
	}
}
