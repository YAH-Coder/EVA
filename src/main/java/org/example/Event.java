package org.example;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Date;

@Getter
@Setter
public class Event {
    private long id;
    private String name;
    private String location;
    private Date date;
    private int nmbTickets;

    public Event(long id, String name, String location, Date date, int nmbTickets) {
        this.id = id;
        this.name = name;
        this.location = location;
        if (date.toInstant().isAfter(Instant.now())) {
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
        return "Name: " + name + "\n" + "Location: " + location + "\n" + "Date: " + date + "\n" + "Number of Tickets: " + nmbTickets;
    }
}