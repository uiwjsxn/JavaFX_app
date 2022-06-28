package presenter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import fxml_controller.ChoiceWindowController;
import fxml_controller.ControllerForDragDrop;
import fxml_controller.PromptWindowController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.NetClient;
import model.sslEngineUtil.ConnectionClosedException;
import model.sslEngineUtil.ConnectionErrorException;
//every Presenter controls one promptState and one choiceStage, plus providing mouse drag & mouse drop features by pressing the header
//Also, it has access to model(NetClient), etc.
public abstract class Presenter {
	String resourcesDir = System.getProperty("user.dir") + File.separator + "resources";
	NetClient netClient;
	//current showing stage(except promptWindow, choiceWindow, etc.)
	Stage stage;
	ResourceBundle resourceBundle;
	//only Connection Error will show a promptStage passively for user, all the other promptStage is shown by user operation
	//therefore if one promptStage is shown by user operation and later connection error occurs, 
	//the program will just close the previews promptStage, reset the promptStage and show it again with Connection Error
	Stage promptStage;
	//no multiple choiceStage should be shown at the same time
	Stage choiceStage;
	//promptPane is created here and used by subClass of Presenter, only one promptPane and one choicePane can be shown at the same time
	//closeBtn, closeBtn2, setPromptInfo()
	double mouseToStageX;
	double mouseToStageY;
	private BorderPane promptPane;
	private PromptWindowController promptController;
	//yesBtn, initChoicePane
	private BorderPane choicePane;
	private ChoiceWindowController choiceController;
	
	ExecutorService threadPool = null;
	
	Presenter(ExecutorService threadPool_, Stage stage_, NetClient netClient_,ResourceBundle rb){
		netClient = netClient_;
		stage = stage_;
		resourceBundle = rb;
		threadPool = threadPool_;
		loadPane();
	}
	
	private void initStage() {
		promptStage = createNewStage();
		setStage(promptStage, promptController, promptPane);
		choiceStage = createNewStage();
		setStage(choiceStage, choiceController, choicePane);
	}
	private void loadPane() {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "common"+ File.separator + "promptWindow.fxml").toURI().toURL(), resourceBundle);
			promptPane = fxmlLoader.<BorderPane>load();
			promptController = fxmlLoader.<PromptWindowController>getController();
			FXMLLoader fxmlLoader2 = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "common"+ File.separator + "choiceWindow.fxml").toURI().toURL(), resourceBundle);
			choicePane = fxmlLoader2.<BorderPane>load();
			choiceController = fxmlLoader2.<ChoiceWindowController>getController();
			initStage();
		}catch(IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load fxml files in loadPane() of Presenter");
		}
	}
	/*private void onReconnectClose() {
		showChoiceStage(resourceBundle.getString("ExitProgram"), resourceBundle.getString("ExitProgramConfirmMessage"), ()->exitWithConnectionError());
		choiceStage.show();
	}*/
	private void setPromptInfo(String title, String message) {
		promptController.titleText.setText(title);
		promptController.messageText.setText(message);
	}
	//set the position of newStage based on stage, make the newStage shown in the middle of the stage
	
	/************************************************package accessible methods************************************************/
	void setStagePosition(Stage newStage, Stage relevantStage) {
		//if the stage never call stage.show(), then stage.getScene().getWidth() or height() will be zero 
		Scene newScene = newStage.getScene();
		Scene relevantScene = relevantStage.getScene();
		if(relevantStage != null && newScene != null && relevantScene != null && newScene.getWidth() > 0) {
			double x = relevantStage.getX() + relevantScene.getWidth()/2.0 - newScene.getWidth()/2.0;
			double y = relevantStage.getY() + relevantScene.getHeight()/2.0 - newScene.getHeight()/2.0;
			newStage.setX(x);
			newStage.setY(y);
		}
	}
	Stage createNewStage() {
		Stage newStage = new Stage();
		newStage.initModality(Modality.APPLICATION_MODAL);
		newStage.initStyle(StageStyle.UNDECORATED);
		return newStage;
	}
	Stage createNewStage(Stage parentStage) {
		Stage newStage = new Stage();
		newStage.initModality(Modality.APPLICATION_MODAL);
		newStage.initStyle(StageStyle.UNDECORATED);
		newStage.initOwner(parentStage);
		return newStage;
	}
	//you should set closeBtn and closeBtn2 additionally according to the situation where promptPane is used, this method will show promptStage
	void showPromptPaneInfo(String title, String message) {
		//close and show, rearrange the size of new window
		promptStage.close();
		promptController.titleText.setText(title);
		promptController.messageText.setText(message);
		promptStage.show();
		setStagePosition(promptStage,stage);
	}
	//you should set yesBtn additionally according to the situation where choicePane is used
	void initChoicePane(Stage choiceStage, String title, String message) {
		choiceController.titleText.setText(title);
		choiceController.messageText.setText(message);
		choiceController.cancelBtn.setOnAction(event->choiceStage.close());
		choiceController.noBtn.setOnAction(event->choiceStage.close());
		choiceController.closeBtn.setOnAction(event->choiceStage.close());
	}
	
	//default handler, just close the PromptStage
	void setPromptStage(String title, String message) {
		if(promptStage.isShowing()) {
			promptStage.close();
		}
		promptController.closeBtn.setOnAction(e->promptStage.close());
		promptController.closeBtn2.setOnAction(e->promptStage.close());
		setPromptInfo(title, message);
	}
	//close the promptStage and do some other work like closing the parent Stage of PromptStage
	void setPromptStage(String title, String message,Runnable additionalTask) {
		if(promptStage.isShowing()) {
			promptStage.close();
		}
		promptController.closeBtn.setOnAction(e->{
			promptStage.close();
			additionalTask.run();
		});
		promptController.closeBtn2.setOnAction(e->{
			promptStage.close();
			additionalTask.run();
		});
		setPromptInfo(title, message);
	}
	void setPromptStage(String title, String message, EventHandler<ActionEvent> closeBtnHandler) {
		if(promptStage.isShowing()) {
			promptStage.close();
		}
		promptController.closeBtn.setOnAction(closeBtnHandler);
		promptController.closeBtn2.setOnAction(closeBtnHandler);
		setPromptInfo(title, message);
	}
	void showPromptStage(String title, String message) {
		setPromptStage(title, message);
		promptStage.show();
		setStagePosition(promptStage, stage);
	}
	//close the promptStage and do some other work like closing the parent Stage of PromptStage
	void showPromptStage(String title, String message,Runnable additionalTask) {
		setPromptStage(title, message, additionalTask);
		promptStage.show();
		setStagePosition(promptStage, stage);
	}
	void showPromptStage(String title, String message, EventHandler<ActionEvent> closeBtnHandler) {
		setPromptStage(title, message, closeBtnHandler);
		promptStage.show();
		setStagePosition(promptStage, stage);
	}
	void showChoiceStage(String title, String message, Runnable yesBtnHandler) {
		if(choiceStage.isShowing()) {
			choiceStage.close();
		}
		choiceController.yesBtn.setOnAction(event->{
			choiceStage.close();
			yesBtnHandler.run();
		});
		initChoicePane(choiceStage, title, message);
		choiceStage.show();
		setStagePosition(choiceStage, stage);
	}
	//when user press closeBtn, use this method to handle it
	void onExitingConfirm() {
		showChoiceStage(resourceBundle.getString("Exiting_Program"), resourceBundle.getString("Are_you_sure_to_exit?"), ()->closePresenter());
	}
	//when you call netClient.sendBuffer(...) or netClient.receiveBuffer and Exception occurs, using this method to handle connection error
	//should be called in JavaFX Application Thread, do not exit the program
	void presenterHandleException(Exception e) {
		//handle connection error
		e.printStackTrace();
		Platform.runLater(()->{
			if(e instanceof ConnectionErrorException || e instanceof ConnectionClosedException) {
				threadPool.submit(()->reconnect());
			}else {
				//other Exception, not Connection Error
				showPromptStage(resourceBundle.getString("Program_error"), e.getMessage(), aEvent->closePresenter());
				promptStage.show();
			}
		});
	}
	void presenterHandleExceptionExit(Exception e) {
		//handle connection error
		e.printStackTrace();
		/*Platform.runLater(()->{
			if(e instanceof ConnectionErrorException || e instanceof ConnectionClosedException) {
				reconnect();
			}else {
				//other Exception, not Connection Error
				showPromptStage(resourceBundle.getString("Program_error"), e.getMessage(), aEvent->closePresenter());
				promptStage.show();
			}
			//closePresenter();
		});*/
		closePresenter();
	}
	//press-drag feature
	void handleMousePressed(Stage newStage, MouseEvent e){
		mouseToStageX = e.getScreenX() - newStage.getX();
		mouseToStageY = e.getScreenY() - newStage.getY();
	}
	void handleMouseDragged(Stage newStage, MouseEvent e){
		newStage.setX(e.getScreenX()-mouseToStageX);
		newStage.setY(e.getScreenY()-mouseToStageY);
	}
	//transfer to a new scene for the stage and set the mouse-event handler for headerBox to support press-drag
	//the pane is expected to be managed by passed controller.
	void setStage(Stage newStage, ControllerForDragDrop controller, Parent pane) {
		if(pane.getScene() == null) {
			newStage.setScene(new Scene(pane));
		}else {
			newStage.setScene(pane.getScene());
		}
		//clear old Event handler.
		controller.headerBox.setOnMousePressed(null);
		controller.headerBox.setOnMouseDragged(null);
		controller.headerBox.setOnMousePressed(e->handleMousePressed(newStage, e));
		controller.headerBox.setOnMouseDragged(e->handleMouseDragged(newStage, e));	
	}
	/************************************************public methods************************************************/
	public void closePresenter() {
		Platform.exit();
		netClient.exit();
		threadPool.shutdownNow();
	}
	public void exitWithConnectionError() {
		//tell use the program will exit
		showPromptStage(resourceBundle.getString("Connection_Error"), resourceBundle.getString("ConnectionFailedMessage"),e->closePresenter());
		promptStage.show();
	}
	//Override it in subClass
	//language code 0--en	1--cn
	/*public void setResourceBundle(int languageCode, ResourceBundle rb) {
		resourceBundle = rb;
		loadPane();
		//set the value of language in the configuration file
		try(RandomAccessFile randomAccessFile = new RandomAccessFile(new File(resourcesDir + File.separator + "data" + File.separator + "data3.dat"),"rw")) {
			randomAccessFile.writeInt(languageCode);
		}catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to setResourceBundle() in Presenter for file data1.dat");
		}
	}*/
	//Override it, you should call super.receiveData() at the last line of subclass method
	abstract public void receiveData(ByteBuffer byteBuffer, int bufferIndex);
	abstract public void reconnect();
}
