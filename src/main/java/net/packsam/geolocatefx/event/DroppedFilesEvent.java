package net.packsam.geolocatefx.event;

import java.io.File;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventType;
import net.packsam.geolocatefx.model.LatLong;

/**
 * Event when the user dropped some images on the application.
 *
 * @author osterrath
 */
public class DroppedFilesEvent extends Event {
	/**
	 * Event type for dropping files.
	 */
	private final static EventType<DroppedFilesEvent> DROPPED_FILES = new EventType<>("DROPPED FILES");

	/**
	 * Dropped files.
	 */
	private final List<File> files;

	/**
	 * Optional geolocation for files.
	 */
	private final LatLong geolocation;

	/**
	 * Ctor.
	 *
	 * @param files
	 * 		dropped files
	 */
	public DroppedFilesEvent(List<File> files) {
		this(files, null);
	}

	/**
	 * Ctor.
	 *
	 * @param files
	 * 		dropped files
	 * @param geolocation
	 * 		optional new geolocation for files
	 */
	public DroppedFilesEvent(List<File> files, LatLong geolocation) {
		super(DROPPED_FILES);
		this.files = files;
		this.geolocation = geolocation;
	}

	/**
	 * Returns the files.
	 *
	 * @return files
	 */
	public List<File> getFiles() {
		return files;
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
