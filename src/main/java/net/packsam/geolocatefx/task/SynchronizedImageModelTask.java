package net.packsam.geolocatefx.task;

import javafx.concurrent.Task;
import net.packsam.geolocatefx.model.ImageModel;

/**
 * Base class for a task that can synchronize on image models.
 *
 * @param <V>
 * 		return type of task
 */
abstract class SynchronizedImageModelTask<V> extends Task<V> {
	/**
	 * Locks the given image model or waits until
	 *
	 * @param imageModel
	 * 		image model to lock
	 * @throws InterruptedException
	 * 		thread got interrupted
	 */
	void lockImageModel(ImageModel imageModel) throws InterruptedException {
		synchronized (imageModel) {
			if (imageModel.isFileInProgress()) {
				imageModel.wait();
			}
			imageModel.setFileInProgress(true);
		}
	}

	/**
	 * Releases the given image model
	 *
	 * @param imageModel
	 * 		image model to release
	 */
	void releaseImageModel(ImageModel imageModel) {
		synchronized (imageModel) {
			imageModel.setFileInProgress(false);
			imageModel.notify();
		}
	}
}
