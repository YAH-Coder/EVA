package org.example.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

// Note on Test Isolation:
// IDServiceParallel is a singleton. Its state (available IDs, next prime start point)
// persists across test method executions within the same JVM run.
// This can affect tests, especially high-volume ones, if they are sensitive to the initial
// state of the ID pool. For true isolation, IDServiceParallel would need a reset mechanism
// or tests would need to re-initialize the singleton (e.g., via reflection if in the same package),
// which is beyond the scope of typical unit testing for singletons.
// These tests are designed to stress concurrency aspects assuming the singleton behaves correctly
// even if its initial state is affected by prior tests. Consider running tests in isolation
// or implementing reset capabilities for more deterministic behavior in complex scenarios.
public class IDServiceParallelConcurrencyTest {

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS) // Timeout to prevent test hanging indefinitely
    void testUniqueIdGenerationConcurrently() throws InterruptedException {
        IDServiceParallel idService = IDServiceParallel.getInstance();
        int numThreads = 20;
        int idsPerThread = 500; // Reduced from 1000 to make test faster and less memory intensive
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        Set<Long> allGeneratedIds = ConcurrentHashMap.newKeySet();
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        allGeneratedIds.add(idService.getNew());
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
        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS); // Wait for graceful shutdown

        long endTime = System.currentTimeMillis();
        System.out.println("testUniqueIdGenerationConcurrently duration: " + (endTime - startTime) + " ms");


        assertFalse(exceptionOccurred.get(), "Exception occurred in one of the threads.");
        assertTrue(terminated, "Executor service did not terminate in time.");
        assertEquals(numThreads * idsPerThread, allGeneratedIds.size(),
                "Number of unique IDs should match expected. Duplicates might exist or IDs lost.");
        System.out.println("Total unique IDs generated: " + allGeneratedIds.size());
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS) // Increased timeout
    void testConcurrentGetAndDeletes() throws InterruptedException {
        IDServiceParallel idService = IDServiceParallel.getInstance();
        int initialIdCount = 2000; // Number of IDs to pre-generate
        int numGetterThreads = 10;
        int numDeleterThreads = 10;
        int idsToGetPerGetter = 200; // Reduced for performance
        // Deleters will try to delete from the initial set + what getters might add (best effort)

        Set<Long> initialIds = ConcurrentHashMap.newKeySet();
        System.out.println("Generating initial IDs for delete test...");
        for (int i = 0; i < initialIdCount; i++) {
            try {
                initialIds.add(idService.getNew());
            } catch (Exception e) {
                fail("Failed to generate initial IDs: " + e.getMessage());
            }
        }
        System.out.println("Initial IDs generated: " + initialIds.size());
        assertEquals(initialIdCount, initialIds.size(), "Should have generated all initial IDs.");


        ExecutorService executor = Executors.newFixedThreadPool(numGetterThreads + numDeleterThreads);
        CountDownLatch latch = new CountDownLatch(numGetterThreads + numDeleterThreads);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        Set<Long> concurrentlyAddedIds = ConcurrentHashMap.newKeySet();
        Set<Long> idsToDelete = ConcurrentHashMap.newKeySet();
        idsToDelete.addAll(initialIds); // Copy initial IDs for deletion attempts


        long startTime = System.currentTimeMillis();

        // Getter threads
        for (int i = 0; i < numGetterThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsToGetPerGetter; j++) {
                        long newId = idService.getNew();
                        concurrentlyAddedIds.add(newId);
                        // Occasionally add some of these new IDs to the deletion pool
                        // to simulate more dynamic get/delete scenarios.
                        if (j % 10 == 0) {
                            idsToDelete.add(newId);
                        }
                    }
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Deleter threads
        // Convert Set to List to allow threads to pick distinct IDs for deletion more easily
        // This is still best-effort to avoid contention on the same ID by multiple deleters.
        Object[] deletableIdsArray = idsToDelete.toArray(); // Snapshot of IDs to attempt deletion
        for (int i = 0; i < numDeleterThreads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    // Each deleter thread attempts to delete a subset of IDs
                    // This is a simplified approach. A more robust one might involve a shared queue of IDs to delete.
                    for (int j = 0; j < deletableIdsArray.length / numDeleterThreads; j++) {
                        int idIndex = (threadIdx * (deletableIdsArray.length / numDeleterThreads)) + j;
                        if (idIndex < deletableIdsArray.length) {
                           Long idToDelete = (Long) deletableIdsArray[idIndex];
                            try {
                                idService.delete(idToDelete);
                                // System.out.println("Thread " + Thread.currentThread().getId() + " deleted ID: " + idToDelete);
                            } catch (Exception e) { // Catch NoSuchElementException if ID already deleted or never existed
                                // This is expected in a concurrent scenario, so not necessarily a failure
                                // System.err.println("Thread " + Thread.currentThread().getId() + " failed to delete ID " + idToDelete + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) { // Catch unexpected exceptions
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        boolean terminated = executor.awaitTermination(60, TimeUnit.SECONDS); // Increased wait time

        long endTime = System.currentTimeMillis();
        System.out.println("testConcurrentGetAndDeletes duration: " + (endTime - startTime) + " ms");

        assertFalse(exceptionOccurred.get(), "Unexpected exception occurred during get/delete operations.");
        assertTrue(terminated, "Executor service did not terminate in time for get/delete test.");
        System.out.println("Get/Delete test completed. Stability and absence of exceptions are primary checks.");
        // Verifying exact counts here is complex due to concurrent nature.
        // A more advanced test might involve checking the internal state of IDServiceParallel
        // or ensuring that a certain percentage of delete operations were successful.
        // For now, the main assertion is that no unexpected exceptions were thrown.
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS) // Generous timeout for high volume
    void testHighVolumeIdGenerationAndReplenishment() throws InterruptedException {
        IDServiceParallel idService = IDServiceParallel.getInstance();
        int numClientThreads = 50; // High number of threads
        int idsToRequestPerThread = 5000; // High number of IDs per thread
        // REPLENISH_AMOUNT in IDServiceParallel is 100. This will trigger many replenishments.
        long expectedTotalUniqueIds = (long) numClientThreads * idsToRequestPerThread;

        ExecutorService clientExecutor = Executors.newFixedThreadPool(numClientThreads);
        CountDownLatch latch = new CountDownLatch(numClientThreads);
        Set<Long> allGeneratedIds = ConcurrentHashMap.newKeySet();
        AtomicBoolean testFailed = new AtomicBoolean(false);

        System.out.println("Starting high-volume test: " + numClientThreads + " threads, "
                + idsToRequestPerThread + " IDs each. Total: " + expectedTotalUniqueIds);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numClientThreads; i++) {
            clientExecutor.submit(() -> {
                try {
                    for (int j = 0; j < idsToRequestPerThread; j++) {
                        long id = idService.getNew();
                        allGeneratedIds.add(id);
                        if (Thread.currentThread().isInterrupted()) {
                            System.err.println("Client thread interrupted, stopping ID generation for this thread.");
                            testFailed.set(true);
                            break; 
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Exception in client thread: " + e.getMessage());
                    e.printStackTrace();
                    testFailed.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completedInTime = latch.await(170, TimeUnit.SECONDS); // Slightly less than overall timeout

        long endTime = System.currentTimeMillis();
        System.out.println("High-volume test main phase duration: " + (endTime - startTime) + " ms");
        System.out.println("Current unique IDs generated: " + allGeneratedIds.size());


        clientExecutor.shutdown();
        boolean terminatedGracefully = clientExecutor.awaitTermination(10, TimeUnit.SECONDS);
        if (!terminatedGracefully) {
            System.err.println("Client executor did not terminate gracefully. Forcing shutdown.");
            clientExecutor.shutdownNow();
        }
        
        assertFalse(testFailed.get(), "One or more client threads failed with an exception.");
        assertTrue(completedInTime, "Not all client threads completed in the allotted time.");
        assertEquals(expectedTotalUniqueIds, allGeneratedIds.size(),
                "Ensure all requested IDs were generated and are unique. Current count: " + allGeneratedIds.size());
        System.out.println("High-volume test successfully generated " + allGeneratedIds.size() + " unique IDs.");
    }
}
