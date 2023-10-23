package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.LogEntry;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "-v")
@RolesAllowed("ADMIN")
public class LoggingPanel extends VerticalLayout implements BeforeEnterObserver {

    private Grid<LogEntry> logGrid;
    private LogService logService;

    public LoggingPanel(LogService logService) {
        this.logService = logService;
        logGrid = new Grid<>();
        logGrid.addColumn(LogEntry::getLevel).setHeader("Level").setClassNameGenerator(logEntry -> getColorForLogLevel(logEntry.getLevel()));
        logGrid.addColumn(LogEntry::getMessage).setHeader("Message");
        logGrid.addColumn(LogEntry::getTimestamp).setHeader("Timestamp");
        add(logGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        List<LogEntry> logs = logService.getLogEntries();
        logGrid.setItems(logs);
    }

    private String getColorForLogLevel(String level) {
        if (LogService.ERROR.equals(level)) {
            return "red";
        } else if (LogService.WARN.equals(level)) {
            return "yellow";
        } else {
            return "black";
        }
    }

}