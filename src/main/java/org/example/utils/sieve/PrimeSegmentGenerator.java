package org.example.utils.sieve;

/**
 * Interface for classes that generate prime numbers within a specific numerical segment.
 * Implementations are expected to use algorithms like the Sieve of Eratosthenes,
 * optimized for finding primes in a given range [segmentStart, segmentStart + segmentSize - 1].
 */
public interface PrimeSegmentGenerator {
    /**
     * Finds all prime numbers within the specified segment [segmentStart, segmentStart + segmentSize - 1]
     * and stores them into the provided {@code outPrimes} array.
     *
     * <p>Implementers should be efficient in finding primes within the given range.
     * If the generator is intended for specific use cases (e.g., generating 10-digit primes for {@link org.example.utils.SharedIDService}),
     * it might implicitly filter or assume ranges relevant to that use case. However, the primary contract
     * is to find all primes in the mathematical segment defined by {@code segmentStart} and {@code segmentSize}.
     * Numbers like 0 and 1, if they fall within the segment, should not be considered prime.
     * </p>
     *
     * @param segmentStart      The starting number (inclusive) of the segment to sieve. Must be non-negative.
     * @param segmentSize       The size of the segment (e.g., common values might be 2^16 to 2^20). Must be positive.
     * @param outPrimes         An array pre-allocated by the caller to hold the prime numbers found within the segment.
     *                          The size of this array must be sufficient to accommodate all potential primes.
     *                          The caller is responsible for estimating this size. A common estimation for the number of primes
     *                          up to N is N/ln(N); for a segment, density varies but this can be a rough guide.
     *                          If the array is too small, the behavior is implementation-dependent but should ideally
     *                          be documented by the implementer (e.g., throwing an exception).
     * @return The number of prime numbers actually found in the segment and stored in the {@code outPrimes} array.
     *         This count indicates how many of the initial elements of {@code outPrimes} are valid primes.
     * @throws IllegalArgumentException if {@code segmentStart} is negative, {@code segmentSize} is not positive,
     *                                  {@code outPrimes} is null, or (depending on implementation) if {@code outPrimes}
     *                                  is determined to be too small to hold the results.
     */
    int generatePrimes(long segmentStart, int segmentSize, long[] outPrimes);
}
