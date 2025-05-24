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
        Ticket ticket = new Ticket(id, purchaseDate, customerId, eventId);
        tickets.put(id, ticket);
        StatisticsService.getInstance().recordIdAssigned("Ticket", ticket.getId());
        customerService.get(customerId).addTicket(eventId, id);
        eventService.get(eventId).decreaseNmbTickets();
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
        if (!tickets.containsKey(id)) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        Ticket ticket = tickets.get(id); // Get ticket before removing for customer/event updates
        if (ticket == null) { // Should not happen if containsKey is true, but good practice
             throw new NoSuchElementException("Ticket " + id + " disappeared before deletion operations.");
        }
        tickets.remove(id);
        SharedIDService.getInstance().delete(id); // Changed to SharedIDService
        eventService.get(ticket.getEventId()).increaseNmbTickets();
        customerService.get(ticket.getCustomerId()).remooveTicket(ticket.getEventId(), id);
    }

    @Override
    public Ticket[] getAll() {
        return tickets.values().toArray(new Ticket[tickets.size()]);
    }

    @Override
    public void deleteAll() {
        for (Long id : tickets.keySet()) {
            SharedIDService.getInstance().delete(id); // Changed to SharedIDService
        }
        tickets.clear();
    }

    public Boolean checkTicket(Long ticketId, Long eventId, Long customerId) {
        if (tickets.containsKey(ticketId)) {
            // Check if ticketId exists before calling get to avoid NullPointerException
            Ticket ticket = tickets.get(ticketId);
            if (ticket != null && customerId.equals(ticket.getCustomerId()) && eventId.equals(ticket.getEventId())) {
                return true;
            }
        }
        // Removed redundant else, as it will fall through to return false
        return false;
    }
}
