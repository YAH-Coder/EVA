package org.example.event;

// import org.example.utils.IDService; // IDService might be an interface
import org.example.utils.SharedIDService; // Added import
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.HashMap; // Will be replaced by ConcurrentHashMap
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap; // Added import

/**
 * Service class for managing event-related operations.
 * This class provides functionalities to add, retrieve, update, and delete events.
 * It uses a ConcurrentHashMap for thread-safe storage of events and SharedIDService for generating unique IDs.
 * This class is implemented as a singleton.
 */
public class EventService implements EventServiceInterface {
    private final ConcurrentHashMap<Long, Event> events; // Changed to ConcurrentHashMap
    // private final IDServiceParallel idService; // Removed
    private static EventService INSTANCE;

    private EventService() { // Removed throws InterruptedException
        this.events = new ConcurrentHashMap<>(); // Changed to ConcurrentHashMap
        // this.idService = new IDServiceParallel(10000); // Removed
    }

    /**
     * Returns the singleton instance of EventService.
     *
     * @return The singleton EventService instance.
     */
    public static EventService getInstance() { // Removed throws InterruptedException
        if(INSTANCE == null){
            // SharedIDService.getInstance().awaitInitialGeneration(); // REMOVED
            INSTANCE = new EventService();
        }
        return INSTANCE;
    }

    @Override
    public Event add(String name, String location, LocalDateTime date, int nmbTickets) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew(); // Changed to SharedIDService
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
        Event event = get(id); // Ensures event exists or throws NoSuchElementException
        event.setName(name);
        event.setLocation(location);
        event.setDate(date);
        event.setNmbTickets(nmbTickets);
    }

    @Override
    public void delete(long id) {
        Event existingEvent = events.remove(id); // Atomically removes and returns the event
        if (existingEvent == null) {
            throw new NoSuchElementException("No event found with ID " + id);
        }
        SharedIDService.getInstance().delete(id); // Changed to SharedIDService
    }

    @Override
    public Event[] getAll() {
        return events.values().toArray(new Event[0]); // More robust way for empty array
    }

    @Override
    public void deleteAll() {
        // Iterate over keys to delete from SharedIDService before clearing the map
        events.keySet().forEach(SharedIDService.getInstance()::delete); // Changed to SharedIDService
        events.clear();
    }
}