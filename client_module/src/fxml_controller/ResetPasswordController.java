package fxml_controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

public class ResetPasswordController extends Controller_TypeA{
	@FXML public JFXPasswordField passwordTfd1;
	@FXML public JFXPasswordField passwordTfd2;
	@FXML public JFXButton submitBtn;
	@FXML public JFXButton cancelBtn;
	//just set visible of promptLabel, do not need to set text of promptLabel
	@FXML public Label promptLabel;
	@FXML public Label titleLabel;
	@FXML public Label iconPromptLbl;
	@FXML public Circle iconCircle;
	
	private byte[] imageBytes;
	
	public ResetPasswordController() {}
	public void setImageBytes(byte[] imageBytes) {
		this.imageBytes = imageBytes;
	}
	public byte[] getImageBytes() {
		return imageBytes;
	}
}
