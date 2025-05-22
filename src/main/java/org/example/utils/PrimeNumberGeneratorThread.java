package org.example.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PrimeNumberGeneratorThread extends Thread {
    private final Set<Long> bucket = Collections.synchronizedSet(new HashSet<>());
    private final long lowerBound;
    private final long needed;

    public PrimeNumberGeneratorThread(long lowerBound, long needed) {
        this.lowerBound = lowerBound;
        this.needed = needed;
        start(); // Start the thread immediately
    }

    @Override
    public void run() {
        PrimeNumberGenerator generator = new PrimeNumberGenerator(lowerBound);
        for (int i = 0; i < needed && !Thread.currentThread().isInterrupted(); i++) {
            bucket.add(generator.nextPrime());
        }
    }

    public Set<Long> getBucket() throws InterruptedException {
        join(); // Wait for thread to complete
        return bucket;
    }
}