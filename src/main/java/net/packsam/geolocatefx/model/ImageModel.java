package net.packsam.geolocatefx.model;

import java.io.File;
import java.util.Date;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Class for a model of an image.
 *
 * @author osterrath
 */
public class ImageModel {
	/**
	 * Flag if a current process is using this file.
	 */
	private boolean fileInProgress = false;

	/**
	 * Property for the (raw) image file.
	 */
	private final ObjectProperty<File> image = new SimpleObjectProperty<>(null);

	/**
	 * Property for the thumbnail image file.
	 */
	private final ObjectProperty<File> thumbnail = new SimpleObjectProperty<>(null);

	/**
	 * Property for the image geolocation.
	 */
	private final ObjectProperty<LatLong> geolocation = new SimpleObjectProperty<>(null);

	/**
	 * Property for the image creation date.
	 */
	private final ObjectProperty<Date> creationDate = new SimpleObjectProperty<>(null);

	/**
	 * Property for the video duration.
	 */
	private final ObjectProperty<Double> duration = new SimpleObjectProperty<>(null);

	/**
	 * Property for the video frame rate.
	 */
	private final ObjectProperty<Double> videoFrameRate = new SimpleObjectProperty<>(null);

	/**
	 * Returns the fileInProgress.
	 *
	 * @return fileInProgress
	 */
	public boolean isFileInProgress() {
		return fileInProgress;
	}

	/**
	 * Sets the fileInProgress.
	 *
	 * @param fileInProgress
	 * 		new value for fileInProgress
	 */
	public void setFileInProgress(boolean fileInProgress) {
		this.fileInProgress = fileInProgress;
	}

	public File getImage() {
		return image.get();
	}

	public ObjectProperty<File> imageProperty() {
		return image;
	}

	public void setImage(File image) {
		this.image.set(image);
	}

	public File getThumbnail() {
		return thumbnail.get();
	}

	public ObjectProperty<File> thumbnailProperty() {
		return thumbnail;
	}

	public void setThumbnail(File thumbnail) {
		this.thumbnail.set(thumbnail);
	}

	public LatLong getGeolocation() {
		return geolocation.get();
	}

	public ObjectProperty<LatLong> geolocationProperty() {
		return geolocation;
	}

	public void setGeolocation(LatLong geolocation) {
		this.geolocation.set(geolocation);
	}

	public Date getCreationDate() {
		return creationDate.get();
	}

	public ObjectProperty<Date> creationDateProperty() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate.set(creationDate);
	}

	public Double getDuration() {
		return duration.get();
	}

	public ObjectProperty<Double> durationProperty() {
		return duration;
	}

	public void setDuration(Double duration) {
		this.duration.set(duration);
	}

	public Double getVideoFrameRate() {
		return videoFrameRate.get();
	}

	public ObjectProperty<Double> videoFrameRateProperty() {
		return videoFrameRate;
	}

	public void setVideoFrameRate(Double videoFrameRate) {
		this.videoFrameRate.set(videoFrameRate);
	}
}
