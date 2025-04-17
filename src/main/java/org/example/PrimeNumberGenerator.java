package org.example;

import java.util.Iterator;
import java.util.stream.IntStream;

public class PrimeNumberGenerator implements Iterable<Long>
{
    public PrimeNumberGenerator(long lowerBound) {
        this.lowerBound = lowerBound;
    }

    private long lowerBound;

    static boolean isPrime (long number) {
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

    @Override
    public Iterator<Long> iterator() {
        Iterator<Long> it = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                boolean prime = isPrime(lowerBound);
                while(!prime) {
                    prime = isPrime(++lowerBound);
                }
                return lowerBound++;
            }

        };
        return it;
    }
}
