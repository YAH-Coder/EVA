package org.example.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.example.utils.StatisticsService; // Restoring import
import org.example.utils.sieve.JavaSegmentedSieveGenerator;
import org.example.utils.sieve.PrimeSegmentGenerator;
import java.util.logging.Logger;

/**
 * Manages the generation and distribution of unique, 10-digit prime IDs in a multithreaded environment.
 *
 * <p><b>Architectural Overview:</b></p>
 * <p>
 * The {@code SharedIDService} employs a sophisticated producer-consumer pattern to efficiently provide prime IDs.
 * It is designed to minimize contention and latency when requesting new IDs, primarily through pre-generation
 * and a queuing mechanism.
 * </p>
 * <ul>
 *   <li><b>Producer-Consumer with BlockingQueue:</b>
 *     <ul>
 *       <li>Prime IDs are generated asynchronously and stored in a {@link java.util.concurrent.BlockingQueue} (specifically, an {@link java.util.concurrent.ArrayBlockingQueue}).
 *       <li>Consumers (various service classes like {@code CustomerService}, {@code EventService}, {@code TicketService}) request IDs via the {@link #getNew()} method, which takes an ID from the queue. If the queue is empty, the call blocks until an ID becomes available.
 *       <li>When an ID is no longer in use (e.g., an entity is deleted), it can be returned to the queue via the {@link #delete(long)} method, making it available for reuse if the queue has space.
 *     </ul>
 *   </li>
 *   <li><b>Prime Generation Orchestration (Producer Side):</b>
 *     <ul>
 *       <li>A dedicated single-thread executor, {@code primeGeneratorExecutor}, acts as an orchestrator. It runs the {@code primeGenerationOrchestrationLoop}.
 *       <li>This loop monitors the size of the {@code idQueue}. If the number of available IDs falls below a {@link #QUEUE_LOW_WATER_MARK}, it triggers new prime generation.
 *     </ul>
 *   </li>
 *   <li><b>Parallel Prime Searching (Sieve Workers):</b>
 *     <ul>
 *       <li>The actual prime number generation is performed by a pool of worker threads, {@code primeSearcherPool}.
 *       <li>The orchestrator divides the search space (large number ranges) into segments. Each segment is processed by a task submitted to the {@code primeSearcherPool}.
 *       <li>These tasks use a {@link org.example.utils.sieve.PrimeSegmentGenerator} (specifically, {@link org.example.utils.sieve.JavaSegmentedSieveGenerator}) which implements the Sieve of Eratosthenes algorithm optimized for segments. This allows for efficient prime finding in large ranges.
 *       <li>The {@code nextSieveSegmentStart} {@link java.util.concurrent.atomic.AtomicLong} ensures that different worker threads process distinct segments of the number line, starting from {@code LOWER_BOUND}.
 *     </ul>
 *   </li>
 *   <li><b>Performance and Scalability:</b>
 *     <ul>
 *       <li>Pre-generating IDs and storing them in a queue significantly reduces the time taken by {@link #getNew()}, as it often involves a quick queue retrieval rather than on-demand prime generation.
 *       <li>The use of a fixed-size thread pool ({@code primeSearcherPool}) allows for parallel computation of primes, leveraging multi-core processors to speed up the generation process.
 *       <li>The segmented sieve approach is more memory-efficient than a simple sieve for very large ranges.
 *       <li>Backpressure is naturally handled: if the {@code idQueue} is full, the orchestrator's attempts to {@code put()} new primes will block, preventing excessive memory usage from over-generation.
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * The service is implemented as a singleton, accessible via {@link #getInstance()}. It also includes a shutdown hook
 * to gracefully terminate its internal thread pools when the application exits.
 * </p>
 */
public class SharedIDService {
    private static final Logger LOGGER = Logger.getLogger(SharedIDService.class.getName());

    private static final long LOWER_BOUND = 1_000_000_000L;

    public static final int QUEUE_CAPACITY = 4096;
    public static final int QUEUE_LOW_WATER_MARK = 2048;

    private final BlockingQueue<Long> idQueue;

    private final ExecutorService primeGeneratorExecutor;
    private final ExecutorService primeSearcherPool;
    private final int numPrimeSearcherThreads;

    private Future<?> lastGenerationTaskFuture;

    private final PrimeSegmentGenerator primeSieveGenerator;
    private final AtomicLong nextSieveSegmentStart;
    private final int segmentSize = 1_048_576;

    private SharedIDService() {
        this.idQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.primeSieveGenerator = new JavaSegmentedSieveGenerator();
        this.nextSieveSegmentStart = new AtomicLong(LOWER_BOUND);

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
        int localNumSearcherThreads = Math.max(1, availableProcessors);
        this.numPrimeSearcherThreads = localNumSearcherThreads;
        this.primeSearcherPool = Executors.newFixedThreadPool(this.numPrimeSearcherThreads, primeSearcherThreadFactory);

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

    /**
     * Returns the singleton instance of the SharedIDService.
     * This is the standard way to access the service.
     *
     * @return The single instance of {@code SharedIDService}.
     */
    public static SharedIDService getInstance() {
        return Holder.INSTANCE;
    }

    private void primeGenerationOrchestrationLoop() {
        LOGGER.info("Prime generation orchestrator started (segmented sieve model).");
        final int estimatedMaxPrimesPerSegment = this.segmentSize / 15;

        try {
            while (!Thread.currentThread().isInterrupted()) {
                int currentQueueSize = idQueue.size();
                if (currentQueueSize < QUEUE_LOW_WATER_MARK) {
                    long primesNeededApprox = QUEUE_CAPACITY - currentQueueSize;
                    LOGGER.info("Queue size (" + currentQueueSize + ") is below low water mark (" + QUEUE_LOW_WATER_MARK + "). Aiming to add approx. " + primesNeededApprox + " primes.");

                    int tasksToLaunch = this.numPrimeSearcherThreads;
                    List<Future<List<Long>>> subTaskFutures = new ArrayList<>(tasksToLaunch);

                    LOGGER.fine("Orchestrator: Submitting " + tasksToLaunch + " segment sieving tasks to PrimeSearcherPool.");

                    for (int i = 0; i < tasksToLaunch; i++) {
                        Callable<List<Long>> segmentSieveTask = () -> {
                            StatisticsService.getInstance().recordTaskExecution("PrimeSearchSegment", "PrimeSearcherPool", Thread.currentThread().getName()); // Uncommenting
                            long currentSegmentStart = nextSieveSegmentStart.getAndAdd(this.segmentSize);

                            LOGGER.fine("PrimeSearcher task starting: Segment [" + currentSegmentStart + " to " + (currentSegmentStart + this.segmentSize - 1) + "]");
                            long[] primesInSegmentArray = new long[estimatedMaxPrimesPerSegment];
                            int primeCountInSegment = 0;
                            try {
                                primeCountInSegment = this.primeSieveGenerator.generatePrimes(currentSegmentStart, this.segmentSize, primesInSegmentArray);
                            } catch (IllegalArgumentException e) {
                                LOGGER.log(Level.SEVERE, "Error generating primes for segment " + currentSegmentStart + ": " + e.getMessage(), e);
                                return Collections.emptyList();
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
                        if (Thread.currentThread().isInterrupted()) break;
                        try {
                            List<Long> primesFromSegment = future.get();
                            if (primesFromSegment != null && !primesFromSegment.isEmpty()) {
                                for (Long prime : primesFromSegment) {
                                    if (Thread.currentThread().isInterrupted()) break;
                                    idQueue.put(prime);
                                    totalPrimesAddedThisCycle++;
                                }
                            }
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "Orchestrator interrupted while waiting for future or putting prime to queue.", e);
                            Thread.currentThread().interrupt();
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

                } else {
                    LOGGER.fine("Queue is sufficiently full (size: " + currentQueueSize + "). Orchestrator sleeping.");
                    Thread.sleep(500);
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
    
    /**
     * Retrieves a new, unique prime ID from the pool of available IDs.
     * This method will block if the queue of pre-generated IDs is empty, waiting for the
     * background prime generation tasks to produce more IDs.
     *
     * @return A unique 10-digit prime number as a {@code long}.
     * @throws InterruptedException If the calling thread is interrupted while waiting for an ID to become available.
     */
    public long getNew() throws InterruptedException {
        try {
            Long id = idQueue.take();
            StatisticsService.getInstance().recordIdGenerated("SharedIDService"); // Uncommenting
            return id;
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "getNew() was interrupted while waiting to take ID from queue.", e);
            Thread.currentThread().interrupt();
            throw e; 
        }
    }

    /**
     * Returns a previously used ID to the pool, making it potentially available for future reuse.
     * The ID is offered to the internal queue. If the queue is full (which is unlikely if
     * {@link #QUEUE_CAPACITY} is large and consumption is steady), the ID might be dropped.
     * This method is non-blocking.
     *
     * @param id The prime ID to be returned.
     */
    public void delete(long id) {
        boolean offered = idQueue.offer(id);
        if (offered) {
            LOGGER.fine("ID " + id + " returned to queue. Queue size: " + idQueue.size());
        } else {
            LOGGER.warning("Could not return ID " + id + " to queue (it might be full). ID is dropped.");
        }
    }

    /**
     * Gets the current number of IDs available in the internal queue.
     * This can be used for monitoring or testing purposes.
     *
     * @return The number of IDs currently in the queue.
     */
    public int getAvailableCount() {
        return idQueue.size();
    }
}
