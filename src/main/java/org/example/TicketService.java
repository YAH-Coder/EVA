package org.example;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class TicketService {
        private final HashMap<Long, Ticket> tickets;
        private final IDService idService;

        public TicketService() {
            this.tickets = new HashMap<>();
            this.idService = new IDService();
        }

        public Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) {
            long id = idService.getNew();
            Ticket ticket = new Ticket(id, purchaseDate, customerId, eventId);
            tickets.put(id, ticket);
            return ticket;
        }

        public Ticket get(long id) {
            Ticket ticket = tickets.get(id);
            if (ticket == null) {
                throw new NoSuchElementException("No ticket found with ID " + id);
            }
            return ticket;
        }

        public void delete(long id) {
            if (!tickets.containsKey(id)) {
                throw new NoSuchElementException("No customer found with ID " + id);
            }
            tickets.remove(id);
            idService.delete(id);
        }

        public Ticket[] getAllTickets() {
            return tickets.values().toArray(new Ticket[tickets.size()]);
        }

        public void deleteAll() {
            for (Long id : tickets.keySet()) {
                idService.delete(id);
            }
            tickets.clear();
        }

        public void printAll() {
            if (tickets.isEmpty()) {
                System.out.println("No tickets available.");
                return;
            }
            for (Ticket ticket : tickets.values()) {
                System.out.println(ticket);
                System.out.println("===============");
            }
        }

        public Boolean checkTicket(Long ticketId, Long eventId, Long customerId) {
            if (tickets.containsKey(ticketId)) {
                if (customerId == tickets.get(ticketId).getCustomerId() && eventId == tickets.get(ticketId).getEventId()) {
                    return true;
                }
            }
            else {
                return false;
            }
            return false;
        }
}
