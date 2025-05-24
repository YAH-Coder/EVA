package org.example.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections; // Added import
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable; // Added import
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException; // Added import
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
// import java.util.concurrent.CountDownLatch; // To be removed
import java.util.concurrent.ThreadFactory;
import org.example.utils.StatisticsService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.example.utils.sieve.JavaSegmentedSieveGenerator;
import org.example.utils.sieve.PrimeSegmentGenerator;
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
    public static final int QUEUE_CAPACITY = 4096; // Updated and made public
    public static final int QUEUE_LOW_WATER_MARK = 2048; // Updated (QUEUE_CAPACITY / 2) and made public

    // Replaced available and active sets with idQueue
    // private final Set<Long> available = ConcurrentHashMap.newKeySet(); // Removed
    // private final Set<Long> active = ConcurrentHashMap.newKeySet(); // Removed
    private final BlockingQueue<Long> idQueue;

    private final ExecutorService primeGeneratorExecutor; // Orchestrator
    private final ExecutorService primeSearcherPool;    // Worker pool for parallel search
    private final int numPrimeSearcherThreads; 

    // private volatile long nextLowerBoundForGeneration = LOWER_BOUND; // Removed old field
    private Future<?> lastGenerationTaskFuture; // This might be repurposed or removed if not used by new orchestrator logic

    // New fields for Sieve-based generation
    private final PrimeSegmentGenerator primeSieveGenerator;
    private final AtomicLong nextSieveSegmentStart;
    private final int segmentSize = 1_048_576; // Default segment size (2^20)

    // Fields for awaitable initial generation REMOVED
    // private final CountDownLatch initialGenerationLatch;
    // private volatile boolean initialGenerationPerformed = false;
    // private final Object initialGenLock = new Object();

    private SharedIDService() {
        this.idQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY); // Initialize idQueue
        this.primeSieveGenerator = new JavaSegmentedSieveGenerator(); // Initialize Sieve Generator
        this.nextSieveSegmentStart = new AtomicLong(LOWER_BOUND); // Initialize Sieve Start (using existing LOWER_BOUND)

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
        LOGGER.info("Prime generation orchestrator started (segmented sieve model).");
        // Estimate for allocating array within Callable, segmentSize is 1,048,576
        // Max prime density for 10-digit numbers (around 1/ln(10^9) ~ 1/20.7).
        // So, segmentSize / 20 is approx 50k. Add some buffer. Let's use segmentSize / 15.
        final int estimatedMaxPrimesPerSegment = this.segmentSize / 15; 

        try {
            while (!Thread.currentThread().isInterrupted()) {
                int currentQueueSize = idQueue.size();
                if (currentQueueSize < QUEUE_LOW_WATER_MARK) {
                    long primesNeededApprox = QUEUE_CAPACITY - currentQueueSize;
                    LOGGER.info("Queue size (" + currentQueueSize + ") is below low water mark (" + QUEUE_LOW_WATER_MARK + "). Aiming to add approx. " + primesNeededApprox + " primes.");

                    int tasksToLaunch = this.numPrimeSearcherThreads; // Launch one segment-processing task per searcher thread
                    List<Future<List<Long>>> subTaskFutures = new ArrayList<>(tasksToLaunch);

                    LOGGER.fine("Orchestrator: Submitting " + tasksToLaunch + " segment sieving tasks to PrimeSearcherPool.");

                    for (int i = 0; i < tasksToLaunch; i++) {
                        Callable<List<Long>> segmentSieveTask = () -> {
                            StatisticsService.getInstance().recordTaskExecution("PrimeSearchSegment", "PrimeSearcherPool", Thread.currentThread().getName());
                            long currentSegmentStart = nextSieveSegmentStart.getAndAdd(this.segmentSize);
                            // Ensure segmentStart does not exceed a max value if we have an upper bound for 10-digit primes (e.g., 9,999,999,999)
                            // For now, assume it can grow indefinitely and filtering of non-10-digit happens elsewhere or by context.
                            // SharedIDService is meant for 10-digit primes (>= 1,000,000,000)

                            LOGGER.fine("PrimeSearcher task starting: Segment [" + currentSegmentStart + " to " + (currentSegmentStart + this.segmentSize - 1) + "]");
                            long[] primesInSegmentArray = new long[estimatedMaxPrimesPerSegment];
                            int primeCountInSegment = 0;
                            try {
                                primeCountInSegment = this.primeSieveGenerator.generatePrimes(currentSegmentStart, this.segmentSize, primesInSegmentArray);
                            } catch (IllegalArgumentException e) {
                                // This might happen if outPrimes is too small, log and return empty.
                                LOGGER.log(Level.SEVERE, "Error generating primes for segment " + currentSegmentStart + ": " + e.getMessage(), e);
                                return Collections.emptyList(); // Return empty list on error
                            }
                            
                            List<Long> resultList = new ArrayList<>(primeCountInSegment);
                            for (int j = 0; j < primeCountInSegment; j++) {
                                resultList.add(primesInSegmentArray[j]);
                            }
                            LOGGER.fine("PrimeSearcher task finished: Segment [" + currentSegmentStart + "]. Found " + primeCountInSegment + " primes.");
                            return resultList;
                        };
                        subTaskFutures.add(primeSearcherPool.submit(segmentSieveTask));
                    }

                    int totalPrimesAddedThisCycle = 0;
                    for (Future<List<Long>> future : subTaskFutures) {
                        if (Thread.currentThread().isInterrupted()) break; // Orchestrator interrupted
                        try {
                            List<Long> primesFromSegment = future.get(); // Wait for each segment to be processed
                            if (primesFromSegment != null && !primesFromSegment.isEmpty()) {
                                for (Long prime : primesFromSegment) {
                                    if (Thread.currentThread().isInterrupted()) break;
                                    idQueue.put(prime); // Blocks if queue is full; handles backpressure
                                    totalPrimesAddedThisCycle++;
                                }
                            }
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "Orchestrator interrupted while waiting for future or putting prime to queue.", e);
                            Thread.currentThread().interrupt(); // Preserve interrupt status
                            break; 
                        } catch (ExecutionException e) {
                            LOGGER.log(Level.SEVERE, "Exception in prime generation sub-task (retrieved via Future).", e.getCause());
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        LOGGER.warning("Orchestrator thread interrupted during result processing, breaking generation loop.");
                        break; 
                    }
                    if (totalPrimesAddedThisCycle > 0) {
                        LOGGER.info("Orchestration cycle complete. Added " + totalPrimesAddedThisCycle + " primes to queue. New queue size: " + idQueue.size());
                    } else {
                        LOGGER.info("Orchestration cycle complete. No new primes were added to the queue.");
                    }

                } else { // Queue is sufficiently full
                    LOGGER.fine("Queue is sufficiently full (size: " + currentQueueSize + "). Orchestrator sleeping.");
                    Thread.sleep(500); // Polling interval when queue is full
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Prime generation orchestrator thread interrupted. Loop terminating.");
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
            StatisticsService.getInstance().recordIdGenerated("SharedIDService");
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
    // public long getNextLowerBoundForGeneration() { // To be replaced by nextSieveSegmentStart.get()
    //     return nextLowerBoundForGeneration;
    // }

    // Method to check if initial generation is done (useful for testing)
    // public boolean isInitialGenerationPerformed() { // REMOVED
    //     return initialGenerationPerformed;
    // }
}
