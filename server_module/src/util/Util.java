package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EmailFormatVerifier{
	//String regex = "^\b(?<name>([a-zA-Z0-9][\w\-]*(\b\.\b)?)+)@(?<domain>[a-zA-Z0-9][a-zA-Z0-9\-]*\b\.([a-zA-Z0-9][a-zA-Z0-9\-]*(\b\.\b)?)+)\b$";
	private String regex = "^\\b(?<name>([a-zA-Z0-9][\\w\\-]*(\\b\\.\\b)?)+)@(?<domain>[a-zA-Z0-9][a-zA-Z0-9\\-]*\\b\\.([a-zA-Z0-9][a-zA-Z0-9\\-]*(\\b\\.\\b)?)+)\\b$";
	private String regex2 = "--";
	private Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
	private Pattern p2 = Pattern.compile(regex2);
	
	
	public boolean verify(String emailAddress){
		Matcher matcher = pattern.matcher(emailAddress);
	 	Matcher matcher2 = p2.matcher(emailAddress);
		if(matcher.find() && !matcher2.find()){
			return true;
		}
		return false;
	}
}

public class Util {
	private static MessageDigest md;
	private static EmailFormatVerifier emailFormatVerifier = new EmailFormatVerifier();
	
	static {
		try {
			md = MessageDigest.getInstance("SHA-256");
		}catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	//bytes in Buffer to String
	public static String parseString(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return new String(bytes); 
	}
	public static String parseUTF8String(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		String resString = null;
		try {
			resString = new String(bytes,"utf-8"); 
		}catch(IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to parse String in parseUTF8String() of class Util, which should never happen");
		}
		return resString;
	}
	public static byte[] parseBytes(ByteBuffer buffer) {
		byte[] bytes = new byte[buffer.getInt()];
		buffer.get(bytes);
		return bytes;
	}
	public static SecureRandom genSecureRandom() {
		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstanceStrong();
		}catch(NoSuchAlgorithmException e) {
			secureRandom = new SecureRandom();
		}
		return secureRandom;
	}
	synchronized public static byte[] hashPassword(byte[] passwordBytes) {
		md.update(passwordBytes);
		return md.digest();
	}
	public static boolean comparePasswordWithHash(byte[] passwordBytes, byte[] hashBytes) {
		byte[] passwordHashBytes = hashPassword(passwordBytes);
		return Arrays.equals(passwordHashBytes, hashBytes);
	}
	public static boolean checkEmailAddressFormat(String emailAddress) {
		if(emailAddress.length() == 0) {
			return false;
		}
		return emailFormatVerifier.verify(emailAddress);
	}
}
