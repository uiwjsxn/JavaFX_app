package statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;

import database.DatabaseManager;
import model.ClientData;
import model.ServerProtocol;
import security_code.PHPMailerConnector;
import security_code.SecurityCode;
import security_code.SecurityCodeGen;
import util.Util;

class NeedEmailVerifyState extends State{
	private USAGE usage;
	private StateMachine stateMachine;
	private SecurityCodeGen codeGen;
	private SecurityCode currentCode = null;
	private PHPMailerConnector connector = new PHPMailerConnector();
	private String emailAddress;
	
	public NeedEmailVerifyState(StateMachine stateMachine, USAGE usage) {
		this.stateMachine = stateMachine;
		this.usage = usage;
		codeGen = new SecurityCodeGen();
	}
	public void setUsage(USAGE usage) {
		this.usage = usage;
	}
	@Override public void sendSecurityCode(int protocolId, ByteBuffer buffer, ClientData clientData) throws IOException, InterruptedException {
		emailAddress = Util.parseString(buffer);
		int result = 0;
		if(Util.checkEmailAddressFormat(emailAddress)) {
			boolean isSent = false;
			try(Connection conn = DatabaseManager.getDataBaseManager().getConnection()){
				int userId = DatabaseManager.getDataBaseManager().matchEmailAddress(conn, emailAddress);
				if(usage != USAGE.REGISTER) {
					if(userId != -1) {
						clientData.setUserID(userId);
						isSent = true;
					}
				}else {
					if(userId == -1) {
						isSent = true;
					}
				}
			}catch(SQLException e) {
				result = 2;
			}
			if(currentCode != null && currentCode.isInsideTimeInterval()){
				isSent = false;
			}
			if(isSent) {
				currentCode = codeGen.genSecurityCode();
				System.out.println("Requested code: " + currentCode.toString());
				//For debugging, do not really send email.
				if(connector.sendRandomCode(emailAddress, currentCode)){
				//if(true) {
					System.out.println("Security code was sent to: " + emailAddress);
					result = 1;
				}else {
					System.out.println("Failed to send security code to: " + emailAddress);
				}
			}else {
				System.out.println("Should not send security code to: " + emailAddress);
			}
		}
		ByteBuffer replyBuffer = ServerProtocol.genResultBuffer(protocolId, result);
		clientData.sslEngineHandler.genWrappedBuffer(replyBuffer);
	}
	@Override public void verifySecurityCode(int protocolId, ByteBuffer byteBuffer, ClientData clientData) throws IOException, InterruptedException {
		String codeStr = Util.parseString(byteBuffer);
		int result = 0;
		if(currentCode.checkSecurityCode(codeStr)) {
			result = 1;
			switch(usage) {
			case LOGIN:
				stateMachine.toNextState(StateMachine.STATE.LOGIN_DONE, null);
				break;
			case REGISTER:
				clientData.sslEngineHandler.getSSLEngine().getSession().putValue("emailAddress", emailAddress);
				stateMachine.toNextState(StateMachine.STATE.RESET_PASSWORD, USAGE.REGISTER);
				break;
			case RESET_PASSWORD:
				stateMachine.toNextState(StateMachine.STATE.RESET_PASSWORD, USAGE.RESET_PASSWORD);
				break;
			default:
				throw new IOException("Invalid USAGE value");
			}
		}
		ByteBuffer replyBuffer = ServerProtocol.genResultBuffer(protocolId, result);
		clientData.sslEngineHandler.genWrappedBuffer(replyBuffer);
	}
}
