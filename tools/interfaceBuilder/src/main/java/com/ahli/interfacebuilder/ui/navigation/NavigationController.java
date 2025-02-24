// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.interfacebuilder.ui.navigation;

import com.ahli.interfacebuilder.ui.FXMLSpringLoader;
import com.ahli.interfacebuilder.ui.Updateable;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class NavigationController {
	public static final int HOME_TAB = 0;
	public static final int PROGRESS_TAB = 1;
	public static final int BROWSE_TAB = 2;
	public static final int SETTINGS_TAB = 3;
	/* ContentPages:
	 * 0: taskChoice
	 * 1: tabPane
	 * 2: settings */
	private final Parent[] contentPages = new Parent[4];
	private final Updateable[] controllers = new Updateable[4];
	private final List<Notification> notifications = new ArrayList<>(0);
	private final ApplicationContext appContext;
	@FXML
	private AnchorPane selectedMarker;
	@FXML
	private Button browse;
	@FXML
	private Label notificationLabel;
	@FXML
	private AnchorPane notificationBar;
	@FXML
	private AnchorPane contentContainer;
	@FXML
	private Button home;
	@FXML
	private Button settings;
	@FXML
	private Button progress;
	private int activeContent = -1;
	
	public NavigationController(final ApplicationContext appContext) {
		this.appContext = appContext;
	}
	
	/**
	 * Automatically called by FxmlLoader
	 */
	public void initialize() {
		// nav button icons
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.HOME);
		icon.setFill(Color.WHITE);
		home.setGraphic(icon);
		home.setText(null);
		icon = new FontAwesomeIconView(FontAwesomeIcon.COG);
		icon.setFill(Color.WHITE);
		settings.setGraphic(icon);
		settings.setText(null);
		icon = new FontAwesomeIconView(FontAwesomeIcon.BAR_CHART);
		icon.setFill(Color.WHITE);
		progress.setGraphic(icon);
		progress.setText(null);
		icon = new FontAwesomeIconView(FontAwesomeIcon.EYE);
		icon.setFill(Color.WHITE);
		browse.setGraphic(icon);
		browse.setText(null);
		
		notificationBar.setVisible(false);
		notificationBar.managedProperty().bind(notificationBar.visibleProperty());
		notificationBar.setBackground(new Background(new BackgroundFill(Color.color(211.0D / 256.0D,
				168.0D / 255.0D,
				3.0D / 255.0D), CornerRadii.EMPTY, Insets.EMPTY)));
		selectedMarker.setBackground(new Background(new BackgroundFill(Color.color(211.0D / 256.0D,
				168.0D / 255.0D,
				3.0D / 255.0D), CornerRadii.EMPTY, Insets.EMPTY)));
		
		// content pages
		initFXML("classpath:view/Home.fxml", 0);
		initFXML("classpath:view/Progress.fxml", 1);
		initFXML("classpath:view/Browse.fxml", 2);
		initFXML("classpath:view/Settings.fxml", 3);
		
		// make tabPane visible
		showPanelContent(HOME_TAB);
	}
	
	/**
	 * Initializes the TabPane for the Compiling Progress.
	 */
	private void initFXML(final String path, final int index) {
		try {
			final FXMLSpringLoader loader = new FXMLSpringLoader(appContext);
			contentPages[index] = loader.load(path);
			final Object controller = loader.getController();
			controllers[index] = (controller instanceof final Updateable updateable) ? updateable : null;
		} catch (final IOException e) {
			log.error(String.format("failed to load FXML: %s.", path), e);
			contentPages[index] = null;
			controllers[index] = null;
		}
	}
	
	/**
	 * Shows the content of a specified index in the central panel.
	 *
	 * @param contentIndex
	 */
	private void showPanelContent(final int contentIndex) {
		if (contentIndex != activeContent && contentPages[contentIndex] != null) {
			activeContent = contentIndex;
			final ObservableList<Node> activeNodes = contentContainer.getChildren();
			activeNodes.clear();
			// update event for controller
			if (controllers[contentIndex] != null) {
				controllers[contentIndex].update();
			}
			activeNodes.add(contentPages[contentIndex]);
			// move marker image
			markTab(contentIndex);
		}
	}
	
	/**
	 * @param contentIndex
	 */
	private void markTab(final int contentIndex) {
		selectedMarker.setLayoutY(8.0 + contentIndex * 28.0);
	}
	
	/**
	 * Called when the user clicks on the home button.
	 */
	@FXML
	public void clickHome() {
		showPanelContent(HOME_TAB);
	}
	
	/**
	 * Called when the user clicks on the progress button.
	 */
	@FXML
	public void clickProgress() {
		showPanelContent(PROGRESS_TAB);
	}
	
	/**
	 *
	 */
	@FXML
	public void clickBrowse() {
		showPanelContent(BROWSE_TAB);
	}
	
	/**
	 * Called when the user clicks on the settings button.
	 */
	@FXML
	public void clickSettings() {
		showPanelContent(SETTINGS_TAB);
	}
	
	/**
	 * Locks the navigation to the Progress screen.
	 */
	public void lockNavToProgress() {
		home.setDisable(true);
		settings.setDisable(true);
		browse.setDisable(true);
		showPanelContent(PROGRESS_TAB);
	}
	
	/**
	 * Releases all navigation button locks.
	 */
	public void unlockNav() {
		home.setDisable(false);
		settings.setDisable(false);
		progress.setDisable(false);
		browse.setDisable(false);
	}
	
	/**
	 * Appends a notification that will be shown.
	 *
	 * @param notification
	 */
	public void appendNotification(final Notification notification) {
		notifications.add(notification);
		showFirstNotification();
	}
	
	/**
	 * Shows the first notification. Ensure that the notification-list is not empty!
	 */
	private void showFirstNotification() {
		notificationBar.setVisible(true);
		final Notification notification = notifications.get(0);
		notificationLabel.setText(notification.getText());
	}
	
	/**
	 * Closes a notification of the specified id, if one exists.
	 *
	 * @param id
	 */
	public void closeNotification(final String id) {
		int i = 0;
		for (final var notification : notifications) {
			if (id.equals(notification.getId())) {
				notifications.remove(i);
				if (notifications.isEmpty()) {
					notificationBar.setVisible(false);
				} else if (i == 0) {
					showFirstNotification();
				}
				return;
			}
			++i;
		}
	}
	
	@FXML
	public void closeActiveNotification() {
		if (!notifications.isEmpty()) {
			notifications.remove(0);
			if (notifications.isEmpty()) {
				notificationBar.setVisible(false);
			} else {
				showFirstNotification();
			}
		}
	}
	
	@FXML
	public void openNotificationLink() {
		final Notification notification = notifications.get(0);
		showNavPage(notification.getNavPageIndex());
	}
	
	/**
	 * Shows the nav page of the specified index.
	 *
	 * @param navPageIndex
	 */
	public void showNavPage(final int navPageIndex) {
		showPanelContent(navPageIndex);
	}
	
}
