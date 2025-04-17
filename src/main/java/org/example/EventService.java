package org.example;

import java.util.Date;
import java.util.HashMap;

;
public class EventService {
    private HashMap<Long, Event> events ;
    private IDService idService;

    public EventService() {
        events = new HashMap<>();
        idService = new IDService();
    }

    public void add(String name, String location, Date date, Integer nmbTickets) {
        long id = idService.getNew();
        Event event = new Event(id, name, location, date, nmbTickets);
        events.put(id, event);
    }

    public Event get(long id) {
        return events.get(id);
    }

    public void update(Event event) {
        events.replace(event.getId(),event);
    }
    public void delete(long id) {
        idService.delete(id);
        events.remove(id);
    }

    public Event[] getAllEvents() {
        return events.values().toArray(new Event[events.size()]);
    }

    public void printAll() {
        for (Event event : events.values()) {
            System.out.println(event);
            System.out.println("===============");
        }
    }
}