package fxml_controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;

import javafx.fxml.FXML;

public class LoginController extends Controller_TypeA {
	@FXML public JFXTextField usernameTfd;
	@FXML public JFXPasswordField passwordTfd;
	@FXML public JFXCheckBox usernameCB;
	@FXML public JFXCheckBox passwordCB;
	@FXML public JFXButton forgetPasswordBtn;
	@FXML public JFXButton loginBtn;
	@FXML public JFXButton registerBtn;
	
	public LoginController() {}
}
