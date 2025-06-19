package org.example.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.logging.Level;

import lombok.extern.java.Log;

@Log
public class LogService {
    private static final String LOG_FOLDER = "logs";
    private static final String LOG_FILE = "application.log";
    private static PrintWriter logWriter;
    
    static {
        try {
            // Create logs directory if it doesn't exist
            Path logDir = Paths.get(LOG_FOLDER);
            if (!Files.exists(logDir)) {
                Files.createDirectory(logDir);
            }
            
            // Set up the log file (overwrite mode)
            File logFile = new File(logDir.toFile(), LOG_FILE);
            logWriter = new PrintWriter(new FileOutputStream(logFile, false), true);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to initialize log file", e);
            // Fall back to console if file logging fails
            logWriter = new PrintWriter(System.out, true);
        }
        
        // Add shutdown hook to close the writer
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (logWriter != null) {
                logWriter.close();
            }
        }));
    }

    public static void log(String event) {
        LocalDateTime localDateTime = LocalDateTime.now();
        long threadId = Thread.currentThread().threadId();
        logWriter.println(String.format("%s [%d] - %s;", localDateTime, threadId, event));
    }

    public static void log(String event, long eventId) {
        LocalDateTime localDateTime = LocalDateTime.now();
        long threadId = Thread.currentThread().threadId();
        logWriter.println(String.format("%s [%d] - %s | %d;", localDateTime, threadId, event, eventId));
    }
}