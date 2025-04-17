package org.example;

import java.time.LocalDateTime;

public class Event {
    private final long id;
    private String name;
    private String location;
    private LocalDateTime date;
    private int nmbTickets;

    public Event(long id, String name, String location, LocalDateTime date, int nmbTickets) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.date = date;
        this.nmbTickets = nmbTickets;
    }

    public Event(Event other) {
        this(other.id, other.name, other.location, other.date, other.nmbTickets);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        this.location = location;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (!date.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Date must be in the future");
        }
        this.date = date;
    }

    public int getNmbTickets() {
        return nmbTickets;
    }

    public void setNmbTickets(int nmbTickets) {
        if (nmbTickets < 0) {
            throw new IllegalArgumentException("Number of tickets cannot be negative");
        }
        this.nmbTickets = nmbTickets;
    }

    @Override
    public String toString() {
        return String.format(
                "Id: %d%nName: %s%nLocation: %s%nDate: %s%nNumber of Tickets: %d",
                id, name, location, date, nmbTickets
        );
    }
}