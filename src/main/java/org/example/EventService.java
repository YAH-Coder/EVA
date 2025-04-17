package org.example;

import java.util.HashMap;

;
public class EventService {
    private HashMap<Integer, Event> events = new HashMap<>();

    public EventService() {
        this.events = events;
    }

    public void add(Event event) {
        events.put(event.getId(),event);
    }

    public Event get(int id) {
        return events.get(id);
    }

    public void update(Event event) {
        events.replace(event.getId(),event);
    }
    public void delete(int id) {
        events.remove(id);
    }
}