package org.example.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class SharedIDServiceTest {

    private static SharedIDService service;
    private static final long LOWER_BOUND = 1_000_000_000L; // As defined in SharedIDService
    private static final int INITIAL_PRE_GENERATE_COUNT = 200; // As defined in SharedIDService
    private static final int REPLENISH_AMOUNT = 100; // As defined in SharedIDService

    @BeforeAll
    static void setUp() {
        // Ensure the logger for SharedIDService is not too verbose during tests,
        // or at least that we are aware of its output.
        Logger.getLogger(SharedIDService.class.getName()).setLevel(Level.WARNING);
        service = SharedIDService.getInstance();
        try {
            System.out.println("SharedIDServiceTest @BeforeAll: Waiting for initial prime generation...");
            long startTime = System.currentTimeMillis();
            service.awaitInitialGeneration(); // New line to ensure full initial setup
            long endTime = System.currentTimeMillis();
            System.out.println("SharedIDServiceTest @BeforeAll: Initial prime generation complete. Took " + (endTime - startTime) + "ms.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test setup interrupted while waiting for initial prime generation: " + e.getMessage());
        }
    }

    @Test
    void testSingletonInstance() {
        SharedIDService instance1 = SharedIDService.getInstance();
        SharedIDService instance2 = SharedIDService.getInstance();
        assertSame(instance1, instance2, "SharedIDService.getInstance() should always return the same instance.");
    }

    @Test
    void testInitialPrimeGeneration() throws InterruptedException {
        // The @BeforeAll setUp method now ensures initial generation is complete.
        // This test can now directly check the results.
        assertTrue(service.isInitialGenerationPerformed(), "Initial generation should be marked as performed.");
        assertTrue(service.getAvailableCount() > 0, "Initial primes should be generated and available.");
        
        long id1 = service.getNew(); // This should not block for long now
        assertTrue(id1 >= LOWER_BOUND, "Generated ID should be greater than or equal to LOWER_BOUND.");
    }

    @Test
    void testGetNew_ProvidesUniqueIDs() throws InterruptedException {
        // @BeforeAll ensures initial generation. Subsequent requests might trigger replenishment,
        // which should also be handled by the service.
        Set<Long> ids = new HashSet<>();
        // Use a smaller number of IDs to request, as the large initial pool is already generated.
        // This test focuses on uniqueness and basic replenishment triggering if necessary.
        int idsToRequest = REPLENISH_AMOUNT + 50; // Test beyond one replenish amount

        for (int i = 0; i < idsToRequest; i++) {
            long id = service.getNew();
            assertTrue(ids.add(id), "Failed to add ID " + id + ", it's a duplicate. Iteration: " + i);
        }
        assertEquals(idsToRequest, ids.size(), "All requested IDs should be unique.");
    }

    @Test
    void testDelete_IDBecomesAvailableOrRemoved() throws InterruptedException {
        // @BeforeAll ensures initial generation.
        assertTrue(service.getAvailableCount() > 0, "Should have IDs available after initial setup.");
        long id = service.getNew();
        int initialAvailable = service.getAvailableCount();
        int initialActive = service.getActiveCount();

        service.delete(id);

        assertEquals(initialAvailable + 1, service.getAvailableCount(), "Available count should increase by 1 after delete.");
        assertEquals(initialActive - 1, service.getActiveCount(), "Active count should decrease by 1 after delete.");

        // Test deleting a non-active ID
        long nonActiveId = -1L; // Assuming -1 is never a valid/active ID
        if (service.getActiveCount() > 0) { // Try to find a real non-active ID if possible
            // This is hard without knowing the full range or state.
            // For simplicity, we use -1L. If -1L could be active, pick a different strategy.
        }
        assertThrows(NoSuchElementException.class, () -> service.delete(nonActiveId),
                "Deleting a non-existent or non-active ID should throw NoSuchElementException.");
        
        // Test deleting an ID that was just returned to pool (now available, not active)
        assertThrows(NoSuchElementException.class, () -> service.delete(id),
                "Deleting an ID that was just returned (now available, not active) should throw NoSuchElementException.");
    }

    // Using RepeatedTest to run concurrency test multiple times for better confidence
    // Increased timeout for this test as it involves multiple threads and potential waiting
    @RepeatedTest(3)
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testGetNew_ConcurrentRequests_AllUnique() throws InterruptedException {
        int numThreads = 20; // Increased threads
        int idsPerThread = 50; // Increased IDs per thread
        int totalIdsToGenerate = numThreads * idsPerThread;

        Set<Long> allGeneratedIds = ConcurrentHashMap.newKeySet();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);

        // Pre-warming for concurrency test is less critical now that @BeforeAll handles initial large generation.
        // However, if idsPerThread is very large, a short check can still be useful.
        if (service.getAvailableCount() < totalIdsToGenerate && totalIdsToGenerate > REPLENISH_AMOUNT) {
             System.err.println("Warning: Concurrency test might require significant on-the-fly prime generation. Available: " + service.getAvailableCount() + ", Needed: " + totalIdsToGenerate);
        }

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        allGeneratedIds.add(service.getNew());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Log error, but let test proceed to see if set size matches
                    System.err.println("Thread interrupted while getting new ID: " + e.getMessage());
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        assertTrue(doneSignal.await(25, TimeUnit.SECONDS), "Threads did not complete in time.");
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS), "Executor service did not terminate in time.");

        assertEquals(totalIdsToGenerate, allGeneratedIds.size(),
                "All IDs generated by concurrent threads should be unique.");
    }
}
