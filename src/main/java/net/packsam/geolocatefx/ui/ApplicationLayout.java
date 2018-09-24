package net.packsam.geolocatefx.ui;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import com.lynden.gmapsfx.javascript.object.Marker;
import com.lynden.gmapsfx.javascript.object.MarkerOptions;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import net.packsam.geolocatefx.event.DroppedFilesEvent;
import net.packsam.geolocatefx.event.SetGeolocationEvent;
import net.packsam.geolocatefx.model.DragDropDataFormat;
import net.packsam.geolocatefx.model.ImageModel;

/**
 * Class for the main application layout.
 *
 * @author osterrath
 */
public class ApplicationLayout implements Initializable, MapComponentInitializedListener {
	/**
	 * Event handler when a geolocation has been set.
	 */
	private EventHandler<SetGeolocationEvent> onGeolocationSet;

	/**
	 * Event handler when the "Open images" button has been clicked.
	 */
	private EventHandler<ActionEvent> onOpenImages;

	/**
	 * Event handler when the "Settings" button has been clicked.
	 */
	private EventHandler<ActionEvent> onOpenSettings;

	/**
	 * Event handler when the user has been dropped files from external application.
	 */
	private EventHandler<DroppedFilesEvent> onFilesDropped;

	/**
	 * Property for the list of images.
	 */
	private final ListProperty<ImageModel> images = new SimpleListProperty<>(FXCollections.observableArrayList());

	/**
	 * Prooperty for the number of background tasks in queue.
	 */
	private final LongProperty backgroundTaskCount = new SimpleLongProperty();

	/**
	 * Map for the change listener of the image models.
	 */
	private final Map<ImageModel, ChangeListener<net.packsam.geolocatefx.model.LatLong>> changeListenerMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Map for the markers of the image models.
	 */
	private final Map<ImageModel, Marker> markerMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Root pane.
	 */
	@FXML
	private Pane rootPane;

	/**
	 * Container for the images.
	 */
	@FXML
	private ImageThumbnailList imageList;

	/**
	 * Container for the map view.
	 */
	@FXML
	private BorderPane mapContainer;

	/**
	 * Label for the size of the background tasks.
	 */
	@FXML
	private Label backgroundTaskCountLabel;

	/**
	 * Image for drag view.
	 */
	private Image dragView;

	/**
	 * Map view.
	 */
	private GoogleMapView mapView;

	/**
	 * Loaded map.
	 */
	private GoogleMap map;

	/**
	 * Initial map postiton.
	 */
	private net.packsam.geolocatefx.model.LatLong initialPosition;

	/**
	 * Initial zoom level.
	 */
	private Integer initialZoom;

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
		dragView = new Image(this.getClass().getResource("target.png").toExternalForm());

		// initialize image lists
		imageList.itemsProperty().bind(images);

		// monitor changes on images
		images.addListener(this::imageListChanged);

		backgroundTaskCount.addListener(this::backgroundTaskCountChanged);

		// disable all for now
		rootPane.setDisable(true);
	}

	/**
	 * Initializes the map view with the given Google Maps API key.
	 *
	 * @param apiKey
	 * 		Google Maps API key
	 * @param initialPosition
	 * 		initial map position
	 * @param initialZoom
	 * 		initial map zoom
	 */
	public void initializeMap(String apiKey, net.packsam.geolocatefx.model.LatLong initialPosition, Integer initialZoom) {
		this.initialPosition = initialPosition;
		this.initialZoom = initialZoom;

		// initialize map
		mapView = new GoogleMapView(Locale.getDefault().getLanguage(), apiKey);
		mapView.addMapInitializedListener(this);
		mapContainer.setCenter(mapView);
	}

	/**
	 * Event handler when the Google Map View has been initialized.
	 */
	@Override
	public void mapInitialized() {
		double latitude = initialPosition != null ? initialPosition.getLatitude() : 51.477778;
		double longitude = initialPosition != null ? initialPosition.getLongitude() : 0;
		int zoom = initialZoom != null ? initialZoom : 7;

		// initialize map
		MapOptions mapOptions = new MapOptions()
				.center(new LatLong(latitude, longitude))
				.zoom(zoom)
				.mapType(MapTypeIdEnum.HYBRID)
				.overviewMapControl(false)
				.panControl(true)
				.rotateControl(false)
				.scaleControl(true)
				.streetViewControl(false)
				.zoomControl(true);
		map = mapView.createMap(mapOptions);

		// enable all
		rootPane.setDisable(false);
	}

	/**
	 * Event handler when the user clicked on an image.
	 *
	 * @param mouseEvent
	 * 		mouse event
	 */
	@FXML
	private void clickedOnImage(MouseEvent mouseEvent) {
		Object source = mouseEvent.getSource();
		if (source instanceof ImageThumbnail) {
			ImageThumbnail imageThumbnail = (ImageThumbnail) source;
			ImageModel imageModel = imageThumbnail.getImageModel();
			if (imageModel != null) {
				net.packsam.geolocatefx.model.LatLong geolocation = imageModel.getGeolocation();
				if (geolocation != null) {
					mapView.setCenter(geolocation.getLatitude(), geolocation.getLongitude());
				}
			}
		}
	}

	/**
	 * Event handler when the user started dragging an image.
	 *
	 * @param mouseEvent
	 * 		mouse event
	 */
	@FXML
	private void dragDetectedOnImage(MouseEvent mouseEvent) {
		ObservableList<ImageModel> selectedImages = imageList.getSelectionModel().getSelectedItems();
		if (selectedImages != null) {
			List<File> selectedFiles = selectedImages.stream()
					.map(ImageModel::getImage)
					.collect(Collectors.toList());

			Dragboard db = rootPane.startDragAndDrop(TransferMode.MOVE);

			// set special cursor
			mapView.setMouseTransparent(true);
			db.setDragView(dragView);
			db.setDragViewOffsetX(dragView.getWidth() / 2);
			db.setDragViewOffsetY(dragView.getHeight() / 2);
			rootPane.getScene().setCursor(Cursor.NONE);

			// put image model in clipboard
			ClipboardContent content = new ClipboardContent();
			content.put(DragDropDataFormat.ADD, selectedFiles);
			db.setContent(content);
		}

		mouseEvent.consume();
	}

	/**
	 * Event handler when the user starts dragging some objects from extern.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragEntered(DragEvent dragEvent) {
		mapView.setMouseTransparent(true);
	}

	/**
	 * Event handler when the user leaves the scene while dragging.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragExited(DragEvent dragEvent) {
		mapView.setMouseTransparent(false);
	}

	/**
	 * Event handler when the user is dragging over map.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragOverMap(DragEvent dragEvent) {
		Dragboard db = dragEvent.getDragboard();

		List<File> images;
		if ((images = (List<File>) db.getContent(DragDropDataFormat.ADD)) != null && !images.isEmpty()) {
			dragEvent.acceptTransferModes(TransferMode.MOVE);
		} else if ((images = (List<File>) db.getContent(DataFormat.FILES)) != null && !images.isEmpty()) {
			dragEvent.acceptTransferModes(TransferMode.ANY);
		}
	}

	/**
	 * Event handler when the user is dragging over image list.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragOverImageList(DragEvent dragEvent) {
		Dragboard db = dragEvent.getDragboard();

		List<File> images;
		if ((images = (List<File>) db.getContent(DataFormat.FILES)) != null && !images.isEmpty()) {
			dragEvent.acceptTransferModes(TransferMode.ANY);
		}
	}

	/**
	 * Event handler when dragging is completed.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragDone(DragEvent dragEvent) {
		mapView.setMouseTransparent(false);
		rootPane.getScene().setCursor(Cursor.DEFAULT);
		dragEvent.consume();
	}

	/**
	 * Event handler when dragging dropped over map.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragDroppedOnMap(DragEvent dragEvent) {
		if (onGeolocationSet != null && onFilesDropped != null) {
			Point2D p = mapContainer.sceneToLocal(dragEvent.getSceneX(), dragEvent.getSceneY());
			LatLong latLong = map.fromPointToLatLng(p);

			Dragboard db = dragEvent.getDragboard();
			List<File> images;
			if ((images = (List<File>) db.getContent(DragDropDataFormat.ADD)) != null && !images.isEmpty()) {
				onGeolocationSet.handle(new SetGeolocationEvent(images, new net.packsam.geolocatefx.model.LatLong(latLong.getLatitude(), latLong.getLongitude())));
			} else if ((images = (List<File>) db.getContent(DataFormat.FILES)) != null && !images.isEmpty()) {
				onFilesDropped.handle(new DroppedFilesEvent(images, new net.packsam.geolocatefx.model.LatLong(latLong.getLatitude(), latLong.getLongitude())));
			}
		}
		dragEvent.setDropCompleted(true);
	}

	/**
	 * Event handler when dragging dropped over image list.
	 *
	 * @param dragEvent
	 * 		drag event
	 */
	@FXML
	private void dragDroppedOnImageList(DragEvent dragEvent) {
		if (onFilesDropped != null) {
			Dragboard db = dragEvent.getDragboard();
			List<File> images;
			if ((images = (List<File>) db.getContent(DataFormat.FILES)) != null && !images.isEmpty()) {
				onFilesDropped.handle(new DroppedFilesEvent(images, null));
			}
		}
		dragEvent.setDropCompleted(true);
	}

	/**
	 * Event handler when the user set geolocations for some images.
	 *
	 * @param setGeolocationEvent
	 * 		event
	 */
	@FXML
	private void geolocationSetForImages(SetGeolocationEvent setGeolocationEvent) {
		if (onGeolocationSet != null) {
			onGeolocationSet.handle(setGeolocationEvent);
		}
	}

	/**
	 * Event handler when the image list has been changed.
	 *
	 * @param change
	 * 		list change
	 */
	private void imageListChanged(ListChangeListener.Change<? extends ImageModel> change) {
		while (change.next()) {
			if (change.wasRemoved()) {
				List<? extends ImageModel> removed = change.getRemoved();
				removed.forEach(this::unobserveImageChanges);
			}
			if (change.wasAdded()) {
				List<? extends ImageModel> addedSubList = change.getAddedSubList();
				addedSubList.forEach(this::observeImageChanges);
			}
		}
	}

	/**
	 * Event handler when the number of background tasks have been changed.
	 *
	 * @param observableValue
	 * 		observable value
	 * @param oldVal
	 * 		old value
	 * @param newVal
	 * 		new value
	 */
	private void backgroundTaskCountChanged(ObservableValue<? extends Number> observableValue, Number oldVal, Number newVal) {
		if (Objects.equals(oldVal, newVal)) {
			return;
		}

		Platform.runLater(() -> {
			if (newVal == null || newVal.longValue() <= 0L) {
				backgroundTaskCountLabel.setVisible(false);
			} else {
				backgroundTaskCountLabel.setVisible(true);
				backgroundTaskCountLabel.setText(newVal + " files in progress");
			}
		});
	}

	/**
	 * Observes changes of the image model and updates the map.
	 *
	 * @param imageModel
	 * 		image model
	 */
	private void observeImageChanges(ImageModel imageModel) {
		ChangeListener<net.packsam.geolocatefx.model.LatLong> latLongChangeListener = (o, oldVal, newVal) -> {
			if (oldVal == newVal) {
				return;
			}
			if (oldVal != null && newVal != null && oldVal.getLatitude() == newVal.getLatitude() && oldVal.getLongitude() == newVal.getLongitude()) {
				return;
			}

			synchronized (markerMap) {
				Marker marker = markerMap.get(imageModel);

				if (newVal == null) {
					if (marker != null) {
						map.removeMarker(marker);
						markerMap.remove(imageModel);
					}
				} else {
					LatLong latLong = new LatLong(newVal.getLatitude(), newVal.getLongitude());
					if (marker == null) {
						MarkerOptions markerOptions = new MarkerOptions();
						markerOptions.title(imageModel.getImage().getAbsolutePath());
						marker = new Marker(markerOptions);
						marker.setPosition(latLong);
						markerMap.put(imageModel, marker);
						map.addMarker(marker);
						map.addUIEventHandler(marker, UIEventType.click, jsObject -> markerClicked(imageModel));
					} else {
						marker.setPosition(latLong);
					}
				}
			}

		};
		imageModel.geolocationProperty().addListener(latLongChangeListener);
		latLongChangeListener.changed(imageModel.geolocationProperty(), null, imageModel.getGeolocation());

		synchronized (changeListenerMap) {
			changeListenerMap.put(imageModel, latLongChangeListener);
		}
	}

	/**
	 * Event handler when the user clicked a map marker.
	 *
	 * @param imageModel
	 * 		image model for map marker
	 */
	private void markerClicked(ImageModel imageModel) {
		imageList.getSelectionModel().clearSelection();
		imageList.getSelectionModel().select(imageModel);
		imageList.scrollTo(imageModel);
	}

	/**
	 * Removes the observation of the image model.
	 *
	 * @param imageModel
	 * 		image model
	 */
	private void unobserveImageChanges(ImageModel imageModel) {
		synchronized (changeListenerMap) {
			ChangeListener<net.packsam.geolocatefx.model.LatLong> latLongChangeListener = changeListenerMap.get(imageModel);
			if (latLongChangeListener != null) {
				imageModel.geolocationProperty().removeListener(latLongChangeListener);
			}
		}
	}

	/**
	 * Event handler when the user clicked the "Open images" button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	@FXML
	private void openImages(ActionEvent actionEvent) {
		if (onOpenImages != null) {
			onOpenImages.handle(actionEvent);
		}
	}

	/**
	 * Event handler when the user clicked the "Open images" button.
	 *
	 * @param actionEvent
	 * 		action event
	 */
	@FXML
	private void openSettings(ActionEvent actionEvent) {
		if (onOpenSettings != null) {
			onOpenSettings.handle(actionEvent);
		}
	}

	/**
	 * Returns the current map position.
	 *
	 * @return map position
	 */
	public net.packsam.geolocatefx.model.LatLong getMapPosition() {
		if (map != null) {
			LatLong center = map.getCenter();
			return new net.packsam.geolocatefx.model.LatLong(center.getLatitude(), center.getLongitude());
		} else {
			return null;
		}
	}

	/**
	 * Returns the current map zoom level.
	 *
	 * @return zoom level
	 */
	public Integer getMapZoom() {
		if (map != null) {
			return map.getZoom();
		} else {
			return null;
		}
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

	/**
	 * Returns the onOpenImages.
	 *
	 * @return onOpenImages
	 */
	public EventHandler<ActionEvent> getOnOpenImages() {
		return onOpenImages;
	}

	/**
	 * Sets the onOpenImages.
	 *
	 * @param onOpenImages
	 * 		new value for onOpenImages
	 */
	public void setOnOpenImages(EventHandler<ActionEvent> onOpenImages) {
		this.onOpenImages = onOpenImages;
	}

	/**
	 * Returns the onOpenSettings.
	 *
	 * @return onOpenSettings
	 */
	public EventHandler<ActionEvent> getOnOpenSettings() {
		return onOpenSettings;
	}

	/**
	 * Sets the onOpenSettings.
	 *
	 * @param onOpenSettings
	 * 		new value for onOpenSettings
	 */
	public void setOnOpenSettings(EventHandler<ActionEvent> onOpenSettings) {
		this.onOpenSettings = onOpenSettings;
	}

	/**
	 * Returns the onFilesDropped.
	 *
	 * @return onFilesDropped
	 */
	public EventHandler<DroppedFilesEvent> getOnFilesDropped() {
		return onFilesDropped;
	}

	/**
	 * Sets the onFilesDropped.
	 *
	 * @param onFilesDropped
	 * 		new value for onFilesDropped
	 */
	public void setOnFilesDropped(EventHandler<DroppedFilesEvent> onFilesDropped) {
		this.onFilesDropped = onFilesDropped;
	}

	public ObservableList<ImageModel> getImages() {
		return images.get();
	}

	public ListProperty<ImageModel> imagesProperty() {
		return images;
	}

	public void setImages(ObservableList<ImageModel> images) {
		this.images.set(images);
	}

	public long getBackgroundTaskCount() {
		return backgroundTaskCount.get();
	}

	public LongProperty backgroundTaskCountProperty() {
		return backgroundTaskCount;
	}

	public void setBackgroundTaskCount(long backgroundTaskCount) {
		this.backgroundTaskCount.set(backgroundTaskCount);
	}
}
