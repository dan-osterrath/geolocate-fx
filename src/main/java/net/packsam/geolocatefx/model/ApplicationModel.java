package net.packsam.geolocatefx.model;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Wrapper class for the complete application model.
 *
 * @author osterrath
 */
public class ApplicationModel {
	/**
	 * Property for the selected images.
	 */
	private final ListProperty<ImageModel> selectedImages = new SimpleListProperty<>(FXCollections.observableArrayList());

	public ObservableList<ImageModel> getSelectedImages() {
		return selectedImages.get();
	}

	public ListProperty<ImageModel> selectedImagesProperty() {
		return selectedImages;
	}

	public void setSelectedImages(ObservableList<ImageModel> selectedImages) {
		this.selectedImages.set(selectedImages);
	}
}
