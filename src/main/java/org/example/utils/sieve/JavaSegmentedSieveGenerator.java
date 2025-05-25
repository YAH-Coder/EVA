package org.example.utils.sieve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements the {@link PrimeSegmentGenerator} interface using a segmented Sieve of Eratosthenes algorithm.
 * This class is designed to efficiently find prime numbers within a specified numerical segment (range).
 *
 * <p><b>Algorithm Overview:</b></p>
 * <ol>
 *   <li><b>Base Primes Pre-computation:</b> A list of base primes up to a certain limit (square root of the maximum possible number in a segment, e.g., sqrt(10^10) for 10-digit numbers) is pre-calculated using a standard Sieve of Eratosthenes. This is done once when the class is loaded.
 *   <li><b>Segment Sieving:</b> For a given segment [segmentStart, segmentStart + segmentSize - 1]:
 *     <ul>
 *       <li>A boolean array {@code isCompositeInSegment} of size {@code segmentSize} is created, initially marking all numbers in the segment as potentially prime (false).
 *       <li>For each base prime {@code p} from the pre-computed list:
 *         <ul>
 *           <li>Calculate the first multiple of {@code p} that is greater than or equal to {@code segmentStart}.
 *           <li>Mark all multiples of {@code p} within the segment as composite in the {@code isCompositeInSegment} array.
 *         </ul>
 *       </li>
 *       <li>Iterate through the {@code isCompositeInSegment} array. If an entry {@code isCompositeInSegment[i]} is false, then the number {@code segmentStart + i} is prime.
 *     </ul>
 *   </li>
 * </ol>
 * This approach is memory-efficient for large ranges as it only requires a boolean array proportional to the segment size, rather than the entire upper bound.
 */
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

    /**
     * Generates prime numbers within a specified segment [segmentStart, segmentStart + segmentSize - 1]
     * and stores them in the provided {@code outPrimes} array.
     *
     * @param segmentStart The starting number (inclusive) of the segment to sieve. Must be non-negative.
     * @param segmentSize The size of the segment. Must be positive.
     * @param outPrimes An array where the found prime numbers will be stored. The array must be large enough
     *                  to hold all primes found in the segment.
     * @return The total number of prime numbers found and stored in {@code outPrimes}.
     * @throws IllegalArgumentException if {@code segmentStart} is negative, {@code segmentSize} is not positive,
     *                                  {@code outPrimes} is null, or if {@code outPrimes} is too small to hold
     *                                  all the primes found in the segment.
     */
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
