package app;

import java.sql.Connection;

import database.DatabaseManager;
import model.NetServer;

public class App {
	public static void main(String[] args) {
		try {
			DatabaseManager.createDataManager();
			NetServer netServer = new NetServer(4646, 16);
			System.out.println("Test database connection...");
			try(Connection conn = DatabaseManager.getDataBaseManager().getConnection()){
				System.out.println(conn.isValid(10000));
				System.out.println("Test done");
			}
			netServer.start();
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("Failed to initialize the server");
		}
	}
}
