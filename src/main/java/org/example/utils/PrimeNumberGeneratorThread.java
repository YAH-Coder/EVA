package org.example.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PrimeNumberGeneratorThread extends Thread {
    private final Set<Long> bucket = Collections.synchronizedSet(new HashSet<>());
    private final long lowerBound;
    private final long needed;
    private PrimeNumberGenerator generator; // Store the generator instance

    public PrimeNumberGeneratorThread(long lowerBound, long needed) {
        this.lowerBound = lowerBound;
        this.needed = needed;
        start(); // Start the thread immediately
    }

    @Override
    public void run() {
        this.generator = new PrimeNumberGenerator(lowerBound); // Initialize in run()
        for (int i = 0; i < needed && !Thread.currentThread().isInterrupted(); i++) {
            bucket.add(this.generator.nextPrime());
        }
    }

    public Set<Long> getBucket() throws InterruptedException {
        join(); // Wait for thread to complete
        return bucket;
    }

    public long getNextPotentialStart() {
        if (this.generator == null) {
            // This might happen if the thread hasn't started or completed run() initialization part.
            // join() in getBucket() should ensure run() has completed.
            throw new IllegalStateException("PrimeNumberGenerator not initialized or thread run not completed.");
        }
        return this.generator.getCurrent();
    }
}