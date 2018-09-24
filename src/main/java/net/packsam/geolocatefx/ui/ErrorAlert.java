package net.packsam.geolocatefx.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

/**
 * Alert window after an exception occurred. The stacktrace will be displayed inside of an extra pane.
 *
 * @author osterrath
 */
public class ErrorAlert extends Alert {

	/**
	 * Ctor.
	 *
	 * @param title
	 * 		alert title
	 * @param headerText
	 * 		alert header text
	 * @param e
	 * 		exception
	 */
	public ErrorAlert(String title, String headerText, Exception e) {
		// initialize standard alert window
		super(AlertType.ERROR);
		setTitle(title);

		setHeaderText(headerText);
		if (StringUtils.isNotEmpty(e.getMessage())) {
			setContentText(e.getMessage());
		} else {
			setContentText(null);
		}

		// get stack trace
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);

		// create label
		Label label = new Label("Stacktrace:");

		// create text area for stack trace
		TextArea textArea = new TextArea(sw.toString());
		textArea.setEditable(false);
		textArea.setWrapText(false);

		// create grip pane for new components
		GridPane pane = new GridPane();
		pane.setMaxWidth(Double.MAX_VALUE);
		pane.add(label, 0, 0);
		pane.add(textArea, 0, 1);
		getDialogPane().setExpandableContent(pane);
	}

	/**
	 * Ctor.
	 *
	 * @param title
	 * 		alert title
	 * @param headerText
	 * 		alert header text
	 * @param errorMessage
	 * 		error message
	 */
	public ErrorAlert(String title, String headerText, String errorMessage) {
		// initialize standard alert window
		super(AlertType.ERROR);
		setTitle(title);

		setHeaderText(headerText);

		// create label
		Label label = new Label("Error message:");

		// create text area for stack trace
		TextArea textArea = new TextArea(errorMessage);
		textArea.setEditable(false);
		textArea.setWrapText(false);

		// create grip pane for new components
		GridPane pane = new GridPane();
		pane.setMaxWidth(Double.MAX_VALUE);
		pane.add(label, 0, 0);
		pane.add(textArea, 0, 1);
		getDialogPane().setExpandableContent(pane);
	}
}

