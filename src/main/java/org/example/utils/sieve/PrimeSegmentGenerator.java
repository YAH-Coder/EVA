package org.example.utils.sieve;

public interface PrimeSegmentGenerator {
    /**
     * Finds all prime numbers within the range [segmentStart, segmentStart + segmentSize - 1]
     * and stores them into the provided outPrimes array.
     *
     * The implementer must ensure that only primes >= 1,000,000,000 are returned if segmentStart
     * is in that range or if such filtering is implicitly part of its prime generation scope.
     *
     * @param segmentStart      The starting number of the segment (inclusive). Must be non-negative.
     * @param segmentSize       The size of the segment (e.g., 1,048,576). Must be positive.
     * @param outPrimes         An array pre-allocated to hold the primes found. The size of this array
     *                          must be sufficient to hold all potential primes in the segment.
     *                          A loose upper bound for primes in a segment of size S around N is S/ln(N),
     *                          but practically, for 10-digit numbers, S/10 or S/15 might be safer for allocation.
     *                          The caller is responsible for appropriate sizing.
     * @return The number of actual primes found in the segment and stored in the outPrimes array.
     *         This count indicates how many elements in outPrimes are valid.
     * @throws IllegalArgumentException if segmentStart is negative, segmentSize is not positive,
     *                                  or outPrimes is null or too small to hold the primes.
     *                                  (Error handling for outPrimes size can be implementation-specific,
     *                                  but the interface user should be aware.)
     */
    int generatePrimes(long segmentStart, int segmentSize, long[] outPrimes);
}
