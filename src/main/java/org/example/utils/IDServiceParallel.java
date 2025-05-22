package org.example.utils;

import org.example.utils.PrimeNumberGeneratorThread;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IDServiceParallel {
    private final Set<Long> available = ConcurrentHashMap.newKeySet();
    private final Set<Long> active = ConcurrentHashMap.newKeySet();
    private static final long LOWER_BOUND = 1_000_000_000L;
    private static final int REPLENISH_THRESHOLD = 100;
    private static final int REPLENISH_AMOUNT = 100;

    public IDServiceParallel(long initialNeeded) throws InterruptedException {
        PrimeNumberGeneratorThread thread = new PrimeNumberGeneratorThread(LOWER_BOUND, initialNeeded);
        available.addAll(thread.getBucket());
    }

    public long getNew() throws InterruptedException {
        synchronized (this) {
            if (available.isEmpty()) {
                replenish(REPLENISH_AMOUNT);
            }
            Long prime = available.iterator().next();
            available.remove(prime);
            active.add(prime);
            if (available.size() < REPLENISH_THRESHOLD) {
                replenish(REPLENISH_AMOUNT);
            }
            return prime;
        }
    }

    public void delete(long id) {
        synchronized (this) {
            if (!active.remove(id)) {
                throw new NoSuchElementException("ID " + id + " not managed by IDServiceParallel");
            }
            available.add(id);
        }
    }

    private void replenish(long amount) {
        new Thread(() -> {
            PrimeNumberGeneratorThread refillThread = new PrimeNumberGeneratorThread(LOWER_BOUND, amount);
            try {
                available.addAll(refillThread.getBucket());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}