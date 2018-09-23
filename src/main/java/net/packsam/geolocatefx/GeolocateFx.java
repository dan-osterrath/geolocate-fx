package net.packsam.geolocatefx;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javafx.application.Application;
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
	private final ExecutorService es;

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
	 * Ctor.
	 */
	public GeolocateFx() {
		configurationIO = new ConfigurationIO();
		model = new ApplicationModel();
		es = Executors.newFixedThreadPool(2);
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
			es.shutdownNow();

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
			files.forEach(this::addImage);

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

		es.submit(new WriteGeolocationTask(configuration.getExiftoolPath(), event.getGeolocation(), imageModels));
	}

	/**
	 * Event handler when the user dropped files from external application.
	 *
	 * @param event
	 * 		event
	 */
	private void onFilesDropped(DroppedFilesEvent event) {
		LatLong geolocation = event.getGeolocation();
		if (geolocation == null) {
			// only add to images list
			event.getFiles().forEach(this::addImage);
		} else {
			// add to images list and immediately set geo location
			event.getFiles().forEach(i -> addImage(i, geolocation));
		}
	}

	/**
	 * Adds the given image to the list of selected images.
	 *
	 * @param imageFile
	 * 		image file to add
	 */
	private void addImage(File imageFile) {
		addImage(imageFile, null);
	}

	/**
	 * Adds the given image to the list of selected images.
	 *
	 * @param imageFile
	 * 		image file to add
	 * @param newGeolocation
	 * 		optional new geolocation to write to file
	 */
	private void addImage(File imageFile, LatLong newGeolocation) {
		if (imageFile == null) {
			return;
		}
		ObservableList<ImageModel> selectedImages = model.getSelectedImages();
		boolean alreadyAdded = selectedImages.stream()
				.map(ImageModel::getImage)
				.anyMatch(imageFile::equals);
		if (alreadyAdded) {
			return;
		}

		// check if this is an image or video
		String extension = FilenameUtils.getExtension(imageFile.getName());
		boolean isImage = StringUtils.equalsAnyIgnoreCase(extension, "jpg", "jpeg", "png", "tif", "tiff", "dng", "raw", "cr2", "cr3", "nef", "nrw", "arw", "srf", "sr2", "srw", "psd");
		boolean isVideo = StringUtils.equalsAnyIgnoreCase(extension, "mp4", "mov", "m2ts", "avi");

		if (!isImage && !isVideo) {
			return;
		}

		ImageModel imageModel = new ImageModel();
		imageModel.setImage(imageFile);

		if (isVideo) {
			es.submit(new ReadMetaDataAndCreateThumbnailTask(configuration.getExiftoolPath(), configuration.getConvertPath(), imageModel));
		} else {
			es.submit(new CreateThumbnailTask(configuration.getConvertPath(), imageModel));
			es.submit(new ReadMetaDataTask(configuration.getExiftoolPath(), imageModel));
		}
		if (newGeolocation != null) {
			es.submit(new WriteGeolocationTask(configuration.getExiftoolPath(), newGeolocation, Collections.singletonList(imageModel)));
		}

		selectedImages.add(imageModel);
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

	/**
	 * Main method.
	 *
	 * @param args
	 * 		command line args
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
