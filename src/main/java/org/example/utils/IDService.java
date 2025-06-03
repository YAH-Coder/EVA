package org.example.utils;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IDService {
    private final Set<Long> ids;
    private final WheelEulerPrimeIterator iterator;

    public IDService() {
        this.ids = ConcurrentHashMap.newKeySet();
        this.iterator = new WheelEulerPrimeIterator();
    }

    public long getNew() {
        long prime;
        do {
            prime = iterator.next();
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