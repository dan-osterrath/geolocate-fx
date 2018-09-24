package net.packsam.geolocatefx.task;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import javafx.concurrent.Task;

/**
 * Class for tasks that execute external processes.
 *
 * @param <V>
 * 		return type of task
 * @author osterrath
 */
public abstract class ExternalProcessTask<V> extends Task<V> {
	/**
	 * Error handler for task.
	 */
	private ErrorHandler errorHandler;

	/**
	 * Starts the process created by the given process builder.
	 *
	 * @param processBuilder
	 * 		process builder
	 * @return started process
	 * @throws IOException
	 * 		process could not be started
	 */
	Process startProcess(ProcessBuilder processBuilder) throws IOException {
		Process process = null;
		try {
			process = processBuilder.start();
		} catch (Exception e) {
			if (errorHandler != null) {
				errorHandler.handleError(getProcessName(), -1, null, e);
			}
			throw e;
		}
		return process;
	}

	/**
	 * Waits for the given process to exit and handles the exit code and STDERR.
	 *
	 * @param process
	 * 		process to wait for
	 * @return exit code of process
	 * @throws InterruptedException
	 * 		thread got interrupted while waiting for process end
	 * @throws IOException
	 * 		could not read STDERR
	 */
	int waitForProcess(Process process) throws InterruptedException, IOException {
		int exitCode = process.waitFor();

		if (exitCode != 0) {
			if (errorHandler != null) {
				String error = IOUtils.toString(process.getErrorStream(), "UTF-8");
				errorHandler.handleError(getProcessName(), exitCode, error, null);
			}
		}

		return exitCode;
	}

	/**
	 * Returns the process name that is being executed.
	 *
	 * @return process name
	 */
	abstract String getProcessName();

	/**
	 * Returns the errorHandler.
	 *
	 * @return errorHandler
	 */
	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	/**
	 * Sets the errorHandler.
	 *
	 * @param errorHandler
	 * 		new value for errorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Functional interface for the error handler of a task.
	 *
	 * @author osterrath
	 */
	@FunctionalInterface
	public interface ErrorHandler {
		/**
		 * Handles the error from external application.
		 *
		 * @param processName
		 * 		process name that has been executed
		 * @param exitCode
		 * 		exit code
		 * @param errorOutput
		 * 		output from sterrr
		 * @param e
		 * 		optional exception when starting process
		 */
		void handleError(String processName, int exitCode, String errorOutput, Exception e);
	}
}
