package fxml_controller;

import com.jfoenix.controls.JFXButton;

import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class ChoiceWindowController extends ControllerForDragDrop{
	@FXML public JFXButton closeBtn;
	@FXML public JFXButton yesBtn;
	@FXML public JFXButton noBtn;
	@FXML public JFXButton cancelBtn;
	
	@FXML public Text titleText;
	@FXML public Text messageText;
	
	public ChoiceWindowController() {}
}
