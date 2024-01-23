package de.dbuss.tefcontrol.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.textfield.TextArea;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogView extends VerticalLayout {

    private TextArea textArea;

    public LogView() {
        textArea = new TextArea();
        textArea.setWidth("100%");
        textArea.setHeight("300px");

        add(textArea);
    }

    public void logMessage2(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String formattedMessage = String.format("[%s] [%s] %s<br>", timestamp, level, message);

        // Set color based on the log level
        switch (level.toUpperCase()) {
            case "INFO":
                formattedMessage = "<span style=\"color: green;\">" + formattedMessage + "</span>";
                break;
            case "ERROR":
                formattedMessage = "<span style=\"color: red;\">" + formattedMessage + "</span>";
                break;
            // Add more cases for other log levels if needed

            default:
                break;
        }

        // Append the formatted message with a line break to the existing content
        textArea.getElement().executeJs("arguments[0].content.innerHTML += arguments[1];", textArea.getElement(), formattedMessage);

        // Add a line break
        textArea.getElement().executeJs("arguments[0].content.innerHTML += '<br>';", textArea.getElement());

        // Scroll to the bottom
        UI.getCurrent().getPage().executeJs("arguments[0].content.scrollTop = arguments[0].content.scrollHeight;", textArea.getElement());
    }

    public void logMessage(String level, String message) {
        textArea.setValue(textArea.getValue() + level + " " + message + "\n");
        UI.getCurrent().getPage().executeJs("arguments[0].scrollTop = arguments[0].scrollHeight;", textArea.getElement());
    }


}