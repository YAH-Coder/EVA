package org.example;

import java.util.stream.IntStream;

public class PrimeNumberGenerator {
    private long current;

    public PrimeNumberGenerator(long lowerBound) {
        this.current = Math.max(2, lowerBound);
    }

    public long nextPrime() {
        while (true) {
            if (isPrime(current)) {
                return current++;
            }
            current++;
        }
    }

    private static boolean isPrime(long number) {
//        for (long i = 2; i < number; i++) {
//            if(number % i == 0) {
//                return false;
//            }
//        }
//        return true;
        return number > 1
                && IntStream.rangeClosed(2, (int) Math.sqrt(number))
                .noneMatch(i -> number % i == 0);
    }
}