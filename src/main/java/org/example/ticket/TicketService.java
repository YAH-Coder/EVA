package org.example.ticket;

// import org.example.utils.IDService; // Unused
import org.example.customer.CustomerService;
import org.example.event.EventService;
import org.example.utils.SharedIDService; // Added import
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.HashMap; // Will be replaced by ConcurrentHashMap
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap; // Added import

/**
 * Service class for managing ticket-related operations.
 * This class provides functionalities to add, retrieve, delete, and validate tickets.
 * It uses a ConcurrentHashMap for thread-safe storage of tickets and interacts with
 * CustomerService, EventService, and SharedIDService.
 * This class is implemented as a singleton.
 */
public class TicketService implements TicketServiceInterface {
    private final ConcurrentHashMap<Long, Ticket> tickets; // Changed to ConcurrentHashMap
    // private static final IDServiceParallel idService; // Removed

    // Static initializer block removed

    private final CustomerService customerService = CustomerService.getInstance();
    private final EventService eventService = EventService.getInstance();
    private static TicketService INSTANCE;

    private TicketService() { // Removed throws InterruptedException
        this.tickets = new ConcurrentHashMap<>(); // Changed to ConcurrentHashMap
    }

    /**
     * Returns the singleton instance of TicketService.
     *
     * @return The singleton TicketService instance.
     */
    public static TicketService getInstance() { // Removed throws InterruptedException
        if (INSTANCE == null) {
            // Ensure initial ID generation is complete before creating TicketService instance.
            // The constructor of TicketService calls getInstance() on CustomerService and EventService.
            // SharedIDService.getInstance().awaitInitialGeneration(); // REMOVED
            INSTANCE = new TicketService(); 
        }
        return INSTANCE;
    }

    @Override
    public Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew(); // Changed to SharedIDService
        // Validations for customerId and eventId existence are implicitly handled by their respective services
        // when ticket constructor calls eventService.get(eventId) and customerService.get(customerId)
        // Further, event.decreaseNmbTickets() and customer.addTicket() will also ensure they exist.
        Ticket ticket = new Ticket(id, purchaseDate, customerId, eventId);
        tickets.put(id, ticket);
        StatisticsService.getInstance().recordIdAssigned("Ticket", ticket.getId());
        customerService.get(customerId).addTicket(eventId, id); // This can throw NoSuchElementException if customer not found
        eventService.get(eventId).decreaseNmbTickets(); // This can throw NoSuchElementException if event not found or RuntimeException if no tickets
        return ticket;
    }

    @Override
    public Ticket get(long id) {
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            throw new NoSuchElementException("No ticket found with ID " + id);
        }
        return ticket;
    }

    @Override
    public void delete(long id) {
        Ticket ticket = tickets.remove(id); // Atomically removes and returns the ticket
        if (ticket == null) {
            throw new NoSuchElementException("No ticket found with ID " + id + " to delete.");
        }
        SharedIDService.getInstance().delete(id); // Changed to SharedIDService
        eventService.get(ticket.getEventId()).increaseNmbTickets();
        customerService.get(ticket.getCustomerId()).removeTicket(ticket.getEventId(), id); // Corrected typo: remooveTicket -> removeTicket
    }

    @Override
    public Ticket[] getAll() {
        return tickets.values().toArray(new Ticket[0]); // More robust for empty map
    }

    @Override
    public void deleteAll() {
        // To ensure atomicity and prevent issues if other operations modify `tickets` concurrently
        // during this loop, it's better to collect IDs first then iterate, or rely on ConcurrentHashMap's weakly consistent iterators.
        // For SharedIDService.delete, it's fine as each is an independent call.
        // For customer/event updates, this would be more complex (not required by current method signature).
        tickets.keySet().forEach(SharedIDService.getInstance()::delete);
        tickets.clear();
    }

    /**
     * Checks if a given ticket is valid for a specific event and customer.
     *
     * @param ticketId The ID of the ticket to check.
     * @param eventId The ID of the event.
     * @param customerId The ID of the customer.
     * @return {@code true} if the ticket exists, belongs to the specified customer, and is for the specified event; {@code false} otherwise.
     */
    public Boolean checkTicket(Long ticketId, Long eventId, Long customerId) {
        Ticket ticket = tickets.get(ticketId); // Returns null if ticketId is not found
        if (ticket != null) {
            return customerId.equals(ticket.getCustomerId()) && eventId.equals(ticket.getEventId());
        }
        return false;
    }
}
