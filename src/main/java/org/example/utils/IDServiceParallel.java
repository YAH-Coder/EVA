package org.example.utils;

import org.example.utils.PrimeNumberGeneratorThread;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IDServiceParallel {
    private final Set<Long> available = ConcurrentHashMap.newKeySet();
    private final Set<Long> active = ConcurrentHashMap.newKeySet();
    private static final long LOWER_BOUND = 1_000_000_000L;
    private static final int REPLENISH_THRESHOLD = 100;
    private static final int REPLENISH_AMOUNT = 100;
    private static final long DEFAULT_INITIAL_NEEDED = 10000L; // For initial singleton setup

    private volatile long primeGeneratorNextStart = LOWER_BOUND; // Initialize here

    private static final IDServiceParallel INSTANCE;

    static {
        try {
            INSTANCE = new IDServiceParallel(DEFAULT_INITIAL_NEEDED);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to initialize IDServiceParallel singleton", e);
        }
    }

    public static IDServiceParallel getInstance() {
        return INSTANCE;
    }

    // Constructor for the singleton
    private IDServiceParallel(long initialNeeded) throws InterruptedException {
        // initialNeeded is DEFAULT_INITIAL_NEEDED from static block
        // primeGeneratorNextStart is already initialized to LOWER_BOUND
        PrimeNumberGeneratorThread thread = new PrimeNumberGeneratorThread(this.primeGeneratorNextStart, initialNeeded);
        try {
            Set<Long> initialPrimes = thread.getBucket(); // This joins the thread
            available.addAll(initialPrimes);
            // Update primeGeneratorNextStart after successful initial generation
            long nextStart = thread.getNextPotentialStart();
            if (nextStart > this.primeGeneratorNextStart) { // Should always be true here
                this.primeGeneratorNextStart = nextStart;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            // Wrap in RuntimeException because constructor for a static final field can't throw checked exceptions easily
            throw new RuntimeException("Failed to initialize IDServiceParallel during initial prime generation", e);
        } catch (Exception e) { // Catch other potential exceptions like IllegalStateException
            throw new RuntimeException("Failed to initialize IDServiceParallel due to an unexpected error", e);
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
        Runnable replenishmentTask = () -> {
            long lowerBoundForRefill;
            PrimeNumberGeneratorThread refillThread;

            // Synchronize the read of primeGeneratorNextStart
            synchronized (IDServiceParallel.this) { // `this` is fine as IDServiceParallel is a singleton
                lowerBoundForRefill = primeGeneratorNextStart;
            }

            refillThread = new PrimeNumberGeneratorThread(lowerBoundForRefill, amount);

            try {
                Set<Long> newPrimes = refillThread.getBucket(); // This joins the thread
                available.addAll(newPrimes);

                // After successfully getting primes, update the global next starting point.
                // This must be synchronized to prevent race conditions if multiple replenish threads run.
                synchronized (IDServiceParallel.this) { // Use the same lock
                    long nextPotentialStartFromThread = refillThread.getNextPotentialStart();
                    // Ensure we only advance primeGeneratorNextStart
                    if (nextPotentialStartFromThread > primeGeneratorNextStart) {
                        primeGeneratorNextStart = nextPotentialStartFromThread;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                if (blocking) {
                    // This exception will propagate to the getNew() method if it's waiting.
                    throw new RuntimeException("Blocking replenishment interrupted", e);
                }
                // For non-blocking, the thread just terminates, interrupt status is set.
                // Optionally, log this for non-blocking tasks if desired.
                // System.err.println("Non-blocking replenishment thread interrupted: " + Thread.currentThread().getName());
            } catch (Exception e) { // Catch other potential exceptions (e.g., IllegalStateException)
                if (blocking) {
                    throw new RuntimeException("Blocking replenishment failed due to an unexpected error", e);
                }
                // Log error for non-blocking task or handle as appropriate
                System.err.println("Error in non-blocking replenishment thread " + Thread.currentThread().getName() + ": " + e.getMessage());
                // e.printStackTrace(); // For more detailed logging if needed
            }
        };

        if (blocking) {
            replenishmentTask.run(); // Run in the current thread
        } else {
            new Thread(replenishmentTask).start(); // Run in a new thread
        }
    }
}