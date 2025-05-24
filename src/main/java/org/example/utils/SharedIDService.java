package org.example.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch; // Added import
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException; // Added for delete

public class SharedIDService {
    private static final Logger LOGGER = Logger.getLogger(SharedIDService.class.getName());

    private static final long LOWER_BOUND = 1_000_000_000L;
    private static final long INITIAL_PRE_GENERATE_COUNT = 20_000L; // Changed to 20,000L
    private static final long REPLENISH_THRESHOLD = 4_000L;    // Changed to 4,000L (20% of new REPLENISH_AMOUNT)
    private static final long REPLENISH_AMOUNT = 20_000L;     // Changed to 20,000L

    private final Set<Long> available = ConcurrentHashMap.newKeySet();
    private final Set<Long> active = ConcurrentHashMap.newKeySet();

    private final ExecutorService primeGeneratorExecutor; // Orchestrator
    private final ExecutorService primeSearcherPool;    // Worker pool for parallel search
    private final int numPrimeSearcherThreads; 

    private volatile long nextLowerBoundForGeneration = LOWER_BOUND;
    private Future<?> lastGenerationTaskFuture; 

    // Fields for awaitable initial generation
    private final CountDownLatch initialGenerationLatch;
    private volatile boolean initialGenerationPerformed = false;
    private final Object initialGenLock = new Object();

    private SharedIDService() {
        this.initialGenerationLatch = new CountDownLatch(1); // Initialize latch

        ThreadFactory primeGeneratorThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                t.setName("SharedIDService-PrimeGenerator");
                return t;
            }
        };
        this.primeGeneratorExecutor = Executors.newSingleThreadExecutor(primeGeneratorThreadFactory);

        ThreadFactory primeSearcherThreadFactory = new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                t.setName("SharedIDService-PrimeSearcher-" + counter++);
                return t;
            }
        };
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        // Changed calculation to use all available processors, with a minimum of 1.
        int localNumSearcherThreads = Math.max(1, availableProcessors);
        this.numPrimeSearcherThreads = localNumSearcherThreads; // Assign to field
        this.primeSearcherPool = Executors.newFixedThreadPool(this.numPrimeSearcherThreads, primeSearcherThreadFactory); // Use field

        schedulePrimeGeneration(INITIAL_PRE_GENERATE_COUNT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down primeGeneratorExecutor (orchestrator)...");
            primeGeneratorExecutor.shutdown();
            LOGGER.info("Shutting down primeSearcherPool (workers)...");
            primeSearcherPool.shutdown();
            try {
                if (!primeGeneratorExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    primeGeneratorExecutor.shutdownNow();
                    LOGGER.warning("primeGeneratorExecutor did not terminate in 30s.");
                }
                if (!primeSearcherPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    primeSearcherPool.shutdownNow();
                    LOGGER.warning("primeSearcherPool did not terminate in 30s.");
                }
            } catch (InterruptedException ex) {
                primeGeneratorExecutor.shutdownNow();
                primeSearcherPool.shutdownNow();
                Thread.currentThread().interrupt();
                LOGGER.severe("Shutdown sequence interrupted.");
            }
        }));
    }

    private static class Holder {
        private static final SharedIDService INSTANCE = new SharedIDService();
    }

    public static SharedIDService getInstance() {
        return Holder.INSTANCE;
    }

    private void schedulePrimeGeneration(long count) {
        // Removed the check for lastGenerationTaskFuture.isDone()
        // to allow tasks to queue up.
        
        // PrimeNumberGeneratorTask task = new PrimeNumberGeneratorTask(nextLowerBoundForGeneration, count); // Moved inside lambda
        // LOGGER.info("Scheduling prime generation from " + nextLowerBoundForGeneration + " for " + count + " primes."); // Moved inside lambda

        // Storing the future is still useful if we want to inspect it, but it's not used to prevent scheduling.
        this.lastGenerationTaskFuture = primeGeneratorExecutor.submit(() -> {
            long currentGlobalBatchStartBound = nextLowerBoundForGeneration;
            long countToGenerateInThisBatch = count; // count is the parameter to schedulePrimeGeneration

            LOGGER.info("Orchestrating prime generation batch. Target count: " + countToGenerateInThisBatch + ", Starting search from lower bound: " + currentGlobalBatchStartBound);

            int numSubTasks = this.numPrimeSearcherThreads; 
            long primesToFindPerSubTask = (countToGenerateInThisBatch + numSubTasks - 1) / numSubTasks;

            if (primesToFindPerSubTask <= 0) {
                LOGGER.warning("primesToFindPerSubTask is zero or negative (" + primesToFindPerSubTask + "), skipping generation for this batch.");
                return;
            }

            List<Future<Set<Long>>> subTaskFutures = new ArrayList<>();
            for (int i = 0; i < numSubTasks; i++) {
                // Each sub-task gets the same starting lower bound.
                // PrimeNumberGeneratorTask internally handles finding 'primesToFindPerSubTask' new primes for itself.
                PrimeNumberGeneratorTask subTask = new PrimeNumberGeneratorTask(currentGlobalBatchStartBound, primesToFindPerSubTask);
                subTaskFutures.add(primeSearcherPool.submit(subTask));
                LOGGER.fine("Submitted sub-task " + i + " to PrimeSearcherPool to find " + primesToFindPerSubTask + " primes from " + currentGlobalBatchStartBound);
            }

            Set<Long> allNewPrimesForThisBatch = new HashSet<>();
            long maxPrimeFoundInThisBatch = 0L;

            for (Future<Set<Long>> future : subTaskFutures) {
                try {
                    Set<Long> primesFromSubTask = future.get();
                    if (primesFromSubTask != null && !primesFromSubTask.isEmpty()) {
                        allNewPrimesForThisBatch.addAll(primesFromSubTask);
                        for (long p : primesFromSubTask) {
                            if (p > maxPrimeFoundInThisBatch) {
                                maxPrimeFoundInThisBatch = p;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Prime generation sub-task was interrupted.", e);
                    Thread.currentThread().interrupt(); // Preserve interrupt status for the orchestrator thread
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception in prime generation sub-task.", e);
                }
            }
            
            if (Thread.currentThread().isInterrupted()) {
                LOGGER.warning("Orchestrator thread interrupted, abandoning current prime generation batch.");
                return;
            }

            if (!allNewPrimesForThisBatch.isEmpty()) {
                synchronized (SharedIDService.this) {
                    available.addAll(allNewPrimesForThisBatch);
                    LOGGER.info("Completed generation batch. Added " + allNewPrimesForThisBatch.size() + " new unique primes. Total available: " + available.size());
                    SharedIDService.this.notifyAll();
                }
            } else {
                LOGGER.warning("Prime generation batch yielded no new primes. Search started from: " + currentGlobalBatchStartBound);
            }

            // Latch release logic for initial generation
            synchronized (initialGenLock) {
                if (!initialGenerationPerformed && countToGenerateInThisBatch == INITIAL_PRE_GENERATE_COUNT) {
                    initialGenerationLatch.countDown();
                    initialGenerationPerformed = true;
                    LOGGER.info("Initial prime generation (for " + INITIAL_PRE_GENERATE_COUNT + " primes) complete. Latch released.");
                }
            }
            
            if (maxPrimeFoundInThisBatch >= currentGlobalBatchStartBound) {
                nextLowerBoundForGeneration = maxPrimeFoundInThisBatch + 1;
                if (nextLowerBoundForGeneration % 2 == 0) {
                    nextLowerBoundForGeneration++;
                }
            } else {
                long estimatedRangeSearched = countToGenerateInThisBatch * 20; 
                nextLowerBoundForGeneration = currentGlobalBatchStartBound + estimatedRangeSearched;
                if (nextLowerBoundForGeneration % 2 == 0) {
                    nextLowerBoundForGeneration++;
                }
                LOGGER.warning("Max prime in batch (" + maxPrimeFoundInThisBatch + ") was not greater than start bound (" + currentGlobalBatchStartBound + "). Advancing next global lower bound heuristically to " + nextLowerBoundForGeneration);
            }
            LOGGER.info("Next global lower bound for prime generation updated to: " + nextLowerBoundForGeneration);
        });
    }

    public void awaitInitialGeneration() throws InterruptedException {
        if (!initialGenerationPerformed) { // Quick check to avoid waiting if already done
            LOGGER.info("Waiting for initial prime generation to complete...");
        }
        initialGenerationLatch.await();
        LOGGER.info("Initial prime generation confirmed complete (or was already done). Proceeding.");
    }

    public synchronized long getNew() throws InterruptedException {
        while (available.isEmpty()) {
            LOGGER.info("No IDs available, waiting for prime generation...");
            wait(); // Wait for primes to be generated and notifyAll() to be called
        }

        Long id = available.iterator().next(); // Get an ID
        available.remove(id);
        active.add(id);
        LOGGER.info("ID " + id + " provided. Available count: " + available.size() + ", Active count: " + active.size());

        if (available.size() < REPLENISH_THRESHOLD) {
            LOGGER.info("Available IDs (" + available.size() + ") below threshold (" + REPLENISH_THRESHOLD + "). Scheduling replenishment.");
            schedulePrimeGeneration(REPLENISH_AMOUNT);
        }
        return id;
    }

    public synchronized void delete(long id) {
        if (!active.remove(id)) {
            throw new NoSuchElementException("ID " + id + " not found in active set.");
        }
        available.add(id); // Make ID available again
        LOGGER.info("ID " + id + " deleted and returned to available pool. Available count: " + available.size());
        notifyAll(); // Notify threads in getNew() as an ID is now available
    }

    // Method to check available size, useful for testing or monitoring
    public int getAvailableCount() {
        return available.size();
    }

    // Method to check active size, useful for testing or monitoring
    public int getActiveCount() {
        return active.size();
    }

    // Method to get current nextLowerBoundForGeneration, useful for testing
    public long getNextLowerBoundForGeneration() {
        return nextLowerBoundForGeneration;
    }

    // Method to check if initial generation is done (useful for testing)
    public boolean isInitialGenerationPerformed() {
        return initialGenerationPerformed;
    }
}
