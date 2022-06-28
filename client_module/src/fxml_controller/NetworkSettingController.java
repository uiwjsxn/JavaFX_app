package fxml_controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import util.Util;

public class NetworkSettingController extends ControllerForDragDrop{
	@FXML public JFXButton closeBtn;
	@FXML public JFXButton confirmBtn;
	@FXML public JFXButton cancelBtn;
	@FXML public JFXButton resetBtn;
	@FXML public JFXTextField hostnameTfd1;
	@FXML public JFXTextField portTfd1;
	@FXML public JFXTextField hostnameTfd2;
	@FXML public JFXTextField portTfd2;
	@FXML public JFXTextField hostnameTfd3;
	@FXML public JFXTextField portTfd3;
	
	@FXML private Label hostnameLabel1;
	@FXML private Label portLabel1;
	@FXML private Label hostnameLabel2;
	@FXML private Label portLabel2;
	@FXML private Label hostnameLabel3;
	@FXML private Label portLabel3;
	@FXML private Accordion accordion;
	@FXML private TitledPane firstTitledPane;
	
	private String validString;
	private String invalidString;
	
	public NetworkSettingController() {}
	
	private void setHostNameListener(JFXTextField hostnameTfd, Label hostnameLabel) {
		hostnameTfd.textProperty().addListener((property,oldValue,newValue)->{
			if(checkHostName(newValue)) {
				hostnameLabel.setText(validString);
			}else {
				hostnameLabel.setText(invalidString);
			}
		});
	}
	private void setPortListener(JFXTextField portTfd, Label portLabel) {
		portTfd.textProperty().addListener((property,oldValue,newValue)->{
			if(checkPort(Util.parseUnsignedInteger(newValue))) {
				portLabel.setText(validString);
			}else {
				portLabel.setText(invalidString);
			}
		});
	}
	
	@FXML private void initialize() {
		//validString and invalidString may have multiple languages. 
		//To add the support for multiple language and avoid passing ResourceBundle to NetworkSettingController.
		validString = hostnameLabel1.getText();
		invalidString = portLabel1.getText();
		hostnameLabel1.setText("");
		hostnameLabel1.setVisible(true);
		portLabel1.setText("");
		portLabel1.setVisible(true);
		
		setHostNameListener(hostnameTfd1,hostnameLabel1);
		setHostNameListener(hostnameTfd2,hostnameLabel2);
		setHostNameListener(hostnameTfd3,hostnameLabel3);
		setPortListener(portTfd1,portLabel1);
		setPortListener(portTfd2,portLabel2);
		setPortListener(portTfd3,portLabel3);
		resetPane();
	}
	
	public boolean checkHostName(String hostname) {
		if(!hostname.equals("localhost") && !hostname.matches("^([a-z0-9][a-z0-9_]+\\b\\.\\b)+[a-z]+$")) {
			return false;
		}
		return true;
	}
	public boolean checkPort(int port) {
		if(port < 1 || port > 65535) {
			return false;
		}
		return true;
	}
	public void resetPane() {
		accordion.setExpandedPane(firstTitledPane);
		accordion.getParent().setStyle("-fx-border-width: 1; -fx-border-color: silver;");
	}
}
