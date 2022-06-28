package model;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;

import util.DataManager;

//all the method here is static, this class is used to generate ByteBuffer which should be sent by NetClient.sendBuffer()
public class ClientProtocol {
	private static ClientProtocol clientProtocol;
	private ClientProtocol() {}
	public static ClientProtocol getClientProtocol() {
		if(clientProtocol == null) {
			clientProtocol = new ClientProtocol();
		}
		return clientProtocol;
	}
	
	private ByteBuffer genBufferFromOneString(int protocol_id, String info) {
		byte[] codeBytes = info.getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(8+codeBytes.length);
		buffer.putInt(protocol_id);
		buffer.putInt(codeBytes.length);
		buffer.put(codeBytes);
		buffer.flip();
		return buffer;
	}
	private ByteBuffer genBufferFromStrings(int protocol_id, String[] strings) {
		int size = strings.length;
		int byteCount = 4;
		byte[][] bytes = new byte[size][];
		for(int i = 0; i < size; ++i) {
			bytes[i] = strings[i].getBytes();
			byteCount += (4+bytes[i].length);
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(byteCount);
		buffer.putInt(protocol_id);
		for(int i = 0; i < size; ++i) {
			buffer.putInt(bytes[i].length);
			buffer.put(bytes[i]);
		}
		buffer.flip();
		return buffer;
	}
	//methods for packing buffer
	//sending loginBuffer + msgToServer in LoginPresenter
	public ByteBuffer genLoginBuffer(String userID, String password, boolean saveUsername, boolean savePassword) {
		int protocol_id = 3;
		ByteBuffer byteBuffer = genBufferFromStrings(protocol_id, new String[] {userID,password});
		//Form a userInfo Buffer and store it to the database
		byte[][] storedBytes = null;
		if(saveUsername) {
			if(savePassword) {
				storedBytes = new byte[][] {userID.getBytes(), password.getBytes()};
			}else {
				storedBytes = new byte[][] {userID.getBytes()};
			}
		}
		try(Connection conn = DataManager.getDataManager().getConnection()){
			DataManager.getDataManager().setBlobTo_client_info(conn, "userInfo", storedBytes);
		}catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to store user info to database.");
		}
		return byteBuffer;
	}
	public ByteBuffer genEmailAddrBufferForLogin(String emailAddr) {
		return genBufferFromOneString(4, emailAddr);
	}
	public ByteBuffer genEmailAddrBufferForRegister(String emailAddr) {
		return genBufferFromOneString(5, emailAddr);
	}
	public ByteBuffer genEmailAddrBufferForPasswordReset(String emailAddr) {
		return genBufferFromOneString(6, emailAddr);
	}
	public ByteBuffer genSecurityCodeBuffer(String code) {
		return genBufferFromOneString(7, code);
	}
	public ByteBuffer genNewPasswordBuffer(String password, byte[] imageBytes) {
		if(imageBytes == null) {
			return genBufferFromOneString(8,password);
		}
		byte[] strBytes = password.getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(12 + strBytes.length + imageBytes.length);
		buffer.putInt(8);
		buffer.putInt(strBytes.length);
		buffer.put(strBytes);
		buffer.putInt(imageBytes.length);
		buffer.put(imageBytes);
		buffer.flip();
		return buffer;
	}
	
	
	//methods for unpacking buffer
	
	
	
}
