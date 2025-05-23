package org.example.utils;

// import org.example.utils.PrimeNumberGeneratorThread; // No longer needed
import org.example.utils.PrimeNumberGeneratorTask; // New import

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class IDServiceParallel {
    private final Set<Long> available = ConcurrentHashMap.newKeySet();
    private final Set<Long> active = ConcurrentHashMap.newKeySet();
    private static final long LOWER_BOUND = 1_000_000_000L;
    private static final int REPLENISH_THRESHOLD = 100;
    private static final int REPLENISH_AMOUNT = 100;
    private static final long DEFAULT_INITIAL_NEEDED = 10000L; // For initial singleton setup

    private volatile long primeGeneratorNextStart = LOWER_BOUND; // Initialize here

    private final ExecutorService primeGenerationExecutor; // New ExecutorService field

    private static final IDServiceParallel INSTANCE;

    static {
        try {
            // INSTANCE = new IDServiceParallel(DEFAULT_INITIAL_NEEDED);
            // The InterruptedException was problematic for static initializer.
            // It's better to handle it within the constructor and rethrow as RuntimeException if needed.
            INSTANCE = new IDServiceParallel(DEFAULT_INITIAL_NEEDED);
        } catch (Exception e) { // Catching general exception from constructor, including RuntimeException
            throw new RuntimeException("Failed to initialize IDServiceParallel singleton", e);
        }
    }

    public static IDServiceParallel getInstance() {
        return INSTANCE;
    }

    // Constructor for the singleton
    private IDServiceParallel(long initialNeeded) { // Removed throws InterruptedException
        // Initialize ExecutorService first
        this.primeGenerationExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "prime-generator-pool");
            t.setDaemon(true); // Daemon threads won't prevent JVM shutdown
            return t;
        });

        // initialNeeded is DEFAULT_INITIAL_NEEDED from static block
        // primeGeneratorNextStart is already initialized to LOWER_BOUND
        PrimeNumberGeneratorTask initialTask = new PrimeNumberGeneratorTask(this.primeGeneratorNextStart, initialNeeded);
        Future<PrimeNumberGeneratorTask.Result> futureResult = this.primeGenerationExecutor.submit(initialTask);
        try {
            PrimeNumberGeneratorTask.Result result = futureResult.get(); // This blocks and can throw InterruptedException or ExecutionException
            this.available.addAll(result.primes);
            // Update primeGeneratorNextStart after successful initial generation
            if (result.nextPotentialStart > this.primeGeneratorNextStart) { // Should always be true here
                this.primeGeneratorNextStart = result.nextPotentialStart;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            throw new RuntimeException("Failed to initialize IDServiceParallel during initial prime generation (interrupted)", e);
        } catch (ExecutionException e) {
            // Log the actual cause e.g. e.getCause().printStackTrace();
            // System.err.println("ExecutionException cause: " + e.getCause());
            throw new RuntimeException("Failed to initialize IDServiceParallel during prime generation execution", e.getCause());
        } catch (Exception e) { // Catch other potential exceptions
            throw new RuntimeException("Failed to initialize IDServiceParallel due to an unexpected error during prime generation", e);
        }
    }

    public long getNew() throws InterruptedException {
        synchronized (this) {
            if (available.isEmpty()) {
                replenish(REPLENISH_AMOUNT, true); // Blocking call
                if (available.isEmpty()) { // Still empty after blocking replenish? Problem.
                    throw new RuntimeException("Replenishment failed to provide new IDs.");
                }
            }
            Long prime = available.iterator().next(); // This could still throw NoSuchElementException if the set is empty
            available.remove(prime);
            active.add(prime);
            if (available.size() < REPLENISH_THRESHOLD) {
                replenish(REPLENISH_AMOUNT, false); // Non-blocking call
            }
            return prime;
        }
    }

    public void delete(long id) {
        synchronized (this) {
            if (!active.remove(id)) {
                throw new NoSuchElementException("ID " + id + " not managed by IDServiceParallel");
            }
            available.add(id);
        }
    }

    private void replenish(long amount, boolean blocking) {
        final long currentGlobalNextStart;
        synchronized (IDServiceParallel.this) {
            currentGlobalNextStart = primeGeneratorNextStart;
        }

        if (blocking) {
            PrimeNumberGeneratorTask task = new PrimeNumberGeneratorTask(currentGlobalNextStart, amount);
            try {
                Future<PrimeNumberGeneratorTask.Result> futureResult = this.primeGenerationExecutor.submit(task);
                PrimeNumberGeneratorTask.Result result = futureResult.get(); // Blocks
                
                this.available.addAll(result.primes);
                synchronized (IDServiceParallel.this) {
                    if (result.nextPotentialStart > this.primeGeneratorNextStart) {
                        this.primeGeneratorNextStart = result.nextPotentialStart;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Blocking replenishment interrupted", e);
            } catch (ExecutionException e) {
                // System.err.println("ExecutionException cause in blocking replenish: " + e.getCause());
                throw new RuntimeException("Blocking replenishment failed during execution", e.getCause());
            }
        } else { // Non-blocking
            this.primeGenerationExecutor.submit(() -> {
                try {
                    // currentGlobalNextStart is captured from the outer scope (effectively final)
                    PrimeNumberGeneratorTask task = new PrimeNumberGeneratorTask(currentGlobalNextStart, amount);
                    // Call task.call() directly as this lambda is already running in an executor thread.
                    PrimeNumberGeneratorTask.Result result = task.call(); 
                    
                    this.available.addAll(result.primes);
                    synchronized (IDServiceParallel.this) {
                        if (result.nextPotentialStart > this.primeGeneratorNextStart) {
                            this.primeGeneratorNextStart = result.nextPotentialStart;
                        }
                    }
                } catch (InterruptedException e) { // From task.call() if it's interrupted
                    Thread.currentThread().interrupt();
                    System.err.println("Non-blocking replenishment task interrupted: " + Thread.currentThread().getName() + ", " + e.getMessage());
                } catch (Exception e) { // Catch generic Exception from task.call()
                    System.err.println("Error in non-blocking replenishment task: " + Thread.currentThread().getName() + ", " + e.getMessage());
                    // e.printStackTrace(); // For more detailed logging
                }
            });
        }
    }
}