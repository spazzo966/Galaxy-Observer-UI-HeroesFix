// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.interfacebuilder.ui.home;

import com.ahli.interfacebuilder.i18n.Messages;
import com.ahli.interfacebuilder.integration.FileService;
import com.ahli.interfacebuilder.projects.Project;
import com.ahli.interfacebuilder.projects.ProjectService;
import com.ahli.interfacebuilder.projects.enums.GameType;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Path;

@Log4j2
public class AddProjectController {
	private final ProjectService projectService;
	private final FileService fileService;
	@FXML
	private ChoiceBox<GameType> gameDropdown;
	@FXML
	private TextField projectPathLabel;
	@FXML
	private TextField projectNameLabel;
	private Dialog<Project> dialog;
	private Project project;
	
	public AddProjectController(final ProjectService projectService, final FileService fileService) {
		this.projectService = projectService;
		this.fileService = fileService;
	}
	
	public void browsePathAction() {
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(Messages.getString("addProject.selectExistingObserverUiDirectory"));
		final Path path = fileService.cutTillValidDirectory(Path.of(projectPathLabel.getText()));
		if (path != null) {
			directoryChooser.setInitialDirectory(path.toFile());
		}
		final File f = directoryChooser.showDialog(getWindow());
		if (f != null) {
			projectPathLabel.setText(f.getAbsolutePath());
		}
	}
	
	private Window getWindow() {
		return projectPathLabel.getScene().getWindow();
	}
	
	/**
	 * Set a project to be edited.
	 *
	 * @param project
	 */
	public void setProjectToEdit(final Project project) {
		this.project = project;
		dialog.setTitle(Messages.getString("addProject.editObserverInterfaceProject"));
		projectNameLabel.setText(project.getName());
		projectPathLabel.setText(project.getProjectPath().toString());
		gameDropdown.getSelectionModel().select(project.getGameType());
	}
	
	/**
	 * Initialize this Controller and sets its dialog.
	 *
	 * @param dialog
	 */
	public void initialize(final Dialog<Project> dialog) {
		this.dialog = dialog;
		initialize();
	}
	
	/**
	 * Automatically called by FxmlLoader
	 */
	private void initialize() {
		if (dialog != null) {
			dialog.setTitle(Messages.getString("addProject.addObserverInterfaceProject"));
			final DialogPane dialogPane = dialog.getDialogPane();
			dialogPane.getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
			final Button okBttn = (Button) dialogPane.lookupButton(ButtonType.APPLY);
			okBttn.addEventFilter(ActionEvent.ACTION, this::addProjectAction);
			okBttn.setText(Messages.getString("general.addButton"));
			dialog.setResultConverter(param -> project);
			
			gameDropdown.setItems(FXCollections.observableArrayList(GameType.SC2, GameType.HEROES));
			gameDropdown.getSelectionModel().select(0);
		}
	}
	
	public void addProjectAction(final Event event) {
		log.trace("add project action event fired");
		final GameType gameType = gameDropdown.getValue();
		if (gameType == null) {
			// eat event before it reaches the resultConverter
			event.consume();
			return;
		}
		final String name = projectNameLabel.getText();
		final Path path = Path.of(projectPathLabel.getText());
		if (project == null) {
			project = new Project(name, path, gameType);
		} else {
			project.setGameType(gameType);
			project.setName(name);
			project.setProjectPath(path);
		}
		project = projectService.saveProject(project);
	}
}
