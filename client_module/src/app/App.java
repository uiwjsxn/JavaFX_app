package app;

import java.sql.Connection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.application.Application;
import javafx.stage.Stage;
import model.NetClient;
import presenter.LoginPresenter;
import util.DataManager;

public class App extends Application{
	public static void main(String[] args) {
		Application.launch(args);
	}
	private static ResourceBundle loadResourceBundle(int languageCode) {
		ResourceBundle resourceBundle = null;
		switch(languageCode) {
		case 1:
			resourceBundle = ResourceBundle.getBundle("languages.language", Locale.ENGLISH);
			break;
		case 2:
			resourceBundle = ResourceBundle.getBundle("languages.language", Locale.CHINESE);
			break;
		}
		return resourceBundle;
	}
	@Override public void start(Stage stage) throws Exception{
		ExecutorService threadPool = Executors.newFixedThreadPool(6,new ThreadFactory(){
			public Thread newThread(Runnable r){
				Thread thread = Executors.defaultThreadFactory().newThread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		//initialize the Coder and DataManager
		DataManager.createDataManager(threadPool);
		//Coder.getCoder();
		int language = DataManager.getDataManager().getLanguageSetting();
		System.out.println("language code: " + language);
		ResourceBundle resourceBundle = loadResourceBundle(language);
		NetClient netClient = new NetClient();
		
		
		System.out.println("Test database connection...");
		try(Connection conn = DataManager.getDataManager().getConnection()){
			System.out.println(conn.isValid(10000));
			System.out.println("Test done");
		}
		@SuppressWarnings("unused")
		LoginPresenter loginPresenter = new LoginPresenter(threadPool, stage, netClient, resourceBundle);
	}
}
