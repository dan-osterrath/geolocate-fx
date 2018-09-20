package net.packsam.geolocatefx.ui;

import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Class for the drag indication icon.
 *
 * @author osterrath
 */
public class DragIndicator extends ImageView {

	/**
	 * Cache for width of icon.
	 */
	private Double widthCache;

	/**
	 * Cache for height of icon.
	 */
	private Double heightCache;

	/**
	 * Ctor.
	 */
	public DragIndicator() {
		super(new Image(DragIndicator.class.getResource("target.png").toExternalForm()));
		setMouseTransparent(true);
	}

	/**
	 * Sets the position while dragging.
	 *
	 * @param sceneX
	 * 		scene X position
	 * @param sceneY
	 * 		scene Y position
	 */
	public void setPosition(double sceneX, double sceneY) {
		if (widthCache == null || heightCache == null) {
			Bounds bounds = getLayoutBounds();
			widthCache = bounds.getWidth();
			heightCache = bounds.getHeight();
		}

		setLayoutX(sceneX - widthCache / 2);
		setLayoutY(sceneY - heightCache / 2);
	}
}
