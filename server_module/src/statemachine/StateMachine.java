package statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;

import model.ClientData;
import statemachine.State.USAGE;

public class StateMachine{
	public enum STATE{NEED_LOGIN, NEED_EMAIL_VERIFY, RESET_PASSWORD, LOGIN_DONE}
	
	private State currentState;
	LoginDoneState loginDoneState;
	NeedEmailVerifyState needEmailVerifyState;
	NeedLoginState needLoginState;
	ResetPasswordState resetPasswordState;
	
	public StateMachine(){
		needLoginState = new NeedLoginState(this);
		currentState = needLoginState;
	}
	
	void toNextState(STATE nextState, USAGE usage) {
		switch(nextState) {
		case NEED_LOGIN:
			currentState = needLoginState;
			break;
		case NEED_EMAIL_VERIFY:
			if(needEmailVerifyState == null) {
				needEmailVerifyState = new NeedEmailVerifyState(this, usage);
			}else {
				needEmailVerifyState.setUsage(usage);
			}
			currentState = needEmailVerifyState;
			break;
		case RESET_PASSWORD:
			if(resetPasswordState == null) {
				resetPasswordState = new ResetPasswordState(this, usage);
			}else {
				resetPasswordState.setUsage(usage);
			}
			currentState = resetPasswordState;
			break;
		case LOGIN_DONE:
			if(loginDoneState == null) {
				loginDoneState = new LoginDoneState(this);
			}
			currentState = loginDoneState;
		}
	}
	
	public boolean isLoginDone() {
		if(currentState == loginDoneState) {
			return true;
		}
		return false;
	}
	//return -1 if login failed or return userID
	public void login(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		currentState.login(protocolId, byteBuffer, clientData);
	}
	public void sendSecurityCode(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		currentState.sendSecurityCode(protocolId, byteBuffer, clientData);
	}
	public void verifySecurityCode(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		currentState.verifySecurityCode(protocolId, byteBuffer, clientData);
	}
	public void receiveNewPassword(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException{
		currentState.receiveNewPassword(protocolId, byteBuffer, clientData);
	}
}