package org.example.ticket;

import org.example.utils.IDService;
import org.example.customer.CustomerService;
import org.example.event.EventService;
import org.example.utils.IDServiceParallel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class TicketService implements TicketServiceInterface {
    private final HashMap<Long, Ticket> tickets;
    private static final IDServiceParallel idService;

    static {
        try {
            idService = new IDServiceParallel(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final CustomerService customerService = CustomerService.getInstance();
    private final EventService eventService = EventService.getInstance();
    private static TicketService INSTANCE;

    public TicketService(IDServiceParallel idService) throws InterruptedException {
        this.tickets = new HashMap<>();
    }

    public static TicketService getInstance() throws InterruptedException {
        if (INSTANCE == null) {
            INSTANCE = new TicketService(idService);
        }
        return INSTANCE;
    }

    @Override
    public Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) throws InterruptedException {
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
    public void delete(long id) {
        if (!tickets.containsKey(id)) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        tickets.remove(id);
        idService.delete(id);
        Ticket ticket = tickets.get(id);
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
            idService.delete(id);
        }
        tickets.clear();
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
