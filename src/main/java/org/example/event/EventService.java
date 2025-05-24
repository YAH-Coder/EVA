package org.example.event;

// import org.example.utils.IDService; // IDService might be an interface
import org.example.utils.SharedIDService; // Added import

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class EventService implements EventServiceInterface {
    private final HashMap<Long, Event> events;
    // private final IDServiceParallel idService; // Removed
    private static EventService INSTANCE;

    private EventService() { // Removed throws InterruptedException
        this.events = new HashMap<>();
        // this.idService = new IDServiceParallel(10000); // Removed
    }

    public static EventService getInstance() throws InterruptedException { // Kept throws InterruptedException as per clarification
        if(INSTANCE == null){
            INSTANCE = new EventService();
        }
        return INSTANCE;
    }

    @Override
    public Event add(String name, String location, LocalDateTime date, int nmbTickets) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew(); // Changed to SharedIDService
        Event event = new Event(id, name, location, date, nmbTickets);
        events.put(id, event);
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