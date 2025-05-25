package org.example.event;

import org.example.utils.SharedIDService;
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for managing event-related operations.
 * This class provides functionalities to add, retrieve, update, and delete events.
 * It uses a ConcurrentHashMap for thread-safe storage of events and SharedIDService for generating unique IDs.
 * This class is implemented as a singleton.
 */
public class EventService implements EventServiceInterface {
    private final ConcurrentHashMap<Long, Event> events;
    private static EventService INSTANCE;

    private EventService() {
        this.events = new ConcurrentHashMap<>();
    }

    /**
     * Returns the singleton instance of EventService.
     *
     * @return The singleton EventService instance.
     */
    public static EventService getInstance() {
        if(INSTANCE == null){
            INSTANCE = new EventService();
        }
        return INSTANCE;
    }

    @Override
    public Event add(String name, String location, LocalDateTime date, int nmbTickets) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew();
        Event event = new Event(id, name, location, date, nmbTickets);
        events.put(id, event);
        StatisticsService.getInstance().recordIdAssigned("Event", event.getId());
        return event;
    }

    @Override
    public Event get(long id) {
        Event event = events.get(id);
        if (event == null) {
            throw new NoSuchElementException("No event found with ID " + id);
        }
        return event;
    }

    @Override
    public void update(long id, String name, String location, LocalDateTime date, int nmbTickets) {
        Event event = get(id);
        event.setName(name);
        event.setLocation(location);
        event.setDate(date);
        event.setNmbTickets(nmbTickets);
    }

    @Override
    public void delete(long id) {
        Event existingEvent = events.remove(id);
        if (existingEvent == null) {
            throw new NoSuchElementException("No event found with ID " + id);
        }
        SharedIDService.getInstance().delete(id);
    }

    @Override
    public Event[] getAll() {
        return events.values().toArray(new Event[0]);
    }

    @Override
    public void deleteAll() {
        events.keySet().forEach(SharedIDService.getInstance()::delete);
        events.clear();
    }
}