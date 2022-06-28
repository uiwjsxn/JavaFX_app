package presenter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import fxml_controller.Controller_TypeA;
import fxml_controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.NetClient;

public class MainPresenter extends Presenter {
	//userDataBuffer is acquired after successfully login in LoginPresenter.
	//LoginPresenter does not handle this Buffer, instead handing it out to MainPresenter since the buffer is irrelevant to login checking
	private LoginPresenter loginPresenter;
	private BorderPane mainPane;
	private MainController mainController;
	
	public MainPresenter(ExecutorService threadPool_, Stage stage_, NetClient netClient_, ResourceBundle rb, ByteBuffer userDataBuffer, LoginPresenter loginPresenter_){
		super(threadPool_, stage_, netClient_, rb);
		loginPresenter = loginPresenter_;
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(new File(resourcesDir + File.separator + "fxml" + File.separator + "pane"+ File.separator + "main.fxml").toURI().toURL(),resourceBundle);
			mainPane = fxmlLoader.<BorderPane>load();
			mainController = fxmlLoader.<MainController>getController();
		}catch(IOException e) {
			e.printStackTrace();
			presenterHandleExceptionExit(new RuntimeException("Failed to load fxml files in MainPresenter"));
		}
		setStage(stage, mainController, mainPane);
		setTemplateHandler(new Controller_TypeA[] {mainController});
		stage.show();
	}
	
	@Override public void receiveData(ByteBuffer byteBuffer, int bufferIndex) {
		
		
	}

	@Override public void reconnect() {
		loginPresenter.reconnect();
	}
	
	private void setTemplateHandler(Controller_TypeA[] controllers) {
		MenuItem menuItemInternet = new MenuItem(resourceBundle.getString("Internet"));
		ToggleGroup toggleGroup = new ToggleGroup();
		RadioMenuItem enMenuItem = new RadioMenuItem("English");
		enMenuItem.setToggleGroup(toggleGroup);
		RadioMenuItem cnMenuItem = new RadioMenuItem("中文");
		cnMenuItem.setToggleGroup(toggleGroup);
		Menu languageMenu = new Menu(resourceBundle.getString("Language"),null,enMenuItem,cnMenuItem);
		
		ContextMenu contextMenu = new ContextMenu(languageMenu,menuItemInternet);
		
		for(Controller_TypeA controller : controllers) {
			controller.closeBtn.setOnAction(e->onExitingConfirm());
			controller.minusBtn.setOnAction(e->stage.setIconified(true));
			controller.configBtn.setOnAction(e->{
				contextMenu.show(controller.configBtn,Side.BOTTOM,0.0,0.0);
			});
			try {
				Image image = new Image(new File(resourcesDir + File.separator + "pictures" + File.separator + "login.jpg").toURI().toURL().toString(), 600, 150, true, true);
				if(controller.imageView != null) {
					controller.imageView.setImage(image);
				}
			}catch(Exception e) {System.out.println(e.getMessage());}
		}
	}
}
