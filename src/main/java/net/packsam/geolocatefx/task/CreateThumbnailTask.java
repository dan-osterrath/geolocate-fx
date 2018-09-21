package net.packsam.geolocatefx.task;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.packsam.geolocatefx.Constants;
import net.packsam.geolocatefx.model.ImageModel;

/**
 * Task for creating the thumbnail for the given image file.
 *
 * @author osterrath
 */
public class CreateThumbnailTask extends Task<Void> {

	/**
	 * Size parameter for convert -resize.
	 */
	private final static String RESIZE_DIMENSIONS_PARAM = "\"" + Constants.THUMBNAIL_WIDTH + "x" + (Constants.THUMBNAIL_WIDTH << 1) + ">\"";

	/**
	 * Path to Image Magick convert.
	 */
	private final String convertPath;

	/**
	 * Target image model.
	 */
	private final ImageModel imageModel;

	/**
	 * Callback when the thumbnail has been created.
	 */
	private final Callback callback;

	/**
	 * Ctor.
	 *
	 * @param convertPath
	 * 		path to Image Magick convert
	 * @param imageModel
	 * 		target image model
	 */
	public CreateThumbnailTask(String convertPath, ImageModel imageModel) {
		this(convertPath, imageModel, null);
	}

	/**
	 * Ctor.
	 *
	 * @param convertPath
	 * 		path to Image Magick convert
	 * @param imageModel
	 * 		target image model
	 * @param callback
	 * 		callback when the thumbnail has been created
	 */
	public CreateThumbnailTask(String convertPath, ImageModel imageModel, Callback callback) {
		this.convertPath = convertPath;
		this.imageModel = imageModel;
		this.callback = callback;
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
		File imageFile = imageModel.getImage();
		File thumbnailFile = getThumbnailFile(imageFile);

		if (!(thumbnailFile.exists() && thumbnailFile.isFile() && thumbnailFile.canRead() && thumbnailFile.lastModified() >= imageFile.lastModified())) {
			if (thumbnailFile.exists()) {
				// thumb exists but seems to be too old
				thumbnailFile.delete();
			}

			String filename = imageFile.getAbsolutePath();
			if (imageModel.getDuration() != null && imageModel.getDuration() > 0 && imageModel.getVideoFrameRate() != null && imageModel.getVideoFrameRate() > 0) {
				// this seems to be a video, use frame in the middle of video
				int totalFrames = (int) (imageModel.getDuration() * imageModel.getVideoFrameRate());
				filename += "[" + (totalFrames >> 1) + "]";
			}

			// call convert
			String convert = StringUtils.isNotEmpty(convertPath) ? convertPath : "convert";
			ProcessBuilder processBuilder = new ProcessBuilder(
					convert,
					filename,
					"-background", "white",
					"-flatten",
					"-thumbnail", RESIZE_DIMENSIONS_PARAM,
					"-quality", "60%",
					"jpg:" + thumbnailFile.getAbsolutePath()
			);
			Process process = processBuilder.start();
			int returnValue = process.waitFor();

			if (returnValue != 0) {
				return null;
			}
		}

		if (callback == null) {
			Platform.runLater(() -> {
				imageModel.setThumbnail(thumbnailFile);
			});
		} else {
			callback.handleThumbnailFile(thumbnailFile);
		}
		return null;
	}

	/**
	 * Returns the file for the thumbnail image.
	 *
	 * @param imageFile
	 * 		raw image file
	 * @return file for thumbnail
	 */
	private File getThumbnailFile(File imageFile) {
		File parent = imageFile.getParentFile();
		String baseName = FilenameUtils.getBaseName(imageFile.getName());

		StringBuilder thumbname = new StringBuilder();
		if (!StringUtils.startsWith(baseName, ".")) {
			thumbname.append(".");
		}
		thumbname.append(baseName);
		thumbname.append(".thumb");
		return new File(parent, thumbname.toString());
	}

	/**
	 * Functional interface for the callback to save all data.
	 *
	 * @author osterrath
	 */
	@FunctionalInterface
	public interface Callback {
		/**
		 * Saves the thumbnail file.
		 *
		 * @param thumbnailFile
		 * 		created thumbnail file
		 */
		void handleThumbnailFile(File thumbnailFile);
	}

}
