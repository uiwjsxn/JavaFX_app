package statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;

import database.DatabaseManager;
import model.ClientData;
import model.ServerProtocol;
import util.Util;

class ResetPasswordState extends State{
	private USAGE usage;
	private StateMachine stateMachine;
	
	public ResetPasswordState(StateMachine stateMachine, USAGE usage) {
		this.stateMachine = stateMachine;
		this.usage = usage;
	}
	public void setUsage(USAGE usage) {
		this.usage = usage;
	}
	public void receiveNewPassword(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException {
		byte[] newPasswordBytes = Util.parseBytes(byteBuffer);
		byte[] passwdHash = Util.hashPassword(newPasswordBytes);
		byte[] imageBytes = null;
		if(byteBuffer.hasRemaining()) {
			imageBytes = Util.parseBytes(byteBuffer);
		}
		int result = 1;
		String emailAddress;
		try(Connection conn = DatabaseManager.getDataBaseManager().getConnection()){
			switch(usage) {
			case RESET_PASSWORD:
				DatabaseManager.getDataBaseManager().modifyUserPasswd(conn, clientData.getUserID(), passwdHash);
				System.out.println("The passowrd has been set to: " + new String(newPasswordBytes));
				stateMachine.toNextState(StateMachine.STATE.NEED_LOGIN, null);
				break;
			case REGISTER:
				emailAddress = (String)clientData.sslEngineHandler.getSSLEngine().getSession().getValue("emailAddress");
				DatabaseManager.getDataBaseManager().createUserAccount(conn, emailAddress, emailAddress, passwdHash, imageBytes);
				System.out.println("The passowrd has been set to: " + new String(newPasswordBytes));
				stateMachine.toNextState(StateMachine.STATE.NEED_LOGIN, null);
				break;
			default:
				throw new IOException("Invalid USAGE value");
			}
		}catch(SQLException e) {
			result = 2;
		}
		ByteBuffer replyBuffer = ServerProtocol.genResultBuffer(protocolId, result);
		clientData.sslEngineHandler.genWrappedBuffer(replyBuffer);
	}
}