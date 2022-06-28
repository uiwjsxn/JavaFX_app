package fxml_controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

public abstract class ControllerForDragDrop {
	@FXML public HBox headerBox;
	public ControllerForDragDrop(){}
	@FXML private void initialize() {
		Parent parent = headerBox;
		while(parent.getParent() != null) {
			parent = parent.getParent();
		}
		parent.setStyle("-fx-border-width: 2; -fx-border-color: lavender;");
	}
}
