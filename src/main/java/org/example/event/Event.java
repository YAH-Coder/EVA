package org.example.event;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger; // Added import

public class Event {
    private final long id;
    private String name;
    private String location;
    private LocalDateTime date;
    private AtomicInteger nmbTickets; // Changed to AtomicInteger

    public Event(long id, String name, String location, LocalDateTime date, int nmbTickets) {
        this.id = id;
        this.name = name;
        this.location = location;
        if (!date.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Date must be in the future");
        }
        this.date = date;
        if (nmbTickets < 0) {
            throw new IllegalArgumentException("Number of tickets cannot be negative");
        }
        this.nmbTickets = new AtomicInteger(nmbTickets); // Changed to AtomicInteger
    }

    public Event(Event other) {
        this(other.id, other.name, other.location, other.date, other.nmbTickets.get()); // Use .get() for AtomicInteger
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
        return nmbTickets.get(); // Use .get() for AtomicInteger
    }

    public void setNmbTickets(int nmbTickets) {
        if (nmbTickets < 0) {
            throw new IllegalArgumentException("Number of tickets cannot be negative");
        }
        this.nmbTickets.set(nmbTickets); // Use .set() for AtomicInteger
    }

    public void decreaseNmbTickets() {
        while (true) {
            int current = nmbTickets.get();
            if (current == 0) {
                throw new RuntimeException("Can't decrease amount of tickets below 0");
            }
            if (nmbTickets.compareAndSet(current, current - 1)) {
                break; // Successfully decremented
            }
            // If CAS failed, loop again (another thread modified nmbTickets)
        }
    }

    public void increaseNmbTickets() {
        this.nmbTickets.incrementAndGet(); // Use atomic operation
    }

    @Override
    public String toString() {
        return String.format(
                "Id: %d%nName: %s%nLocation: %s%nDate: %s%nNumber of Tickets: %d",
                id, name, location, date, nmbTickets.get() // Use .get() for AtomicInteger
        );
    }
}