package org.example.spring;

import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {
    private final EventServiceInterface eventServiceInterface;

    public EventService(EventServiceInterface eventServiceInterface) {
        this.eventServiceInterface = eventServiceInterface;
    }

    public List<Event> getAll() {
        return eventServiceInterface.getAll();
    }
}
