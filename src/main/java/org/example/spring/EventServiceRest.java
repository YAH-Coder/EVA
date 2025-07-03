package org.example.spring;

import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

public class EventServiceRest implements EventServiceInterface {
    private final String BASE_URL = "http://localhost:8080/api/events";
    private final RestClient restClient;

    public EventServiceRest() {
        this.restClient = RestClient.create();
    }

    @Override
    public Event add(String name, String location, LocalDateTime date, int nmbTickets) {
        return null;
    }

    @Override
    public Event get(long id) {
        return null;
    }

    @Override
    public void update(long id, String name, String location, LocalDateTime date, int nmbTickets) {

    }

    @Override
    public void delete(long id) {

    }

    @Override
    public List<Event> getAll() {
        return restClient.get().uri(BASE_URL).retrieve().body(new ParameterizedTypeReference<List<Event>>() {
        });
    }

    @Override
    public void deleteAll() {

    }
}
