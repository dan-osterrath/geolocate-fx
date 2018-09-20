package net.packsam.geolocatefx.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Model class for the initial map setup.
 */
public class MapSetup {
	/**
	 * Latitude position.
	 */
	private Double latitude;

	/**
	 * Longitude position.
	 */
	private Double longitude;

	/**
	 * Zoom level.
	 */
	private Integer zoom;

	/**
	 * Returns the latitude.
	 *
	 * @return latitude
	 */
	@XmlAttribute
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * Sets the latitude.
	 *
	 * @param latitude
	 * 		new value for latitude
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Returns the longitude.
	 *
	 * @return longitude
	 */
	@XmlAttribute
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * Sets the longitude.
	 *
	 * @param longitude
	 * 		new value for longitude
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Returns the zoom.
	 *
	 * @return zoom
	 */
	@XmlAttribute
	public Integer getZoom() {
		return zoom;
	}

	/**
	 * Sets the zoom.
	 *
	 * @param zoom
	 * 		new value for zoom
	 */
	public void setZoom(Integer zoom) {
		this.zoom = zoom;
	}
}
