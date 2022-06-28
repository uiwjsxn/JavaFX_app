package fxml_controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class VerifyEmailController  extends Controller_TypeA{
	public enum USAGE{REGISTER, RESET_PASSWORD};
	@FXML public JFXTextField codeTfd;
	@FXML public JFXButton resendCodeBtn;
	@FXML public JFXButton verifyBtn;
	@FXML public JFXButton backBtn;
	@FXML public Label countDownLbl;
	@FXML public Label titleLabel;
	
	private USAGE usage;
	
	public VerifyEmailController() {}
	public void setUsage(USAGE usage) {
		this.usage = usage;
	}
	public USAGE getUsage() {
		return usage;
	}
}
