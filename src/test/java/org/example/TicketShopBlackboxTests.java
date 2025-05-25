package org.example;

import org.example.customer.Customer;
import org.example.event.Event;
import org.example.ticket.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TicketShopBlackboxTests {

    private TicketShop ticketShop;

    @BeforeEach
    void setUp() throws InterruptedException {
        // It's important to reset statistics if they are used or affect behavior,
        // or if previous test runs could leave state.
        // org.example.utils.StatisticsService.getInstance().reset(); // Optional, if stats affect logic or are asserted

        // Re-instantiate TicketShop or reset its state if possible.
        // Given the singleton nature of services, true reset is hard.
        // deleteAll in AfterEach is the primary mechanism for isolation.
        ticketShop = new TicketShop();
    }

    @AfterEach
    void tearDown() {
        // Order might matter if there are foreign key-like dependencies,
        // e.g., tickets depend on customers and events.
        // Delete tickets first, then customers and events.
        ticketShop.getTicketServiceInterface().deleteAll();
        ticketShop.getEventServiceInterface().deleteAll();
        ticketShop.getCustomerServiceInterface().deleteAll();
        // Potentially reset SharedIDService if its state affects subsequent tests,
        // though for black-box testing, we try to avoid such deep interactions.
        // The current deleteAll methods do return IDs to SharedIDService.
    }

    // --- Event Management Tests ---

    @Test
    void testCreateAndGetEvent() throws InterruptedException {
        String eventName = "Summer Concert";
        String location = "Central Park";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(10);
        int initialTickets = 100;

        Event createdEvent = ticketShop.getEventServiceInterface().add(eventName, location, eventDate, initialTickets);
        assertNotNull(createdEvent);
        assertTrue(createdEvent.getId() > 0);

        Event retrievedEvent = ticketShop.getEventServiceInterface().get(createdEvent.getId());
        assertNotNull(retrievedEvent);
        assertEquals(createdEvent.getId(), retrievedEvent.getId());
        assertEquals(eventName, retrievedEvent.getName());
        assertEquals(location, retrievedEvent.getLocation());
        assertEquals(eventDate, retrievedEvent.getDate());
        assertEquals(initialTickets, retrievedEvent.getNmbTickets());
    }

    @Test
    void testUpdateEvent() throws InterruptedException {
        Event event = ticketShop.getEventServiceInterface().add("Old Name", "Old Location", LocalDateTime.now().plusMonths(1), 50);
        long eventId = event.getId();

        String newName = "New Year Bash";
        String newLocation = "Times Square";
        LocalDateTime newDate = LocalDateTime.now().plusMonths(2);
        int newTicketCount = 75;

        ticketShop.getEventServiceInterface().update(eventId, newName, newLocation, newDate, newTicketCount);

        Event updatedEvent = ticketShop.getEventServiceInterface().get(eventId);
        assertEquals(newName, updatedEvent.getName());
        assertEquals(newLocation, updatedEvent.getLocation());
        assertEquals(newDate, updatedEvent.getDate());
        assertEquals(newTicketCount, updatedEvent.getNmbTickets());
    }

    @Test
    void testDeleteEvent() throws InterruptedException {
        Event event = ticketShop.getEventServiceInterface().add("To Be Deleted", "Someplace", LocalDateTime.now().plusDays(5), 10);
        long eventId = event.getId();

        ticketShop.getEventServiceInterface().delete(eventId);

        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getEventServiceInterface().get(eventId);
        });
    }

    @Test
    void testGetNonExistentEvent() {
        long nonExistentId = 999999999L; // Assuming this ID won't exist
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getEventServiceInterface().get(nonExistentId);
        });
    }
    
    @Test
    void testUpdateNonExistentEvent() {
        long nonExistentId = 999999998L;
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getEventServiceInterface().update(nonExistentId, "Attempt Update", "No Where", LocalDateTime.now().plusDays(1), 0);
        });
    }

    @Test
    void testDeleteNonExistentEvent() {
        long nonExistentId = 999999997L;
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getEventServiceInterface().delete(nonExistentId);
        });
    }

    // --- Customer Management Tests ---

    @Test
    void testCreateAndGetCustomer() throws InterruptedException {
        String username = "johndoe";
        String email = "john.doe@example.com";
        LocalDateTime birthday = LocalDateTime.now().minusYears(25);

        Customer createdCustomer = ticketShop.getCustomerServiceInterface().add(username, email, birthday);
        assertNotNull(createdCustomer);
        assertTrue(createdCustomer.getId() > 0);

        Customer retrievedCustomer = ticketShop.getCustomerServiceInterface().get(createdCustomer.getId());
        assertNotNull(retrievedCustomer);
        assertEquals(createdCustomer.getId(), retrievedCustomer.getId());
        assertEquals(username, retrievedCustomer.getUsername());
        assertEquals(email, retrievedCustomer.getEmail());
        assertEquals(birthday, retrievedCustomer.getBirthday());
    }

    @Test
    void testUpdateCustomer() throws InterruptedException {
        Customer customer = ticketShop.getCustomerServiceInterface().add("OldUser", "old@example.com", LocalDateTime.now().minusYears(30));
        long customerId = customer.getId();

        String newUsername = "NewUser";
        String newEmail = "new@example.com";
        LocalDateTime newBirthday = LocalDateTime.now().minusYears(20);

        ticketShop.getCustomerServiceInterface().update(customerId, newUsername, newEmail, newBirthday);

        Customer updatedCustomer = ticketShop.getCustomerServiceInterface().get(customerId);
        assertEquals(newUsername, updatedCustomer.getUsername());
        assertEquals(newEmail, updatedCustomer.getEmail());
        assertEquals(newBirthday, updatedCustomer.getBirthday());
    }

    @Test
    void testDeleteCustomer() throws InterruptedException {
        Customer customer = ticketShop.getCustomerServiceInterface().add("ToDelete", "delete@example.com", LocalDateTime.now().minusYears(40));
        long customerId = customer.getId();

        ticketShop.getCustomerServiceInterface().delete(customerId);

        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getCustomerServiceInterface().get(customerId);
        });
    }
    
    @Test
    void testGetNonExistentCustomer() {
        long nonExistentId = 888888888L;
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getCustomerServiceInterface().get(nonExistentId);
        });
    }

    @Test
    void testUpdateNonExistentCustomer() {
        long nonExistentId = 888888887L;
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getCustomerServiceInterface().update(nonExistentId, "Attempt Update", "no@where.com", LocalDateTime.now().minusYears(20));
        });
    }

    @Test
    void testDeleteNonExistentCustomer() {
        long nonExistentId = 888888886L;
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getCustomerServiceInterface().delete(nonExistentId);
        });
    }

    // --- Ticket Purchasing and Management Tests ---

    @Test
    void testPurchaseTicketSuccessfully() throws InterruptedException {
        Event event = ticketShop.getEventServiceInterface().add("Music Fest", "Beach Stage", LocalDateTime.now().plusDays(20), 10);
        Customer customer = ticketShop.getCustomerServiceInterface().add("MusicLover", "lover@music.com", LocalDateTime.now().minusYears(22));
        int initialEventTickets = event.getNmbTickets();

        Ticket ticket = ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), customer.getId(), event.getId());
        assertNotNull(ticket);
        assertTrue(ticket.getId() > 0);
        assertEquals(event.getId(), ticket.getEventId());
        assertEquals(customer.getId(), ticket.getCustomerId());

        Event eventAfterPurchase = ticketShop.getEventServiceInterface().get(event.getId());
        assertEquals(initialEventTickets - 1, eventAfterPurchase.getNmbTickets());

        // Verify customer has the ticket (Customer class needs a way to check this, e.g., getTickets())
        // For black-box, we might not be able to directly inspect Customer's internal ticket list easily
        // without modifying Customer or CustomerService.
        // We can, however, check if the ticket exists via TicketService.get(ticket.getId()).
        assertNotNull(ticketShop.getTicketServiceInterface().get(ticket.getId()));
        // And also using the checkTicket method
        assertTrue(ticketShop.getTicketServiceInterface().checkTicket(ticket.getId(), event.getId(), customer.getId()));

    }

    @Test
    void testPurchaseTicketNonExistentEvent() throws InterruptedException {
        Customer customer = ticketShop.getCustomerServiceInterface().add("UserForNoEvent", "user@noevent.com", LocalDateTime.now().minusYears(28));
        long nonExistentEventId = 777777777L;

        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), customer.getId(), nonExistentEventId);
        });
    }

    @Test
    void testPurchaseTicketNonExistentCustomer() throws InterruptedException {
        Event event = ticketShop.getEventServiceInterface().add("EventForNoUser", "Venue X", LocalDateTime.now().plusDays(15), 5);
        long nonExistentCustomerId = 666666666L;

        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), nonExistentCustomerId, event.getId());
        });
    }

    @Test
    void testPurchaseTicketEventSoldOut() throws InterruptedException {
        Event event = ticketShop.getEventServiceInterface().add("Limited Show", "Small Hall", LocalDateTime.now().plusDays(30), 1);
        Customer customer1 = ticketShop.getCustomerServiceInterface().add("LuckyBuyer", "lucky@example.com", LocalDateTime.now().minusYears(30));
        Customer customer2 = ticketShop.getCustomerServiceInterface().add("UnluckyBuyer", "unlucky@example.com", LocalDateTime.now().minusYears(31));

        // First customer buys the only ticket
        ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), customer1.getId(), event.getId());

        // Second customer attempts to buy a ticket
        assertThrows(RuntimeException.class, () -> { // Assuming Event.decreaseNmbTickets throws RuntimeException for 0 tickets
            ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), customer2.getId(), event.getId());
        }, "Expected an exception when buying ticket for sold-out event");
    }
    
    @Test
    void testDeleteTicket() throws InterruptedException {
        Event event = ticketShop.getEventServiceInterface().add("Cancellable Event", "Online", LocalDateTime.now().plusDays(5), 5);
        Customer customer = ticketShop.getCustomerServiceInterface().add("Canceller", "cancel@example.com", LocalDateTime.now().minusYears(25));
        int initialEventTickets = event.getNmbTickets();

        Ticket ticket = ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), customer.getId(), event.getId());
        long ticketId = ticket.getId();

        ticketShop.getTicketServiceInterface().delete(ticketId);

        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getTicketServiceInterface().get(ticketId);
        });

        Event eventAfterDeletion = ticketShop.getEventServiceInterface().get(event.getId());
        assertEquals(initialEventTickets, eventAfterDeletion.getNmbTickets()); // Ticket number should be restored

        // Verify ticket is no longer associated with customer using checkTicket
        assertFalse(ticketShop.getTicketServiceInterface().checkTicket(ticketId, event.getId(), customer.getId()));
    }

    @Test
    void testDeleteNonExistentTicket() {
        long nonExistentTicketId = 555555555L;
        assertThrows(NoSuchElementException.class, () -> {
            ticketShop.getTicketServiceInterface().delete(nonExistentTicketId);
        });
    }

    // --- Basic Concurrency Test ---
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS) // Timeout to prevent test hanging indefinitely
    void testConcurrentTicketPurchases() throws InterruptedException {
        int initialTickets = 100;
        int numCustomers = 20; // Should be <= initialTickets for this test variant
        Event event = ticketShop.getEventServiceInterface().add("Popular Concert", "Stadium", LocalDateTime.now().plusDays(60), initialTickets);
        
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < numCustomers; i++) {
            customers.add(ticketShop.getCustomerServiceInterface().add("User" + i, "user" + i + "@concurrent.com", LocalDateTime.now().minusYears(20 + i)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numCustomers > 0 ? numCustomers : 1);
        AtomicInteger successfulPurchases = new AtomicInteger(0);
        AtomicInteger failedPurchases = new AtomicInteger(0);

        for (Customer customer : customers) {
            executor.submit(() -> {
                try {
                    ticketShop.getTicketServiceInterface().add(LocalDateTime.now(), customer.getId(), event.getId());
                    successfulPurchases.incrementAndGet();
                } catch (Exception e) {
                    // Catching general exception as different issues might arise (sold out, other runtime)
                    System.err.println("Concurrent purchase failed for customer " + customer.getId() + ": " + e.getMessage());
                    failedPurchases.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS), "Executor did not terminate in time.");

        Event finalEventState = ticketShop.getEventServiceInterface().get(event.getId());
        assertEquals(initialTickets - successfulPurchases.get(), finalEventState.getNmbTickets(), "Ticket count mismatch after concurrent purchases.");
        assertEquals(numCustomers, successfulPurchases.get() + failedPurchases.get(), "Total attempts should match number of customers.");
        assertTrue(successfulPurchases.get() <= initialTickets, "More tickets sold than available.");

        // Verify total tickets in the system for this event
        Ticket[] allTickets = ticketShop.getTicketServiceInterface().getAll();
        long ticketsForThisEvent = 0;
        for(Ticket t : allTickets) {
            if(t.getEventId() == event.getId()){
                ticketsForThisEvent++;
            }
        }
        assertEquals(successfulPurchases.get(), ticketsForThisEvent, "Number of tickets in system does not match successful purchases.");
    }
}
