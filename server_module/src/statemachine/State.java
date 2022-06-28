package statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;

import model.ClientData;

abstract class State {
	enum USAGE{LOGIN,REGISTER,RESET_PASSWORD};
	//the information of the current user is recorded here and shared among all 
	
	//return userID or -1 for failed login, sslEngineHandler is used to send the reply to a client.
	public void login(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		throw new IOException("An Illegal operation to call login() for the current StateMachine");
	}
	public void sendSecurityCode(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		throw new IOException("An Illegal operation to call login() for the current StateMachine");
	}
	public void verifySecurityCode(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		throw new IOException("An Illegal operation to call login() for the current StateMachine");
	}
	public void receiveNewPassword(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		throw new IOException("An Illegal operation to call login() for the current StateMachine");
	}
}
