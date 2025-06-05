package org.example.event;

import org.example.utils.IDService;
import org.example.utils.LogService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class EventService implements EventServiceInterface {
    private final ConcurrentHashMap<Long, Event> events;
    private final IDService idService;
    private static EventService INSTANCE;

    private EventService() {
        this.events = new ConcurrentHashMap<>();
        this.idService = new IDService();
    }

    public static EventService getInstance() {
        if(INSTANCE == null){
            INSTANCE = new EventService();
        }
        return INSTANCE;
    }

    @Override
    public Event add(String name, String location, LocalDateTime date, int nmbTickets) {
        long id = idService.getNew();
        Event event = new Event(id, name, location, date, nmbTickets);
        events.put(id, event);
        LogService.log("Event-Created", id);
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
        idService.delete(id);
    }

    @Override
    public List<Event> getAll() {
        synchronized (events) {
            return new ArrayList<>(events.values());
        }
    }

    @Override
    public void deleteAll() {
        for (Long id : events.keySet()) {
            idService.delete(id);
        }
        events.clear();
    }
}