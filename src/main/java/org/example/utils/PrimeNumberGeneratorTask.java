package org.example.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

// Import for PrimeNumberGenerator, though not strictly needed if in the same package.
// For clarity, it can be included.
import org.example.utils.PrimeNumberGenerator;

public class PrimeNumberGeneratorTask implements Callable<PrimeNumberGeneratorTask.Result> {

    private final long lowerBound;
    private final long needed;

    public static class Result {
        public final java.util.Set<Long> primes;
        public final long nextPotentialStart;

        public Result(java.util.Set<Long> primes, long nextPotentialStart) {
            this.primes = primes;
            this.nextPotentialStart = nextPotentialStart;
        }
    }

    public PrimeNumberGeneratorTask(long lowerBound, long needed) {
        this.lowerBound = lowerBound;
        this.needed = needed;
    }

    @Override
    public Result call() throws Exception {
        PrimeNumberGenerator generator = new PrimeNumberGenerator(this.lowerBound);
        java.util.Set<Long> generatedPrimes = new java.util.HashSet<>();
        for (int i = 0; i < needed; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Prime generation task was interrupted.");
            }
            generatedPrimes.add(generator.nextPrime());
        }
        return new Result(generatedPrimes, generator.getCurrent());
    }
}
