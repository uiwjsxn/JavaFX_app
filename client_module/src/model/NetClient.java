package model;
//one thread for receiving data and delegate the handling of received data to other threads
//and multiple threads for sending data

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngine;

import javafx.application.Platform;
import model.sslEngineUtil.SSLEngineGenerator;
import model.sslEngineUtil.SSLEngineHandler;
import presenter.Presenter;

public class NetClient {
	private final int LESS = 500;
	private final int REVBUFFERNUM = 12;
	
	private SecureRandom secureRandom;
	private String resourcePath;
	private SSLEngineHandler sslEngineHandler;
	private Presenter presenter;
	private String serverHostName;
	private int serverPort;
	private Thread receivingThread = null;
	private AtomicInteger fileIDGenerator = new AtomicInteger(0);
	private AtomicInteger bigBufferIDGenerator = new AtomicInteger(0);
	
	private ByteBuffer[] revBuffers = new ByteBuffer[REVBUFFERNUM];
	//random bytes for filling in the extra bytes in a ByteBuffer packet
	//private ByteBuffer[] supplementBuffers = new ByteBuffer[REVBUFFERNUM];
	private LinkedBlockingQueue<Integer> revBufferIndexes = new LinkedBlockingQueue<>(REVBUFFERNUM);
	//private LinkedBlockingQueue<Integer> supplementBufferIndexes = new LinkedBlockingQueue<>(REVBUFFERNUM);
	
	public NetClient() {
		for(int i = 0; i < REVBUFFERNUM;++i) {
			revBufferIndexes.offer(i);
		}
		resourcePath = System.getProperty("user.dir") + File.separator + "resources";
		try {
			secureRandom = SecureRandom.getInstanceStrong();
		}catch(NoSuchAlgorithmException e) {
			secureRandom = new SecureRandom();
		}
	}
	private static SocketChannel getSocketChannel() throws IOException{
		SocketChannel sChannel = SocketChannel.open();
		sChannel.configureBlocking(false);
		return sChannel;
	}
	//try connection for 3 times
	private SocketChannel tryConnect() throws IOException, InterruptedException{
		int leftCount = 3;
		SocketChannel sChannel = null;
		while(leftCount > 0){
			leftCount -= 1;
			//the connection before has failed, and the old sChannel is closed, which can not be used to build a new connection, so create a new channel;
			sChannel = getSocketChannel();
			try{
				//return immediately for non-blocking channel
				sChannel.connect(new InetSocketAddress(serverHostName, serverPort));
				System.out.println((3-leftCount) + ": trying to connect to the server at " + sChannel.getRemoteAddress());				
				while(sChannel.isConnectionPending()){
					//return immediately for non-blocking channel, If the connection has failed, the finishConnect() call will throw an IOException. 
					if(sChannel.finishConnect()){
						leftCount = -1;
					}
				}
			}catch(IOException e){
				e.printStackTrace();
				System.out.print("Failed to connect to the server.");
				if(leftCount > 0){
					System.out.print(" Try again...");
					Thread.sleep(3000);
				}
				System.out.println("\n");
			}
		}
		if(leftCount == 0){
			System.out.println("The program will exit");
			return null;
		}
		System.out.println("the connection is established");
		return sChannel;
	}
	
	//buildConnection will return a non-null SSLEngineHandler or throw an Exception
	private SSLEngineHandler buildConnection() throws IOException{
		if(receivingThread != null) {
			receivingThread.interrupt();
		}
		SSLEngineHandler handler = null;
		SocketChannel sChannel = null;
		try {
			sChannel = tryConnect();
			if(sChannel != null) {
				SSLEngineGenerator sslEngineGen = new SSLEngineGenerator(new File(resourcePath + File.separator + "data" + File.separator + "data1.dat"),"f839qhQ$F32rH65QORxdUC#()E&^APFOIofh&uaf89hnjfjkasd",new File(resourcePath + File.separator + "data" + File.separator + "data2.dat"),"#$(*URISVJ4RF#W^%&URdfFIAOLKdfedg42wedsjoia9898sdifjl3fdipfa");
				SSLEngine sslEngine = sslEngineGen.generateSSLEngine((InetSocketAddress)sChannel.getRemoteAddress());
				sslEngine.setUseClientMode(true);
				handler = new SSLEngineHandler(sChannel,sslEngine);
				if(!handler.doHandshake()){
					throw new IOException("handshake failure");
				}
				System.out.println("Handshake success with " + sChannel.getRemoteAddress());
			}
		}catch(Exception e) {
			//the SSLEngineGenerator may throw some other Exception other than IOExcetion
			e.printStackTrace();
			if(handler != null){
				handler.clientHandleException(e);
			}else if(sChannel != null){
				try{
					sChannel.close();
				}catch(IOException ee){
					ee.printStackTrace();
				}
			}
		}
		if(handler == null) {
			throw new IOException("Failed to build connection");
		}
		
		final SocketChannel socketChannel = sChannel;
		receivingThread = new Thread(()->{
			for(int i = 0;i < revBuffers.length;++i) {
				if(revBuffers[i] == null) {
					revBuffers[i] = ByteBuffer.allocate(sslEngineHandler.getAppBufferSize());
				}
			}
			try(Selector selector = Selector.open()){
				SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
				while(true) {
					if(Thread.currentThread().isInterrupted()) {
						break;
					}
					if(selector.select() > 0 && key.isReadable()) {
						Set<SelectionKey> selectedKeys = selector.selectedKeys();
						Iterator<SelectionKey> iterator = selectedKeys.iterator();
						while(iterator.hasNext()) {
							//only one key here
							iterator.next();
							iterator.remove();
							int index = revBufferIndexes.take();
							ByteBuffer revBuffer = revBuffers[index];
							boolean isRev = sslEngineHandler.receiveBuffer(revBuffer);
							//received application data, not handshake data which will cause the revBuffer.hasRemaining() return false
							if(isRev && revBuffer.hasRemaining()) {
								Platform.runLater(()->presenter.receiveData(revBuffer,index));
							}/*else {
								//A close_notify from server may have been received.
								if(!sslEngineHandler.getSocketChannel().isConnected()) {
									Platform.runLater(()->presenter.reconnect());
								}
								giveBackBuffer(index);
							}*/
						}
					}
				}
			}catch(IOException | InterruptedException e) {
				e.printStackTrace();
				try {
					sslEngineHandler.clientHandleException(e);
				}catch(IOException ee) {
					//reconnect: go back to NeedLoginState
					Platform.runLater(()->presenter.reconnect());
				}
			}
		});
		receivingThread.setDaemon(true);
		receivingThread.start();
		return handler;
	}
	//fill in the buffers ArrayList with byteBuffers from crtIndex, return the index of last handled byteBuffer
	//add payloadSizeBuffer to buffers
	private int fillBuffers(List<ByteBuffer> buffers, ByteBuffer payloadSizeBuffer, ByteBuffer[] byteBuffers, int crtIndex, int requiredSize) {
		payloadSizeBuffer.clear();
		buffers.add(payloadSizeBuffer);
		int leftSize = requiredSize;
		while(leftSize > 0 && crtIndex < byteBuffers.length) {
			int remaining = byteBuffers[crtIndex].remaining();
			if(remaining < leftSize) {
				buffers.add(byteBuffers[crtIndex++]);
				leftSize -= remaining;
			}else {
				break;
			}
		}
		payloadSizeBuffer.putInt(requiredSize-leftSize);
		payloadSizeBuffer.flip();
		return crtIndex;
	}
	/***********************************public methods***********************************/
	//for all the public methods, if IOException occurs when calling methods of SSLEngineHandler, you should just
	//close the connection here, and let Presenter decides whether to rebuild the connection
	//finished LongProperty is provided by Presenter, used to record the sent data
	//sendFile either success or if the connection is closed, an IOException will be thrown to indicate the netClient
	//to rebuilt a connection or just Platform.exit();
	public void setPresenter(Presenter presenter_) {
		presenter = presenter_;
	}
	//in Presenter.receiveData() method call this method to give back buffer
	public void giveBackBuffer(int index) {
		try {
			revBufferIndexes.put(index);
		}catch(InterruptedException e) {}
	}
	//connect is called by Presenter, when calling the method of NetClient and receiving a connection error, calling Presenter.reconnect() method to handle it
	//the initial value of integerProperty should be -1
	public boolean connect() {
		try {
			sslEngineHandler = buildConnection();
		}catch (IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public void exit() {
		if(receivingThread != null) {
			receivingThread.interrupt();
		}
		try {
			if(sslEngineHandler != null) {
				sslEngineHandler.closeConnection();
			}
		}catch(IOException e) {
			//this version do not throw new Exception
			sslEngineHandler.serverHandleException(e);
		}
	}
	public void setNetwork(String serverHostName_, int serverPort_){
		serverHostName = serverHostName_;
		serverPort = serverPort_;
	}
	//send message
	public void sendBuffer(ByteBuffer byteBuffer) throws IOException{
		sendBuffer(new ByteBuffer[] {byteBuffer});
	}
	//all the byteBuffers consists one packet
	public void sendBuffer(ByteBuffer[] byteBuffers) throws IOException{
		int appBufferSize = sslEngineHandler.getAppBufferSize()-LESS;
		int totalSize = 0;
		for(int i = 0;i < byteBuffers.length;++i) {
			totalSize += byteBuffers[i].remaining();
		}
		try {
			if(totalSize <= appBufferSize) {
				sslEngineHandler.genWrappedBuffer(byteBuffers);
			}else {
				//protocol_id 2, sending big buffer
				int dataSliceSize = sslEngineHandler.getAppBufferSize()-LESS;
				int bigBufferID = bigBufferIDGenerator.getAndIncrement();
				ByteBuffer header = ByteBuffer.allocate(8);
				header.putInt(2);
				header.putInt(bigBufferID);
				header.flip();
				ByteBuffer totalSizeBuffer = ByteBuffer.allocate(4);
				totalSizeBuffer.putInt(totalSize);
				ByteBuffer payloadSizeBuffer = ByteBuffer.allocate(4);
				//int sentBytes = 0;
				//the buffers to be sent at next loop
				List<ByteBuffer> buffers = new ArrayList<>();
				//last handled buffer, may still have remaining bytes
				int crtBufferIndex = 0;
				boolean isFirst = true;
				int neededBytes = dataSliceSize-12;
				
				while(crtBufferIndex != byteBuffers.length) {
					buffers.clear();
					buffers.add(header);
					header.clear();
					if(isFirst) {
						buffers.add(totalSizeBuffer);
						isFirst = false;
					}
					crtBufferIndex = fillBuffers(buffers,payloadSizeBuffer,byteBuffers,crtBufferIndex,neededBytes);
					sslEngineHandler.genWrappedBuffer((ByteBuffer[])buffers.toArray());
					if(crtBufferIndex == byteBuffers.length) {
						//send the last buffer
						buffers.clear();
						header.clear();
						buffers.add(header);
						payloadSizeBuffer.clear();
						payloadSizeBuffer.putInt(-1);
						payloadSizeBuffer.flip();
						buffers.add(payloadSizeBuffer);
						sslEngineHandler.genWrappedBuffer((ByteBuffer[])buffers.toArray());
					}
				}
			}
		}catch(IOException | InterruptedException e) {
			e.printStackTrace();
			sslEngineHandler.clientHandleException(e);
		}
	}
	/*
	//should be called in a separate thread, fileChannel should be closed by the caller
	public int sendFile(String fileName, FileChannel filechannel, LongProperty finished) throws IOException,IllegalArgumentException{
		int fileId = fileIDGenerator.getAndAdd(1);
		try {
			ByteBuffer fileNameBuffer = ByteBuffer.wrap(fileName.getBytes("utf-8"));
			//make the appBufferSize smaller than netBufferSize(approximately 16000) in SSLEngineHandler to avoid BufferOverFlow
			int appBufferSize = sslEngineHandler.getAppBufferSize()-LESS;
			int index = supplementBufferIndexes.take();
			ByteBuffer extraBuffer = supplementBuffers[index];
			if(fileNameBuffer.remaining() > appBufferSize-12) {
				//cut the fileName, it is too long
				fileNameBuffer.limit(appBufferSize-12);
				setSupplementBuffer(extraBuffer, 0);
			}else {
				setSupplementBuffer(extraBuffer, appBufferSize-12-fileNameBuffer.remaining());
			}
			//protocol_id 1 means sending file
			ByteBuffer headerBuffer = ByteBuffer.allocate(8);
			headerBuffer.putInt(1);
			headerBuffer.putInt(fileId);
			headerBuffer.flip();
			//valid bytes
			ByteBuffer dataSizeBuffer = ByteBuffer.allocate(4);
			dataSizeBuffer.putInt(fileNameBuffer.remaining());
			dataSizeBuffer.flip();
			
			int dataBufferSize = appBufferSize-headerBuffer.remaining()-dataSizeBuffer.remaining();
			ByteBuffer dataBuffer = ByteBuffer.allocate(dataBufferSize);
			//send fileName
			sslEngineHandler.genWrappedBuffer(new ByteBuffer[] {headerBuffer,dataSizeBuffer,fileNameBuffer,extraBuffer});
			supplementBufferIndexes.put(index);
			headerBuffer.clear();
			dataSizeBuffer.clear();
			//send file slices, does not use randomBytes to fill in extra empty bytes, just send a whole dataBuffer
			long[] sentBytes = {0L};
			int count = 0;
			int readBytes = 0;
			//here I do not assume that a calling to FileChannel.read(ByteBuffer byteBuffer) will read the bytes whose size is equals to the remaining size number of byteBuffer
			while((readBytes = filechannel.read(dataBuffer)) != -1) {
				if(readBytes == 0) {
					//read nothing from FileChannel, since the empty dataBuffer means last packet for server, we just skip it
					dataBuffer.clear();
					continue;
				}
				dataBuffer.flip();
				
				sentBytes[0] += dataBuffer.limit();
				dataSizeBuffer.putInt(dataBuffer.limit());
				dataSizeBuffer.flip();
				//send the whole dataBuffer to server, the valid data is specified by dataSizeBuffer
				dataBuffer.clear();
				sslEngineHandler.genWrappedBuffer(new ByteBuffer[] {headerBuffer,dataSizeBuffer,dataBuffer});
				dataBuffer.clear();
				headerBuffer.clear();
				dataSizeBuffer.clear();
				++count;
				if(count % 100 == 0) {
					Platform.runLater(()->finished.set(sentBytes[0]));
				}
			}
			//end of file
			dataSizeBuffer.putInt(-1);
			dataSizeBuffer.flip();
			//a special packet indicating the end of the file with dataSizeBuffer value -1
			sslEngineHandler.genWrappedBuffer(new ByteBuffer[] {headerBuffer,dataSizeBuffer,dataBuffer});
			System.out.printf("The last packet of file %d has been sent to SSLEngineHandler\n", fileId);
			Platform.runLater(()->finished.set(sentBytes[0]));
		}catch(IOException | InterruptedException e) {
			e.printStackTrace();
			sslEngineHandler.clientHandleException(e);
		}
		return fileId;
	}
	*/
}
