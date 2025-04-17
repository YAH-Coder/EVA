package org.example;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
public class Event {
    private long id;
    private String name;
    private String location;
    private LocalDateTime date;
    private int nmbTickets;

    public Event(long id, String name, String location, LocalDateTime date, int nmbTickets) {

        this.name = name;
        this.location = location;
        if (date.isAfter(LocalDateTime.now())) {
            this.date = date;
        } else {
            throw new RuntimeException("date needs to be in the future");
        }
        if (nmbTickets >= 0) {
            this.nmbTickets = nmbTickets;
        } else {
            throw new RuntimeException("nmbOfTickets can not be negative");
        }
    }

    public Event(Event event) {
        this.id = event.id;
        this.name = event.name;
        this.location = event.location;
        this.date = event.date;
        this.nmbTickets = event.nmbTickets;
    }

    @Override
    public String toString() {
        return "Id: " + id + "Name: " + name + "\n" + "Location: " + location + "\n" + "Date: " + date + "\n" + "Number of Tickets: " + nmbTickets;
    }
}