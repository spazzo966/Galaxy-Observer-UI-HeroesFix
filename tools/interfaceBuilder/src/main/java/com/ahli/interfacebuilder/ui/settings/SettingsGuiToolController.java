// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.interfacebuilder.ui.settings;

import com.ahli.interfacebuilder.config.ConfigService;
import com.ahli.interfacebuilder.integration.SettingsIniInterface;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;

public class SettingsGuiToolController extends SettingsAutoSaveController {
	
	@FXML
	private CheckBox buildUnprotectedToo;
	@FXML
	private RadioButton compressMpqBlizz;
	@FXML
	private RadioButton compressMpqNone;
	@FXML
	private RadioButton compressMpqExperimentalBest;
	@FXML
	private RadioButton compressMpqSystemDefault;
	@FXML
	private CheckBox compressXml;
	@FXML
	private CheckBox checkLayout;
	@FXML
	private CheckBox repairLayoutOrder;
	@FXML
	private CheckBox checkXml;
	
	public SettingsGuiToolController(final ConfigService configService) {
		super(configService);
	}
	
	/**
	 * Automatically called by FxmlLoader
	 */
	@Override
	public void initialize() {
		super.initialize();
		loadValuesFromSettings();
	}
	
	private void loadValuesFromSettings() {
		final SettingsIniInterface settings = configService.getIniSettings();
		checkXml.setSelected(settings.isGuiVerifyXml());
		checkLayout.setSelected(settings.isGuiVerifyLayout());
		repairLayoutOrder.setSelected(settings.isGuiRepairLayoutOrder());
		compressXml.setSelected(settings.isGuiCompressXml());
		initCompressMpq(settings.getGuiCompressMpq());
		buildUnprotectedToo.setSelected(settings.isGuiBuildUnprotectedToo());
	}
	
	/**
	 * Applies the selected mode to the radio buttons.
	 *
	 * @param val
	 * 		selected mpq compression mode
	 */
	private void initCompressMpq(final int val) {
		switch (val) {
			case 1 -> {
				compressMpqNone.setSelected(false);
				compressMpqBlizz.setSelected(true);
				compressMpqExperimentalBest.setSelected(false);
				compressMpqSystemDefault.setSelected(false);
			}
			case 2 -> {
				compressMpqNone.setSelected(false);
				compressMpqBlizz.setSelected(false);
				compressMpqExperimentalBest.setSelected(true);
				compressMpqSystemDefault.setSelected(false);
			}
			case 3 -> {
				compressMpqNone.setSelected(false);
				compressMpqBlizz.setSelected(false);
				compressMpqExperimentalBest.setSelected(false);
				compressMpqSystemDefault.setSelected(true);
			}
			default -> {
				compressMpqNone.setSelected(true);
				compressMpqBlizz.setSelected(false);
				compressMpqExperimentalBest.setSelected(false);
				compressMpqSystemDefault.setSelected(false);
			}
		}
	}
	
	@Override
	public void update() {
		super.update();
		loadValuesFromSettings();
	}
	
	@FXML
	public void onCheckXmlClick(final ActionEvent actionEvent) {
		final boolean val = ((CheckBox) actionEvent.getSource()).selectedProperty().getValue();
		configService.getIniSettings().setGuiVerifyXml(val);
		persistSettingsIni();
	}
	
	@FXML
	public void onCheckLayoutClick(final ActionEvent actionEvent) {
		final boolean val = ((CheckBox) actionEvent.getSource()).selectedProperty().getValue();
		configService.getIniSettings().setGuiVerifyLayout(val);
		persistSettingsIni();
	}
	
	@FXML
	public void onRepairLayoutOrderClick(final ActionEvent actionEvent) {
		final boolean val = ((CheckBox) actionEvent.getSource()).selectedProperty().getValue();
		configService.getIniSettings().setGuiRepairLayoutOrder(val);
		persistSettingsIni();
	}
	
	@FXML
	public void onCompressXmlClick(final ActionEvent actionEvent) {
		final boolean val = ((CheckBox) actionEvent.getSource()).selectedProperty().getValue();
		configService.getIniSettings().setGuiCompressXml(val);
		persistSettingsIni();
	}
	
	@FXML
	public void onCompressMpqNoneClick() {
		clickedCompressionMode(0);
	}
	
	/**
	 * @param i
	 */
	private void clickedCompressionMode(final int i) {
		configService.getIniSettings().setGuiCompressMpq(i);
		persistSettingsIni();
		initCompressMpq(i);
	}
	
	@FXML
	public void onCompressMpqBlizzClick() {
		clickedCompressionMode(1);
	}
	
	@FXML
	public void onCompressMpqExperimentalBestClick() {
		clickedCompressionMode(2);
	}
	
	@FXML
	public void onCompressMpqSystemDefaultClick() {
		clickedCompressionMode(3);
	}
	
	@FXML
	public void onBuildUnprotectedTooClick(final ActionEvent actionEvent) {
		final boolean val = ((CheckBox) actionEvent.getSource()).selectedProperty().getValue();
		configService.getIniSettings().setGuiBuildUnprotectedToo(val);
		persistSettingsIni();
	}
}
