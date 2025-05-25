package org.example.event;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Interface defining the contract for event management services.
 * Implementing classes should provide functionalities to add, retrieve,
 * update, and delete events, as well as retrieve all events.
 */
public interface EventServiceInterface {
    /**
     * Adds a new event to the system.
     *
     * @param name The name of the event.
     * @param location The location of the event.
     * @param date The date and time of the event. Must be in the future.
     * @param nmbTickets The number of tickets available for the event.
     * @return The newly created {@link Event} object.
     * @throws InterruptedException If the ID generation process is interrupted.
     * @throws IllegalArgumentException If any of the provided event details are invalid (e.g., date not in future, negative tickets).
     */
    Event add(String name, String location, LocalDateTime date, int nmbTickets) throws InterruptedException;

    /**
     * Retrieves an event by its unique ID.
     *
     * @param id The ID of the event to retrieve.
     * @return The {@link Event} object associated with the given ID.
     * @throws NoSuchElementException If no event is found with the specified ID.
     */
    Event get(long id);

    /**
     * Updates the details of an existing event.
     *
     * @param id The ID of the event to update.
     * @param name The new name for the event.
     * @param location The new location for the event.
     * @param date The new date for the event. Must be in the future.
     * @param nmbTickets The new number of tickets for the event.
     * @throws NoSuchElementException If no event is found with the specified ID.
     * @throws IllegalArgumentException If any of the provided event details for update are invalid.
     */
    void update(long id, String name, String location, LocalDateTime date, int nmbTickets);

    /**
     * Deletes an event from the system by its ID.
     *
     * @param id The ID of the event to delete.
     * @throws NoSuchElementException If no event is found with the specified ID.
     */
    void delete(long id);

    /**
     * Retrieves all events currently in the system.
     *
     * @return An array of {@link Event} objects. The array will be empty if there are no events.
     */
    Event[] getAll();

    /**
     * Deletes all events from the system.
     * This operation will also release their associated IDs.
     */
    void deleteAll();
}
