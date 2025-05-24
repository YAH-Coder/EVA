package org.example.event;

// import org.example.utils.IDService; // IDService might be an interface
import org.example.utils.SharedIDService; // Added import
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.HashMap; // Will be replaced by ConcurrentHashMap
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap; // Added import

public class EventService implements EventServiceInterface {
    private final ConcurrentHashMap<Long, Event> events; // Changed to ConcurrentHashMap
    // private final IDServiceParallel idService; // Removed
    private static EventService INSTANCE;

    private EventService() { // Removed throws InterruptedException
        this.events = new ConcurrentHashMap<>(); // Changed to ConcurrentHashMap
        // this.idService = new IDServiceParallel(10000); // Removed
    }

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
        Event event = get(id);
        event.setName(name);
        event.setLocation(location);
        event.setDate(date);
        event.setNmbTickets(nmbTickets);
    }

    @Override
    public void delete(long id) {
        if (!events.containsKey(id)) {
            throw new NoSuchElementException("No event found with ID " + id);
        }
        events.remove(id);
        SharedIDService.getInstance().delete(id); // Changed to SharedIDService
    }

    @Override
    public Event[] getAll() {
        return events.values().toArray(new Event[events.size()]);
    }

    @Override
    public void deleteAll() {
        for (Long id : events.keySet()) {
            SharedIDService.getInstance().delete(id); // Changed to SharedIDService
        }
        events.clear();
    }
}