package net.packsam.geolocatefx.ui;

import java.io.File;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import net.packsam.geolocatefx.Constants;
import net.packsam.geolocatefx.event.SetGeolocationEvent;
import net.packsam.geolocatefx.model.DragDropDataFormat;
import net.packsam.geolocatefx.model.ImageModel;
import net.packsam.geolocatefx.model.LatLong;

/**
 * Class for a draggable image thumbnail.
 *
 * @author osterrath
 */
public class ImageThumbnail extends AnchorPane {
	/**
	 * Dummy image when there is no thumbnail image available.
	 */
	private final static Image NO_IMAGE = new Image(ImageThumbnail.class.getResource("no-image.png").toExternalForm());

	/**
	 * Spinner image.
	 */
	private final static Image SPINNER = new Image(ImageThumbnail.class.getResource("spinner.png").toExternalForm());

	/**
	 * Date format for parsing date time.
	 */
	private final static DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);

	/**
	 * Image model.
	 */
	private final ImageModel imageModel;

	/**
	 * Thumbnail image view.
	 */
	private final ImageView imageView;

	/**
	 * Loader animation image view.
	 */
	private final ImageView loader;

	/**
	 * Loader animation timeline.
	 */
	private final Timeline loaderAnimation;

	/**
	 * Tooltip.
	 */
	private final Tooltip tooltip;

	/**
	 * Event handler when a geolocation has been set.
	 */
	private EventHandler<SetGeolocationEvent> onGeolocationSet;

	/**
	 * Creates a new view that represents an IMG element.
	 *
	 * @param imageModel
	 * 		image model
	 */
	public ImageThumbnail(ImageModel imageModel) {
		super();
		this.imageModel = imageModel;

		imageView = new ImageView(NO_IMAGE);
		imageView.setPreserveRatio(true);
		imageView.setFitWidth(Constants.THUMBNAIL_WIDTH);
		imageView.setOnDragOver(this::dragOverImage);
		imageView.setOnDragDropped(this::dragDroppedOnImage);
		getChildren().add(imageView);

		loader = new ImageView(SPINNER);
		loader.setMouseTransparent(true);
		getChildren().add(loader);
		AnchorPane.setBottomAnchor(loader, 0.0);
		AnchorPane.setRightAnchor(loader, 0.0);

		Rotate rotation = new Rotate(0, SPINNER.getWidth() / 2, SPINNER.getHeight() / 2);
		loader.getTransforms().add(rotation);
		loaderAnimation = new Timeline();
		loaderAnimation.getKeyFrames().add(new KeyFrame(
				Duration.seconds(1),
				new KeyValue(
						rotation.angleProperty(),
						360
				)
		));
		loaderAnimation.setCycleCount(Animation.INDEFINITE);
		loader.setVisible(false);

		tooltip = new Tooltip();
		Tooltip.install(imageView, tooltip);

		imageModel.imageProperty().addListener(o -> updateTooltip());
		imageModel.creationDateProperty().addListener(o -> updateTooltip());
		imageModel.thumbnailProperty().addListener(this::updateImage);
		imageModel.geolocationProperty().addListener(this::updateGeolocation);
		imageModel.fileInProgressProperty().addListener(this::updateFileInProgress);
		updateTooltip();
		updateImage(imageModel.thumbnailProperty(), null, imageModel.getThumbnail());
		updateGeolocation(imageModel.geolocationProperty(), null, imageModel.getGeolocation());
		updateFileInProgress(imageModel.fileInProgressProperty(), false, imageModel.isFileInProgress());
	}

	/**
	 * Event handler when the user is dragging over the thumbnail.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragOverImage(DragEvent dragEvent) {
		Dragboard db = dragEvent.getDragboard();
		List<File> images = (List<File>) db.getContent(DragDropDataFormat.ADD);
		if (images != null && !images.isEmpty() && imageModel.getGeolocation() != null) {
			if (images.size() > 1 || !images.get(0).equals(imageModel.getImage())) {
				dragEvent.acceptTransferModes(TransferMode.MOVE);
			}
		}
	}

	/**
	 * Event handler when dragging dropped over the thumbnail.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragDroppedOnImage(DragEvent dragEvent) {
		Dragboard db = dragEvent.getDragboard();
		List<File> images = (List<File>) db.getContent(DragDropDataFormat.ADD);

		if (onGeolocationSet != null) {
			onGeolocationSet.handle(new SetGeolocationEvent(images, imageModel.getGeolocation()));
		}
		dragEvent.setDropCompleted(true);
	}

	/**
	 * Event handler when the tooltip info have been changed
	 */
	private void updateTooltip() {
		File image = imageModel.getImage();
		Date creationDate = imageModel.getCreationDate();

		StringBuilder sb = new StringBuilder();
		if (image != null) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(image.getAbsolutePath());
		}
		if (creationDate != null) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(DF.format(creationDate));
		}

		if (sb.length() > 0) {
			tooltip.setText(sb.toString());
		} else {
			tooltip.setText(null);
		}
	}

	/**
	 * Event handler when the thumbnail image has been changed.
	 *
	 * @param property
	 * 		observable property
	 * @param oldVal
	 * 		old value
	 * @param newVal
	 * 		new value
	 */
	private void updateImage(ObservableValue<? extends File> property, File oldVal, File newVal) {
		if (oldVal == newVal) {
			return;
		}
		if (newVal != null) {
			try {
				imageView.setImage(new Image(newVal.toURI().toURL().toExternalForm()));
			} catch (MalformedURLException e) {
				imageView.setImage(NO_IMAGE);
			}
		} else {
			imageView.setImage(NO_IMAGE);
		}
	}

	/**
	 * Event handler when the geolocation has been changed.
	 *
	 * @param property
	 * 		observable property
	 * @param oldVal
	 * 		old value
	 * @param newVal
	 * 		new value
	 */
	private void updateGeolocation(ObservableValue<? extends LatLong> property, LatLong oldVal, LatLong newVal) {
		if (oldVal == newVal) {
			return;
		}
		if (newVal != null) {
			imageView.setOpacity(0.2);
		} else {
			imageView.setOpacity(1.0);
		}
	}

	/**
	 * Event handler when the "file in progress" flag has been changed.
	 *
	 * @param property
	 * 		observable property
	 * @param oldVal
	 * 		old value
	 * @param newVal
	 * 		new value
	 */
	private void updateFileInProgress(ObservableValue<? extends Boolean> property, boolean oldVal, boolean newVal) {
		if (oldVal == newVal) {
			return;
		}

		if (newVal) {
			Platform.runLater(this::showLoader);
		} else {
			Platform.runLater(this::hideLoader);
		}
	}

	/**
	 * Shows the loader animation.
	 */
	private void showLoader() {
		loader.setVisible(true);
		loaderAnimation.play();
	}

	/**
	 * Hides the loader animation.
	 */
	private void hideLoader() {
		loader.setVisible(false);
		loaderAnimation.pause();
	}

	/**
	 * Returns the imageModel.
	 *
	 * @return imageModel
	 */
	public ImageModel getImageModel() {
		return imageModel;
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
