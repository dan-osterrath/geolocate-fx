package net.packsam.geolocatefx.task;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.packsam.geolocatefx.model.ImageModel;

/**
 * Base class for a task that can synchronize on image models.
 *
 * @param <V>
 * 		return type of task
 * @author osterrath
 */
public abstract class SynchronizedImageModelTask<V> extends ExternalProcessTask<V> {
	/**
	 * Locks the given image model or waits until it is unlocked.
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
	 * Locks the given image models or waits until they are unlocked.
	 *
	 * @param imageModels
	 * 		image model to lock
	 * @return sorted list of image models
	 * @throws InterruptedException
	 * 		thread got interrupted
	 */
	List<ImageModel> lockImageModels(Collection<ImageModel> imageModels) throws InterruptedException {
		// lock all image models in correct sort order to avoid dead locks
		List<ImageModel> sortedImageModels = imageModels.stream()
				.sorted(Comparator.comparing(ImageModel::getImage))
				.collect(Collectors.toList());
		for (ImageModel imageModel : sortedImageModels) {
			lockImageModel(imageModel);
		}

		return sortedImageModels;
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
