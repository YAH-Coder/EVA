package org.example.utils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WheelEulerPrimeIterator implements Iterator<Long> {
    // 2×3×5 wheel - only consider numbers coprime to 2,3,5
    private static final int WHEEL_SIZE = 30;
    private static final int[] WHEEL_OFFSETS = {1, 7, 11, 13, 17, 19, 23, 29}; // 8 positions in wheel
    private static final byte[] WHEEL_INDEX = new byte[30]; // Lookup table

    private static final long START = 1_000_000_007L;
    private static final int SEGMENT_SIZE = 262144; // 256KB segments

    // Thread-safe locks
    private final Object stateLock = new Object();
    private final ReentrantReadWriteLock primesLock = new ReentrantReadWriteLock();

    // Volatile fields for thread safety
    private volatile long segmentStart;
    private volatile long lastPrime;
    private volatile int lastWheelPos;
    private volatile boolean hasNextCached;
    private volatile Long nextPrime;

    // Protected by primesLock
    private long[] primes;
    private int primeCount;
    private int primeCapacity;

    // Protected by stateLock
    private BitSet sieve; // Only stores wheel positions
    
    // Store the exact start value to ensure we don't return primes less than this
    private final long exactStart;

    static {
        // Initialize wheel lookup table
        Arrays.fill(WHEEL_INDEX, (byte)-1);
        for (int i = 0; i < WHEEL_OFFSETS.length; i++) {
            WHEEL_INDEX[WHEEL_OFFSETS[i]] = (byte)i;
        }
    }

    public WheelEulerPrimeIterator() {
        this(START);
    }

    public WheelEulerPrimeIterator(long startValue) {
        this.exactStart = startValue;
        // Align segment start to beginning of a segment that contains startValue
        this.segmentStart = (startValue / SEGMENT_SIZE) * SEGMENT_SIZE;
        this.lastPrime = -1;
        this.primeCapacity = 50000;
        this.primes = new long[primeCapacity];
        this.primeCount = 0;
        this.lastWheelPos = 0;
        this.hasNextCached = false;
        this.nextPrime = null;

        generateBasePrimes();
        generateSegment();
        
        // Position lastWheelPos correctly for the startValue
        positionToStartValue();
    }
    
    private void positionToStartValue() {
        synchronized (stateLock) {
            // Find the position in the sieve that corresponds to exactStart or the next prime after it
            long wheelBase = segmentStart / WHEEL_SIZE;
            
            // Skip ahead in the current segment until we find a position >= exactStart
            while (true) {
                int nextBit = sieve.nextSetBit(lastWheelPos);
                
                if (nextBit == -1) {
                    // Move to next segment if we've exhausted this one
                    segmentStart += SEGMENT_SIZE;
                    generateSegment();
                    continue;
                }
                
                int wheelIdx = nextBit % WHEEL_OFFSETS.length;
                long base = wheelBase + nextBit / WHEEL_OFFSETS.length;
                long candidate = wheelToNumber(base, wheelIdx);
                
                if (candidate >= exactStart) {
                    // We found a valid starting position, don't advance lastWheelPos yet
                    // so next() will find this position
                    return;
                }
                
                // Move past this position
                lastWheelPos = nextBit + 1;
            }
        }
    }

    private void generateBasePrimes() {
        // Add wheel base primes
        addPrime(2);
        addPrime(3);
        addPrime(5);

        // Generate primes up to reasonable limit using wheel
        int limit = 100000;
        boolean[] isPrime = new boolean[limit + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = isPrime[1] = false;

        // Mark wheel base multiples
        for (int i = 4; i <= limit; i += 2) isPrime[i] = false;
        for (int i = 9; i <= limit; i += 3) isPrime[i] = false;
        for (int i = 25; i <= limit; i += 5) isPrime[i] = false;

        // Wheel-based Euler sieve
        for (long base = 0; base * WHEEL_SIZE <= limit; base++) {
            for (int wheelIdx = 0; wheelIdx < WHEEL_OFFSETS.length; wheelIdx++) {
                long num = base * WHEEL_SIZE + WHEEL_OFFSETS[wheelIdx];
                if (num > limit) break;

                if (isPrime[(int)num]) {
                    addPrime(num);
                    markCompositesWheel(num, limit, isPrime);
                }
            }
        }
    }

    private void markCompositesWheel(long num, int limit, boolean[] isPrime) {
        primesLock.readLock().lock();
        try {
            for (int i = 3; i < primeCount; i++) { // Skip 2,3,5 as they're wheel bases
                long prime = primes[i];
                long composite = prime * num;

                if (composite > limit) break;
                isPrime[(int)composite] = false;

                if (num % prime == 0) break; // Euler's key optimization
            }
        } finally {
            primesLock.readLock().unlock();
        }
    }

    private void addPrime(long prime) {
        primesLock.writeLock().lock();
        try {
            if (primeCount >= primeCapacity) {
                primeCapacity *= 2;
                primes = Arrays.copyOf(primes, primeCapacity);
            }
            primes[primeCount++] = prime;
        } finally {
            primesLock.writeLock().unlock();
        }
    }

    private long wheelToNumber(long wheelBase, int wheelOffset) {
        return wheelBase * WHEEL_SIZE + WHEEL_OFFSETS[wheelOffset];
    }

    private boolean isWheelNumber(long num) {
        return WHEEL_INDEX[(int)(num % WHEEL_SIZE)] != -1;
    }

    private int getWheelIndex(long num) {
        return WHEEL_INDEX[(int)(num % WHEEL_SIZE)];
    }

    private void generateSegment() {
        // This method is called from synchronized context, so no additional locking needed here
        int wheelSlots = (SEGMENT_SIZE / WHEEL_SIZE + 1) * WHEEL_OFFSETS.length;
        sieve = new BitSet(wheelSlots);
        sieve.set(0, wheelSlots); // Mark all wheel positions as potential primes

        long segmentEnd = segmentStart + SEGMENT_SIZE - 1;
        long wheelBase = segmentStart / WHEEL_SIZE;

        // Process only wheel positions using Euler's sieve
        for (long base = wheelBase; base * WHEEL_SIZE <= segmentEnd; base++) {
            for (int wheelIdx = 0; wheelIdx < WHEEL_OFFSETS.length; wheelIdx++) {
                long num = base * WHEEL_SIZE + WHEEL_OFFSETS[wheelIdx];

                if (num < segmentStart || num > segmentEnd) continue;

                int bitPos = (int)((base - wheelBase) * WHEEL_OFFSETS.length + wheelIdx);
                if (bitPos >= wheelSlots || !sieve.get(bitPos)) continue;

                // Found a prime - add to collection
                long lastKnownPrime;
                primesLock.readLock().lock();
                try {
                    lastKnownPrime = primeCount > 0 ? primes[primeCount - 1] : 0;
                } finally {
                    primesLock.readLock().unlock();
                }

                if (num > lastKnownPrime) {
                    addPrime(num);
                }

                // Mark composites using wheel-optimized Euler method
                markWheelComposites(num, segmentEnd, wheelBase);
            }
        }

        lastWheelPos = 0;
    }

    private void markWheelComposites(long num, long segmentEnd, long wheelBase) {
        primesLock.readLock().lock();
        try {
            for (int i = 3; i < primeCount; i++) { // Skip wheel base primes 2,3,5
                long prime = primes[i];
                long composite = prime * num;

                if (composite > segmentEnd) break;
                if (composite < segmentStart) continue;

                // Only mark if composite falls on wheel position
                if (isWheelNumber(composite)) {
                    long compBase = composite / WHEEL_SIZE;
                    int compWheelIdx = getWheelIndex(composite);
                    int bitPos = (int)((compBase - wheelBase) * WHEEL_OFFSETS.length + compWheelIdx);

                    if (bitPos >= 0 && bitPos < sieve.size()) {
                        sieve.clear(bitPos);
                    }
                }

                if (num % prime == 0) break; // Euler's optimization
            }
        } finally {
            primesLock.readLock().unlock();
        }
    }

    private Long computeNext() {
        // This method is called from synchronized context
        while (true) {
            // Find next prime in current segment
            int nextBit = sieve.nextSetBit(lastWheelPos);

            if (nextBit != -1 && nextBit < sieve.size()) {
                long wheelBase = segmentStart / WHEEL_SIZE;
                int wheelIdx = nextBit % WHEEL_OFFSETS.length;
                long base = wheelBase + nextBit / WHEEL_OFFSETS.length;

                lastPrime = wheelToNumber(base, wheelIdx);
                lastWheelPos = nextBit + 1;
                
                // Make sure we never return a prime less than exactStart
                if (lastPrime < exactStart) {
                    continue;
                }

                return lastPrime;
            }

            // Move to next segment
            segmentStart += SEGMENT_SIZE;
            generateSegment();
        }
    }

    @Override
    public boolean hasNext() {
        synchronized (stateLock) {
            if (!hasNextCached) {
                nextPrime = computeNext();
                hasNextCached = true;
            }
            return nextPrime != null; // Always true for infinite prime sequence
        }
    }

    @Override
    public Long next() {
        synchronized (stateLock) {
            if (!hasNext()) {
                throw new NoSuchElementException("No more primes available");
            }

            Long result = nextPrime;
            hasNextCached = false;
            nextPrime = null;
            return result;
        }
    }

    // Iterator doesn't support remove operation for prime sequences
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove primes from sequence");
    }

    // Utility methods for convenience
    public long getLastPrime() {
        synchronized (stateLock) {
            return lastPrime;
        }
    }

    public int getKnownPrimeCount() {
        primesLock.readLock().lock();
        try {
            return primeCount;
        } finally {
            primesLock.readLock().unlock();
        }
    }

    public boolean isPrime(long n) {
        if (n == 2 || n == 3 || n == 5) return true;
        if (n < 2 || !isWheelNumber(n)) return false;

        primesLock.readLock().lock();
        try {
            for (int i = 0; i < primeCount && primes[i] * primes[i] <= n; i++) {
                if (n % primes[i] == 0) return false;
            }
        } finally {
            primesLock.readLock().unlock();
        }
        return true;
    }

    // Create an iterable that can be used in enhanced for loops
    public static Iterable<Long> primes() {
        return () -> new WheelEulerPrimeIterator();
    }

    public static Iterable<Long> primes(long startFrom) {
        return () -> new WheelEulerPrimeIterator(startFrom);
    }
}