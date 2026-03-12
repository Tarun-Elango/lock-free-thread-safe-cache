
package com.cache;
    
/**
 * Immutable wrapper around the Value
 * CAS; swaps the Reference atomically. if mutable then another thread can sneak and
 * modify the value.
 */
public record CacheEntry<V>(
        V value,
        long createdAt   // System.nanoTime() at insertion — useful for TTL later
) {

    // factory method- use instead of constructor, for accurate timestamp
    public static <V> CacheEntry<V> of(V value){// input is the value to be stored
        return new CacheEntry<>(value, System.nanoTime());// return a new CacheEntry with the current timestamp
    }

    // how old is this entry in milliseconds
    public long ageMillis(){
        return (System.nanoTime() - createdAt) / 1_000_000; // convert nanoseconds to milliseconds
    }

}
