package model;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLEngine;

import model.sslEngineUtil.ConnectionClosedException;
import model.sslEngineUtil.SSLEngineGenerator;
import model.sslEngineUtil.SSLEngineHandler;
import statemachine.StateMachine;

public class NetServer{
	//10GB
	//private final long TOTAL_FILE_LENGTH_LIMIT = 10*1024*1024*1024;
	private InetSocketAddress address = null;
	//here just one client communicate with one server, so just use one appBuffer to receive data
	private ExecutorService threadPool = null;
	//all the data below should be thread-safe
	private ConcurrentHashMap<Integer, SSLEngineHandler> onlineUsers = new ConcurrentHashMap<>();
	
	public NetServer(int port, int threadNum) throws Exception{
		address = new InetSocketAddress(port);
		threadPool = Executors.newFixedThreadPool(threadNum,new ThreadFactory(){
			public Thread newThread(Runnable r){
				Thread thread = Executors.defaultThreadFactory().newThread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		//File configFile = new File(getClass().getResource("/resources/data/data3.dat").toURI());
	}
	//reset config file when exiting
	private void exitServer() {
	}
	//only when 
	private void receiveFile(ClientData clientData) throws IOException, InterruptedException{
		
	}
	private void handleBigBuffer(ClientData clientData) throws IOException, InterruptedException{
		//now the protocol_id has been read, start from data after that protocal_id
		ByteBuffer buffer = clientData.revBuffer;
		int bigBufferID = buffer.getInt();
		ByteBuffer bigBuffer = clientData.getBigBuffer(bigBufferID);
		//either dataSize for remaining data or bigBuffer size
		int dataSize = buffer.getInt();
		if(bigBuffer == null) {
			clientData.createBigBuffer(bigBufferID ,dataSize);
			dataSize = buffer.getInt();
		}
		if(dataSize == buffer.remaining()) {
			bigBuffer.put(buffer);
		}else {
			//the whole buffer has been received, now handle the bigBuffer
			if(dataSize != -1) {
				buffer.limit(buffer.position()+dataSize);
				bigBuffer.put(buffer);
			}else {
				bigBuffer.flip();
				//bigBuffer is just a wrapper to common buffer packets
				handleBuffer(clientData, bigBufferID, bigBuffer);
				clientData.clearBigBuffer(bigBufferID);
			}
		}
	}
	
	//here is the main logic of server program. When connection error occur, just throw an exception and the caller of this method will do clear up work.
	//handleBuffer may handle received buffer in clientData(when bigBufferID is -1) or bigBuffer
	private void handleBuffer(ClientData clientData, int bigBufferID, ByteBuffer bigBuffer) throws IOException, InterruptedException{
		ByteBuffer buffer = clientData.revBuffer;
		if(bigBufferID != -1) {
			//when bigBufferID is -1, bigBuffer is null, it means the clientData.revBuffer is just a common buffer, not a part of bigBuffer
			buffer = bigBuffer;
		}
		int protocol_id = buffer.getInt();
		//no result for current packet if the packet handled is not the last one for the whole file
		switch(protocol_id) {
		case 1:
			System.out.printf("Client: [%s] sent a file packet to the server\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			if(clientData.stateMachine.isLoginDone())
				receiveFile(clientData);
			else
				//throw an IOException to close the connection with the client
				throw new IOException(String.format("Unknown protocol, connection with the client:\n%s\nwill be closed", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress()));
			System.out.printf("Server has received a file packet from client: [%s] \n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			break;
		case 2:
			System.out.printf("Client: [%s] sent a large buffer packet to the server\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			if(clientData.stateMachine.isLoginDone())
				handleBigBuffer(clientData);
			else
				throw new IOException(String.format("Unknown protocol, connection with the client:\n%s\nwill be closed", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress()));
			System.out.printf("Server has received a large buffer packet from client: [%s] \n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			break;
		//all the parameters of the methods of State should be ByteBuffer instead of ClientData
		//and the methods will return some value to reflect the change of ClientData
		case 3:
			System.out.printf("Client: [%s] sent a login request to the server\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			clientData.stateMachine.login(protocol_id, buffer, clientData);
			//System.out.printf("Server has sent login result to client: [%s]\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			break;
		case 4:
		case 5:
		case 6:
			System.out.printf("Client: [%s] sent an email address to the server\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			clientData.stateMachine.sendSecurityCode(protocol_id, buffer, clientData);
			//System.out.printf("Server has sent security code to client email address: [%s]\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			break;
		case 7:
			System.out.printf("Client: [%s] sent a security code to the server\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			clientData.stateMachine.verifySecurityCode(protocol_id, buffer, clientData);
			//System.out.printf("Server has sent security code verification result to client email address: [%s]\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			break;
		case 8:
			System.out.printf("Client: [%s] sent a new password to the server\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			clientData.stateMachine.receiveNewPassword(protocol_id, buffer, clientData);
			//System.out.printf("Server has sent new password setting result to client email address: [%s]\n", clientData.sslEngineHandler.getSocketChannel().getRemoteAddress());
			break;
		case 9:
			
			break;
		default:
			throw new IOException("Fatal Error, unknown Protocol, the connection with the client will be closed");
		}
	}
	private void handleBuffer(ClientData clientData) throws IOException, InterruptedException{
		handleBuffer(clientData, -1, null);
	}
			
			
	public void start() {
		//System.out.println("Path2: " + System.getProperty("user.dir"));
		try(Selector selector = Selector.open(); ServerSocketChannel ssChannel = ServerSocketChannel.open()){
			SSLEngineGenerator sslEngineGenerator = new SSLEngineGenerator(new File(getClass().getResource("/resources/data/data1.dat").toURI()),"(#UFIJAAw3alka(AFW$UFJL#qrifa0jwi905gjf43j-0ok;l$(Ueji8904i",new File(getClass().getResource("/resources/data/data2.dat").toURI()),"jio4fjml$(UJFQ#IK$FRIS9045yv&@jIOESFK($O#{530-o[o;lk90guoipfa");
			ssChannel.bind(address);
			ssChannel.configureBlocking(false);
			ssChannel.register(selector,SelectionKey.OP_ACCEPT);
			System.out.println("The server has been started.");
			while(true){
				System.out.println("A new round");
				try{
					int connectionCount = selector.select();
					if(connectionCount > 0){
						final CountDownLatch countDownLatch = new CountDownLatch(connectionCount);
						Set<SelectionKey> keys = selector.selectedKeys();
						Iterator<SelectionKey> iterator = keys.iterator();
						while(iterator.hasNext()){
							SelectionKey key = iterator.next();
							iterator.remove();
							
							if(key.isValid() && key.isAcceptable()){
								threadPool.submit(()->{
									SSLEngineHandler handler = null;
									SocketChannel sChannel = null;
									try{
										sChannel = ssChannel.accept();
										if(sChannel != null){
											SSLEngine sslEngine = sslEngineGenerator.generateSSLEngine((InetSocketAddress)sChannel.getRemoteAddress());
											sslEngine.setUseClientMode(false);
											sslEngine.setNeedClientAuth(true);
											sChannel.configureBlocking(false);
											handler = new SSLEngineHandler(sChannel,sslEngine);
											if(handler.doHandshake()){
											//you can set an attachment when register a SocketChannel, and use SelectionKey.attachment() to retrieve the attachment.
												System.out.println("Handshake success with " + sChannel.getRemoteAddress());
												sChannel.register(selector,SelectionKey.OP_READ, handler);
												//client identity data, store them in SSLSession
												ByteBuffer revBuffer = ByteBuffer.allocate(handler.getAppBufferSize());
												StateMachine stateMachine = new StateMachine();
												handler.getSSLEngine().getSession().putValue("clientData", new ClientData(stateMachine, handler, revBuffer));
											}else{
												throw new IOException("Failed to perform handshake with " + sChannel.getRemoteAddress());
											}
										}
									}catch(Exception e){
										if(!(e instanceof ConnectionClosedException)){
											e.printStackTrace();
										}
										if(handler != null){
											//SSLEngineHandler.serverHandleException() will close the SocketChannel
											handler.serverHandleException(e);
										}else if(sChannel != null){
											try{
												sChannel.close();
											}catch(IOException ee){
												ee.printStackTrace();
											}
										}
									}finally {
										countDownLatch.countDown();
									}
								});
							}else if(key.isValid() && key.isReadable()){
								SSLEngineHandler sslEngineHandler = (SSLEngineHandler)key.attachment();
								ClientData clientData = (ClientData)sslEngineHandler.getSSLEngine().getSession().getValue("clientData");
								//check if other thread is doing receiving for current client, if so, do nothing
								if(clientData.revSemaphore.tryAcquire()) {
									threadPool.submit(()->{
										//the Exception occurred when communicating with client should not cause the server exiting.
										try{
											boolean isRev = false;
											try {
												isRev = sslEngineHandler.receiveBuffer(clientData.revBuffer);
											}finally {
												countDownLatch.countDown();
											}
											if(isRev && clientData.revBuffer.hasRemaining()) {
												handleBuffer(clientData);
											}
											clientData.revSemaphore.release();
										}catch(Exception e){
											if(clientData != null){
												clientData.clearData();
											}	
											key.cancel();
											sslEngineHandler.serverHandleException(e);
										}
									});
								}else {
									countDownLatch.countDown();
								}
							}else {
								countDownLatch.countDown();
							}
						}
						countDownLatch.await();
					}
				}catch(IOException e){
					System.out.println("Server Failed when running");
					e.printStackTrace();
					exitServer();
					throw e;
				}
			}
		}catch(Exception e){
			System.out.println("Failed to initialize the server");
			e.printStackTrace();
		}
	}
	public void debugPrint(String str) {
		System.out.print(str);
	}
}