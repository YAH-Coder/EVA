package org.example.utils.sieve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaSegmentedSieveGenerator implements PrimeSegmentGenerator {

    private static final int MAX_BASE_PRIME_LIMIT = 100_000; // Sqrt of 10^10 (max 10-digit number is 10^10 -1, sqrt is ~100,000)
    private static final List<Integer> BASE_PRIMES = sieveBasePrimes(MAX_BASE_PRIME_LIMIT);
    
    // No constructor needed if segmentSize is always passed via generatePrimes method
    // and base primes are static.

    private static List<Integer> sieveBasePrimes(int limit) {
        boolean[] isPrime = new boolean[limit + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = isPrime[1] = false;
        for (int p = 2; p * p <= limit; p++) {
            if (isPrime[p]) {
                for (int i = p * p; i <= limit; i += p)
                    isPrime[i] = false;
            }
        }
        List<Integer> primes = new ArrayList<>();
        for (int p = 2; p <= limit; p++) {
            if (isPrime[p]) {
                primes.add(p);
            }
        }
        return primes;
    }

    @Override
    public int generatePrimes(long segmentStart, int segmentSize, long[] outPrimes) {
        if (segmentStart < 0 || segmentSize <= 0 || outPrimes == null) {
            throw new IllegalArgumentException("Invalid arguments for generatePrimes: segmentStart must be non-negative, segmentSize positive, and outPrimes non-null.");
        }
        
        // isCompositeInSegment[i] = true means (segmentStart + i) is composite.
        boolean[] isCompositeInSegment = new boolean[segmentSize]; 

        for (int p : BASE_PRIMES) {
            long p_long = p; // Use long for calculations to avoid overflow
            long square_p = p_long * p_long;

            if (square_p > segmentStart + segmentSize - 1) { 
                break; // Optimization: Primes whose square is larger than the segment's end cannot have multiples in it (unless p itself is in it, but that's handled by startMultiple logic)
            }

            // Calculate the first multiple of p that is >= segmentStart
            long startMultiple = ((segmentStart + p_long - 1) / p_long) * p_long;
            
            // The first multiple to mark in the sieve is max(p*p, smallest multiple of p >= segmentStart)
            startMultiple = Math.max(square_p, startMultiple);
            
            for (long j = startMultiple; j < segmentStart + segmentSize; j += p_long) {
                // j is a multiple of p. Mark segmentStart + i = j as composite.
                // So, i = j - segmentStart.
                if (j >= segmentStart) { // Ensure j is within the segment range (it should be by loop condition, but good for clarity)
                    int indexInSegment = (int)(j - segmentStart);
                    if (indexInSegment < segmentSize) { // Check array bounds before marking
                         isCompositeInSegment[indexInSegment] = true;
                    }
                }
            }
        }

        int primeCount = 0;
        for (int i = 0; i < segmentSize; i++) {
            if (!isCompositeInSegment[i]) {
                long currentNumber = segmentStart + i;
                
                // Filter out numbers less than 2 (0 and 1 are not prime).
                // Also handles cases where segmentStart is 0 or 1.
                if (currentNumber < 2) {
                    continue; 
                }
                
                // The problem statement implies SharedIDService will ensure segmentStart >= 1,000,000,000.
                // If that's a strict guarantee, an explicit check for currentNumber < 1_000_000_000L might be redundant here.
                // However, if this class could be used more generally, such a filter might be needed based on requirements.
                // For this implementation, relying on caller (SharedIDService) to manage segmentStart.

                if (primeCount >= outPrimes.length) {
                    // This indicates an issue with the caller's estimation of required array size.
                    throw new IllegalArgumentException("outPrimes array is too small to hold all primes found in the segment. Found at least " + (primeCount + 1) + " primes.");
                }
                outPrimes[primeCount++] = currentNumber;
            }
        }
        return primeCount;
    }
}
