package fxml_controller;

import com.jfoenix.controls.JFXButton;

import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class PromptWindowController extends ControllerForDragDrop{
	@FXML public JFXButton closeBtn;
	@FXML public JFXButton closeBtn2;
	@FXML public Text titleText;
	@FXML public Text messageText;
	
	public PromptWindowController() {}
}
