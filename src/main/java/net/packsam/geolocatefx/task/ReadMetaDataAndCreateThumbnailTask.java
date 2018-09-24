package net.packsam.geolocatefx.task;

import java.util.Collections;

import javafx.application.Platform;
import net.packsam.geolocatefx.model.ImageModel;

/**
 * Task that combines {@link ReadMetaDataTask} and {@link CreateThumbnailTask} into a single thread.
 *
 * @author osterrath
 */
public class ReadMetaDataAndCreateThumbnailTask extends SynchronizedImageModelTask<Void> {
	/**
	 * Path to exiftool.
	 */
	private final String exiftoolPath;

	/**
	 * Path to Image Magick convert.
	 */
	private final String convertPath;

	/**
	 * Target image model.
	 */
	private final ImageModel imageModel;

	/**
	 * Ctor.
	 *
	 * @param exiftoolPath
	 * 		path to exiftool
	 * @param convertPath
	 * 		path to Image Magick convert
	 * @param imageModel
	 * 		target image model
	 */
	public ReadMetaDataAndCreateThumbnailTask(String exiftoolPath, String convertPath, ImageModel imageModel) {
		this.exiftoolPath = exiftoolPath;
		this.convertPath = convertPath;
		this.imageModel = imageModel;
	}

	/**
	 * Invoked when the Task is executed, the call method must be overridden and implemented by subclasses. The call method actually performs the background thread logic. Only
	 * the updateProgress, updateMessage, updateValue and updateTitle methods of Task may be called from code within this method. Any other interaction with the Task from the
	 * background thread will result in runtime exceptions.
	 *
	 * @return The result of the background work, if any.
	 * @throws Exception
	 * 		an unhandled exception which occurred during the background operation
	 */
	@Override
	protected Void call() throws Exception {
		lockImageModel(imageModel);

		// create cloned image model
		ImageModel dummy = new ImageModel();
		dummy.setImage(this.imageModel.getImage());

		// read meta data
		ReadMetaDataTask task1 = new ReadMetaDataTask(exiftoolPath, Collections.singletonList(dummy), (im, geolocation, creationDate, duration, videoFrameRate) -> {
			dummy.setGeolocation(geolocation);
			dummy.setCreationDate(creationDate);
			dummy.setDuration(duration);
			dummy.setVideoFrameRate(videoFrameRate);
		});
		task1.setErrorHandler(getErrorHandler());
		task1.call();

		// create thumbnail
		CreateThumbnailTask task2 = new CreateThumbnailTask(convertPath, dummy, dummy::setThumbnail);
		task2.setErrorHandler(getErrorHandler());
		task2.call();

		// copy all data to original image model
		Platform.runLater(() -> {
			imageModel.setGeolocation(dummy.getGeolocation());
			imageModel.setCreationDate(dummy.getCreationDate());
			imageModel.setDuration(dummy.getDuration());
			imageModel.setVideoFrameRate(dummy.getVideoFrameRate());
			imageModel.setThumbnail(dummy.getThumbnail());
		});

		releaseImageModel(imageModel);
		return null;
	}

	/**
	 * Returns the process name that is being executed.
	 *
	 * @return process name
	 */
	@Override
	String getProcessName() {
		return null;
	}
}
