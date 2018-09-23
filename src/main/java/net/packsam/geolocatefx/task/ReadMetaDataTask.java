package net.packsam.geolocatefx.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import javafx.application.Platform;
import net.packsam.geolocatefx.model.ImageModel;
import net.packsam.geolocatefx.model.LatLong;

/**
 * Task for reading the geolocation from the given image file.
 *
 * @author osterrath
 */
public class ReadMetaDataTask extends SynchronizedImageModelTask<Void> {

	/**
	 * Pattern for the current file.
	 */
	private final static Pattern CURRENT_FILE_PATTERN = Pattern.compile("^========\\s+(.+)$");

	/**
	 * Pattern for searching latitude.
	 */
	private final static Pattern LATITUDE_PATTERN = Pattern.compile("^GPSLatitude:\\s+(.+)$");

	/**
	 * Pattern for searching longitude.
	 */
	private final static Pattern LONGITUDE_PATTERN = Pattern.compile("^GPSLongitude:\\s+(.+)$");

	/**
	 * Pattern for searching the original creation date.
	 */
	private final static Pattern CREATION_DATE_ORIGINAL_PATTERN = Pattern.compile("^DateTimeOriginal:\\s+(.+)$");

	/**
	 * Pattern for searching the creation date.
	 */
	private final static Pattern CREATION_DATE_PATTERN = Pattern.compile("^CreateDate:\\s+(.+)$");

	/**
	 * Pattern for searching the video duration.
	 */
	private final static Pattern DURATION_PATTERN_1 = Pattern.compile("^Duration:\\s+(.+) s$");

	/**
	 * Pattern for searching the video duration.
	 */
	private final static Pattern DURATION_PATTERN_2 = Pattern.compile("^Duration:\\s+(.+)$");

	/**
	 * Pattern for searching the video frame rate.
	 */
	private final static Pattern VIDEO_FRAME_RATE_PATTERN = Pattern.compile("^VideoFrameRate:\\s+(.+)$");

	/**
	 * Pattern for parsing degrees.
	 */
	private final static Pattern DEGREES_PATTERN = Pattern.compile("^(\\d+)\\s+deg\\s+(\\d+)'\\s+(\\d+\\.\\d+)\"\\s+(.)");

	/**
	 * Date format for parsing date time.
	 */
	private final static DateFormat DF = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

	/**
	 * Pattern for parsing a time.
	 */
	private final static Pattern TIME_PATTERN = Pattern.compile("^(\\d+):(\\d+):(\\d+(\\.\\d+)?)");

	/**
	 * Path to exiftool.
	 */
	private final String exiftoolPath;

	/**
	 * Target image model.
	 */
	private final Collection<ImageModel> imageModels;

	/**
	 * Callback when data has been parsed.
	 */
	private final Callback callback;

	/**
	 * Ctor.
	 *
	 * @param exiftoolPath
	 * 		path to exiftool
	 * @param imageModels
	 * 		target image models
	 */
	public ReadMetaDataTask(String exiftoolPath, Collection<ImageModel> imageModels) {
		this(exiftoolPath, imageModels, null);
	}

	/**
	 * Ctor.
	 *
	 * @param exiftoolPath
	 * 		path to exiftool
	 * @param imageModels
	 * 		target image models
	 * @param callback
	 * 		callback for saving parsed data
	 */
	public ReadMetaDataTask(String exiftoolPath, Collection<ImageModel> imageModels, Callback callback) {
		this.exiftoolPath = exiftoolPath;
		this.imageModels = new ArrayList<>(imageModels);
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
		// lock all image models in correct sort order to avoid dead locks
		List<ImageModel> sortedImageModels = imageModels.stream()
				.sorted(Comparator.comparing(ImageModel::getImage))
				.collect(Collectors.toList());
		for (ImageModel imageModel : sortedImageModels) {
			lockImageModel(imageModel);
		}

		// create map for fast lookup of image models
		Map<String, ImageModel> imageModelMap = sortedImageModels.stream().collect(Collectors.toMap(im -> im.getImage().getAbsolutePath(), Function.identity(), (o1, o2) -> o1));

		// create command line
		String exiftool = StringUtils.isNotEmpty(exiftoolPath) ? exiftoolPath : "exiftool";
		List<String> commandLine = new ArrayList<>(Arrays.asList(
				exiftool,
				"-S",
				"-gpslatitude",
				"-gpslongitude",
				"-alldates",
				"-duration",
				"-videoframerate"
		));
		sortedImageModels.stream()
				.map(ImageModel::getImage)
				.map(File::getAbsolutePath)
				.forEach(commandLine::add);

		ImageModel currentImageModel = null;
		if (sortedImageModels.size() == 1) {
			currentImageModel = sortedImageModels.get(0);
		}

		// call exiftool
		ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
		Process process = processBuilder.start();

		// parse output
		Double latitude = null;
		Double longitude = null;
		Date creationDateOriginal = null;
		Date creationDate = null;
		Double duration = null;
		Double videoFrameRate = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m;
				if ((m = CURRENT_FILE_PATTERN.matcher(line)).matches()) {
					// next file starts
					saveMetaData(
							sortedImageModels,
							currentImageModel,
							latitude,
							longitude,
							creationDate,
							creationDateOriginal,
							duration,
							videoFrameRate
					);
					String fileName = m.group(1);
					fileName = fileName.replaceAll("/", Matcher.quoteReplacement(File.separator));
					currentImageModel = imageModelMap.get(fileName);
				} else if ((m = LATITUDE_PATTERN.matcher(line)).matches()) {
					// parse latitude
					latitude = parseDegrees(m.group(1), "N", "S");
				} else if ((m = LONGITUDE_PATTERN.matcher(line)).matches()) {
					// parse longitude
					longitude = parseDegrees(m.group(1), "E", "W");
				} else if ((m = CREATION_DATE_ORIGINAL_PATTERN.matcher(line)).matches()) {
					// parse original creation date
					creationDateOriginal = parseDateTime(m.group(1));
				} else if ((m = CREATION_DATE_PATTERN.matcher(line)).matches()) {
					// parse creation date
					creationDate = parseDateTime(m.group(1));
				} else if ((m = DURATION_PATTERN_1.matcher(line)).matches()) {
					// parse duration
					duration = parseDouble(m.group(1));
				} else if ((m = DURATION_PATTERN_2.matcher(line)).matches()) {
					// parse duration
					duration = parseTime(m.group(1));
				} else if ((m = VIDEO_FRAME_RATE_PATTERN.matcher(line)).matches()) {
					// parse video frame rate
					videoFrameRate = parseDouble(m.group(1));
				}
			}

			// finally save all data of last image
			saveMetaData(
					sortedImageModels,
					currentImageModel,
					latitude,
					longitude,
					creationDate,
					creationDateOriginal,
					duration,
					videoFrameRate
			);
		}

		process.waitFor();

		// release all images that have not been handled (should be none!)
		sortedImageModels.forEach(this::releaseImageModel);

		return null;
	}

	/**
	 * Saves the parsed meta data to the given target image model
	 *
	 * @param imageModels
	 * 		list of image models to remove image model from
	 * @param targetImageModel
	 * 		image model to save data to
	 * @param latitude
	 * 		geolocation latitude
	 * @param longitude
	 * 		geolocation longitude
	 * @param creationDate
	 * 		creation date
	 * @param creationDateOriginal
	 * 		original creation date
	 * @param duration
	 * 		video duration
	 * @param videoFrameRate
	 * 		video frame rate
	 */
	private void saveMetaData(List<ImageModel> imageModels, ImageModel targetImageModel, Double latitude, Double longitude, Date creationDate, Date creationDateOriginal, Double duration, Double videoFrameRate) {
		if (targetImageModel == null) {
			return;
		}

		LatLong geolocation = latitude != null && longitude != null ? new LatLong(latitude, longitude) : null;
		Date finalCreationDate = creationDateOriginal != null ? creationDateOriginal : creationDate;

		if (callback == null) {
			Platform.runLater(() -> {
				targetImageModel.setGeolocation(geolocation);
				targetImageModel.setCreationDate(finalCreationDate);
				targetImageModel.setDuration(duration);
				targetImageModel.setVideoFrameRate(videoFrameRate);
			});
		} else {
			callback.handleMetaData(targetImageModel, geolocation, finalCreationDate, duration, videoFrameRate);
		}

		releaseImageModel(targetImageModel);
		imageModels.remove(targetImageModel);
	}

	/**
	 * Tries to parse the given degrees string.
	 *
	 * @param degrees
	 * 		degrees string
	 * @param positiveDirection
	 * 		suffix for positive direction
	 * @param negativeDirection
	 * 		suffix for negative direction
	 * @return parsed numeric degrees
	 */
	private Double parseDegrees(String degrees, String positiveDirection, String negativeDirection) {
		try {
			Matcher m = DEGREES_PATTERN.matcher(degrees);
			if (!m.matches()) {
				return null;
			}

			String degreesS = m.group(1);
			String minutesS = m.group(2);
			String secondsS = m.group(3);
			String suffix = m.group(4);

			int sig;
			if (StringUtils.equalsIgnoreCase(suffix, positiveDirection)) {
				sig = 1;
			} else if (StringUtils.equalsIgnoreCase(suffix, negativeDirection)) {
				sig = -1;
			} else {
				return null;
			}

			double degreesD = Integer.parseInt(degreesS, 10);
			double minutesD = Integer.parseInt(minutesS, 10);
			double secondsD = Double.parseDouble(secondsS);

			return sig * (degreesD + (minutesD / 60) + (secondsD / 3600));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Tries to parse the given time string.
	 *
	 * @param time
	 * 		time string
	 * @return parsed time in seconds
	 */
	private Double parseTime(String time) {
		try {
			Matcher m = TIME_PATTERN.matcher(time);
			if (!m.matches()) {
				return null;
			}

			String hoursS = m.group(1);
			String minutesS = m.group(2);
			String secondsS = m.group(3);

			double hoursD = Integer.parseInt(hoursS, 10);
			double minutesD = Integer.parseInt(minutesS, 10);
			double secondsD = Double.parseDouble(secondsS);

			return secondsD + minutesD * 60 + hoursD * 3600;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Parses the given EXIF date string.
	 *
	 * @param dateString
	 * 		date string
	 * @return date
	 */
	private Date parseDateTime(String dateString) {
		try {
			return DF.parse(dateString);
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * Parses the given double value.
	 *
	 * @param val
	 * 		double value string
	 * @return parsed double
	 */
	private Double parseDouble(String val) {
		try {
			return Double.parseDouble(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Functional interface for the callback to save all data.
	 *
	 * @author osterrath
	 */
	@FunctionalInterface
	public interface Callback {
		/**
		 * Saves the parsed meta data.
		 *
		 * @param targetImageModel
		 * 		image model to save data to
		 * @param geolocation
		 * 		geolocation
		 * @param creationDate
		 * 		creation date
		 * @param duration
		 * 		video duration
		 * @param videoFrameRate
		 * 		video frame rate
		 */
		void handleMetaData(ImageModel targetImageModel, LatLong geolocation, Date creationDate, Double duration, Double videoFrameRate);
	}

}
