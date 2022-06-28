package fxml_controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class EnterEmailController extends Controller_TypeA {
	public enum USAGE{REGISTER, RESET_PASSWORD};
	@FXML public JFXTextField emailAddressTfd;
	@FXML public JFXButton sendEmailBtn;
	@FXML public Label titleLabel;
	@FXML public JFXButton backBtn;
	
	private USAGE usage;
	
	public EnterEmailController() {}
	public void setUsage(USAGE usage) {
		this.usage = usage;
	}
	public USAGE getUsage() {
		return usage;
	}
}
