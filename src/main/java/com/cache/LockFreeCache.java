package com.cache;
import java.util.Optional;

public interface LockFreeCache<K, V> {  
    // k and v - dont care about type
    // define k and v, when creating instance of LockFreeCache 
    // like LockFreeCache<String, Integer> cache = new LockFreeCache<>();
    
    void put(K key, V value);
    Optional<V> get(K key);// retrieve value for key
    void remove(K key);// remove key and value for key
    int size();
    void clear(); 
}
