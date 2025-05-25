package org.example.ticket;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Interface defining the contract for ticket management services.
 * Implementing classes should provide functionalities to add, retrieve,
 * delete, and manage tickets.
 */
public interface TicketServiceInterface {

    /**
     * Adds a new ticket to the system for a given customer and event.
     * This operation involves creating a unique ID for the ticket, associating it with the
     * customer and event, and updating the event's available ticket count.
     *
     * @param purchaseDate The date and time of the ticket purchase.
     * @param customerId The ID of the customer purchasing the ticket.
     * @param eventId The ID of the event for which the ticket is being purchased.
     * @return The newly created {@link Ticket} object.
     * @throws InterruptedException If the ID generation or service interaction is interrupted.
     * @throws IllegalArgumentException If customer, event details are invalid (e.g., event date in past, customer/event not found).
     * @throws NoSuchElementException If the specified customer or event does not exist.
     * @throws RuntimeException If the event has no tickets left or other runtime issues occur.
     */
    Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) throws InterruptedException;

    /**
     * Retrieves a ticket by its unique ID.
     *
     * @param id The ID of the ticket to retrieve.
     * @return The {@link Ticket} object associated with the given ID.
     * @throws NoSuchElementException If no ticket is found with the specified ID.
     */
    Ticket get(long id);

    /**
     * Deletes a ticket from the system by its ID.
     * This operation also involves updating the event's available ticket count and
     * removing the ticket from the customer's record.
     *
     * @param id The ID of the ticket to delete.
     * @throws NoSuchElementException If no ticket is found with the specified ID.
     */
    void delete(long id);

    /**
     * Retrieves all tickets currently in the system.
     *
     * @return An array of {@link Ticket} objects. The array will be empty if there are no tickets.
     */
    Ticket[] getAll();

    /**
     * Deletes all tickets from the system.
     * This operation will also release their associated IDs.
     * Note: This typically does not automatically update customer/event records for all deleted tickets,
     * focusing primarily on clearing the ticket repository and their IDs.
     */
    void deleteAll();
}
