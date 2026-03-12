package com.cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap; // this is a hash map that is thread-safe
import java.util.concurrent.atomic.AtomicReference; // this is used for lock-free operations

/**
 * 2 layers of safety:
 * 1. ConcurrentHashMap - thread-safe operations
 * 2. AtomicReference <CacheEntry> - lock-free operations, updates to same keys values.
 *      CAS - compare and swap; in single CPU instruction
 *      Protects concurrent updates to the SAME key's value.
 * 
 * No synchronized. No ReentrantLock. No blocking
 * many threads allowed at a time ( not locked ) , and we dont block 
 * worst case - threads keep retrying until success
 */
public class LockFreeCacheImpl<K,V> implements LockFreeCache<K,V>{

    private final ConcurrentHashMap<K, AtomicReference<CacheEntry<V>>> store = new ConcurrentHashMap<>();

    @Override
    public void put(K key, V value) {
        CacheEntry<V> entry = CacheEntry.of(value);

        // try to insert new entry
        /*
         * computeIfAbsent is atomic operation
         * if key is not present, it will create a new entry ( new AtomicReference<CacheEntry<V>> )
         * if key is present, it will return the existing entry
         */
        AtomicReference<CacheEntry<V>> ref = store.computeIfAbsent(
                key,// key to check
                k -> new AtomicReference<>(entry)  // new key: we're done here
        );

        if (ref.get()!= null) {
            // key already exists, need to update
            //CAS loop

            while(true){
                CacheEntry<V> currentEntry = ref.get();// get current value from AtomicReference
                 // compare current value with the one in the AtomicReference and swap if same
                if (ref.compareAndSet(currentEntry, entry)) {
                    // update successful
                    break;
                }
                // update failed, retry
            }
        }
        // else key was inserted successfully
    }

    @Override
    public Optional<V> get(K key){
        AtomicReference<CacheEntry<V>> ref = store.get(key);// from map return AtomicReference( no need to lock)
        if (ref == null) {
            return Optional.empty();
        }
        
        CacheEntry<V> entry = ref.get();// get the value from AtomicReference
        
        if (entry == null) {
            return Optional.empty();
        }
        
        return Optional.of(entry.value());// optional container means may or may not have a value, avoid null returns
    }

    @Override
    public void remove(K key){
        store.remove(key);
    }

    @Override
    public int size(){
        return store.size();
    }

    @Override
    public void clear(){
        store.clear();
    }
}
