package net.packsam.geolocatefx.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;

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
public class CreateThumbnailTask extends Task<File> {

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
	 * Ctor.
	 *
	 * @param convertPath
	 * 		path to Image Magick convert
	 * @param imageModel
	 * 		target image model
	 */
	public CreateThumbnailTask(String convertPath, ImageModel imageModel) {
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
	protected File call() throws Exception {
		File imageFile = imageModel.getImage();
		File thumbnailFile = getThumbnailFile(imageFile);

		if (!(thumbnailFile.exists() && thumbnailFile.isFile() && thumbnailFile.canRead() && thumbnailFile.lastModified() >= imageFile.lastModified())) {
			if (thumbnailFile.exists()) {
				// thumb exists but seems to be too old
				thumbnailFile.delete();
			}

			// call convert
			String convert = StringUtils.isNotEmpty(convertPath) ? convertPath : "convert";
			ProcessBuilder processBuilder = new ProcessBuilder(
					convert,
					imageFile.getAbsolutePath(),
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

			// make file hidden
			try {
				Files.setAttribute(thumbnailFile.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
			} catch (IOException e) {
				// ignore errors
			}
		}

		Platform.runLater(() -> {
			imageModel.setThumbnail(thumbnailFile);
		});
		return thumbnailFile;
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
}
