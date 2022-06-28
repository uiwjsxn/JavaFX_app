package database;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import util.Util;

//all the public methods of DatabaseManager except createDatabaseManager() should be thread-safe
public class DatabaseManager{
	//do not use singleton, since every client need a Database object in server
	private static String dbURL = "jdbc:mysql://localhost:3306/serverDB?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";
	private static String username = "server";
	//all the client .class files hash value, prestored in server	
	private static DatabaseManager databaseManager = null;
	private static DataSource connectionPool = null;
	private static SecureRandom secureRandom = null;
	//see https://commons.apache.org/proper/commons-dbcp/api-1.2.2/org/apache/commons/dbcp/BasicDataSource.html#validationQuery for the properties
	private static void initConnectionPool() {
		PoolProperties p = new PoolProperties();
        p.setUrl(dbURL);
        p.setUsername(username);
        //p.setPassword("mcoajvFJ#@re%wfvst0i8u0pjFKJ#KAio8u4doj324u8fr9uFJAIO3oruaf8392r7f9q"+new String(getDatabaseUserPassword()));
        p.setPassword("mcoajvFJ#@re%wfvst0i8u0pjFKJ#KAio8u4doj324u8fr9uFJAIO3oruaf8392r7f9q");
        p.setDriverClassName("com.mysql.jdbc.Driver");
        p.setInitialSize(10);
        p.setMinIdle(5);
        p.setMaxIdle(50);
        p.setMaxWait(1000);
        p.setMaxActive(1000);
        p.setDefaultAutoCommit(false);
        //just a test, confirm the connection returned by connectionPool is still valid
        p.setValidationQuery("SELECT 1");
        p.setTestOnBorrow(true);
        p.setTestOnReturn(false);
       
        p.setTestWhileIdle(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setLogAbandoned(true);
        p.setRemoveAbandoned(true);
        p.setRemoveAbandonedTimeout(60);
        connectionPool = new DataSource();
        connectionPool.setPoolProperties(p);
	}
	//call createDataManager() first, then call getDataBaseManager() to avoid the situation where multiple threads call getDataManager() at the same time 
	//and only to find the DatabaseManager object has not been created, which may lead to multiple threads create and use the same DatabaseManager simultaneously
	//so I divide the creation of DatabaseManager and the using of DatabaseManager into two methods
	public static DatabaseManager getDataBaseManager() {
		if(databaseManager == null) {
			throw new RuntimeException("You must call createDataManager() first before calling getDataManager()");
		}
		return databaseManager;
	}
	public static void createDataManager() {
		if(databaseManager == null) {
			databaseManager = new DatabaseManager();
			initConnectionPool();
			secureRandom = Util.genSecureRandom();
		}
	}
	
	private void setBlob(Blob blob, byte[][] bytes) throws SQLException{
		try(BufferedOutputStream outputStream = new BufferedOutputStream(blob.setBinaryStream(1))){
			for(int i = 0; i < bytes.length;++i) {
				outputStream.write(bytes[i]);
			}
		}catch(SQLException | IOException e) {
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
	}
	private void setBlob(Blob blob, byte[] bytes) throws SQLException{
		try(BufferedOutputStream outputStream = new BufferedOutputStream(blob.setBinaryStream(1))){
			outputStream.write(bytes);
		}catch(SQLException | IOException e) {
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
	}
	
	private void handleSQLException(Connection conn, SQLException e, boolean isQuery) throws SQLException{
		e.printStackTrace();
		if(!isQuery) {
			rollback(conn);
		}
		throw e;
	}
	private void rollback(Connection conn){
		try{
			conn.rollback();
		}catch(SQLException e){
			System.out.println("rollback with SQLException: " + e.getMessage());
		}
	}
/****************************************************************public methods****************************************************************/
	public Connection getConnection() throws SQLException{
		Connection conn = connectionPool.getConnection();
		conn.setAutoCommit(false);
		return conn;
	}
	/*public void closeConnection(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}*/
/********************************************************bannedUsers table********************************************************/
	public boolean isUserBanned(Connection conn, int person_id) throws SQLException{
		boolean res = false;
		String sqlStr = "select person_id from bannedUsers where person_id=?";
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setInt(1,person_id);
			try(ResultSet resultSet = pStatement.executeQuery()){
				if(resultSet.next()){
					res = true;
				}
			}catch(SQLException e){throw e;}
		}catch(SQLException e){
			handleSQLException(conn,e,true);
		}
		return res;
	}
	public void banUser(Connection conn, int person_id) throws SQLException{
		String sqlStr = "insert into bannedUsers values(?)";
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setInt(1,person_id);
			int count = pStatement.executeUpdate();
			if(count != 1){
				throw new SQLException("Failed to banUser() in database");
			}
			conn.commit();
		}catch(SQLException e){
			handleSQLException(conn,e,false);
		}
	}
	public void unbanUser(Connection conn, int person_id) throws SQLException{
		String sqlStr = "delete from bannedUsers where person_id=?";
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setInt(1,person_id);
			int count = pStatement.executeUpdate();
			if(count != 1){
				throw new SQLException("Failed to banUser() in database");
			}
			conn.commit();
		}catch(SQLException e){
			handleSQLException(conn,e,false);
		}
	}
/********************************************************users table********************************************************/
	//blobName should be "hash", "secretKey", "publicKey", "user_icon" or "msgToServer", return null is there is no data
	public byte[] getBlobBytesFromUsersTable(Connection conn, int person_id, String blobName) throws SQLException{
		String sqlStr = String.format("select %s from users where person_id=%d", blobName, person_id);
		byte[] bytes = null;
		try(PreparedStatement statement = conn.prepareStatement(sqlStr)){
			try(ResultSet resultSet = statement.executeQuery()){
				if(resultSet.next()) {
					Blob blob = resultSet.getBlob(1);
					if(!resultSet.wasNull()) {
						bytes = blob.getBytes(1,(int)blob.length());
					}
					if(blob != null) blob.free();
				}
			}catch(SQLException e) {throw e;}
		}catch(SQLException e) {
			handleSQLException(conn, e, true);
		}
		return bytes;
	}
	public void setBlobToUsersTable(Connection conn, int person_id, String blobName, byte[] msgBytes) throws SQLException{
		setBlobToUsersTable(conn, person_id, blobName, (msgBytes == null ? null : new byte[][] {msgBytes}));
	}
	//you may set null to the blob by passing a null for msgBytes parameter
	public void setBlobToUsersTable(Connection conn, int person_id, String blobName, byte[][] msgBytes) throws SQLException{
		String sqlStr = String.format("update users set %s=? where person_id=%d", blobName, person_id);
		try(PreparedStatement statement = conn.prepareStatement(sqlStr)){
			Blob blob = conn.createBlob();
			if(msgBytes != null) {
				setBlob(blob, msgBytes);
				statement.setBlob(1, blob);
			}else {
				statement.setNull(1, Types.BLOB);
			}
			int res = statement.executeUpdate();
			if(res != 1) {
				throw new SQLException(String.format("Failed to set Blob to client_info table with blob name: %s.", blobName));
			}
			blob.free();
			conn.commit();
		}catch(SQLException e) {
			handleSQLException(conn, e, false);
		}
	}
	//create a user account with user_icon
	public void createUserAccount(Connection conn, String alias, String email, byte[] hash, byte[] userIcon) throws SQLException{
		String sqlStr = "insert into users(alias,email,hash,user_icon) values(?,?,?,?)";
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			Blob hashBlob = conn.createBlob();
			setBlob(hashBlob, hash);
			pStatement.setString(1,alias);
			pStatement.setString(2,email);
			pStatement.setBlob(3,hashBlob);
			if(userIcon == null) {
				pStatement.setNull(4, Types.BLOB);
			}else {
				Blob userIconBlob = conn.createBlob();
				setBlob(userIconBlob,userIcon);
				pStatement.setBlob(4, userIconBlob);
			}
			int count = pStatement.executeUpdate();
			if(count != 1){
				throw new SQLException("Failed to createUserAccount() in database");
			}
			conn.commit();
		}catch(SQLException e){
			handleSQLException(conn,e,false);
		}
	}
	//create a user account without user_icon
	public void createUserAccount(Connection conn, String alias, String email, byte[] hash) throws SQLException{
		createUserAccount(conn,alias,email,hash,null);
	}
	public boolean checkUserPassword(Connection conn, int personId, String password) throws SQLException{
		byte[] passwordHash = null;
		try {
			passwordHash = getBlobBytesFromUsersTable(conn, personId, "hash");
		}catch(SQLException e) {
			handleSQLException(conn, e, false);
		}
		return Util.comparePasswordWithHash(password.getBytes(), passwordHash);
	}

	//return personID, return -1 if the emailAddr has not been registered
	public int matchEmailAddress(Connection conn, String emailAddr) throws SQLException{
		int res = -1;
		String sqlStr = "select person_id from users where email=?";
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setString(1,emailAddr);
			try(ResultSet resultSet = pStatement.executeQuery()){
				if(resultSet.next()){
					res = resultSet.getInt(1);
				}
			}
		}catch(SQLException e) {
			handleSQLException(conn, e, false);
		}
		return res;
	}
	public void modifyUserPasswd(Connection conn, int person_id, byte[] hash) throws SQLException{
		String sqlStr = "update users set hash=? where person_id=?";
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			Blob blob = conn.createBlob();
			blob.setBytes(1L,hash);
			pStatement.setBlob(1,blob);
			pStatement.setInt(2,person_id);
			int count = pStatement.executeUpdate();
			if(count != 1){
				throw new SQLException("Failed to update users table in database");
			}
			blob.free();
			conn.commit();
		}catch(SQLException e){
			//System.out.println("SQLException occurs when modifyUserPasswd() in DataBaseUtil. Exception: " + e.getMessage());
			//not query, need rollback
			handleSQLException(conn, e, true);
		}
	}	
/**************************************** Message ****************************************/
	//store the message sent by user to database and update last_send time, created_time is the time in server, not in client
	//this method should be called when one client send a message to server and the contact is not online or the server failed to send the message to the contact
	public boolean storeMessage(Connection conn, int fromID, int record_id, int toID, byte[] msgbytes){
		boolean res = false;
		String sqlStr1 = "insert into records(sent_by,record_id,sent_to,message) values(?,?,?,?)";
		//String sqlStr2 = "update relations set last_send=? where person1=? and person2=?";
		try(PreparedStatement pStatement1 = conn.prepareStatement(sqlStr1)){
			Blob blob = conn.createBlob();
			blob.setBytes(1L,msgbytes);
			long created_time = System.currentTimeMillis();
			pStatement1.setInt(1,fromID);
			pStatement1.setInt(2,record_id);
			pStatement1.setInt(3,toID);
			pStatement1.setBlob(4,blob);
			int count = pStatement1.executeUpdate();
			blob.free();
			if(count != 1){
				throw new SQLException("1:Failed to insert message to database");
			}
			
			/*pStatement2.setLong(1,created_time);
			pStatement2.setInt(2,fromID);
			pStatement2.setInt(3,toID)；
			count = pStatement2.executeUpdate();
			if(count != 1){
				throw new SQLException("2:Failed to insert message to database");
			}*/
			conn.commit();
			res = true;
		}catch(SQLException e){
			System.out.println("SQLException occurs when receiveUserMessage() in DataBaseUtil. Exception: " + e.getMessage());
			//not query, need rollback
			rollback(conn);
		}
		return res;
	}
	//retreive all messege sent to person_id(to), this method is called when logining in
	//msgCount+(person_id1(from)+record_id+msg1BytesCount+msg1Bytes) * n
	/*public ByteBuffer queryMessage(Connection conn, int toID){
		String sqlStr = "select sent_by,record_id,message from records where sent_to=?";
		ByteBuffer resBuffer = null;
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setInt(1,toID);
			int bufferLen = 0;
			ArrayList<byte[]> messages = new ArrayList<>();
			ArrayList<Integer> recordIDs = new ArrayList<>();
			ArrayList<Integer> personIDs = new ArrayList<>();
			try(ResultSet resultSet = pStatement.executeQuery()){
				while(resultSet.next()){
					personIDs.add(resultSet.getInt(1));
					recordIDs.add(resultSet.getInt(2));
					Blob blob = resultSet.getBlob(3);
					int blobLength = (int)blob.length();
					messages.append(blob.getBytes(0,blob.length()));
					blob.free();
					bufferLen += (16+blobLength);
				}
			}catch(SQLException e){throw e;}
			
			if(bufferLen > 0){
				resBuffer = ByteBuffer.allocate(bufferLen+4);
				resBuffer.putInt(messages.size())
				for(int i = 0;i < messages.size();++i){
					resBuffer.putInt(personIDs[i]);
					resBuffer.putInt(recordIDs[i]);
					resBuffer.putInt(messages[i].length);
					resBuffer.put(messages[i]);
				}
				resBuffer.flip();
			}
		}catch(SQLException e){
			System.out.println("SQLException occurs when retrieveMessage() in DataBaseUtil. Exception: " + e.getMessage());
			resBuffer = null;
		}
		return resBuffer;
	}
		//get all the contacts of personID:
	//format: contactsCount(int) + (personID1+aliasCount+aliasBytes+publicKeyCount+ publicKeyBytes+user_iconCount+user_iconBytes+last_read_record_id(long)) * n
	public byte[] queryContacts(Connection conn, int personID){
		String sqlstr = "select person_id,alias,publicKey,user_icon,last_read_record_id"+
						"from (select person2,last_read_record_id from relations where person1=?) as t1 left join users on t1.person2=users.person_id";
		byte[] bytes = null;
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setInt(1,personID);
			ArrayList<Integer> contacts = new ArrayList<>();
			ArrayList<byte[]> aliasBytes = new ArrayList<>();
			ArrayList<byte[]> publicKeyBytes = new ArrayList<>();
			ArrayList<byte[]> userIconBytes = new ArrayList<>();
			ArrayList<Long> lastReadRecords = new ArrayList<>();
			int bufferLen = 0;	
			try(ResultSet resultSet = pStatement.executeQuery()){
				while(resultSet.next()){
					contacts.append(resultSet.getInt(1));
					byte[] alias_bytes = resultSet.getString(2).getBytes("utf-8");
					aliasBytes.append(alias_bytes);
					Blob keyBlob = resultSet.getBlob(3);
					Blob iconBlob = resultSet.getBlob(4);
					int keyLen = (int)keyBlob.length();
					int iconLen = (int)iconBlob.length();
					publicKeyBytes.append(keyBlob.getBytes(256,keyLen));
					userIconBytes.append(iconBlob.getBytes(0,iconLen));
					keyBlob.free();
					iconBlob.free();
					lastReadRecords.append(resultSet.getInt(5));
					bufferLen += (keyLen+iconLen+alias_bytes.length+24)
				}
			}catch(SQLException e){throw e;}
			
			if(bufferLen > 0){
				bytes = new byte[bufferLen+4] 
				ByteBuffer resBuffer = ByteBuffer.wrap(bytes);
				//first 4 bytes is the count of contacts
				resBuffer.putInt(contacts.size());
				for(int i = 0;i < contacts.size();++i){
					resBuffer.putInt(contacts[i]);
					resBuffer.putInt(aliasBytes[i].length);
					resBuffer.put(aliasBytes[i]);
					resBuffer.putInt(publicKeyBytes[i].length);
					resBuffer.put(publicKeyBytes[i]);
					resBuffer.putInt(userIconBytes[i].length);
					resBuffer.put(userIconBytes[i]);
					resBuffer.putInt(lastReadRecords[i]);
				}
				resBuffer.flip();
			}
		}catch(SQLException e){
			//return null if any SQLException occurs
			bytes = null;
			System.out.println("SQLException occurs when queryContacts() in DataBaseUtil. Exception: " + e.getMessage());
		}
		return bytes;
	}
	//the message has been read by sent_to, this method is called when "sent_to" has read the message and the message read info has been sent to the server
	public boolean setMessageRead(Connection conn, int sent_by, int sent_to, int last_read_record_id){
		String sqlStr1 = "delete from records where sent_to=? and sent_by=? and record_id<=?";
		String sqlStr2 = "update relations set last_read_record_id=? where person1=? and person2=?";
		boolean res = false;
		try(PreparedStatement pStatement1 = conn.prepareStatement(sqlStr1);PreparedStatement pStatement2 = conn.prepareStatement(sqlStr2)){
			//delete the old request which may already exist and insert the new request(with new info and alias)
			pStatement1.setInt(1,sent_to);
			pStatement1.setInt(2,sent_by);
			pStatement1.setInt(3,last_read_record_id);
			if(pStatement1.executeUpdate() == 0){
				throw new SQLException("1:Failed to setMessageRead() in DatabaseUtil");
			}
			
			pStatement2.setInt(1,last_read_record_id);
			pStatement2.setInt(2,sent_to);
			pStatement2.setInt(3,sent_by);
			if(pStatement2.executeUpdate() != 1){
				throw new SQLException("2:Failed to setMessageRead() in DatabaseUtil");
			}
			conn.commit();
			res = true;
		}catch(SQLException e){
			System.out.println("SQLException occurs when setMessageRead() in DataBaseUtil. Exception: " + e.getMessage());
			//not query, need rollback
			rollback(conn);
		}
		return res;
	}
	//query when login, format: requestCount(int) + (sent_by(int) + infoByteCount(int) + infoBytes)
	public ByteBuffer queryContactRequest(Connection conn, int toID){
		String sqlStr = "select sent_by, info from request_contact where sent_to=?";
		ByteBuffer resBuffer = null
		try(PreparedStatement pStatement = conn.prepareStatement(sqlStr)){
			pStatement.setInt(1,toID);
			ArrayList<byte[]> infos = new ArrayList<>();
			ArrayList<Integer> contacts = new ArrayList<>();
			int bufferLen = 0;
			try(ResultSet resultSet = pStatement.executeQuery()){
				while(resultSet.next()){
					contacts.append(resultSet.getInt(1));
					byte[] info = resultSet.getString(2).getBytes("utf-8");
					infos.append(info);
					bufferLen += (info.length+8);
				}
			}catch(SQLException e){throw e;}
			
			if(bufferLen > 0){
				resBuffer = ByteBuffer.allocate(bufferLen+4);
				resBuffer.putInt(contacts.size())
				for(int i = 0;i < contacts.size();++i){
					resBuffer.putInt(contacts);
					resBuffer.putInt(infos[i].length);
					resBuffer.put(infos[i])
				}
				resBuffer.flip();
			}
		}catch(SQLException e){
			System.out.println("SQLException occurs when queryContactRequest() in DataBaseUtil. Exception: " + e.getMessage());
			resBuffer = null;
		}
		return resBuffer;
	}
	
	public boolean insertContactRequest(Connection conn, int sent_by, int sent_to, String info, String alias){
		String sqlStr1 = "delete from request_contact where sent_to=? and sent_by=?";
		String sqlStr2 = "insert into request_contact values(?,?,?,?)";
		boolean res = false;
		try(PreparedStatement pStatement1 = conn.prepareStatement(sqlStr1);PreparedStatement pStatement2 = conn.prepareStatement(sqlStr2)){
			//delete the old request which may already exist and insert the new request(with new info and alias)
			pStatement1.setInt(1,sent_to);
			pStatement1.setInt(2,sent_by);
			pStatement1.executeUpdate();
			
			pStatement2.setInt(1,sent_by);
			pStatement2.setInt(2,sent_to);
			pStatement2.setString(3,alias);
			pStatement2.setString(4,info);
			int count = pStatement2.executeUpdate();
			if(count != 1){
				throw new SQLException("Failed to insertContactRequest() in DatabaseUtil");
			}
			conn.commit();
			res = true;
		}catch(SQLException e){
			System.out.println("SQLException occurs when insertContactRequest() in DataBaseUtil. Exception: " + e.getMessage());
			//not query, need rollback
			rollback(conn);
		}
		return res;
	}
	//alias1 is set by "sent_by" person, alias2 is set by "sent_to" person
	public boolean acceptContactRequest(Connection conn, int sent_by, int sent_to, String alias1, String alias2){
		String sqlStr1 = "delete from request_contact where sent_to=? and sent_by=?";
		String sqlStr2 = "insert into relations(person1,person2,person2_alias) values(?,?,?)";
		boolean res = false;
		try(PreparedStatement pStatement1 = conn.prepareStatement(sqlStr1);PreparedStatement pStatement2 = conn.prepareStatement(sqlStr2)){
			//delete the old request which may already exist and insert the new request(with new info and alias)
			pStatement1.setInt(1,sent_to);
			pStatement1.setInt(2,sent_by);
			int count = pStatement1.executeUpdate();
			if(count == 0){
				throw new SQLException(String.format("no contact request exists in request_contact table of database. This should never happen\nsent_by: %d, sent_to: %d", sent_by, sent_to));
			}

			pStatement2.setInt(1,sent_by);
			pStatement2.setInt(2,sent_to);
			pStatement2.setString(3,alias1);
			pStatement.addBatch();
			pStatement2.setInt(1,sent_to);
			pStatement2.setInt(2,sent_by);
			pStatement2.setString(3,alias2);
			pStatement.addBatch();
			int[] resCount = pStatement.executeBatch();
			if(resCount.length != 2 || resCount[0] != 1 || resCount[1] != 1){
				throw new SQLException("Failed to acceptContactRequest() in DatabaseUtil");
			}
			conn.commit();
			res = true;
		}catch(SQLException e){
			System.out.println("SQLException occurs when acceptContactRequest() in DataBaseUtil. Exception: " + e.getMessage());
			//not query, need rollback
			rollback(conn);
		}
		return res;
	}
	*/
}		