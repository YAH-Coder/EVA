package org.example.ticket;

import org.example.utils.IDService;
import org.example.customer.CustomerService;
import org.example.event.EventService;
import org.example.utils.IDServiceParallel;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.NoSuchElementException;

public class TicketService implements TicketServiceInterface {
    private final ConcurrentHashMap<Long, Ticket> tickets;
    private static final IDServiceParallel idService; // Keep this, it will be initialized from singleton
    private static final TicketService INSTANCE;

    static {
        try {
            // idService = new IDServiceParallel(10000); // Removed
            idService = IDServiceParallel.getInstance(); // Get the singleton instance
            INSTANCE = new TicketService();
        } catch (RuntimeException e) { // Catching RuntimeException from IDServiceParallel.getInstance()
            throw new RuntimeException("Failed to initialize TicketService", e);
        }
    }

    private final CustomerService customerService = CustomerService.getInstance();
    private final EventService eventService = EventService.getInstance();

    private TicketService() { // No longer throws InterruptedException directly
        this.tickets = new ConcurrentHashMap<>();
    }

    public static TicketService getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) throws InterruptedException {
        long id = idService.getNew();
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
    public synchronized void delete(long id) {
        Ticket ticket = tickets.get(id); // Get ticket first
        if (ticket == null) { // Check if ticket exists
            throw new NoSuchElementException("No ticket found with ID " + id);
        }
        tickets.remove(id);
        idService.delete(id);
        eventService.get(ticket.getEventId()).increaseNmbTickets();
        customerService.get(ticket.getCustomerId()).removeTicket(ticket.getEventId(), id); // Corrected call
    }

    @Override
    public Ticket[] getAll() {
        return tickets.values().toArray(new Ticket[tickets.size()]);
    }

    @Override
    public synchronized void deleteAll() {
        // It's important to collect keys first if modifying the map while iterating,
        // but ConcurrentHashMap's keySet is weakly consistent and should be safe.
        // However, the main atomicity concern here is the interaction with idService.
        for (Long id : tickets.keySet()) {
            // We need to ensure that for each ticket, its ID is deleted from idService
            // and then it's removed from the local map.
            // The provided logic below is fine for atomicity of deleting from idService
            // and then clearing the map.
            // If atomicity per ticket was required (e.g. if other operations could occur
            // during this loop), we'd need to handle it differently.
            idService.delete(id);
        }
        tickets.clear(); // This is atomic for ConcurrentHashMap
    }

    public Boolean checkTicket(Long ticketId, Long eventId, Long customerId) {
        if (tickets.containsKey(ticketId)) {
            if (customerId == tickets.get(ticketId).getCustomerId() && eventId == tickets.get(ticketId).getEventId()) {
                return true;
            }
        } else {
            return false;
        }
        return false;
    }
}
