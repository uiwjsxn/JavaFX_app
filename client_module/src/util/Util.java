package util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

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

//the methods in Util should be thread-safe
public class Util {
	private static EmailFormatVerifier emailFormatVerifier = new EmailFormatVerifier();
	
	/*private static void deleteDir(File dir) throws IOException{
		if(!dir.isDirectory()) {
			Files.deleteIfExists(dir.toPath());
		}else {
			for(File file : dir.listFiles()) {
				if(file.isDirectory()) {
					deleteDir(file);
				}else {
					Files.deleteIfExists(file.toPath());
					//System.out.println("delete: " + file.getName());
				}
			}
			Files.deleteIfExists(dir.toPath());
		}
	}
	//if the previous methods fails, try this one
	private static void deleteDirOnExit(File dir){
		if(!dir.isDirectory()) {
			dir.deleteOnExit();
		}else {
			//when you call File.deleteOnExit(), you must "delete" the directory first, then the "delete" the files and subDirectory in the directory,
			//which is just the opposite of Files.deleteIfExists(Path), where you delete all the files and subDirectory first, then delete the whole directory
			dir.deleteOnExit();
			for(File file : dir.listFiles()) {
				if(file.isDirectory()) {
					deleteDirOnExit(file);
				}else {
					//System.out.println("delete: " + file.getName());
					file.deleteOnExit();
				}
			}
		}
	}*/
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
	public static ByteBuffer bytesToBuffer(byte[][] bytes) {
		int size = 0;
		for(int i = 0; i < bytes.length; ++i) {
			size += bytes[i].length;
		}
		ByteBuffer buffer = ByteBuffer.allocate(size + bytes.length*4);
		for(int i = 0; i < bytes.length; ++i) {
			buffer.putInt(bytes[i].length);
			buffer.put(bytes[i]);
		}
		buffer.flip();
		return buffer;
	}
	
	//get String from the whole Buffer bytes
	/*public static String getStringUTF8FromBuffer(ByteBuffer buffer) throws IOException {
		buffer.clear();
		if(buffer.hasArray()) {
			return new String(buffer.array(),"utf-8");
		}
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return new String(bytes,"utf-8");
	}
	public static String getStringFromBuffer(ByteBuffer buffer) throws IOException {
		buffer.clear();
		if(buffer.hasArray()) {
			return new String(buffer.array());
		}
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return new String(bytes);
	}*/
	//fill in fixed number of bytes into byteBuffer
	//fileChannel.read(byteBuffer) just try to read the number of byteBuffer.remaing() to the byteBuffer, it does not guarantee it.
	//RandomAccessFile.readFully(byte[]) has such function while FileChannel does not
	//see here:		https://stackoverflow.com/questions/22214477/does-filechannel-readbytebuffer-work-like-randomaccessfile-readfullybyte
	//however FileChannel.write(byteBuffer) will guarantee writing all the bytes to Bytebuffer, see Java doc WritableByteChannel.write()
	public static void readFullFromFileChannel(FileChannel fileChannel, ByteBuffer byteBuffer, int requiredSize) throws IOException{
		if(byteBuffer.remaining() < requiredSize) {
			throw new RuntimeException("The remaining size in ByteBuffer not equal to the requiredSize");
		}else if(byteBuffer.remaining() > requiredSize) {
			byteBuffer.limit(byteBuffer.position()+requiredSize);
		}
		int read = 0;
		while(read < requiredSize) {
			read += fileChannel.read(byteBuffer);
			if(read == requiredSize) {
				break;
			}else if(read == -1) {
				throw new RuntimeException("There is not enough bytes to be read in fileChannel when calling readFullFromFileChannel in Util class");
			}
		}
		//so that you can use this buffer to read more data again
		byteBuffer.limit(byteBuffer.capacity());
	}
	public static boolean checkEmailAddressFormat(String emailAddress) {
		if(emailAddress.length() == 0) {
			return false;
		}
		return emailFormatVerifier.verify(emailAddress);
	}
	public static boolean checkPasswordFormat(String passwd){
		int count = 0;
		if(passwd != null && passwd.length() >= 8){
			if(passwd.matches("[A-Z]")){
				count += 1;
			}
			if(passwd.matches("[a-z]")){
				count += 1;
			}
			if(count >= 1 && passwd.matches("[0-9]")){
				count += 1;
			}
			if(count >= 2 && passwd.matches("[^A-Za-z0-9\\s]")){
				count += 1;
			}
		}
		return  count >= 3;
	}
	//return -1 if no valid unsigned integer generated
	public static int parseUnsignedInteger(String str) {
		int res = -1;
		try {
			//this method will throw NumberFormatException, which is not very convenient
			res = Integer.parseInt(str);
		}catch(Exception e) {
			res = -1;
		}
		return (res >= 0 ? res : -1);
	}
	public static ByteBuffer genIntBuffer(int number) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(number);
		buffer.flip();
		return buffer;
	}
	public static byte[] imageToBytes(Image image) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
		ImageIO.write(bufferedImage, "png", baos);
		return baos.toByteArray();
	}
	//it is used only for format conversion, the method does not change the position and limit of the buffer
	public static byte[] getAllBytesFromBuffer(ByteBuffer buffer) {
		if(buffer.hasArray()) {
			return buffer.array();
		}
		int old_pos = buffer.position();
		int old_limit = buffer.limit();
		byte[] bytes = new byte[buffer.capacity()];
		buffer.clear();
		buffer.put(bytes);
		buffer.position(old_pos);
		buffer.limit(old_limit);
		return bytes;
	}
	//it is used only for format conversion, the method does not change the position and limit of the buffer
	/*public static byte[] getPartialBytesFromBuffer(ByteBuffer buffer) {
		int position = buffer.position();
		byte[] bytes = new byte[buffer.remaining()];
		buffer.put(bytes);
		buffer.position(position);
		return bytes;
	}*/
	/*public static void deleteDirectory(File dir){
		try {
			deleteDir(dir);
		}catch(IOException e) {
			//failed to delete the directory, try again when JVM exits
			deleteDirOnExit(dir);
		}
	}*/
	/*public static SecureRandom genSecureRandom() {
		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstanceStrong();
		}catch(NoSuchAlgorithmException e) {
			secureRandom = new SecureRandom();
		}
		return secureRandom;
	}
	public static ByteBuffer encodeBuffer(ByteBuffer byteBuffer, Cipher encodeCipher) {
		ByteBuffer outBuffer = ByteBuffer.allocate(encodeCipher.getOutputSize(byteBuffer.remaining()));
		try {
			encodeCipher.doFinal(byteBuffer,outBuffer);
			outBuffer.flip();
		}catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to encode buffer");
		}
		return outBuffer;
	}
	public static ByteBuffer decodeBuffer(ByteBuffer byteBuffer, Cipher decodeCipher) {
		ByteBuffer outBuffer = ByteBuffer.allocate(decodeCipher.getOutputSize(byteBuffer.remaining()));
		try {
			decodeCipher.doFinal(byteBuffer,outBuffer);
			outBuffer.flip();
		}catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to encode buffer");
		}
		return outBuffer;
	}*/
}
