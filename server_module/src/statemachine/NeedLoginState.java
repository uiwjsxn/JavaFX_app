package statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;

import database.DatabaseManager;
import model.ClientData;
import model.ServerProtocol;
import statemachine.StateMachine.STATE;
import util.Util;

class NeedLoginState extends State {
	private StateMachine stateMachine;
	
	public NeedLoginState(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	//return 0 for login failure, 1 for login success and 2 for server internal error
	@Override public void login(int protocolId, ByteBuffer buffer, ClientData clientData) throws IOException, InterruptedException{
		int result = 0;
		//you may login by userID or EmailAddress, so the userInfo here will be one of the both
		String userInfo = Util.parseString(buffer);
		int userId = -1;
		System.out.println("Received userInfo: " + userInfo);
		try {
			userId = Integer.valueOf(userInfo);
		}catch(NumberFormatException e) {}
		String password = Util.parseString(buffer);
		System.out.println("Received password: " + password);
		//now the buffer becomes msgToServer buffer
		DatabaseManager databaseManager = DatabaseManager.getDataBaseManager();
		try(Connection conn = databaseManager.getConnection()) {
			if(userId == -1) {
				if(Util.checkEmailAddressFormat(userInfo)) {
					userId = databaseManager.matchEmailAddress(conn, userInfo);
				}
			}
			if(userId != -1 && databaseManager.checkUserPassword(conn, userId, password)) {
				result = 1;
				clientData.setUserID(userId);
			}
		}catch(SQLException e) {
			e.printStackTrace();
			result = 2;
		}
		//set back reply and change state machine
		if(result == 1) {
			stateMachine.toNextState(StateMachine.STATE.LOGIN_DONE, null);
		}
		ByteBuffer replyBuffer = ServerProtocol.genResultBuffer(protocolId, result);
		clientData.sslEngineHandler.genWrappedBuffer(replyBuffer);
	}
	@Override public void sendSecurityCode(int protocolId, ByteBuffer buffer, ClientData clientData) throws IOException, InterruptedException{
		switch(protocolId) {
		case 4:
			stateMachine.toNextState(STATE.NEED_EMAIL_VERIFY, USAGE.LOGIN);
			break;
		case 5:
			stateMachine.toNextState(STATE.NEED_EMAIL_VERIFY, USAGE.REGISTER);
			break;
		case 6:
			stateMachine.toNextState(STATE.NEED_EMAIL_VERIFY, USAGE.RESET_PASSWORD);
			break;
		default:
			throw new IOException("Illegal protocol_id");
		}
		stateMachine.sendSecurityCode(protocolId, buffer, clientData);
	}
}
