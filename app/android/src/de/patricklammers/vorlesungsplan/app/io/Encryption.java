package de.patricklammers.vorlesungsplan.app.io;

import java.io.UnsupportedEncodingException;

import javax.crypto.SecretKey;

public class Encryption implements SecretKey {
	private static final long serialVersionUID = 1L;
	private String key;
	
	public Encryption(String key) {
		this.key = key;
	}

	public String getAlgorithm() {
		return null;
	}

	public byte[] getEncoded() {
		try {
			return this.key.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public String getFormat() {
		return null;
	}
	
	public Integer getLength() {
		try {
			return this.key.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

}
