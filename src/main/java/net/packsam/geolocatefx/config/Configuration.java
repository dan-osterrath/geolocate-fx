package net.packsam.geolocatefx.config;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Application configuration.
 *
 * @author osterrath
 */
@XmlRootElement
public class Configuration {
	/**
	 * Path to exiftool.
	 */
	private String exiftoolPath;

	/**
	 * Path to Image magick convert.
	 */
	private String convertPath;

	/**
	 * Path for last selected image.
	 */
	private String lastImagePath;

	/**
	 * API key for Google Maps.
	 */
	private String googleMapsApiKey;

	/**
	 * Last map position.
	 */
	private MapSetup lastPosition;

	/**
	 * Returns the exiftoolPath.
	 *
	 * @return exiftoolPath
	 */
	public String getExiftoolPath() {
		return exiftoolPath;
	}

	/**
	 * Sets the exiftoolPath.
	 *
	 * @param exiftoolPath
	 * 		new value for exiftoolPath
	 */
	public void setExiftoolPath(String exiftoolPath) {
		this.exiftoolPath = exiftoolPath;
	}

	/**
	 * Returns the convertPath.
	 *
	 * @return convertPath
	 */
	public String getConvertPath() {
		return convertPath;
	}

	/**
	 * Sets the convertPath.
	 *
	 * @param convertPath
	 * 		new value for convertPath
	 */
	public void setConvertPath(String convertPath) {
		this.convertPath = convertPath;
	}

	/**
	 * Returns the lastImagePath.
	 *
	 * @return lastImagePath
	 */
	public String getLastImagePath() {
		return lastImagePath;
	}

	/**
	 * Sets the lastImagePath.
	 *
	 * @param lastImagePath
	 * 		new value for lastImagePath
	 */
	public void setLastImagePath(String lastImagePath) {
		this.lastImagePath = lastImagePath;
	}

	/**
	 * Returns the googleMapsApiKey.
	 *
	 * @return googleMapsApiKey
	 */
	public String getGoogleMapsApiKey() {
		return googleMapsApiKey;
	}

	/**
	 * Sets the googleMapsApiKey.
	 *
	 * @param googleMapsApiKey
	 * 		new value for googleMapsApiKey
	 */
	public void setGoogleMapsApiKey(String googleMapsApiKey) {
		this.googleMapsApiKey = googleMapsApiKey;
	}

	/**
	 * Returns the lastPosition.
	 *
	 * @return lastPosition
	 */
	public MapSetup getLastPosition() {
		return lastPosition;
	}

	/**
	 * Sets the lastPosition.
	 *
	 * @param lastPosition
	 * 		new value for lastPosition
	 */
	public void setLastPosition(MapSetup lastPosition) {
		this.lastPosition = lastPosition;
	}
}
