package org.example.spring;

import org.example.TicketShopClient;
import org.example.TicketShopStringReader;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {
    @Autowired
    private EventServiceInterface eventServiceInterface;

    public List<Event> getAll() {
        return eventServiceInterface.getAll();
    }
}
