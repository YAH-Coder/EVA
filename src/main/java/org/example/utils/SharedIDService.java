package org.example.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory; // Added import
import java.util.concurrent.TimeUnit; // Added for awaitTermination
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException; // Added for delete

public class SharedIDService {
    private static final Logger LOGGER = Logger.getLogger(SharedIDService.class.getName());

    private static final long LOWER_BOUND = 1_000_000_000L;
    private static final long INITIAL_PRE_GENERATE_COUNT = 200;
    private static final int REPLENISH_THRESHOLD = 50;
    private static final int REPLENISH_AMOUNT = 100;

    private final Set<Long> available = ConcurrentHashMap.newKeySet();
    private final Set<Long> active = ConcurrentHashMap.newKeySet();

    private final ExecutorService primeGeneratorExecutor = 
        Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true); // Make the thread a daemon thread
                t.setName("SharedIDService-PrimeGenerator"); // Optional: give it a descriptive name
                return t;
            }
        });
    private volatile long nextLowerBoundForGeneration = LOWER_BOUND;
    private Future<?> lastGenerationTaskFuture; // To track the ongoing generation task

    private SharedIDService() {
        schedulePrimeGeneration(INITIAL_PRE_GENERATE_COUNT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down primeGeneratorExecutor...");
            primeGeneratorExecutor.shutdown();
            try {
                if (!primeGeneratorExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    primeGeneratorExecutor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                primeGeneratorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
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
            // This entire block is executed by the SingleThreadExecutor, ensuring serial access for these steps.
            long currentLowerBoundForThisTask = nextLowerBoundForGeneration; // Capture the bound AT THE TIME OF EXECUTION.
            
            // Log just before task creation/execution for clarity
            LOGGER.info("Starting prime generation task. Count: " + count + ", Current Lower Bound: " + currentLowerBoundForThisTask);
            PrimeNumberGeneratorTask actualTask = new PrimeNumberGeneratorTask(currentLowerBoundForThisTask, count);

            try {
                Set<Long> generatedPrimes = actualTask.call();
                if (generatedPrimes != null && !generatedPrimes.isEmpty()) {
                    synchronized (SharedIDService.this) {
                        available.addAll(generatedPrimes);
                        LOGGER.info("Generated " + generatedPrimes.size() + " primes. Available size: " + available.size() + ". Task started from: " + currentLowerBoundForThisTask);
                        SharedIDService.this.notifyAll(); 
                    }
                }

                // Update nextLowerBoundForGeneration based on THIS task's actual work
                if (actualTask.getLastPrimeGenerated() >= currentLowerBoundForThisTask) { 
                    nextLowerBoundForGeneration = actualTask.getLastPrimeGenerated() + 1;
                    if (nextLowerBoundForGeneration % 2 == 0) { 
                        nextLowerBoundForGeneration++;
                    }
                } else if ((generatedPrimes == null || generatedPrimes.isEmpty()) && count > 0) {
                    // This case might happen if the range is exhausted or numbers are too large.
                    LOGGER.warning("Prime generation task from " + currentLowerBoundForThisTask + " returned no primes for count " + count + ". Advancing generation window.");
                    // Advance from where *this task* started to avoid getting stuck.
                    nextLowerBoundForGeneration = currentLowerBoundForThisTask + (count > 1 ? count * 2L : 2L); // Heuristic
                    if (nextLowerBoundForGeneration % 2 == 0) {
                        nextLowerBoundForGeneration++;
                    }
                }
                LOGGER.info("Next lower bound for global generation updated to: " + nextLowerBoundForGeneration);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during prime generation task that started from " + currentLowerBoundForThisTask, e);
            }
        });
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
}
