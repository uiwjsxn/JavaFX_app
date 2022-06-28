package fxml_controller;

import com.jfoenix.controls.JFXButton;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public abstract class Controller_TypeA extends ControllerForDragDrop{
	@FXML public JFXButton configBtn;
	@FXML public JFXButton minusBtn;
	@FXML public JFXButton closeBtn;
	@FXML public ImageView imageView;
	
	public Controller_TypeA(){}
}
