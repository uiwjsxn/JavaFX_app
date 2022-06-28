package model;
//DataManager read config file for server
public class ServerConfigManager {
	private static ServerConfigManager dataManager;
	
	private ServerConfigManager() {
		
	}
	
	public static ServerConfigManager getDataManager() {
		if(dataManager == null) {
			dataManager = new ServerConfigManager(); 
		}
		return dataManager;
	}
	
	public long getFileIDCount() {
		return 0;
	}
	public void storeFileIDCount() {
		
	}
}