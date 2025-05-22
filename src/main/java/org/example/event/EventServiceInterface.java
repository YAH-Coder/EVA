package org.example.event;

import java.time.LocalDateTime;

public interface EventServiceInterface {
    Event add(String name, String location, LocalDateTime date, int nmbTickets) throws InterruptedException;

    Event get(long id);

    void update(long id, String name, String location, LocalDateTime date, int nmbTickets);

    void delete(long id);

    Event[] getAll();

    void deleteAll();
}
