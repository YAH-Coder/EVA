package org.example.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

// Renamed from PrimeNumberGeneratorThread and made a Callable
public class PrimeNumberGeneratorTask implements Callable<Set<Long>> {
    private final long lowerBound;
    private final long needed;
    private long lastPrimeGenerated; // To help update the next lower bound

    public PrimeNumberGeneratorTask(long lowerBound, long needed) {
        this.lowerBound = lowerBound;
        this.needed = needed;
    }

    @Override
    public Set<Long> call() throws Exception {
        Set<Long> bucket = new HashSet<>();
        PrimeNumberGenerator generator = new PrimeNumberGenerator(lowerBound);
        for (int i = 0; i < needed && !Thread.currentThread().isInterrupted(); i++) {
            long prime = generator.nextPrime();
            bucket.add(prime);
            lastPrimeGenerated = prime;
        }
        return bucket;
    }

    public long getLastPrimeGenerated() {
        return lastPrimeGenerated;
    }
}
