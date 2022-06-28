package model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import model.sslEngineUtil.SSLEngineHandler;
import statemachine.StateMachine;

//a client data is for one client, it is operated in a single thread
public class ClientData{
	public StateMachine stateMachine;
	public SSLEngineHandler sslEngineHandler;
	//revBuffer is allocated when ClientData created
	public ByteBuffer revBuffer;
	//userID is set when login success
	public int userID;
	public Semaphore revSemaphore = new Semaphore(1);
	//for any index i, the fileID of fileIDs[i] indicates the file with the same fileID represented by openedFileChannels[i]
	private File directory;
	private Map<Integer, FileChannel> openedFileChannels = new HashMap<>();
	private Map<Integer, File> openedFiles = new HashMap<>();
	private Set<Integer> handledFiles = new HashSet<>();
	//whether transferring file packet directly to the client or not, depending on the isOnline.get(peerID) when server handles the first file packet
	//do not need it, just check whether the fileChannel is null or not 
	//private Map<Integer, Boolean> isTransfer = new HashMap<>();
	
	//if the server received a big buffer(protocol_id = 2), it will firstly call handleBigBuffer() method to allocate a bigRevBuffer
	//and collect all the buffer packet. After that, it will call handleBuffer(clientData, false) method again to handle this big buffer
	//bigBufferID(from client) + a big buffer
	private Map<Integer, ByteBuffer> bigBuffers = new HashMap<>();
	
	public ClientData(StateMachine stateMachine, SSLEngineHandler handler, ByteBuffer revBuffer) {
		this.stateMachine = stateMachine;
		this.revBuffer = revBuffer;
		this.sslEngineHandler = handler;
		try {
			directory = new File(getClass().getResource("/resources/receivedFiles/").toURI());
		}catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to open receivedFiles directory");
		}
	}
	public boolean isFileHandling(int fileID) {
		if(handledFiles.contains(fileID)) {
			return true;
		}
		handledFiles.add(fileID);
		return false;
	}
	//return null if the fileChannel is not opened, then you may call openFile(...) to open the FileChannel or just transfer the file packet to the peer client
	public FileChannel getFileChannel(int fileID){
		FileChannel fileChannel = openedFileChannels.get(fileID);
		return fileChannel;
	}
	@SuppressWarnings("resource")
	public FileChannel openFile(int fileID) throws IOException{
		//temporary file name: userID_fileID, to guarantee the unique file name
		File newFile = new File(directory, userID+"_"+String.valueOf(fileID));
		openedFiles.put(fileID, newFile);
		FileChannel fileChannel = new FileOutputStream(newFile).getChannel();
		openedFileChannels.put(fileID, fileChannel);
		return fileChannel;
	}
	public void closeFile(int fileID) throws IOException{
		handledFiles.remove(fileID);
		FileChannel fileChannel = openedFileChannels.get(fileID);
		//if it is null, it means the server just transfer the file packet to the client and no fileChannel is created for this file
		if(fileChannel != null) {
			openedFileChannels.remove(fileID);
			openedFiles.remove(fileID);
			//it may throw IOException, so put it at the last of closeFile(...) method
			fileChannel.close();
		}
	}
	//close filechannels and delete all the files failed to be handled
	public void clearData() {
		for(int index : openedFileChannels.keySet()) {
			FileChannel fileChannel = openedFileChannels.get(index);
			File file = openedFiles.get(index);
			try {
				fileChannel.close();
				if(!file.delete()) {
					throw new IOException("Failed to delete file: " + file.getAbsolutePath());
				}
			}catch(IOException e) {
				e.printStackTrace();
				System.out.println("Failed to close or delete file:" + file.getAbsolutePath());
			}
		}
		bigBuffers.clear();
	}
	//handle big buffer
	//return null if the BigBuffer does not exist
	public ByteBuffer getBigBuffer(int bigBufferID) {
		return bigBuffers.get(bigBufferID);
	}
	public ByteBuffer createBigBuffer(int bigBufferID, int size) {
		ByteBuffer bigBuffer = ByteBuffer.allocate(size);
		bigBuffers.put(bigBufferID, bigBuffer);
		return bigBuffer;
	}
	public void clearBigBuffer(int bigBufferID) {
		bigBuffers.remove(bigBufferID);
	}
	public void setUserID(int userID) {
		this.userID = userID;
	}
	public int getUserID() {
		return userID;
	}
}