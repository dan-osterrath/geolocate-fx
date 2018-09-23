package net.packsam.geolocatefx.event;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventType;
import net.packsam.geolocatefx.model.LatLong;

/**
 * Event when a geolocation has been set for some images.
 *
 * @author osterrath
 */
public class SetGeolocationEvent extends Event {
	/**
	 * Event type for setting a geo location.
	 */
	private final static EventType<SetGeolocationEvent> SET_GEOLOCATION = new EventType<>("SET GEOLOCATION");

	/**
	 * Images to be updated.
	 */
	private final List<File> images;

	/**
	 * New geolocation.
	 */
	private final LatLong geolocation;

	/**
	 * Ctor.
	 *
	 * @param images
	 * 		images to be updated
	 * @param geolocation
	 * 		geo location
	 */
	public SetGeolocationEvent(List<File> images, LatLong geolocation) {
		super(SET_GEOLOCATION);
		this.images = Collections.unmodifiableList(images);
		this.geolocation = geolocation;
	}

	/**
	 * Returns the images.
	 *
	 * @return images
	 */
	public List<File> getImages() {
		return images;
	}

	/**
	 * Returns the geolocation.
	 *
	 * @return geolocation
	 */
	public LatLong getGeolocation() {
		return geolocation;
	}
}
