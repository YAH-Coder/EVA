package org.example.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.math.BigInteger; // Added for BigInteger
import org.junit.jupiter.api.MethodOrderer; // Added for @Order
import org.junit.jupiter.api.Order; // Added for @Order
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeEach; // Added for @BeforeEach

/**
 * Integration tests for the {@link SharedIDService}.
 * These tests verify the behavior of the SharedIDService, particularly focusing on
 * ID uniqueness, primality, concurrent access, and queue management.
 *
 * Note: {@code @TestMethodOrder(MethodOrderer.OrderAnnotation.class)} is used because
 * some tests (like {@code deterministicSamplingTest}) are sensitive to the state of the
 * singleton {@code SharedIDService} and rely on being run early.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SharedIDServiceIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(SharedIDServiceIntegrationTest.class.getName());
    private static final long MIN_ID_VALUE = 1_000_000_000L; // Lower bound for generated IDs

    private SharedIDService idService;

    @BeforeEach
    void setUp() {
        // Obtain the singleton instance before each test
        idService = SharedIDService.getInstance();
    }

    @Test
    @Order(2) // Run after deterministicSamplingTest, but before heavier tests.
    void testSingletonInstance() {
        SharedIDService instance1 = SharedIDService.getInstance(); // First call in this test
        SharedIDService instance2 = SharedIDService.getInstance(); // Second call
        // idService is already initialized via @BeforeEach
        assertSame(idService, instance1, "First call to getInstance() should return the same instance as from setUp.");
        assertSame(instance1, instance2, "Consecutive calls to getInstance() should return the same instance.");
    }

    @Test
    @Order(3) // Runs after deterministic and singleton check.
    void testGetNew_returnsUniquePrimeIDs_quickCheck() throws InterruptedException {
        Set<Long> retrievedIDs = new HashSet<>();
        int idsToRetrieve = 10;
        String testContext = "testGetNew_returnsUniquePrimeIDs_quickCheck";

        // Wait for the queue to have some elements
        int attempts = 0;
        final int maxAttempts = 20; // Max 10 seconds wait
        while (idService.getAvailableCount() < idsToRetrieve && attempts < maxAttempts) {
            LOGGER.info(String.format("[%s] Waiting for IDs. Available: %d, Needed: %d, Attempt: %d/%d",
                    testContext, idService.getAvailableCount(), idsToRetrieve, attempts + 1, maxAttempts));
            Thread.sleep(500);
            attempts++;
        }
        assertTrue(idService.getAvailableCount() >= idsToRetrieve,
                String.format("[%s] Not enough IDs generated in time. Available: %d, Needed: %d after %d attempts.",
                        testContext, idService.getAvailableCount(), idsToRetrieve, attempts));

        LOGGER.info(String.format("[%s] Starting ID retrieval. IDs to retrieve: %d. Available: %d",
                testContext, idsToRetrieve, idService.getAvailableCount()));

        List<Long> currentBatchIds = new ArrayList<>();
        for (int i = 0; i < idsToRetrieve; i++) {
            long id = idService.getNew();
            currentBatchIds.add(id);
            final int finalI = i;
            final long finalId = id;
            assertAll(
                () -> assertTrue(finalId >= MIN_ID_VALUE, String.format("[%s] ID %d (value: %d) should be >= %d. (Call %d/%d)", testContext, finalI+1, finalId, MIN_ID_VALUE, finalI+1, idsToRetrieve)),
                () -> assertTrue(BigInteger.valueOf(finalId).isProbablePrime(10), String.format("[%s] ID %d (value: %d) should be a probable prime. (Call %d/%d)", testContext, finalI+1, finalId, finalI+1, idsToRetrieve)),
                () -> assertTrue(retrievedIDs.add(finalId), String.format("[%s] ID %d (value: %d) should be unique. (Call %d/%d)", testContext, finalI+1, finalId, finalI+1, idsToRetrieve))
            );
            LOGGER.info(String.format("[%s] Retrieved and verified ID %d: %d", testContext, i + 1, id));
        }
        assertEquals(idsToRetrieve, retrievedIDs.size(), String.format("[%s] Number of unique IDs retrieved (%d) should match requested count (%d).", testContext, retrievedIDs.size(), idsToRetrieve));
        LOGGER.info(String.format("[%s] %d unique prime IDs retrieved successfully.", testContext, idsToRetrieve));
    }

    @Test
    @Order(4) // Runs after the quicker checks.
    void smokeTestSingleThreadedUniquenessAndPrimality() throws InterruptedException {
        List<Long> retrievedIdsList = new ArrayList<>();
        Set<Long> uniqueIdsSet = new HashSet<>();
        int numIdsToGenerate = 1000;
        String testContext = "smokeTestSingleThreadedUniquenessAndPrimality";

        int attempts = 0;
        final int maxAttempts = 60; // Max 30 seconds wait
        while (idService.getAvailableCount() < numIdsToGenerate && attempts < maxAttempts) {
            LOGGER.info(String.format("[%s] Waiting for IDs. Available: %d, Needed: %d, Attempt: %d/%d",
                    testContext, idService.getAvailableCount(), numIdsToGenerate, attempts + 1, maxAttempts));
            Thread.sleep(500);
            attempts++;
        }
        assertTrue(idService.getAvailableCount() >= numIdsToGenerate,
                String.format("[%s] Not enough IDs generated. Available: %d, Needed: %d after %d attempts.",
                        testContext, idService.getAvailableCount(), numIdsToGenerate, attempts));

        LOGGER.info(String.format("[%s] Starting ID retrieval: %d IDs.", testContext, numIdsToGenerate));
        for (int i = 0; i < numIdsToGenerate; i++) {
            retrievedIdsList.add(idService.getNew());
        }
        LOGGER.info(String.format("[%s] Finished retrieving %d IDs. Starting assertions.", testContext, numIdsToGenerate));

        assertAll(String.format("Verify %d IDs from %s", numIdsToGenerate, testContext),
            () -> assertEquals(numIdsToGenerate, retrievedIdsList.size(),
                    String.format("[%s] Should have retrieved %d IDs, but got %d.", testContext, numIdsToGenerate, retrievedIdsList.size())),
            () -> {
                for (int i = 0; i < retrievedIdsList.size(); i++) {
                    long id = retrievedIdsList.get(i);
                    final int finalI = i; // for lambda
                    final long finalId = id; // for lambda
                    assertAll(String.format("ID %d (value: %d) checks", finalI + 1, finalId),
                        () -> assertTrue(finalId >= MIN_ID_VALUE, String.format("[%s] ID %d (value: %d) must be >= %d.", testContext, finalI + 1, finalId, MIN_ID_VALUE)),
                        () -> assertTrue(BigInteger.valueOf(finalId).isProbablePrime(10), String.format("[%s] ID %d (value: %d) must be a probable prime.", testContext, finalI + 1, finalId))
                    );
                }
                LOGGER.info(String.format("[%s] Verified primality and lower bound for all %d IDs.", testContext, numIdsToGenerate));
            },
            () -> {
                uniqueIdsSet.addAll(retrievedIdsList);
                assertEquals(numIdsToGenerate, uniqueIdsSet.size(),
                        String.format("[%s] All %d IDs should be unique, but %d unique IDs found.", testContext, numIdsToGenerate, uniqueIdsSet.size()));
                LOGGER.info(String.format("[%s] Verified uniqueness for all %d IDs.", testContext, numIdsToGenerate));
            }
        );
        LOGGER.info(String.format("[%s] Smoke test completed successfully.", testContext));
    }

    @Test
    @Order(5) // Runs after single-threaded tests.
    void concurrencyTestMultiThreadedSafety() throws InterruptedException {
        final int NUM_THREADS = 4;
        final int IDS_PER_THREAD = 500;
        final int TOTAL_IDS_EXPECTED = NUM_THREADS * IDS_PER_THREAD;
        String testContext = "concurrencyTestMultiThreadedSafety";

        List<Long> collectedIds = Collections.synchronizedList(new ArrayList<>(TOTAL_IDS_EXPECTED));
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

        int attempts = 0;
        final int maxAttempts = 120; // Max 60 seconds wait
        while (idService.getAvailableCount() < TOTAL_IDS_EXPECTED && attempts < maxAttempts) {
            LOGGER.info(String.format("[%s] Waiting for IDs. Available: %d, Needed: %d, Attempt: %d/%d",
                    testContext, idService.getAvailableCount(), TOTAL_IDS_EXPECTED, attempts + 1, maxAttempts));
            Thread.sleep(500);
            attempts++;
        }
        assertTrue(idService.getAvailableCount() >= TOTAL_IDS_EXPECTED,
                String.format("[%s] Not enough IDs generated. Available: %d, Needed: %d after %d attempts.",
                        testContext, idService.getAvailableCount(), TOTAL_IDS_EXPECTED, attempts));

        LOGGER.info(String.format("[%s] Starting test: %d threads, %d IDs per thread.", testContext, NUM_THREADS, IDS_PER_THREAD));
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadNum = i + 1;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < IDS_PER_THREAD; j++) {
                        try {
                            collectedIds.add(idService.getNew());
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, String.format("[%s] Thread %d interrupted while getting new ID.", testContext, threadNum), e);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        boolean tasksCompleted = doneSignal.await(2, TimeUnit.MINUTES);
        assertTrue(tasksCompleted, String.format("[%s] Not all tasks completed within the 2-minute timeout period. Tasks remaining: %d", testContext, doneSignal.getCount()));
        
        executorService.shutdown();
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            LOGGER.warning(String.format("[%s] Executor service did not terminate gracefully after 30 seconds. Forcing shutdown.", testContext));
            executorService.shutdownNow();
        }
        LOGGER.info(String.format("[%s] All threads finished. Total IDs collected: %d", testContext, collectedIds.size()));

        assertAll(String.format("Verify %d IDs from %s", TOTAL_IDS_EXPECTED, testContext),
            () -> assertEquals(TOTAL_IDS_EXPECTED, collectedIds.size(),
                    String.format("[%s] Should have collected %d IDs, but got %d.", testContext, TOTAL_IDS_EXPECTED, collectedIds.size())),
            () -> {
                Set<Long> uniqueIds = new HashSet<>(collectedIds);
                assertEquals(TOTAL_IDS_EXPECTED, uniqueIds.size(),
                        String.format("[%s] All %d IDs should be unique, but %d unique IDs found.", testContext, TOTAL_IDS_EXPECTED, uniqueIds.size()));
                LOGGER.info(String.format("[%s] Verified uniqueness for all %d concurrently generated IDs.", testContext, TOTAL_IDS_EXPECTED));
            },
            () -> {
                for (int i = 0; i < collectedIds.size(); i++) {
                    long id = collectedIds.get(i);
                    final int finalI = i; // for lambda
                    final long finalId = id; // for lambda
                    assertAll(String.format("ID %d (value: %d) checks", finalI + 1, finalId),
                         () -> assertTrue(finalId >= MIN_ID_VALUE, String.format("[%s] ID %d (value: %d) must be >= %d.", testContext, finalI + 1, finalId, MIN_ID_VALUE)),
                         () -> assertTrue(BigInteger.valueOf(finalId).isProbablePrime(10), String.format("[%s] ID %d (value: %d) must be a probable prime.", testContext, finalI + 1, finalId))
                    );
                }
                LOGGER.info(String.format("[%s] Verified primality and lower bound for all %d concurrently generated IDs.", testContext, TOTAL_IDS_EXPECTED));
            }
        );
        LOGGER.info(String.format("[%s] Concurrency test completed successfully.", testContext));
    }

    @Test
    @Order(6) // Runs after concurrency tests.
    void blockingBehaviorEdgeCaseTest() throws InterruptedException {
        final long MAX_ACCEPTABLE_BLOCK_TIME_MS = 100L;
        final long MAX_ACCEPTABLE_BLOCK_TIME_NS = TimeUnit.MILLISECONDS.toNanos(MAX_ACCEPTABLE_BLOCK_TIME_MS);
        final int idsToConsume = SharedIDService.QUEUE_CAPACITY + 10;
        String testContext = "blockingBehaviorEdgeCaseTest";

        LOGGER.info(String.format("[%s] Starting test. Will consume %d IDs. Max block time per call: %d ms.",
                testContext, idsToConsume, MAX_ACCEPTABLE_BLOCK_TIME_MS));

        int initialWaitAttempts = 0;
        final int maxInitialWaitAttempts = 60; // Max 30s wait
        final int targetInitialQueueSize = SharedIDService.QUEUE_CAPACITY / 2;
        while (idService.getAvailableCount() < targetInitialQueueSize && initialWaitAttempts < maxInitialWaitAttempts) {
            LOGGER.info(String.format("[%s] Initial wait for IDs. Available: %d, Target: %d, Attempt: %d/%d",
                    testContext, idService.getAvailableCount(), targetInitialQueueSize, initialWaitAttempts + 1, maxInitialWaitAttempts));
            Thread.sleep(500);
            initialWaitAttempts++;
        }
        if (idService.getAvailableCount() < targetInitialQueueSize) {
            LOGGER.warning(String.format("[%s] Queue did not reach target %d capacity (actual: %d) before starting. Test might be less effective.",
                    testContext, targetInitialQueueSize, idService.getAvailableCount()));
        }

        List<Long> retrievedIds = new ArrayList<>(idsToConsume);
        List<String> slowCallMessages = new ArrayList<>();

        for (int i = 0; i < idsToConsume; i++) {
            long startTime = System.nanoTime();
            long id = -1;
            try {
                id = idService.getNew();
                retrievedIds.add(id);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, String.format("[%s] Test thread interrupted while calling getNew() on call %d/%d.", testContext, i + 1, idsToConsume), e);
                Thread.currentThread().interrupt();
                fail(String.format("[%s] Test was interrupted during getNew() call %d/%d", testContext, i + 1, idsToConsume));
                break;
            }
            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;

            if (durationNs > MAX_ACCEPTABLE_BLOCK_TIME_NS) {
                String message = String.format("[%s] getNew() call %d/%d (ID: %d) took too long: %.3f ms (Max: %d ms). Available queue: %d",
                        testContext, i + 1, idsToConsume, id, durationNs / 1_000_000.0, MAX_ACCEPTABLE_BLOCK_TIME_MS, idService.getAvailableCount());
                LOGGER.warning(message);
                slowCallMessages.add(message);
            } else if ((i + 1) % 100 == 0) {
                LOGGER.info(String.format("[%s] Call %d/%d (ID: %d) within time: %.3f ms. Available queue: %d",
                        testContext, i + 1, idsToConsume, id, durationNs / 1_000_000.0, idService.getAvailableCount()));
            }
        }

        assertTrue(slowCallMessages.isEmpty(),
                String.format("[%s] There were %d getNew() calls that exceeded max block time of %d ms. First few:\n%s",
                        testContext, slowCallMessages.size(), MAX_ACCEPTABLE_BLOCK_TIME_MS,
                        String.join("\n", slowCallMessages.stream().limit(5).toList())));

        assertEquals(idsToConsume, retrievedIds.size(), String.format("[%s] Should have retrieved %d IDs, but got %d.", testContext, idsToConsume, retrievedIds.size()));
        Set<Long> uniqueIds = new HashSet<>(retrievedIds);
        assertEquals(idsToConsume, uniqueIds.size(), String.format("[%s] All %d retrieved IDs should be unique, but found %d unique IDs.", testContext, idsToConsume, uniqueIds.size()));

        LOGGER.info(String.format("[%s] Test completed. All %d getNew() calls were within the acceptable time limit or issues logged.", testContext, idsToConsume));
    }

    @Test
    @Order(1) // Run this test absolutely first.
    void deterministicSamplingTest() throws InterruptedException {
        String testContext = "deterministicSamplingTest";
        // This test's success is sensitive to the execution order due to SharedIDService being a singleton
        // and its ID queue being populated asynchronously from construction.
        // It assumes that not too many IDs have been consumed by prior tests.
        // The Sieve generator starts from MIN_ID_VALUE.
        
        List<Long> expectedFirstTenPrimes = List.of(
            1000000007L, 1000000009L, 1000000021L, 1000000033L, 1000000087L,
            1000000093L, 1000000097L, 1000000103L, 1000000123L, 1000000181L
        );
        
        ArrayList<Long> retrievedIds = new ArrayList<>();
        int idsToRetrieve = 10;

        int attempts = 0;
        final int maxAttempts = 60; // Max 30 seconds wait
        while (idService.getAvailableCount() < idsToRetrieve && attempts < maxAttempts) {
            LOGGER.info(String.format("[%s] Waiting for initial %d IDs. Available: %d, Attempt: %d/%d",
                    testContext, idsToRetrieve, idService.getAvailableCount(), attempts + 1, maxAttempts));
            Thread.sleep(500);
            attempts++;
        }
        assertTrue(idService.getAvailableCount() >= idsToRetrieve,
                String.format("[%s] Not enough IDs generated. Available: %d, Needed: %d after %d attempts.",
                        testContext, idService.getAvailableCount(), idsToRetrieve, attempts));

        LOGGER.info(String.format("[%s] Retrieving %d IDs.", testContext, idsToRetrieve));
        for (int i = 0; i < idsToRetrieve; i++) {
            try {
                long id = idService.getNew();
                retrievedIds.add(id);
                LOGGER.info(String.format("[%s] Retrieved ID %d/%d: %d", testContext, i + 1, idsToRetrieve, id));
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, String.format("[%s] Test thread interrupted while calling getNew() on call %d/%d.", testContext, i + 1, idsToRetrieve), e);
                Thread.currentThread().interrupt();
                fail(String.format("[%s] Test was interrupted during getNew() call %d/%d", testContext, i + 1, idsToRetrieve));
                break;
            }
        }

        assertEquals(expectedFirstTenPrimes, retrievedIds,
                String.format("[%s] The first %d retrieved IDs (%s) did not match the expected sequence (%s). " +
                        "This can happen if other tests consumed IDs first.",
                        testContext, idsToRetrieve, retrievedIds, expectedFirstTenPrimes));
        LOGGER.info(String.format("[%s] Test completed successfully. Retrieved IDs match expected initial sequence.", testContext));
    }
}
