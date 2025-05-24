package org.example.ticket;

// import org.example.utils.IDService; // Unused
import org.example.customer.CustomerService;
import org.example.event.EventService;
import org.example.utils.SharedIDService; // Added import

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class TicketService implements TicketServiceInterface {
    private final HashMap<Long, Ticket> tickets;
    // private static final IDServiceParallel idService; // Removed

    // Static initializer block removed

    private final CustomerService customerService = CustomerService.getInstance();
    private final EventService eventService = EventService.getInstance();
    private static TicketService INSTANCE;

    private TicketService() throws InterruptedException { // Changed signature, kept InterruptedException due to CS/ES.getInstance()
        this.tickets = new HashMap<>();
    }

    public static TicketService getInstance() throws InterruptedException { // Kept InterruptedException
        if (INSTANCE == null) {
            INSTANCE = new TicketService(); // Changed: no longer passing idService
        }
        return INSTANCE;
    }

    @Override
    public Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew(); // Changed to SharedIDService
        Ticket ticket = new Ticket(id, purchaseDate, customerId, eventId);
        tickets.put(id, ticket);
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
