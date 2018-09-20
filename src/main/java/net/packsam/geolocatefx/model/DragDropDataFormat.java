package net.packsam.geolocatefx.model;

import javafx.scene.input.DataFormat;

/**
 * Data format for the drag drop operations.
 *
 * @author osterrath
 */
public final class DragDropDataFormat extends DataFormat {
	/**
	 * Data format for adding the geo locations to an image.
	 */
	public final static DragDropDataFormat ADD = new DragDropDataFormat("Add");

	/**
	 * Data format for changing the geo locations of an image.
	 */
	public final static DragDropDataFormat MOVE = new DragDropDataFormat("Move");

	/**
	 * Ctor.
	 *
	 * @param type
	 * 		data format type suffix
	 */
	private DragDropDataFormat(String type) {
		super("application.GeolocateFX." + type);
	}
}
