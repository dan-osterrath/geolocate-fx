package net.packsam.geolocatefx.task;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javafx.application.Platform;
import net.packsam.geolocatefx.model.ImageModel;
import net.packsam.geolocatefx.model.LatLong;

/**
 * Task for writing the geolocation to the given image files.
 *
 * @author osterrath
 */
public class WriteGeolocationTask extends SynchronizedImageModelTask<Void> {

	/**
	 * Path to exiftool.
	 */
	private final String exiftoolPath;

	/**
	 * Geolocation to set.
	 */
	private final LatLong geolocation;

	/**
	 * Target image models.
	 */
	private final Collection<ImageModel> imageModels;

	/**
	 * Ctor.
	 *
	 * @param exiftoolPath
	 * 		path to exiftool
	 * @param geolocation
	 * 		geolocation to set
	 * @param imageModels
	 * 		target image models
	 */
	public WriteGeolocationTask(String exiftoolPath, LatLong geolocation, Collection<ImageModel> imageModels) {
		this.exiftoolPath = exiftoolPath;
		this.geolocation = geolocation;
		this.imageModels = new ArrayList<>(imageModels);
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
		// lock all image models in correct sort order to avoid dead locks
		List<ImageModel> sortedImageModels = imageModels.stream()
				.sorted(Comparator.comparing(ImageModel::getImage))
				.collect(Collectors.toList());
		for (ImageModel imageModel : sortedImageModels) {
			lockImageModel(imageModel);
		}

		File tempFile = null;
		try {
			// create temp files as input for exiftool
			List<String> inputFiles = sortedImageModels.stream()
					.map(ImageModel::getImage)
					.map(File::getAbsolutePath)
					.collect(Collectors.toList());

			tempFile = File.createTempFile("GeolocateFX_exiftool", ".txt");
			IOUtils.writeLines(inputFiles, System.lineSeparator(), new FileOutputStream(tempFile), Charset.defaultCharset());

			// create command line
			String exiftool = getProcessName();
			List<String> commandLine = new ArrayList<>(Arrays.asList(
					exiftool,
					"-P",
					"-overwrite_original",
					"-q",
					"-gpslatitude=" + Math.abs(geolocation.getLatitude()),
					"-gpslatituderef=" + (geolocation.getLatitude() >= 0 ? "N" : "S"),
					"-gpslongitude=" + Math.abs(geolocation.getLongitude()),
					"-gpslongituderef=" + (geolocation.getLongitude() >= 0 ? "E" : "W"),
					"-@",
					tempFile.getAbsolutePath()
			));

			// call exiftool
			ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
			Process process = startProcess(processBuilder);
			int returnValue = waitForProcess(process);

			if (returnValue == 0) {
				Platform.runLater(() -> {
					imageModels.forEach(im -> im.setGeolocation(geolocation));
				});
			}

		} finally {
			sortedImageModels.forEach(this::releaseImageModel);

			if (tempFile != null) {
				tempFile.delete();
			}
		}

		return null;
	}

	/**
	 * Returns the process name that is being executed.
	 *
	 * @return process name
	 */
	@Override
	String getProcessName() {
		return StringUtils.isNotEmpty(exiftoolPath) ? exiftoolPath : "exiftool";
	}
}
