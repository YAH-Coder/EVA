package org.example.event;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger; // Added import

/**
 * Represents an event in the ticket shop system.
 * Each event has a unique ID, name, location, date, and a number of available tickets.
 * The number of tickets is managed atomically to support concurrent access.
 */
public class Event {
    private final long id;
    private String name;
    private String location;
    private LocalDateTime date;
    private AtomicInteger nmbTickets; // Changed to AtomicInteger

    /**
     * Constructs a new Event instance.
     *
     * @param id The unique ID of the event.
     * @param name The name of the event. Must not be null or blank.
     * @param location The location of the event. Must not be null or blank.
     * @param date The date and time of the event. Must be in the future.
     * @param nmbTickets The initial number of tickets available for the event. Cannot be negative.
     * @throws IllegalArgumentException if the date is not in the future, or if the number of tickets is negative.
     */
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

    /**
     * Constructs a new Event by copying another Event instance (copy constructor).
     *
     * @param other The Event object to copy.
     */
    public Event(Event other) {
        this(other.id, other.name, other.location, other.date, other.nmbTickets.get()); // Use .get() for AtomicInteger
    }

    /**
     * Gets the unique ID of the event.
     *
     * @return The event ID.
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the name of the event.
     *
     * @return The event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     *
     * @param name The new name for the event. Must not be null or blank.
     * @throws IllegalArgumentException if the name is null or blank.
     */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
    }

    /**
     * Gets the location of the event.
     *
     * @return The event location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the event.
     *
     * @param location The new location for the event. Must not be null or blank.
     * @throws IllegalArgumentException if the location is null or blank.
     */
    public void setLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        this.location = location;
    }

    /**
     * Gets the date and time of the event.
     *
     * @return The event date.
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Sets the date and time of the event.
     *
     * @param date The new date for the event. Must be in the future and not null.
     * @throws IllegalArgumentException if the date is null or not in the future.
     */
    public void setDate(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (!date.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Date must be in the future");
        }
        this.date = date;
    }

    /**
     * Gets the current number of available tickets for the event.
     * This value is retrieved atomically.
     *
     * @return The number of available tickets.
     */
    public int getNmbTickets() {
        return nmbTickets.get(); // Use .get() for AtomicInteger
    }

    /**
     * Sets the number of available tickets for the event.
     * This value is set atomically.
     *
     * @param nmbTickets The new number of tickets. Cannot be negative.
     * @throws IllegalArgumentException if the number of tickets is negative.
     */
    public void setNmbTickets(int nmbTickets) {
        if (nmbTickets < 0) {
            throw new IllegalArgumentException("Number of tickets cannot be negative");
        }
        this.nmbTickets.set(nmbTickets); // Use .set() for AtomicInteger
    }

    /**
     * Atomically decreases the number of available tickets by one.
     * Uses a compare-and-set loop to ensure thread safety.
     *
     * @throws RuntimeException if the number of tickets is already zero and cannot be decreased further.
     */
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

    /**
     * Atomically increases the number of available tickets by one.
     */
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