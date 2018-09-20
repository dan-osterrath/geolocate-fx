package net.packsam.geolocatefx.ui;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import net.packsam.geolocatefx.config.Configuration;

/**
 * The modal settings dialog.
 *
 * @author osterrath
 */
public class SettingsDialog implements Initializable {

	/**
	 * Configuration property.
	 */
	private final ObjectProperty<Configuration> configuration = new SimpleObjectProperty<>();

	/**
	 * Root dialog pane.
	 */
	@FXML
	private BorderPane dialogPane;

	/**
	 * Textfield for exiftool location.
	 */
	@FXML
	private TextField exiftoolTF;

	/**
	 * Textfield for Image Magick convert location.
	 */
	@FXML
	private TextField convertTF;

	/**
	 * Text field for Google Maps API.
	 */
	@FXML
	private TextField googleMapsApiKeyTF;

	/**
	 * Called to initialize a controller after its root element has been completely processed.
	 *
	 * @param location
	 * 		The location used to resolve relative paths for the root object, or
	 * 		<tt>null</tt> if the location is not known.
	 * @param resources
	 * 		The resources used to localize the root object, or <tt>null</tt> if
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		configuration.addListener(this::updateConfiguration);
		updateConfiguration(configuration, null, configuration.get());
	}

	/**
	 * Event handler when the value of the configuration property changed
	 *
	 * @param configuration
	 * 		observed property
	 * @param oldVal
	 * 		old value
	 * @param newVal
	 * 		new value
	 */
	private void updateConfiguration(ObservableValue<? extends Configuration> configuration, Configuration oldVal, Configuration newVal) {
		if (oldVal == newVal) {
			return;
		}

		if (newVal == null) {
			exiftoolTF.setText(null);
			convertTF.setText(null);
			googleMapsApiKeyTF.setText(null);
		} else {
			exiftoolTF.setText(newVal.getExiftoolPath());
			convertTF.setText(newVal.getConvertPath());
			googleMapsApiKeyTF.setText(newVal.getGoogleMapsApiKey());
		}
	}

	/**
	 * Event handler when the user clicked the "search exiftool" button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	@FXML
	private void selectExiftool(ActionEvent actionEvent) {
		File exiftool = openSearchDialog("Search exiftool", exiftoolTF.getText());
		if (exiftool != null) {
			exiftoolTF.setText(exiftool.getAbsolutePath());
		}
	}

	/**
	 * Event handler when the user clicked the "search exiftool" button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	@FXML
	private void selectConvert(ActionEvent actionEvent) {
		File convert = openSearchDialog("Search convert", convertTF.getText());
		if (convert != null) {
			convertTF.setText(convert.getAbsolutePath());
		}
	}

	/**
	 * Opens a "search file" dialog.
	 *
	 * @param title
	 * 		dialog title
	 * @param initialFile
	 * 		initial file
	 * @return new file
	 */
	private File openSearchDialog(String title, String initialFile) {
		File initialFolder = null;
		String initialFileName = null;
		if (StringUtils.isNotEmpty(initialFile)) {
			File file = new File(initialFile);
			File folder = file.getParentFile();
			if (folder.exists() && folder.isDirectory() && folder.canRead()) {
				initialFolder = folder;
				if (file.exists() && file.isFile() && file.canRead() && file.canExecute()) {
					initialFileName = file.getName();
				}
			}
		}

		// open file dialog
		FileChooser dialog = new FileChooser();
		dialog.setTitle(title);
		dialog.setInitialDirectory(initialFolder);
		dialog.setInitialFileName(initialFileName);
		return dialog.showOpenDialog(dialogPane.getScene().getWindow());
	}

	/**
	 * Event handler when the user clicked the save button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	@FXML
	private void save(ActionEvent actionEvent) {
		boolean googleMapsKeyChanged = false;

		Configuration configuration = this.configuration.get();
		if (configuration != null) {
			String oldGoogleMapsApiKey = configuration.getGoogleMapsApiKey();

			configuration.setExiftoolPath(StringUtils.trimToNull(exiftoolTF.getText()));
			configuration.setConvertPath(StringUtils.trimToNull(convertTF.getText()));
			configuration.setGoogleMapsApiKey(StringUtils.trimToNull(googleMapsApiKeyTF.getText()));

			googleMapsKeyChanged = !StringUtils.equals(oldGoogleMapsApiKey, configuration.getGoogleMapsApiKey());
		}

		dialogPane.getScene().getWindow().hide();

		if (googleMapsKeyChanged) {
			Alert alert = new Alert(Alert.AlertType.NONE, "You have to restart the application to use the new Google Maps API key", ButtonType.OK);
			alert.setTitle("Information");
			alert.showAndWait();
		}
	}

	/**
	 * Event handler when the user clicked the cancel button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	@FXML
	private void cancel(ActionEvent actionEvent) {
		dialogPane.getScene().getWindow().hide();
	}

	public Configuration getConfiguration() {
		return configuration.get();
	}

	public ObjectProperty<Configuration> configurationProperty() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration.set(configuration);
	}

}
