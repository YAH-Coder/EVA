package org.example.utils;

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
        if (number <= 3) return number > 1;
        if ((number & 1) == 0) return false;
        if (number % 3 == 0) return false;

        for (long i = 5; i * i <= number; i += 6) {
            if (number % i == 0 || number % (i + 2) == 0)
                return false;
        }
        return true;
    }

    public long getCurrent() {
        return current;
    }
}