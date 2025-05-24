package org.example.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue; // Added import
import java.util.concurrent.BlockingQueue; // Added import
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
// import java.util.concurrent.CountDownLatch; // To be removed
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException; // Added for delete

public class SharedIDService {
    private static final Logger LOGGER = Logger.getLogger(SharedIDService.class.getName());

    private static final long LOWER_BOUND = 1_000_000_000L;
    // Old constants removed
    // private static final long INITIAL_PRE_GENERATE_COUNT = 20_000L; 
    // private static final long REPLENISH_THRESHOLD = 4_000L;    
    // private static final long REPLENISH_AMOUNT = 20_000L;     

    // New constants for BlockingQueue
    private static final int QUEUE_CAPACITY = 2048;
    private static final int QUEUE_LOW_WATER_MARK = QUEUE_CAPACITY / 4; // e.g., 512

    // Replaced available and active sets with idQueue
    // private final Set<Long> available = ConcurrentHashMap.newKeySet(); // Removed
    // private final Set<Long> active = ConcurrentHashMap.newKeySet(); // Removed
    private final BlockingQueue<Long> idQueue;

    private final ExecutorService primeGeneratorExecutor; // Orchestrator
    private final ExecutorService primeSearcherPool;    // Worker pool for parallel search
    private final int numPrimeSearcherThreads; 

    private volatile long nextLowerBoundForGeneration = LOWER_BOUND;
    private Future<?> lastGenerationTaskFuture; 

    // Fields for awaitable initial generation REMOVED
    // private final CountDownLatch initialGenerationLatch;
    // private volatile boolean initialGenerationPerformed = false;
    // private final Object initialGenLock = new Object();

    private SharedIDService() {
        this.idQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY); // Initialize idQueue

        // this.initialGenerationLatch = new CountDownLatch(1); // Removed latch initialization

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
        this.numPrimeSearcherThreads = localNumSearcherThreads; 
        this.primeSearcherPool = Executors.newFixedThreadPool(this.numPrimeSearcherThreads, primeSearcherThreadFactory); 

        // Start the prime generation orchestrator loop
        this.primeGeneratorExecutor.submit(this::primeGenerationOrchestrationLoop);

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

    // New primeGenerationOrchestrationLoop method
    private void primeGenerationOrchestrationLoop() {
        LOGGER.info("Prime generation orchestrator started.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                int currentQueueSize = idQueue.size();
                if (currentQueueSize < QUEUE_LOW_WATER_MARK) {
                    long primesToGenerate = QUEUE_CAPACITY - currentQueueSize;
                    if (primesToGenerate <= 0) { 
                         LOGGER.fine("Queue size is " + currentQueueSize + ", no primes needed for now despite being below low water mark. Sleeping.");
                         Thread.sleep(500); 
                         continue;
                    }

                    LOGGER.info("Queue size (" + currentQueueSize + ") is below low water mark (" + QUEUE_LOW_WATER_MARK + "). Need to generate " + primesToGenerate + " primes.");

                    long batchStartBound = this.nextLowerBoundForGeneration;
                                                                        
                    int numSubTasks = this.numPrimeSearcherThreads;
                    long primesPerSubTask = (primesToGenerate + numSubTasks - 1) / numSubTasks; // Ceiling division

                    if (primesPerSubTask <= 0) {
                         LOGGER.warning("Calculated primesPerSubTask is " + primesPerSubTask + ", skipping generation cycle.");
                         Thread.sleep(500); 
                         continue;
                    }
                    
                    List<Future<Set<Long>>> subTaskFutures = new ArrayList<>();
                    LOGGER.fine("Orchestrator: Submitting " + numSubTasks + " sub-tasks to find approx. " + primesPerSubTask + " primes each, starting search from global bound " + batchStartBound);

                    for (int i = 0; i < numSubTasks; i++) {
                        PrimeNumberGeneratorTask subTask = new PrimeNumberGeneratorTask(batchStartBound, primesPerSubTask);
                        subTaskFutures.add(primeSearcherPool.submit(subTask));
                    }

                    Set<Long> allNewPrimesForThisBatch = new HashSet<>();
                    long maxPrimeFoundInThisBatch = 0L;

                    for (Future<Set<Long>> future : subTaskFutures) {
                        try {
                            Set<Long> primesFromSubTask = future.get();
                            if (primesFromSubTask != null && !primesFromSubTask.isEmpty()) {
                                allNewPrimesForThisBatch.addAll(primesFromSubTask);
                                for (long p : primesFromSubTask) {
                                    if (p > maxPrimeFoundInThisBatch) maxPrimeFoundInThisBatch = p;
                                }
                            }
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "Orchestrator's prime generation sub-task was interrupted.", e);
                            Thread.currentThread().interrupt(); 
                            break; 
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Exception in prime generation sub-task.", e);
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        LOGGER.warning("Orchestrator thread interrupted, breaking generation loop.");
                        break; 
                    }

                    if (!allNewPrimesForThisBatch.isEmpty()) {
                        LOGGER.info("Batch generated " + allNewPrimesForThisBatch.size() + " unique primes. Max prime in batch: " + maxPrimeFoundInThisBatch + ". Adding to queue...");
                        int addedCount = 0;
                        for (Long prime : allNewPrimesForThisBatch) { 
                            if (Thread.currentThread().isInterrupted()) {
                                LOGGER.warning("Orchestrator interrupted while adding primes to queue.");
                                break;
                            }
                            idQueue.put(prime); 
                            addedCount++;
                        }
                        LOGGER.info("Added " + addedCount + " primes to queue. New queue size: " + idQueue.size());
                    } else {
                        LOGGER.warning("Prime generation batch yielded no new unique primes. Search started from: " + batchStartBound);
                    }
                    
                    if (maxPrimeFoundInThisBatch >= batchStartBound) {
                        this.nextLowerBoundForGeneration = maxPrimeFoundInThisBatch + 1;
                        if (this.nextLowerBoundForGeneration % 2 == 0) this.nextLowerBoundForGeneration++;
                    } else {
                        long estimatedRangeSearchedTotal = primesToGenerate * 20; 
                        this.nextLowerBoundForGeneration = batchStartBound + estimatedRangeSearchedTotal;
                        if (this.nextLowerBoundForGeneration % 2 == 0) this.nextLowerBoundForGeneration++;
                        LOGGER.warning("Max prime in batch was not greater than start bound. Advancing next global lower bound heuristically to " + this.nextLowerBoundForGeneration);
                    }
                    LOGGER.info("Next global lower bound for prime generation updated to: " + this.nextLowerBoundForGeneration);

                } else {
                    LOGGER.fine("Queue is sufficiently full (size: " + currentQueueSize + "). Orchestrator sleeping.");
                    Thread.sleep(500); 
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Prime generation orchestrator thread interrupted. Shutting down.");
            Thread.currentThread().interrupt(); 
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled exception in prime generation orchestrator loop. Loop will terminate.", e);
        } finally {
            LOGGER.info("Prime generation orchestrator loop finished.");
        }
    }
    
    // Removed schedulePrimeGeneration method and awaitInitialGeneration method

    // public void awaitInitialGeneration() throws InterruptedException { // REMOVED
    //     if (!initialGenerationPerformed) { 
    //         LOGGER.info("Waiting for initial prime generation to complete...");
    //     }
    //     initialGenerationLatch.await();
    //     LOGGER.info("Initial prime generation confirmed complete (or was already done). Proceeding.");
    // }

    public long getNew() throws InterruptedException { // Removed synchronized
        // Remove old logic related to 'available' set, 'active' set, wait/notify, and replenishment checks.
        // The new logic is much simpler:
        try {
            // LOGGER.info("Attempting to take ID from queue. Queue size: " + idQueue.size()); // Optional: for debugging
            Long id = idQueue.take(); // Blocks if queue is empty
            // LOGGER.info("ID " + id + " taken from queue. New queue size: " + idQueue.size()); // Optional: for debugging
            
            // The 'active' set was removed. If distinct tracking of active IDs is needed later,
            // it would be managed here, e.g., by adding to a concurrent 'activeIDs' set.
            // For now, no 'active' set management.

            // Replenishment is no longer triggered here. It's handled by the background orchestrator
            // monitoring idQueue.size() against QUEUE_LOW_WATER_MARK.
            
            return id;
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "getNew() was interrupted while waiting to take ID from queue.", e);
            Thread.currentThread().interrupt(); // Preserve interrupt status
            // Depending on desired behavior, could re-throw a custom runtime exception or return a specific value.
            // For now, re-throwing as InterruptedException is fine as per original signature.
            throw e; 
        }
    }

    public void delete(long id) { // Removed synchronized
        boolean offered = idQueue.offer(id); // Non-blocking, adds if space allows
        if (offered) {
            LOGGER.fine("ID " + id + " returned to queue. Queue size: " + idQueue.size());
        } else {
            LOGGER.warning("Could not return ID " + id + " to queue (it might be full). ID is dropped.");
        }
    }

    // Method to check available size, useful for testing or monitoring
    public int getAvailableCount() {
        return idQueue.size(); // Reflects items in queue
    }

    // Method to check active size, useful for testing or monitoring
    // public int getActiveCount() { // REMOVED
    //     return 0; 
    // }

    // Method to get current nextLowerBoundForGeneration, useful for testing
    public long getNextLowerBoundForGeneration() {
        return nextLowerBoundForGeneration;
    }

    // Method to check if initial generation is done (useful for testing)
    // public boolean isInitialGenerationPerformed() { // REMOVED
    //     return initialGenerationPerformed;
    // }
}
