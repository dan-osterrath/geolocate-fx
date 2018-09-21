package net.packsam.geolocatefx.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.packsam.geolocatefx.model.ImageModel;
import net.packsam.geolocatefx.model.LatLong;

/**
 * Task for reading the geolocation from the given image file.
 *
 * @author osterrath
 */
public class ReadGeolocationTask extends Task<LatLong> {

	/**
	 * Pattern for searching latitude.
	 */
	private final static Pattern LATITUDE_PATTERN = Pattern.compile("^GPS Latitude\\s+:\\s+(.+)$");

	/**
	 * Pattern for searching longitude.
	 */
	private final static Pattern LONGITUDE_PATTERN = Pattern.compile("^GPS Longitude\\s+:\\s+(.+)$");

	/**
	 * Pattern for searching the creation date.
	 */
	private final static Pattern CREATION_DATE_PATTERN = Pattern.compile("^Date/Time Original\\s+:\\s+(.+)$");

	/**
	 * Pattern for parsing degrees.
	 */
	private final static Pattern DEGREES_PATTERN = Pattern.compile("^(\\d+)\\s+deg\\s+(\\d+)'\\s+(\\d+\\.\\d+)\"\\s+(.)");

	/**
	 * Date format for parsing date time.
	 */
	private final static DateFormat DF = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

	/**
	 * Path to exiftool.
	 */
	private final String exiftoolPath;

	/**
	 * Target image model.
	 */
	private final ImageModel imageModel;

	/**
	 * Ctor.
	 *
	 * @param exiftoolPath
	 * 		path to exiftool
	 * @param imageModel
	 * 		target image model
	 */
	public ReadGeolocationTask(String exiftoolPath, ImageModel imageModel) {
		this.exiftoolPath = exiftoolPath;
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
	protected LatLong call() throws Exception {
		File imageFile = imageModel.getImage();

		// call exiftool
		String exiftool = StringUtils.isNotEmpty(exiftoolPath) ? exiftoolPath : "exiftool";
		ProcessBuilder processBuilder = new ProcessBuilder(
				exiftool,
				"-gpslatitude",
				"-gpslongitude",
				"-datetimeoriginal",
				imageFile.getAbsolutePath()
		);
		Process process = processBuilder.start();
		int returnValue = process.waitFor();

		if (returnValue != 0) {
			return null;
		}

		// parse output
		Double latitude = null;
		Double longitude = null;
		Date creationDate = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m;
				if ((m = LATITUDE_PATTERN.matcher(line)).matches()) {
					// parse latitude
					latitude = parseDegrees(m.group(1), "N", "S");
				} else if ((m = LONGITUDE_PATTERN.matcher(line)).matches()) {
					// parse longitude
					longitude = parseDegrees(m.group(1), "E", "W");
				} else if ((m = CREATION_DATE_PATTERN.matcher(line)).matches()) {
					// parse creation date
					creationDate = parseDateTime(m.group(1));
				}
			}
		}
		if (latitude == null || longitude == null) {
			return null;
		}

		LatLong geolocation = new LatLong(latitude, longitude);
		Date finalCreationDate = creationDate;
		Platform.runLater(() -> {
			imageModel.setGeolocation(geolocation);
			imageModel.setCreationDate(finalCreationDate);
		});
		return geolocation;
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

}
