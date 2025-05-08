package org.example.event;

import org.example.utils.IDService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class EventService implements EventServiceInterface, EventServiceInterface {
    private final HashMap<Long, Event> events;
    private final IDService idService;
    private static EventService INSTANCE;

    private EventService() {
        this.events = new HashMap<>();
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
    public Event[] getAllEvents() {
        return events.values().toArray(new Event[events.size()]);
    }

    @Override
    public void deleteAll() {
        for (Long id : events.keySet()) {
            idService.delete(id);
        }
        events.clear();
    }

    @Override
    public void printAll() {
        if (events.isEmpty()) {
            System.out.println("No events available.");
            return;
        }
        for (Event event : events.values()) {
            System.out.println(event);
            System.out.println("===============");
        }
    }
}