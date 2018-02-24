package application.ui.home;

import application.InterfaceBuilderApp;
import application.projects.Project;
import application.projects.ProjectService;
import application.ui.settings.Updateable;
import application.util.FXMLSpringLoader;
import application.util.FileService;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class HomeController implements Updateable {
	private static final Logger logger = LogManager.getLogger();
	@FXML
	private Pane selectedPanel;
	@FXML
	private Label selectedName;
	@FXML
	private ImageView selectedImage;
	@FXML
	private Label selectedPath;
	@FXML
	private Label selectedDirSize;
	@FXML
	private Label selectedDirFiles;
	@FXML
	private Label selectedBuildDate;
	@FXML
	private Label selectedBuildSize;
	@FXML
	private Button addProject;
	@FXML
	private Button removeProject;
	@FXML
	private Button editProject;
	@Autowired
	private ApplicationContext appContext;
	@FXML
	private ListView<Project> selectionList;
	@FXML
	private Button buildSelection;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private FileService fileService;
	
	private ObservableList<Project> projectsObservable;
	
	/**
	 * Automatically called by FxmlLoader
	 */
	public void initialize() {
		// grab list of projects per game
		projectsObservable = FXCollections.observableList(projectService.getAllProjects());
		selectionList.setItems(projectsObservable);
		selectionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		selectionList.setCellFactory(new Callback<>() {
			@Override
			public ListCell<Project> call(final ListView<Project> p) {
				return new ListCell<>() {
					@Override
					protected void updateItem(final Project project, final boolean empty) {
						super.updateItem(project, empty);
						if (empty || project == null) {
							setText(null);
							setGraphic(null);
						} else {
							setText(project.getName());
							try {
								setGraphic(getListItemGameImage(project));
							} catch (final IOException e) {
								logger.error("Failed to find image resource.", e);
							}
						}
					}
				};
			}
		});
		selectionList.getSelectionModel().selectAll();
		selectionList.getSelectionModel().getSelectedItems()
				.addListener((InvalidationListener) observable -> updateSelectedDetailsPanel());
	}
	
	/**
	 * Returns an image reflecting the game with proper size for the project list.
	 *
	 * @param project
	 * @return
	 * @throws IOException
	 */
	private ImageView getListItemGameImage(final Project project) throws IOException {
		final ImageView iv = new ImageView(getResourceAsUrl(getGameItemPath(project)).toString());
		iv.setFitHeight(32);
		iv.setFitWidth(32);
		return iv;
	}
	
	/**
	 * Updates the panel showing info about the selected projects.
	 */
	private void updateSelectedDetailsPanel() {
		final ObservableList<Project> selectedItems = selectionList.getSelectionModel().getSelectedItems();
		if (selectedItems.size() == 1) {
			selectedPanel.setVisible(true);
			final Project p = selectedItems.get(0);
			selectedName.setText(p.getName());
			final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			selectedBuildDate.setText(p.getLastBuildDate() == null ? "-" : formatter.format(p.getLastBuildDate()));
			selectedBuildSize.setText(
					p.getLastBuildSize() == null ? "-" : String.format("%,d", p.getLastBuildSize() / 1024) + " kb");
			try {
				final File f = new File(p.getProjectPath());
				selectedDirFiles.setText(String.format("%,d", fileService.getFileCountOfDirectory(f)) + " files");
				selectedDirSize.setText(String.format("%,d", fileService.getDirectorySize(f) / 1024) + " kb");
			} catch (final IOException e) {
				selectedDirSize.setText("-");
				selectedDirFiles.setText("-");
			}
			selectedPath.setText(p.getProjectPath());
			try {
				selectedImage.setImage(new Image(getResourceAsUrl(getGameItemPath(p)).toString()));
			} catch (final IOException e) {
				logger.error("Failed to load image from project's game setting.", e);
			}
		} else {
			selectedPanel.setVisible(false);
		}
	}
	
	/**
	 * Returns a resource as a URL.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private URL getResourceAsUrl(final String path) throws IOException {
		return appContext.getResource(path).getURL();
	}
	
	/**
	 * Returns the path of the image that reflects the game of the specified project.
	 *
	 * @param project
	 * @return
	 */
	private String getGameItemPath(final Project project) {
		switch (project.getGame()) {
			case SC2:
				return "res/sc2.png";
			case HEROES:
				return "res/heroes.png";
			default:
				return "res/ahli.png";
		}
	}
	
	@Override
	public void update() {
		updateSelectedDetailsPanel();
	}
	
	/**
	 * Add a Project to the list.
	 *
	 * @throws IOException
	 */
	public void addProjectAction() throws IOException {
		final FXMLLoader loader = new FXMLSpringLoader(appContext);
		final Dialog<Project> dialog =
				loader.load(appContext.getResource("view/Home_AddProject.fxml").getInputStream());
		dialog.initOwner(addProject.getScene().getWindow());
		final Optional<Project> result = dialog.showAndWait();
		if (result.isPresent()) {
			logger.trace("dialog 'add project' result: " + result.get());
			projectsObservable.add(result.get());
		}
	}
	
	/**
	 * Edit a single selected project.
	 *
	 * @throws IOException
	 */
	public void editProjectAction() throws IOException {
		final List<Project> projects = selectionList.getSelectionModel().getSelectedItems();
		if (projects.size() == 1) {
			final Project project = projects.get(0);
			final FXMLLoader loader = new FXMLSpringLoader(appContext);
			final Dialog<Project> dialog =
					loader.load(appContext.getResource("view/Home_AddProject.fxml").getInputStream());
			dialog.initOwner(addProject.getScene().getWindow());
			((AddProjectController) loader.getController()).setProjectToEdit(project);
			final Optional<Project> result = dialog.showAndWait();
			if (result.isPresent()) {
				logger.trace("dialog 'edit project' result: " + result.get());
				updateProjectList();
			}
		}
	}
	
	/**
	 * Update the project list to reflect name/game changes.
	 */
	private void updateProjectList() {
		final MultipleSelectionModel<Project> selectionModel = selectionList.getSelectionModel();
		final Object[] selectedIndices = selectionModel.getSelectedIndices().toArray();
		selectionModel.clearSelection();
		for (final Object i : selectedIndices) {
			selectionModel.select((int) i);
		}
	}
	
	/**
	 * Builds the selected projects.
	 */
	public void buildSelectedAction() {
		final List<Project> selectedItems = selectionList.getSelectionModel().getSelectedItems();
		if (!selectedItems.isEmpty()) {
			InterfaceBuilderApp.getInstance().getNavigationController().clickProgress();
			projectService.build(selectedItems, false);
		}
	}
	
	/**
	 * Removes the selected projects.
	 */
	public void removeSelectedAction() {
		final List<Project> selectedItems = selectionList.getSelectionModel().getSelectedItems();
		if (!selectedItems.isEmpty()) {
			final Project[] items = selectedItems.toArray(new Project[0]);
			final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.initOwner(getWindow());
			alert.setTitle(
					String.format("Remove selected Project from list? - %s items selected", selectedItems.size()));
			alert.setHeaderText("Are you sure you want to remove the selected projects?" + "\n" +
					"This will not remove any files from the project.");
			final Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.OK) {
				for (final Project p : items) {
					projectService.deleteProject(p);
					projectsObservable.remove(p);
				}
			}
		}
	}
	
	/**
	 * Returns this UI's window.
	 *
	 * @return
	 */
	public Window getWindow() {
		return selectionList.getScene().getWindow();
	}
}
