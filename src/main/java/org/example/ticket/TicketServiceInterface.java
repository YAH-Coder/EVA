package org.example.ticket;

import java.time.LocalDateTime;

public interface TicketServiceInterface {
    Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId);

    Ticket get(long id);

    void delete(long id);

    Ticket[] getAll();

    void deleteAll();
}
