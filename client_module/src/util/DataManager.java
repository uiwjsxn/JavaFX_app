package util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
//manage the data in local Derby database
public class DataManager {	
	private static ExecutorService threadPool;
	//private static SecureRandom secureRandom;
	private File passwdSecretKeyFile;
	private File dataFile;
	private File passwordFile;
	//plain text
	//private Cipher encodeCipher;
	//private Cipher decodeCipher;
	private byte[][] encryptedPasswords = new byte[5][];
	private ByteBuffer msgToServerBuffer;
	private int language;
	private String dbPath;
	
	private static DataManager dataManager = null;
	private DataSource connectionPool = null;
	//see https://commons.apache.org/proper/commons-dbcp/api-1.2.2/org/apache/commons/dbcp/BasicDataSource.html#validationQuery for the properties
	private void initConnectionPool() {
		PoolProperties p = new PoolProperties();
        p.setUrl("jdbc:derby:"+dbPath+";user=client;password=893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$");
        p.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        p.setInitialSize(1);
        p.setMinIdle(0);
        p.setMaxIdle(0);
        p.setMaxWait(1);
        p.setMaxActive(1);
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
	/*private void createDataFile(File file) {
		try(FileOutputStream fileOutputStream =  new FileOutputStream(file)){
			try(FileChannel fileChannel = fileOutputStream.getChannel()){
				ByteBuffer buffer = ByteBuffer.allocate(4);
				//in English by default
				language = 1;
				buffer.putInt(language);
				buffer.flip();
				fileChannel.write(buffer);
				fileChannel.truncate(fileChannel.position());
			}catch(IOException e) {throw e;}
		}catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to create DataFile");
		}
	}
	private void loadUserData(File file) {
		try(RandomAccessFile input = new RandomAccessFile(file, "r")){
			language = input.readInt();
		}catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load user data");
		}
	}
	//delete the Derby database because it is not completely created and something went wrong in its creation.
	//throw a RuntimeException to indicate the failure of the creation of the database.
	/*private void deleteDerby() {
		try {
			//shutdown Derby database first, then delete the database. or the deletion will fail
			shutDownDerby();
		}catch(SQLException e) {
			e.printStackTrace();
		}finally {
			Util.deleteDirectory(new File(dbPath));
			//delete the directory in C:\Users\\user\\AppData\\Local\\
			Util.deleteDirectory(passwdSecretKeyFile.getParentFile());
		}
		throw new RuntimeException("Failed to build a new Derby database");
	}
	private void buildDatabase() {
		boolean deleteDir = false;
		//build the database
		try(Connection conn = DriverManager.getConnection("jdbc:derby:"+dbPath+";create=true;dataEncryption=true;encryptionAlgorithm=Blowfish/CBC/NoPadding;bootPassword=jfioaJKLFaijq3oAJFIL839ojjqf398ujalkfJFAjld83207Q#@RJFIa8p3fai"  + ";user=client")){
			CallableStatement statement = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_CREATE_USER(?, ?)");
			statement.setString(1, "client");
			statement.setString(2, "893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$");
			statement.executeUpdate();
			
			statement = conn.prepareCall("{CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(?,?)}");
			statement.setString(1, "derby.connection.requireAuthentication");
			statement.setBoolean(2, true);
			statement.executeUpdate();
			
			statement.setString(1, "derby.database.propertiesOnly");
			statement.executeUpdate();
		}catch(SQLException e) {
			e.printStackTrace();
			//remove the database failed to built
			deleteDir = true;
		}finally {
			if(deleteDir) deleteDerby();
		}
		//shutdown the database and restart it with bootPassword, it is just for testing the validation of the newly created database
		try{
			shutDownDerby();
		}catch(SQLException e) {
			deleteDir = true;
		}finally {
			if(deleteDir) deleteDerby();
		}
		System.out.println("Derby database created");
		//first, connect the derby database with bootPassword, then the later connection will not need it
		String initialConnURL = "jdbc:derby:"+dbPath + ";bootPassword=jfioaJKLFaijq3oAJFIL839ojjqf398ujalkfJFAjld83207Q#@RJFIa8p3fai" + ";user=client;password=893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$";
		try(Connection conn = DriverManager.getConnection(initialConnURL)){
			initConnectionPool();
		}catch(SQLException e) {
			e.printStackTrace();
			deleteDir = true;
		}finally {
			if(deleteDir) deleteDerby();
		}
		Connection connection = null;
		//using connection pool to get the connection to the database, this time should succeed
		try(Connection conn = connectionPool.getConnection()){
			connection = conn;
			System.out.println("Connection built from the connection pool");
			ArrayList<String> sqlstrs = new ArrayList<>();
			//create tables
			sqlstrs.add("CREATE TABLE client_info("
					+ "	id int primary key,"
					+ "	passwordKeyStore blob,"
					+ "	userID int,"
					+ "	userInfo blob,"
					+ "	keyPair blob,"
					+ "	profileIcon blob)");
			sqlstrs.add("INSERT INTO client_info(id) values(1)");
			sqlstrs.add("CREATE TABLE contact_info("
					+ "	id int primary key,"
					+ "	pubKey blob NOT NULL,"
					+ "	alias varchar(100) NOT NULL,"
					+ "	aliasByUser varchar(100) NOT NULL,"
					+ "	profileIcon blob,"
					+ "	last_rev_msg_id int default 0 NOT NULL)");
			sqlstrs.add("CREATE TABLE message_from("
					+ "	id bigint primary key GENERATED ALWAYS AS IDENTITY(Start with 1, Increment by 1),"
					+ "	peerID int NOT NULL,"
					+ "	message_id int NOT NULL,"
					+ "	message blob NOT NULL,"
					+ "	isFile boolean NOT NULL,"
					+ "	CONSTRAINT mffk foreign key(peerID) references contact_info(id) on delete cascade on update restrict)");
			sqlstrs.add("CREATE INDEX mfi ON message_from (peerID, message_id)");
			sqlstrs.add("CREATE TABLE message_to("
					+ "	id bigint primary key GENERATED ALWAYS AS IDENTITY(Start with 1, Increment by 1),"
					+ "	peerID int NOT NULL,"
					+ "	message_id int NOT NULL,"
					+ "	message blob NOT NULL,"
					+ "	isFile boolean NOT NULL,"
					+ "	CONSTRAINT mtfk foreign key(peerID) references contact_info(id) on delete cascade on update restrict)");
			sqlstrs.add("CREATE INDEX mti ON message_to (peerID, message_id)");
			sqlstrs.add("CREATE TABLE contract_adding_request("
					+ "	sentTo int primary key NOT NULL,"
					+ "	peerAlias varchar(100) NOT NULL,"
					+ "	aliasByUser varchar(100) NOT NULL)");
			Statement statement = conn.createStatement();
			for(int i = 0;i < sqlstrs.size();++i) {
				statement.executeUpdate(sqlstrs.get(i));
			}
			conn.commit();
			System.out.println("All the tables has been created");
		}catch(SQLException e) {
			e.printStackTrace();
			deleteDir = true;
			if(connection != null) {
				try {
					connection.rollback();
				}catch(SQLException ee) {ee.printStackTrace();}
			}
		}finally {
			if(deleteDir) deleteDerby();
		}
	}*/
	private void loadDatabase() {
		String initialConnURL = "jdbc:derby:"+dbPath + ";bootPassword=jfioaJKLFaijq3oAJFIL839ojjqf398ujalkfJFAjld83207Q#@RJFIa8p3fai" + ";user=client;password=893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$";
		try (Connection initConnection = DriverManager.getConnection(initialConnURL)){
			//the URL in initConnectionPool() does not contain "bootPassword", which is special for Derby database embedded mode, so build a Connection with bootPassword first, then call initConnectionPool()
			initConnectionPool();
		}catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to initialize the DataManager");
		}
	}
	private DataManager(ExecutorService threadPool_){
		dbPath = System.getProperty("user.dir") + File.separator + "resources" + File.separator + "database" + File.separator + "clientDB";
		threadPool = threadPool_;
	}
	public static DataManager getDataManager() {
		if(dataManager == null) {
			throw new RuntimeException("You must call createDataManager() first before calling getDataManager()");
		}
		return dataManager;
	}
	public static void createDataManager(ExecutorService threadPool) {
		if(dataManager == null) {
			//secureRandom = Util.genSecureRandom();
			dataManager = new DataManager(threadPool);
			dataManager.loadDatabase();
		}
	}
	
	private void setBlob(Blob blob, byte[][] bytes) throws SQLException{
		try(BufferedOutputStream outputStream = new BufferedOutputStream(blob.setBinaryStream(1))){
			for(int i = 0; i < bytes.length;++i) {
				if(bytes[i] != null) {
					outputStream.write(bytes[i]);
				}
			}
		}catch(SQLException | IOException e) {
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
	}
	private void setBlob(Blob blob, byte[] bytes) throws SQLException{
		setBlob(blob, new byte[][] {bytes});
	}
	private void handleSQLException(Connection conn, SQLException e, boolean isQuery) throws SQLException{
		e.printStackTrace();
		if(!isQuery) {
			conn.rollback();
		}
		throw e;
	}
/*************************************************public methods*************************************************/
	public Connection getConnection() throws SQLException{
		Connection conn = connectionPool.getConnection();
		conn.setAutoCommit(false);
		return conn;
	}
	public void shutDownDerby() throws SQLException{
		String dbShutdownURL = "jdbc:derby:"+dbPath+";user=client;password=893fujcnh38r9ufjlse78q349puQ?FJ89rfvrlesdf849382332uofu#@$" + ";shutdown=true";
		if(connectionPool != null) connectionPool.close();
		try {
			DriverManager.getConnection(dbShutdownURL);
		}catch(SQLException e) {
			if(!e.getSQLState().equals("08006") && !e.getSQLState().equals("XJ015")) {	
				e.printStackTrace();
				throw e;
			}
		}
	}
/*************************************************client_info table*************************************************/	
	//blobName should be "passwordKeyStore", "keyPair", "profileIcon" or "userInfo", return null is there is no data
	public byte[] getBlobBytesFrom_client_info(Connection conn, String blobName) throws SQLException{
		String sqlStr = String.format("select %s from client_info where id=1",blobName);
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
	public void setBlobTo_client_info(Connection conn, String blobName, byte[] msgBytes) throws SQLException{
		setBlobTo_client_info(conn, blobName, (msgBytes == null ? null : new byte[][] {msgBytes}));
	}
	//you may set null to the blob by passing a null for msgBytes parameter
	public void setBlobTo_client_info(Connection conn, String blobName, byte[][] msgBytes) throws SQLException{
		byte[] dataBytes = null;
		if(msgBytes != null) {
			ByteBuffer buffer = Util.bytesToBuffer(msgBytes);
			dataBytes = new byte[buffer.remaining()];
			buffer.get(dataBytes);
		}
		String sqlStr = String.format("update client_info set %s=? where id=1", blobName);
		try(PreparedStatement statement = conn.prepareStatement(sqlStr)){
			Blob blob = conn.createBlob();
			if(dataBytes != null) {
				setBlob(blob, dataBytes);
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
	public void setUserID(Connection conn, int userID) throws SQLException{
		String sqlStr = "update client_info set userID=? where id=1";
		try(PreparedStatement statement = conn.prepareStatement(sqlStr)){
			statement.setInt(1, userID);
			int res = statement.executeUpdate();
			if(res != 1) {
				throw new SQLException("Failed to set userID to client_info table");
			}
			conn.commit();
		}catch(SQLException e) {
			handleSQLException(conn, e, false);
		}
	}
	public int getUserID(Connection conn) throws SQLException{
		String sqlStr = "select userID from client_info where id=1";
		int res = -1;
		try(PreparedStatement statement = conn.prepareStatement(sqlStr)){
			try(ResultSet resultSet = statement.executeQuery()){
				if(resultSet.next()) {
					res = resultSet.getInt(1);
				}
			}catch(SQLException e) {throw e;}
		}catch(SQLException e) {
			handleSQLException(conn, e, true);
		}
		return res;
	}
/*************************************************contact_info table*************************************************/	




/*************************************************message_from table*************************************************/	




/*************************************************message_to table*************************************************/	


	
	
/*************************************************contract_adding_request table*************************************************/
	public void addContactRequest(Connection conn, int send_to, String peerAlias, String aliasByUser) throws SQLException{
		String sql1 = "insert into contact_adding_request values(?,?,?)";
		String sql2 = "update contact_adding_request set peerAlias=?, aliasByUser=? where sentTo=?";
		try(PreparedStatement statement = conn.prepareStatement(sql1)){
			statement.setInt(1, send_to);
			statement.setString(2, peerAlias);
			statement.setString(3, aliasByUser);
			int res = statement.executeUpdate();
			if(res != 1) {
				throw new SQLException("Failed to addContactRequest()");
			}
			conn.commit();
		}catch(SQLException e) {
			try(PreparedStatement statement2 = conn.prepareStatement(sql2)){
				conn.rollback();
				statement2.setString(1, peerAlias);
				statement2.setString(2, aliasByUser);
				statement2.setInt(3, send_to);
				int res = statement2.executeUpdate();
				if(res != 1) {
					throw new SQLException("Failed to addContactRequest()");
				}
				conn.commit();
			}catch(SQLException ee) {
				handleSQLException(conn, ee, false);
			}
		}
	}
	public void deleteContactRequest(Connection conn, int send_to) throws SQLException{
		String sql1 = "delete from contact_adding_request where sentTo=?";
		try(PreparedStatement statement = conn.prepareStatement(sql1)){
			statement.setInt(1, send_to);
			int res = statement.executeUpdate();
			if(res != 1) {
				throw new SQLException("Failed to deleteContactRequest()");
			}
			conn.commit();
		}catch(SQLException e) {
			handleSQLException(conn, e, false);
		}
	}
	public int getLanguageSetting(){
		return 1;	//For now, English only
	}
}