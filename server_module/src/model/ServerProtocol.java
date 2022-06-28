package model;

import java.nio.ByteBuffer;

public class ServerProtocol {
	public static ByteBuffer genResultBuffer(int protocol_id, int res) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putInt(protocol_id);
		buffer.putInt(res);
		buffer.flip();
		return buffer;
	}
	public static ByteBuffer genFileTranferBuffer(ByteBuffer buffer) {
		//modify the protocol_id to 11
		buffer.position(0);
		buffer.putInt(11);
		buffer.position(0);
		return buffer;
	}
	public static ByteBuffer genFileReceivingResultBuffer(int res) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putInt(1);
		buffer.putInt(res);
		buffer.flip();
		return buffer;
	}
	public static ByteBuffer genEmailSendingResult(int res) {
		return genResultBuffer(4, res);
	}
}
