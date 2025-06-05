package org.example.utils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WheelEulerPrimeIterator implements Iterator<Long> {
    private static final int WHEEL_SIZE = 30;
    private static final int[] WHEEL_OFFSETS = {1,7,11,13,17,19,23,29};
    private static final byte[] WHEEL_INDEX = new byte[WHEEL_SIZE];
    private static final long[] SMALL_PRIMES = {2,3,5};
    private static final int SEGMENT_SIZE = 262_144;

    private final Object stateLock = new Object();
    private final ReentrantReadWriteLock primesLock = new ReentrantReadWriteLock();

    private volatile long segmentStart;
    private volatile long lastPrime = -1L;
    private volatile int lastWheelPos = 0;
    private volatile boolean hasNextCached = false;
    private volatile Long nextPrime = null;

    private long[] primes = new long[50_000];
    private int primeCount = 0;

    private BitSet sieve;
    private final long exactStart;

    static {
        Arrays.fill(WHEEL_INDEX,(byte)-1);
        for(int i=0;i<WHEEL_OFFSETS.length;i++) WHEEL_INDEX[WHEEL_OFFSETS[i]]=(byte)i;
    }

    public WheelEulerPrimeIterator(){
        this(1_000_000_001L);
    }

    public WheelEulerPrimeIterator(long startValue){
        if(startValue<0) throw new IllegalArgumentException("negative start");
        exactStart=startValue;
        segmentStart=(startValue/SEGMENT_SIZE)*SEGMENT_SIZE;
        generateBasePrimes();
        generateSegment();
    }

    @Override
    public boolean hasNext(){
        synchronized(stateLock){
            if(!hasNextCached){
                nextPrime=computeNext();
                hasNextCached=true;
            }
            return nextPrime!=null;
        }
    }

    @Override
    public Long next(){
        synchronized(stateLock){
            if(!hasNext()) throw new NoSuchElementException();
            Long r=nextPrime;
            hasNextCached=false;
            nextPrime=null;
            return r;
        }
    }

    @Override
    public void remove(){
        throw new UnsupportedOperationException();
    }

    private static long wheelToNumber(long base,int offset){
        return base*WHEEL_SIZE+WHEEL_OFFSETS[offset];
    }

    private static boolean isWheelNumber(long n){
        return WHEEL_INDEX[(int)(n%WHEEL_SIZE)]!=-1;
    }

    private static int getWheelIndex(long n){
        return WHEEL_INDEX[(int)(n%WHEEL_SIZE)];
    }

    private static boolean overflow(long a,long b){
        return a>0&&b>0&&a>Long.MAX_VALUE/b;
    }

    private void generateBasePrimes(){
        int limit=100_000;
        boolean[] mark=new boolean[limit+1];
        Arrays.fill(mark,true);
        mark[0]=mark[1]=false;
        for(int p=2;p*p<=limit;p++) if(mark[p]) for(int m=p*p;m<=limit;m+=p) mark[m]=false;
        for(int n=2;n<=limit;n++) if(mark[n]) addPrime(n);
    }

    private void addPrime(long p){
        primesLock.writeLock().lock();
        try{
            if(primeCount==primes.length) primes=Arrays.copyOf(primes,primes.length*2);
            primes[primeCount++]=p;
        }finally{primesLock.writeLock().unlock();}
    }

    private void generateSegment(){
        long end=segmentStart+SEGMENT_SIZE-1;
        long wheelBase0=segmentStart/WHEEL_SIZE;
        int slots=(int)(((end-segmentStart)/WHEEL_SIZE+1)*WHEEL_OFFSETS.length);
        sieve=new BitSet(slots);
        sieve.set(0,slots);

        long sqrt=(long)Math.sqrt(end);
        primesLock.readLock().lock();
        try{
            for(int i=0;i<primeCount&&primes[i]<=sqrt;i++){
                long p=primes[i];
                if(p==2||p==3||p==5) continue;
                long first=((segmentStart+p-1)/p)*p;
                while(!isWheelNumber(first)) first+=p;
                for(long m=first;m<=end;m+=p){
                    if(!isWheelNumber(m)) continue;
                    int bit=(int)(((m/WHEEL_SIZE)-wheelBase0)*WHEEL_OFFSETS.length+getWheelIndex(m));
                    sieve.clear(bit);
                }
            }
        }finally{primesLock.readLock().unlock();}

        for(int pos=sieve.nextSetBit(0);pos>=0;pos=sieve.nextSetBit(pos+1)){
            long cand=wheelToNumber(wheelBase0+pos/WHEEL_OFFSETS.length,pos%WHEEL_OFFSETS.length);
            if(cand>end) break;
            addPrime(cand);
            markEuler(cand,end,wheelBase0);
        }
        lastWheelPos=0;
    }

    private void markEuler(long q,long end,long wheelBase0){
        primesLock.readLock().lock();
        try{
            for(int i=0;i<primeCount;i++){
                long p=primes[i];
                if(p==2||p==3||p==5) continue;
                if(overflow(p,q)) break;
                long comp=p*q;
                if(comp>end) break;
                if(!isWheelNumber(comp)) continue;
                int bit=(int)(((comp/WHEEL_SIZE)-wheelBase0)*WHEEL_OFFSETS.length+getWheelIndex(comp));
                if(bit>=0&&bit<sieve.size()) sieve.clear(bit);
                if(q%p==0) break;
            }
        }finally{primesLock.readLock().unlock();}
    }

    private Long computeNext(){
        while(true){
            int bit=sieve.nextSetBit(lastWheelPos);
            if(bit>=0){
                long wheelBase=segmentStart/WHEEL_SIZE;
                long cand=wheelToNumber(wheelBase+bit/WHEEL_OFFSETS.length,bit%WHEEL_OFFSETS.length);
                lastPrime=cand;
                lastWheelPos=bit+1;
                if(cand>=exactStart) return cand;
                continue;
            }
            segmentStart+=SEGMENT_SIZE;
            generateSegment();
            lastWheelPos=0;
        }
    }

    public long getLastPrime(){
        synchronized(stateLock){return lastPrime;}
    }

    public int getKnownPrimeCount(){
        primesLock.readLock().lock();
        try{return primeCount;}finally{primesLock.readLock().unlock();}
    }

    public boolean isPrime(long n){
        if(n<2) return false;
        for(long sp:SMALL_PRIMES) if(n==sp) return true;
        if(!isWheelNumber(n)) return false;
        long limit=(long)Math.sqrt(n);
        primesLock.readLock().lock();
        try{
            for(int i=0;i<primeCount&&primes[i]<=limit;i++) if(n%primes[i]==0) return false;
        }finally{primesLock.readLock().unlock();}
        return true;
    }

    public static Iterable<Long> primes(){
        return WheelEulerPrimeIterator::new;
    }

    public static Iterable<Long> primes(long start){
        return ()->new WheelEulerPrimeIterator(start);
    }
}