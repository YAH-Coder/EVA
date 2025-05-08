package org.example.utils;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class IDService {
    private final Set<Long> ids;
    private final PrimeNumberGenerator generator;

    public IDService() {
        this.ids = new HashSet<>();
        this.generator = new PrimeNumberGenerator(1_000_000_000L);
    }

    public long getNew() {
        long prime;
        do {
            prime = generator.nextPrime();
        } while (ids.contains(prime));
        ids.add(prime);
        return prime;
    }

    public void delete(long id) {
        if (!ids.remove(id)) {
            throw new NoSuchElementException("ID " + id + " not managed by IDService");
        }
    }
}