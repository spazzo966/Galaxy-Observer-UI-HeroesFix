package com.ahli.hotkeyUi.application.controller;

import java.net.URL;

import com.ahli.hotkeyUi.application.Main;
import com.ahli.hotkeyUi.application.Messages;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * 
 * @author Ahli
 *
 */
public class MenuBarController {
//	static Logger LOGGER = LogManager.getLogger("MenuBarController"); //$NON-NLS-1$

	private Main main;

	@FXML
	private MenuItem menuOpen;
	@FXML
	private MenuItem menuSave;
	@FXML
	private MenuItem menuSaveAs;
	@FXML
	private MenuItem menuClose;

	/**
	 * Automatically called on Controller initialization.
	 */
	public void initialize() {
		menuOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
		menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		menuSaveAs.setAccelerator(
				new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
		menuClose.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
	}

	/**
	 * 
	 * @param main
	 */
	public void setMainApp(Main main) {
		this.main = main;
	}

	/**
	 * Called when the user clicks on the delete button.
	 */
	@FXML
	public void close(ActionEvent event) {
		main.closeFile();
	}

	/**
	 * Called when the user clicks on the open button.
	 */
	@FXML
	public void open(ActionEvent event) {
		main.openUiMpq();
	}

	/**
	 * Called when the user clicks on the save as button.
	 */
	@FXML
	public void saveAs(ActionEvent event) {
		main.saveAsUiMpq();
	}

	/**
	 * Called when the user clicks on the save as button.
	 */
	@FXML
	public void saveCurrent(ActionEvent event) {
		main.saveUiMpq();
	}

	/**
	 * Called when the user clicks on the save as button.
	 */
	@FXML
	public void exit(ActionEvent event) {
		main.closeApp();
	}

	/**
	 * Updates the menu bar's items.
	 */
	public void updateMenuBar() {
		boolean docLoaded = main.isValidOpenedDocPath();

		menuSave.setDisable(!docLoaded);
		menuSaveAs.setDisable(!docLoaded);
		menuClose.setDisable(!docLoaded);
	}

	/**
	 * clicked on About
	 */
	@FXML
	public void aboutClicked() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(Messages.getString("MenuBarController.About")); //$NON-NLS-1$
		alert.setHeaderText(Messages.getString("MenuBarController.ObserverUISettingsEditor")); //$NON-NLS-1$
		alert.setContentText(String.format(Messages.getString("MenuBarController.AboutText"), Main.VERSION)); //$NON-NLS-1$

		URL imgUrl = Main.class.getResource("/res/ahliLogo.png"); //$NON-NLS-1$
		alert.setGraphic(new ImageView(imgUrl.toString()));
		alert.showAndWait();
	}

}
