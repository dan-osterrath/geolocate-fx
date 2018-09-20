package net.packsam.geolocatefx.model;

/**
 * Model class for a GPS geo location.
 *
 * @author osterrath
 */
public class LatLong {
	/**
	 * Latitude.
	 */
	private final double latitude;

	/**
	 * Longitude.
	 */
	private final double longitude;

	/**
	 * Ctor.
	 *
	 * @param latitude
	 * 		latitude
	 * @param longitude
	 * 		longitude
	 */
	public LatLong(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Returns the latitude.
	 *
	 * @return latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Returns the longitude.
	 *
	 * @return longitude
	 */
	public double getLongitude() {
		return longitude;
	}
}
