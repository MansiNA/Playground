package de.dbuss.tefcontrol.views;

import de.dbuss.tefcontrol.data.entity.LogEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class LogService {

    public static final String INFO = "INFO";
    public static final String ERROR = "ERROR";
    public static final String WARN = "WARN";
    private final List<LogEntry> logEntries = new ArrayList<>();

    public void addLogEntry(LogEntry logEntry) {
        logEntries.add(logEntry);
    }

    public List<LogEntry> getLogEntries() {
        return logEntries;
    }

    public void addLogMessage(String level, String message) {

        // Create a LogEntry and add it to the central log repository
        LogEntry logEntry = new LogEntry(level, message, new Date());
        addLogEntry(logEntry);
    }
}
