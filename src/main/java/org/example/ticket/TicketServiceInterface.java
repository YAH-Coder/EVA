package org.example.ticket;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketServiceInterface {
    Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId);

    Ticket get(long id);

    void delete(long id);

    List<Ticket> getAll();

    void deleteAll();
}
