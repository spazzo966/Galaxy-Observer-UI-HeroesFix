// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.interfacebuilder.integration.log4j;

import com.ahli.interfacebuilder.ui.progress.ErrorTabController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * TextAreaAppender for Log4j2. <a href="http://blog.pikodat.com/2015/10/11/frontend-logging-with-javafx/">initial
 * Source</a>, modified for org.fxmisc.richtext.StyleClassedTextArea: Ahli
 * <p>
 * If this Appender does not work, then the Log4j2Plugins.dat might not have been created.
 */
@Plugin(name = "StylizedTextAreaAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public final class StylizedTextAreaAppender extends AbstractAppender {
	private static final Map<String, ErrorTabController> workerTaskControllers = new UnifiedMap<>(4);
	private static ErrorTabController generalController;
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();
	
	/**
	 * @param name
	 * @param filter
	 * @param layout
	 * @param ignoreExceptions
	 */
	private StylizedTextAreaAppender(
			final String name,
			final Filter filter,
			final Layout<? extends Serializable> layout,
			final boolean ignoreExceptions,
			final Property... properties) {
		super(name, filter, layout, ignoreExceptions, properties);
	}
	
	/**
	 * Factory method. Log4j will parse the configuration and call this factory method to construct the appender with
	 * the configured attributes.
	 *
	 * @param name
	 * 		Name of appender
	 * @param layout
	 * 		Log layout of appender
	 * @param filter
	 * 		Filter for appender
	 * @return The TextAreaAppender
	 */
	@PluginFactory
	public static StylizedTextAreaAppender createAppender(
			@PluginAttribute("name") final String name,
			@PluginElement("Layout") final Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter) {
		if (name == null) {
			LOGGER.error("No name provided for StylizedTextAreaAppender");
			return null;
		}
		final var resultLayout = layout == null ? PatternLayout.createDefaultLayout() : layout;
		return new StylizedTextAreaAppender(name, filter, resultLayout, true, Property.EMPTY_ARRAY);
	}
	
	/**
	 * @param controller
	 */
	public static void setGeneralController(final ErrorTabController controller) {
		StylizedTextAreaAppender.generalController = controller;
	}
	
	/**
	 * @param controller
	 * @param threadName
	 */
	public static void setWorkerTaskController(final ErrorTabController controller, final String threadName) {
		getStatusLogger().trace("registering error tab controller for thread: {}", threadName);
		workerTaskControllers.put(threadName, controller);
	}
	
	/**
	 * Sets the thread's ErrorTabController to finished and unregisters it.
	 *
	 * @param threadName
	 * @param unregister
	 * @param delayInMs
	 */
	public static void finishedWork(final String threadName, final boolean unregister, final long delayInMs) {
		final ErrorTabController ctrl = getWorkerTaskController(threadName);
		if (ctrl != generalController) {
			Platform.runLater(() -> {
				try {
					ctrl.setRunning(false);
				} catch (final Exception e) {
					System.err.println("Error while cleaning up: " + e.getMessage());
				}
			});
		}
		if (unregister) {
			if (delayInMs > 0) {
				final var ctrlToRemove = workerTaskControllers.get(threadName);
				Platform.runLater(() -> {
					try {
						Thread.sleep(delayInMs);
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					// ensure that the correct one is removed
					if (ctrlToRemove == workerTaskControllers.get(threadName)) {
						workerTaskControllers.remove(threadName);
					}
				});
			} else {
				workerTaskControllers.remove(threadName);
			}
		}
	}
	
	/**
	 * @param threadName
	 * @return
	 */
	private static ErrorTabController getWorkerTaskController(final String threadName) {
		return workerTaskControllers.getOrDefault(threadName, generalController);
	}
	
	/**
	 * Unregisters a specified ErrorTabController.
	 */
	public static void unregister(final ErrorTabController controller) {
		final var keys = workerTaskControllers.keySet().toArray(new String[0]);
		for (final String key : keys) {
			final ErrorTabController curController = workerTaskControllers.get(key);
			if (curController == controller) {
				getStatusLogger().trace("unregistering error tab controller for thread: {}", key);
				workerTaskControllers.remove(key);
			}
		}
	}
	
	/**
	 * This method is where the appender does the work.
	 *
	 * @param event
	 * 		Log event with log data
	 */
	@Override
	public void append(final LogEvent event) {
		readLock.lock();
		
		// append log text to TextArea
		try {
			final ErrorTabController controller = getWorkerTaskController(event.getThreadName());
			if (controller != null) {
				final TextFlow txtArea = controller.getTextArea();
				final String message = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8);
				final Level level = event.getLevel();
				
				Platform.runLater(() -> {
					try {
						final Text text = new Text(message);
						text.getStyleClass().add(level.toString());
						text.setFontSmoothingType(FontSmoothingType.LCD);
						final ObservableList<Node> children = txtArea.getChildren();
						children.add(text);
						
						if (level == Level.ERROR || level == Level.FATAL) {
							controller.reportError();
						} else if (level == Level.WARN) {
							controller.reportWarning();
						}
						
						if (children.size() > 2000) {
							children.remove(0);
						}
					} catch (final Exception e) {
						System.err.println("Error while append to TextArea: " + e.getMessage());
					}
				});
			}
		} catch (final IllegalStateException ex) {
			ex.printStackTrace();
		} finally {
			readLock.unlock();
		}
	}
}
