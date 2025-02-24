package com.ahli.hotkey_ui.application.galaxy.ext;

import com.ahli.hotkey_ui.application.model.Constants;
import com.ahli.hotkey_ui.application.model.OptionValueDef;
import com.ahli.hotkey_ui.application.model.OptionValueDefType;
import com.ahli.hotkey_ui.application.model.abstracts.ValueDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

public class GameStringsUpdater extends SimpleFileVisitor<Path> {
	
	private static final Logger logger = LoggerFactory.getLogger(GameStringsUpdater.class);
	
	private final List<OptionValueDef> gamestringsAddSettings;
	
	public GameStringsUpdater(final List<OptionValueDef> gamestringsAddSettings) {
		this.gamestringsAddSettings = gamestringsAddSettings;
	}
	
	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
		
		if (file.getFileName().toString().equalsIgnoreCase("GameStrings.txt")) {
			
			logger.debug("processing file: {}", file);
			
			StringBuilder gamestrings = new StringBuilder(Files.readString(file, StandardCharsets.UTF_8));
			
			boolean changed = false;
			for (final ValueDef setting : gamestringsAddSettings) {
				if (setting instanceof OptionValueDef ovd && ovd.getType() == OptionValueDefType.BOOLEAN) {
					if (Objects.equals(ovd.getSelectedValue(), Constants.TRUE)) {
						if (ovd.getGamestringsAdd().charAt(0) != '\n') {
							gamestrings.append('\n');
						}
						gamestrings.append(ovd.getGamestringsAdd());
						changed = true;
					} else if (Objects.equals(ovd.getSelectedValue(), Constants.FALSE)) {
						gamestrings = new StringBuilder(gamestrings.toString()
								.replaceAll("[\n\r]?" + ovd.getGamestringsAdd(), ""));
						changed = true;
					}
				}
			}
			
			if (changed) {
				Files.writeString(file, gamestrings, StandardCharsets.UTF_8);
			}
		}
		
		return FileVisitResult.CONTINUE;
	}
	
}
