package org.example.ticket;

import org.example.customer.CustomerService;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.utils.IDService;
import org.example.utils.LogService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class TicketService implements TicketServiceInterface {
    private final ConcurrentHashMap<Long, Ticket> tickets;
    private final IDService idService;
    private final CustomerService customerService = CustomerService.getInstance();
    private final EventService eventService = EventService.getInstance();
    private static TicketService INSTANCE;

    public TicketService() {
        this.tickets = new ConcurrentHashMap<>();
        this.idService = new IDService();
    }

    public static TicketService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TicketService();
        }
        return INSTANCE;
    }

    @Override
    public Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) {
        Event event = eventService.get(eventId);

        if (!event.decreaseNmbTickets()) {
            throw new RuntimeException("No tickets available for event with ID " + eventId);
        }

        long id = idService.getNew();
        try {
            customerService.get(customerId);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Customer ID " + customerId + " not found.");
        }
        try {
            eventService.get(eventId);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Event ID " + eventId + " not found.");
        }
        if (purchaseDate.isAfter(event.getDate())) {
            throw new IllegalArgumentException("Purchase date must be before event date.");
        }

        Ticket ticket = new Ticket(id, purchaseDate, customerId, eventId);
        tickets.put(id, ticket);
        customerService.get(customerId).addTicket(eventId, id);
        LogService.log("Ticket-Created", id);
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
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            throw new NoSuchElementException("No ticket found with ID " + id);
        }

        tickets.remove(id);
        idService.delete(id);
        eventService.get(ticket.getEventId()).increaseNmbTickets();
        customerService.get(ticket.getCustomerId()).remooveTicket(ticket.getEventId(), id);
    }

    @Override
    public List<Ticket> getAll() {
        return new ArrayList<>(tickets.values());
    }

    @Override
    public void deleteAll() {
        for (Long id : tickets.keySet()) {
            idService.delete(id);
        }
        tickets.clear();
    }

    public Boolean checkTicket(Long ticketId, Long eventId, Long customerId) {
        Ticket ticket = tickets.get(ticketId);
        if (ticket != null) {
            return customerId.equals(ticket.getCustomerId()) && eventId.equals(ticket.getEventId());
        }
        return false;
    }
}