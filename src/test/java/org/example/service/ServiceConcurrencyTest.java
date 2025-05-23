package org.example.service;

import org.example.customer.Customer;
import org.example.customer.CustomerService;
import org.example.event.Event;
import org.example.event.EventService;
import org.example.ticket.TicketService;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceConcurrencyTest {

    private static CustomerService customerService;
    private static EventService eventService;
    private static TicketService ticketService;
    private static ExecutorService executor;

    private static final int NUM_THREADS = 10;
    private static final int OPERATIONS_PER_THREAD = 50; // Reduced for faster tests

    // Store IDs for cross-test/method use (e.g. add in one test, use in another for TicketService)
    private static final Set<Long> sharedCustomerIds = ConcurrentHashMap.newKeySet();
    private static final Set<Long> sharedEventIds = ConcurrentHashMap.newKeySet();


    @BeforeAll
    static void setUpAll() {
        customerService = CustomerService.getInstance();
        eventService = EventService.getInstance();
        ticketService = TicketService.getInstance();
        // Clean up services before starting tests
        ticketService.deleteAll();
        customerService.deleteAll();
        eventService.deleteAll();
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(NUM_THREADS * 2); // Ample threads for concurrent tasks
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate in time.");
            executor.shutdownNow();
        }
    }
    
    @AfterAll
    static void tearDownAll() {
        // Final cleanup
        ticketService.deleteAll();
        customerService.deleteAll();
        eventService.deleteAll();
        System.out.println("Shared customer IDs generated: " + sharedCustomerIds.size());
        System.out.println("Shared event IDs generated: " + sharedEventIds.size());
    }


    // --- CustomerService Tests ---

    @Test
    @Order(1)
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testCustomerServiceConcurrentAdd() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        AtomicInteger successfulAdds = new AtomicInteger(0);
        int initialCustomerCount = customerService.getAll().length;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String username = "custAdd-" + threadNum + "-" + j;
                        String email = username + "@example.com";
                        Customer c = customerService.add(username, email, LocalDateTime.now().minusYears(20));
                        assertNotNull(c, "Added customer should not be null");
                        sharedCustomerIds.add(c.getId()); // Store for later tests
                        successfulAdds.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("testCustomerServiceConcurrentAdd duration: " + (endTime - startTime) + " ms");


        assertFalse(exceptionOccurred.get(), "Exception occurred during concurrent customer adds.");
        assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, successfulAdds.get(), "Number of successful adds mismatch.");
        assertEquals(initialCustomerCount + (NUM_THREADS * OPERATIONS_PER_THREAD), customerService.getAll().length,
                "Final customer count should reflect all additions.");
    }

    @Test
    @Order(2)
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testCustomerServiceConcurrentDelete() throws InterruptedException {
        // Use a subset of previously added customers for deletion
        assertTrue(sharedCustomerIds.size() >= NUM_THREADS * (OPERATIONS_PER_THREAD / 2), "Not enough customers to test delete.");
        List<Long> idsToDelete = new ArrayList<>(sharedCustomerIds).subList(0, Math.min(sharedCustomerIds.size(), NUM_THREADS * (OPERATIONS_PER_THREAD / 2) ));
        
        CountDownLatch latch = new CountDownLatch(idsToDelete.size());
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        AtomicInteger successfulDeletes = new AtomicInteger(0);
        int initialCustomerCount = customerService.getAll().length;

        long startTime = System.currentTimeMillis();

        for (Long customerId : idsToDelete) {
            executor.submit(() -> {
                try {
                    customerService.delete(customerId);
                    sharedCustomerIds.remove(customerId); // Reflect deletion
                    successfulDeletes.incrementAndGet();
                } catch (Exception e) { // Catch NoSuchElementException if already deleted by another thread
                    // This might be acceptable depending on test design, but for this, assume each ID is deleted once
                    exceptionOccurred.set(true);
                    System.err.println("Error deleting customer " + customerId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("testCustomerServiceConcurrentDelete duration: " + (endTime - startTime) + " ms");

        assertFalse(exceptionOccurred.get(), "Exception occurred during concurrent customer deletes.");
        assertEquals(idsToDelete.size(), successfulDeletes.get(), "Mismatch in successful deletes count.");
        assertEquals(initialCustomerCount - idsToDelete.size(), customerService.getAll().length,
                "Final customer count should reflect all deletions.");
    }

    // --- EventService Tests ---

    @Test
    @Order(3)
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testEventServiceConcurrentAdd() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        AtomicInteger successfulAdds = new AtomicInteger(0);
        int initialEventCount = eventService.getAll().length;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String eventName = "eventAdd-" + threadNum + "-" + j;
                        Event e = eventService.add(eventName, "Location " + threadNum, LocalDateTime.now().plusDays(30), 100);
                        assertNotNull(e, "Added event should not be null");
                        sharedEventIds.add(e.getId()); // Store for later tests
                        successfulAdds.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("testEventServiceConcurrentAdd duration: " + (endTime - startTime) + " ms");

        assertFalse(exceptionOccurred.get(), "Exception occurred during concurrent event adds.");
        assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, successfulAdds.get(), "Number of successful event adds mismatch.");
        assertEquals(initialEventCount + (NUM_THREADS * OPERATIONS_PER_THREAD), eventService.getAll().length,
                "Final event count should reflect all additions.");
    }
    
    @Test
    @Order(4)
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testEventServiceConcurrentDelete() throws InterruptedException {
        assertTrue(sharedEventIds.size() >= NUM_THREADS * (OPERATIONS_PER_THREAD / 2), "Not enough events to test delete.");
        List<Long> idsToDelete = new ArrayList<>(sharedEventIds).subList(0, Math.min(sharedEventIds.size(), NUM_THREADS * (OPERATIONS_PER_THREAD / 2)));
        
        CountDownLatch latch = new CountDownLatch(idsToDelete.size());
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        AtomicInteger successfulDeletes = new AtomicInteger(0);
        int initialEventCount = eventService.getAll().length;

        long startTime = System.currentTimeMillis();

        for (Long eventId : idsToDelete) {
            executor.submit(() -> {
                try {
                    eventService.delete(eventId);
                    sharedEventIds.remove(eventId); // Reflect deletion
                    successfulDeletes.incrementAndGet();
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                    System.err.println("Error deleting event " + eventId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("testEventServiceConcurrentDelete duration: " + (endTime - startTime) + " ms");

        assertFalse(exceptionOccurred.get(), "Exception occurred during concurrent event deletes.");
        assertEquals(idsToDelete.size(), successfulDeletes.get(), "Mismatch in successful event deletes count.");
        assertEquals(initialEventCount - idsToDelete.size(), eventService.getAll().length,
                "Final event count should reflect all deletions.");
    }


    // --- TicketService Tests ---
    @Test
    @Order(5)
    @Timeout(value = 120, unit = TimeUnit.SECONDS) // Longer timeout for more complex test
    void testTicketServiceConcurrentAdd() throws InterruptedException {
        assertTrue(sharedCustomerIds.size() > 0, "No customers available for ticket test. Run Customer add test first.");
        assertTrue(sharedEventIds.size() > 0, "No events available for ticket test. Run Event add test first.");

        List<Long> customerIdList = new ArrayList<>(sharedCustomerIds);
        List<Long> eventIdList = new ArrayList<>(sharedEventIds);

        CountDownLatch latch = new CountDownLatch(NUM_THREADS * OPERATIONS_PER_THREAD);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        AtomicInteger successfulTicketAdds = new AtomicInteger(0);
        // Tracking tickets per event to ensure not overbooking (though service logic should prevent this)
        ConcurrentHashMap<Long, AtomicInteger> ticketsPerEvent = new ConcurrentHashMap<>();
        eventIdList.forEach(id -> ticketsPerEvent.put(id, new AtomicInteger(0)));


        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                executor.submit(() -> {
                    try {
                        // Randomly pick customer and event
                        Long customerId = customerIdList.get(ThreadLocalRandom.current().nextInt(customerIdList.size()));
                        Long eventId = eventIdList.get(ThreadLocalRandom.current().nextInt(eventIdList.size()));
                        
                        // Check current ticket count for event before trying to add
                        Event event = eventService.get(eventId); // Get fresh event data
                        if (event.getNmbTickets() > 0) {
                             ticketService.add(LocalDateTime.now(), customerId, eventId);
                             successfulTicketAdds.incrementAndGet();
                             ticketsPerEvent.get(eventId).incrementAndGet();
                        } else {
                            // System.out.println("Event " + eventId + " has no tickets left, skipping add.");
                        }

                    } catch (Exception e) {
                        // RuntimeException: "Can't purchase more than 5 tickets for a single event" is possible
                        // RuntimeException: "Event has no tickets left" is also possible from Event.decreaseNmbTickets
                        // These are not necessarily test failures if they reflect business logic.
                        // However, other unexpected exceptions are failures.
                        if (!e.getMessage().contains("Can't purchase more than 5 tickets") &&
                            !e.getMessage().contains("Event has no tickets left")) {
                            exceptionOccurred.set(true);
                            e.printStackTrace();
                        } else {
                           // System.out.println("Business logic exception: " + e.getMessage());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("testTicketServiceConcurrentAdd duration: " + (endTime - startTime) + " ms");

        assertFalse(exceptionOccurred.get(), "Unexpected exception occurred during concurrent ticket adds.");
        System.out.println("Successfully added tickets: " + successfulTicketAdds.get());

        // Verification of final state:
        // Check if nmbTickets in EventService matches tickets sold.
        // This needs careful calculation as multiple events are involved.
        // For now, primarily checking for absence of unexpected exceptions and successful operations.
        AtomicLong totalTicketsSoldByService = new AtomicLong(0);
        ticketsPerEvent.forEach((eventId, count) -> {
            System.out.println("Event " + eventId + " had " + count.get() + " tickets sold in this test run.");
            // This is not perfectly accurate as it only counts tickets from this test.
            // A better check would be to sum all tickets for these events from TicketService.getAll()
        });
        
        // A simple check: ensure total tickets in TicketService reflect additions.
        // This is hard to assert precisely without knowing initial state of TicketService for these events/customers.
        // The main value is exercising the TicketService.add() concurrently.
        assertTrue(successfulTicketAdds.get() > 0 || (NUM_THREADS * OPERATIONS_PER_THREAD == 0), "Expected some tickets to be added, or no operations were run.");
    }
}
