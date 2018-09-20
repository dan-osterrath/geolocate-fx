package net.packsam.geolocatefx.ui;

import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import net.packsam.geolocatefx.event.SetGeolocationEvent;
import net.packsam.geolocatefx.model.ImageModel;

/**
 * List view for the image thumbnails.
 *
 * @author osterrath
 */
public class ImageThumbnailList extends ListView<ImageModel> {
	/**
	 * Event handler when the user started dragging an image.
	 */
	private EventHandler<MouseEvent> onDragImageDetected;

	/**
	 * Event handler when a geolocation has been set.
	 */
	private EventHandler<SetGeolocationEvent> onGeolocationSet;

	/**
	 * Ctor.
	 */
	public ImageThumbnailList() {
		super();
		setCellFactory(p -> new ListCell<ImageModel>() {
			@Override
			protected void updateItem(ImageModel item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(createImage(item));
				}
			}
		});
		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	/**
	 * Creates a new image to the image container.
	 *
	 * @param imageModel
	 * 		image model for image
	 */
	private ImageThumbnail createImage(ImageModel imageModel) {
		ImageThumbnail imageThumbnail = new ImageThumbnail(imageModel);
		imageThumbnail.setOnDragDetected(this::dragDetectedOnImage);
		imageThumbnail.setOnGeolocationSet(this::geolocationSetForImages);
		return imageThumbnail;
	}

	/**
	 * Event handler when the user started dragging an image.
	 *
	 * @param mouseEvent
	 * 		mouse event
	 */
	private void dragDetectedOnImage(MouseEvent mouseEvent) {
		if (onDragImageDetected != null) {
			onDragImageDetected.handle(mouseEvent);
		}
	}

	/**
	 * Event handler when the user set geolocations for some images.
	 *
	 * @param setGeolocationEvent
	 * 		event
	 */
	private void geolocationSetForImages(SetGeolocationEvent setGeolocationEvent) {
		if (onGeolocationSet != null) {
			onGeolocationSet.handle(setGeolocationEvent);
		}
	}

	/**
	 * Returns the onDragImageDetected.
	 *
	 * @return onDragImageDetected
	 */
	public EventHandler<MouseEvent> getOnDragImageDetected() {
		return onDragImageDetected;
	}

	/**
	 * Sets the onDragImageDetected.
	 *
	 * @param onDragImageDetected
	 * 		new value for onDragImageDetected
	 */
	public void setOnDragImageDetected(EventHandler<MouseEvent> onDragImageDetected) {
		this.onDragImageDetected = onDragImageDetected;
	}

	/**
	 * Returns the onGeolocationSet.
	 *
	 * @return onGeolocationSet
	 */
	public EventHandler<SetGeolocationEvent> getOnGeolocationSet() {
		return onGeolocationSet;
	}

	/**
	 * Sets the onGeolocationSet.
	 *
	 * @param onGeolocationSet
	 * 		new value for onGeolocationSet
	 */
	public void setOnGeolocationSet(EventHandler<SetGeolocationEvent> onGeolocationSet) {
		this.onGeolocationSet = onGeolocationSet;
	}
}
