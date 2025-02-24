// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.interfacebuilder;

import com.ahli.interfacebuilder.config.AppConfiguration;
import com.ahli.interfacebuilder.config.FxmlConfiguration;
import com.ahli.interfacebuilder.integration.CommandLineParams;
import com.ahli.interfacebuilder.integration.ipc.IpcCommunication;
import com.ahli.interfacebuilder.integration.ipc.IpcServerThread;
import com.ahli.interfacebuilder.integration.ipc.TcpIpSocketCommunication;
import com.ahli.interfacebuilder.integration.ipc.UnixDomainSocketCommunication;
import javafx.application.Application;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static com.ahli.interfacebuilder.ui.AppController.FATAL_ERROR;

// start with VM parameter if java modules are used: -add-opens=javafx.controls/javafx.scene.control=interfacex.builder

@Log4j2
@Import({ AppConfiguration.class, FxmlConfiguration.class, ConfigurationPropertiesAutoConfiguration.class,
		MessageSourceAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class, FlywayAutoConfiguration.class })
public final class SpringBootApplication {
	
	public static final int INTER_PROCESS_COMMUNICATION_PORT = 12317;
	public static final String DB_FILE_NAME = "DB.mv.db";
	public static final String DB_TRACE_FILE_NAME = "DB.trace.db";
	private static final boolean USE_DOMAIN_SOCKET = true;
	
	public SpringBootApplication() {
		// explicit constructor
	}
	
	@SuppressWarnings("java:S2095") //
	public static void main(final String[] args) {
		try {
			if (getIpc().isAvailable()) {
				if (!actAsServer(args)) {
					log.warn("Failed to create Server Thread. Starting App without it...");
					launch(args, null);
				}
			} else {
				log.info("App already running. Passing over command line arguments.");
				actAsClient(args);
			}
		} catch (final InterruptedException e) {
			log.error("Interrupted server/client entry point", e);
			Thread.currentThread().interrupt();
		} catch (final Exception e) {
			log.error(FATAL_ERROR, e);
		}
	}
	
	private static void actAsClient(final String[] args) {
		try (final IpcCommunication interProcessCommunication = getIpc()) {
			if (!interProcessCommunication.sendToServer(args)) {
				log.error("InterProcessCommunication as Client failed.");
			}
		} catch (final Exception e) {
			log.error("ClosingInterProcessCommunication as Client failed.", e);
		}
	}
	
	private static IpcCommunication getIpc() {
		return USE_DOMAIN_SOCKET ? new UnixDomainSocketCommunication(getIpcPath()) :
				new TcpIpSocketCommunication(INTER_PROCESS_COMMUNICATION_PORT);
	}
	
	private static boolean actAsServer(final String[] args) {
		try (final IpcCommunication interProcessCommunication = getIpc()) {
			final IpcServerThread serverThread = interProcessCommunication.actAsServer();
			launch(args, serverThread);
			return true;
		} catch (final InterruptedException e) {
			log.error("Interrupted during H2 DB migration check", e);
			Thread.currentThread().interrupt();
		} catch (final IOException e) {
			log.error("H2 DB migration check failed", e);
		} catch (final Exception e) {
			log.error("Closing InterProcessCommunication failed", e);
		}
		return false;
	}
	
	private static Path getIpcPath() {
		return Path.of(
				System.getProperty("user.home") + File.separator + ".GalaxyObsUI" + File.separator + "ipc.socket");
	}
	
	private static void launch(final String[] args, @Nullable final IpcServerThread serverThread)
			throws IOException, InterruptedException {
		
		log.trace("System's Log4j2 Configuration File: {}", () -> System.getProperty("log4j.configurationFile"));
		log.info(
				"Launch arguments: {}\nMax Heap Space: {}mb.",
				() -> Arrays.toString(args),
				() -> Runtime.getRuntime().maxMemory() / 1_048_576L);
		
		migrateH2Db();
		
		boolean noGui = false;
		for (final String arg : args) {
			if ("--noGUI".equalsIgnoreCase(arg)) {
				noGui = true;
				break;
			}
		}
		if (noGui) {
			launchNoGui(args, serverThread);
		} else {
			launchGui(args, serverThread);
		}
	}
	
	/**
	 * Writes exportMigrate.zip and deletes the database files. The zip archive should be loaded by the DB migration.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void migrateH2Db() throws IOException, InterruptedException {
		final Path dbDir = getDbDir();
		int oldVersion = oldH2DbFilePresent();
		switch (oldVersion) {
			case 1 -> {
				backupH2Db("1.4.200", dbDir);
				Files.deleteIfExists(dbDir.resolve(DB_FILE_NAME));
				Files.deleteIfExists(dbDir.resolve(DB_TRACE_FILE_NAME));
			}
			case 2 -> {
				backupH2Db("2.1.214", dbDir);
				Files.deleteIfExists(dbDir.resolve(DB_FILE_NAME));
				Files.deleteIfExists(dbDir.resolve(DB_TRACE_FILE_NAME));
			}
			default -> Files.deleteIfExists(dbDir.resolve("exportMigrate.zip"));
		}
	}
	
	private static int oldH2DbFilePresent() {
		final Path dbPath = getDbDir().resolve(DB_FILE_NAME);
		if (Files.exists(dbPath)) {
			try (final BufferedReader brTest = Files.newBufferedReader(dbPath)) {
				final String firstLine = brTest.readLine();
				if (firstLine.contains(",format:1,")) {
					return 1;
				}
				if (firstLine.contains(",format:2,")) {
					return 2;
				}
			} catch (final IOException e) {
				log.error("Failed to read H2 DB file to determine version.");
			}
		}
		return 0;
	}
	
	private static Path getDbDir() {
		return Path.of(System.getProperty("user.home"), ".GalaxyObsUI", "database");
	}
	
	private static void backupH2Db(final String h2Version, final Path dbDir) throws IOException, InterruptedException {
		final Path h2JarFile = dbDir.resolve("h2-" + h2Version + ".jar");
		if (!Files.exists(h2JarFile)) {
			// download e.g. https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar
			final URI uri = URI.create(
					"https://repo1.maven.org/maven2/com/h2database/h2/" + h2Version + "/h2-" + h2Version + ".jar");
			try (final ReadableByteChannel readableByteChannel = Channels.newChannel(uri.toURL().openStream());
			     final FileOutputStream fileOutputStream = new FileOutputStream(h2JarFile.toFile())) {
				fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			}
		}
		
		// java -cp "h2-1.4.200.jar" org.h2.tools.Script -url jdbc:h2:~/test -user sa -script test.zip -options compression zip
		final String exportZipFile = getDbDir() + File.separator + "exportMigrate.zip";
		
		final String[] cmd = new String[] { "java", "-cp", h2JarFile.toString(), "org.h2.tools.Script", "-url",
				"jdbc:h2:file:~/.GalaxyObsUI/database/DB", "-user", "sa", /*"-password", "",*/ "-script", exportZipFile,
				"-options", "compression", "zip" };
		
		if (log.isTraceEnabled()) {
			log.trace("executing: {}", Arrays.toString(cmd));
		}
		Runtime.getRuntime().exec(cmd).waitFor();
		
		Files.deleteIfExists(h2JarFile);
	}
	
	private static void launchNoGui(final String[] args, @Nullable final IpcServerThread serverThread) {
		new NoGuiApplication(args, serverThread);
	}
	
	private static void launchGui(final String[] args, @Nullable final IpcServerThread serverThread) {
		setJavaFxPreloader(AppPreloader.class.getCanonicalName());
		Application.launch(JavafxApplication.class, argsWithServerThread(args, serverThread));
	}
	
	private static void setJavaFxPreloader(final String canonicalPath) {
		System.setProperty("javafx.preloader", canonicalPath);
	}
	
	private static String[] argsWithServerThread(final String[] args, @Nullable final IpcServerThread serverThread) {
		if (serverThread != null) {
			final String[] destArray = Arrays.copyOf(args, args.length + 1);
			destArray[destArray.length - 1] =
					CommandLineParams.PARAM_PREFIX + CommandLineParams.SERVER + CommandLineParams.EQUAL +
							serverThread.getId();
			return destArray;
		}
		return args;
	}
	
}
