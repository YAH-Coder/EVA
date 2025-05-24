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
        // Give some time for initial primes to generate, especially for the first test run.
        // This helps make tests more stable.
        try {
            int attempts = 0;
            // Wait for at least a few primes to be available.
            while (service.getAvailableCount() < 10 && attempts < 100) { // Wait for 10, or up to 5s
                Thread.sleep(50);
                attempts++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Setup interrupted during initial prime generation wait.");
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
        // Wait up to a few seconds for initial primes if not already done by BeforeAll
        int attempts = 0;
        while (service.getAvailableCount() == 0 && attempts < 60) { // Max ~3 seconds
            Thread.sleep(50);
            attempts++;
        }
        assertTrue(service.getAvailableCount() > 0, "Initial primes should be generated.");
        long id1 = service.getNew();
        assertTrue(id1 >= LOWER_BOUND, "Generated ID should be greater than or equal to LOWER_BOUND.");
    }

    @Test
    void testGetNew_ProvidesUniqueIDs() throws InterruptedException {
        Set<Long> ids = new HashSet<>();
        // Request enough IDs to potentially trigger replenishment
        int idsToRequest = INITIAL_PRE_GENERATE_COUNT + REPLENISH_AMOUNT / 2; 
        
        // Ensure enough primes are available before starting, or wait
        int initialAttempts = 0;
        while(service.getAvailableCount() < REPLENISH_AMOUNT && initialAttempts < 100) {
             Thread.sleep(50); // wait for generation
             initialAttempts++;
        }
        if(service.getAvailableCount() < REPLENISH_AMOUNT && idsToRequest > service.getAvailableCount()){
            // if not enough, reduce the request to what's available to avoid deadlock in test if generation is slow
            idsToRequest = service.getAvailableCount();
            System.err.println("Warning: Reducing idsToRequest in testGetNew_ProvidesUniqueIDs to " + idsToRequest + " due to slow initial prime generation.");
        }
        if(idsToRequest == 0 && service.getAvailableCount() == 0){
             fail("No IDs available to test getNew_ProvidesUniqueIDs. Initial generation might have failed or is too slow.");
        }


        for (int i = 0; i < idsToRequest; i++) {
            ids.add(service.getNew());
        }
        assertEquals(idsToRequest, ids.size(), "All requested IDs should be unique.");
    }

    @Test
    void testDelete_IDBecomesAvailableOrRemoved() throws InterruptedException {
        // Ensure there's an ID to work with
        if (service.getAvailableCount() == 0) {
            // Try to get one to trigger generation if needed and wait for it
            service.getNew(); // This might wait
            int attempts = 0;
            while (service.getAvailableCount() == 0 && attempts < 60) { // wait up to 3s
                Thread.sleep(50);
                attempts++;
            }
        }
        // If still no ID, fail
        if (service.getAvailableCount() == 0) {
            fail("Could not obtain an ID to test deletion.");
        }

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

        // Pre-warm: Ensure enough IDs are available or can be generated quickly.
        // This is to prevent all threads from immediately blocking on an empty 'available' set
        // and then overwhelming the replenishment logic in a very spiky way.
        long requiredInitial = Math.min(totalIdsToGenerate, INITIAL_PRE_GENERATE_COUNT + REPLENISH_AMOUNT);
        int attempts = 0;
        while (service.getAvailableCount() < requiredInitial && attempts < 200) { // wait up to 10s
            Thread.sleep(50);
            attempts++;
            if (attempts % 20 == 0) { // Log progress if waiting long
                 System.out.println("Waiting for primes for concurrency test... Available: " + service.getAvailableCount() + "/" + requiredInitial);
            }
        }
         if (service.getAvailableCount() < REPLENISH_AMOUNT && service.getAvailableCount() < totalIdsToGenerate) {
            System.err.println("Warning: Concurrency test might be slow or unstable due to insufficient initial primes. Available: " + service.getAvailableCount());
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
