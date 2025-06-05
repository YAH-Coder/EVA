package org.example.utils;

import java.time.LocalDateTime;

public class LogService {
    public static void log(String event) {
        LocalDateTime localDateTime = LocalDateTime.now();
        long threadId = Thread.currentThread().threadId();
        System.out.println(String.format("%s [%d] - %s;", localDateTime, threadId, event));
    }

    public static void log(String event, long eventId) {
        LocalDateTime localDateTime = LocalDateTime.now();
        long threadId = Thread.currentThread().threadId();
        System.out.println(String.format("%s [%d] - %s | %d;", localDateTime, threadId, event, eventId));
    }
}
