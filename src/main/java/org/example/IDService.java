package org.example;

import java.util.HashSet;

public class IDService {
    private HashSet<Long> primes;
    private PrimeNumberGenerator generator ;

    public IDService() {
       primes = new HashSet<Long>();
       generator = new PrimeNumberGenerator(1_000_000_000L);
    }

    public long getNew(){
        long prime = generator.iterator().next();
        primes.add(prime);
        return prime;
    }
    public void delete(long prime) {
        primes.remove(prime);
    }

    public Long[] getPrimes() {
        return primes.toArray(new Long[primes.size()]);
    }
}
