package net.packsam.geolocatefx;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.packsam.geolocatefx.config.Configuration;
import net.packsam.geolocatefx.config.ConfigurationIO;
import net.packsam.geolocatefx.config.MapSetup;
import net.packsam.geolocatefx.event.DroppedFilesEvent;
import net.packsam.geolocatefx.event.SetGeolocationEvent;
import net.packsam.geolocatefx.model.ApplicationModel;
import net.packsam.geolocatefx.model.ImageModel;
import net.packsam.geolocatefx.model.LatLong;
import net.packsam.geolocatefx.task.CreateThumbnailTask;
import net.packsam.geolocatefx.task.ExternalProcessTask;
import net.packsam.geolocatefx.task.ReadMetaDataAndCreateThumbnailTask;
import net.packsam.geolocatefx.task.ReadMetaDataTask;
import net.packsam.geolocatefx.task.WriteGeolocationTask;
import net.packsam.geolocatefx.ui.ApplicationLayout;
import net.packsam.geolocatefx.ui.ErrorAlert;
import net.packsam.geolocatefx.ui.SettingsDialog;

/**
 * Main application.
 *
 * @author osterrath
 */
public class GeolocateFx extends Application {

	/**
	 * Comparator for sorting selected images.
	 */
	private final static Comparator<ImageModel> IMAGE_COMPARATOR = Comparator
			.comparing(ImageModel::getCreationDate)
			.thenComparing(ImageModel::getImage);

	/**
	 * IO system for loading / writing configuration.
	 */
	private final ConfigurationIO configurationIO;

	/**
	 * Application model.
	 */
	private final ApplicationModel model;

	/**
	 * Executor service for running background tasks.
	 */
	private final ExecutorService backgroundTaskExecutorService;

	/**
	 * Executor service für scheduled tasks.
	 */
	private final ScheduledExecutorService scheduledExecutorService;

	/**
	 * Application configuration.
	 */
	private Configuration configuration;

	/**
	 * Primary stage.
	 */
	private Stage primaryStage;

	/**
	 * Root controller.
	 */
	private ApplicationLayout rootController;

	/**
	 * Flag if the image list has to be sorted.
	 */
	private final MutableBoolean shouldSortImages = new MutableBoolean(false);

	/**
	 * Flag if the images in progress should be counted.
	 */
	private final MutableBoolean shouldCountTasks = new MutableBoolean(false);

	/**
	 * Ctor.
	 */
	public GeolocateFx() {
		configurationIO = new ConfigurationIO();
		model = new ApplicationModel();
		backgroundTaskExecutorService = Executors.newFixedThreadPool(2, new GroupedThreadFactory("background-tasks"));
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new GroupedThreadFactory("scheduled-tasks"));
	}

	/**
	 * The main entry point for all JavaFX applications. The start method is called after the init method has returned, and after the system is ready for the application to
	 * begin running.
	 *
	 * <p>
	 * NOTE: This method is called on the JavaFX Application Thread.
	 * </p>
	 *
	 * @param primaryStage
	 * 		the primary stage for this application, onto which the application scene can be set. The primary stage will be embedded in the browser if the application was launched
	 * 		as an applet. Applications may create other stages, if needed, but they will not be primary stages and will not be embedded in the browser.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;

		try {
			// load configuration
			configuration = configurationIO.readConfiguration();
			MapSetup lastPositionConfiguration = configuration.getLastPosition();
			LatLong lastPosition = null;
			Integer lastZoom = null;
			if (lastPositionConfiguration != null) {
				Double latitude = lastPositionConfiguration.getLatitude();
				Double longitude = lastPositionConfiguration.getLongitude();
				if (latitude != null && longitude != null) {
					lastPosition = new LatLong(latitude, longitude);
				}
				lastZoom = lastPositionConfiguration.getZoom();
			}

			// load FXML
			FXMLLoader rootLoader = new FXMLLoader(ApplicationLayout.class.getResource("ApplicationLayout.fxml").toURI().toURL());
			Parent root = rootLoader.load();

			primaryStage.setScene(new Scene(root, 1280, 900, null));
			primaryStage.initStyle(StageStyle.DECORATED);
			primaryStage.setTitle("GeolocateFX");
			primaryStage.getIcons().addAll(
					new Image(GeolocateFx.class.getResource("AppIcon_256.png").toExternalForm()),
					new Image(GeolocateFx.class.getResource("AppIcon_128.png").toExternalForm()),
					new Image(GeolocateFx.class.getResource("AppIcon_64.png").toExternalForm()),
					new Image(GeolocateFx.class.getResource("AppIcon_32.png").toExternalForm()),
					new Image(GeolocateFx.class.getResource("AppIcon_16.png").toExternalForm()),
					new Image(GeolocateFx.class.getResource("AppIcon.ico").toExternalForm())
			);
			primaryStage.sizeToScene();
			primaryStage.show();

			// bind all together
			rootController = rootLoader.getController();
			rootController.setOnOpenImages(this::onSelectImages);
			rootController.setOnGeolocationSet(this::onGeolocationSet);
			rootController.setOnFilesDropped(this::onFilesDropped);
			rootController.setOnOpenSettings(this::onOpenSettings);
			rootController.imagesProperty().bind(model.selectedImagesProperty());
			rootController.initializeMap(configuration.getGoogleMapsApiKey(), lastPosition, lastZoom);
		} catch (Exception e) {
			Alert alert = new ErrorAlert("Error", "Error while starting GeolocateFX", e);
			alert.showAndWait();
		}

	}

	/**
	 * This method is called when the application should stop, and provides a convenient place to prepare for application exit and destroy resources.
	 *
	 * <p>
	 * The implementation of this method provided by the Application class does nothing.
	 * </p>
	 *
	 * <p>
	 * NOTE: This method is called on the JavaFX Application Thread.
	 * </p>
	 */
	@Override
	public void stop() throws Exception {
		try {
			// stop all tasks
			backgroundTaskExecutorService.shutdownNow();
			scheduledExecutorService.shutdownNow();

			// save last position
			LatLong mapPosition = rootController.getMapPosition();
			if (mapPosition != null) {
				if (configuration.getLastPosition() == null) {
					configuration.setLastPosition(new MapSetup());
				}
				configuration.getLastPosition().setLatitude(mapPosition.getLatitude());
				configuration.getLastPosition().setLongitude(mapPosition.getLongitude());
			}
			Integer mapZoom = rootController.getMapZoom();
			if (mapZoom != null) {
				if (configuration.getLastPosition() == null) {
					configuration.setLastPosition(new MapSetup());
				}
				configuration.getLastPosition().setZoom(mapZoom);
			}

			// write configuration
			configurationIO.writeConfiguration(configuration);
		} catch (Exception e) {
			Alert alert = new ErrorAlert("Error", "Error while shutting down GeolocateFX", e);
			alert.showAndWait();
		}

		super.stop();
	}

	/**
	 * Event handler for the "select images" button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	private void onSelectImages(ActionEvent actionEvent) {
		// open file dialog
		FileChooser dialog = new FileChooser();
		dialog.setTitle("Select images");
		if (StringUtils.isNotEmpty(configuration.getLastImagePath())) {
			dialog.setInitialDirectory(new File(configuration.getLastImagePath()));
		}
		dialog.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("All images / videos", "*.jpg; *.jpeg; *.png; *.tif; *.tiff; *.dng; *.raw; *.cr2; *.cr3; *.nef; *.nrw; *.arw; *.srf; *.sr2; *.srw; *.psd; *.mp4; *.mov; *.m2ts; *.avi"),
				new FileChooser.ExtensionFilter("JPEG images", "*.jpg; *.jpeg"),
				new FileChooser.ExtensionFilter("PNG images", "*.png"),
				new FileChooser.ExtensionFilter("TIFF images", "*.tif; *.tiff"),
				new FileChooser.ExtensionFilter("Canon RAW images", "*.raw; *.cr2; *.cr3"),
				new FileChooser.ExtensionFilter("Nikon RAW images", "*.nef; *.nrw"),
				new FileChooser.ExtensionFilter("Sony RAW images", "*.arw; *.srf; *.sr2"),
				new FileChooser.ExtensionFilter("Samsung RAW images", "*.raw; *.srw"),
				new FileChooser.ExtensionFilter("Adobe Photoshop images", "*.psd"),
				new FileChooser.ExtensionFilter("Videos", "*.mp4; *.mov; *.m2ts; *.avi"),
				new FileChooser.ExtensionFilter("All files", "*.*")
		);
		List<File> files = dialog.showOpenMultipleDialog(primaryStage);

		if (files != null && !files.isEmpty()) {
			addImages(files);

			String imageDirectory = files.get(0).getAbsoluteFile().getParent();
			configuration.setLastImagePath(imageDirectory);
		}
	}

	/**
	 * Event handler when a geo location should be set.
	 *
	 * @param event
	 * 		event
	 */
	private void onGeolocationSet(SetGeolocationEvent event) {
		List<File> images = event.getImages();
		if (images == null || images.isEmpty()) {
			return;
		}

		List<ImageModel> imageModels = model.getSelectedImages().stream()
				.filter(im -> images.contains(im.getImage()))
				.collect(Collectors.toList());

		scheduleTask(new WriteGeolocationTask(configuration.getExiftoolPath(), event.getGeolocation(), imageModels));
	}

	/**
	 * Event handler when the user dropped files from external application.
	 *
	 * @param event
	 * 		event
	 */
	private void onFilesDropped(DroppedFilesEvent event) {
		addImages(event.getFiles(), event.getGeolocation());
	}

	/**
	 * Adds the given images to the list of selected images.
	 *
	 * @param imageFiles
	 * 		image file to add
	 */
	private void addImages(List<File> imageFiles) {
		addImages(imageFiles, null);
	}

	/**
	 * Adds the given images to the list of selected images.
	 *
	 * @param imageFiles
	 * 		image files to add
	 * @param newGeolocation
	 * 		optional new geolocation to write to file
	 */
	private void addImages(List<File> imageFiles, LatLong newGeolocation) {
		if (imageFiles == null || imageFiles.isEmpty()) {
			return;
		}

		ObservableList<ImageModel> selectedImages = model.getSelectedImages();

		// filter only new files
		Set<File> existingFiles = selectedImages.stream()
				.map(ImageModel::getImage)
				.collect(Collectors.toSet());
		List<File> newImageFiles = imageFiles.stream()
				.filter(((Predicate<File>) existingFiles::contains).negate())
				.collect(Collectors.toList());

		// handle images
		List<ImageModel> imageIMs = newImageFiles.stream()
				.filter(this::isImage)
				.map(this::createImageModel)
				.collect(Collectors.toList());

		if (!imageIMs.isEmpty()) {
			imageIMs.forEach(im -> scheduleTask(new CreateThumbnailTask(configuration.getConvertPath(), im)));
			scheduleTask(new ReadMetaDataTask(configuration.getExiftoolPath(), imageIMs));
		}

		// handle videos
		List<ImageModel> videoIMs = newImageFiles.stream()
				.filter(this::isVideo)
				.map(this::createImageModel)
				.collect(Collectors.toList());

		if (!videoIMs.isEmpty()) {
			videoIMs.forEach(im -> scheduleTask(new ReadMetaDataAndCreateThumbnailTask(configuration.getExiftoolPath(), configuration.getConvertPath(), im)));
		}

		ArrayList<ImageModel> allIMs = new ArrayList<>();
		allIMs.addAll(imageIMs);
		allIMs.addAll(videoIMs);

		if (newGeolocation != null && !allIMs.isEmpty()) {
			scheduleTask(new WriteGeolocationTask(configuration.getExiftoolPath(), newGeolocation, allIMs));
		}

		selectedImages.addAll(allIMs);
	}

	/**
	 * Creates an image model for the given file.
	 *
	 * @param file
	 * 		file
	 * @return new image model
	 */
	private ImageModel createImageModel(File file) {
		ImageModel imageModel = new ImageModel();
		imageModel.setImage(file);
		imageModel.setCreationDate(new Date(file.lastModified()));
		try {
			imageModel.setCreationDate(new Date(Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis()));
		} catch (Exception e) {
		}

		imageModel.creationDateProperty().addListener(this::onImageModelChanged);
		imageModel.fileInProgressProperty().addListener(this::onImageModelInProgress);

		return imageModel;
	}

	/**
	 * Checks if the given file is an image.
	 *
	 * @param file
	 * 		file
	 * @return <code>true</code> if this is an image
	 */
	private boolean isImage(File file) {
		String extension = FilenameUtils.getExtension(file.getName());
		return StringUtils.equalsAnyIgnoreCase(extension, "jpg", "jpeg", "png", "tif", "tiff", "dng", "raw", "cr2", "cr3", "nef", "nrw", "arw", "srf", "sr2", "srw", "psd");
	}

	/**
	 * Checks if the given file is a video.
	 *
	 * @param file
	 * 		file
	 * @return <code>true</code> if this is a video
	 */
	private boolean isVideo(File file) {
		String extension = FilenameUtils.getExtension(file.getName());
		return StringUtils.equalsAnyIgnoreCase(extension, "mp4", "mov", "m2ts", "avi");
	}

	/**
	 * Opens the settings dialog.
	 *
	 * @param actionEvent
	 * 		event
	 */
	private void onOpenSettings(ActionEvent actionEvent) {
		try {
			FXMLLoader dialogLoader = new FXMLLoader(SettingsDialog.class.getResource("SettingsDialog.fxml").toURI().toURL());
			Parent dialog = dialogLoader.load();

			Scene dialogScene = new Scene(dialog);
			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			dialogStage.setTitle("Settings");
			dialogStage.initStyle(StageStyle.UTILITY);
			dialogStage.initOwner(primaryStage.getOwner());
			dialogStage.setScene(dialogScene);

			// bind all together
			SettingsDialog dialogController = dialogLoader.getController();
			dialogController.setConfiguration(configuration);

			dialogStage.showAndWait();

		} catch (Exception e) {
			Alert alert = new ErrorAlert("Error", "Could not open settings dialog", e);
			alert.showAndWait();
		}
	}

	private void scheduleOncePerSecond(MutableBoolean syncLock, Runnable command) {
		synchronized (syncLock) {
			if (syncLock.isTrue()) {
				// already scheduled
				return;
			}
			syncLock.setTrue();
			scheduledExecutorService.schedule(command, 1, TimeUnit.SECONDS);
		}
	}

	/**
	 * Event handler when the image model data has been changed.
	 *
	 * @param observable
	 * 		observable
	 */
	private void onImageModelChanged(Observable observable) {
		scheduleOncePerSecond(shouldSortImages, this::sortImages);
	}

	/**
	 * Sorts the image list.
	 */
	private void sortImages() {
		shouldSortImages.setFalse();
		Platform.runLater(() -> model.selectedImagesProperty().sort(IMAGE_COMPARATOR));
	}

	/**
	 * Event handler when the progress flag of the image model has been changed.
	 *
	 * @param observable
	 * 		observable
	 */
	private void onImageModelInProgress(Observable observable) {
		scheduleOncePerSecond(shouldCountTasks, this::countImagesInProgress);
	}

	/**
	 * Counts the model images that are in progress.
	 */
	private void countImagesInProgress() {
		shouldCountTasks.setFalse();
		long imagesInProgress = model.selectedImagesProperty().stream()
				.filter(ImageModel::isFileInProgress)
				.count();
		Platform.runLater(() -> rootController.setBackgroundTaskCount(imagesInProgress));
	}

	/**
	 * Adds the given task to the internal scheduler and adds an error handler.
	 *
	 * @param task
	 * 		task to execute
	 */
	private void scheduleTask(ExternalProcessTask<?> task) {
		task.setErrorHandler(this::handleTaskError);
		backgroundTaskExecutorService.submit(task);
	}

	/**
	 * Handles an erroneous exit code from task
	 *
	 * @param processName
	 * 		process name that has been executed
	 * @param exitCode
	 * 		exit code
	 * @param error
	 * 		optional output from STDERR
	 * @param e
	 * 		optional exception when starting process
	 */
	private void handleTaskError(String processName, int exitCode, String error, Exception e) {
		Platform.runLater(() -> {
			Alert alert;
			if (e != null) {
				alert = new ErrorAlert(
						"Error",
						processName + " could not be started",
						e
				);
			} else {
				alert = new ErrorAlert(
						"Error",
						processName + " exited with error code " + exitCode,
						error
				);
			}
			alert.showAndWait();
		});
	}

	/**
	 * Main method.
	 *
	 * @param args
	 * 		command line args
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Internal thread factory that groups all created threads in an own thread group and sets the daemon flag to true.
	 *
	 * @author osterrath
	 */
	private class GroupedThreadFactory implements ThreadFactory {
		/**
		 * Thread group name.
		 */
		private final String threadGroupName;

		/**
		 * Thread group.
		 */
		private final ThreadGroup threadGroup;

		/**
		 * Counter for created threads.
		 */
		private long threadCounter = 0;

		/**
		 * Ctor.
		 *
		 * @param threadGroupName
		 * 		thread group name.
		 */
		private GroupedThreadFactory(String threadGroupName) {
			this.threadGroupName = threadGroupName;
			threadGroup = new ThreadGroup(threadGroupName);
		}

		/**
		 * Constructs a new {@code Thread}.  Implementations may also initialize priority, name, daemon status, {@code ThreadGroup}, etc.
		 *
		 * @param r
		 * 		a runnable to be executed by new thread instance
		 * @return constructed thread, or {@code null} if the request to create a thread is rejected
		 */
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(threadGroup, r, threadGroupName + "-" + (threadCounter++));
			thread.setDaemon(true);
			return thread;
		}
	}

}
