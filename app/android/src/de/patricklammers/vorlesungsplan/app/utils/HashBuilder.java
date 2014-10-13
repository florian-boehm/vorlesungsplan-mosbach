package de.patricklammers.vorlesungsplan.app.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Diese Klasse
 * 
 * @author Florian
 * @version 21.01.2013
 */
public class HashBuilder
{
	/**
	 * Diese Methode berechnet den MD5 Hash des Übergebenen
	 * 
	 * @param input
	 * @return
	 */
	public static String toMD5(String input, String charset)
	{
		try
		{
			// Default Charset ist "ISO-8859-1"
			charset = (charset == null || charset.equals("")) ? "ISO-8859-1" : charset;
			
			return byteArray2Hex(toMD5(input.getBytes(charset))).toUpperCase();
		}
		catch (UnsupportedEncodingException e)
		{
			// e.printStackTrace();
			return null;
		}
	}
	
	public static byte[] toMD5(byte[] input)
	{
		try
		{
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return md5.digest(input);
		}
		catch (NoSuchAlgorithmException e)
		{
			return null;
		}
	}
	
	public static String toSHA1(String input, String charset)
	{
		try
		{
			// Default Charset ist "ISO-8859-1"
			charset = (charset == null || charset.equals("")) ? "ISO-8859-1" : charset;
			return byteArray2Hex(toSHA1(input.getBytes(charset)));
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}

	public static byte[] toSHA1(byte[] input)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			return md.digest(input);
		}
		catch (NoSuchAlgorithmException e)
		{
			return null;
		}
	}
	
	public static String byteArray2Hex(final byte[] hash) 
	{
	    Formatter formatter = new Formatter();
	    String result = "";
	    
	    for (byte b : hash) 
	    {
	        formatter.format("%02x", b);
	    }
	    
	    result = formatter.toString();
	    formatter.close();
	    
	    return result;
	}
}
